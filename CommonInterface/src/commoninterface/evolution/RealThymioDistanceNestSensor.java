package commoninterface.evolution;

import java.util.List;

import commoninterface.CISensor;
import commoninterface.RobotCI;
import commoninterface.ThymioCI;
import commoninterface.utils.CIArguments;

public class RealThymioDistanceNestSensor extends CISensor {
	//public static boolean DEBUG = false;
	private ThymioCI thymio;
	private List<Short> readings;
	protected double sensorValue;
	protected final double minCutoff = 100, maxCutoff = 940;
	
	public RealThymioDistanceNestSensor(int id, RobotCI robot, CIArguments args) {
		super(id, robot, args);
		thymio = (ThymioCI)robot;
	}

	@Override
	public int getNumberOfSensors() {
		return 1;
	}

	@Override
	public double getSensorReading(int sensorNumber) {
		return sensorValue;
	}
	
	/*********
	 * prox.ground.ambiant : ambient light intensity at the ground, varies between 0 (no light) and 1023 (maximum light)
	prox.ground.reflected : amount of light received when the sensor emits infrared, varies between 0 (no reflected light) and 1023 (maximum reflected light)
	prox.ground.delta : difference between reflected light and ambient light, linked to the distance and to the ground colour.
	 */

	@Override
	public void update(double time, Object[] entities) {
		readings = thymio.getGroundReflectedLightReadings();
		double currentSensorValue = (readings.get(0) + readings.get(1))/2;
		if(currentSensorValue < minCutoff)
			currentSensorValue = minCutoff;
		else if(currentSensorValue > maxCutoff)
			currentSensorValue = maxCutoff;
		
		//normalize
		double normalised = (currentSensorValue - minCutoff)/(maxCutoff-minCutoff);
		/**
		 * normalised == 0 equals in the nest
		 * normalised == 1, not in range
		 * 
		 *  so, flip value
		 */
		
		normalised = 1 - normalised;
		
		this.sensorValue = normalised;
	}

}
