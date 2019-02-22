/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * This class is used to select the target from the camera frame. The user MUST MODIFY the process() method. The user
 * must create a new GripPipeline class using GRIP, modify the TargetData class, and modify this class.
 * 
 * @author FRC Team 4237
 * @version 2019.01.28.14.20
 */
public class TargetSelection
{
	private String id;
	// This object is used to run the GripPipeline
	private GripPipelineYellowCube gripPipelineYellowCube = new GripPipelineYellowCube();

	// This field is used to determine if debugging information should be displayed.
	private boolean debuggingEnabled = false;

	TargetSelection(String id)
	{
		this.id = id;
	}

	/**
	 * This method sets the field to display debugging information.
	 * 
	 * @param enabled Set to true to display debugging information.
	 */
	public void setDebuggingEnabled(boolean enabled)
	{
		debuggingEnabled = enabled;
	}

	/**
	 * This method is used to select the next target. The user MUST MODIFY this method.
	 * 
	 * @param mat            The camera frame containing the image to process.
	 * @param nextTargetData The target data found in the camera frame.
	 */
	public void process(Mat mat, TargetData nextTargetData)
	{
		// Let the GripPipeline filter through the camera frame
		gripPipelineYellowCube.process(mat);

		// The GripPipeline creates an array of contours that must be searched to find the target.
		ArrayList<MatOfPoint> filteredContours;
		filteredContours = new ArrayList<MatOfPoint>(gripPipelineYellowCube.filterContoursOutput());

		// Check if no contours were found in the camera frame.
		if (filteredContours.isEmpty())
		{
			// Indicate that no target was found.
			nextTargetData.isTargetFound = false;

			if (debuggingEnabled)
			{
				System.out.println("[TargetSelection " + id + "] No Contours");

				// Display a message if no contours are found.
				Imgproc.putText(mat, "No Contours", new Point(20, 20), Core.FONT_HERSHEY_SIMPLEX, 0.25,
						new Scalar(0, 0, 0), 1);
			}

		}
		else // if contours were found ...
		{
			// Indicate that a target is found.
			nextTargetData.isTargetFound = true;

			if (debuggingEnabled)
			{
				System.out.println("[TargetSelection " + id + "] " + filteredContours.size() + " contours");

				// Draw all contours at once (negative index).
				// Positive thickness means not filled, negative thickness means filled.
				Imgproc.drawContours(mat, filteredContours, -1, new Scalar(255, 255, 255), 2);
			}

			// These variables are used to put a bounding rectangle around the contour and then calculate the center of
			// gravity in the horizontal (x) and vertical (y) direction.
			Rect boundRect;
			int cogX;
			int cogY;

			// Iterate through all of the contours, one at a time.
			for (MatOfPoint contour : filteredContours)
			{
				// Create a bounding rectangle around each contour.
				boundRect = Imgproc.boundingRect(contour);

				// Calculate the center of gravity for each contour.
				cogX = boundRect.x + (boundRect.width / 2);
				cogY = boundRect.y + (boundRect.height / 2);

				// Check if this contour is the best target data so far.
				if (boundRect.width > nextTargetData.width)
				{
					// Store the target data
					nextTargetData.cogX = cogX;
					nextTargetData.cogY = cogY;
					nextTargetData.width = boundRect.width;
					nextTargetData.area = boundRect.width * boundRect.height;
				}

				if (debuggingEnabled)
				{
					System.out.println("[TargetSelection " + id + "] " + contour.size() + " points in contour");

					// Draw marks at the center of gravity.
					Imgproc.drawMarker(mat, new Point(cogX, cogY), new Scalar(255, 255, 255), Imgproc.MARKER_CROSS, 10,
							2, 1);
					Imgproc.drawMarker(mat, new Point(cogX, cogY), new Scalar(140, 140, 140), Imgproc.MARKER_CROSS, 5,
							1, 1);
				}
			}
		}
	}
}
