package org.fides.client.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

/**
 * UI Screen where the user enters a hostname and a portnumber.
 */
public class ServerAddressScreen {

	/**
	 * Opens a screen where the user can insert a hostname and a portnumber.
	 * 
	 * @return The InetSocketAddress the user selected. Will be null if the user pressed cancel.
	 */
	public static InetSocketAddress getAddress() {
		// Create a Panel
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		// Add a panel where the inputfields can be added
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new GridLayout(2, 1, 0, 5));

		// Add a panel where errors can be shown later
		JPanel errorPanel = new JPanel();
		errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.Y_AXIS));
		errorPanel.setVisible(false);
		errorPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		mainPanel.add(errorPanel);

		// Adds the hostname input
		JLabel labelHostname = new JLabel("Hostname:");
		inputPanel.add(labelHostname);
		JTextField hostName = new JTextField(10);
		inputPanel.add(hostName);

		// Adds the port input
		JLabel labelPort = new JLabel("Port:");
		inputPanel.add(labelPort);
		JTextField port = new JTextField(10);
		inputPanel.add(port);

		// Combines the inputpanel with the mainpanel
		mainPanel.add(inputPanel);

		// Make sure that the username field is selected while it is still possible to press enter for
		// OK
		hostName.addHierarchyListener(new HierarchyListener() {
			public void hierarchyChanged(HierarchyEvent event) {
				final Component component = event.getComponent();
				if (component.isShowing() && (event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
					Window top = SwingUtilities.getWindowAncestor(component);
					top.addWindowFocusListener(new WindowAdapter() {
						public void windowGainedFocus(WindowEvent e) {
							component.requestFocus();
						}
					});
				}
			}
		});

		String[] options = new String[] { "Connect", "Cancel" };
		int option = 0;

		// While the user selects 'connect'
		while (option == 0) {
			option = JOptionPane.showOptionDialog(null, mainPanel, "Choose Fides Server", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

			ArrayList<String> errorMessages = new ArrayList<String>();
			// Check for empty hostname
			if (StringUtils.isBlank(hostName.getText())) {
				errorMessages.add("Hostname can not be blank");
			}
			// Check for empty port and if the port is an integer
			if (StringUtils.isBlank(port.getText())) {
				errorMessages.add("Port can not be blank");
			} else if (!tryParseInt(port.getText())) {
				errorMessages.add("Port has to be a valid number");
			} else {
				// Check if the port is a valid port.
				int portInt = Integer.parseInt(port.getText());
				if (portInt < 0 || portInt > 65535) {
					errorMessages.add("Port has to be a valid port");
				}
			}
			// Check if there were any errors, if not, the address is returned.
			if (errorMessages.isEmpty()) {
				return new InetSocketAddress(hostName.getText(), Integer.parseInt(port.getText()));
			} else {
				// If there were errors, they are added to the dialog and it gets shown again.
				setErrorLabels(errorPanel, errorMessages);
			}
		}
		// The user pressed 'cancel' or the close button.
		return null;
	}

	private static void setErrorLabels(JPanel errorPanel, ArrayList<String> errors)
	{
		errorPanel.removeAll();
		errorPanel.setVisible(true);

		for (String error : errors) {
			JLabel errorLabel = new JLabel();
			errorLabel.setText(error);
			errorLabel.setForeground(Color.red);
			errorPanel.add(errorLabel);
		}
	}

	private static boolean tryParseInt(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}
}
