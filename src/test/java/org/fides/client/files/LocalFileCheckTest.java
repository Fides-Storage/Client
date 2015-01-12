package org.fides.client.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
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
 * A {@link Runnable} which checks the local filesystem for changes
 * 
 */
@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest(UserProperties.class)
public class LocalFileCheckTest {

	private FileSyncManager syncManagerMock;

	private File testDir;

	private Collection<String> calledFiles;

	private Thread thread;

	/**
	 * Setup before test
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		calledFiles = new HashSet<>();

		syncManagerMock = Mockito.mock(FileSyncManager.class);
		Mockito.when(syncManagerMock.checkClientSideFile(Matchers.anyString())).then(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				calledFiles.add(invocation.getArgumentAt(0, String.class));
				return true;
			}
		});

		testDir = new File(UserProperties.getInstance().getFileDirectory(), "Test");
		if (testDir.exists()) {
			FileUtils.deleteDirectory(testDir);
		}
		assertTrue(testDir.mkdirs());

		UserProperties userPropMock = Mockito.mock(UserProperties.class);
		Mockito.when(userPropMock.getFileDirectory()).thenReturn(testDir);
		PowerMockito.mockStatic(UserProperties.class);
		Mockito.when(UserProperties.getInstance()).thenReturn(userPropMock);
	}

	/**
	 * Tear down after test
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (thread != null && thread.isAlive()) {
			thread.interrupt();
		}
		thread = null;
		Thread.sleep(1000);
		FileUtils.deleteDirectory(testDir);
		testDir = null;
		calledFiles.clear();
		calledFiles = null;
	}

	/**
	 * The test
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void test() throws IOException, InterruptedException {
		File fileChange = new File(testDir, "fileChange.txt");
		assertTrue(fileChange.createNewFile());
		File preSubDir = new File(testDir, "preSubDir");
		assertTrue(preSubDir.mkdir());
		File fileRemoveSub = new File(preSubDir, "FileRemoveSub.txt");
		assertTrue(fileRemoveSub.createNewFile());

		thread = new LocalFileChecker(syncManagerMock);
		thread.start();

		// Give it some time to start
		Thread.sleep(1000);

		// Create some files
		File file1 = new File(testDir, "File1.txt");
		assertTrue(file1.createNewFile());
		File file1b = new File(testDir, "File1b.txt");
		assertTrue(file1b.createNewFile());
		File file1c = new File(testDir, "File1c.txt");
		assertTrue(file1c.createNewFile());

		// Create and remove
		File fileCreateRemove = new File(testDir, "FileCR.txt");
		assertTrue(fileCreateRemove.createNewFile());
		assertTrue(fileCreateRemove.delete());

		// Create and remove subdir
		File fileCreateRemoveSub = new File(testDir, "FileCRS.txt");
		assertTrue(fileCreateRemoveSub.createNewFile());
		assertTrue(fileCreateRemoveSub.delete());

		// Remove subdir
		assertTrue(fileRemoveSub.delete());

		// Create a sub directory
		File subDir = new File(testDir, "subDir");
		assertTrue(subDir.mkdir());

		// Create a sub directory file
		File file2 = new File(subDir, "File2.txt");
		assertTrue(file2.createNewFile());

		// Create a sub sub directory
		File subDir2 = new File(subDir, "subDir2");
		assertTrue(subDir2.mkdirs());

		// Create a sub sub directory file
		File file3 = new File(subDir2, "File3.txt");
		assertTrue(file3.createNewFile());

		// Create a existing sub directory file
		File file4 = new File(preSubDir, "File4.txt");
		assertTrue(file4.createNewFile());

		// Change existing file
		try (OutputStream out = new FileOutputStream(fileChange)) {
			out.write("SomeThing".getBytes());
		}

		// Give it some time to process
		Thread.sleep(1000);

		assertEquals(10, calledFiles.size());
		assertTrue(calledFiles.contains("File1.txt"));
		assertTrue(calledFiles.contains("File1b.txt"));
		assertTrue(calledFiles.contains("File1c.txt"));
		assertTrue(calledFiles.contains("preSubDir/FileRemoveSub.txt"));
		assertTrue(calledFiles.contains("FileCR.txt"));
		assertTrue(calledFiles.contains("FileCRS.txt"));
		assertTrue(calledFiles.contains("fileChange.txt"));
		assertTrue(calledFiles.contains("subDir/File2.txt"));
		assertTrue(calledFiles.contains("subDir/subDir2/File3.txt"));
		assertTrue(calledFiles.contains("preSubDir/File4.txt"));
	}
}
