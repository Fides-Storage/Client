package org.fides.client.ui;

import java.awt.Component;
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
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

/**
 * UI where a password can be submitted by a user
 */
public class PasswordScreen {

	/**
	 * Show a dialog where the user can enter its password
	 * 
	 * @return the entered password, returns null if nothing was entered
	 */
	public static String getPassword(ArrayList<UserMessage> messages, boolean confirmPassword) {
		JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);

		// Create a Panel
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		// Add a panel where errors can be shown later
		JPanel messagePanel = new JPanel();
		messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
		messagePanel.setVisible(false);
		panel.add(messagePanel);

		UiUtils.setMessageLabels(messagePanel, messages);

		// Add a label to the panel
		JLabel passwordLabel = new JLabel("Password:");
		panel.add(passwordLabel);

		// Add a passwordfield to the panel with a coloumn with of 10
		JPasswordField pass = new JPasswordField(10);
		panel.add(pass);

		// Add a label to the panel
		JLabel passwordConfrimlabel = new JLabel("Confirm password:");
		passwordConfrimlabel.setVisible(confirmPassword);
		panel.add(passwordConfrimlabel);

		// Add a passwordfield to the panel with a coloumn with of 10
		JPasswordField passConfirm = new JPasswordField(10);
		passConfirm.setVisible(confirmPassword);
		panel.add(passConfirm);

		// Make sure that the password field is selected while it is still possible to press enter for OK
		pass.addHierarchyListener(new HierarchyListener() {
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
		int option = 0;

		while (option == 0) {
			option = JOptionPane.showOptionDialog(frame, panel, "Enter password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

			// If OK was pressed
			if (option == 0) {

				String passwordString = new String(pass.getPassword());

				if (confirmPassword) {

					String passwordConfirmString = new String(passConfirm.getPassword());

					if (!StringUtils.isBlank(passwordString) && !StringUtils.isBlank(passwordConfirmString) && passwordString.equals(passwordConfirmString)) {
						frame.dispose();
						return new String(passwordString);
					} else {
						messages.clear();
						messages.add(new UserMessage("Confirm password is incorrect", true));
						UiUtils.setMessageLabels(messagePanel, messages);
					}

				} else {
					frame.dispose();
					return new String(passwordString);
				}
			}
		}

		frame.dispose();
		return null;
	}
}
