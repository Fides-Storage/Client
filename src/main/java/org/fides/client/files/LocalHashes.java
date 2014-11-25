package org.fides.client.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Used for loading and storing local hashes
 * 
 * @author Koen
 *
 */
public final class LocalHashes {

	private static final String LOCAL_HASHSES_FILE = "./hashes.xml";

	private static LocalHashes instance;

	private Properties localHashes;

	/**
	 * Constuctor
	 */
	private LocalHashes() {
		localHashes = new Properties();
		try (InputStream in = new FileInputStream(LOCAL_HASHSES_FILE)) {
			File file = new File(LOCAL_HASHSES_FILE);
			if (file.exists()) {
				localHashes.loadFromXML(in);
			}
		} catch (IOException e) {
			e.printStackTrace();
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
	 * @param filename
	 *            The filename of the file
	 * @param hash
	 *            The has of the file
	 */
	public void setHash(String filename, String hash) {
		localHashes.setProperty(filename, hash);
		saveHashes();
	}

	private synchronized void saveHashes() {
		File file = new File(LOCAL_HASHSES_FILE);
		try (OutputStream out = new FileOutputStream(file)) {
			localHashes.storeToXML(out, "Local file hashes");
		} catch (IOException e) {
			// We accept this
			e.printStackTrace();
		}
	}

	/**
	 * Checks if a hash for a file exists
	 * 
	 * @param filename
	 *            The name of the file
	 * @return true if the hash exists
	 */
	public boolean containsHash(String filename) {
		return localHashes.containsKey(filename);
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