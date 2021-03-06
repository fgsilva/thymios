package gui.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import commoninterface.network.messages.CameraCaptureMessage;
import commoninterface.network.messages.JavaCameraCaptureMessage;

import threads.UpdateThread;

public class ThymioCapturePanel extends UpdatePanel{

	private JPanel capturePanel;
	private UpdateThread thread;
	private JLabel picLabel;
	
	private long sleepTime = 1000;
	
	public ThymioCapturePanel() {
		setBorder(BorderFactory.createTitledBorder("Camera"));
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(640, 480));
		
		picLabel = new JLabel();
		JPanel imagePanel = new JPanel(new GridLayout());
		capturePanel = new JPanel();
		imagePanel.add(capturePanel);
		
		add(imagePanel);
		add(createRefreshRatePanel(), BorderLayout.SOUTH);
	}

	private JPanel createRefreshRatePanel() {
		JPanel refreshPanel = new JPanel(new BorderLayout());
		refreshPanel.add(new JLabel("Refresh Rate"), BorderLayout.WEST);
		
		final JComboBox<String> comboBoxUpdateRate = new JComboBox<String>();
		comboBoxUpdateRate.setModel(new DefaultComboBoxModel<String>(new String[] {
				"10 Hz", "5 Hz", "1 Hz", "0.1Hz" }));
		comboBoxUpdateRate.setSelectedIndex(2);
		refreshPanel.add(comboBoxUpdateRate, BorderLayout.EAST);
		
		comboBoxUpdateRate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switch (comboBoxUpdateRate.getSelectedIndex()) {
				case 0:
					sleepTime = 100;
					break;
				case 1:
					sleepTime = 200;
					break;
				case 2:
					sleepTime = 1000;
					break;
				case 3:
					sleepTime = 10000;
					break;
				default:
					sleepTime = 1000;
					break;
				}
				wakeUpThread();
			}
		});
		
		JPanel bottom = new JPanel(new GridLayout(1,1));
		bottom.add(refreshPanel);
		
		return bottom;
	}
	
	public synchronized void displayData(JavaCameraCaptureMessage message) {
	
		byte[] bytes = message.getFrameBytes();
		if(bytes != null && bytes.length > 0){
			picLabel.setIcon(new ImageIcon(bytes));
			capturePanel.add(picLabel);
			capturePanel.validate();
			notifyAll();
		}
	}
	
	@Override
	public void registerThread(UpdateThread t) {
		this.thread = t;
	}

	private synchronized void wakeUpThread() {
		notifyAll();
		thread.interrupt();
	}
	
	@Override
	public void threadWait() {
		try {
			synchronized(this){
				wait();
			}
		}catch(Exception e) {}
	}

	@Override
	public long getSleepTime() {
		return sleepTime;
	}

}
