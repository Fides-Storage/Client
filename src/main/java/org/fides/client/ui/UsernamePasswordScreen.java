package org.fides.client.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.tools.HashUtils;

/**
 * UI where a username and password can be submitted by a user
 */
public class UsernamePasswordScreen {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(UsernamePasswordScreen.class);

	/**
	 * Show a dialog where the user can enter its username and password
	 * 
	 * @return the entered username and password in a string array, returns an null if nothing was entered
	 */
	public static String[] getUsernamePassword() {
		JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);

		// Create a Panel with the correct dimensions
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

		// Add a label to the panel
		JLabel labelUsername = new JLabel("Username:");
		inputPanel.add(labelUsername);

		// Add a usernamefield to the panel with a coloumn with of 10
		JTextField username = new JTextField(10);
		inputPanel.add(username);

		// Add a label to the panel
		JLabel labelPassword = new JLabel("Password:");
		inputPanel.add(labelPassword);

		// Add a passwordfield to the panel with a coloumn with of 10
		JPasswordField password = new JPasswordField(10);
		inputPanel.add(password);

		// Combines the inputpanel with the mainpanel
		mainPanel.add(inputPanel);

		// Make sure that the username field is selected while it is still possible to press enter for
		// OK
		username.addHierarchyListener(new HierarchyListener() {
			public void hierarchyChanged(HierarchyEvent e) {
				final Component c = e.getComponent();
				if (c.isShowing() && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
					Window top = SwingUtilities.getWindowAncestor(c);
					top.addWindowFocusListener(new WindowAdapter() {
						public void windowGainedFocus(WindowEvent e) {
							c.requestFocus();
						}
					});
				}
			}
		});

		// Place the 2 buttons for OK and Cancel and show the dialog
		String[] options = new String[] { "Login", "Register", "Cancel" };
		int option = 0;

		while (option >= 0) {
			option = JOptionPane.showOptionDialog(frame, mainPanel, "Enter credentials",
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

			ArrayList<String> errorMessages = new ArrayList<String>();

			// Get values from form
			String usernameString = username.getText();
			String passwordString = new String(password.getPassword());

			// Ask for confirm password if needed
			String confirmPassword = null;
			if (option == 1) {
				confirmPassword = PasswordScreen.getPassword();
				// Check for confirm password
				if (StringUtils.isBlank(confirmPassword)) {
					errorMessages.add("Confirm password can not be blank");
				}
				if (!confirmPassword.equals(passwordString)) {
					errorMessages.add("Password not confirmed");
				}
			}

			// Check for empty username
			if (StringUtils.isBlank(usernameString)) {
				errorMessages.add("Username can not be blank");
			}
			// Check for empty port and if the port is an integer
			if (StringUtils.isBlank(passwordString)) {
				errorMessages.add("Password can not be blank");
			}

			// Check if there were any errors, if not, the address is returned.
			if (errorMessages.isEmpty()) {
				frame.dispose();

				if (option == 0) {
					String[] result = new String[3];
					result[0] = "login";
					result[1] = usernameString;
					result[2] = HashUtils.hash(passwordString);
					return result;
				} else if (option == 1) {
					String[] result = new String[4];
					result[0] = "register";
					result[1] = username.getText();
					result[2] = HashUtils.hash(passwordString);
					result[3] = HashUtils.hash(confirmPassword);
					return result;
				}
			} else {
				// If there were errors, they are added to the dialog and it gets shown again.
				setErrorLabels(errorPanel, errorMessages);
			}
		}

		frame.dispose();

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

}
