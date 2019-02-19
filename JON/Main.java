
/*
RaspBerry Pi setup:

Format an SD card [SD Card Formatter]
Download frcvision image
Load image on SD card with balena Etcher [or others]
Add auto mount of our camera image log USB flash drive to /etc/fstab
Make camera image log directory mount point [mkdir /mnt/usb]
Add a file to indicate boot system or the mounted flash drive system [touch /mnt/usb/NoFlashDriveMounted]
On the flash drive(s) make the directories for the camera images
   [mkdir /mnt/usb/B; mkdir /mnt/usb/BR; mkdir /mnt/usb/E; mkdir /mnt/usb/ER]
Configure cameras [browser frcvision.local/]

/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
//import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTableInstance;

/*
   JSON format:
   {
       "team": <team number>,
       "ntmode": <"client" or "server", "client" if unspecified>
       "cameras": [
           {
               "name": <camera name>
               "path": <path, e.g. "/dev/video0">
               "pixel format": <"MJPEG", "YUYV", etc>   // optional
               "width": <video mode width>              // optional
               "height": <video mode height>            // optional
               "fps": <video mode fps>                  // optional
               "brightness": <percentage brightness>    // optional
               "white balance": <"auto", "hold", value> // optional
               "exposure": <"auto", "hold", value>      // optional
               "properties": [                          // optional
                   {
                       "name": <property name>
                       "value": <property value>
                   }
               ],
               "stream": {                              // optional
                   "properties": [
                       {
                           "name": <stream property name>
                           "value": <stream property value>
                       }
                   ]
               }
           }
       ]
   }
 */

