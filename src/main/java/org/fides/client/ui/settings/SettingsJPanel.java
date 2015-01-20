package org.fides.client.ui.settings;

import java.util.List;

import javax.swing.JPanel;

import org.fides.client.ui.UserMessage;

/**
 * A {@link JPanel} with additional methods for applying settings
 *
 */
public abstract class SettingsJPanel extends JPanel {

	private static final long serialVersionUID = -15736148359935264L;
	
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
	public abstract List<UserMessage> applySettings();

	public String getName() {
		return name;
	}

}
