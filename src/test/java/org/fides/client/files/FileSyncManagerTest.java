package org.fides.client.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.fides.client.encryption.EncryptionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link FileSyncManager}
 * 
 * @author Koen
 *
 */
public class FileSyncManagerTest {

	private Collection<FileCompareResult> compareResults;

	private FileManager fileManagerMock;

	private EncryptionManager encManagerMock;

	private FileSyncManager fileSyncManager;

	private KeyFile keyFile;

	private ByteArrayOutputStream outUpdate;

	private ByteArrayOutputStream outAdd;

	/**
	 * Do before each test
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		compareResults = new HashSet<>();

		outUpdate = new ByteArrayOutputStream();
		outAdd = new ByteArrayOutputStream();

		fileManagerMock = mock(FileManager.class);
		when(fileManagerMock.compareFiles((KeyFile) any())).thenReturn(compareResults);
		when(fileManagerMock.addFile("AddedServerFile")).thenReturn(outAdd);
		when(fileManagerMock.updateFile("UpdatedServerFile")).thenReturn(outUpdate);
		when(fileManagerMock.readFile(anyString())).thenReturn(null); // TODO for later tests

		keyFile = new KeyFile();

		encManagerMock = mock(EncryptionManager.class);
		when(encManagerMock.requestKeyFile()).thenReturn(keyFile); // TODO for later tests

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
		outUpdate.close();
		outUpdate.reset();
		outUpdate = null;
		outAdd.close();
		outAdd.reset();
		outAdd = null;
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#LOCAL_ADDED}
	 */
	@Test
	public void testHandleLocalAdded() {
		// TODO inplement when needed
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
	 */
	@Test
	public void testHandleLocalUpdated() {
		// TODO inplement when needed
	}

	/**
	 * Test to handle a {@link FileCompareResult} with a {@link CompareResultType#SERVER_ADDED}
	 */
	@Test
	public void testHandleServerAdded() throws Exception {
		// Setup for this specific test
		ClientFile addedFile = new ClientFile("AddedServerFile", "asf", null, "");
		keyFile.addClientFile(addedFile);
		compareResults.add(new FileCompareResult("AddedServerFile", CompareResultType.SERVER_ADDED));
		when(encManagerMock.requestFile(addedFile)).thenReturn(new ByteArrayInputStream("This is the added file".getBytes()));

		// The real test
		try {
			fileSyncManager.fileManagerCheck();
			assertEquals("This is the added file", new String(outAdd.toByteArray()));
			assertEquals(0, outUpdate.toByteArray().length);
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
		ClientFile updatedFile = new ClientFile("UpdatedServerFile", "usf", null, "");
		keyFile.addClientFile(updatedFile);
		compareResults.add(new FileCompareResult("UpdatedServerFile", CompareResultType.SERVER_UPDATED));
		when(encManagerMock.requestFile(updatedFile)).thenReturn(new ByteArrayInputStream("This is the updated file".getBytes()));

		// The real test
		try {
			fileSyncManager.fileManagerCheck();
			assertEquals("This is the updated file", new String(outUpdate.toByteArray()));
			assertEquals(0, outAdd.toByteArray().length);
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
