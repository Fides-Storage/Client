package org.fides.client.files;

import java.util.TimerTask;

/**
 * A {@link TimerTask} which periodical check for changed files
 */
public final class FileCheckTask extends TimerTask {

	private FileSyncManager syncManager;

	/**
	 * Constructor for FileCheckTask
	 * 
	 * @param syncManager
	 *            The {@link FileSyncManager} used for checking
	 */
	public FileCheckTask(FileSyncManager syncManager) {
		this.syncManager = syncManager;
	}

	@Override
	public void run() {
		syncManager.fileManagerCheck();
	}

}
