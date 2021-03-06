package frc.robot;

import frc.components.Arm;
import frc.components.Climber;
import frc.components.Drivetrain;
import frc.components.Elevator;
import edu.wpi.first.wpilibj.TimedRobot;
import frc.control.DriverXbox;
import frc.robot.Teleop;
import frc.robot.SlabShuffleboard.MotorsAndSensorsTabData;
import frc.robot.SlabShuffleboard.PregameSetupTabData;
import frc.robot.SlabShuffleboard.Recording;
import frc.control.Xbox;
import frc.control.Xbox.Constants;
import frc.visionForWhiteTape.TargetDataB;
import frc.visionForRetroReflectiveTape.TargetDataE;
import frc.util.UdpReceive;

import edu.wpi.first.wpilibj.Timer;

public class Robot extends TimedRobot
{
    private Drivetrain drivetrain = Drivetrain.getInstance();
    private DriverXbox driverXbox = DriverXbox.getInstance();
    private Teleop teleop = Teleop.getInstance();
    private Elevator elevator = Elevator.getInstance();
    private Arm arm = Arm.getInstance();
    private Climber climber = Climber.getInstance();
    private Autonomous autonomous = Autonomous.getInstance();

    private SlabShuffleboard slabShuffleboard;
    private PregameSetupTabData pregameSetupTabData;
    private MotorsAndSensorsTabData motorsAndSensorsTabData;
    private boolean hasTeleopRun = false;
    private boolean isPregame = true;
    private boolean isNewPregameData = true;

    private int shuffleboardPrintCounter = 0;

	public static TargetDataB targetInfoB = new TargetDataB();
    public static TargetDataE targetInfoE = new TargetDataE();

    private static UdpReceive UDPreceive; // get messages from the RPi
    private static Thread UDPreceiveThread;
    
    public Robot()
    {
        System.out.println(this.getClass().getName() + ": Started Constructing");
        System.out.println(this.getClass().getName() + ": Finished Constructing");
    }

    @Override
    public void robotInit()
    {
        UDPreceive = new UdpReceive(5800);
        UDPreceiveThread = new Thread(UDPreceive, "4237UDPreceive");
        UDPreceiveThread.start();
  
        slabShuffleboard = SlabShuffleboard.getInstance();

        pregameSetupTabData = slabShuffleboard.getPregameSetupTabData();
        motorsAndSensorsTabData = slabShuffleboard.getMotorsAndSensorsTabData();
    }

    @Override
    public void robotPeriodic()
    {
        // double time1 = Timer.getFPGATimestamp();
        updateAllShuffleboardData();
        // System.out.println(Timer.getFPGATimestamp() - time1);
    }

    @Override
    public void disabledInit()
    {
        if (pregameSetupTabData.recording == Recording.kEntireMatch && hasTeleopRun)
        {
            slabShuffleboard.stopRecording();
        }
        else if (pregameSetupTabData.recording == Recording.kThisModeOnly)
        {
            slabShuffleboard.stopRecording();
        }

        isPregame = true;
        hasTeleopRun = false;
    }

    @Override
    public void disabledPeriodic()
    {
        if (isPregame)
        {
            getPregameSetupData();
        }
    }

    @Override
    public void autonomousInit()
    {
        isPregame = false;

        if (pregameSetupTabData.recording != Recording.kDoNotRecord)
        {
            slabShuffleboard.startRecording();
        }
        
        autonomous.autoInit();
    }

    @Override
    public void autonomousPeriodic()
    {
        //drivetrain.driveCartesian(0, .2, 0);
        System.out.println(drivetrain);
    }

    @Override
    public void teleopInit()
    {
        isPregame = false;
        
        if (pregameSetupTabData.recording == Recording.kThisModeOnly)
        {
            slabShuffleboard.startRecording();
        }

        teleop.teleopInit();

        hasTeleopRun = true;
    }

    @Override
    public void teleopPeriodic()
    {
        double[] scaledArray = driverXbox.getScaledAxes(Constants.LEFT_STICK_AXES, Xbox.Constants.PolynomialDrive.kCubicDrive);
        teleop.teleop();
        //System.out.println(drivetrain);

        // System.out.printf("X = %5.3f Y = %5.3f X = %5.3f Y = %5.3f \n",
        //         driverXbox.getRawAxis(Constants.LEFT_STICK_X_AXIS), driverXbox.getRawAxis(Constants.LEFT_STICK_Y_AXIS),
        //         scaledArray[0], scaledArray[1]);
    }

    @Override
    public void testInit()
    {
        isPregame = true;
        hasTeleopRun = false;
    }

    @Override
    public void testPeriodic()
    {

    }

    public void getPregameSetupData()
    {
        if (slabShuffleboard.getSendData() && isNewPregameData)
        {
            pregameSetupTabData = slabShuffleboard.getPregameSetupTabData();
            System.out.println(pregameSetupTabData);
            isNewPregameData = false;

            // TODO: Maxwell needs to create a set function to set the pregame data
            // autonomous.setPregameSetupData(pregameSetupTabData);
        }
        else if (!slabShuffleboard.getSendData() && !isNewPregameData)
        {
            isNewPregameData = true;
        }
    }

    public void updateAllShuffleboardData()
    {
        // Motors and Sensors Tab Data
        switch (shuffleboardPrintCounter)
        {
        case 10:
            motorsAndSensorsTabData.frontLeftMotor = drivetrain.getFrontLeftMotorData();
            break;
        case 20:
            motorsAndSensorsTabData.frontRightMotor = drivetrain.getFrontRightMotorData();
            break;
        case 30:
            motorsAndSensorsTabData.backLeftMotor = drivetrain.getBackLeftMotorData();
            break;
        case 40:
            motorsAndSensorsTabData.backRightMotor = drivetrain.getBackRightMotorData();
            break;
        case 50:
            motorsAndSensorsTabData.omniWheel = drivetrain.getOmniWheelData();
            break;
        case 60:
            motorsAndSensorsTabData.elevator = elevator.getMasterLegElevatorMotorData();
            break;
        case 70:
            motorsAndSensorsTabData.arm = arm.getArmMotorData();
            break;
        case 80:
            motorsAndSensorsTabData.wrist = arm.getWristSolenoidData();
            break;
        case 90:
            motorsAndSensorsTabData.cargoIntakeRoller = arm.getIntakeRollerMotorData();
            break;
        case 100:
            motorsAndSensorsTabData.hatchPanelGrabber = arm.getGrabberSolenoidData();
            break;
        case 110:
            motorsAndSensorsTabData.climber = climber.getMasterLegMotorData();
            break;
        case 120:
            motorsAndSensorsTabData.climberPinSolenoid = climber.getPinSolenoidData();
            break;
        case 130:
            // motorsAndSensorsTabData.lidar = 
            break;
        case 140:
            motorsAndSensorsTabData.navX = drivetrain.getNavXData();
            break;
        case 150:
            slabShuffleboard.updateMotorsAndSensorsTabData(motorsAndSensorsTabData);
            shuffleboardPrintCounter = 0;
            break;
        default:
            break;
        }
        shuffleboardPrintCounter += 10;
    }
}