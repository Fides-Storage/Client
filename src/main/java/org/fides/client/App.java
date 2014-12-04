package org.fides.client;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.connector.ServerConnector;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.files.FileCheckTask;
import org.fides.client.files.FileManager;
import org.fides.client.files.FileSyncManager;
import org.fides.client.files.LocalFileChecker;
import org.fides.client.ui.CertificateValidationScreen;
import org.fides.client.ui.ErrorMessageScreen;
import org.fides.client.ui.ServerAddressScreen;
import org.fides.client.ui.UsernamePasswordScreen;

/**
 * Client application
 * 
 */
public class App {
	private static final long CHECK_TIME = 5 * 60 * 1000;

	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(App.class);

	private static boolean isRunning = true;

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ServerConnector serverConnector = new ServerConnector();
		InetSocketAddress serverAddress = newServerConnection(serverConnector);
		if (serverAddress == null) {
			System.exit(1);
		}

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

			FileManager fileManager = new FileManager();
			EncryptionManager encManager = new EncryptionManager(serverConnector, "Default");

			FileSyncManager syncManager = new FileSyncManager(fileManager, encManager);
			LocalFileChecker checker = new LocalFileChecker(syncManager);
			checker.start();

			Timer timer = new Timer("CheckTimer");
			timer.scheduleAtFixedRate(new FileCheckTask(syncManager), CHECK_TIME, CHECK_TIME);

			// try {
			// Thread.sleep(5000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
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

}