package org.fides.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

/**
 * Contains the settings on where files should be saved.
 * 
 * @author Koen
 *
 */
public final class UserProperties {

	private static final String DEFAULT_FILE_DIR = "./Fides";

	private static final String USER_SETTINGS_FILE = "./user.properties";

	private static final String FILE_DIRECTORY_KEY = "FidesFiles";

	private static UserProperties instance;

	private Properties properties;

	private File fileDirectory;

	/**
	 * Constructor, reads the {@link UserProperties}
	 */
	private UserProperties() {
		properties = new Properties();

		try {
			File file = new File(USER_SETTINGS_FILE);
			if (file.exists()) {
				properties.load(new FileInputStream(file));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create the a file referencing the the location were the files should be saved
		String fileDirectoryName = properties.getProperty(FILE_DIRECTORY_KEY);
		if (StringUtils.isBlank(fileDirectoryName)) {
			fileDirectoryName = DEFAULT_FILE_DIR;

			try {
				properties.setProperty(FILE_DIRECTORY_KEY, new File(fileDirectoryName).getCanonicalPath());
			} catch (IOException e) {
				// Do Nothing
			}

			saveProperties();
		}
		fileDirectory = new File(fileDirectoryName);
		if (!fileDirectory.exists()) {
			//TODO: check if the mkdir was successful
			fileDirectory.mkdirs();
		}
	}

	public File getFileDirectory() {
		return fileDirectory;
	}

	/**
	 * Save the properties
	 */
	private void saveProperties() {
		try (OutputStream out = new FileOutputStream(new File(USER_SETTINGS_FILE))) {
			properties.store(out, "Fides user settings");
		} catch (IOException e) {
			// Do nothing, can happen
		}
	}

	/**
	 * Returns the instance of the {@link UserProperties}, this is a singleton. If the {@link UserProperties} are not
	 * loaded they will be.
	 * 
	 * @return The instance of the {@link UserProperties}
	 */
	public static UserProperties getInstance() {
		if (instance == null) {
			instance = new UserProperties();
		}
		return instance;
	}
}
