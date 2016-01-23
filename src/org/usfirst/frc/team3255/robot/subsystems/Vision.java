package org.usfirst.frc.team3255.robot.subsystems;

import java.util.Comparator;
import java.util.Vector;

import org.usfirst.frc.team3255.robot.commands.CommandBase;
import org.usfirst.frc.team3255.robot.commands.VisionUpdate;

import com.ni.vision.NIVision;
import com.ni.vision.NIVision.DrawMode;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.ImageType;
import com.ni.vision.NIVision.ShapeMode;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.command.Subsystem;


/**
 *
 */
public class Vision extends Subsystem {
	
	public class ParticleReport implements Comparator<ParticleReport>, Comparable<ParticleReport>{
		double PercentAreaToImageArea;
		double Area;
		double ConvexHullArea;
		double BoundingRectLeft;
		double BoundingRectTop;
		double BoundingRectRight;
		double BoundingRectBottom;
		@Override
		public int compareTo(ParticleReport r) {
			return (int)(r.Area - this.Area);
		}
		@Override
		public int compare(ParticleReport r1, ParticleReport r2) {
			return (int)(r1.Area - r2.Area);
		}
	};
	
	//Structure to represent the scores for the various tests used for target identification
	public class Scores {
		double Trapezoid;
		double LongAspect;
		double ShortAspect;
		double AreaToConvexHullArea;
	};
	
	//Images
	Image frame;
	Image HSVFrame;
	Image binaryFrame;
	int imaqError;
	
	int frontSession, rearSession, currSession;
	
	int numRawParticles = 0;
	int numParticles = 0;
	
	boolean isTote = false;
	double distance = 0.0;
	boolean started = false;
	
	boolean frontCamera = true;
	
	int newSession;
	NIVision.Rect rect = new NIVision.Rect(10, 10, 100, 100);
	
	//Constants
	public static NIVision.Range TOTE_HUE_RANGE = new NIVision.Range(24, 49);	//Default hue range for yellow tote
	public static NIVision.Range TOTE_SAT_RANGE = new NIVision.Range(67, 255);	//Default saturation range for yellow tote
	public static NIVision.Range TOTE_VAL_RANGE = new NIVision.Range(49, 255);	//Default value range for yellow tote
	public static double AREA_MINIMUM = 0.5; //Default Area minimum for particle as a percentage of total image area
	double LONG_RATIO = 2.22; //Tote long side = 26.9 / Tote height = 12.1 = 2.22
	double SHORT_RATIO = 1.4; //Tote short side = 16.9 / Tote height = 12.1 = 1.4
	double SCORE_MIN = 75.0;  //Minimum score to be considered a tote
	double VIEW_ANGLE = 49.4; //View angle fo camera, set to Axis m1011 by default, 64 for m1013, 51.7 for 206, 52 for HD3000 square, 60 for HD3000 640x480
	NIVision.ParticleFilterCriteria2 criteria[] = new NIVision.ParticleFilterCriteria2[1];
	NIVision.ParticleFilterOptions2 filterOptions = new NIVision.ParticleFilterOptions2(0,0,1,1);
	Scores scores = new Scores();
	
	public Vision() {
		// create images
		frame = NIVision.imaqCreateImage(ImageType.IMAGE_RGB, 0);
		HSVFrame = NIVision.imaqCreateImage(ImageType.IMAGE_U8, 0);
		binaryFrame = NIVision.imaqCreateImage(ImageType.IMAGE_U8, 0);
		criteria[0] = new NIVision.ParticleFilterCriteria2(NIVision.MeasurementType.MT_AREA_BY_IMAGE_AREA, AREA_MINIMUM, 100.0, 0, 0);

        // the camera name (ex "cam0") can be found through the roborio web interface
        frontSession = NIVision.IMAQdxOpenCamera("cam0",
                NIVision.IMAQdxCameraControlMode.CameraControlModeController);

        rearSession = NIVision.IMAQdxOpenCamera("cam1",
                NIVision.IMAQdxCameraControlMode.CameraControlModeController);
        
        currSession = frontSession;
        NIVision.IMAQdxConfigureGrab(currSession);	
    }
	
