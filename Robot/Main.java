/*
Note there are some settable parameters located at the SKULL in the right wide scroller

A couple of changes to the standard example project:

The changes from the standard example project are:

The inclusion of the GSON in build.gradle

dependencies {
    compile 'com.google.code.gson:gson:2.8.5'

Changes to and creation of the compile the project cmd file:

RPiVisionCompile.cmd
====

To resolve the dependency in the build.gradle for:

   compile 'com.google.code.gson:gson:2.8.5'

Add those files to your maven repository.

They are included in this project so just copy the folder 2.8.5 in this project to your maven probably located at:

C:\Users\Public\frc2019\maven\com\google\code\gson\gson\

You should then have a 2.2.4 folder and a 2.8.5 folder under gson

====

RaspBerry Pi setup:

Download frcvision image (from some WPI Github repository)
Load image on SD card with balena Etcher [or others]
Add auto mount of our camera image log USB flash drive to /etc/fstab

# USB flash drive mounted for logging
/dev/sda1	/mnt/usb	vfat	auto,users,rw,uid=1000,gid=100,umask=0002,nofail	0	0
Copy of the file fstab is included in this project and can be used.

Make camera image log directory mount point [sudo mkdir /mnt/usb]

Directories for the camera images on the flash drive are automatically made if the flash drive is inserted before our program runs
   [mkdir /mnt/usb/B; mkdir /mnt/usb/BR; mkdir /mnt/usb/E; mkdir /mnt/usb/ER]

Configure cameras [browser frcvision.local/]
The configuration file is then saved in /boot/frc.json
Copy of frc.json included in this project can be used for the Genius camera from ELevator and Microsoft HD-3000 camera for the bumper.

Some of this project is based on the frc provided example thus:
*/
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.opencv.core.Core;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;

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
       "switched cameras": [
           {
               "name": <virtual camera name>
               "key": <network table key used for selection>
               // if NT value is a string, it's treated as a name
               // if NT value is a double, it's treated as an integer index
           }
       ]
   }
 */

public final class Main {
    private static final String pId = new String("[Main]");

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
    public static class CameraConfig {
        public String name;
        public String path;
        public JsonObject config;
        public JsonElement streamConfig;
    }
	
 @SuppressWarnings("MemberName")
  public static class SwitchedCameraConfig {
    public String name;
    public String key;
  };

    private static CameraProcessB cpB;
    private static CameraProcessE cpE;
    private static ImageMerge imageDriver;

    private static Thread visionThreadB;
    private static Thread visionThreadE;
    private static Thread imageMergeThread;

    // TODO:
    // all messages go to one UDP sender defined for one port but could have two
    // senders on different ports if that makes it easier to separate the messages
    protected static UdpSend sendMessage = new UdpSend(5800);

    private static UdpReceive testUDPreceive; // test UDP receiver in place of a roboRIO
    private static Thread UDPreceiveThread; // remove these or at least don't start this thread if using the roboRIO

    static Main obj;
    Images bumperCamera;
    Images bumperPipeline;
    Images elevatorCamera;
    Images elevatorPipeline;
    AtomicInteger tapeDistance;
    ShuffleboardTab cameraTab;
    Object tabLock;




// Settable parameters for some outputs listed below the skull


     // _____________uu$$$$$$$$$$uu__________
    // ___________uu$$$$$$$$$$$$$$$$uu__________
    // _________u$$$$$$$$$$$$$$$$$$$$$u_________
    // ________u$$$$$$$$$$$$$$$$$$$$$$$u________
    // _______u$$$$$$$$$$$$$$$$$$$$$$$$$u_______
    // _______u$$$$$$$$$$$$$$$$$$$$$$$$$u_______
    // _______u$$$$$$"___"$$$"___"$$$$$$u________
    // _______"$$$$"______u$u_______$$$$"________
    // ________$$$———u$u_______u$$$________
    // ________$$$u______u$$$u______u$$$________
    // _________"$$$$uu$$$___$$$uu$$$$"_________
    // __________"$$$$$$$"___"$$$$$$$"__________
    // ____________u$$$$$$$u$$$$$$$u____________
    // _____________u$"$"$"$"$"$"$u______________
    // __uuu________$$u$_$_$_$_$u$$_______uuu__
    // _u$$$$________$$$$$u$u$u$$$_______u$$$$_
    // __$$$$$uu______"$$$$$$$$$"_____uu$$$$$$__
    // u$$$$$$$$$$$uu____"""""____uuuu$$$$$$$$$$
    // $$$$"""$$$$$$$$$$uuu___uu$$$$$$$$$"""$$$"
    // _"""______""$$$$$$$$$$$uu_""$"""___________
    // ___________uuuu_""$$$$$$$$$$uuu___________
    // __u$$$uuu$$$$$$$$$uu_""$$$$$$$$$$$uuu$$$__
    // __$$$$$$$$$$""""___________""$$$$$$$$$$$"__
    // ___"$$$$$"______________________""$$$$""__



// Settable parameters for some outputs listed below

