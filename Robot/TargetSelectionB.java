import java.util.ArrayList;
import java.lang.Math;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;

/**
 * This class is used to select the target from the camera frame. The user MUST
 * MODIFY the process() method. The user must create a new GripPipeline class
 * using GRIP, modify the TargetData class, and modify this class.
 * 
 * @author FRC Team 4237
 * @version 2019.01.28.14.20
 */
public class TargetSelectionB
{
    private static final String pId = new String("[TargetSelectionB]");

	// This object is used to run the GripPipeline
   private WhiteLineVision gripPipelineWhiteTape = new WhiteLineVision();

	// This field is used to determine if debugging information should be displayed.
	private boolean debuggingEnabled = false;

	TargetSelectionB()
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

    // int heightOfMask = 76;
   
    // public int getHeightOfMask()
    // {
    //     return heightOfMask;
    // }

    // public void setHeightOfMask(int newHeight)
    // {
    //     heightOfMask = newHeight;
    // }

    /**
	 * This method is used to select the next target. The user MUST MODIFY this
	 * method.
	 * 
	 * @param mat
	 *                           The camera frame containing the image to process.
	 * @param nextTargetData
	 *                           The target data found in the camera frame.
	 */
     public void process(Mat mat, TargetDataB nextTargetData)
    {
        //int tapeDistance = Main.obj.tapeDistance.get();
        RotatedRect boundRect;

        // // Mask off the top of the screen
        // Mat mask = new Mat(mat.rows(), mat.cols(), CvType.CV_8U, Scalar.all(0)); // Create mask with the size of the source image
        // Imgproc.rectangle(mask, new Point(0.0,heightOfMask+1.0), new Point(mat.cols(),mat.rows()), new Scalar(255, 255, 255), -1);

  		// Let the GripPipeline filter through the camera frame
        gripPipelineWhiteTape.process(mat);

        // The GripPipeline creates an array of contours that must be searched to find
        // the target.
        ArrayList<MatOfPoint> filteredContours;
        filteredContours = new ArrayList<MatOfPoint>(gripPipelineWhiteTape.filterContoursOutput());

        // gripPipelineWhiteTape.maskOutput();

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
				Imgproc.drawContours(mat, filteredContours, -1, new Scalar(255, 0, 0), 1);
			}

            // Loop thru all contours to find the best contour
            // Each contour is reduced to a single point - the COG x, y of the contour
            
            int contourIndex = -1;
            int bestContourIndex = -1;
            Point endpoint = new Point();

            for (MatOfPoint contour : filteredContours)
            {
                contourIndex++;

                // Create a bounding upright rectangle for the contour's points
                MatOfPoint2f  NewMtx = new MatOfPoint2f(contour.toArray() );
                boundRect = Imgproc.minAreaRect(NewMtx);

                // Draw a rotatedRect, using lines, that represents the minAreaRect
                Point boxPts[] = new Point[4];
                boundRect.points(boxPts);
                for(int i = 0; i<4; i++)
                {
                    Imgproc.line(mat, boxPts[i], boxPts[(i+1)%4], new Scalar(0, 255, 255));
                }

                // Determine if this is the best contour using center.y

                if (nextTargetData.center.y < boundRect.center.y)
                {
                    bestContourIndex = contourIndex;

                    // Find the center x, center y, width, height, and angle of the bounding rectangle
                    nextTargetData.center = boundRect.center;
                    nextTargetData.size = boundRect.size;
                    nextTargetData.angle = boundRect.angle;

                    // Create an accurate angle
                    if(Math.abs((int)(nextTargetData.size.width - nextTargetData.size.height)) <= 5)
                    {
                        nextTargetData.fixedAngle = 90.0;
                    }
                    else if(nextTargetData.size.width < nextTargetData.size.height)
                    {
                        nextTargetData.fixedAngle = nextTargetData.angle + 90;
                    }
                    else
                    {
                        nextTargetData.fixedAngle = nextTargetData.angle + 180;
                    }

                    //Update the target
                    nextTargetData.isFreshData = true;
                    nextTargetData.isTargetFound = true;
                }
            }

        Imgproc.drawContours(mat, filteredContours, bestContourIndex, new Scalar(0, 0, 255), 1);

        double angleInRadians = nextTargetData.fixedAngle * (Math.PI/180);
        endpoint.x = nextTargetData.center.x - ( (nextTargetData.size.height / 2) * Math.cos(angleInRadians) );
        endpoint.y = nextTargetData.center.y - ( (nextTargetData.size.height / 2) * Math.sin(angleInRadians) );
        Imgproc.line(mat, nextTargetData.center, endpoint,  new Scalar(255, 0, 255), 1);

        // // Print the points of the best contour
        // for(Point aPoint : filteredContours.get(bestContourIndex).toArray())System.out.print(" " + aPoint);
        // System.out.println("");

        // // Count the number of points in a contour
        // System.out.println("Number of points in best contour: " + filteredContours.get(bestContourIndex).toArray().length);

        } // end of processing all contours in this camera frame
            

        /*
        // Slope line
        if (nextTargetData.isFreshData == true && nextTargetData.isTargetFound == true)
        {
            Point endpoint = new Point();
            double angleInRadians = nextTargetData.angle * (Math.PI/180);
            endpoint.x = nextTargetData.center.x + ( (nextTargetData.size.height / 2) * Math.cos(angleInRadians) );
            endpoint.y = nextTargetData.center.y + ( (nextTargetData.size.height / 2) * Math.sin(angleInRadians) );
            Imgproc.line(mat, nextTargetData.center, endpoint,  new Scalar(255, 0, 255), 1);
            
            nextTargetData.slope = (endpoint.y-nextTargetData.center.y)/(endpoint.x-nextTargetData.center.x);
        }
        */
        }
    }
