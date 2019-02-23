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
public class CameraProcess implements Runnable
{
	private String cameraName = "Bumper Camera";
	private int cameraWidth = 160;
	private int cameraHeight = 120;
	private PipelineProcess pipelineProcess = new PipelineProcess(this);
	private Thread pipeline;

	// This object is used to capture frames from the camera.
	// The captured image is stored to a Mat
	private CvSink inputStream;

	// This object is used to store the camera frame returned from the inputStream
	// Mats require a lot of memory. Placing this in a loop will cause an 'out of
	// memory' error.
	//protected Mat cameraFrame = new Mat();
	protected Images cameraFrame = new Images();
	protected boolean isFreshImage = false;
	private Mat cameraFrameTemp = new Mat(120, 160, CvType.CV_8UC3);

	// This object is used to track the time of each iteration of the thread loop.
	private Timer timer = new Timer();

	// This field is used to determine if debugging information should be displayed.
	// Use the setDebuggingEnabled() method to set this value.
	private boolean debuggingEnabled = true;

	// These fields are used to set the camera resolution and camera name.
	// Use the set...() method to set these values.

	private VideoSource camera;

	public CameraProcess(VideoSource camera)
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
		pipelineProcess.setDebuggingEnabled(enabled);
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
		double loopStartTime = -999.0;
		double loopCameraTime = -999.0;

		// Set up the input stream to get frames from the camera.
		// inputStream = CameraServer.getInstance().getVideo();
		inputStream = new CvSink("cvsink");
		inputStream.setSource(camera);

		System.out.println("[CameraProcess] Starting Bumper pipeline");

		pipelineProcess = new PipelineProcess(this);
		pipeline = new Thread(pipelineProcess, "4237BumperPipeline");
		pipeline.start();
		try
		{
			Thread.sleep(3000);
		} catch (Exception e)
		{
		}

		this.setDebuggingEnabled(true);

		// Reset and start the timer to time each iteration of the thread loop.
		timer.reset();
		timer.start();

		// This is the thread loop. It can be stopped by calling the interrupt() method.
		while (!Thread.interrupted())
		{
			if (debuggingEnabled)
			{
				loopStartTime = timer.get();
				// System.out.println("[CameraProcess] Loop Thread Start Time = " +
				// loopStartTime);
			}

			// Tell the input stream to grab a frame from the camera and store it to the
			// mat.
			// Check if there was an error with the frame grab.
			loopCameraTime = timer.get();
			if (inputStream.grabFrame(cameraFrameTemp) == 0)
			{
				System.out.println("[CameraProcess] grabFrame error " + inputStream.getError());
				cameraFrameTemp.setTo(new Scalar(100, 100, 100));
			}

			// synchronized (this.cameraFrame)
			// {
			// 	cameraFrameTemp.copyTo(this.cameraFrame);
			// 	this.isFreshImage = true;
			// 	this.cameraFrame.notify();
			// }
			this.cameraFrame.setImage(cameraFrameTemp);
			loopCameraTime = timer.get() - loopCameraTime;

			if (debuggingEnabled)
			{
				double loopTime = timer.get() - loopStartTime;
				System.out.println("[CameraProcess] " + 1.0 / loopTime + " FPS, total time " + loopTime
						+ ", camera time " + loopCameraTime);
			}
		} // End of the thread loop

		if (debuggingEnabled)
		{
			System.out.println("[CameraProcess] Camera Frame Grab Interrupted and Ended Thread");
		}
	}
}
