package org.fides.client.files;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.fides.client.tools.UserProperties;

/**
 * A {@link TimerTask} which periodical check for changed files
 */
public final class FileCheckTask extends TimerTask {

	private static Timer timer;

	private FileSyncManager syncManager;

	/**
	 * Constructor
	 * 
	 * @param syncManager
	 *            The {@link FileSyncManager} used for checking
	 */
	private FileCheckTask(FileSyncManager syncManager) {
		this.syncManager = syncManager;
	}

	@Override
	public void run() {
		syncManager.fileManagerCheck();
	}

	/**
	 * Starts {@link FileCheckTask} that periodically checks for updates
	 * 
	 * @param syncManager
	 *            The {@link FileSyncManager} the
	 */
	public static void startCheckTimer(FileSyncManager syncManager) {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer("CheckTimer");

		long timeCheck = TimeUnit.SECONDS.toMillis(UserProperties.getInstance().getCheckTimeInSeconds());
		timer.scheduleAtFixedRate(new FileCheckTask(syncManager), 0, timeCheck);
	}

	/**
	 * Stop the {@link FileCheckTask} that checks for updates
	 */
	public static void stopCheckTimer() {
		if (timer != null) {
			timer.cancel();
		}
	}

}
