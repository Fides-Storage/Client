package org.fides.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Properties;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.connector.ServerConnector;
import org.fides.client.files.FileCompareResult;
import org.fides.client.files.FileManager;
import org.fides.client.files.KeyFile;
import org.fides.client.ui.CertificateValidationScreen;
import org.fides.client.ui.ErrorMessageScreen;
import org.fides.client.ui.ServerAddressScreen;
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

	private static boolean isRunning = true;

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ServerConnector serverConnector = new ServerConnector();
		InetSocketAddress serverAddress = newServerConnection(serverConnector);

		// TODO: move this away from here, its pretty big
		while (isRunning) {

			String[] data = UsernamePasswordScreen.getUsernamePassword();

			// User ask to close application
			if (data == null) {
				isRunning = false;
			}

			try {
				serverConnector.connect(serverAddress);
			} catch (Exception e) {
				log.error(e);
				isRunning = false;
			}

			if (isRunning && (data[0]).equals("register")) {

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
			} else if (isRunning && (data[0]).equals("login")) {
				if (serverConnector.login(data[1], data[2])) {
					log.debug("login successful");
					break;
				} else {
					log.debug("login failed");
					serverConnector.disconnect();
				}
			}

		}

		if (serverConnector.isConnected() && serverConnector.isLoggedIn() && isRunning) {
			// TODO Do normal work, we are going to loop here
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static InetSocketAddress newServerConnection(ServerConnector serverConnector) {
		InetSocketAddress serverAddress = null;
		boolean validCertificate = false;
		while (!validCertificate) {
			boolean connected = false;
			while (!connected) {
				serverAddress = ServerAddressScreen.getAddress();
				try {
					if (serverAddress != null) {
						connected = serverConnector.connect(serverAddress);
					} else {
						break;
					}
				} catch (UnknownHostException e) {
					ErrorMessageScreen.showErrorMessage("Could not connect to host " + serverAddress.getHostName());
				} catch (ConnectException e) {
					ErrorMessageScreen.showErrorMessage("Could not connect to " + serverAddress.getHostName() + ":" + serverAddress.getPort());
				}
			}
			if (!connected) {
				return null;
			}

			Certificate[] certificates = serverConnector.getServerCertificates();
			serverConnector.disconnect();

			if (certificates.length > 0) {
				validCertificate = CertificateValidationScreen.validateCertificate((X509Certificate) certificates[0]);
			}
		}
		return serverAddress;
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