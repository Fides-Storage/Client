package org.fides.client.ui;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * UI where a username and password can be submitted by a user
 */
public class UsernamePasswordScreen {

	/**
	 * Show a dialog where the user can enter its username and password
	 *
	 * @return the entered username and password in a string array, returns an null if nothing was entered
	 */
	public static String[] getUsernamePassword() {
		// Create a Panel with the correct dimensions
		JPanel panel = new JPanel();
		// TODO: Use boxlayout with BoxLayout.Y_AXIS
		panel.setPreferredSize(new Dimension(100, 55));

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

		// Place the 2 buttons for OK and Cancel and show the dialog
		String[] options = new String[] { "OK", "Cancel" };
		int option = JOptionPane.showOptionDialog(null, panel, "Enter credentials", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		// If OK was pressed
		if (option == 0) {
			String[] result = new String[2];
			result[0] = username.getText();
			result[1] = new String(password.getPassword());
			return result;
		}
		return null;
	}

}
