import java.util.ArrayList;
import java.io.File;
//import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
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

class camera_process implements Runnable {

	private TargetInfo targetInfo = new TargetInfo();
	VideoSource camera;

	camera_process(VideoSource aCamera) {
		camera = aCamera;
	}

	public void getTargetInfo(TargetInfo aTargetInfo) {
		targetInfo.get(aTargetInfo);
	}

	public void run() {
		visionPipeline();
	}

	private void visionPipeline() {
		// System.runFinalization();
		// System.gc();

		GripPipelineCube gripperCube = new GripPipelineCube();

		// initialize target data from this camera frame
		targetInfo.set(-1., -1., 0., 0.);

		// Get a CvSink. This will capture Mats from the camera
		CvSink cvSink = new CvSink("cvsink");
		cvSink.setSource(camera);

		// Setup a CvSource. This will send images back to the Dashboard WPI style
		// CvSource outputStream = CameraServer.getInstance().putVideo("RectangleB",
		// 320, 240);

		CvSource outputStream = new CvSource("Bumper", VideoMode.PixelFormat.kMJPEG, 160, 120, 30);
		// CvSource cvsource = new CvSource("cvsource", VideoMode.PixelFormat.kMJPEG,
		// width, height, frames_per_sec);

		MjpegServer mjpegServer2 = new MjpegServer("serve_Bumper", 1183);

		mjpegServer2.setSource(outputStream);
		// Mats are very memory expensive. Lets reuse this Mat.
		// all Java Mats have to be "pre-allocated" - they can't be in the loop because
		// they are never released and will cause out of memory
		Mat mat = new Mat();
		Mat zeros = Mat.zeros(60, 160, CvType.CV_8UC3);
		Timer time = new Timer();
		time.start();
		int FrameNumber = 0;

		while (!Thread.interrupted()) // This lets the robot stop this thread
		{
			double startTime = time.get();
			// System.out.println("\n[Vision] Starting camera frame grab at Time = " +
			// startTime);

			// Tell the CvSink to grab a frame from the camera and put it
			// in the source mat. If there is an error notify the output.
			if (cvSink.grabFrame(mat) == 0) {
				// Send the output the error.
				outputStream.notifyError(cvSink.getError());
				// skip the rest of the current iteration
				continue;
			}

			// try {
			// 	String filename = String.format("/mnt/usb/BR%06d.jpg", ++FrameNumber);
			// 	final File file = new File(filename);
			// 	filename = file.toString();
			// 	Imgcodecs.imwrite(filename, mat);
			// } catch (Exception e) {
			// 	System.out.println(e.toString());
			// }

			// Run the GRIP image processing pipeline to find all contours each of which are
			// a series of points (x, y).
			// The assumption is each contour represents the point of a light on a string of
			// lights.
			// Each contour will then be reduced to a bounding upright rectangle with a
			// center of gravity (x, y) - the light.
			// The set of all centers of gravity are used in the RANSAC to cluster the
			// points in straight lines - the string of lights.

			ArrayList<MatOfPoint> contoursFiltered;

			// System.out.println("[Vision] CUBE ");

			// zeros.copyTo(mat.submat(0, 120, 0, 320)); // zero top half of image

			// Imgproc.rectangle(mat, new Point(0,0), new Point(320,120), new Scalar(0, 0,
			// 0), -1); //alternate way to make a mask

			gripperCube.process(mat);
			contoursFiltered = new ArrayList<MatOfPoint>(gripperCube.filterContoursOutput());

			if (contoursFiltered.isEmpty()) { // no contours in this frame
												// debug output
												// System.out.println("No GRIP Contours");
				Imgproc.putText(mat, "No Contours", new Point(50, 22), Core.FONT_HERSHEY_SIMPLEX, 1.,
						new Scalar(255, 255, 255), 2);
				targetInfo.set(-1., -1., 0., 0.);
			} else { // process the contours
						// debug output
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

				targetInfo.set(bestX, bestY, bestWidth, bestArea);

			} // end of processing all contours in this camera frame

			// wrap up this camera frame

			//System.out.println("Bumper " + targetInfo.toJson());
			// Bumper {"COGX":-1.0,"COGY":-1.0,"Width":0.0,"Area":0.0,"Fresh":true}
			// Elevator {"COGX":82.0,"COGY":60.0,"Width":147.0,"Area":17640.0,"Fresh":true}

			// Give the output stream a new image to display
			outputStream.putFrame(mat);

			// try {
			// 	String filename = String.format("/mnt/usb/B%06d.jpg", FrameNumber);
			// 	final File file = new File(filename);
			// 	filename = file.toString();
			// 	Imgcodecs.imwrite(filename, mat);
			// } catch (Exception e) {
			// 	System.out.println(e.toString());
			// }

			// print statistics about this frame
			//System.out.println("[Vision] End Camera Frame Loop Elapsed time = " + (time.get()-startTime));
			System.out.println("Bumper " + 1./(time.get()-startTime) + " fps");
			// System.out.println("Free memory " + Runtime.getRuntime().freeMemory() + "
			// Total memory " + Runtime.getRuntime().totalMemory() + " Max memory " +
			// Runtime.getRuntime().maxMemory());

		} // end of this camera frame

		// Interrupted thread so end the camera frame grab with one last targetInfo of
		// no data
		targetInfo.set(-1., -1., 0., 0.);
		mat.release();
		zeros.release();
		System.out.println("[Vision] Camera Frame Grab Interrupted and Ended Thread");

	} // end of visionPipeline method

} // end of class camera_process

//// 320 width = 320 cols; 240 height = 240 rows
//// Mat rows, cols =240 320
//// bottom half start row 120 end row 240 start col 0 end col 320
//// Mat maskOffTop = new Mat(240, 320, CvType.CV_8UC1, new Scalar(0));
//// Mat.ones(120, 320, CvType.CV_8UC1).copyTo(maskOffTop.submat(120, 240, 0,
//// 320));
// Mat maskOffTop = new Mat(24, 32, CvType.CV_8UC1, new Scalar(0));
// Mat.ones(12, 32, CvType.CV_8UC1).copyTo(maskOffTop.submat(12, 24, 0, 32));
// byte[] aRow = new byte[32];
//
// for(int row = 0;row <24; row++)
// {
// maskOffTop.get(row, 0, aRow);
// for (int col = 0; col < 32; col++)
// {
// System.out.print(aRow[col] + " ");
// }
// System.out.println();
// }
/// *
// */
//
//// maskOffTop.dump(); dump does nothing
//// System.out.println(maskOffTop);
// Mat.zeros(120, 320, CvType.CV_8UC3).copyTo(mat.submat(0, 120, 0, 320));

// {
// byte[] aRow = new byte[320*3];
//
// for(int row = 0;row <1/*240*/; row++)
// {
// mat.get(row, 0, aRow);
// for (int col = 0; col < 320*3; col++)
// {
// System.out.print(aRow[col] + " ");
// }
// System.out.println();
// }
// }
//
// Imgproc.rectangle(mat, new Point(0,0), new Point(320,120), new Scalar(0, 0,
// 0), -1);
//
// {
// byte[] aRow = new byte[320*3];
//
// for(int row = 0;row <1/*240*/; row++)
// {
// mat.get(row, 0, aRow);
// for (int col = 0; col < 320*3; col++)
// {
// System.out.print(aRow[col] + " ");
// }
// System.out.println();
// }
// }
