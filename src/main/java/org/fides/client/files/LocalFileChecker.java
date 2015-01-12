package org.fides.client.files;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.tools.UserProperties;

/**
 * Checks the local file system for changes
 * 
 */
public class LocalFileChecker extends Thread {
	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(LocalFileChecker.class);

	private final FileSyncManager syncManager;

	private final Map<WatchKey, Path> keys = new HashMap<>();

	private final BlockingQueue<EventPair> eventsQueue = new LinkedBlockingQueue<>();

	private final Thread handleThread;

	private WatchService watcher;

	private final AtomicBoolean continueBoolean = new AtomicBoolean(true);

	/**
	 * Constructor for LocalFileChecker. Creates an extra thread to handle the events.
	 * 
	 * @param syncManager
	 *            The FileSyncManager to use
	 */
	public LocalFileChecker(FileSyncManager syncManager) {
		super("LocalFileChecker Thread");
		this.syncManager = syncManager;
		handleThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (continueBoolean.get()) {
						EventPair pair = eventsQueue.take();
						handleEvent(pair.kind, pair.child);
					}
				} catch (InterruptedException e) {
					LOG.error("LocalFileChecker handle interrupted: " + e);
				}
			}
		}, "Handle Thread");
	}

	@Override
	public void run() {
		// start the handling thread
		handleThread.start();
		try {
			setup();
		} catch (IOException e) {
			LOG.error(e);
			return;
		}
		while (continueBoolean.get()) {
			// wait for key to be signaled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException e) {
				LOG.debug(e);
				return;
			}

			handleKey(key);

			// Reset the key -- this step is critical if you want to
			// receive further watch events. If the key is no longer valid,
			// the directory is inaccessible so exit the loop.
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);

				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
		} // End for(;;) loop
	} // End run

	/**
	 * Stop the watching and handling of local file changes. This does interrupt the watching thread
	 */
	public void stopHandling() {
		continueBoolean.set(false);
		this.interrupt();
	}

	/**
	 * Handles the even in a {@link WatchKey}
	 * 
	 * @param key
	 *            The key to handle
	 */
	private void handleKey(WatchKey key) {
		Path dir = keys.get(key);
		if (dir == null) {
			LOG.error("WatchKey not recognized!!");
			return;
		}

		for (WatchEvent<?> event : key.pollEvents()) {
			// Get the right location
			Path file = (Path) event.context();
			Path child = dir.resolve(file);
			EventPair pair = new EventPair(event.kind(), child);

			if (!eventsQueue.contains(pair)) {
				eventsQueue.add(pair);
			}
		}
	}

	/**
	 * Handles a {@link WatchEvent}
	 * 
	 * @param kind
	 *            The ind of event to handle
	 * @param child
	 *            The location of the event
	 */
	private void handleEvent(WatchEvent.Kind<?> kind, Path child) {
		// We can ignore an Overflow
		if (kind == OVERFLOW) {
			return;
		}

		LOG.debug(kind + " : " + child);

		if (Files.isRegularFile(child)) {
			// Transform string to local space and upload (or remove)
			String localName = FileManager.fileToLocalName(child.toFile());
			if (!StringUtils.isBlank(localName)) {
				syncManager.checkClientSideFile(localName);
			}
		} else if (Files.isDirectory(child)) {
			// Change is a directory
			if (kind == ENTRY_CREATE) {
				// We want to watch it from now on
				try {
					WatchKey newKey = child.register(watcher,
						ENTRY_CREATE,
						ENTRY_DELETE,
						ENTRY_MODIFY);
					keys.put(newKey, child);
				} catch (IOException e) {
					e.printStackTrace();
					LOG.error(e);
				}
				checkDirectory(child);
			}
		} else if (kind == ENTRY_DELETE) {
			// Transform string to local space and remove
			String localName = FileManager.fileToLocalName(child.toFile());
			if (!StringUtils.isBlank(localName)) {
				syncManager.checkClientSideFile(localName);
			}
		}
	}

	/**
	 * Check the files in the directory.
	 * 
	 * @param child
	 *            The folder to check
	 */
	private void checkDirectory(Path child) {
		// It is still possible that is some way files are added before it being added, this will check the
		// change directory
		File subPathFile = child.toFile();
		File[] fileList = subPathFile.listFiles();
		if (fileList != null) {
			for (File subFile : fileList) {
				checkSubPath(subFile.toPath());
			}
		}
	}

	/**
	 * When Recursively checks a directory
	 * 
	 * @param subPath
	 *            The directory to check
	 */
	private void checkSubPath(Path subPath) {
		if (Files.isRegularFile(subPath)) {
			// Transform string to local space and upload (or remove)
			String localName = FileManager.fileToLocalName(subPath.toFile());
			if (!StringUtils.isBlank(localName)) {
				syncManager.checkClientSideFile(localName);
			}
		} else if (Files.isDirectory(subPath)) {
			try {
				WatchKey newKey = subPath.register(watcher,
					ENTRY_CREATE,
					ENTRY_DELETE,
					ENTRY_MODIFY);
				keys.put(newKey, subPath);
			} catch (IOException e) {
				LOG.error(e);
			}
			checkDirectory(subPath);
		}
	}

	/**
	 * Setup the listening
	 * 
	 * @throws IOException
	 */
	private void setup() throws IOException {
		// Create a watchter and watch the file directory
		Path basePath = UserProperties.getInstance().getFileDirectory().toPath();
		watcher = FileSystems.getDefault().newWatchService();
		WatchKey key = basePath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		keys.put(key, basePath);

		// Also watch all sub directories
		Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				WatchKey subKey = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
				keys.put(subKey, dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * A class containing and {@link WatchEvent} and a {@link Path}
	 * 
	 */
	private static final class EventPair {
		private final WatchEvent.Kind<?> kind;

		private final Path child;

		public EventPair(WatchEvent.Kind<?> kind, Path child) {
			this.kind = kind;
			this.child = child;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + child.hashCode();
			result = prime * result + kind.name().hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			EventPair other = (EventPair) obj;

			if (!child.equals(other.child)) {
				return false;
			}
			if (!kind.name().equals(other.kind.name())) {
				return false;
			}

			return true;
		}

	}

}
