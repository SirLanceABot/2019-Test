import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.wpilibj.Timer;

/**
 * This class creates a camera thread to process camera frames. DO NOT MODIFY
 * this class. The user must create a new GripPipeline class using GRIP, modify
 * the TargetData class, and modify the TargetSelection class.
 * 
 * @author FRC Team 4237
 * @version 2019.01.28.14.20
 */
public class CameraProcessE implements Runnable
{
	private static final String pId = new String("[ECameraProcess]");

	private String cameraName = "Elevator Camera";
	private int cameraWidth = 320;
	private int cameraHeight = 240;
	private PipelineProcessE pipelineProcessE = new PipelineProcessE(this);
	private Thread pipeline;

	// This object is used to capture frames from the camera.
	// The captured image is stored to a Mat
	private CvSink inputStream;

	// This object is used to store the camera frame returned from the inputStream
	// Mats require a lot of memory. Placing this in a loop will cause an 'out of
	// memory' error.
	protected Images cameraFrame = new Images();
	private Mat cameraFrameTemp = new Mat(240, 320, CvType.CV_8UC3);

	// This field is used to determine if debugging information should be displayed.
	// Use the setDebuggingEnabled() method to set this value.
	private boolean debuggingEnabled = false;

	// These fields are used to set the camera resolution and camera name.
	// Use the set...() method to set these values.

	private VideoSource camera;

	public CameraProcessE(VideoSource camera)
	{
		this.camera = camera;
	}

	/**
	 * This method sets the field to display debugging information.
	 * 
	 * @param enabled
	 *                    Set to true to display debugging information.
	 */
	public void setDebuggingEnabled(boolean enabled)
	{
		debuggingEnabled = enabled;
		pipelineProcessE.setDebuggingEnabled(enabled);
	}

	/**
	 * This method sets the camera resolution.
	 */
	public void setCameraResolution(int width, int height)
	{
		cameraWidth = width;
		cameraHeight = height;
	}

	/**
	 * This method sets the camera name.
	 */
	public void setCameraName(String name)
	{
		cameraName = name;
	}

	// TODO: write the method
	public void setExposure(int exposure)
	{

	}

	// TODO: write the method
	public void setAutoExposure(boolean enabled)
	{

	}

	public void run()
	{
		// This variable will be used to time each iteration of the thread loop.
		double loopTotalTime = -999.0;

		// Set up the input stream to get frames from the camera.
		// inputStream = CameraServer.getInstance().getVideo();
		inputStream = new CvSink("cvsink");
		inputStream.setSource(camera);

		pipelineProcessE = new PipelineProcessE(this);
		pipeline = new Thread(pipelineProcessE, "4237Epipeline");
		pipeline.start();

        this.setDebuggingEnabled(Main.debug);

		// This is the thread loop. It can be stopped by calling the interrupt() method.
		while (!Thread.interrupted())
		{
			if (debuggingEnabled)
			{
				loopTotalTime = Timer.getFPGATimestamp();
			}
			
			// Tell the input stream to grab a frame from the camera and store it to the
			// mat.
			// Check if there was an error with the frame grab.
			if (inputStream.grabFrame(cameraFrameTemp) == 0)
			{
				System.out.println(pId + " grabFrame error " + inputStream.getError());
				cameraFrameTemp.setTo(new Scalar(100, 100, 100));
			}

			this.cameraFrame.setImage(cameraFrameTemp);

			if (debuggingEnabled)
			{
				loopTotalTime = Timer.getFPGATimestamp() - loopTotalTime;
				System.out.format("%s %6.2f FPS, loop/camera time %5.3f\n", pId, 1.0 / loopTotalTime, loopTotalTime);
			}
		} // End of the thread loop

			System.out.println(pId + " Camera Frame Grab Interrupted and Ended Thread");
	}
}
