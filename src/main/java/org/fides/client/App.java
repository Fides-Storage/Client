package org.fides.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.connector.ServerConnector;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.files.FileCheckTask;
import org.fides.client.files.FileManager;
import org.fides.client.files.FileSyncManager;
import org.fides.client.files.LocalFileChecker;
import org.fides.client.files.data.KeyFile;
import org.fides.client.ui.AuthenticateUser;
import org.fides.client.ui.CertificateValidationScreen;
import org.fides.client.ui.ErrorMessageScreen;
import org.fides.client.ui.PasswordScreen;
import org.fides.client.ui.ServerAddressScreen;

/**
 * Client application
 * 
 */
public class App {
	/**
	 * The time used to check changes with the server TODO: Shouldn't this be in the settings?
	 */
	private static final long CHECK_TIME = TimeUnit.MINUTES.toMillis(5);

	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(App.class);

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

		try {
			serverConnector.connect(serverAddress);
		} catch (ConnectException | UnknownHostException e) {
			log.error(e);
			System.exit(1);
		}

		Boolean isAuthenticated = AuthenticateUser.authenticateUser(serverConnector);

		if (isAuthenticated && serverConnector.isConnected()) {

			// TODO: Do normal work, we are going to loop here

			String passwordString = null;
			while (StringUtils.isBlank(passwordString)) {
				passwordString = PasswordScreen.getPassword();
			}

			FileManager fileManager = new FileManager();
			EncryptionManager encManager = new EncryptionManager(serverConnector, passwordString);
			serverConnector.disconnect();

			// Check if the user already has a keyfile.
			InputStream keyFileStream = serverConnector.requestKeyFile();
			if (keyFileStream != null) {
				try {
					if (keyFileStream.read() == -1) {
						System.out.println("No keyfile available, new key generated");
						serverConnector.disconnect();
						encManager.updateKeyFile(new KeyFile());
						encManager.getConnector().disconnect();
					} else {
						System.out.println("Keyfile already available on the server");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block, what to do when this fails?
					e.printStackTrace();
				} finally {
					IOUtils.closeQuietly(keyFileStream);
					serverConnector.disconnect();
				}
			}

			FileSyncManager syncManager = new FileSyncManager(fileManager, encManager);
			LocalFileChecker checker = new LocalFileChecker(syncManager);
			checker.start();

			Timer timer = new Timer("CheckTimer");
			timer.scheduleAtFixedRate(new FileCheckTask(syncManager), CHECK_TIME, CHECK_TIME);

			// TODO: We need to place this somewhere, but we do not know where jet
			// serverConnector.disconnect();

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