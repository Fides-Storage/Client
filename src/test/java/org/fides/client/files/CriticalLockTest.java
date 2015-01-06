package org.fides.client.files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.Thread.State;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fides.client.connector.ServerConnector;
import org.fides.client.encryption.EncryptionManager;
import org.junit.Before;
import org.junit.Test;

/**
 * The tests for the critical- and stop-locks of the FileSyncManager
 */
public class CriticalLockTest {

	private FileSyncManager syncManager;

	private AtomicBoolean successful;

	private AtomicBoolean doneWaiting;

	private AtomicBoolean doneWaiting2;

	private Semaphore mainSemaphore;

	private Semaphore semaphoreThread1;

	private Semaphore semaphoreThread2;

	/**
	 * Sets up before each test by creating new instances of all shared variables.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		syncManager = new FileSyncManager(new FileManager(), new EncryptionManager(new ServerConnector(), "Password"));
		successful = new AtomicBoolean(false);
		doneWaiting = new AtomicBoolean(false);
		doneWaiting2 = new AtomicBoolean(false);
		mainSemaphore = new Semaphore(0);
		semaphoreThread1 = new Semaphore(0);
		semaphoreThread2 = new Semaphore(0);
	}

	/**
	 * Tests if starting a critical action will block the stopping of the FileSyncManager and if finishing the critical
	 * action will reactivate the stopping.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testStartStopCritical() throws Exception {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Step 2
					mainSemaphore.release();
					semaphoreThread1.acquire();

					// Step 4
					syncManager.waitForStop();

					// Step 6
					doneWaiting.set(true);
					mainSemaphore.release();
					semaphoreThread1.acquire();

					// Step 8
					successful.set(true);
				} catch (InterruptedException e) {
					successful.set(false);
				} finally {
					mainSemaphore.release(100);
				}
			}
		});
		try {
			// Step 1
			thread.start();
			mainSemaphore.acquire();
			semaphoreThread1.release();

			// Step 3
			syncManager.startCritical();
			semaphoreThread1.release();
			waitForThread(thread, doneWaiting);

			// Step 5
			syncManager.stopCritical();
			mainSemaphore.acquire();

			// Step 7
			assertTrue(doneWaiting.get());
			semaphoreThread1.release();
			thread.join();
		} finally {
			thread.interrupt();
		}
		assertTrue(successful.get());
	}

	/**
	 * Tests the blocking of stopping the FileSyncManager with 2 threads.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCriticalStop2Threads() throws Exception {
		Thread thread1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Step 2
					mainSemaphore.release();
					semaphoreThread1.acquire();

					// Step 4
					syncManager.waitForStop();

					// Step 6
					doneWaiting.set(true);
					mainSemaphore.release();
					semaphoreThread1.acquire();

					// Step 8
					successful.set(true);
				} catch (InterruptedException e) {
					successful.set(false);
				} finally {
					mainSemaphore.release(100);
				}
			}
		});
		Thread thread2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Step 2
					mainSemaphore.release();
					semaphoreThread2.acquire();

					// Step 4
					syncManager.waitForStop();

					// Step 6
					doneWaiting2.set(true);
					mainSemaphore.release();
					semaphoreThread2.acquire();

					// Step 8
					successful.set(true);
				} catch (InterruptedException e) {
					successful.set(false);
				} finally {
					mainSemaphore.release(100);
				}
			}
		});
		try {
			// Step 1
			thread1.start();
			thread2.start();
			mainSemaphore.acquire(2);

			// Step 3
			syncManager.startCritical();
			semaphoreThread1.release();
			semaphoreThread2.release();
			waitForThread(thread1, doneWaiting);
			waitForThread(thread2, doneWaiting2);

			// Step 5
			syncManager.stopCritical();
			mainSemaphore.acquire(2);

			// Step 7
			assertTrue(doneWaiting.get());
			semaphoreThread1.release();
			thread1.join();
			assertTrue(successful.compareAndSet(true, false));

			semaphoreThread2.release();
			thread2.join();
		} finally {
			thread1.interrupt();
			thread2.interrupt();
		}
		assertTrue(successful.get());
	}

	/**
	 * Tests if trying to start a critical operation after the syncmanager has been stopped fails.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testStartCriticalAfterStop() throws Exception {
		syncManager.waitForStop();
		assertFalse(syncManager.startCritical());
	}

	/**
	 * Tests if the reenable works correctly.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReenable() throws Exception {
		assertTrue(syncManager.startCritical());
		syncManager.stopCritical();
		syncManager.waitForStop();
		assertFalse(syncManager.startCritical());
		syncManager.waitForStop();

		syncManager.reenable();
		assertTrue(syncManager.startCritical());
		syncManager.stopCritical();
	}

	/**
	 * Tests if the reenable reenables the syncmanager by running the StartSTopCritical test twice.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testThreadedReenable() throws Exception {
		testStartStopCritical();
		syncManager.reenable();
		testStartStopCritical();
	}

	private void waitForThread(Thread thread, AtomicBoolean stoppedWaiting) throws InterruptedException {
		int maxWait = 20;
		while (thread.getState() == State.RUNNABLE && !stoppedWaiting.get()) {
			if (maxWait >= 0) {
				Thread.sleep(500);
				maxWait--;
			} else {
				fail();
			}
		}
	}
}
