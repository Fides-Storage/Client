package org.fides.client.ui;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

/**
 * UI where a password can be submitted by a user
 */
public class PasswordScreen {

	/**
	 * Show a dialog where the user can enter its password
	 * 
	 * @return the entered password, returns null if nothing was entered
	 */
	public static String getPassword() {
		JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);

		// Create a Panel
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		// Add a label to the panel
		JLabel label = new JLabel("Password:");
		panel.add(label);

		// Add a passwordfield to the panel with a coloumn with of 10
		JPasswordField pass = new JPasswordField(10);
		panel.add(pass);

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
		int option = JOptionPane.showOptionDialog(frame, panel, "Enter password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		frame.dispose();

		// If OK was pressed
		if (option == 0) {
			return new String(pass.getPassword());
		}
		return null;
	}

}
