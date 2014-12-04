package org.fides.client.files;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.UserProperties;
import org.fides.client.encryption.EncryptionManager;

/**
 * Checks the local file system for changes
 * 
 * @author Koen
 *
 */
public class LocalFileChecker extends Thread {
	/**
	 * Log for this class
	 */
	// TODO: use correct class
	private static Logger log = LogManager.getLogger(EncryptionManager.class);

	private final FileSyncManager syncManager;

	private final Map<WatchKey, Path> keys = new HashMap<>();

	private BlockingQueue<EventPair> eventsQueue = new LinkedBlockingQueue<>();

	private final Thread handleThread;

	private WatchService watcher;

	private Path basePath;

	/**
	 * Constructor
	 * 
	 * @param syncManager
	 */
	public LocalFileChecker(FileSyncManager syncManager) {
		this.syncManager = syncManager;
		handleThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (;;) {
					try {
						EventPair pair = eventsQueue.take();
						handleEvent(pair.event, pair.dir);
					} catch (InterruptedException e) {
						log.error(e);
					}
				}
			}
		}, "HandleThread");
	}

	@Override
	public void run() {
		// start the handling thread
		handleThread.start();
		try {
			setup();
		} catch (IOException e) {
			log.error(e);
			return;
		}
		for (;;) {
			// wait for key to be signaled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException e) {
				log.error(e);
				return;
			}

			// eventsQueue.add(key);
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
	 * Handles the even in a {@link WatchKey}
	 * 
	 * @param key
	 *            The key to handle
	 */
	private void handleKey(WatchKey key) {
		Path dir = keys.get(key);
		if (dir == null) {
			log.error("WatchKey not recognized!!");
			return;
		}

		for (WatchEvent<?> event : key.pollEvents()) {
			// handleEvent(event, dir);
			eventsQueue.add(new EventPair(event, dir));
		}
	}

	/**
	 * Handles a {@link WatchEvent}
	 * 
	 * @param event
	 *            The event to handle
	 * @param dir
	 *            The location of the event
	 */
	private void handleEvent(WatchEvent<?> event, Path dir) {
		// TODO: give explanation why?
		@SuppressWarnings("unchecked")
		WatchEvent<Path> watchEvent = (WatchEvent<Path>) event;
		WatchEvent.Kind<?> kind = watchEvent.kind();
		// We can ignore an Overflow
		if (kind == OVERFLOW) {
			return;
		}

		// Get he right location
		Path file = watchEvent.context();
		Path child = dir.resolve(file);
		log.debug(kind + " : " + file + " : " + child);

		if (Files.isDirectory(child)) {
			// Change is a directory
			// We want to watch it from now on
			try {
				WatchKey newKey = child.register(watcher,
					ENTRY_CREATE,
					ENTRY_DELETE,
					ENTRY_MODIFY);
				keys.put(newKey, child);
			} catch (IOException e) {
				e.printStackTrace();
				log.error(e);
			}
		} else if (Files.isRegularFile(child)) {
			// Transform string to local space and upload (or remove)
			String localName = FileManager.fileToLocalName(child.toFile());
			if (localName != null) {
				syncManager.checkClientFile(localName);
			}
		}
	}

	/**
	 * Setup the listening
	 * 
	 * @throws IOException
	 */
	private void setup() throws IOException {
		// Create a watchter and watch the file directory
		basePath = UserProperties.getInstance().getFileDirectory().toPath();
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

	@Override
	public void interrupt() {
		super.interrupt();
		handleThread.interrupt();
	}

	/**
	 * A class containing and {@link WatchEvent} and a {@link Path}
	 * 
	 * @author Koen
	 *
	 */
	private class EventPair {
		private WatchEvent<?> event;

		private Path dir;

		public EventPair(WatchEvent<?> event, Path dir) {
			this.event = event;
			this.dir = dir;
		}
	}

}
