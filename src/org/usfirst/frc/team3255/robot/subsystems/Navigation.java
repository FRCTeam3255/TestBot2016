package org.usfirst.frc.team3255.robot.subsystems;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.command.Subsystem;

/**
 *
 */
public class Navigation extends Subsystem {
	public static AHRS ahrs = null;
	
	public static void AHRSinit() {
        try {
            /* Communicate w/navX MXP via the MXP SPI Bus.                                     */
            /* Alternatively:  I2C.Port.kMXP, SerialPort.Port.kMXP or SerialPort.Port.kUSB     */
            /* See http://navx-mxp.kauailabs.com/guidance/selecting-an-interface/ for details. */
            ahrs = new AHRS(SPI.Port.kMXP); 
        } catch (RuntimeException ex ) {
            DriverStation.reportError("Error instantiating navX MXP:  " + ex.getMessage(), true);
        }
	}
	
	public Navigation() {
		super();
		
		init();
	}
	
	public Navigation(String name) {
		super(name);
		
		init();
	}
	
	public void init() {
	}
	
	public double getYaw() {
		return ahrs.getYaw();
	}
	
	public void resetYaw() {
		ahrs.reset();
	}
	

    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand(new MySpecialCommand());
    }
}

