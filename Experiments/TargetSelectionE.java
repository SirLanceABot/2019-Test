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
		if(true) return; // bailout now without running anything
		
		double centerTarget = 5;
		int distanceTarget = Integer.MIN_VALUE;
		boolean isTargetFoundLocal = true;
		// Let the GripPipeline filter through the camera frame
		gripPipeline.process(mat);

		MatOfPoint2f contour1;
		MatOfPoint2f contour2;
        RotatedRect box;
		RotatedRect box2;
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
			Imgproc.drawContours(mat, filteredContours, -1, new Scalar(255, 0, 0), 1);
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
            if((angle >5 && angle < 45) && (box.size.area() > lBox.size.area()))
            {
                lBox = box;
                lBox.angle = angle;
            }
            else if((angle < 175 && angle > 135) && (box.size.area() > rBox.size.area()))
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
                if (debuggingEnabled)
                {
                    System.out.printf(pId + " Two pieces\tLeft: %.2f\tRight: %.2f\tReturn:%.2f\n",
                     lBox.angle, rBox.angle,((((rBox.center.x + lBox.center.x)/ 2.0) - (mat.width() / 2)) / (mat.width() / 2)));
                }
                centerTarget = ((((rBox.center.x + lBox.center.x)/ 2.0) - (mat.width() / 2)) / (mat.width() / 2));
				distanceTarget = (int)(rBox.center.x - lBox.center.x);
			}
            else
            {
                if(lBox.size.area() > rBox.size.area())
                {
                    if (debuggingEnabled)
                    {
                        System.out.printf(pId + " Two pieces\tLeft: %.2f\n", lBox.angle);
                    }
                    centerTarget = -1;
                }
                else
                {
                    if (debuggingEnabled)
                    {
                        System.out.printf(pId + " Two pieces\tRight: %.2f\n", rBox.angle);
                    }
                    centerTarget = 1;
                }
            }
        }
        else if(rBox.size.area() > 0)
        {
            if (debuggingEnabled)
			{
                System.out.printf(pId + " Right: %.2f\n", rBox.angle);
            }
            centerTarget = 1;
        }
        else if(lBox.size.area() > 0)
        {
            if (debuggingEnabled)
			{
                System.out.printf(pId + " Left: %.2f\n", lBox.angle);
            }
            centerTarget = -1;
        }
		else if(filteredContours.size() >= 2)
		{
			contour2 = new MatOfPoint2f(filteredContours.get(1).toArray());
			contour1 = new MatOfPoint2f(filteredContours.get(0).toArray());
			box2 = org.opencv.imgproc.Imgproc.minAreaRect(contour2);
			box = org.opencv.imgproc.Imgproc.minAreaRect(contour1);
			if(Math.min(box.center.y, box2.center.y) / Math.max(box.center.y, box2.center.y) > .8)
			{
				centerTarget = ((((box2.center.x + box.center.x)/ 2.0) - (mat.width() / 2)) / (mat.width() / 2));
				distanceTarget = (int)(Math.max(box.center.x,box2.center.x) - Math.min(box2.center.x,box.center.x));
			}
			else isTargetFoundLocal = false;
		}
        else isTargetFoundLocal = false;

		//Update the target
		nextTargetData.center = centerTarget;
		nextTargetData.distance = distanceTarget;

		if (debuggingEnabled)
		{
			System.out.println("Distance: " + distanceTarget);
		}

		Main.obj.tapeDistance.set(distanceTarget);
		nextTargetData.isFreshData = true;
		nextTargetData.isTargetFound = isTargetFoundLocal;
	}
}
