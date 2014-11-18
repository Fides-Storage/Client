package org.fides.client.encryption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import org.fides.client.connector.ServerConnector;
import org.fides.client.files.ClientFile;
import org.fides.client.files.KeyFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * The JUnit Test Case for the EncryptionManager
 */
//@RunWith(PowerMockRunner.class);
public class EncryptionManagerTest {
	
	private static final String PASS = "DEFAULT PASSWORD";
	private static final byte[] SALT = "DEFAULT SALTINGS".getBytes();
	
	/**
	 * Tests the encryption of a key file.
	 */
	@Test
	public void testKeyFileEncrypt() {
		// Create a mock of the ServerConnector to catch the call to uploadKeyFile.
		ServerConnector mockConnector = mock(ServerConnector.class);
		ByteArrayOutputStream mockOut = new ByteArrayOutputStream();
		when(mockConnector.uploadKeyFile()).thenReturn(mockOut);
		
		// Create a mock of the KeyGenerator to replace the call to generateSalt
		mockStatic(KeyGenerator.class);
		when(KeyGenerator.getSalt(Mockito.anyInt())).thenReturn(SALT);
		
		
		KeyFile keyfile = new KeyFile();
		ClientFile clientFile = new ClientFile("Name", "Location", null, "Hash");
		keyfile.addClientFile(clientFile);
		
		try {
			// Creates an EncryptionManager with the mock ServerConnector
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS, new KeyGenerator());
			manager.uploadKeyFile(keyfile);
			
			// Check if the mock ServerConnector received an encrypted stream
			assertTrue(mockOut.size() > 0);
			
			ByteArrayInputStream mockIn = new ByteArrayInputStream(mockOut.toByteArray());
			//TODO: Check size blabla
		} catch (Exception e) {
			fail("An exception has occured: " + e.getMessage());
		}
	}
	
	/**
	 * Tests the decryption of a key file by first encrypting a key file and then decrypting it.
	 */
	@Test
	public void testKeyFileDecrypt() {
		// Create a mock of the ServerConnector to catch the call to uploadKeyFile.
		ServerConnector mockConnector = mock(ServerConnector.class);
		ByteArrayOutputStream mockOut = new ByteArrayOutputStream();
		when(mockConnector.uploadKeyFile()).thenReturn(mockOut);
		
		KeyFile keyfile = new KeyFile();
		ClientFile clientFile = new ClientFile("Name", "Location", null, "Hash");
		keyfile.addClientFile(clientFile);
		
		try {
			// Creates an EncryptionManager with the mock ServerConnector
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS, new KeyGenerator());
			manager.uploadKeyFile(keyfile);
			
			// Use the mock ServerConnector to replace the call to requestKeyFile to return the encrypted key file.
			ByteArrayInputStream mockIn = new ByteArrayInputStream(mockOut.toByteArray());
			when(mockConnector.requestKeyFile()).thenReturn(mockIn);
			KeyFile requestedKeyFile = manager.requestKeyFile();
			
			// Check if the Key File was correctly decrypted
			assertNotNull(requestedKeyFile);
			assertEquals(clientFile, requestedKeyFile.getClientFileByName(clientFile.getName()));
		} catch (Exception e) {
			fail("An exception has occured: " + e.getMessage());
		}
	}

}
