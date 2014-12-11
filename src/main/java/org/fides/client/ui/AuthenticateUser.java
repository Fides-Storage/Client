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
import org.fides.client.tools.UserProperties;
import org.fides.components.Actions;
import org.fides.tools.HashUtils;

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

	private static final int LOGIN = 0;

	private static final int REGISTER = 1;

	private static final int CANCEL = 2;

	/**
	 * Authenticate user, you can login and register
	 * 
	 * @param serverConnector
	 *            connection to server
	 * @return whether the user is authenticated or not
	 */
	public static boolean authenticateUser(ServerConnector serverConnector) {

		if (authenticateUserFromConfig(serverConnector)) {
			return true;
		}

		return authenticateUserWithUi(serverConnector);

	}

	private static boolean authenticateUserWithUi(ServerConnector serverConnector) {
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
		int option = LOGIN;

		while (option == LOGIN || option == REGISTER || option == CANCEL) {
			option = JOptionPane.showOptionDialog(frame, mainPanel, "Enter credentials", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

			// Get values from form
			String usernameString = username.getText();
			String passwordString = new String(password.getPassword());
			String confirmPassword = new String(passwordConfirmation.getPassword());

			if (option == CANCEL) {
				if (labelPasswordConfirmation.isVisible()) {
					labelPasswordConfirmation.setVisible(false);
					passwordConfirmation.setVisible(false);
					passwordConfirmation.setText(null);
				} else {
					frame.dispose();
					return false;
				}
			} else {
				labelPasswordConfirmation.setVisible(!(option == LOGIN));
				passwordConfirmation.setVisible(!(option == LOGIN));
			}

			ArrayList<String> errorMessages = validate(option, serverConnector, usernameString, passwordString, confirmPassword);

			String usernameHashString = HashUtils.hash(usernameString);
			String passwordHashString = HashUtils.hash(passwordString);

			Boolean authenticated = execute(option, errorMessages, serverConnector, usernameHashString, passwordHashString);

			if (authenticated) {
				if (option == LOGIN) {
					frame.dispose();
					return true;
				} else if (option == REGISTER) {
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

	private static boolean authenticateUserFromConfig(ServerConnector serverConnector) {
		String usernameHashString = UserProperties.getInstance().getUsernameHash();
		String passwordHashString = UserProperties.getInstance().getPasswordHash();

		if (StringUtils.isNotEmpty(usernameHashString) && StringUtils.isNotEmpty(passwordHashString)) {
			return execute(LOGIN, new ArrayList<String>(), serverConnector, usernameHashString, passwordHashString);
		}

		return false;
	}

	private static boolean execute(int option, ArrayList<String> errorMessages, ServerConnector serverConnector, String usernameHashString, String passwordHashString) {
		// Check if there were any errors
		if (errorMessages.isEmpty()) {
			if (option == LOGIN) {
				if (serverConnector.login(usernameHashString, passwordHashString)) {
					log.debug("login successful");
					UserProperties.getInstance().setPasswordHash(passwordHashString);
					UserProperties.getInstance().setUsernameHash(usernameHashString);
					return true;
				} else {
					log.debug(serverConnector.getErrorMessage(Actions.LOGIN));
					errorMessages.add("Login failed: " + serverConnector.getErrorMessage(Actions.LOGIN));
				}
			}
			if (option == REGISTER) {
				if (serverConnector.register(usernameHashString, passwordHashString)) {
					log.debug("Register successful");
					errorMessages.add("Register successful");
					return true;
				} else {
					log.debug(serverConnector.getErrorMessage(Actions.CREATEUSER));
					errorMessages.add("Register failed: " + serverConnector.getErrorMessage(Actions.CREATEUSER));
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
