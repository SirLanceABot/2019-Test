package frc.visionForWhiteTape;

public class CameraDrivingFactors
{
    private static final double ROTATION_SPEED = 0.5;
    private static final double STRAFE_SPEED = 0.5;

    public static double getStrafeFactor(TargetDataB targetDataParameter)
    {
        double returnStrafeFactor = STRAFE_SPEED * ((targetDataParameter.center.x - 80) / 80.0);

        return returnStrafeFactor;
    }

    public static double getRotateFactor(TargetDataB targetDataParameter)
    {
        double returnRotateFactor = ROTATION_SPEED * ((targetDataParameter.fixedAngle - 90) / 90.0);

        return returnRotateFactor;
    }
}