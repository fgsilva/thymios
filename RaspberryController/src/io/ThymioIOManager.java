package io;

import io.input.ControllerInput;
import io.input.JavaCameraCaptureInput;
import io.input.ThymioProximitySensorsInput;
import java.util.ArrayList;
import java.util.List;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import commoninterface.network.messages.MotorMessage;
import commoninterface.utils.CIArguments;
import commoninterfaceimpl.RealThymioCI;
import ch.epfl.mobots.AsebaNetwork;
import ch.epfl.mobots.Aseba.ThymioRemoteConnection;

public class ThymioIOManager {

	private ArrayList<ControllerInput> inputs = new ArrayList<ControllerInput>();

	private ThymioRemoteConnection thymioRemoteConnection;

	//for pi camera (python)
	//private CameraCaptureInput cameraInput;

	//java interface

	private JavaCameraCaptureInput javaCameraInput;

	private String initMessages = "\n";

	private RealThymioCI thymioCI;

	private boolean localCommEnabled;

	public ThymioIOManager(RealThymioCI thymioCI, CIArguments args) {
		try {
			this.thymioCI = thymioCI;
			initThymioConnection();
			initInputs(args);

			if (args.getFlagIsTrue("filelogger")) {
				thymioCI.startLogger();
			}


		} catch (DBusException e) {
			initMessages += ("\n[INIT] DBus/Aseba: not ok! (" + e.getMessage() + ")\n");
			e.printStackTrace();
		}
	}

	private void initThymioConnection() throws DBusException {
		DBusConnection conn = DBusConnection
				.getConnection(DBusConnection.SESSION);
		AsebaNetwork recvInterface = (AsebaNetwork) conn.getRemoteObject(
				"ch.epfl.mobots.Aseba", "/", AsebaNetwork.class);
		thymioRemoteConnection = new ThymioRemoteConnection(recvInterface);
	}

	private void initInputs(CIArguments args) {
		ThymioProximitySensorsInput proximitySensorsInput = new ThymioProximitySensorsInput(
				this);
		inputs.add(proximitySensorsInput);

		/*if (args.getFlagIsTrue("picamera")) {
			cameraInput = new CameraCaptureInput(thymioCI);
			inputs.add(cameraInput);
			initMessages += "[INIT] Raspberry Camera initialized \n";
		}*/

		if(args.getFlagIsTrue("javapicamera")){
			javaCameraInput = new JavaCameraCaptureInput(thymioCI);
			inputs.add(javaCameraInput);
			initMessages += "[INIT] Raspberry Camera initialized (JAVA bindings) \n";
		}

	}

	public void shutdown() {
		System.out.println("# Shutting down IO...");
		stopThymio();
		/*if (cameraInput != null)
			CommandLine.executeShellCommand("./picamera/stop_server.sh");*/

		if(this.javaCameraInput != null)
			this.javaCameraInput.release();
		System.out.println("# Finished IO cleanup!");
	}

	public void stopThymio() {
		setMotorSpeeds(0, 0);
	}

	public List<Short> getProximitySensorsReadings() {
		return thymioRemoteConnection.getProximitySensorValues();
	}
	
	public List<Short> getGroundReflectedLightReadings() {
		return thymioRemoteConnection.getGroundReflectedLight();
	}
	
	public List<Short> getGroundAmbientLightReadings() {
		return thymioRemoteConnection.getGroundAmbientLightIntensity();
	}

	public void setMotorSpeeds(MotorMessage message) {
		thymioRemoteConnection.setTargetWheelSpeed(
				(short) (message.getLeftMotor() * 500),
				(short) (message.getRightMotor() * 500));
	}

	public void setMotorSpeeds(double left, double right) {
		thymioRemoteConnection.setTargetWheelSpeed((short) (left * 500),
				(short) (right * 500));
	}

	public ThymioRemoteConnection getThymioRemoteConnection() {
		return thymioRemoteConnection;
	}

	public String getInitMessages() {
		return initMessages;
	}

	public ArrayList<ControllerInput> getInputs() {
		return inputs;
	}

	public RealThymioCI getThymioCI() {
		return thymioCI;
	}

	
	
}
