package org.fides.client;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.files.FileCheckTask;
import org.fides.client.files.FileSyncManager;
import org.fides.client.files.LocalFileChecker;
import org.fides.client.tools.UserProperties;

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

	public ApplicationHandler(FileSyncManager syncManager) {
		this.syncManager = syncManager;
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

	public void startApplication() {
		if (!running) {
			System.out.println("Start Application");
			syncManager.reenable();

			// File Changed Listener opstarten
			fileChecker = new LocalFileChecker(syncManager);
			fileChecker.start();

			// Periodieke Checker opstarten
			fileCheckTask = new FileCheckTask(syncManager);
			checkTimer = new Timer("CheckTimer");
			long timeCheck = TimeUnit.SECONDS.toMillis(UserProperties.getInstance().getCheckTimeInSeconds());
			checkTimer.scheduleAtFixedRate(fileCheckTask, 0, timeCheck);
		}
		running = true;
	}

	public void stopApplication() {
		if (running) {
			System.out.println("Stop Application");
			try {
				syncManager.waitForStop();
			} catch (InterruptedException e) {
				LOG.error("Interrupted Exception while trying to safely stop the FileSyncManager");
			}
			// File Changed Listener stoppen
			fileChecker.stopHandling();
			fileChecker = null;

			// Periodieke Checker stoppen
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
		if (Desktop.getDesktop().isSupported(Action.OPEN)) {
			try {
				Desktop.getDesktop().open(UserProperties.getInstance().getFileDirectory());
			} catch (IOException e) {
				LOG.error("Couldn't open the folder");
			}
		}
	}
}
