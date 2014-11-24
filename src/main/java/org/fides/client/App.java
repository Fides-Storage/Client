package org.fides.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;

import org.fides.client.connector.ServerConnector;
import org.fides.client.files.FileCompareResult;
import org.fides.client.files.FileManager;
import org.fides.client.files.KeyFile;
import org.fides.client.ui.UsernamePasswordScreen;

/**
 * Hello world!
 *
 */
public class App {

	private static final String LOCAL_HASHSES_FILE = "./hashes.prop";

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ServerConnector serverConnector = new ServerConnector();

		while (true) {

			String[] data = UsernamePasswordScreen.getUsernamePassword();

			if (data == null) {
				System.exit(1);
			}

			if ((data[0]).equals("register")) {

				// checks if password and password confirmation is the same
				if (data[2].equals(data[3])) {
					// register on the server
					if (serverConnector.register(data[1], data[2])) {
						System.out.println("Register succesfull");
					} else {
						System.out.println("Register failed");
					}
				} else {
					System.out.println("Register password confirmation is not valid.");
				}
			} else if ((data[0]).equals("login")) {
				if (serverConnector.login(data[1], data[2])) {
					break;
				} else {
					System.out.println("Login failed");
				}
			}

		}

		if (serverConnector.isConnected()) {
			// TODO: Do normal work
		} else {
			System.exit(1);

		}

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
			e.printStackTrace();
		}

		FileManager manager = new FileManager(localHashes); // TODO maybe remember it
		KeyFile keyFile = new KeyFile(); // TODO get from the server

		Collection<FileCompareResult> results = manager.compareFiles(keyFile);
		System.out.println(results);
	}

}
