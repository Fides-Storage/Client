package org.fides.client.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.fides.client.connector.EncryptedOutputStreamData;
import org.fides.client.encryption.EncryptionManager;
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
		Mockito.when(LocalHashes.getInstance()).thenReturn(mock(LocalHashes.class));

		fileManagerMock = mock(FileManager.class);
		when(fileManagerMock.compareFiles((KeyFile) any())).thenReturn(compareResults);

		keyFile = new KeyFile();

		encManagerMock = mock(EncryptionManager.class);
		when(encManagerMock.requestKeyFile()).thenReturn(keyFile);

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
		try {
			fileSyncManager.fileManagerCheck();
			assertEquals("This is an in update file", new String(out.toByteArray()));
		} catch (IOException e) {
			e.printStackTrace();
			fail("Exception: " + e.getMessage());
		}
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#LOCAL_REMOVED}
	 */
	@Test
	public void testHandleLocalRemoved() {
		// TODO inplement when needed
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
		try {
			fileSyncManager.fileManagerCheck();
			assertEquals("This is an in update file", new String(out.toByteArray()));
		} catch (IOException e) {
			e.printStackTrace();
			fail("Exception: " + e.getMessage());
		}
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
		try {
			fileSyncManager.fileManagerCheck();
			assertEquals("This is the added file", new String(outAdd.toByteArray()));
		} catch (IOException e) {
			e.printStackTrace();
			fail("Exception: " + e.getMessage());
		}
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#SERVER_REMOVED}
	 */
	@Test
	public void testHandleServerRemoved() {
		// TODO inplement when needed
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
		try {
			fileSyncManager.fileManagerCheck();
			assertEquals("This is the updated file", new String(outUpdate.toByteArray()));
		} catch (IOException e) {
			e.printStackTrace();
			fail("Exception: " + e.getMessage());
		}
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#CONFLICTED}
	 */
	@Test
	public void testHandleConflict() {
		// TODO inplement when needed
	}

}
