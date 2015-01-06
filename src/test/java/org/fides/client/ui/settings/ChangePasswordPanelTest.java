package org.fides.client.ui.settings;

import org.fides.client.connector.ServerConnector;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.files.data.KeyFile;
import org.fides.tools.HashUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.swing.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for ChangePasswordPanel
 */
@RunWith(PowerMockRunner.class)
public class ChangePasswordPanelTest {

	private static ChangePasswordPanel changePasswordPanel = null;

	private static final String CORRECT_PASSWORD = "Correct Password";

	private static final String INCORRECT_PASSWORD = "Incorrect Password";

	/**
	 * This beforeClass function will set the majority of mocks for this testing class
	 */
	@BeforeClass
	public static void beforeClass() {
		// Mock KeyFile
		KeyFile keyFile = mock(KeyFile.class);

		// Mock EncryptionManager
		EncryptionManager encryptionManager = mock(EncryptionManager.class);
		encryptionManager.setPassword(HashUtils.hash(CORRECT_PASSWORD));
		when(encryptionManager.requestKeyFile()).thenReturn(keyFile);
		when(encryptionManager.requestKeyFile(Mockito.matches(HashUtils.hash(CORRECT_PASSWORD)))).thenReturn(keyFile);
		when(encryptionManager.requestKeyFile(Mockito.matches(HashUtils.hash(INCORRECT_PASSWORD)))).thenReturn(null);
		when(encryptionManager.updateKeyFile(Mockito.any(KeyFile.class))).thenReturn(true);

		// Mock ServerConnector
		ServerConnector serverConnector = mock(ServerConnector.class);
		when(encryptionManager.getConnector()).thenReturn(serverConnector);

		changePasswordPanel = new ChangePasswordPanel(encryptionManager);
	}

	/**
	 * With this function you can set the passwords in the JPasswordFields in the ChangePasswordPanel
	 * 
	 * @param oldPassword
	 *            The old password where the keyfile is encrypted with
	 * @param newPassword1
	 *            The new password where the keyfile should be encrypted with
	 * @param newPassword2
	 *            The confirmation of the new password
	 */
	private void setPasswords(String oldPassword, String newPassword1, String newPassword2) {
		JPasswordField oldPasswordField = mock(JPasswordField.class);
		when(oldPasswordField.getPassword()).thenReturn(oldPassword.toCharArray());
		Whitebox.setInternalState(changePasswordPanel, "passOld", oldPasswordField);

		JPasswordField newPassword1Field = mock(JPasswordField.class);
		when(newPassword1Field.getPassword()).thenReturn(newPassword1.toCharArray());
		Whitebox.setInternalState(changePasswordPanel, "passNew1", newPassword1Field);

		JPasswordField newPassword2Field = mock(JPasswordField.class);
		when(newPassword2Field.getPassword()).thenReturn(newPassword2.toCharArray());
		Whitebox.setInternalState(changePasswordPanel, "passNew2", newPassword2Field);
	}

	/**
	 * This test will test the ApplySettings with all correct passwords
	 */
	@Test
	public void testApplySettingsCorrectPasswords() {
		setPasswords(CORRECT_PASSWORD, "Correct New Password", "Correct New Password");
		assertTrue(changePasswordPanel.applySettings().isEmpty());
	}

	/**
	 * This test will test the ApplySettings with no changes.
	 */
	@Test
	public void testApplySettingsNoChanges() {
		setPasswords("", "", "");
		assertNull(changePasswordPanel.applySettings());
	}

	/**
	 * This test will test the ApplySettings with an incorrect old password
	 */
	@Test
	public void testApplySettingsIncorrectOldPassword() {
		setPasswords(INCORRECT_PASSWORD, "Correct New Password", "Correct New Password");
		assertEquals(changePasswordPanel.applySettings().size(), 1);
	}

	/**
	 * This test will test the ApplySettings with an incorrect confirmation password
	 */
	@Test
	public void testApplySettingsIncorrectConfirmPassword() {
		setPasswords(CORRECT_PASSWORD, "Correct New Password", "Incorrect New Password");
		assertEquals(changePasswordPanel.applySettings().size(), 1);
	}

	/**
	 * This test will test the ApplySettings with a blank new password
	 */
	@Test
	public void testApplySettingsBlankNewPassword() {
		setPasswords(CORRECT_PASSWORD, "", "Correct New Password");
		assertEquals(changePasswordPanel.applySettings().size(), 1);
	}
}
