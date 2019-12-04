package frc.robot;

import edu.wpi.first.wpilibj.drive.MecanumDrive;
import edu.wpi.first.wpilibj.Encoder;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import com.revrobotics.CANSparkMaxLowLevel.ConfigParameter;
import com.revrobotics.CANSparkMax.IdleMode;

import frc.robot.MotorConstants;

/**
 * This class represents the robot's drivetrain. It contains all the code for
 * properly controlling and measuring the movements of the robot.
 * 
 * @author: Yash Gautam Created: 1/15/19 Last Worked On: 1/26/19
 */
public class Drivetrain extends MecanumDrive
{
    private static CANSparkMax frontRightMotor = new CANSparkMax(Constants.FRONT_RIGHT_MOTOR_PORT,
            CANSparkMaxLowLevel.MotorType.kBrushless);
    private static CANSparkMax frontLeftMotor = new CANSparkMax(Constants.FRONT_LEFT_MOTOR_PORT,
            CANSparkMaxLowLevel.MotorType.kBrushless);
    private static CANSparkMax backRightMotor = new CANSparkMax(Constants.BACK_RIGHT_MOTOR_PORT,
            CANSparkMaxLowLevel.MotorType.kBrushless);
    private static CANSparkMax backLeftMotor = new CANSparkMax(Constants.BACK_LEFT_MOTOR_PORT,
            CANSparkMaxLowLevel.MotorType.kBrushless);

    private Encoder leftEncoder = new Encoder(Constants.LEFT_ENCODER_CHANNEL_A, Constants.LEFT_ENCODER_CHANNEL_B, true, Encoder.EncodingType.k4X);
    private Encoder rightEncoder = new Encoder(Constants.RIGHT_ENCODER_CHANNEL_A, Constants.RIGHT_ENCODER_CHANNEL_B, false, Encoder.EncodingType.k4X);

    private double speedFactor = 1.0;

    private static Drivetrain instance = new Drivetrain();

    // constructor for drivetrain class
    private Drivetrain()
    {
        super(frontLeftMotor, backLeftMotor, frontRightMotor, backRightMotor);

        frontRightMotor.restoreFactoryDefaults();
        frontLeftMotor.restoreFactoryDefaults();
        backRightMotor.restoreFactoryDefaults();
        backLeftMotor.restoreFactoryDefaults();


        System.out.println(this.getClass().getName() + ": Started Constructing");

        setSafetyEnabled(false);

        frontRightMotor.setSmartCurrentLimit(MotorConstants.getMotorStallCurrent(MotorConstants.Constants.MotorType.kNeoMotor, 0.3));
        // frontRightMotor.setSecondaryCurrentLimit(Constants.SECONDARY_MOTOR_CURRENT_LIMIT);
        frontRightMotor.setOpenLoopRampRate(Constants.DRIVE_RAMP_TIME);
        frontRightMotor.setIdleMode(IdleMode.kBrake);
        frontRightMotor.setParameter(ConfigParameter.kHardLimitFwdEn, false);
        frontRightMotor.setParameter(ConfigParameter.kHardLimitRevEn, false);
        frontRightMotor.setParameter(ConfigParameter.kSoftLimitFwdEn, false);
        frontRightMotor.setParameter(ConfigParameter.kSoftLimitRevEn, false);
        frontRightMotor.setParameter(ConfigParameter.kInputDeadband, Constants.MOTOR_DEADBAND);

        frontLeftMotor.setSmartCurrentLimit(MotorConstants.getMotorStallCurrent(MotorConstants.Constants.MotorType.kNeoMotor, 0.3));
        // frontLeftMotor.setSecondaryCurrentLimit(Constants.SECONDARY_MOTOR_CURRENT_LIMIT);
        frontLeftMotor.setOpenLoopRampRate(Constants.DRIVE_RAMP_TIME);
        frontLeftMotor.setIdleMode(IdleMode.kBrake);
        frontLeftMotor.setParameter(ConfigParameter.kHardLimitFwdEn, false);
        frontLeftMotor.setParameter(ConfigParameter.kHardLimitRevEn, false);
        frontLeftMotor.setParameter(ConfigParameter.kSoftLimitFwdEn, false);
        frontLeftMotor.setParameter(ConfigParameter.kSoftLimitRevEn, false);
        frontLeftMotor.setParameter(ConfigParameter.kInputDeadband, 0);

        backRightMotor.setSmartCurrentLimit(MotorConstants.getMotorStallCurrent(MotorConstants.Constants.MotorType.kNeoMotor, 0.3));
        // backRightMotor.setSecondaryCurrentLimit(Constants.SECONDARY_MOTOR_CURRENT_LIMIT);
        backRightMotor.setOpenLoopRampRate(Constants.DRIVE_RAMP_TIME);
        backRightMotor.setIdleMode(IdleMode.kBrake);
        backRightMotor.setParameter(ConfigParameter.kHardLimitFwdEn, false);
        backRightMotor.setParameter(ConfigParameter.kHardLimitRevEn, false);
        backRightMotor.setParameter(ConfigParameter.kSoftLimitFwdEn, false);
        backRightMotor.setParameter(ConfigParameter.kSoftLimitRevEn, false);
        backRightMotor.setParameter(ConfigParameter.kInputDeadband, 0);

        backLeftMotor.setSmartCurrentLimit(MotorConstants.getMotorStallCurrent(MotorConstants.Constants.MotorType.kNeoMotor, 0.3));
        // backLeftMotor.setSecondaryCurrentLimit(Constants.SECONDARY_MOTOR_CURRENT_LIMIT);
        backLeftMotor.setOpenLoopRampRate(Constants.DRIVE_RAMP_TIME);
        backLeftMotor.setIdleMode(IdleMode.kBrake);
        backLeftMotor.setParameter(ConfigParameter.kHardLimitFwdEn, false);
        backLeftMotor.setParameter(ConfigParameter.kHardLimitRevEn, false);
        backLeftMotor.setParameter(ConfigParameter.kSoftLimitFwdEn, false);
        backLeftMotor.setParameter(ConfigParameter.kSoftLimitRevEn, false);
        backLeftMotor.setParameter(ConfigParameter.kInputDeadband, 0);

        System.out.println(this.getClass().getName() + ": Finished Constructing");
    }

