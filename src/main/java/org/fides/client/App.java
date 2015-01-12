package org.fides.client;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

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
import org.fides.client.tools.UserProperties;
import org.fides.client.ui.AuthenticateUser;
import org.fides.client.ui.CertificateValidationScreen;
import org.fides.client.ui.ErrorMessageScreen;
import org.fides.client.ui.PasswordScreen;
import org.fides.client.ui.ServerAddressScreen;
import org.fides.client.ui.UserMessage;
import org.fides.tools.HashUtils;

/**
 * Main class for the client application
 * 
 */
public class App {

	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(App.class);

	/**
	 * Main
	 * 
	 * @param args
	 *            The arguments passed on to the application
	 */
	public static void main(String[] args) {
		ServerConnector serverConnector = new ServerConnector();

		InetSocketAddress serverAddress = newServerConnection(serverConnector);

		if (serverAddress == null) {
			System.exit(1);
		}

		try {
			serverConnector.init(serverAddress);
		} catch (ConnectException | UnknownHostException e) {
			LOG.error(e);
			System.exit(1);
		}

		Boolean isAuthenticated = AuthenticateUser.authenticateUser(serverConnector);

		if (isAuthenticated && serverConnector.isConnected()) {

			EncryptionManager encManager = null;

			boolean isAccepted = false;
			ArrayList<UserMessage> messages = new ArrayList<>();
			while (!isAccepted) {

				/**
				 * Do normal work, we are going to loop here
				 */

				// Check if the user already has a keyfile.
				boolean hasKeyFile = serverConnector.checkIfKeyFileExists();

				String passwordString = PasswordScreen.getPassword(messages, !hasKeyFile);
				if (StringUtils.isNotBlank(passwordString)) {
					passwordString = HashUtils.hash(passwordString);
				} else {
					System.exit(0);
				}

				encManager = new EncryptionManager(serverConnector, passwordString);

				if (!hasKeyFile) {
					encManager.updateKeyFile(new KeyFile());
				}

				// check if key file can be decrypted
				KeyFile keyFile = encManager.requestKeyFile();

				if (keyFile != null) {
					isAccepted = true;
				} else {
					messages.clear();
					messages.add(new UserMessage("Password is incorrect", true));
				}

			}

			// Disconnect after login
			serverConnector.disconnect();

			FileManager fileManager = new FileManager();
			FileSyncManager syncManager = new FileSyncManager(fileManager, encManager);
			LocalFileChecker checker = new LocalFileChecker(syncManager);
			checker.start();

			Timer timer = new Timer("CheckTimer");
			long timeCheck = TimeUnit.SECONDS.toMillis(UserProperties.getInstance().getCheckTimeInSeconds());
			timer.scheduleAtFixedRate(new FileCheckTask(syncManager), 0, timeCheck);

		}

	}

	private static InetSocketAddress newServerConnection(ServerConnector serverConnector) {
		InetSocketAddress serverAddress = null;
		X509Certificate certificate = null;
		boolean validCertificate = false;

		while (!validCertificate) {
			boolean connected = false;
			while (!connected) {

				serverAddress = getServerAddress();
				try {
					if (serverAddress != null) {
						connected = serverConnector.init(serverAddress);
					} else {
						break;
					}
				} catch (UnknownHostException e) {
					ErrorMessageScreen.showErrorMessage("Could not connect to host " + serverAddress.getHostName());
					break;
				} catch (ConnectException e) {
					ErrorMessageScreen.showErrorMessage("Could not connect to " + serverAddress.getHostName() + ":" + serverAddress.getPort());
					break;
				}
			}

			if (!connected) {
				return null;
			}

			Certificate[] certificates = serverConnector.getServerCertificates();
			serverConnector.disconnect();

			if (certificates.length > 0) {
				certificate = (X509Certificate) certificates[0];

				if (!checkValidCertificate(certificate)) {
					ErrorMessageScreen.showErrorMessage("Server certificate not valid!!!!");
					System.exit(1);
				}

				validCertificate = checkCertificateAccepted(certificate);

			}
		}

		// success, save address and certificate to config
		UserProperties.getInstance().setServerAddress(serverAddress);
		UserProperties.getInstance().setCertificate(certificate);

		return serverAddress;
	}

	private static InetSocketAddress getServerAddress() {
		String host = UserProperties.getInstance().getHost();
		int hostPort = UserProperties.getInstance().getHostPort();

		if (StringUtils.isNotBlank(host) && hostPort >= 1 && hostPort <= 65535) {
			return new InetSocketAddress(host, hostPort);
		} else {
			return ServerAddressScreen.getAddress();
		}
	}

	/**
	 * Check certificate if same as user settings
	 * 
	 * @param certificate
	 *            the given certificate
	 * @return if certificate is the same
	 */
	private static boolean checkCertificateAccepted(X509Certificate certificate) {
		// Check saved certificate with current one
		String certificateId = UserProperties.getInstance().getCertificateId();
		String certificateIssuer = UserProperties.getInstance().getCertificateIssuer();
		if (StringUtils.isNotBlank(certificateId) && StringUtils.isNotBlank(certificateIssuer)) {
			return certificate.getSerialNumber().toString().equals(certificateId) && certificate.getIssuerX500Principal().getName().equals(certificateIssuer);
		}

		return CertificateValidationScreen.validateCertificate(certificate);
	}

	private static boolean checkValidCertificate(X509Certificate certificate) {

		try {
			certificate.checkValidity();
			// The rest of the checks are done by SSLSocket, if failed the socket is closed
			return true;
		} catch (CertificateExpiredException e) {
			LOG.error(e);
		} catch (CertificateNotYetValidException e) {
			LOG.error(e);
		}

		return false;
	}
}