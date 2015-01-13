package org.fides.client.ui.settings;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.swing.JTextField;

import org.fides.client.connector.ServerConnector;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.tools.LocalHashes;
import org.fides.client.tools.UserProperties;
import org.fides.client.ui.AuthenticateUser;
import org.fides.client.ui.CertificateValidationScreen;
import org.fides.client.ui.UserMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
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
@PrepareForTest({ UserProperties.class, LocalHashes.class, CertificateValidationScreen.class, AuthenticateUser.class })
@PowerMockIgnore({ "javax.management.*", "javax.swing.*", "java.security.*", "javax.security.*" })
public class ChangeServerPanelTest {

	private ChangeServerPanel changeServerPanel;

	private EncryptionManager encryptionManagerMock;

	private ServerConnector serverConnectorMock;

	private UserProperties userPropertiesMock;

	private LocalHashes localHashesMock;

	private JTextField hostAddressFieldMock;

	private JTextField hostPortFieldMock;

	private X509Certificate certificate;

	@Before
	public void setUp() throws Exception {
		encryptionManagerMock = mock(EncryptionManager.class);
		serverConnectorMock = mock(ServerConnector.class);
		userPropertiesMock = mock(UserProperties.class);
		localHashesMock = mock(LocalHashes.class);
		certificate = mock(X509Certificate.class);

		hostAddressFieldMock = mock(JTextField.class);
		hostPortFieldMock = mock(JTextField.class);

		PowerMockito.mockStatic(UserProperties.class);
		PowerMockito.mockStatic(LocalHashes.class);
		PowerMockito.mockStatic(CertificateValidationScreen.class);
		PowerMockito.mockStatic(AuthenticateUser.class);

		when(UserProperties.getInstance()).thenReturn(userPropertiesMock);
		when(LocalHashes.getInstance()).thenReturn(localHashesMock);
		when(CertificateValidationScreen.validateCertificate((X509Certificate) Matchers.any())).thenReturn(true);
		when(AuthenticateUser.authenticateUser((ServerConnector) Matchers.any())).thenReturn(true);

		when(encryptionManagerMock.getConnector()).thenReturn(serverConnectorMock);

		changeServerPanel = mock(ChangeServerPanel.class);
		when(changeServerPanel.applySettings()).thenCallRealMethod();

		Whitebox.setInternalState(changeServerPanel, "hostAddressField", hostAddressFieldMock);
		Whitebox.setInternalState(changeServerPanel, "hostPortField", hostPortFieldMock);
		Whitebox.setInternalState(changeServerPanel, "encryptionManager", encryptionManagerMock);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		when(hostAddressFieldMock.getText()).thenReturn("123.456.789.1");
		when(hostPortFieldMock.getText()).thenReturn("1234");
		when(serverConnectorMock.getServerCertificates()).thenReturn(new Certificate[] { certificate });

		List<UserMessage> messages = changeServerPanel.applySettings();
		assertEquals(0, messages.size());
	}

}
