package org.fides.client.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashSet;

import org.fides.client.connector.EncryptedOutputStreamData;
import org.fides.client.connector.ServerConnector;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.CompareResultType;
import org.fides.client.files.data.FileCompareResult;
import org.fides.client.files.data.KeyFile;
import org.fides.client.tools.LocalHashes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests for the {@link FileSyncManager}
 * 
 * @author Koen
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ LocalHashes.class })
public class FileSyncManagerTest {

	private Collection<FileCompareResult> compareResults;

	private FileManager fileManagerMock;

	private EncryptionManager encManagerMock;

	private FileSyncManager fileSyncManager;

	private ServerConnector serverConnectorMock;

	private KeyFile keyFile;

	/**
	 * Do before each test
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		compareResults = new HashSet<>();
		PowerMockito.mockStatic(LocalHashes.class);
		Mockito.when(LocalHashes.getInstance()).thenReturn(Mockito.mock(LocalHashes.class));

		fileManagerMock = Mockito.mock(FileManager.class);
		Mockito.when(fileManagerMock.compareFiles((KeyFile) Mockito.any())).thenReturn(compareResults);

		keyFile = new KeyFile();

		serverConnectorMock = Mockito.mock(ServerConnector.class);

		encManagerMock = Mockito.mock(EncryptionManager.class);
		Mockito.when(encManagerMock.requestKeyFile()).thenReturn(keyFile);
		Mockito.when(encManagerMock.getConnector()).thenReturn(serverConnectorMock);

		fileSyncManager = new FileSyncManager(fileManagerMock, encManagerMock);
	}

	/**
	 * Do after each test
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		compareResults.clear();
		compareResults = null;
		fileManagerMock = null;
		encManagerMock = null;
		fileSyncManager = null;
		serverConnectorMock = null;
		keyFile = null;
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#LOCAL_ADDED}
	 * 
	 * @throws FileNotFoundException
	 */
	@Test
	public void testHandleLocalAdded() throws FileNotFoundException {
		// Setup for this specific test
		compareResults.add(new FileCompareResult("AddedLocalFile", CompareResultType.LOCAL_ADDED));
		when(fileManagerMock.readFile("AddedLocalFile")).thenReturn(new ByteArrayInputStream("This is an in update file".getBytes()));

		// Create output data for the file on the server
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		EncryptedOutputStreamData outData = new EncryptedOutputStreamData(out, "laf", null);
		when(encManagerMock.uploadFile()).thenReturn(outData);

		// The real test
		fileSyncManager.fileManagerCheck();
		assertEquals("This is an in update file", new String(out.toByteArray()));
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#LOCAL_REMOVED}
	 */
	@Test
	public void testHandleLocalRemoved() throws InvalidClientFileException {
		String filename = "removedLocalFile";
		compareResults.add(new FileCompareResult(filename, CompareResultType.LOCAL_REMOVED));
		when(encManagerMock.removeFile(Mockito.any(ClientFile.class))).thenReturn(true);

		keyFile.addClientFile(new ClientFile(filename, filename, null, null));

		when(encManagerMock.requestKeyFile()).thenReturn(keyFile);

		// The real test
		fileSyncManager.fileManagerCheck();
		assertNull(keyFile.getClientFileByName(filename));
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#LOCAL_UPDATED}
	 * 
	 * @throws FileNotFoundException
	 * @throws InvalidClientFileException
	 */
	@Test
	public void testHandleLocalUpdated() throws FileNotFoundException, InvalidClientFileException {
		// Setup for this specific test
		compareResults.add(new FileCompareResult("UpdatedLocalFile", CompareResultType.LOCAL_UPDATED));
		when(fileManagerMock.readFile("UpdatedLocalFile")).thenReturn(new ByteArrayInputStream("This is an in update file".getBytes()));

		// The clientfile of the existing file on the server
		ClientFile updatedFile = new ClientFile("UpdatedLocalFile", "ulf", null, "");
		keyFile.addClientFile(updatedFile);
		// Set an outputstream we can read
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		when(encManagerMock.updateFile(updatedFile)).thenReturn(out);

		// The real test
		fileSyncManager.fileManagerCheck();
		assertEquals("This is an in update file", new String(out.toByteArray()));
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#SERVER_ADDED}
	 */
	@Test
	public void testHandleServerAdded() throws Exception {
		// Setup for this specific test
		ByteArrayOutputStream outAdd = new ByteArrayOutputStream();
		when(fileManagerMock.addFile("AddedServerFile")).thenReturn(outAdd);
		ClientFile addedFile = new ClientFile("AddedServerFile", "asf", null, "");
		keyFile.addClientFile(addedFile);
		compareResults.add(new FileCompareResult("AddedServerFile", CompareResultType.SERVER_ADDED));
		when(encManagerMock.requestFile(addedFile)).thenReturn(new ByteArrayInputStream("This is the added file".getBytes()));

		// The real test
		fileSyncManager.fileManagerCheck();
		assertEquals("This is the added file", new String(outAdd.toByteArray()));
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#SERVER_UPDATED}
	 */
	@Test
	public void testHandleServerUpdated() throws Exception {
		// Setup for this specific test
		ByteArrayOutputStream outUpdate = new ByteArrayOutputStream();
		when(fileManagerMock.updateFile("UpdatedServerFile")).thenReturn(outUpdate);
		ClientFile updatedFile = new ClientFile("UpdatedServerFile", "usf", null, "");
		keyFile.addClientFile(updatedFile);
		compareResults.add(new FileCompareResult("UpdatedServerFile", CompareResultType.SERVER_UPDATED));
		when(encManagerMock.requestFile(updatedFile)).thenReturn(new ByteArrayInputStream("This is the updated file".getBytes()));

		// The real test
		fileSyncManager.fileManagerCheck();
		assertEquals("This is the updated file", new String(outUpdate.toByteArray()));
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#CONFLICTED}
	 */
	@Test
	public void testHandleConflict() {
		// TODO inplement when needed
	}

}