	public void update() {
		if(frontCamera) {
			newSession = frontSession;
		}
		else {
			newSession = rearSession;
		}
		
		if(newSession != currSession) {
			NIVision.IMAQdxStopAcquisition(currSession);

			currSession = newSession;
	        NIVision.IMAQdxConfigureGrab(currSession);
			NIVision.IMAQdxStartAcquisition(currSession);					
		}

		//read file in from disk. For this example to run you need to copy image20.jpg from the SampleImages folder to the
		//directory shown below using FTP or SFTP: http://wpilib.screenstepslive.com/s/4485/m/24166/l/282299-roborio-ftp
		// NIVision.imaqReadFile(frame, "/home/lvuser/SampleImages/image20.jpg");
        NIVision.IMAQdxGrab(currSession, frame, 1);
        
		//Update threshold values from SmartDashboard. For performance reasons it is recommended to remove this after calibration is finished.
		TOTE_HUE_RANGE.minValue = (int)CommandBase.telemetry.getHueMin();
		TOTE_HUE_RANGE.maxValue = (int)CommandBase.telemetry.getHueMax();
		TOTE_SAT_RANGE.minValue = (int)CommandBase.telemetry.getSatMin();
		TOTE_SAT_RANGE.maxValue = (int)CommandBase.telemetry.getSatMax();
		TOTE_VAL_RANGE.minValue = (int)CommandBase.telemetry.getValMin();
		TOTE_VAL_RANGE.maxValue = (int)CommandBase.telemetry.getValMax();

		//Threshold the image looking for yellow (tote color)
		NIVision.imaqColorThreshold(HSVFrame, frame, 255, NIVision.ColorMode.HSV, TOTE_HUE_RANGE, TOTE_SAT_RANGE, TOTE_VAL_RANGE);

		//Send particle count to dashboard
		numRawParticles = NIVision.imaqCountParticles(HSVFrame, 1);

		//filter out small particles
		float areaMin = (float)CommandBase.telemetry.getAreaMin();
		criteria[0].lower = areaMin;
		imaqError = NIVision.imaqParticleFilter4(binaryFrame, HSVFrame, criteria, filterOptions, null);

		//Send particle count after filtering to dashboard
		numParticles = NIVision.imaqCountParticles(binaryFrame, 1);

		if(numParticles > 0) {
			//Measure particles and sort by particle size
			Vector<ParticleReport> particles = new Vector<ParticleReport>();
			for(int particleIndex = 0; particleIndex < numParticles; particleIndex++) {
				ParticleReport par = new ParticleReport();
				par.PercentAreaToImageArea = NIVision.imaqMeasureParticle(binaryFrame, particleIndex, 0, NIVision.MeasurementType.MT_AREA_BY_IMAGE_AREA);
				par.Area = NIVision.imaqMeasureParticle(binaryFrame, particleIndex, 0, NIVision.MeasurementType.MT_AREA);
				par.ConvexHullArea = NIVision.imaqMeasureParticle(binaryFrame, particleIndex, 0, NIVision.MeasurementType.MT_CONVEX_HULL_AREA);
				par.BoundingRectTop = NIVision.imaqMeasureParticle(binaryFrame, particleIndex, 0, NIVision.MeasurementType.MT_BOUNDING_RECT_TOP);
				par.BoundingRectLeft = NIVision.imaqMeasureParticle(binaryFrame, particleIndex, 0, NIVision.MeasurementType.MT_BOUNDING_RECT_LEFT);
				par.BoundingRectBottom = NIVision.imaqMeasureParticle(binaryFrame, particleIndex, 0, NIVision.MeasurementType.MT_BOUNDING_RECT_BOTTOM);
				par.BoundingRectRight = NIVision.imaqMeasureParticle(binaryFrame, particleIndex, 0, NIVision.MeasurementType.MT_BOUNDING_RECT_RIGHT);
				particles.add(par);
			}
			particles.sort(null);

			rect.top = (int) particles.elementAt(0).BoundingRectTop;
			rect.left = (int) particles.elementAt(0).BoundingRectLeft;
			rect.height = (int) particles.elementAt(0).BoundingRectBottom - rect.top;
			rect.width = (int) particles.elementAt(0).BoundingRectRight - rect.left;
			
            NIVision.imaqDrawShapeOnImage(frame, frame, rect,
                    DrawMode.DRAW_VALUE, ShapeMode.SHAPE_OVAL, 0.0f);

            NIVision.imaqDrawShapeOnImage(HSVFrame, HSVFrame, rect,
                    DrawMode.DRAW_VALUE, ShapeMode.SHAPE_OVAL, 0.0f);

			//This example only scores the largest particle. Extending to score all particles and choosing the desired one is left as an exercise
			//for the reader. Note that the long and short side scores expect a single tote and will not work for a stack of 2 or more totes.
			//Modification of the code to accommodate 2 or more stacked totes is left as an exercise for the reader.
			scores.Trapezoid = TrapezoidScore(particles.elementAt(0));
			scores.LongAspect = LongSideScore(particles.elementAt(0));
			scores.ShortAspect = ShortSideScore(particles.elementAt(0));
			scores.AreaToConvexHullArea = ConvexHullAreaScore(particles.elementAt(0));
			isTote = scores.Trapezoid > SCORE_MIN && (scores.LongAspect > SCORE_MIN || scores.ShortAspect > SCORE_MIN) && scores.AreaToConvexHullArea > SCORE_MIN;
			boolean isLong = scores.LongAspect > scores.ShortAspect;
			distance = computeDistance(binaryFrame, particles.elementAt(0), isLong);
		} 
		else {
			isTote = false;
		}

		if(CommandBase.telemetry.isProcessed()) {
			CameraServer.getInstance().setImage(HSVFrame);
		}
		else {
			CameraServer.getInstance().setImage(frame);
		}
	}
	
