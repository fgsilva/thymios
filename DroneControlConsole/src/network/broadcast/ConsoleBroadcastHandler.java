package network.broadcast;

import gui.DroneGUI;
import gui.ThymioGUI;
import gui.panels.LogsPanel;
import gui.panels.ThymioLogsPanel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import main.DroneControlConsole;
import main.RobotControlConsole;
import network.server.shared.dataObjects.DroneData;
import network.server.shared.dataObjects.DronesSet;
import commoninterface.entities.Entity;
import commoninterface.entities.RobotLocation;
import commoninterface.network.NetworkUtils;
import commoninterface.network.broadcast.BroadcastMessage;
import commoninterface.network.broadcast.EntitiesBroadcastMessage;
import commoninterface.network.broadcast.HeartbeatBroadcastMessage;
import commoninterface.network.broadcast.PositionBroadcastMessage;

public class ConsoleBroadcastHandler {

	public static int PORT = 8888;
	public static int RETRANSMIT_PORT = 8888 + 100;
	public static int BUFFER_LENGTH = 15000;

	private BroadcastSender sender;
	private BroadcastReceiver receiver;
	private RobotControlConsole console;
	private String ownAddress;
	private String broadcastAddress;
	
	// TODO this has to be true for the mixed experiments to work
	private boolean retransmit = false;

	public ConsoleBroadcastHandler(RobotControlConsole console) {
		this.console = console;
		receiver = new BroadcastReceiver();
		sender = new BroadcastSender();
		receiver.start();
	}

	public void messageReceived(String address, String message) {
		newBroadcastMessage(address, message);
	}

	public void sendMessage(String message) {
		sender.sendMessage(message);
	}

	public void closeConnections() {
		receiver.shutdown();
		sender.shutdown();
	}

	class BroadcastSender {

		private DatagramSocket socket;
		private DatagramSocket retransmitSocket;

		public BroadcastSender() {
			try {
				InetAddress ownInetAddress = InetAddress.getByName(NetworkUtils
						.getAddress());
				ownAddress = ownInetAddress.getHostAddress();
				System.out.println("SENDER " + ownInetAddress);
				broadcastAddress = NetworkUtils.getBroadcastAddress(ownAddress);
				socket = new DatagramSocket(PORT + 1, ownInetAddress);
				socket.setBroadcast(true);

				if (retransmit) {
					retransmitSocket = new DatagramSocket(RETRANSMIT_PORT - 1,
							ownInetAddress);
					retransmitSocket.setBroadcast(true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void sendMessage(String message) {
			try {
				byte[] sendData = message.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData,
						sendData.length,
						InetAddress.getByName(broadcastAddress), PORT);
				socket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void retransmit(String message) {

			if (!retransmit)
				return;

			try {
				byte[] sendData = message.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData,
						sendData.length,
						InetAddress.getByName(broadcastAddress),
						RETRANSMIT_PORT);
				retransmitSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void shutdown() {
			if (socket != null)
				socket.close();
			if (retransmitSocket != null)
				retransmitSocket.close();
		}
	}

	public void newBroadcastMessage(String address, String message) {
		
		String[] split = message.split(BroadcastMessage.MESSAGE_SEPARATOR);
		switch (split[0]) {
		case "HEARTBEAT":
				long timeElapsed = Long.parseLong(HeartbeatBroadcastMessage
						.decode(message)[1]);
				console.getGUI().getConnectionPanel().newAddress(address);
				updateDroneData(address,
						HeartbeatBroadcastMessage.decode(message)[2], split[0],
						timeElapsed);
			break;
		case "GPS":
			RobotLocation di = PositionBroadcastMessage
					.decode(message);
			if (di != null) {
				if (console.getGUI() instanceof DroneGUI) {
					((DroneGUI) console.getGUI()).getMapPanel().displayData(di);
				}
				console.getGUI().getConnectionPanel().newAddress(address);
				updateDroneData(address, di.getName(), split[0], di);
			}
			break;
		case "ENTITIES":
			if (!address.equals(ownAddress)) {
				ArrayList<Entity> entities = EntitiesBroadcastMessage.decode(
						address, message);
				if (console instanceof DroneControlConsole) {
					((DroneGUI) ((DroneControlConsole) console).getGUI())
							.getMapPanel().replaceEntities(entities);
				}
			}
			break;
		default:
			//System.out.println("Uncategorized message > " + message
				//	+ " < from " + address);
		}

		if (retransmit)
			sender.retransmit(message);

	}

	private void updateDroneData(String address, String name, String msgType, Object obj) {
		if (console instanceof DroneControlConsole) {
			try {
				DronesSet dronesSet = ((DroneControlConsole) console)
						.getDronesSet();
				DroneData drone;
				boolean exists = false;

				if (!dronesSet.existsDrone(name)) {
					drone = new DroneData();
					drone.setIpAddr(InetAddress.getByName(address).getHostAddress());
					drone.setName(name);
				} else {
					drone = dronesSet.getDrone(name);
					exists = true;
				}

				switch (msgType) {
				
				case "HEARTBEAT":
					drone.setTimeSinceLastHeartbeat(System.currentTimeMillis());
					break;
				case "GPS":
					drone.getGPSData().setLatitudeDecimal(((RobotLocation) obj).getLatLon().getLat());	
					drone.getGPSData().setLongitudeDecimal(((RobotLocation) obj).getLatLon().getLon());
					drone.setOrientation(((RobotLocation)obj).getOrientation());
					break;
				default:
					System.out
							.println("Uncategorized message type to update on drone data, from "
									+ name + " at " + address);
				}

				if (!exists) {
					dronesSet.addDrone(drone);
				}
			} catch (UnknownHostException e) {
				System.err
						.println("UnknownHostException on ConsoleBroadcastHandler....\n"
								+ e.getMessage());
			}

		}
	}

	class BroadcastReceiver extends Thread {

		private DatagramSocket socket;
		private boolean execute = true;

		public BroadcastReceiver() {
			try {
				System.out.println("RECEIVER "
						+ InetAddress.getByName("0.0.0.0") + ", port: " + PORT);
				socket = new DatagramSocket(PORT,
						InetAddress.getByName("0.0.0.0"));
				socket.setBroadcast(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				while (execute) {
					byte[] recvBuf = new byte[BUFFER_LENGTH];
					DatagramPacket packet = new DatagramPacket(recvBuf,
							recvBuf.length);
					socket.receive(packet);

					String message = new String(packet.getData()).trim();

					// if(!packet.getAddress().getHostAddress().equals(ownAddress))
					messageReceived(packet.getAddress().getHostAddress(),
							message);
					
					if(console.getGUI() instanceof DroneGUI){
						LogsPanel logsPanel = ((DroneGUI)console.getGUI()).getLogsPanel();
						
						if(logsPanel.getLoggerCheckBox().isSelected())
							logsPanel.getBroadcastLogger().log(message);
					}
					else if(console.getGUI() instanceof ThymioGUI){
						ThymioLogsPanel logsPanel = ((ThymioGUI)console.getGUI()).getLogsPanel();
						
						if(logsPanel.getLoggerCheckBox().isSelected())
							logsPanel.getBroadcastLogger().log(message);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				socket.close();
			}
		}

		public void shutdown() {
			execute = false;
		}
	}
}
