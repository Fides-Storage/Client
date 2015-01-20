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
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.CompareResultType;
import org.fides.client.files.data.FileCompareResult;
import org.fides.client.files.data.KeyFile;
import org.fides.client.tools.LocalHashes;
import org.fides.client.tools.UserProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for the {@link FileManager}
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ UserProperties.class, FileUtil.class, LocalHashes.class })
@PowerMockIgnore("javax.management.*")
public class FileManagerCompareTest {

	private UserProperties settingsMock;

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
		assertTrue(testDir.mkdir());
		testDir.deleteOnExit();

		// Mock the UserProperties so it returns the test directory
		settingsMock = mock(UserProperties.class);
		when(settingsMock.getFileDirectory()).thenReturn(testDir);
		PowerMockito.mockStatic(UserProperties.class);
		Mockito.when(UserProperties.getInstance()).thenReturn(settingsMock);

		// Mock the LocalHashes so it uses our own hashes
		LocalHashes localHashesMock = mock(LocalHashes.class);
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
		final String fileName = "File1.txt";
		// Setup
		keyFile.addClientFile(new ClientFile(fileName, "", null, fileName));

		FileCompareResult expected = new FileCompareResult("File1.txt", CompareResultType.SERVER_ADDED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertEquals(1, results.size());
		assertTrue(results.contains(expected));

		FileCompareResult result = fileManager.checkServerSideFile(fileName, keyFile);
		assertEquals(expected, result);
	}

	/**
	 * Test the check for a file being added locally
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCompareClientAdded() throws IOException {
		final String fileName = "File2.txt";
		// Setup
		assertTrue(new File(testDir, fileName).createNewFile());

		FileCompareResult expected = new FileCompareResult(fileName, CompareResultType.LOCAL_ADDED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertEquals(1, results.size());
		assertTrue(results.contains(expected));

		FileCompareResult result = fileManager.checkClientSideFile(fileName, keyFile);
		assertEquals(expected, result);
	}

	/**
	 * Test the check for a file being removed on the server
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCompareServerRemoved() throws IOException {
		final String fileName = "File3.txt";
		// Setup
		localHashes.setProperty(fileName, fileName);
		assertTrue(new File(testDir, fileName).createNewFile());

		FileCompareResult expected = new FileCompareResult(fileName, CompareResultType.SERVER_REMOVED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertEquals(1, results.size());
		assertTrue(results.contains(expected));

		FileCompareResult result = fileManager.checkClientSideFile(fileName, keyFile);
		assertEquals(expected, result);
	}

	/**
	 * Test the check for a file being removed locally
	 */
	@Test
	public void testCompareClientRemoved() {
		final String fileName = "File4.txt";
		// Setup
		keyFile.addClientFile(new ClientFile(fileName, "", null, fileName));
		localHashes.setProperty(fileName, fileName);

		FileCompareResult expected = new FileCompareResult(fileName, CompareResultType.LOCAL_REMOVED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertEquals(1, results.size());
		assertTrue(results.contains(expected));

		FileCompareResult result = fileManager.checkServerSideFile(fileName, keyFile);
		assertEquals(expected, result);
	}

	/**
	 * Test the check for a file being updated on the server
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCompareServerUpdated() throws IOException {
		final String fileName = "File5.txt";
		final String fileNameC = "File5C.txt";

		// Setup
		keyFile.addClientFile(new ClientFile(fileName, "", null, fileNameC));
		localHashes.setProperty(fileName, fileName);
		assertTrue(new File(testDir, fileName).createNewFile());

		FileCompareResult expected = new FileCompareResult(fileName, CompareResultType.SERVER_UPDATED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertEquals(1, results.size());
		assertTrue(results.contains(expected));

		FileCompareResult resultClient = fileManager.checkClientSideFile(fileName, keyFile);
		assertEquals(expected, resultClient);

		FileCompareResult resultServer = fileManager.checkServerSideFile(fileName, keyFile);
		assertEquals(expected, resultServer);
	}

	/**
	 * Test the check for a file being updated locally
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCompareClientUpdated() throws IOException {
		final String fileName = "File6.txt";
		final String fileNameD = "File6D.txt";

		// Setup
		keyFile.addClientFile(new ClientFile(fileName, "", null, fileNameD));
		localHashes.setProperty(fileName, fileNameD);
		assertTrue(new File(testDir, fileName).createNewFile());

		FileCompareResult expected = new FileCompareResult(fileName, CompareResultType.LOCAL_UPDATED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertEquals(1, results.size());
		assertTrue(results.contains(expected));

		FileCompareResult resultClient = fileManager.checkClientSideFile(fileName, keyFile);
		assertEquals(expected, resultClient);

		FileCompareResult resultServer = fileManager.checkServerSideFile(fileName, keyFile);
		assertEquals(expected, resultServer);
	}

	/**
	 * Test the check for a file being updated on the server and local
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCompareConflict() throws IOException {
		final String fileName = "File7.txt";
		final String fileNameC = "File7C.txt";
		final String fileNameO = "File7O.txt";

		// Setup
		keyFile.addClientFile(new ClientFile(fileName, "", null, fileNameC));
		localHashes.setProperty(fileName, fileNameO);
		assertTrue(new File(testDir, fileName).createNewFile());

		FileCompareResult expected = new FileCompareResult(fileName, CompareResultType.CONFLICTED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertEquals(1, results.size());
		assertTrue(results.contains(expected));

		FileCompareResult resultClient = fileManager.checkClientSideFile(fileName, keyFile);
		assertEquals(expected, resultClient);

		FileCompareResult resultServer = fileManager.checkServerSideFile(fileName, keyFile);
		assertEquals(expected, resultServer);
	}

	/**
	 * Test the check for a file existing local and on the server but not in hashes, the files are the same
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCompareServerLocalSame() throws IOException {
		final String fileName = "File7.txt";

		// Setup
		keyFile.addClientFile(new ClientFile(fileName, "", null, fileName));
		assertTrue(new File(testDir, fileName).createNewFile());

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertEquals(0, results.size());

		FileCompareResult resultClient = fileManager.checkClientSideFile(fileName, keyFile);
		assertTrue(resultClient == null);

		FileCompareResult resultServer = fileManager.checkServerSideFile(fileName, keyFile);
		assertTrue(resultServer == null);
	}

	/**
	 * Test the check for a file existing local and on the server but not in hashes, the files are different
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCompareServerLocalDifferent() throws IOException {
		final String fileName = "File7.txt";
		final String fileNameD = "File7D.txt";

		// Setup
		keyFile.addClientFile(new ClientFile(fileName, "", null, fileNameD));
		assertTrue(new File(testDir, fileName).createNewFile());

		FileCompareResult expected = new FileCompareResult(fileName, CompareResultType.CONFLICTED);

		// Test
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		assertEquals(1, results.size());
		assertTrue(results.contains(expected));

		FileCompareResult resultClient = fileManager.checkClientSideFile(fileName, keyFile);
		assertEquals(expected, resultClient);

		FileCompareResult resultServer = fileManager.checkServerSideFile(fileName, keyFile);
		assertEquals(expected, resultServer);
	}

}
