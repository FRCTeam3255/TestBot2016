package org.usfirst.frc.team3255.robot.subsystems;

import org.usfirst.frc.team3255.robot.commands.*;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 *
 */
public class Telemetry extends Subsystem {
	
	public Telemetry() {
		super();
		
		init();
	}
	
	public Telemetry(String name) {
		super(name);
		
		init();
	}
    
    // Put methods for controlling this subsystem
    // here. Call these from Commands.
	
	public void init() {
		// DriveTrain Commands
		SmartDashboard.putData("Drive Forward", new DriveForward());
		SmartDashboard.putData("Drive Reverse", new DriveReverse());
		SmartDashboard.putData("Drive Forward 1 Rotation", new DriveDistanceForward(1));
		SmartDashboard.putData("Drive Forward 4 Rotations", new DriveDistanceForward(4));
		SmartDashboard.putData("Drive Stop", new DriveStop());
		SmartDashboard.putData("Reset Encoders", new DriveEncoderReset());

		SmartDashboard.putData("Shooter Set Speed", new ShooterSetSpeed());
		SmartDashboard.putData("Shooter Set Voltage", new ShooterSetVoltage());
		SmartDashboard.putData("Shooter Stop", new ShooterStop());
		SmartDashboard.putData("Shooter Reset Encoders", new ShooterResetEncoders());
		
		SmartDashboard.putData("Solenoid Open", new SolenoidOpen());
		SmartDashboard.putData("Solenoid Close", new SolenoidClose());
		
	}
	
	public void update() {
		// SmartDashboard.putNumber("Left Encoder Counts", CommandBase.drivetrain.getLeftEncoderCount());
		// SmartDashboard.putNumber("Left Encoder Distance", CommandBase.drivetrain.getLeftEncoderDistance());
		SmartDashboard.putNumber("Yaw", CommandBase.navigation.getYaw());
		
		
		SmartDashboard.putNumber("Shooter Encoder Position", CommandBase.PIDShooter.getEncoderPosition());
		SmartDashboard.putNumber("Shooter Encoder Velocity", CommandBase.PIDShooter.getEncoderVelocity());
		/*
		SmartDashboard.putNumber("Input Voltage", CommandBase.PIDShooter.getInputVoltage());
		SmartDashboard.putNumber("Output Voltage", CommandBase.PIDShooter.getOutputVoltage());
		SmartDashboard.putNumber("Output Current", CommandBase.PIDShooter.getOutputCurrent());
		*/
	}

    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand(new MySpecialCommand());
    	setDefaultCommand(new TelemetryUpdate());
    }
}

