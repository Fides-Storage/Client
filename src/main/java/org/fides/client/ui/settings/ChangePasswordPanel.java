package org.fides.client.ui.settings;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPasswordField;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.encryption.InvalidPasswordException;
import org.fides.client.files.data.KeyFile;
import org.fides.client.ui.UiUtils;
import org.fides.client.ui.UserMessage;
import org.fides.tools.HashUtils;

/**
 * UI where a password can be changed by a user
 */
public class ChangePasswordPanel extends SettingsJPanel {
	private static final long serialVersionUID = 900999340999410699L;

	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(ChangePasswordPanel.class);

	private EncryptionManager encryptionManager = null;

	private String newValidatedPassword = null;

	private final JPasswordField passOld = new JPasswordField(10);

	private final JPasswordField passNew1 = new JPasswordField(10);

	private final JPasswordField passNew2 = new JPasswordField(10);

	/**
	 * Constructor for ChangePasswordScreen, this screen extends a JPanel and can be used to show a password change
	 * screen.
	 * 
	 * @param encryptionManager
	 *            the EncryptionManager that is used for requesting and updating KeyFiles
	 */
	public ChangePasswordPanel(EncryptionManager encryptionManager) {
		super("Change password");
		this.encryptionManager = encryptionManager;

		// Set layout on panel
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		UiUtils.setMaxHeightToPreferred(passOld);
		UiUtils.setMaxHeightToPreferred(passNew1);
		UiUtils.setMaxHeightToPreferred(passNew2);

		// Add a labels to the panel
		JLabel labelOld = new JLabel("Old password:");
		this.add(labelOld);
		this.add(passOld);

		JLabel labelNew1 = new JLabel("New password:");
		this.add(labelNew1);
		this.add(passNew1);

		JLabel labelNew2 = new JLabel("Confirm new password:");
		this.add(labelNew2);
		this.add(passNew2);
	}

	private ArrayList<UserMessage> validateSettings(String oldPassword, String newPassword1, String newPassword2) {
		// ArrayList of UserMessages that will be returned.
		ArrayList<UserMessage> messages = new ArrayList<>();

		// Check for empty passwords
		if (StringUtils.isBlank(oldPassword)) {
			messages.add(new UserMessage("Old password can not be blank", true));
		} else {
			// Validate the old password by decrypting the keyfile with the given password.
			KeyFile keyFile = null;
			try {
				encryptionManager.getConnector().connect();
				// check if key file can be decrypted with the old password
				keyFile = encryptionManager.requestKeyFile(HashUtils.hash(oldPassword));
			} catch (ConnectException | UnknownHostException e) {
				LOG.error(e);
			} catch (InvalidPasswordException e) {
				messages.add(new UserMessage("Old password is incorrect", true));
			} finally {
				encryptionManager.getConnector().disconnect();
			}

			// Decryption of keyfile failed, add error message.
			if (messages.isEmpty() && keyFile == null) {
				messages.add(new UserMessage("Could not connect to server", true));
			}
		}
		if (StringUtils.isBlank(newPassword1)) {
			messages.add(new UserMessage("New password can not be blank", true));
		} else if (!newPassword1.equals(newPassword2)) {
			messages.add(new UserMessage("Confirm password does not match", true));
		}

		// Set the validated password
		newValidatedPassword = newPassword1;

		// return true when there are no errors
		return messages;
	}

	/**
	 * Apply the password change. This will (1) request the keyfile, (2) change the password in the EncryptionManager
	 * and (3) upload the keyfile
	 * 
	 * @return whether the password change was successful or not
	 */
	@Override
	public ArrayList<UserMessage> applySettings() {

		// Get passwords from fields
		String oldPassword = new String(passOld.getPassword());
		String newPassword1 = new String(passNew1.getPassword());
		String newPassword2 = new String(passNew2.getPassword());

		// When nothing was filled in, do nothing
		if (StringUtils.isBlank(oldPassword) && StringUtils.isBlank(newPassword1) && StringUtils.isBlank(newPassword2)) {
			return null;
		}

		ArrayList<UserMessage> messages = validateSettings(oldPassword, newPassword1, newPassword2);
		if (messages.isEmpty() && newValidatedPassword != null && encryptionManager != null) {
			// Retrieve current KeyFile
			KeyFile keyFile = null;
			try {
				encryptionManager.getConnector().connect();
				keyFile = encryptionManager.requestKeyFile();
			} catch (ConnectException | UnknownHostException | InvalidPasswordException e) {
				LOG.error(e);
			}

			// Change the password in the EncryptionManager
			encryptionManager.setPassword(HashUtils.hash(newValidatedPassword));

			// Upload the KeyFile encrypted with the new password
			if (!encryptionManager.updateKeyFile(keyFile)) {
				messages.add(new UserMessage("Password change could be completed, try again later", true));
			}
			encryptionManager.getConnector().disconnect();
		}
		return messages;
	}
}
