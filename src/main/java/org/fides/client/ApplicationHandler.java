package org.fides.client;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.files.FileCheckTask;
import org.fides.client.files.FileSyncManager;
import org.fides.client.files.LocalFileChecker;
import org.fides.client.tools.UserProperties;

/**
 * The class responsible for running and stopping all threads in the application.
 */
public class ApplicationHandler {
	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(FileSyncManager.class);

	private final FileSyncManager syncManager;

	private Timer checkTimer;

	private boolean running;

	private LocalFileChecker fileChecker;

	private FileCheckTask fileCheckTask;

	/**
	 * The constructor for the {@link ApplicationHandler}
	 * 
	 * @param syncManager
	 *            The {@link FileSyncManager} used by the application
	 */
	public ApplicationHandler(FileSyncManager syncManager) {
		this.syncManager = syncManager;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				stopApplication();
			}
		});
	}

	public FileSyncManager getSyncManager() {
		return syncManager;
	}

	/**
	 * Exits the Fides application
	 */
	public void exit() {
		stopApplication();
		System.exit(0);
	}

	/**
	 * Starts the {@link LocalFileChecker} thread and the {@link FileCheckTask} thread.
	 */
	public void startApplication() {
		if (!running) {
			LOG.debug("Starting the application");
			syncManager.reenable();

			// Starting the File Changed Listener
			fileChecker = new LocalFileChecker(syncManager);
			fileChecker.start();

			// Starting the Periodical Checker
			fileCheckTask = new FileCheckTask(syncManager);
			checkTimer = new Timer("CheckTimer");
			long timeCheck = TimeUnit.SECONDS.toMillis(UserProperties.getInstance().getCheckTimeInSeconds());
			checkTimer.scheduleAtFixedRate(fileCheckTask, 0, timeCheck);
		}
		running = true;
	}

	/**
	 * Stops the {@link LocalFileChecker} thread and the {@link FileCheckTask} thread.
	 */
	public void stopApplication() {
		if (running) {
			LOG.debug("Stopping the application");
			try {
				syncManager.waitForStop();
			} catch (InterruptedException e) {
				LOG.error("Interrupted Exception while trying to safely stop the FileSyncManager");
			}
			// Stopping the File Changed Listener
			fileChecker.stopHandling();
			fileChecker = null;

			// Stopping the Periodical Checker
			checkTimer.cancel();
			checkTimer.purge();
			fileCheckTask.cancel();

			checkTimer = null;
			fileCheckTask = null;
		}
		running = false;
	}

	/**
	 * Opens the Fides folder
	 */
	public void openFolder() {
		File folder = UserProperties.getInstance().getFileDirectory();
		if (Desktop.getDesktop().isSupported(Action.OPEN) && folder.exists()) {
			try {
				Desktop.getDesktop().open(folder);
			} catch (IOException e) {
				LOG.error("Couldn't open the folder");
			}
		}
	}
}
