package org.fides.client.ui.settings;

import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.files.FileCheckTask;
import org.fides.client.files.FileSyncManager;
import org.fides.client.tools.UserProperties;
import org.fides.client.ui.UserMessage;

/**
 * UI where the check time can be changed by a user
 */
public class CheckTimePanel extends SettingsJPanel {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(CheckTimePanel.class);

	private final JTextField checkTimeField = new JTextField();

	private final FileSyncManager syncManager;

	/**
	 * Constructor, creates the panel
	 */
	public CheckTimePanel(FileSyncManager syncManager) {
		super("Check time");
		this.syncManager = syncManager;

		// Set layout on panel
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		checkTimeField.setText("" + UserProperties.getInstance().getCheckTimeInSeconds());

		// Add a labels to the panel
		this.add(new JLabel("Check time (seconds):"));
		this.add(checkTimeField);
	}

	@Override
	public ArrayList<UserMessage> applySettings() {
		// ArrayList of UserMessages that will be returned.
		ArrayList<UserMessage> messages = new ArrayList<>();
		String text = checkTimeField.getText();

		if (StringUtils.isBlank(text)) {
			return messages;
		}

		try {
			// Can we parse it
			int value = Integer.parseInt(checkTimeField.getText());
			// Update it
			UserProperties.getInstance().setCheckTimeInSeconds(value);
			// Restart the timer
			FileCheckTask.startCheckTimer(syncManager);
		} catch (NumberFormatException e) {
			// Add an error message, we cannot parse it!
			messages.add(new UserMessage("Check time is no number", true));
		}

		return messages;
	}

}
