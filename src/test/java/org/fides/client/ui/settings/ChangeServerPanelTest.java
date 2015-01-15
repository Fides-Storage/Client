package org.fides.client.ui.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.swing.JTextField;

import org.fides.client.connector.ServerConnector;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.files.data.KeyFile;
import org.fides.client.tools.CertificateUtil;
import org.fides.client.tools.LocalHashes;
import org.fides.client.tools.UserProperties;
import org.fides.client.ui.AuthenticateUser;
import org.fides.client.ui.CertificateValidationScreen;
import org.fides.client.ui.UserMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test for the {@link ChangeServerPanel}
 * 
 * @author Koen
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ UserProperties.class, LocalHashes.class, CertificateUtil.class, CertificateValidationScreen.class, AuthenticateUser.class })
@PowerMockIgnore({ "javax.management.*", "javax.swing.*", "java.security.*", "javax.security.*" })
public class ChangeServerPanelTest {

	private static final String IP = "123.456.789.1";

	private static final String PORT = "1234";

	private static final int PORT_INT = 1234;

	private ChangeServerPanel changeServerPanel;

	private EncryptionManager encryptionManagerMock;

	private ServerConnector serverConnectorMock;

	private UserProperties userPropertiesMock;

	private LocalHashes localHashesMock;

	private JTextField hostAddressFieldMock;

	private JTextField hostPortFieldMock;

	private X509Certificate certificateMock;

	/**
	 * Set up before each
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		PowerMockito.mockStatic(UserProperties.class);
		PowerMockito.mockStatic(LocalHashes.class);
		PowerMockito.mockStatic(CertificateUtil.class);
		PowerMockito.mockStatic(CertificateValidationScreen.class);
		PowerMockito.mockStatic(AuthenticateUser.class);

		encryptionManagerMock = mock(EncryptionManager.class);
		serverConnectorMock = mock(ServerConnector.class);
		userPropertiesMock = mock(UserProperties.class);
		localHashesMock = mock(LocalHashes.class);
		certificateMock = mock(X509Certificate.class);

		hostAddressFieldMock = mock(JTextField.class);
		hostPortFieldMock = mock(JTextField.class);

		when(UserProperties.getInstance()).thenReturn(userPropertiesMock);
		when(LocalHashes.getInstance()).thenReturn(localHashesMock);
		when(CertificateUtil.checkValidCertificate(Mockito.any(X509Certificate.class))).thenReturn(true);
		when(CertificateValidationScreen.validateCertificate(Mockito.any(X509Certificate.class))).thenReturn(true);
		when(AuthenticateUser.authenticateUser(Mockito.any(ServerConnector.class))).thenReturn(true);

		when(encryptionManagerMock.getConnector()).thenReturn(serverConnectorMock);

		changeServerPanel = new ChangeServerPanel(encryptionManagerMock);
		// when(changeServerPanel.applySettings()).thenCallRealMethod();

		Whitebox.setInternalState(changeServerPanel, "hostAddressField", hostAddressFieldMock);
		Whitebox.setInternalState(changeServerPanel, "hostPortField", hostPortFieldMock);
		Whitebox.setInternalState(changeServerPanel, "encryptionManager", encryptionManagerMock);
	}

	/**
	 * Tear down after each test
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		encryptionManagerMock = null;
		serverConnectorMock = null;
		userPropertiesMock = null;
		localHashesMock = null;
		certificateMock = null;

		hostAddressFieldMock = null;
		hostPortFieldMock = null;

		changeServerPanel = null;
	}

	/**
	 * Test for the normal run
	 * 
	 * @throws IOException
	 */
	@Test
	public void testNormal() throws IOException {
		when(hostAddressFieldMock.getText()).thenReturn(IP);
		when(hostPortFieldMock.getText()).thenReturn(PORT);
		when(serverConnectorMock.getServerCertificates()).thenReturn(new Certificate[] { certificateMock });

		List<UserMessage> messages = changeServerPanel.applySettings();
		assertTrue(messages == null || messages.isEmpty());

		// Should never be called, is mocked
		verify(userPropertiesMock, Mockito.never()).getUsernameHash();
		verify(userPropertiesMock, Mockito.never()).getPasswordHash();

		// Null check
		verify(hostAddressFieldMock, Mockito.atLeastOnce()).getText();
		verify(hostPortFieldMock, Mockito.atLeastOnce()).getText();

		// Get the connector
		verify(encryptionManagerMock, Mockito.atLeastOnce()).getConnector();

		// Connect check
		verify(serverConnectorMock).init(Mockito.any(InetSocketAddress.class));

		// Certificate check
		verify(serverConnectorMock).getServerCertificates();

		// update keyfile when successful if it does not exist
		verify(encryptionManagerMock).updateKeyFile(Mockito.any(KeyFile.class));

		// actions when successful
		verify(localHashesMock).removeAllHashes();
		verify(userPropertiesMock).setServerAddress(Mockito.any(InetSocketAddress.class));
		verify(userPropertiesMock).setCertificate(Mockito.any(X509Certificate.class));

		// verify(encryptionManagerMock, Mockito.times(1)).updateKeyFile(Mockito.any(KeyFile.class));
	}