    //returns the instance of the drivetrain
    public static Drivetrain getInstance()
    {
        return instance;
    }

    /**
     * Gets the distance from the left encoder converted to inches.
     * 
     * @return Distance traveled.
     */
    public double getLeftDistanceInInches()
    {
        return leftEncoder.getRaw() / Constants.ENCODER_TICKS_PER_INCH;
    }

    /**
     * Gets the distance from the right encoder converted to inches.
     * 
     * @return Distance traveled.
     */
    public double getRightDistanceInInches()
    {
        return rightEncoder.getRaw() / Constants.ENCODER_TICKS_PER_INCH;
    }

    /**
     * Gets the average distance of the encoders converted to inches.
     * 
     * @return Distance traveled.
     */
    public double getAvgDistanceInInches()
    {
        return (getRightDistanceInInches() + getLeftDistanceInInches()) / 2.0;
    }

    /**
     * Gets the further distance of the encoders converted to inches.
     * 
     * @return Distance traveled.
     */
    public double getDistanceInInches()
    {
        double rightDistance = getRightDistanceInInches();
        double leftDistance = getLeftDistanceInInches();

        return rightDistance > leftDistance ? rightDistance : leftDistance;
    }

    //resets both the encoder values
    public void resetBothEncoders()
    {
        leftEncoder.reset();
        rightEncoder.reset();
    }

    public void setIdleMode(IdleMode mode)
    {
        frontLeftMotor.setIdleMode(mode);
        frontRightMotor.setIdleMode(mode);
        backLeftMotor.setIdleMode(mode);
        backRightMotor.setIdleMode(mode);
    }

    @Override
    public void driveCartesian(double ySpeed, double xSpeed, double zRotation)
    {
        System.out.println(ySpeed + " " + xSpeed + " " + zRotation);
        System.out.flush();
        this.driveCartesian(ySpeed, xSpeed, zRotation, 0.0);
    }

    @Override
    public void driveCartesian(double ySpeed, double xSpeed, double zRotation, double gyroAngle)
    {
        ySpeed *= speedFactor;
        xSpeed *= speedFactor;
        zRotation *= speedFactor;
        
        super.driveCartesian(ySpeed, xSpeed, zRotation, gyroAngle);
    }

        public String getFrontRightMotorData()
    {
        return String.format("%6.3f,  %6.0f,  %6.3f,  %5.1f",
         frontRightMotor.get(), frontRightMotor.getEncoder().getPosition(),
         frontRightMotor.getOutputCurrent(), frontRightMotor.getEncoder().getVelocity());
    }

    public String getFrontLeftMotorData()
    {
        return String.format("%6.3f,  %6.0f,  %6.3f,  %5.1f",
         frontLeftMotor.get(), frontLeftMotor.getEncoder().getPosition(),
         frontLeftMotor.getOutputCurrent(), frontLeftMotor.getEncoder().getVelocity());
    }

    public String getBackRightMotorData()
    {
        return String.format("%6.3f,  %6.0f,  %6.3f,  %5.1f",
         backRightMotor.get(), backRightMotor.getEncoder().getPosition(),
         backRightMotor.getOutputCurrent(), backRightMotor.getEncoder().getVelocity());
    }

    public String getBackLeftMotorData()
    {
        return String.format("%6.3f,  %6.0f,  %6.3f,  %5.1f",
         backLeftMotor.get(), backLeftMotor.getEncoder().getPosition(),
         backLeftMotor.getOutputCurrent(), backLeftMotor.getEncoder().getVelocity());
    }

    @Override
    public String toString()
    {
        return String.format(" FR: %.2f, FL: %.2f, BR: %.2f, BL: %.2f",
                frontRightMotor.getEncoder().getVelocity(), frontLeftMotor.getEncoder().getVelocity(),
                backRightMotor.getEncoder().getVelocity(), backLeftMotor.getEncoder().getVelocity());
    }

    //constants class for drivetrain
    public static class Constants
    {
        public static final int FRONT_LEFT_MOTOR_PORT = 4;
        public static final int FRONT_RIGHT_MOTOR_PORT = 1;
        public static final int BACK_RIGHT_MOTOR_PORT = 2;
        public static final int BACK_LEFT_MOTOR_PORT = 3;

        public static final int PRIMARY_MOTOR_CURRENT_LIMIT = 35;
        public static final int SECONDARY_MOTOR_CURRENT_LIMIT = 45;

        public static final double DRIVE_RAMP_TIME = 0.10;

        public static final double MOTOR_DEADBAND = 0.01;

        public static final double STARTING_SPEED = 0.3;
        public static final double STOPPING_SPEED = 0.175;
        public static final int ROTATE_THRESHOLD = 10;

        public static final int LEFT_SERVO_PORT = 1;
        public static final int RIGHT_SERVO_PORT = 0;

        public static final int LEFT_ENCODER_CHANNEL_A = 18;
        public static final int LEFT_ENCODER_CHANNEL_B = 16;
        public static final int RIGHT_ENCODER_CHANNEL_A = 14;
        public static final int RIGHT_ENCODER_CHANNEL_B = 15;

        public static final double ENCODER_TICKS_PER_INCH = (360.0 * 4.0) / (3.25 * Math.PI);

    }
}
