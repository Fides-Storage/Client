package org.fides.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.connector.ServerConnector;
import org.fides.client.files.FileCompareResult;
import org.fides.client.files.FileManager;
import org.fides.client.files.KeyFile;
import org.fides.client.ui.UsernamePasswordScreen;

/**
 * Client application
 * 
 */
public class App {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(App.class);

	private static final String LOCAL_HASHSES_FILE = "./hashes.prop";

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ServerConnector serverConnector = new ServerConnector();

		// TODO: Check settings for IP&Port

		// TODO: Ask user for IP&Port

		// TODO: Get locally saved certificate

		// TODO: Compare certificates

		// TODO: If new: user prompt.

		// TODO: If user doesn't accept certificate: return false.

		while (true) {

			String[] data = UsernamePasswordScreen.getUsernamePassword();

			if (data == null) {
				System.exit(1);
			}

			try {
				serverConnector.connect("127.0.0.1", 4444);
			} catch (Exception e) {
				System.exit(1);
			}

			if ((data[0]).equals("register")) {

				// checks if password and password confirmation is the same
				if (data[2].equals(data[3])) {
					// register on the server
					if (serverConnector.register(data[1], data[2])) {
						log.debug("Register successful");
						serverConnector.disconnect();
					} else {
						log.debug("Register failed");
						serverConnector.disconnect();
					}
				} else {
					log.debug("Register password confirmation is not valid.");
				}
			} else if ((data[0]).equals("login")) {
				if (serverConnector.login(data[1], data[2])) {
					log.debug("login successful");
					break;
				} else {
					log.debug("login failed");
					serverConnector.disconnect();
				}
			}

		}

		if (serverConnector.isConnected()) {
			// TODO: Do normal work, we are going to loop here

		} else {
			System.exit(1);

		}

		serverConnector.disconnect();

	}

	/**
	 * Check the differences between the files local and on the server
	 */
	private static void fileManagerCheck() {
		// TODO make it the real code, not half test code
		Properties localHashes = new Properties();
		try (InputStream in = new FileInputStream(LOCAL_HASHSES_FILE)) {
			File file = new File(LOCAL_HASHSES_FILE);
			if (file.exists()) {
				localHashes.loadFromXML(in);
			}
		} catch (IOException e) {
			log.error(e);
		}

		FileManager manager = new FileManager(localHashes); // TODO maybe remember it
		KeyFile keyFile = new KeyFile(); // TODO get from the server

		Collection<FileCompareResult> results = manager.compareFiles(keyFile);
		log.debug(results);
	}
}