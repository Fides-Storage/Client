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
 * @author Koen
 *
 */
@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest(UserProperties.class)
public class LocalFileCheckTest {

	private static final int SLEEP_TIME = 20;

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
		Mockito.when(syncManagerMock.checkClientFile(Matchers.anyString())).then(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				calledFiles.add(invocation.getArgumentAt(0, String.class));
				return true;
			}
		});

		testDir = new File("./testdir");
		if (testDir.exists()) {
			FileUtils.deleteDirectory(testDir);
		}
		testDir.mkdirs();

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
		FileUtils.deleteDirectory(testDir);
		testDir = null;
		calledFiles.clear();
		calledFiles = null;

		Thread.sleep(20);
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
		fileChange.createNewFile();
		File preSubDir = new File(testDir, "preSubDir");
		preSubDir.mkdir();

		thread = new LocalFileChecker(syncManagerMock);
		thread.start();

		Thread.sleep(SLEEP_TIME);

		File file1 = new File(testDir, "File1.txt");
		file1.createNewFile();
		Thread.sleep(SLEEP_TIME);

		File subDir = new File(testDir, "subDir");
		subDir.mkdir();
		Thread.sleep(SLEEP_TIME);

		File file2 = new File(subDir, "File2.txt");
		file2.createNewFile();
		Thread.sleep(SLEEP_TIME);

		File subDir2 = new File(subDir, "subDir2");
		subDir2.mkdirs();
		Thread.sleep(SLEEP_TIME);

		File file3 = new File(subDir2, "File3.txt");
		file3.createNewFile();
		Thread.sleep(SLEEP_TIME);

		File file4 = new File(preSubDir, "File4.txt");
		file4.createNewFile();
		Thread.sleep(SLEEP_TIME);

		try (OutputStream out = new FileOutputStream(fileChange)) {
			out.write("SomeThing".getBytes());
		}
		Thread.sleep(SLEEP_TIME);

		assertEquals(5, calledFiles.size());
		assertTrue(calledFiles.contains("File1.txt"));
		assertTrue(calledFiles.contains("fileChange.txt"));
		assertTrue(calledFiles.contains("subDir/File2.txt"));
		assertTrue(calledFiles.contains("subDir/subDir2/File3.txt"));
		assertTrue(calledFiles.contains("preSubDir/File4.txt"));
	}
}
