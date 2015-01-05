package org.fides.client.ui;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * UI utilities to modify the UI
 *
 */
public class UiUtils {

	/**
	 * Display all message to panel
	 * 
	 * @param messagePanel
	 *            panel to show the messages on
	 * @param messages
	 *            messages to show
	 */
	public static void setMessageLabels(JPanel messagePanel, ArrayList<UserMessage> messages)
	{
		messagePanel.removeAll();
		messagePanel.setVisible(true);

		for (UserMessage message : messages) {
			JLabel messageLabel = new JLabel();
			messageLabel.setText(message.getMessage());
			if (message.isError()) {
				messageLabel.setForeground(Color.red);
			} else {
				messageLabel.setForeground(Color.green);
			}
			messagePanel.add(messageLabel);
		}
	}
}
