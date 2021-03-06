package org.fides.client.ui.settings;

import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.fides.client.tools.UserProperties;
import org.fides.client.ui.UiUtils;
import org.fides.client.ui.UserMessage;

/**
 * UI where the check interval can be changed by a user
 */
public class CheckIntervalPanel extends SettingsJPanel {
	private static final long serialVersionUID = 6834169516638826769L;

	private final JTextField checkTimeField = new JTextField();

	/**
	 * Constructor, creates the panel
	 */
	public CheckIntervalPanel() {
		super("Check interval");

		// Set layout on panel
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		// Add a labels to the panel
		this.add(new JLabel("Check interval (seconds):"));
		this.add(checkTimeField);

		checkTimeField.setText(Long.toString(UserProperties.getInstance().getCheckTimeInSeconds()));

		// To prevent stretching
		UiUtils.setMaxHeightToPreferred(checkTimeField);
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
		} catch (NumberFormatException e) {
			// Add an error message, we cannot parse it!
			messages.add(new UserMessage("Check time is no number", true));
		}

		return messages;
	}

}
