
/*
RaspBerry Pi setup:

Format an SD card [SD Card Formatter]
Download frcvision image
Load image () [ B? Etcher]
Add auto mount of image log USB flash drive to /etc/fstab
make image log directory mount point [mkdir /mnt/usb]
add a file to indicate boot system or the mointed system [touch /mnt/usb/NoFlashDriveMounted]
configure cameras [browser frcvision.local/]


/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

public class Main {
  private static String configFile = "/boot/frc.json";

  @SuppressWarnings("MemberName")
  public static class CameraConfig {
    public String name;
    public String path;
    public JsonObject config;
    public JsonElement streamConfig;
  }

  public static camera_process cp;
  public static camera_processB cpB;
  public static imageMerge imageDriver;

  public static Thread visionThread;
  public static Thread visionThreadB;
  public static Thread imageMergeThread;

  public static UDPsend sendMessage = new UDPsend(5800); // allmessages go to one UDP sender defined for one port but could have two senders on
                                           // different ports if that makes it easier to separate the messages

  public static UDPreceive testUDPreceive; // test UDP receiver in place of a roboRIO
  public static Thread UDPreceiveThread;   // remove these or at least don't start this thread if using the roboRIO

  static images bumperCamera = new images();
  static images bumperPipeline = new images();
  static images elevatorCamera = new images();
  static images elevatorPipeline = new images();

  static boolean logImage = false;

  public static int team;
  public static boolean server;
  public static List<CameraConfig> cameraConfigs = new ArrayList<>();

  private Main() {
  }

  /**
   * Report parse error.
   */
  public static void parseError(String str) {
    System.err.println("config error in '" + configFile + "': " + str);
  }

  /**
   * Read single camera configuration.
   */
  public static boolean readCameraConfig(JsonObject config) {
    CameraConfig cam = new CameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement pathElement = config.get("path");
    if (pathElement == null) {
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
  public static boolean readConfig() {
    // parse file
    JsonElement top;
    try {
      top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
    } catch (IOException ex) {
      System.err.println("could not open '" + configFile + "': " + ex);
      return false;
    }

    // top level must be an object
    if (!top.isJsonObject()) {
      parseError("must be JSON object");
      return false;
    }
    JsonObject obj = top.getAsJsonObject();

    // team number
    JsonElement teamElement = obj.get("team");
    if (teamElement == null) {
      parseError("could not read team number");
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
      } else {
        parseError("could not understand ntmode value '" + str + "'");
      }
    }

    // cameras
    JsonElement camerasElement = obj.get("cameras");
    if (camerasElement == null) {
      parseError("could not read cameras");
      return false;
    }
    JsonArray cameras = camerasElement.getAsJsonArray();
    for (JsonElement camera : cameras) {
      if (!readCameraConfig(camera.getAsJsonObject())) {
        return false;
      }
    }

    return true;
  }

  /**
   * Start running the camera.
   */
  public static VideoSource startCamera(CameraConfig config, int port) {
    System.out.println(config.name + " camera on USB path " + config.path);
    // CameraServer inst = CameraServer.getInstance();
    UsbCamera camera = new UsbCamera(config.name, config.path);

    MjpegServer server = new MjpegServer("serve_" + config.name, port);
    server.setSource(camera);

    Gson gson = new GsonBuilder().create();

    camera.setConfigJson(gson.toJson(config.config));
    camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

    if (config.streamConfig != null) {
      server.setConfigJson(gson.toJson(config.streamConfig));
    }

    return camera;
  }

  /**
   * Main.
   */
  public static void main(String... args) {

    if (args.length > 0) {
      configFile = args[0];
    }

    // read configuration
    if (!readConfig()) {
      return;
    }

    // start test UDP receive since we don't have a roboRIO to test with - this
    // would go on the roboRIO not here on the RPi
    testUDPreceive = new UDPreceive(5800);
    UDPreceiveThread = new Thread(testUDPreceive);
    UDPreceiveThread.start();

    // start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    if (server) {
      System.out.println("Setting up NetworkTables server");
      ntinst.startServer();
    } else {
      System.out.println("Setting up NetworkTables client for team " + team);
      ntinst.startClientTeam(team);
    }

    // start cameras
    for (CameraConfig cameraConfig : cameraConfigs) {

      if (cameraConfig.name.equalsIgnoreCase("Bumper")) {
        System.out.println("Starting Bumper camera port 1181");
        cp = new camera_process(startCamera(cameraConfig, 1181));
        visionThread = new Thread(cp);
        visionThread.start(); // start thread using the class' run() method (just saying run() won't start a
                              // thread - that just runs run() once)
      } else if (cameraConfig.name.equalsIgnoreCase("Elevator")) {
        System.out.println("Starting Elevator camera on port 1182");
        cpB = new camera_processB(startCamera(cameraConfig, 1182));
        visionThreadB = new Thread(cpB);
        visionThreadB.start();
      } else
        System.out.println("Unknown camera in cameraConfigs");
    }

    // see if USB Flash Drive mounted and if so log the images
    {
      final File NoFlashDriveMounted = new File("/mnt/usb/NoFlashDriveMounted");
      if (NoFlashDriveMounted.exists()) {
        logImage = false;
        System.out.println("No Flash Drive Mounted");
      } else {
        logImage = true;
        System.out.println("Flash Drive Mounted and image logging is on");
      }
    }

    // start processedmiamges merge and serve thread
    imageDriver = new imageMerge();
    imageMergeThread = new Thread(imageDriver);
    imageMergeThread.start();

    // visionThread.setDaemon(true); // defines a sort of "background" task that
    // just keeps running (until all the normal threads have terminated; must set
    // before the ".start"

    // loop forever
    for (;;)

    {
      try {
        // System.out.println("Parent sleeping 10 seconds");
        Thread.sleep(10000);
      } catch (InterruptedException ex) {
        return;
      }
    }
  }
}