    static String version = "5  3/20/2019";
    static boolean runTestUDPreceiver = false;
    static boolean runImageMerge = false;
    static boolean debug = false;
    static boolean displayBumperContours = false;
    static boolean displayElevatorContours = false;
// Shuffleboard display video streams commented out for contour images and merged images
// No settable variables here for that
// See the code to uncomment 
/////////////////////////////////////////////////////////////////////////////////////////////////////

    static boolean logImage = false;
    public static int team;
    public static boolean server;
    public static List<CameraConfig> cameraConfigs = new ArrayList<>();
  public static List<SwitchedCameraConfig> switchedCameraConfigs = new ArrayList<>();
  public static List<VideoSource> cameras = new ArrayList<>();

  private Main() {
  }

    /**
     * Report parse error.
     */
    public static void parseError(String str) {
        System.err.println(pId + " config error in '" + configFile + "': " + str);
    }

    /**
     * Read single camera configuration.
     */
    public static boolean readCameraConfig(JsonObject config) {
        CameraConfig cam = new CameraConfig();

        // name
        JsonElement nameElement = config.get("name");
        if (nameElement == null) {
            parseError(pId + " could not read camera name");
            return false;
        }
        cam.name = nameElement.getAsString();

        // path
        JsonElement pathElement = config.get("path");
        if (pathElement == null) {
            parseError(pId + " camera '" + cam.name + "': could not read path");
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
   * Read single switched camera configuration.
   */
  public static boolean readSwitchedCameraConfig(JsonObject config) {
    SwitchedCameraConfig cam = new SwitchedCameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read switched camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement keyElement = config.get("key");
    if (keyElement == null) {
      parseError("switched camera '" + cam.name + "': could not read key");
      return false;
    }
    cam.key = keyElement.getAsString();

    switchedCameraConfigs.add(cam);
    return true;
  }

  /**
     * Read configuration file.
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public static boolean readConfig() {
        // parse file
        JsonElement top;
        try {
            top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
        } catch (IOException ex) {
            System.err.println(pId + " could not open '" + configFile + "': " + ex + "\n");
            return false;
        }

        // top level must be an object
        if (!top.isJsonObject()) {
            parseError(pId + " must be JSON object");
            return false;
        }
        JsonObject obj = top.getAsJsonObject();

        // team number
        JsonElement teamElement = obj.get("team");
        if (teamElement == null) {
            parseError(pId + " could not read team number");
            return false;
        }
        team = teamElement.getAsInt();

        // ntmode (optional)
        if (obj.has("ntmode")) {
            String str = obj.get("ntmode").getAsString();
            if ("client".equalsIgnoreCase(str)) {
                server = false;
            } else if ("server".equalsIgnoreCase(str)) {
                server = true;
            } else
            {
                parseError(pId + " could not understand ntmode value '" + str + "'");
            }
        }

        // cameras
        JsonElement camerasElement = obj.get("cameras");
        if (camerasElement == null) {
            parseError(pId + " could not read cameras");
            return false;
        }
        JsonArray cameras = camerasElement.getAsJsonArray();
        for (JsonElement camera : cameras) {
            if (!readCameraConfig(camera.getAsJsonObject())) {
                return false;
            }
        }

    if (obj.has("switched cameras")) {
      JsonArray switchedCameras = obj.get("switched cameras").getAsJsonArray();
      for (JsonElement camera : switchedCameras) {
        if (!readSwitchedCameraConfig(camera.getAsJsonObject())) {
          return false;
        }
      }
    }

        return true;
    }

    /**
     * Start running the camera.
     */
    public static VideoSource startCamera(CameraConfig config) {
        System.out.println(pId + " Starting camera '" + config.name + "' on path " + config.path);

        // this
        CameraServer inst = CameraServer.getInstance();
        UsbCamera camera = new UsbCamera(config.name, config.path);
        MjpegServer server = inst.startAutomaticCapture(camera);

        // or this and need port to be passed in
        // UsbCamera camera = new UsbCamera(config.name, config.path);
        // MjpegServer server = new MjpegServer("serve_" + config.name, port);
        // server.setSource(camera);

        Gson gson = new GsonBuilder().create();

        camera.setConfigJson(gson.toJson(config.config));
        camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

        if (config.streamConfig != null) {
            server.setConfigJson(gson.toJson(config.streamConfig));
        }

        return camera;
    }

    /**
   * Start running the switched camera.
   */
  public static MjpegServer startSwitchedCamera(SwitchedCameraConfig config) {
    System.out.println("Starting switched camera '" + config.name + "' on " + config.key);
    MjpegServer server = CameraServer.getInstance().addSwitchedCamera(config.name);

    NetworkTableInstance.getDefault()
        .getEntry(config.key)
        .addListener(event -> {
              if (event.value.isDouble()) {
                int i = (int) event.value.getDouble();
                if (i >= 0 && i < cameras.size()) {
                  server.setSource(cameras.get(i));
                }
              } else if (event.value.isString()) {
                String str = event.value.getString();
                for (int i = 0; i < cameraConfigs.size(); i++) {
                  if (str.equals(cameraConfigs.get(i).name)) {
                    server.setSource(cameras.get(i));
                    break;
                  }
                }
              }
            },
            EntryListenerFlags.kImmediate | EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

    return server;
  }

   /**
     * Main.
     */
    public static void main(String... args) {
        Thread.currentThread().setName("4237Main");

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        if (args.length > 0) {
            configFile = args[0];
        }

        // read configuration
        if (!readConfig()) {
            System.out.println(pId + " FATAL ERROR - could not read camera configuration file " + configFile);
            return;
        }

        obj = new Main(); // instantiate Main class to provide for the Images' Mats
        Main.obj.bumperCamera = new Images();
        Main.obj.bumperPipeline = new Images();
        Main.obj.elevatorCamera = new Images();
        Main.obj.elevatorPipeline = new Images();
        Main.obj.tabLock = new Object();
        Main.obj.tapeDistance = new AtomicInteger();
        
        if(runTestUDPreceiver)
            {
            // start test UDP receiver since we don't have a roboRIO to test with
            // this would go on the roboRIO not here on the RPi
            testUDPreceive = new UdpReceive(5800);
            UDPreceiveThread = new Thread(testUDPreceive, "4237UDPreceive");
            UDPreceiveThread.start();
            }
            
        // start NetworkTables
        NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
        if (server) {
            System.out.println(pId + " Setting up NetworkTables server");
            ntinst.startServer();
        } else {
            System.out.println(pId + " Setting up NetworkTables client for team " + team);
            ntinst.startClientTeam(team);
        }

        // Create the Camera tab on the shuffleboard
        synchronized(Main.obj.tabLock)
        {
        Main.obj.cameraTab = Shuffleboard.getTab("Camera");
        }
 
        // see if USB Flash Drive mounted and if so, log the images
        try
        {
            System.out.println(pId + " Sleeping 3 seconds so auto mount will be done by now, we are hopeful.");
            Thread.sleep(3000);
        } catch (InterruptedException exc)
        {
            System.out.println(pId + " Sleep 3 seconds was interrupted");
        }

        try
        {
            // execute command to check for flash drive mounted
            List<String> command = new ArrayList<String>(); // build my command as a list of strings
            command.add("bash");
            command.add("-c");
            command.add("mountpoint -q /mnt/usb ; echo $?");

            System.out.println(pId + " Run mountpoint /mnt/usb command");
            ProcessBuilder pb1 = new ProcessBuilder(command);
            Process process1 = pb1.start();
            int errCode1 = process1.waitFor();
            command.clear();
            System.out.println(pId + " mountpoint command executed, any errors? " + (errCode1 == 0 ? "No" : "Yes"));
            String mountOutput = output(process1.getInputStream());
            System.out.println(pId + " mountpoint output:\n" + mountOutput);
            System.out.println(pId + " mountpoint errors:\n" + output(process1.getErrorStream()));
            logImage = mountOutput.startsWith("0");
            if (logImage)
            {
                System.out.println(pId + " Flash Drive Mounted /mnt/usb and image logging is on");
                // mkdir in case they don't exist. Don't bother checking for existance - just do
                // it.

                command.add("bash");
                command.add("-c");
                command.add("sudo mkdir /mnt/usb/B /mnt/usb/BR /mnt/usb/E /mnt/usb/ER");

                // execute command
                System.out.println(pId + " Run mkdir B BR E ER command");
                ProcessBuilder pb2 = new ProcessBuilder(command);
                Process process2 = pb2.start();
                int errCode2 = process2.waitFor();
                System.out.println(pId + " mkdir command executed, any errors? " + (errCode2 == 0 ? "No" : "Yes"));
                System.out.println(pId + " mkdir output:\n" + output(process2.getInputStream()));
                System.out.println(pId + " mkdir errors:\n" + output(process2.getErrorStream()));
            }
            else
                System.out.println(pId + " No Flash Drive Mounted");

        } catch (Exception ex2)
        {
            System.out.println(pId + " Error in mount process " + ex2);
        }

        System.out.flush();

        // start cameras
        for (CameraConfig config : cameraConfigs) {
            if (config.name.equalsIgnoreCase("Bumper"))
            {
                System.out.println(pId + " Starting Bumper camera");
                VideoSource Bcamera = startCamera(config);
                ///////////////////
                // Widget in Shuffleboard Tab
                Map<String, Object> mapBumperCamera = new HashMap<String, Object>();
                mapBumperCamera.put("Show crosshair", false);
                mapBumperCamera.put("Show controls", false);
                   
                synchronized(Main.obj.tabLock)
                {
                Main.obj.cameraTab.add("Bumper Camera", Bcamera)
                    .withWidget(BuiltInWidgets.kCameraStream)
                    .withPosition(20, 0)
                    .withSize(13, 13)
                    .withProperties(mapBumperCamera)
                    ;
                }
                //////////////////
                cpB = new CameraProcessB(Bcamera);
                visionThreadB = new Thread(cpB, "4237BumperCamera");
                visionThreadB.start(); // start thread using the class' run() method (just saying run() won't start a
                // thread - that just runs run() once)
            }
            else if (config.name.equalsIgnoreCase("Elevator"))
            {
                System.out.println(pId + " Starting Elevator camera");
                VideoSource Ecamera = startCamera(config);
                 ///////////////////
                // Widget in Shuffleboard Tab
                Map<String, Object> mapElevatorCamera = new HashMap<String, Object>();
                mapElevatorCamera.put("Show crosshair", false);
                mapElevatorCamera.put("Show controls", false);
          
                synchronized(Main.obj.tabLock)
                {
                Main.obj.cameraTab.add("Elevator Camera", Ecamera)
                    .withWidget(BuiltInWidgets.kCameraStream)
                    .withPosition(0, 0)
                    .withSize(20, 17)
                    .withProperties(mapElevatorCamera)
                    ;
                }
                //////////////////
                cpE = new CameraProcessE(Ecamera);
                visionThreadE = new Thread(cpE, "4237ElevatorCamera");
                visionThreadE.start();
            }
            else
                System.out.println(pId + " Unknown camera in cameraConfigs " + config.name);
        }
        
        Shuffleboard.update();
 
        // start switched cameras
        for (SwitchedCameraConfig config : switchedCameraConfigs) {
        startSwitchedCamera(config);
        }

        if(runImageMerge)
        {
            // start processed images merge and serve thread
            try
            {
                // Wait for other processes to make some images otherwise first time though gets
                // an error
                Thread.sleep(2000);
            } catch (InterruptedException ex)
            {
            }

            imageDriver = new ImageMerge();
            imageMergeThread = new Thread(imageDriver, "4237ImageMerge");
            imageMergeThread.start();
        }
        
        // visionThreadB.setDaemon(true); // defines a sort of "background" task that
        // just keeps running (until all the normal threads have terminated; must set
        // before the ".start"

        // loop forever
        for (;;) {
            try {
                System.out.println(pId + " Program Version " + version + "  current time ms " + System.currentTimeMillis());
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }
}