public class Main
{
    private static String output(InputStream inputStream) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = br.readLine()) != null)
            {
                sb.append(line + System.getProperty("line.separator"));
            }
        } finally
        {
            br.close();
        }
        return sb.toString();
    }

    private static String configFile = "/boot/frc.json";

    @SuppressWarnings("MemberName")
    public static class CameraConfig
    {
        public String name;
        public String path;
        public JsonObject config;
        public JsonElement streamConfig;
    }

    public static CameraProcess cp;
    public static CameraProcessB cpB;
    public static ImageMerge imageDriver;

    public static Thread visionThread;
    public static Thread visionThreadB;
    public static Thread imageMergeThread;

    // TODO:
    // all messages go to one UDP sender defined for one port but could have two
    // senders on different ports if that makes it easier to separate the messages
    public static UdpSend sendMessage = new UdpSend(5800);

    public static UdpReceive testUDPreceive; // test UDP receiver in place of a roboRIO
    public static Thread UDPreceiveThread; // remove these or at least don't start this thread if using the roboRIO

    static Images bumperCamera = new Images();
    static Images bumperPipeline = new Images();
    static Images elevatorCamera = new Images();
    static Images elevatorPipeline = new Images();

    static boolean logImage = false;

    public static int team;
    public static boolean server;
    public static List<CameraConfig> cameraConfigs = new ArrayList<>();

    /**
     * Report parse error.
     */
    public static void parseError(String str)
    {
        System.err.println("config error in '" + configFile + "': " + str);
    }

    /**
     * Read single camera configuration.
     */
    public static boolean readCameraConfig(JsonObject config)
    {
        CameraConfig cam = new CameraConfig();

        // name
        JsonElement nameElement = config.get("name");
        if (nameElement == null)
        {
            parseError("could not read camera name");
            return false;
        }
        cam.name = nameElement.getAsString();

        // path
        JsonElement pathElement = config.get("path");
        if (pathElement == null)
        {
            parseError("camera '" + cam.name + "': could not read path");
            return false;
        }
        cam.path = pathElement.getAsString();

        // stream properties
        cam.streamConfig = config.get("stream");

        cam.config = config;

        cameraConfigs.add(cam);
        return true;
    }

    /**
     * Read configuration file.
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public static boolean readConfig()
    {
        // parse file
        JsonElement top;
        try
        {
            top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
        } catch (IOException ex)
        {
            System.err.println("could not open '" + configFile + "': " + ex);
            return false;
        }

        // top level must be an object
        if (!top.isJsonObject())
        {
            parseError("must be JSON object");
            return false;
        }
        JsonObject obj = top.getAsJsonObject();

        // team number
        JsonElement teamElement = obj.get("team");
        if (teamElement == null)
        {
            parseError("could not read team number");
            return false;
        }
        team = teamElement.getAsInt();

        // ntmode (optional)
        if (obj.has("ntmode"))
        {
            String str = obj.get("ntmode").getAsString();
            if ("client".equalsIgnoreCase(str))
            {
                server = false;
            }
            else if ("server".equalsIgnoreCase(str))
            {
                server = true;
            }
            else
            {
                parseError("could not understand ntmode value '" + str + "'");
            }
        }

        // cameras
        JsonElement camerasElement = obj.get("cameras");
        if (camerasElement == null)
        {
            parseError("could not read cameras");
            return false;
        }
        JsonArray cameras = camerasElement.getAsJsonArray();
        for (JsonElement camera : cameras)
        {
            if (!readCameraConfig(camera.getAsJsonObject()))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Start running the camera.
     */
    public static VideoSource startCamera(CameraConfig config, int port)
    {
        System.out.println(config.name + " camera on USB path " + config.path);
        // CameraServer inst = CameraServer.getInstance();
        UsbCamera camera = new UsbCamera(config.name, config.path);

        MjpegServer server = new MjpegServer("serve_" + config.name, port);
        server.setSource(camera);

        Gson gson = new GsonBuilder().create();

        camera.setConfigJson(gson.toJson(config.config));
        camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

        if (config.streamConfig != null)
        {
            server.setConfigJson(gson.toJson(config.streamConfig));
        }

        return camera;
    }

    /**
     * Main.
     */
    public static void main(String... args)
    {

        if (args.length > 0)
        {
            configFile = args[0];
        }

        // read configuration
        if (!readConfig())
        {
            return;
        }

        // start test UDP receiver since we don't have a roboRIO to test with - this
        // would go on the roboRIO not here on the RPi
        testUDPreceive = new UdpReceive(5800);
        UDPreceiveThread = new Thread(testUDPreceive);
        UDPreceiveThread.start();

        // start NetworkTables
        NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
        if (server)
        {
            System.out.println("Setting up NetworkTables server");
            ntinst.startServer();
        }
        else
        {
            System.out.println("Setting up NetworkTables client for team " + team);
            ntinst.startClientTeam(team);
        }

        // see if USB Flash Drive mounted and if so, log the images
        try
        {
            System.out.println("Parent sleeping 3 seconds so auto mount will be done");
            Thread.sleep(3000);
        } catch (InterruptedException exc)
        {
            System.out.println("Sleep 3 seconds was interrupted");
        }

        try
        {
            List<String> command = new ArrayList<String>(); // build my command as a list of strings

            command.add("bash");
            command.add("-c");
            command.add("mountpoint -q /mnt/usb ; echo $?");

            // execute command
            System.out.println("Run mountpoint /mnt/usb command");
            ProcessBuilder pb1 = new ProcessBuilder(command);
            Process process1 = pb1.start();
            int errCode1 = process1.waitFor();
            command.clear();
            System.out.println("mountpoint command executed, any errors? " + (errCode1 == 0 ? "No" : "Yes"));
            String mountOutput = output(process1.getInputStream());
            System.out.println("mountpoint output:\n" + mountOutput);
            System.out.println("mountpoint errors:\n" + output(process1.getErrorStream()));
            logImage = mountOutput.startsWith("0");
            if (logImage)
            {
                System.out.println("Flash Drive Mounted /mnt/usb and image logging is on");
                // mkdir in case they don't exist. Don't bother checking for existance - just do
                // it.

                command.add("bash");
                command.add("-c");
                command.add("sudo mkdir /mnt/usb/B /mnt/usb/BR /mnt/usb/E /mnt/usb/ER");

                // execute command
                System.out.println("Run mkdir B BR E ER command");
                ProcessBuilder pb2 = new ProcessBuilder(command);
                Process process2 = pb2.start();
                int errCode2 = process2.waitFor();
                System.out.println("mkdir command executed, any errors? " + (errCode2 == 0 ? "No" : "Yes"));
                System.out.println("mkdir output:\n" + output(process2.getInputStream()));
                System.out.println("mkdir errors:\n" + output(process2.getErrorStream()));
            }
            else
                System.out.println("No Flash Drive Mounted");

        } catch (Exception ex2)
        {
            System.out.println("Error in mount process " + ex2);
        }

        System.out.flush();

        // start cameras
        for (CameraConfig cameraConfig : cameraConfigs)
        {

            if (cameraConfig.name.equalsIgnoreCase("Bumper"))
            {
                System.out.println("Starting Bumper camera port 1181");
                cp = new CameraProcess(startCamera(cameraConfig, 1181));
                visionThread = new Thread(cp);
                visionThread.start(); // start thread using the class' run() method (just saying run() won't start a
                                      // thread - that just runs run() once)
            }
            else if (cameraConfig.name.equalsIgnoreCase("Elevator"))
            {
                System.out.println("Starting Elevator camera on port 1182");
                cpB = new CameraProcessB(startCamera(cameraConfig, 1182));
                visionThreadB = new Thread(cpB);
                visionThreadB.start();
            }
            else
                System.out.println("Unknown camera in cameraConfigs");
        }

        // start processed iamges merge and serve thread
        try
        {
            // Wait for other processes to make some images otherwise first time though gets
            // an error
            Thread.sleep(2000);
        } catch (InterruptedException ex)
        {
        }

        imageDriver = new ImageMerge();
        imageMergeThread = new Thread(imageDriver);
        imageMergeThread.start();

        // visionThread.setDaemon(true); // defines a sort of "background" task that
        // just keeps running (until all the normal threads have terminated; must set
        // before the ".start"

        // loop forever
        for (;;)
        {
            try
            {
                // System.out.println("Parent sleeping 10 seconds");
                Thread.sleep(10000);
            } catch (InterruptedException ex)
            {
                return;
            }
        }
    }
}
