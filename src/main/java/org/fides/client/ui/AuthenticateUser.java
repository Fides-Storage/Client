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
import org.fides.client.connector.ServerConnector;
import org.fides.client.tools.HashUtils;

/**
 * Authenticate user by asking username and password, or ask to register
 * 
 * @author jesse
 * 
 */
public class AuthenticateUser {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(AuthenticateUser.class);

	/**
	 * Authenticate user, you can login and register
	 * 
	 * @param serverConnector
	 *            connection to server
	 * @return whether the user is authenticated or not
	 */
	public static boolean authenticateUser(ServerConnector serverConnector) {

		JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);

		// Create a Panel with the correct dimensions
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		// Add a panel where the inputfields can be added
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new GridLayout(3, 1, 0, 5));

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

		// Add a label to the panel
		JLabel labelPasswordConfirmation = new JLabel("Password confirmation:");
		labelPasswordConfirmation.setVisible(false);
		inputPanel.add(labelPasswordConfirmation);

		// Add a passwordfield to the panel with a coloumn with of 10
		JPasswordField passwordConfirmation = new JPasswordField(10);
		passwordConfirmation.setVisible(false);
		inputPanel.add(passwordConfirmation);

		// Combines the inputpanel with the mainpanel
		mainPanel.add(inputPanel);

		// Make sure that the username field is selected while it is still possible to press enter for OK
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

		// Place the three buttons for login, register and cancel and show the dialog
		String[] options = new String[] { "Login", "Register", "Cancel" };
		int option = 0;

		while (option == 0 || option == 1 || option == 2) {
			option = JOptionPane.showOptionDialog(frame, mainPanel, "Enter credentials", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

			Boolean login = option == 0;
			Boolean register = option == 1;
			Boolean cancel = option == 2;

			// Get values from form
			String usernameString = username.getText();
			String passwordString = new String(password.getPassword());
			String confirmPassword = new String(passwordConfirmation.getPassword());

			if (cancel) {
				if (labelPasswordConfirmation.isVisible()) {
					labelPasswordConfirmation.setVisible(false);
					passwordConfirmation.setVisible(false);
					passwordConfirmation.setText(null);
				} else {
					frame.dispose();
					return false;
				}
			} else {
				labelPasswordConfirmation.setVisible(!login);
				passwordConfirmation.setVisible(!login);
			}

			ArrayList<String> errorMessages = validate(option, serverConnector, usernameString, passwordString, confirmPassword);

			Boolean authenticated = execute(option, errorMessages, serverConnector, usernameString, passwordString);

			if (authenticated) {
				if (login) {
					frame.dispose();
					return true;

				} else if (register) {
					labelPasswordConfirmation.setVisible(false);
					passwordConfirmation.setVisible(false);
				}
			}

			// If there were errors, they are added to the dialog and it gets shown again.
			setErrorLabels(errorPanel, errorMessages);
		}

		frame.dispose();

		return false;

	}

	private static Boolean execute(int option, ArrayList<String> errorMessages, ServerConnector serverConnector, String usernameString, String passwordString) {
		Boolean login = option == 0;
		Boolean register = option == 1;
		// Boolean cancel = option == 2;

		// Check if there were any errors
		if (errorMessages.isEmpty()) {
			if (login) {

				if (serverConnector.login(usernameString, HashUtils.hash(passwordString))) {
					log.debug("login successful");
					return true;
				} else {
					log.debug("login failed");
					errorMessages.add("login failed");
				}
			}
			if (register) {
				if (serverConnector.register(usernameString, HashUtils.hash(passwordString))) {
					log.debug("Register successful");
					errorMessages.add("Register successful");
					return true;
				} else {
					log.debug("Register failed");
					errorMessages.add("Register failed");
				}
			}
		}
		return false;
	}

	private static ArrayList<String> validate(int option, ServerConnector serverConnector, String usernameString, String passwordString, String confirmPassword) {

		ArrayList<String> errorMessages = new ArrayList<String>();

		// Check for empty username
		if (StringUtils.isBlank(usernameString)) {
			errorMessages.add("Username can not be blank");
		}
		// Check for empty port and if the port is an integer
		if (StringUtils.isBlank(passwordString)) {
			errorMessages.add("Password can not be blank");
		}

		// Ask for confirm password if needed
		if (option == 1) {
			// Check for confirm password
			if (StringUtils.isBlank(confirmPassword)) {
				errorMessages.add("Please confirm your password");
			}
			if (!StringUtils.isBlank(confirmPassword) && !confirmPassword.equals(passwordString)) {
				errorMessages.add("Password not confirmed");
			}
		}

		return errorMessages;
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
