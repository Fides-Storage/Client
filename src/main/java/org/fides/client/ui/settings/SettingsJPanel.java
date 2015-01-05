package org.fides.client.ui.settings;

import javax.swing.JPanel;

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
	 * Validate the settings
	 *
	 * @return true if validated
	 */
	public abstract boolean validateSettings();

	/**
	 * Apply the settings
	 *
	 * @return true if succesful
	 */
	public abstract boolean applySettings();

	public String getName() {
		return name;
	}

}
