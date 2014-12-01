package org.fides.client.ui;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * A screen for displaying the user an error message.
 */
public class ErrorMessageScreen {

	/**
	 * Shows the user a message dialog with one or more error messages
	 * @param lines The error messages to show
	 */
	public static void showErrorMessage(String... lines) {
		JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
		
		// Create a Panel
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<html>");
		for (String line : lines) {
			stringBuilder.append(line);
			stringBuilder.append("<br>");
		}
		stringBuilder.append("</html>");
		
		JOptionPane.showMessageDialog(frame, stringBuilder.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		frame.dispose();
	}
}
