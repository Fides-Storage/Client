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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(ChangeServerPanel.class);

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
		hostPortField.setText("" + UserProperties.getInstance().getHostPort());

		UiUtils.setMaxHeightToPreferred(hostAddressField);
		UiUtils.setMaxHeightToPreferred(hostPortField);
	}

	@Override
	public List<UserMessage> applySettings() {
		// Return if we did not change
		if (hostAddressField.getText().equals(UserProperties.getInstance().getHost()) &&
			hostPortField.getText().equals("" + UserProperties.getInstance().getHostPort())) {
			return null;
		}

		ArrayList<UserMessage> errorMessages = new ArrayList<>();

		InetSocketAddress serverAddress;

		// Get the server address
		serverAddress = getServerAddress(errorMessages);

		ServerConnector connector = encryptionManager.getConnector();

		// Continue if we don't have errors
		if (serverAddress != null && errorMessages.isEmpty()) {
			serverAddress = new InetSocketAddress(hostAddressField.getText(), Integer.parseInt(hostPortField.getText()));
			// Check if we don't have error and if we can connect
			if (errorMessages.isEmpty() && connectCheck(serverAddress, errorMessages)) {
				// Check the certificate
				X509Certificate certificate = certificateCheck(connector, errorMessages);
				connector.disconnect();
				// No errors? then save it
				if (certificate != null && errorMessages.isEmpty()) {
					System.out.println("BLAAA");
					if (AuthenticateUser.authenticateUser(connector)) {
						UserProperties.getInstance().setServerAddress(serverAddress);
						UserProperties.getInstance().setCertificate(certificate);
						LocalHashes.getInstance().removeAllHashes();
						boolean hasKeyFile = connector.checkIfKeyFileExists();
						if (!connector.checkIfKeyFileExists()) {
							encryptionManager.updateKeyFile(new KeyFile());
						}
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
		// Check for empty hostname
		if (StringUtils.isBlank(hostAddressField.getText())) {
			errorMessages.add(new UserMessage("Hostname can not be blank", true));
		}
		// Check for empty port and if the port is an integer
		if (StringUtils.isBlank(hostPortField.getText())) {
			errorMessages.add(new UserMessage("Port can not be blank", true));
		} else {
			try {
				// Check if the port is a valid port.
				int portInt = Integer.parseInt(hostPortField.getText());
				if (portInt < 0 || portInt > 65535) {
					errorMessages.add(new UserMessage("Port has to be a valid port", true));
				}
				return new InetSocketAddress(hostAddressField.getText(), Integer.parseInt(hostPortField.getText()));
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

}
