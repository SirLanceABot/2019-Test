
/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import com.google.gson.Gson;

/**
 * This class is used to store the target data. The user MUST MODIFY the
 * process() method. The user must create a new GripPipeline class using GRIP,
 * modify the TargetSelection class, and modify this class.
 * 
 * @author FRC Team 4237
 * @version 2019.01.28.14.20
 */
public class TargetData
{
    // NOTE: No modifier means visible to both the class and package.

    // The user MUST MODIFY these following fields.
    // --------------------------------------------------------------------------
    // Target data of the bounding rectangle around the contour
    int cogX; // Horizontal (X) center of gravity of the rectangle
    int cogY; // Vertical (Y) center of gravity of the rectangle
    int width; // Width of the rectangle
    int area; // Area of the rectangle
    boolean isTargetFound; // Was a target found?
    // --------------------------------------------------------------------------

    // These fields are used to track the validity of the data.
    int frameNumber = 1; // Number of the camera frame
    boolean isFreshData; // Is the data fresh?

    /**
     * Default contructor - resets all of the target data.
     */
    public TargetData()
    {
        reset();
    }

    /**
     * This method resets all of the target data, except the frameNumber. The user
     * MUST MODIFY
     */
    void reset()
    {
        cogX = -1;
        cogY = -1;
        width = -1;
        area = -1;
        isTargetFound = false;

        // DO NOT reset the frameNumber
        isFreshData = true;
    }

    /**
     * This method stores all of the target data.
     * 
     * @param targetData
     *                       The new target data to store.
     */
    void set(TargetData targetData)
    {
        cogX = targetData.cogX;
        cogY = targetData.cogY;
        width = targetData.width;
        area = targetData.area;
        isTargetFound = targetData.isTargetFound;

        // DO NOT MODIFY these values.
        frameNumber++;
        isFreshData = true;
     }

    /**
     * This method returns all of the target data.
     * 
     * @return The target data.
     */
    TargetData get()
    {
        TargetData targetData = new TargetData();

        targetData.cogX = cogX;
        targetData.cogY = cogY;
        targetData.width = width;
        targetData.area = area;
        targetData.isTargetFound = isTargetFound;
        targetData.frameNumber = frameNumber;
        targetData.isFreshData = isFreshData;

        // Indicate that the data is no longer fresh data.
        isFreshData = false;

        return targetData;
    }

    /**
     * This method returns the horizontal(x) center of gravity of the bounding
     * rectangle around the target.
     * 
     * @return The horizontal(x) center of gravity of the bounding rectangle.
     */
    public int getCogX()
    {
        return cogX;
    }

    /**
     * This method returns the vertical(y) center of gravity of the bounding
     * rectangle around the target.
     * 
     * @return The vertical(y) center of gravity of the bounding rectangle.
     */
    public int getCogY()
    {
        return cogY;
    }

    /**
     * This method returns the width of the bounding rectangle around the target.
     * 
     * @return The width of the bounding rectangle.
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * This method returns the area of the bounding rectangle around the target.
     * 
     * @return The area of the bounding rectangle.
     */
    public int getArea()
    {
        return area;
    }

    /**
     * This method indicates if a target was found.
     * 
     * @return True if target is found. False if target is not found.
     */
    public boolean isTargetFound()
    {
        return isTargetFound;
    }

    /**
     * This method returns the frame number of the image.
     * 
     * @return The frame number of the camera image, starting at 1 and incrementing
     *         by 1.
     */
    public int getFrameNumber()
    {
        return frameNumber;
    }

    /**
     * This method indicates if the target data is fresh (new).
     * 
     * @return True if data is fresh. False is data is not fresh.
     */
    public boolean isFreshData()
    {
        return isFreshData;
    }

    public synchronized void fromJson(String message)
    {
        TargetData temp = new Gson().fromJson(message, TargetData.class);
        // set(temp.cogX, temp.cogY, temp.width, temp.area);
        set(temp);
    }

    public synchronized String toJson()
    {
        Gson gson = new Gson(); // Or use new GsonBuilder().create();
        String json = gson.toJson(this); // serializes target to Json
        return json;
    }

    /**
     * This method converts the data to a string format for output.
     * 
     * @return The string to display.
     */
    public String toString()
    {
        return String.format("[TargetData] Frame = %d, cogX = %d, cogY = %d, width = %d, area = %d  %s", frameNumber,
                cogX, cogY, width, area, isFreshData ? "FRESH" : "stale");
    }
}