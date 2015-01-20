package org.fides.client.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Used for loading and storing local hashes
 *
 */
public final class LocalHashes {
	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(LocalHashes.class);

	private static final String LOCAL_HASHES_FILE = "hashes.xml";

	private static LocalHashes instance;

	private final Properties localHashes = new Properties();

	/**
	 * Constructor for LocalHashes, checks if the hash file exists and loads the local hashes
	 */
	private LocalHashes() {
		File file = new File(UserProperties.SETTINGS_DIRECTORY, LOCAL_HASHES_FILE);
		if (file.exists()) {
			try (InputStream in = new FileInputStream(file)) {
				localHashes.loadFromXML(in);
			} catch (IOException e) {
				LOG.error(e);
			}
		}
	}

	/**
	 * Returns the hash for a file
	 * 
	 * @param filename
	 *            The name of the file
	 * @return The hash for the file, null if not existing
	 */
	public String getHash(String filename) {
		return localHashes.getProperty(filename);
	}

	/**
	 * Sets the hash for a file and saves the hashes
	 * 
	 * @param fileName
	 *            The filename of the file
	 * @param hash
	 *            The has of the file
	 */
	public void setHash(String fileName, String hash) {
		localHashes.setProperty(fileName, hash);
		saveHashes();
	}

	/**
	 * Saves the Properties with the current hashes
	 */
	private synchronized void saveHashes() {
		File file = new File(UserProperties.SETTINGS_DIRECTORY, LOCAL_HASHES_FILE);
		try (OutputStream out = new FileOutputStream(file)) {
			localHashes.storeToXML(out, "Local file hashes");
		} catch (IOException e) {
			// We accept this
			LOG.warn(e);
		}
	}

	/**
	 * Removes a hash from the local hashes
	 * 
	 * @param fileName
	 *            The filename of the hash
	 * @return Whether the remove was successful or not
	 */
	public boolean removeHash(String fileName) {
		if (fileName != null) {
			localHashes.remove(fileName);
			saveHashes();
			return true;
		}
		LOG.debug("Given filename was NULL");
		return false;
	}

	/**
	 * Removes all hashes from the local hashes
	 */
	public void removeAllHashes() {
		localHashes.clear();
		saveHashes();
	}

	/**
	 * Checks if a hash for a file exists
	 * 
	 * @param fileName
	 *            The name of the file
	 * @return true if the hash exists
	 */
	public boolean containsHash(String fileName) {
		return localHashes.containsKey(fileName);
	}

	/**
	 * Returns the instance of the {@link LocalHashes}, this is a singleton. If the {@link LocalHashes} are not loaded
	 * they will be.
	 * 
	 * @return The instance of the {@link LocalHashes}
	 */
	public static LocalHashes getInstance() {
		if (instance == null) {
			instance = new LocalHashes();
		}
		return instance;
	}
}
