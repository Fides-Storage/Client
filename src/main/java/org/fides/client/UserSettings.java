package org.fides.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Contains the settings on where files should be saved.
 * 
 * @author Koen
 *
 */
public final class UserSettings {

	private static final String USER_SETTINGS_FILE = "./fides.prop";

	private static final String FILE_DIRECTORY_KEY = "FidesFiles";

	private static UserSettings instance;

	private Properties properties;

	private File fileDirectory;

	/**
	 * Constructor, reads the {@link UserSettings}
	 */
	private UserSettings() {
		properties = new Properties();

		try {
			File file = new File(USER_SETTINGS_FILE);
			if (file.exists()) {
				properties.load(new FileInputStream(USER_SETTINGS_FILE));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create the a file referencing the the location were the files should be saved
		String fileDirectoryName = properties.getProperty(FILE_DIRECTORY_KEY);
		if (StringUtils.isBlank(fileDirectoryName)) {
			fileDirectoryName = System.getProperty("user.home") + "/Fides";

			try {
				properties.setProperty(FILE_DIRECTORY_KEY, new File(fileDirectoryName).getCanonicalPath());
			} catch (IOException e) {
				// Do Nothing
			}

			saveProperties();
		}
		fileDirectory = new File(fileDirectoryName);
		if (!fileDirectory.exists()) {
			fileDirectory.mkdirs();
		}
	}

	public File getFileDirectory() {
		return fileDirectory;
	}

	private void saveProperties() {
		OutputStream out = null;
		try {
			out = new FileOutputStream(new File(USER_SETTINGS_FILE));
			properties.store(out, "Fides user settings");
		} catch (IOException e1) {
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * Returns the instance of the {@link UserSettings}, this is a singleton. If the {@link UserSettings} are not loaded
	 * they will be.
	 * 
	 * @return The instance of the {@link UserSettings}
	 */
	public static UserSettings getInstance() {
		if (instance == null) {
			instance = new UserSettings();
		}
		return instance;
	}
}