	public void cameraStart() {
		// return if the camera is already started
		if(started) {
			return;
		}
		
		NIVision.IMAQdxStartAcquisition(currSession);
		started = true;
	}
	
	public void cameraStop() {
		// return if the camera is already stopped
		if(!started) {
			return;
		}
		
		NIVision.IMAQdxStopAcquisition(currSession);
		started = false;
	}
	
	public void useFrontCamera(boolean front) {
		frontCamera = front;
	}
	
	public boolean isFrontCamera() {
		return frontCamera;
	}
	
	public boolean isTote() {
		return isTote;
	}
	
	public double getDistance() {
		return distance;
	}
	
	public int getNumRawParticles() {
		return numRawParticles;
	}
	
	public int getNumParticles() {
		return numParticles;
	}
	
	public double getTrapezoidScore() {
		return scores.Trapezoid;
	}
	
	public double getLongAspectScore() {
		return scores.LongAspect;
	}
	
	public double getShortAspectScore() {
		return scores.ShortAspect;
	}
	
	public double getConvexAreaScore() {
		return scores.AreaToConvexHullArea;
	}
	
	double ratioToScore(double ratio) {
		return (Math.max(0, Math.min(100*(1-Math.abs(1-ratio)), 100)));
	}

	double TrapezoidScore(ParticleReport report) {
		return ratioToScore(report.ConvexHullArea/((report.BoundingRectRight-report.BoundingRectLeft)*(report.BoundingRectBottom-report.BoundingRectTop)*.954));
	}
	
	double LongSideScore(ParticleReport report) {
		return ratioToScore(((report.BoundingRectRight-report.BoundingRectLeft)/(report.BoundingRectBottom-report.BoundingRectTop))/LONG_RATIO);
	}

	double ShortSideScore(ParticleReport report){
		return ratioToScore(((report.BoundingRectRight-report.BoundingRectLeft)/(report.BoundingRectBottom-report.BoundingRectTop))/SHORT_RATIO);
	}
	
	double ConvexHullAreaScore(ParticleReport report) {
		return ratioToScore((report.Area/report.ConvexHullArea)*1.18);
	}

	double computeDistance (Image image, ParticleReport report, boolean isLong) {
		double normalizedWidth, targetWidth;
		NIVision.GetImageSizeResult size;

		size = NIVision.imaqGetImageSize(image);
		normalizedWidth = 2*(report.BoundingRectRight - report.BoundingRectLeft)/size.width;
		targetWidth = isLong ? 26.0 : 16.9;

		return  targetWidth/(normalizedWidth*12*Math.tan(VIEW_ANGLE*Math.PI/(180*2)));
	}
	
	@Override
	protected void initDefaultCommand() {
		setDefaultCommand(new VisionUpdate());
	}
}
