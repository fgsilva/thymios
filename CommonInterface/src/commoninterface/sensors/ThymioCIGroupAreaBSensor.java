package commoninterface.sensors;

import java.util.HashMap;
import java.util.Set;

import commoninterface.CISensor;
import commoninterface.RobotCI;
import commoninterface.ThymioCI;
import commoninterface.utils.CIArguments;

public class ThymioCIGroupAreaBSensor extends CISensor {
	//public static boolean DEBUG = false;
	private ThymioCI thymio;

	private double inAreaB;

	private int totalRobots;
	
	public ThymioCIGroupAreaBSensor(int id, RobotCI robot, CIArguments args) {
		super(id, robot, args);
		thymio = (ThymioCI)robot;
		this.totalRobots = args.getArgumentAsInt("robots");
	}

	@Override
	public int getNumberOfSensors() {
		return 1;
	}

	@Override
	public double getSensorReading(int sensorNumber) {
		return inAreaB;
	}
	
	@Override
	public void update(double time, Object[] entities) {
		HashMap<Integer, boolean[]> positionings = thymio.getRobotsPositioning();
		double percentage = 0;
		Set<Integer> keys = positionings.keySet();
		for(Integer key : keys){
			if(positionings.get(key)[1])
				percentage++;
		}
		
		//exclude self
		percentage /= (totalRobots - 1);
		this.inAreaB = percentage;
	}

}
