package org.fides.client.ui;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.fides.components.UserMessage;

public class UiUtils {

	public static void setMessageLabels(JPanel messagePanel, ArrayList<UserMessage> messages)
	{
		messagePanel.removeAll();
		messagePanel.setVisible(true);

		for (UserMessage message : messages) {
			JLabel messageLabel = new JLabel();
			messageLabel.setText(message.message);
			if (message.error) {
				messageLabel.setForeground(Color.red);
			} else {
				messageLabel.setForeground(Color.green);
			}
			messagePanel.add(messageLabel);
		}
	}
}
