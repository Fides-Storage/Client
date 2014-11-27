package org.fides.client.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

/**
 * UI where a username and password can be submitted by a user
 */
public class UsernamePasswordScreen {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(UsernamePasswordScreen.class);

	/**
	 * hash function to hash password
	 */
	private static final String HASH = "SHA-256";

	/**
	 * Show a dialog where the user can enter its username and password
	 * 
	 * @return the entered username and password in a string array, returns an null if nothing was entered
	 */
	public static String[] getUsernamePassword() {
		// Create a Panel with the correct dimensions
		JPanel panel = new JPanel();
		// TODO: Use boxlayout with BoxLayout.Y_AXIS
		panel.setPreferredSize(new Dimension(200, 65));

		// Add a label to the panel
		JLabel labelUsername = new JLabel("Username:");
		labelUsername.setBounds(10, 10, 80, 25);
		panel.add(labelUsername);

		// Add a usernamefield to the panel with a coloumn with of 10
		JTextField username = new JTextField(10);
		username.setBounds(100, 10, 160, 25);
		panel.add(username);

		// Add a label to the panel
		JLabel labelPassword = new JLabel("Password:");
		labelPassword.setBounds(10, 40, 80, 25);
		panel.add(labelPassword);

		// Add a passwordfield to the panel with a coloumn with of 10
		JPasswordField password = new JPasswordField(10);
		password.setBounds(100, 40, 160, 25);
		panel.add(password);

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
		String[] options = new String[] { "Login", "Register" };
		int option = JOptionPane.showOptionDialog(null, panel, "Enter credentials",
			JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

		// If Login was pressed
		if (option == 0) {
			String[] result = new String[3];
			result[0] = "login";
			result[1] = username.getText();
			try {
				MessageDigest messageDigest = MessageDigest.getInstance(HASH);
				messageDigest.update(new String(password.getPassword()).getBytes());
				result[2] = Base64.toBase64String(messageDigest.digest());
			} catch (NoSuchAlgorithmException e) {
				log.error(e);
			}
			return result;
		} else if (option == 1) {
			String[] result = new String[4];
			result[0] = "register";
			result[1] = username.getText();

			try {
				MessageDigest messageDigest = MessageDigest.getInstance(HASH);
				messageDigest.update(new String(password.getPassword()).getBytes());
				result[2] = Base64.toBase64String(messageDigest.digest());
				messageDigest.update(new String(PasswordScreen.getPassword()).getBytes());
				result[3] = Base64.toBase64String(messageDigest.digest());
			} catch (NoSuchAlgorithmException e) {
				log.error(e);
			}
			return result;

		}

		return null;
	}
}
