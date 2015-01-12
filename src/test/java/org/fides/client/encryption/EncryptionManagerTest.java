package org.fides.client.encryption;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.fides.client.connector.EncryptedOutputStreamData;
import org.fides.client.connector.OutputStreamData;
import org.fides.client.connector.ServerConnector;
import org.fides.client.files.InvalidClientFileException;
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.KeyFile;
import org.fides.encryption.KeyGenerator;
import org.fides.tools.HashUtils;
import org.junit.Test;
import org.mockito.Mockito;

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
		Mockito.when(mockConnector.updateKeyFile()).thenReturn(mockOut);

		KeyFile keyfile = new KeyFile();
		ClientFile clientFile = new ClientFile("Name", "Location", null, "Hash");
		keyfile.addClientFile(clientFile);

		try {
			// Creates an EncryptionManager with the mock ServerConnector
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS);
			manager.updateKeyFile(keyfile);

			ByteArrayInputStream mockIn = new ByteArrayInputStream(mockOut.toByteArray());
			DataInputStream mockDin = new DataInputStream(mockIn);

			// Check if the mock ServerConnector received a correctly encrypted stream
			assertTrue(mockOut.size() > 0);
			assertEquals(KeyGenerator.getRounds(), mockDin.readInt());
		} catch (Exception e) {
			fail("An unexpected exception has occurred: " + e.getMessage());
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
		Mockito.when(mockConnector.updateKeyFile()).thenReturn(mockOut);

		KeyFile keyfile = new KeyFile();
		ClientFile clientFile = new ClientFile("Name", "Location", null, "Hash");
		keyfile.addClientFile(clientFile);

		try {
			// Creates an EncryptionManager with the mock ServerConnector
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS);
			manager.updateKeyFile(keyfile);

			// Use the mock ServerConnector to replace the call to requestKeyFile to return the encrypted key file.
			ByteArrayInputStream mockIn = new ByteArrayInputStream(mockOut.toByteArray());
			Mockito.when(mockConnector.requestKeyFile()).thenReturn(mockIn);
			KeyFile requestedKeyFile = manager.requestKeyFile();

			// Check if the Key File was correctly decrypted
			assertNotNull(requestedKeyFile);
			assertEquals(clientFile, requestedKeyFile.getClientFileByName(clientFile.getName()));
		} catch (Exception e) {
			fail("An unexpected exception has occurred: " + e.getMessage());
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
		Mockito.when(mockConnector.uploadFile()).thenReturn(new OutputStreamData(mockOut, fileLocation));

		try {
			// Creates an EncryptionManager with the mock ServerConnector
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS);
			EncryptedOutputStreamData outputStreamData = manager.uploadFile();
			// Test if the EncryptedOutputStreamData is valid.
			assertEquals(fileLocation, outputStreamData.getLocation());
			assertNotNull(outputStreamData.getKey());

			// Test if the outputStreamData gets written.
			outputStreamData.getOutputStream().write(MESSAGE);
			outputStreamData.getOutputStream().close();
			assertTrue(mockOut.size() > 0);

			// Check if encrypting the same file twice gives different results
			String result1 = HashUtils.toHex(mockOut.toByteArray());
			mockOut.reset();
			outputStreamData = manager.uploadFile();
			outputStreamData.getOutputStream().write(MESSAGE);
			outputStreamData.getOutputStream().close();
			String result2 = HashUtils.toHex(mockOut.toByteArray());
			assertTrue(mockOut.size() > 0);
			assertNotEquals(result1, result2);
		} catch (Exception e) {
			fail("An unexpected exception has occurred: " + e.getMessage());
		}
	}

	/**
	 * Tests the decryption of a file by encrypting and decrypting the stream and checking if the message is the same
	 * before and after.
	 */
	@Test
	public void testFileDecrypt() {
		// Create a mock of the ServerConnector to catch the call to uploadKeyFile.
		String fileLocation = "Location";
		ServerConnector mockConnector = mock(ServerConnector.class);
		ByteArrayOutputStream mockOut = new ByteArrayOutputStream();
		Mockito.when(mockConnector.uploadFile()).thenReturn(new OutputStreamData(mockOut, fileLocation));

		try {
			// Creates an EncryptionManager with the mock ServerConnector
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS);
			EncryptedOutputStreamData outputStreamData = manager.uploadFile();
			outputStreamData.getOutputStream().write(MESSAGE);
			outputStreamData.getOutputStream().close();

			ClientFile clientFile = new ClientFile("Name", outputStreamData.getLocation(), outputStreamData.getKey(), "Hash");

			InputStream mockIn = new ByteArrayInputStream(mockOut.toByteArray());

			// Check if decrypting the file results in the original message.
			Mockito.when(mockConnector.requestFile(fileLocation)).thenReturn(mockIn);
			InputStream inStream = manager.requestFile(clientFile);
			ByteArrayOutputStream readBytes = new ByteArrayOutputStream();
			IOUtils.copy(inStream, readBytes);
			assertArrayEquals(MESSAGE, readBytes.toByteArray());
		} catch (Exception e) {
			fail("An unexpected exception has occurred: " + e.getMessage());
		}
	}

	/**
	 * Tests the update by uploading a file and then updating it.
	 */
	@Test
	public void testUpdateFile() {
		// Create a mock of the ServerConnector to catch the call to updateKeyFile.
		String fileLocation = "Location";
		ServerConnector mockConnector = mock(ServerConnector.class);
		ByteArrayOutputStream mockUploadOut = new ByteArrayOutputStream();
		ByteArrayOutputStream mockUpdateOut = new ByteArrayOutputStream();
		Mockito.when(mockConnector.uploadFile()).thenReturn(new OutputStreamData(mockUploadOut, fileLocation));
		Mockito.when(mockConnector.updateFile(fileLocation)).thenReturn(mockUpdateOut);

		try {
			// Creates an EncryptionManager with the mock ServerConnector and uploads a file.
			EncryptionManager manager = new EncryptionManager(mockConnector, PASS);
			EncryptedOutputStreamData outputStreamData = manager.uploadFile();
			outputStreamData.getOutputStream().write(MESSAGE);
			outputStreamData.getOutputStream().close();
			byte[] uploadedBytes = mockUploadOut.toByteArray();

			ClientFile clientFile = new ClientFile("Name", outputStreamData.getLocation(), outputStreamData.getKey(), "Hash");

			// Update the uploaded file with a new message.
			OutputStream outStream = manager.updateFile(clientFile);
			outStream.write("A different message than the default message".getBytes());
			outStream.close();

			// Check if the updated OutputStream is different from the uploaded OutputStream
			assertTrue(mockUpdateOut.size() > 0);
			assertFalse(Arrays.equals(uploadedBytes, mockUpdateOut.toByteArray()));

			// Update the uploaded file with the original message.
			mockUpdateOut.reset();
			outStream = manager.updateFile(clientFile);
			outStream.write(MESSAGE);
			outStream.close();

			// Check if the updated OutputStream is the same as the original uploaded OutputStream
			assertTrue(mockUpdateOut.size() > 0);
			assertArrayEquals(uploadedBytes, mockUpdateOut.toByteArray());

		} catch (Exception e) {
			fail("An unexpected exception has occurred: " + e.getMessage());
		}
	}

	/**
	 * Tests the remove file.
	 */
	@Test
	public void testRemoveFile() {
		// Create a mock of the ServerConnector to catch the call to updateKeyFile.
		ServerConnector mockConnector = mock(ServerConnector.class);

		// Creates an EncryptionManager with the mock ServerConnector and uploads a file.
		EncryptionManager manager = new EncryptionManager(mockConnector, PASS);

		// Validate a NullPointerException
		try {
			assertFalse(manager.removeFile(null));
		} catch (Exception e) {
			fail("An Exception was thrown: " + e);
		}

		// Validate InvalidClientFileException
		try {
			ClientFile clientFile = new ClientFile(null, null, null, null);
			manager.removeFile(clientFile);
			fail("An expected InvalidClientFileException was not thrown");
		} catch (Exception e) {
			assertTrue(e instanceof InvalidClientFileException);
		}
	}
}
