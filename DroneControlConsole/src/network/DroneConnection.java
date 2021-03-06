package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import commoninterface.network.messages.Message;

import main.RobotControlConsole;

public abstract class DroneConnection extends Thread {
	
	private final static boolean DEBUG = false;
	protected int port;
	protected Socket socket;
	protected ObjectInputStream in;
	protected ObjectOutputStream out;
	protected InetAddress destHost;
	protected String destHostName;
	protected RobotControlConsole console;
	
	protected boolean ready = false;
	
	public DroneConnection(RobotControlConsole console, InetAddress destHost, int port) throws IOException {
		this.socket = null;
		this.in = null;
		this.out = null;
		this.destHostName = null;
		this.destHost = destHost;
		this.port = port;
		this.console = console;
		
		if(!checkIP(destHost)) {
			throw new UnknownHostException(destHost.getHostAddress()+" unreachable!");
		}
		
		connect();
	}
	
	private void connect() throws IOException {
		System.out.println("connecting to: " + destHost + ";" + port);
		socket = new Socket(destHost, port);
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
	}

	public synchronized void sendData(Object data) {
		
		if(!ready)
			return;
		
		try {
			if (socket != null && !socket.isClosed()) {
				out.writeObject(data);
				out.flush();
				if(DEBUG)
					System.out.println("[SEND] Sent "+data.getClass().getSimpleName());
			}
		} catch (IOException e) {
			System.err.println("Unable to send data... there is an open connection?");
			e.printStackTrace();
		}
	}
	
	private boolean checkIP(InetAddress destHost) throws IOException {
		return InetAddress.getByName(destHost.getHostAddress()).isReachable(5*1000);//5 sec timeout
	}

	@Override
	public void run() {
		try {
			
			initialization();
			
			while (true) {
				update();
			}
			
		} catch (IOException e) {
			System.err.println("Drone Controller closed the connection");
		} catch (ClassNotFoundException e) {
			System.err.println("I didn't reveived a correct name from "+ socket.getInetAddress().getHostAddress());
		} catch (Exception e ) {
			e.printStackTrace();
		}finally {
			console.disconnect();
		}
	}
	
	protected void initialization() throws IOException, ClassNotFoundException{
		out.writeObject(InetAddress.getLocalHost().getHostName());
		out.flush();

		destHostName = (String) in.readObject();
		System.out.println("Connected to " + destHostName
				+ " (" + destHost.getHostAddress()+":"+port + ")");

		ready = true;
	}
	
	protected void update() throws IOException {
		try {
			Message message = (Message) in.readObject();
			if(DEBUG)
				System.out.println("[RECEIVED] Received "+message.getClass().getSimpleName());
			console.processMessage(message);
		} catch (ClassNotFoundException e) {
			System.err.println("Received class of unknown type from "
							+ destHostName
							+ ", so it was discarded....");
		}
	}

	public String getDestHostName() {
		return destHostName;
	}

	public InetAddress getDestInetAddress() {
		return destHost;
	}

	public synchronized void closeConnection() {
		if (socket != null && !socket.isClosed()) {
			System.out.println("Closing Connection.... ");
			try {
				this.interrupt();
				socket.close();
				in.close();
				out.close();
			} catch (IOException e) {
				System.err.println("Unable to close connection... is there an open connection?");
			}
		}
	}
	
	public boolean connectionOK() {
		return socket != null && !socket.isClosed();
	}
}