	/**
	 * Test for when a the serverip is bad
	 * 
	 * @throws IOException
	 */
	@Test
	public void testNoServerAddress() throws IOException {
		when(hostAddressFieldMock.getText()).thenReturn("");
		when(hostPortFieldMock.getText()).thenReturn("");
		when(serverConnectorMock.getServerCertificates()).thenReturn(new Certificate[] { certificateMock });

		List<UserMessage> messages = changeServerPanel.applySettings();
		assertFalse(messages == null || messages.isEmpty());

		// Should never be called, is mocked
		verify(userPropertiesMock, Mockito.never()).getUsernameHash();
		verify(userPropertiesMock, Mockito.never()).getPasswordHash();

		// Null check
		verify(hostAddressFieldMock, Mockito.atLeastOnce()).getText();
		verify(hostPortFieldMock, Mockito.atLeastOnce()).getText();

		// Get the connector
		verify(encryptionManagerMock, Mockito.atLeastOnce()).getConnector();

		// Connect check
		verify(serverConnectorMock, Mockito.never()).init(Mockito.any(InetSocketAddress.class));

		// Certificate check
		verify(serverConnectorMock, Mockito.never()).getServerCertificates();

		// update keyfile when successful if it does not exist
		verify(encryptionManagerMock, Mockito.never()).updateKeyFile(Mockito.any(KeyFile.class));

		// actions when successful
		verify(localHashesMock, Mockito.never()).removeAllHashes();
		verify(userPropertiesMock, Mockito.never()).setServerAddress(Mockito.any(InetSocketAddress.class));
		verify(userPropertiesMock, Mockito.never()).setCertificate(Mockito.any(X509Certificate.class));
	}

	/**
	 * Test for when a the certificate is bad
	 * 
	 * @throws IOException
	 */
	@Test
	public void testBadCertificate() throws IOException {
		when(hostAddressFieldMock.getText()).thenReturn(IP);
		when(hostPortFieldMock.getText()).thenReturn(PORT);
		when(serverConnectorMock.getServerCertificates()).thenReturn(new Certificate[] { certificateMock });

		when(CertificateUtil.checkValidCertificate(Mockito.any(X509Certificate.class))).thenReturn(false);
		when(CertificateValidationScreen.validateCertificate(Mockito.any(X509Certificate.class))).thenReturn(false);

		List<UserMessage> messages = changeServerPanel.applySettings();
		assertFalse(messages == null || messages.isEmpty());

		// Should never be called, is mocked
		verify(userPropertiesMock, Mockito.never()).getUsernameHash();
		verify(userPropertiesMock, Mockito.never()).getPasswordHash();

		// Null check
		verify(hostAddressFieldMock, Mockito.atLeastOnce()).getText();
		verify(hostPortFieldMock, Mockito.atLeastOnce()).getText();

		// Get the connector
		verify(encryptionManagerMock, Mockito.atLeastOnce()).getConnector();

		// Connect check
		verify(serverConnectorMock, Mockito.atLeastOnce()).init(Mockito.any(InetSocketAddress.class));

		// Certificate check
		verify(serverConnectorMock, Mockito.atLeastOnce()).getServerCertificates();

		// update keyfile when successful if it does not exist
		verify(encryptionManagerMock, Mockito.never()).updateKeyFile(Mockito.any(KeyFile.class));

		// actions when successful
		verify(localHashesMock, Mockito.never()).removeAllHashes();
		verify(userPropertiesMock, Mockito.never()).setServerAddress(Mockito.any(InetSocketAddress.class));
		verify(userPropertiesMock, Mockito.never()).setCertificate(Mockito.any(X509Certificate.class));
	}

