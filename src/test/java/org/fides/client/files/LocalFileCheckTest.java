package org.fides.client.files;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.fides.client.connector.ServerConnector;
import org.fides.client.encryption.EncryptionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A {@link Runnable} which checks the local filesystem for changes
 * 
 * @author Koen
 *
 */
public class LocalFileCheckTest {

	private EncryptionManager encryptionManagerMock;

	private KeyFile keyFile;

	private Thread thread;

	@Before
	public void setUp() throws Exception {
		encryptionManagerMock = mock(EncryptionManager.class);
		keyFile = new KeyFile();
		when(encryptionManagerMock.requestKeyFile()).thenReturn(keyFile);
	}

	@After
	public void tearDown() throws Exception {
		if (thread != null && thread.isAlive()) {
			thread.interrupt();
		}
	}

	@Test
	public void test() {
		FileSyncManager syncManager = new FileSyncManager(new FileManager(), new EncryptionManager(new ServerConnector(), "Test"));
		thread = new Thread(new LocalFileChecker(syncManager));
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
