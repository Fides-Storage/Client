package org.fides.client.ui;

import javax.swing.JOptionPane;

public class ErrorMessageScreen {

	public static void showErrorMessage(String... lines) {
		// Create a Panel
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<html>");
		for (String line : lines) {
			stringBuilder.append(line);
			stringBuilder.append("<br>");
		}
		stringBuilder.append("</html>");
		
		JOptionPane.showMessageDialog(null, stringBuilder.toString(), "Error", JOptionPane.ERROR_MESSAGE);
	}
}
