package org.fides.client.files;

import java.util.TimerTask;

/**
 * A {@link TimerTask} which periodical check for changed files
 * 
 * @author Koen
 *
 */
public class FileCheckTask extends TimerTask {

	private FileSyncManager syncManager;

	/**
	 * Constructor
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
