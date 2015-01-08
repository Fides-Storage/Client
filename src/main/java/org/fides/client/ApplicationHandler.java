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
	private static Logger log = LogManager.getLogger(FileSyncManager.class);

	private FileSyncManager syncManager;

	private Timer checkTimer;

	private boolean running;

	private LocalFileChecker fileChecker;

	private FileCheckTask fileCheckTask;

	public ApplicationHandler(FileSyncManager syncManager) {
		this.syncManager = syncManager;
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
			try {
				syncManager.waitForStop();
			} catch (InterruptedException e) {
				log.error("Interrupted Exception while trying to safely stop the FileSyncManager");
			}
			// TODO: File Changed Listener stoppen

			// Periodieke Checker stoppen
			checkTimer.cancel();
			checkTimer.purge();
			fileCheckTask.cancel();
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
				log.error("Couldn't open the folder");
			}
		}
	}
}