	/**
	 * Test for when a user is not authenticated
	 * 
	 * @throws IOException
	 */
	@Test
	public void testBadUser() throws IOException {
		when(hostAddressFieldMock.getText()).thenReturn(IP);
		when(hostPortFieldMock.getText()).thenReturn(PORT);
		when(serverConnectorMock.getServerCertificates()).thenReturn(new Certificate[] { certificateMock });
		when(AuthenticateUser.authenticateUser(Mockito.any(ServerConnector.class))).thenReturn(false);

		List<UserMessage> messages = changeServerPanel.applySettings();
		assertFalse(messages == null || messages.isEmpty());

		// Should never be called, is mocked
		verify(userPropertiesMock, Mockito.never()).getUsernameHash();
		verify(userPropertiesMock, Mockito.never()).getPasswordHash();

		// Null check
		verify(hostAddressFieldMock, Mockito.atLeastOnce()).getText();
		verify(hostPortFieldMock, Mockito.atLeastOnce()).getText();

		// Get the connector
		verify(encryptionManagerMock, Mockito.atLeastOnce()).getConnector();

		// Connect check
		verify(serverConnectorMock, Mockito.atLeastOnce()).init(Mockito.any(InetSocketAddress.class));

		// Certificate check
		verify(serverConnectorMock, Mockito.atLeastOnce()).getServerCertificates();

		// update keyfile when successful if it does not exist
		verify(encryptionManagerMock, Mockito.never()).updateKeyFile(Mockito.any(KeyFile.class));

		// actions when successful
		verify(localHashesMock, Mockito.never()).removeAllHashes();
		verify(userPropertiesMock, Mockito.never()).setServerAddress(Mockito.any(InetSocketAddress.class));
		verify(userPropertiesMock, Mockito.never()).setCertificate(Mockito.any(X509Certificate.class));
	}

	/**
	 * Test for when the host is thesame
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSameHost() throws IOException {
		when(hostAddressFieldMock.getText()).thenReturn(IP);
		when(hostPortFieldMock.getText()).thenReturn(PORT);
		when(userPropertiesMock.getHost()).thenReturn(IP);
		when(userPropertiesMock.getHostPort()).thenReturn(PORT_INT);
		when(serverConnectorMock.getServerCertificates()).thenReturn(new Certificate[] { certificateMock });
		when(AuthenticateUser.authenticateUser(Mockito.any(ServerConnector.class))).thenReturn(false);

		List<UserMessage> messages = changeServerPanel.applySettings();
		assertTrue(messages == null || messages.isEmpty());

		// Should never be called, is mocked
		verify(userPropertiesMock, Mockito.never()).getUsernameHash();
		verify(userPropertiesMock, Mockito.never()).getPasswordHash();

		// Null check
		verify(hostAddressFieldMock, Mockito.atLeastOnce()).getText();
		verify(hostPortFieldMock, Mockito.atLeastOnce()).getText();

		// Get the connector
		verify(encryptionManagerMock, Mockito.never()).getConnector();

		// Connect check
		verify(serverConnectorMock, Mockito.never()).init(Mockito.any(InetSocketAddress.class));

		// Certificate check
		verify(serverConnectorMock, Mockito.never()).getServerCertificates();

		// update keyfile when successful if it does not exist
		verify(encryptionManagerMock, Mockito.never()).updateKeyFile(Mockito.any(KeyFile.class));

		// actions when successful
		verify(localHashesMock, Mockito.never()).removeAllHashes();
		verify(userPropertiesMock, Mockito.never()).setServerAddress(Mockito.any(InetSocketAddress.class));
		verify(userPropertiesMock, Mockito.never()).setCertificate(Mockito.any(X509Certificate.class));
	}
}
