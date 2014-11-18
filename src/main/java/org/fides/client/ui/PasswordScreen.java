package org.fides.client.ui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;

/**
 * UI where a password can be submitted by a user
 */
public class PasswordScreen extends JFrame {

	/**
	 * Show a dialog where the user can enter its password
	 * @return the entered password, returns an empty String if nothing was entered
	 */
	public static String getPassword() {
		//Create a Panel
		JPanel panel = new JPanel();
		//Add a label to the panel
		JLabel label = new JLabel("Enter a password:");
		panel.add(label);
		//Add a passwordfield to the panel
		JPasswordField pass = new JPasswordField(32);
		panel.add(pass);
		//Place the 2 buttons for OK and Cancel and show the dialog
		String[] options = new String[]{"OK", "Cancel"};
		int option = JOptionPane.showOptionDialog(null, panel, "Enter password",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
			null, options, options[0]);
		//When OK was pressed
		if (option == 0) {
			return new String(pass.getPassword());
		}
		return "";
	}

}
