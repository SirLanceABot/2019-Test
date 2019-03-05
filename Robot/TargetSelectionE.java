import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * This class is used to select the target from the camera frame. The user MUST
 * MODIFY the process() method. The user must create a new GripPipeline class
 * using GRIP, modify the TargetData class, and modify this class.
 * 
 * @author FRC Team 4237
 * @version 2019.01.28.14.20
 */
public class TargetSelectionE
{
	private static final String pId = new String("[TargetSelectionE]");

	// This object is used to run the GripPipeline
	private GripPipeline gripPipeline = new GripPipeline();

	// This field is used to determine if debugging information should be displayed.
	private boolean debuggingEnabled = false;

	TargetSelectionE()
	{
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
	}

	/**
	 * This method is used to select the next target. The user MUST MODIFY this
	 * method.
	 * 
	 * @param mat
	 *                           The camera frame containing the image to process.
	 * @param nextTargetData
	 *                           The target data found in the camera frame.
	 */
	public void process(Mat mat, TargetDataE nextTargetData)
	{
		double centerTarget;
		double distanceTarget = -99999.;

		// Let the GripPipeline filter through the camera frame
		gripPipeline.process(mat);

		MatOfPoint2f contour2;
        RotatedRect box;
        RotatedRect lBox = new RotatedRect(new Point (0,0) ,new Size(0,0), 0);
        RotatedRect rBox = new RotatedRect(new Point (0,0) ,new Size(0,0), 0);
		double angle;
		
		// The GripPipeline creates an array of contours that must be searched to find
		// the target.
		ArrayList<MatOfPoint> filteredContours;
		filteredContours = new ArrayList<MatOfPoint>(gripPipeline.filterContoursOutput());

	// Check if no contours were found in the camera frame.
		if (filteredContours.isEmpty())
		{
			if (debuggingEnabled)
			{
				System.out.println(pId + " No Contours");

				// Display a message if no contours are found.
				Imgproc.putText(mat, "No Contours", new Point(20, 20), Core.FONT_HERSHEY_SIMPLEX, 0.25,
						new Scalar(0, 0, 0), 1);
			}

		}
		else // if contours were found ...
		{
			if (debuggingEnabled)
			{
				System.out.println(pId + " " + filteredContours.size() + " contours");

				// Draw all contours at once (negative index).
				// Positive thickness means not filled, negative thickness means filled.
				Imgproc.drawContours(mat, filteredContours, -1, new Scalar(255, 255, 255), 2);
			}

        for(MatOfPoint contour : filteredContours)
        {
            contour2 = new MatOfPoint2f(contour.toArray());
            box = org.opencv.imgproc.Imgproc.minAreaRect(contour2);
            // System.out.println(box.angle);
            if(box.size.width < box.size.height)
            {
                angle = box.angle + 180;
            }
            else
            {
                angle = box.angle + 90;
            }
            // System.out.println(box.angle);
            // if(angle < 0)
            // {
            //     angle += 360;
            // }
            // if((angle < 20 && angle > 5) && (box.size.area() > lBox.size.area()))
            if((angle >5 && angle < 80) && (box.size.area() > lBox.size.area()))
            {
                lBox = box;
                lBox.angle = angle;
            }
            else if((angle < 175 && angle > 100) && (box.size.area() > rBox.size.area()))
            {
                rBox = box;
                rBox.angle = angle;
            }
		}
	}

	// decide what to do with the best boxes that were found
        if(lBox.size.area() > 0 && rBox.size.area() > 0)
        {
            //if one box is more than 20% larger get rid of smaller box
            if(Math.abs((lBox.size.area() / rBox.size.area()) - 1) > .2)
            {
                if(lBox.size.area() > rBox.size.area())
                {
                    rBox = new RotatedRect(new Point (0,0) ,new Size(0,0), 0);
                }
                else
                {
                    lBox = new RotatedRect(new Point (0,0) ,new Size(0,0), 0);   
                }
            }
        }

        if((lBox.size.area() > 0 && rBox.size.area() > 0))
        {
            if(rBox.center.x > lBox.center.x)
            {
                System.out.printf(pId + " Two pieces\tLeft: %.2f\tRight: %.2f\tReturn:%.2f\n", lBox.angle, rBox.angle,((((rBox.center.x + lBox.center.x)/ 2.0) - (mat.width() / 2)) / (mat.width() / 2)));
                centerTarget = ((((rBox.center.x + lBox.center.x)/ 2.0) - (mat.width() / 2)) / (mat.width() / 2));
            }
            else
            {
                if(lBox.size.area() > rBox.size.area())
                {
                    System.out.printf(pId + " Two pieces\tLeft: %.2f\n", lBox.angle);
                    centerTarget = -1;
                }
                else
                {
                    System.out.printf(pId + " Two pieces\tRight: %.2f\n", rBox.angle);
                    centerTarget = 1;
                }
            }
        }
        else if(rBox.size.area() > 0)
        {
            System.out.printf(pId + " Right: %.2f\n", rBox.angle);
            centerTarget = 1;
        }
        else if(lBox.size.area() > 0)
        {
            System.out.printf(pId + " Left: %.2f\n", lBox.angle);
            centerTarget = -1;
        }
        
        else centerTarget = 5;

		//Update the target
		nextTargetData.center = centerTarget;
		nextTargetData.distance = distanceTarget;
		nextTargetData.isFreshData = true;
		//nextTargetData.isTargetFound = true;
// TODO fix is target found and distance

		// // The GripPipeline creates an array of contours that must be searched to find
		// // the target.
		// ArrayList<MatOfPoint> filteredContours;
		// filteredContours = new ArrayList<MatOfPoint>(gripPipelineYellowCube.filterContoursOutput());

		// // Check if no contours were found in the camera frame.
		// if (filteredContours.isEmpty())
		// {
		// 	if (debuggingEnabled)
		// 	{
		// 		System.out.println(pId + " No Contours");

		// 		// Display a message if no contours are found.
		// 		Imgproc.putText(mat, "No Contours", new Point(20, 20), Core.FONT_HERSHEY_SIMPLEX, 0.25,
		// 				new Scalar(0, 0, 0), 1);
		// 	}

		// }
		// else // if contours were found ...
		// {

		// 	if (debuggingEnabled)
		// 	{
		// 		System.out.println(pId + " " + filteredContours.size() + " contours");

		// 		// Draw all contours at once (negative index).
		// 		// Positive thickness means not filled, negative thickness means filled.
		// 		Imgproc.drawContours(mat, filteredContours, -1, new Scalar(255, 255, 255), 2);
		// 	}

		// 	// These variables are used to put a bounding rectangle around the contour and
		// 	// then calculate the center of
		// 	// gravity in the horizontal (x) and vertical (y) direction.
		// 	Rect boundRect;
		// 	int cogX;
		// 	int cogY;

		// 	// Iterate through all of the contours, one at a time.
		// 	for (MatOfPoint contour : filteredContours)
		// 	{
		// 		// Create a bounding rectangle around each contour.
		// 		boundRect = Imgproc.boundingRect(contour);

		// 		// Calculate the center of gravity for each contour.
		// 		cogX = boundRect.x + (boundRect.width / 2);
		// 		cogY = boundRect.y + (boundRect.height / 2);

		// 		// Check if this contour is the best target data so far.
		// 		if (boundRect.width > nextTargetData.width)
		// 		{
		// 			// Store the target data
		// 			nextTargetData.cogX = cogX;
		// 			nextTargetData.cogY = cogY;
		// 			nextTargetData.width = boundRect.width;
		// 			nextTargetData.area = boundRect.width * boundRect.height;
		// 		}

		// 		if (debuggingEnabled)
		// 		{
		// 			//System.out.println(pId + " " + contour.size() + " points in contour");

		// 			// Draw marks at the center of gravity.
		// 			Imgproc.drawMarker(mat, new Point(cogX, cogY), new Scalar(255, 255, 255), Imgproc.MARKER_CROSS, 10,
		// 					2, 1);
		// 			Imgproc.drawMarker(mat, new Point(cogX, cogY), new Scalar(140, 140, 140), Imgproc.MARKER_CROSS, 5,
		// 					1, 1);
		// 		}
		// 	}
		// 	// Indicate that a target is found.
		// 	nextTargetData.isTargetFound = true;
		// }
	}
}
