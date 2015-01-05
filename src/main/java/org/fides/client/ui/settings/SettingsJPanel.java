package org.fides.client.ui.settings;

import org.fides.client.ui.UserMessage;

import javax.swing.JPanel;
import java.util.ArrayList;

/**
 * A {@link JPanel} with additional methods for applying settings
 *
 */
public abstract class SettingsJPanel extends JPanel {

	private final String name;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            The name of the panel
	 */
	public SettingsJPanel(String name) {
		super();
		this.name = name;
	}

	/**
	 * Apply the settings
	 *
	 * @return ArrayList with errorMessages if there were any
	 */
	public abstract ArrayList<UserMessage> applySettings();

	public String getName() {
		return name;
	}

}
