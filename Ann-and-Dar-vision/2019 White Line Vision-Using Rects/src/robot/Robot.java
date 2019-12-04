package frc.robot;

import frc.visionForWhiteTape.CameraDrivingFactors;
import frc.visionForWhiteTape.TargetDataB;
import frc.visionForWhiteTape.UdpReceive;
import edu.wpi.first.wpilibj.TimedRobot;

public class Robot extends TimedRobot {
    private Drivetrain drivetrain = Drivetrain.getInstance();
    private Lights bumperLight = new Lights(0);
    private static Thread UDPreceiveThread;
    public static TargetDataB targetDataNext = new TargetDataB(); // where UDP receive puts the data whenever new data is available
    public static TargetDataB targetData = new TargetDataB(); // copy of the data for consistancy during each period

    @Override
    public void robotInit() {
        
        System.out.println("[robotInit] starting");

        // Start a thread to receive target data from the Raspberry Pi
        UdpReceive UDPreceive = new UdpReceive(5800);
        UDPreceiveThread = new Thread(UDPreceive, "4237UDPreceive");   
        UDPreceiveThread.start();

        bumperLight.turnLightsOn();
    }

    @Override
    public void robotPeriodic() {
    }

    @Override
    public void autonomousInit() {
    }

    @Override
    public void autonomousPeriodic() {
        targetData = Robot.targetDataNext.get(); // Atomically get the latest data for this driving iteration

        if (targetData.isFreshData())
        {
            if (whiteLineAlignment())
            {
                // Note that this loop organization checks the alignment on each iteration.
                // If the robot is not aligned, then it attempts alignment within the called method.
                // If the robot is aligned, then here (below) it drives straight for the iteration.

                // True means aligned with tape so now drive straight slowly
                // False means still trying to move autonomously to align with tape so don't go forward yet
                drivetrain.driveCartesian(0.20, 0.0, 0.0); // y go forward slowly
            }
        }
        // If driving changed above or not (isDataFresh), keep doing whatever is happening.
    }

    @Override
    public void teleopInit() {
    }

    @Override
    public void teleopPeriodic() {
    }

    @Override
    public void disabledInit() {
    }

    public boolean whiteLineAlignment()
    {
            double rotationFactor = CameraDrivingFactors.getRotateFactor(targetData);
            double strafeDirectionFactor = CameraDrivingFactors.getStrafeFactor(targetData);

            // rotate first then strafe
            if (rotationFactor != 0.0)
            {
                drivetrain.driveCartesian(0.0, 0.0, rotationFactor); // rotation
            }
            else if (strafeDirectionFactor != 0.0)
            {
                drivetrain.driveCartesian(0.0, strafeDirectionFactor, 0.0); // x strafe
            }
            else
            {
                return true; // aligned
            }

        return false; // not aligned (motors still on trying to align)
    }
}