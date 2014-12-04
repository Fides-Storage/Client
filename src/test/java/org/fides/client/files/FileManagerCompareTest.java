package org.fides.client.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.fides.client.UserProperties;
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.CompareResultType;
import org.fides.client.files.data.FileCompareResult;
import org.fides.client.files.data.KeyFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for the {@link FileManager}
 * 
 * @author Koen
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ UserProperties.class, FileUtil.class, LocalHashes.class })
public class FileManagerCompareTest {

	private UserProperties settingsMock;

	private LocalHashes localHashesMock;

	private KeyFile keyFile;

	private Properties localHashes;

	private File testDir;

	private FileManager fileManager;

	/**
	 * Setup before tests
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		keyFile = new KeyFile();

		localHashes = new Properties();

		// Create a directory for test files
		testDir = new File("./testDir");
		testDir.mkdir();
		testDir.deleteOnExit();

		// Mock the UserProperties so it returns the test directory
		settingsMock = mock(UserProperties.class);
		when(settingsMock.getFileDirectory()).thenReturn(testDir);
		PowerMockito.mockStatic(UserProperties.class);
		Mockito.when(UserProperties.getInstance()).thenReturn(settingsMock);

		// Mock the LocalHashes so it uses our own hashes
		localHashesMock = mock(LocalHashes.class);
		PowerMockito.mockStatic(LocalHashes.class);
		Mockito.when(LocalHashes.getInstance()).thenReturn(localHashesMock);
		when(localHashesMock.containsHash(Matchers.anyString())).then(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return localHashes.containsKey(invocation.getArgumentAt(0, String.class));
			}
		});
		when(localHashesMock.getHash(Matchers.anyString())).then(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				return localHashes.getProperty(invocation.getArgumentAt(0, String.class));
			}
		});

		// Mock the FileUtil hash function
		PowerMockito.mockStatic(FileUtil.class);
		Mockito.when(FileUtil.generateFileHash((File) Matchers.any())).then(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				return invocation.getArgumentAt(0, File.class).getName();
			}
		});

		fileManager = new FileManager();
	}

	/**
	 * Tear down after tests
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		settingsMock = null;
		keyFile = null;
		localHashes = null;
		FileUtils.deleteDirectory(testDir);
		testDir = null;
	}

	/**
	 * Test the check for a file being added on the server
	 */
	@Test
	public void testCompareServerAdded() {
		// Setup
		keyFile.addClientFile(new ClientFile("File1.txt", "", null, "File1.txt"));

		FileCompareResult expected = new FileCompareResult("File1.txt", CompareResultType.SERVER_ADDED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertTrue(results.contains(expected));

		FileCompareResult result = fileManager.checkServerSideFile("File1.txt", keyFile);
		assertEquals(expected, result);
	}

	/**
	 * Test the check for a file being added locally
	 * 
	 * @throws IOException
	 */
	@Test
	// TODO: Fail
	public void testCompareClientAdded() throws IOException {
		// Setup
		new File(testDir, "File2.txt").createNewFile();

		FileCompareResult expected = new FileCompareResult("File2.txt", CompareResultType.LOCAL_ADDED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertTrue(results.contains(expected));

		FileCompareResult result = fileManager.checkClientSideFile("File2.txt", keyFile);
		assertEquals(expected, result);
	}

	/**
	 * Test the check for a file being removed on the server
	 * 
	 * @throws IOException
	 */
	@Test
	// TODO: Fail
	public void testCompareServerRemoved() throws IOException {
		// Setup
		localHashes.setProperty("File3.txt", "File3.txt");
		new File(testDir, "File3.txt").createNewFile();

		FileCompareResult expected = new FileCompareResult("File3.txt", CompareResultType.SERVER_REMOVED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertTrue(results.contains(expected));

		FileCompareResult result = fileManager.checkClientSideFile("File3.txt", keyFile);
		assertEquals(expected, result);
	}

	/**
	 * Test the check for a file being removed locally
	 */
	@Test
	public void testCompareClientRemoved() {
		// Setup
		keyFile.addClientFile(new ClientFile("File4.txt", "", null, "File4.txt"));
		localHashes.setProperty("File4.txt", "File4.txt");

		FileCompareResult expected = new FileCompareResult("File4.txt", CompareResultType.LOCAL_REMOVED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertTrue(results.contains(expected));

		FileCompareResult result = fileManager.checkServerSideFile("File4.txt", keyFile);
		assertEquals(expected, result);
	}

	/**
	 * Test the check for a file being updated on the server
	 * 
	 * @throws IOException
	 */
	@Test
	// TODO: Fail
	public void testCompareServerUpdated() throws IOException {
		// Setup
		keyFile.addClientFile(new ClientFile("File5.txt", "", null, "File5C.txt"));
		localHashes.setProperty("File5.txt", "File5.txt");
		new File(testDir, "File5.txt").createNewFile();

		FileCompareResult expected = new FileCompareResult("File5.txt", CompareResultType.SERVER_UPDATED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertTrue(results.contains(expected));

		FileCompareResult resultClient = fileManager.checkClientSideFile("File5.txt", keyFile);
		assertEquals(expected, resultClient);

		FileCompareResult resultServer = fileManager.checkServerSideFile("File5.txt", keyFile);
		assertEquals(expected, resultServer);
	}

	/**
	 * Test the check for a file being updated locally
	 * 
	 * @throws IOException
	 */
	@Test
	// TODO: Fail
	public void testCompareClientUpdated() throws IOException {
		// Setup
		keyFile.addClientFile(new ClientFile("File6.txt", "", null, "File6D.txt"));
		localHashes.setProperty("File6.txt", "File6D.txt");
		new File(testDir, "File6.txt").createNewFile();

		FileCompareResult expected = new FileCompareResult("File6.txt", CompareResultType.LOCAL_UPDATED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertTrue(results.contains(expected));

		FileCompareResult resultClient = fileManager.checkClientSideFile("File6.txt", keyFile);
		assertEquals(expected, resultClient);

		FileCompareResult resultServer = fileManager.checkServerSideFile("File6.txt", keyFile);
		assertEquals(expected, resultServer);
	}

	/**
	 * Test the check for a file being updated on the server and local
	 * 
	 * @throws IOException
	 */
	@Test
	// TODO: Fail
	public void testCompareConflict() throws IOException {
		// Setup
		keyFile.addClientFile(new ClientFile("File7.txt", "", null, "File7C.txt"));
		localHashes.setProperty("File7.txt", "File7O.txt");
		new File(testDir, "File7.txt").createNewFile();

		FileCompareResult expected = new FileCompareResult("File7.txt", CompareResultType.CONFLICTED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertTrue(results.contains(expected));

		FileCompareResult resultClient = fileManager.checkClientSideFile("File7.txt", keyFile);
		assertEquals(expected, resultClient);

		FileCompareResult resultServer = fileManager.checkServerSideFile("File7.txt", keyFile);
		assertEquals(expected, resultServer);
	}

}
