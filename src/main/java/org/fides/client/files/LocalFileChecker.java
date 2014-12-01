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
public class LocalFileChecker implements Runnable {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(EncryptionManager.class);

	private final FileSyncManager syncManager;

	private final Map<WatchKey, Path> keys = new HashMap<>();

	private WatchService watcher;

	private Path basePath;

	/**
	 * Constructor
	 * 
	 * @param syncManager
	 */
	public LocalFileChecker(FileSyncManager syncManager) {
		this.syncManager = syncManager;
	}

	@Override
	public void run() {
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

			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				@SuppressWarnings("unchecked")
				WatchEvent<Path> watchEvent = (WatchEvent<Path>) event;
				WatchEvent.Kind<?> kind = watchEvent.kind();
				if (kind == OVERFLOW) {
					continue;
				}

				Path file = watchEvent.context();
				Path child = dir.resolve(file);
				log.debug(kind + " : " + file + " : " + child);

				if (Files.isDirectory(child)) {
					// Change is a directory
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
				} else if (Files.isRegularFile(child) || kind == ENTRY_DELETE) {
					// It is a file
					String childName = child.toString();
					String basePathName = basePath.toString();

					// Transform string to local space and upload (or remove)
					if (childName.startsWith(basePathName)) {
						String localName = childName.substring(basePathName.length() + 1);
						log.debug("Local name: " + localName);
						syncManager.checkClientFile(localName);
					}
				}
			}

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

	private void setup() throws IOException {
		basePath = UserProperties.getInstance().getFileDirectory().toPath();
		watcher = FileSystems.getDefault().newWatchService();
		WatchKey key = basePath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		keys.put(key, basePath);

		Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				WatchKey subKey = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
				keys.put(subKey, dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

}
