package org.fides.client.ui.settings;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.fides.client.connector.ServerConnector;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.files.data.KeyFile;
import org.fides.client.tools.CertificateUtil;
import org.fides.client.tools.LocalHashes;
import org.fides.client.tools.UserProperties;
import org.fides.client.ui.AuthenticateUser;
import org.fides.client.ui.CertificateValidationScreen;
import org.fides.client.ui.UiUtils;
import org.fides.client.ui.UserMessage;

/**
 * UI where the server can be changed by a user
 */
public class ChangeServerPanel extends SettingsJPanel {
	private static final long serialVersionUID = -4725396003359289714L;

	private final JTextField hostAddressField = new JTextField();

	private final JTextField hostPortField = new JTextField();

	private final EncryptionManager encryptionManager;

	/**
	 * Constructor, sets up the panel
	 */
	public ChangeServerPanel(EncryptionManager encryptionManager) {
		super("Server");
		this.encryptionManager = encryptionManager;
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		this.add(new JLabel("Hostname:"));
		this.add(hostAddressField);
		this.add(new JLabel("Port:"));
		this.add(hostPortField);

		hostAddressField.setText(UserProperties.getInstance().getHost());
		hostPortField.setText(Integer.toString(UserProperties.getInstance().getHostPort()));

		UiUtils.setMaxHeightToPreferred(hostAddressField);
		UiUtils.setMaxHeightToPreferred(hostPortField);
	}

	@Override
	public List<UserMessage> applySettings() {
		// Return if we did not change
		if (StringUtils.equals(hostAddressField.getText(), UserProperties.getInstance().getHost()) &&
			StringUtils.equals(hostPortField.getText(), Integer.toString(UserProperties.getInstance().getHostPort()))) {
			return null;
		}

		ArrayList<UserMessage> errorMessages = new ArrayList<>();

		InetSocketAddress serverAddress;

		// Get the server address
		serverAddress = getServerAddress(errorMessages);

		ServerConnector connector = encryptionManager.getConnector();

		// Continue if we don't have errors
		if (serverAddress != null && errorMessages.isEmpty()) {
			// Check if we don't have error and if we can connect
			if (errorMessages.isEmpty() && connectCheck(serverAddress, errorMessages)) {
				// Check the certificate
				X509Certificate certificate = certificateCheck(connector, errorMessages);

				// No errors? then save it
				if (certificate != null && errorMessages.isEmpty()) {
					if (AuthenticateUser.authenticateUser(connector)) {
						UserProperties.getInstance().setServerAddress(serverAddress);
						UserProperties.getInstance().setCertificate(certificate);
						LocalHashes.getInstance().removeAllHashes();
						keyFileCheck();
					} else {
						errorMessages.add(new UserMessage("Could not authenticate user", true));
					}
				}
			}
			// Make sure we are not connected
			connector.disconnect();
		}
		return errorMessages;
	}

	/**
	 * Retrieves the {@link InetSocketAddress} from the UI
	 * 
	 * @param errorMessages
	 *            The list to add error messages to
	 * @return The {@link InetSocketAddress}
	 */
	private InetSocketAddress getServerAddress(List<UserMessage> errorMessages) {
		String hostString = hostAddressField.getText();
		String portString = hostPortField.getText();

		// Check for empty hostname
		if (StringUtils.isBlank(hostString)) {
			errorMessages.add(new UserMessage("Hostname can not be blank", true));
		}
		// Check for empty port and if the port is an integer
		if (StringUtils.isBlank(portString)) {
			errorMessages.add(new UserMessage("Port can not be blank", true));
		} else {
			try {
				// Check if the port is a valid port.
				int portInt = Integer.parseInt(portString);
				if (portInt < 0 || portInt > 65535) {
					errorMessages.add(new UserMessage("Port has to be a valid port", true));
				}
				return new InetSocketAddress(hostString, Integer.parseInt(portString));
			} catch (NumberFormatException e) {
				// We cannot parse it
				errorMessages.add(new UserMessage("Port has to be a valid number", true));
			}
		}
		return null;
	}

	/**
	 * Check if the a server at an {@link InetSocketAddress} is available
	 * 
	 * @param serverAddress
	 *            The address to check
	 * @param errorMessages
	 *            The list to add error messages to
	 * @return true if connected, else false
	 */
	private boolean connectCheck(InetSocketAddress serverAddress, List<UserMessage> errorMessages) {
		try {
			if (serverAddress != null) {
				encryptionManager.getConnector().init(serverAddress);
			}
			return true;
		} catch (UnknownHostException | ConnectException e) {
			errorMessages.add(new UserMessage(String.format("Could not connect to %s : %s", serverAddress.getHostName(), serverAddress.getPort()), true));
			return false;
		}
	}

	/**
	 * Retrieves a certificate and validates it
	 * 
	 * @param serverConnector
	 *            The {@link ServerConnector} to use
	 * @param errorMessages
	 *            The list to add error messages to
	 * @return The certificate if validated
	 */
	private X509Certificate certificateCheck(ServerConnector serverConnector, List<UserMessage> errorMessages) {
		Certificate[] certificates = serverConnector.getServerCertificates();

		if (certificates.length > 0) {
			X509Certificate certificate = (X509Certificate) certificates[0];

			if (!CertificateUtil.checkValidCertificate(certificate)) {
				errorMessages.add(new UserMessage("Server certificate not valid", true));
			} else if (!CertificateValidationScreen.validateCertificate(certificate)) {
				errorMessages.add(new UserMessage("Server certificate not accepted", true));
			}
			return certificate;
		}
		return null;
	}

	/**
	 * Creates a {@link KeyFile} is one does not exist for the current user
	 */
	private void keyFileCheck() {
		boolean hasKeyFile = encryptionManager.getConnector().checkIfKeyFileExists();
		if (!hasKeyFile) {
			encryptionManager.updateKeyFile(new KeyFile());
		}
	}

}
