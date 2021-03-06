package actuator;

import sensors.IntruderSensor;
import simulation.Simulator;
import simulation.robot.Robot;
import simulation.robot.actuators.Actuator;
import simulation.util.Arguments;
import simulation.util.ArgumentsAnnotation;

public class ShareWithActuator extends Actuator {

	@ArgumentsAnnotation(name="idsensor", help="Id of the sensor that will share information.", defaultValue="1")
	private int idSensor;
	private double shareWith = 1;
	private int maxShareWith;
	private IntruderSensor sensor;
	
	public ShareWithActuator(Simulator simulator, int id,
			Arguments args) {
		super(simulator, id, args);
		idSensor = args.getArgumentAsIntOrSetDefault("idsensor", -1);
		
	}

	public void setShareWith(double shareWith) {
		this.shareWith = shareWith;
	}
	
	public int getNumberOfOutputs() {
		return 1;
	}

	@Override
	public void apply(Robot robot, double delta) {
		if(sensor == null){
			sensor = (IntruderSensor)robot.getSensorWithId(idSensor);
			maxShareWith = sensor.getNumberOfRecivingRobots();
		}
		
		sensor.setNumberOfRecivingRobots((int)(shareWith * maxShareWith));
		
	}
	
}
