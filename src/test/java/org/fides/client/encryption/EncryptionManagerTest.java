package org.fides.client.encryption;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.fides.client.connector.EncryptedOutputStreamData;
import org.fides.client.connector.OutputStreamData;
import org.fides.client.connector.ServerConnector;
import org.fides.client.files.ClientFile;
import org.fides.client.files.KeyFile;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * The JUnit Test Case for the EncryptionManager
 */
public class EncryptionManagerTest {

	private static final String PASS = "DEFAULT PASSWORD";

	private static final byte[] MESSAGE = ("DEFAULT MESSAGE: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore "
		+ "et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. "
		+ "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non "
		+ "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.").getBytes();

	/**
	 * Tests the encryption of a key file.
	 */
	@Test
	public void testKeyFileEncrypt() {
		// Create a mock of the ServerConnector to catch the call to uploadKeyFile.
		ServerConnector mockConnector = mock(ServerConnector.class);
		ByteArrayOutputStream mockOut = new ByteArrayOutputStream();
		when(mockConnector.uploadKeyFile()).thenReturn(mockOut);

		KeyFile keyfile = new KeyFile();
		ClientFile clientFile = new ClientFile("Name", "Location", null, "Hash");
		keyfile.addClientFile(clientFile);

		try {
			// Creates an EncryptionManager with the mock ServerConnector
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS);
			manager.uploadKeyFile(keyfile);

			ByteArrayInputStream mockIn = new ByteArrayInputStream(mockOut.toByteArray());
			DataInputStream mockDin = new DataInputStream(mockIn);

			// Check if the mock ServerConnector received a correctly encrypted stream
			assertTrue(mockOut.size() > 0);
			assertEquals(KeyGenerator.getRounds(), mockDin.readInt());
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests the decryption of a key file by first encrypting a key file and then decrypting it.
	 */
	@Test
	public void testKeyFileDecrypt() {
		// Create a mock of the ServerConnector to catch the call to
		// uploadKeyFile.
		ServerConnector mockConnector = mock(ServerConnector.class);
		ByteArrayOutputStream mockOut = new ByteArrayOutputStream();
		when(mockConnector.uploadKeyFile()).thenReturn(mockOut);

		KeyFile keyfile = new KeyFile();
		ClientFile clientFile = new ClientFile("Name", "Location", null, "Hash");
		keyfile.addClientFile(clientFile);

		try {
			// Creates an EncryptionManager with the mock ServerConnector
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS);
			manager.uploadKeyFile(keyfile);

			// Use the mock ServerConnector to replace the call to requestKeyFile to return the encrypted key file.
			ByteArrayInputStream mockIn = new ByteArrayInputStream(mockOut.toByteArray());
			when(mockConnector.requestKeyFile()).thenReturn(mockIn);
			KeyFile requestedKeyFile = manager.requestKeyFile();

			// Check if the Key File was correctly decrypted
			assertNotNull(requestedKeyFile);
			assertEquals(clientFile, requestedKeyFile.getClientFileByName(clientFile.getName()));
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests if a stream is encrypted correctly
	 */
	@Test
	public void testFileEncrypt() {
		// Create a mock of the ServerConnector to catch the call to uploadKeyFile.
		String fileLocation = "Location";
		ServerConnector mockConnector = mock(ServerConnector.class);
		ByteArrayOutputStream mockOut = new ByteArrayOutputStream();
		when(mockConnector.uploadFile()).thenReturn(new OutputStreamData(mockOut, fileLocation));

		try {
			// Creates an EncryptionManager with the mock ServerConnector
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS);
			EncryptedOutputStreamData outputStreamData = manager.uploadFile();
			// Test if the EncryptedOutputStreamData is valid.
			assertEquals(fileLocation, outputStreamData.getLocation());
			assertNotNull(outputStreamData.getKey());

			// Test if the outputstream gets written.
			outputStreamData.getOutputStream().write(MESSAGE);
			outputStreamData.getOutputStream().close();
			assertTrue(mockOut.size() > 0);

			// Check if encrypting the same file twice gives different results
			String result1 = KeyGenerator.toHex(mockOut.toByteArray());
			mockOut = new ByteArrayOutputStream();
			outputStreamData = manager.uploadFile();
			outputStreamData.getOutputStream().write(MESSAGE);
			outputStreamData.getOutputStream().close();
			String result2 = KeyGenerator.toHex(mockOut.toByteArray());
			assertNotEquals(result1, result2);
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests the decryption of a file by encrypting and decrypting the stream and checking if the message is the same before and after.
	 */
	@Test
	public void testFileDecrypt() {
		// Create a mock of the ServerConnector to catch the call to uploadKeyFile.
		String fileLocation = "Location";
		ServerConnector mockConnector = mock(ServerConnector.class);
		ByteArrayOutputStream mockOut = new ByteArrayOutputStream();
		when(mockConnector.uploadFile()).thenReturn(new OutputStreamData(mockOut, fileLocation));

		try {
			// Creates an EncryptionManager with the mock ServerConnector
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS);
			EncryptedOutputStreamData outputStreamData = manager.uploadFile();
			outputStreamData.getOutputStream().write(MESSAGE);
			outputStreamData.getOutputStream().close();

			KeyFile keyFile = new KeyFile();
			ClientFile clientFile = new ClientFile("Name", outputStreamData.getLocation(), outputStreamData.getKey(), "Hash");
			keyFile.addClientFile(clientFile);

			InputStream mockIn = new ByteArrayInputStream(mockOut.toByteArray());

			// Check if decrypting the file results in the original message.
			when(mockConnector.requestFile(fileLocation)).thenReturn(mockIn);
			InputStream inStream = manager.requestFile(fileLocation, keyFile);
			ByteArrayOutputStream readBytes = new ByteArrayOutputStream();
			IOUtils.copy(inStream, readBytes);
			assertArrayEquals(MESSAGE, readBytes.toByteArray());
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

}
