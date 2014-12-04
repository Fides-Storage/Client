package org.fides.client.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.ui.ErrorMessageScreen;

/**
 * Contains the settings on where files should be saved.
 * 
 * @author Koen
 *
 */
public final class UserProperties {

	/**
	 * The directory to store settings
	 */
	public static final File SETTINGS_DIRECTORY = new File("./Settings");

	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(UserProperties.class);

	/**
	 * The default directory to store files
	 */
	private static final String DEFAULT_FILE_DIR = "./Fides";

	/**
	 * The file containing the user settings
	 */
	private static final String USER_SETTINGS_FILE = "user.properties";

	/**
	 * The key to get the file storage directory from the properties
	 */
	private static final String FILE_DIRECTORY_KEY = "FidesFiles";

	/**
	 * Singleton instance
	 */
	private static UserProperties instance;

	/**
	 * The properties used for storing the settings
	 */
	private Properties properties;

	/**
	 * The {@link File} representing the directory in which files can be saves
	 */
	private File fileDirectory;

	/**
	 * Constructor, reads the {@link UserProperties}
	 */
	private UserProperties() {
		properties = new Properties();

		if (!SETTINGS_DIRECTORY.exists()) {
			if (!SETTINGS_DIRECTORY.mkdirs()) {
				log.error("Could not create settings directory");
				ErrorMessageScreen.showErrorMessage("Could not create settings directory.", "Make sure you have the rights to create files.");
				System.exit(1);
			}
		}

		try {
			File file = new File(SETTINGS_DIRECTORY, USER_SETTINGS_FILE);
			if (file.exists()) {
				properties.load(new FileInputStream(file));
			}
		} catch (IOException e) {
			log.error(e);
		}

		// Create the a file referencing the the location were the files should be saved
		String fileDirectoryName = properties.getProperty(FILE_DIRECTORY_KEY);
		if (StringUtils.isBlank(fileDirectoryName)) {
			fileDirectoryName = DEFAULT_FILE_DIR;

			try {
				properties.setProperty(FILE_DIRECTORY_KEY, new File(fileDirectoryName).getCanonicalPath());
			} catch (IOException e) {
				log.debug(e);
			}

			saveProperties();
		}
		fileDirectory = new File(fileDirectoryName);
		if (!fileDirectory.exists()) {
			if (!fileDirectory.mkdirs()) {
				log.error("File directory can not be created");
				ErrorMessageScreen.showErrorMessage("File directory can not be created.", "Make sure you have the rights to create files");
				System.exit(1);
			}
		}
	}

	public File getFileDirectory() {
		return fileDirectory;
	}

	/**
	 * Save the properties
	 */
	private void saveProperties() {
		try (OutputStream out = new FileOutputStream(new File(SETTINGS_DIRECTORY, USER_SETTINGS_FILE))) {
			properties.store(out, "Fides user settings");
		} catch (IOException e) {
			log.error(e);
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
