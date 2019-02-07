import java.util.ArrayList;
import java.io.File;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.wpilibj.Timer;

class camera_processB implements Runnable {

	private TargetInfoB targetInfoB = new TargetInfoB();
	VideoSource camera;

	camera_processB(VideoSource aCamera) {
		camera = aCamera;
	}

	public void getTargetInfoB(TargetInfoB aTargetInfoB) {
		targetInfoB.get(aTargetInfoB);
	}

	public void run() {
		visionPipeline();
	}

	private void visionPipeline() {

		GripPipelineCubeB gripperCubeB = new GripPipelineCubeB();

		// initialize target data from this camera frame
		targetInfoB.set(-1., -1., 0., 0.);

		// Get a CvSink. This will capture Mats from the camera
		CvSink cvSink = new CvSink("cvsinkB");
		cvSink.setSource(camera);

		// Setup a CvSource. This will send images back to the Dashboard WPI style
		CvSource outputStream = new CvSource("Elevator", VideoMode.PixelFormat.kMJPEG, 320, 240, 30);
		// CvSource cvsource = new CvSource("cvsource", VideoMode.PixelFormat.kMJPEG,
		// width, height, frames_per_sec);

		MjpegServer mjpegServer2 = new MjpegServer("serve_Elevator", 1184);

		mjpegServer2.setSource(outputStream);

		// Mats are very memory expensive. Lets reuse this Mat.
		// all Java Mats have to be "pre-allocated" - they can't be in the loop because
		// they are never released and will cause out of memory
		Mat mat = new Mat();
		Timer time = new Timer();
		time.start();
		int FrameNumber = 0;

		while (!Thread.interrupted()) // This lets the robot stop this thread
		{
			double startTime = time.get();
			// System.out.println("\n[Vision] Starting camera frame grab at Time = " +
			// startTime);
			
			FrameNumber++;
			
			// Tell the CvSink to grab a frame from the camera and put it
			// in the source mat. If there is an error notify the output.
			if (cvSink.grabFrame(mat) == 0) {
				// Send the output the error.
				outputStream.notifyError(cvSink.getError());
				// skip the rest of the current iteration
				continue;
			}

			/*
			a couple of examples of a quick and dirty rectangular mask
			copy something into the image (zeros or ones) or draw a rectangle
			maybe making a zeros once is more efficient then use the variable zeros
			Mat zeros = Mat.zeros(60, 160, CvType.CV_8UC3);
			Mat.zeros(120, 320, CvType.CV_8UC3).copyTo(mat.submat(0, 120, 0, 320));
			Imgproc.rectangle(mat, new Point(0,0), new Point(320,120), new Scalar(0, 0, 0), -1);
			*/

			if (Main.logImage)
				try {
					String filename = String.format("/mnt/usb/ER/%06d.jpg", FrameNumber);
					final File file = new File(filename);
					filename = file.toString();
					if(!Imgcodecs.imwrite(filename, mat)) System.out.println("Error writing ER");
				} catch (Exception e) {
					System.out.println(e.toString());
				}

			Main.elevatorCamera.setImage(mat);

			ArrayList<MatOfPoint> contoursFiltered;

			gripperCubeB.process(mat);
			contoursFiltered = new ArrayList<MatOfPoint>(gripperCubeB.filterContoursOutput());

			if (contoursFiltered.isEmpty()) { // no contours in this frame
												// debug output
												// System.out.println("No GRIP Contours");
				Imgproc.putText(mat, "No Contours", new Point(50, 22), Core.FONT_HERSHEY_SIMPLEX, 0.4,
						new Scalar(255, 255, 255), 2);
				targetInfoB.set(-1., -1., 0., 0.);
			} else { // process the contours
					// System.out.println(contoursFiltered.size() + " contours");

				// draw all contours at once (negative index); could draw one at a time within
				// the contour loop but this has to be more efficient
				Imgproc.drawContours(mat, contoursFiltered, -1, new Scalar(255, 255, 255), 2); // + thickness => empty ;
																								// - thickness => filled

				// each contour is reduced to a single point - the COG x, y of the contour

				double bestX = -1;
				double bestY = -1;
				double bestWidth = 0;
				double bestArea = 0;

				for (MatOfPoint aContour : contoursFiltered) // iterate one contour at a time through all the contours
				{
					// debug output
					// System.out.print("[Vision] " + aContour.size() + " points in
					// contour\n[Vision]"); // a contour is a bunch of points
					// convert MatofPoint to an array of those Points and iterate (could do list of
					// Points but no need for this)
					// for(Point aPoint : aContour.toArray())System.out.print(" " + aPoint); //
					// print each point

					Rect br = Imgproc.boundingRect(aContour); // bounding upright rectangle for the contour's points
																// used to find center of contour
					int cogX = br.x + (br.width / 2); // center of gravity
					int cogY = br.y + (br.height / 2);
					int Width = br.width;
					int Area = br.width * br.height;

					if (Width > bestWidth) {
						bestX = cogX;
						bestY = cogY;
						bestWidth = Width;
						bestArea = Area;
					}
					// draw center of gravity markers
					Imgproc.drawMarker(mat, new Point(cogX, cogY), new Scalar(255, 255, 255), Imgproc.MARKER_CROSS, 10,
							2, 1);
					Imgproc.drawMarker(mat, new Point(cogX, cogY), new Scalar(140, 140, 140), Imgproc.MARKER_CROSS, 5,
							1, 1);
					// debug output
					// System.out.printf("\n[Vision] (x, y) (%d, %d) (w, h) (%d, %d) (cog x, y) (%d,
					// %d)\n", br.x, br.y, br.width, br.height, cogX, cogY);
				}

				targetInfoB.set(bestX, bestY, bestWidth, bestArea);

			} // end of processing all contours in this camera frame

			// wrap up this camera frame

			Main.sendMessage.Communicate("Elevator " + targetInfoB.toJson()); // UDP to a receiver who wants this data

			// System.out.println("Elevator " + targetInfoB.toJson());
			// Bumper {"COGX":-1.0,"COGY":-1.0,"Width":0.0,"Area":0.0,"Fresh":true}
			// Elevator {"COGX":82.0,"COGY":60.0,"Width":147.0,"Area":17640.0,"Fresh":true}

			// save image for the merge process
			Main.elevatorPipeline.setImage(mat);

			// Give the output stream a new image to display
			outputStream.putFrame(mat);

			if (Main.logImage)
				try {
					//System.out.println("Image file setup " + time.get());
					String filename = String.format("/mnt/usb/E/%06d.jpg", FrameNumber);
					final File file = new File(filename);
					filename = file.toString();
					//System.out.println("Start imwrite " + time.get());
					if(!Imgcodecs.imwrite(filename, mat)) System.out.println("Error writing E");
					//System.out.println("End imwrite " + time.get());
				} catch (Exception e) {
					System.out.println("error saving image file " + e.toString());
				}

			// print statistics about this frame
			// System.out.println("[Vision] End Camera Frame Loop Elapsed time = " +
			// (time.get()-startTime));
			System.out.println("Elevator " + 1. / (time.get() - startTime) + " fps");
			// System.out.println("Free memory " + Runtime.getRuntime().freeMemory() + "
			// Total memory " + Runtime.getRuntime().totalMemory() + " Max memory " +
			// Runtime.getRuntime().maxMemory());

		} // end of this camera frame

		// Interrupted thread so end the camera frame grab with one last targetInfoB of
		// no data
		targetInfoB.set(-1., -1., 0., 0.);
		mat.release();
		// System.out.println("[Vision] Camera Frame Grab Interrupted and Ended
		// Thread");

	} // end of visionPipeline method

} // end of class camera_process
