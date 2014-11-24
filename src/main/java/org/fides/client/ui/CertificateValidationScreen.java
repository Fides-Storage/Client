package org.fides.client.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.security.cert.X509Certificate;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/**
 * UI where a password can be submitted by a user
 */
public class CertificateValidationScreen {

	/**
	 * Show a dialog where the user can enter its password
	 *
	 * @return the entered password, returns null if nothing was entered
	 */
	public static boolean validateCertificate(X509Certificate certificate) {
		// Create a Panel
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		JTextArea certificateInfo = new JTextArea(readableCertificate(certificate));
		panel.add(certificateInfo);

		// Add a label to the panel
		JLabel label = new JLabel("Password:");
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(label);

		// Place the 2 buttons for OK and Cancel and show the dialog
		String[] options = new String[] { "Decline", "Accept" };
		int option = JOptionPane.showOptionDialog(null, panel, "Enter password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		
		// If Decline was pressed
		if (options[option].equals("Accept")) {
			return true;
		}
		return false;
	}
	
	private static String readableCertificate(X509Certificate certificate) {
		StringBuilder builder = new StringBuilder();
		builder.append("1");
		builder.append(System.lineSeparator());
		builder.append("2");
		
		return builder.toString();
		
	}
	

}
