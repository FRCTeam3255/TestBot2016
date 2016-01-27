package org.usfirst.frc.team3255.robot.subsystems;

import org.usfirst.frc.team3255.robot.commands.CommandBase;

import edu.wpi.first.wpilibj.command.PIDSubsystem;

public class VisionRotatePID extends PIDSubsystem {
    
	private static final double DEFAULT_P = 0.0;
	private static final double DEFAULT_I = 0.0;
	private static final double DEFAULT_D = 0.0;

	private double output = 0;
	private boolean outputValid = false;
	
	public VisionRotatePID() {
		super(DEFAULT_P, DEFAULT_I, DEFAULT_D);
		
		// this controller uses the location of the target relative to the center of the
		// image as the sensed value. This gets computed in the range of -1 -> +1 with
		// -1 being on the left edge of the image, and +1 on the right edge of the image.
		// Therefore, the setpoint to controller wants to achieve is always 0.
		this.setSetpoint(0);
	}

	@Override
	protected double returnPIDInput() {
		// if the vision system did not detect a tote, then return the current
		// setpoint as the PID input so that no additional error is accumulated
		// in the PID controller. Also mark the output as invalid to make sure
		// the output of the PID controller is forced to zero.
		if (CommandBase.vision.isTote() == false) {
			outputValid = false;
			return this.getSetpoint();
		}

		// mark the output of the PID controller as valid because a tote was found
		outputValid = true;		
		
		// return the location of the tote relative to the center of the image (-1 -> +1)
		return -CommandBase.vision.getToteCenterX();
	}

	@Override
	protected void usePIDOutput(double output) {
		this.output = output;
	}

	/*
	 * This method returns the most recent cached value from the PID output
	 */
	public double getOuptut() {
		// return 0 if the PID controller is disabled or if the cached value is not valid
		if((this.getPIDController().isEnabled() == false) || (outputValid == false)) {
			return 0;
		}
		return output;
	}

	@Override
	protected void initDefaultCommand() {
		// TODO Auto-generated method stub
		
	}
}
