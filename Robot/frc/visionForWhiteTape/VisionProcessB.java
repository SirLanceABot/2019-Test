package frc.visionForWhiteTape;

import org.opencv.core.*;
import java.util.ArrayList;
import java.lang.Math;

import frc.robot.Robot;

public class VisionProcessB
{
    public enum strafeDirection
    {
        kLeft, kRight, kNone;
    }

    public enum rotate
    {
        kLeft, kRight, kNone;
    }
   
    private static VisionProcessB instance = new VisionProcessB();

    private VisionProcessB()
    {
    }
    
    public static VisionProcessB getInstance()
    {
        return instance;
    }

    // int heightOfMask = 76;
    //private CvSource outputStream;
    //private MjpegServer mjpegserver2;(

    public strafeDirection getStrafeDirection(TargetDataB targetDataParameter)
    {
        strafeDirection returnStrafeDirectionValue;
        if (targetDataParameter.getCenter().x < 79) 
        {
            returnStrafeDirectionValue = strafeDirection.kLeft;
        } 
        else if (targetDataParameter.getCenter().x > 81) 
        {
            returnStrafeDirectionValue = strafeDirection.kRight;
        } 
        else 
        {
            returnStrafeDirectionValue = strafeDirection.kNone;
        }
        return returnStrafeDirectionValue;

    }

    /**
     * return a value between -80 and 80 that is your strafe factor
     */
     public double getStrafeFactor(TargetDataB targetDataParameter)
    {
        double returnStrafeFactor = targetDataParameter.center.x - 80;

        return returnStrafeFactor;
    }

     public rotate getRotateDirection(TargetDataB targetDataParameter)
    {
        rotate returnRotateDirectionValue;
        if (targetDataParameter.fixedAngle < 89)
         {
            returnRotateDirectionValue = rotate.kLeft;
        } 
        else if (targetDataParameter.fixedAngle > 91) 
        {
            returnRotateDirectionValue = rotate.kRight;
        } 
        else
        {
            returnRotateDirectionValue = rotate.kNone;
        }

        return returnRotateDirectionValue;
    }

    /**
     * 
     * @param targetDataParameter
     * @return  returns the rotation factor between -90 and 90 
     */
     public double getRotateFactor(TargetDataB targetDataParameter)
    {
        double returnRotateFactor = targetDataParameter.fixedAngle - 90;

        return returnRotateFactor;
    }

    public TargetDataB getTargetData()
    {
        return Robot.targetInfoB.get();
    }

    // public int getHeightOfMask()
    // {
    //     return heightOfMask;
    // }

    // public void setHeightOfMask(int newHeight)
    // {
    //     heightOfMask = newHeight;
    // }

} // end of class camera_process