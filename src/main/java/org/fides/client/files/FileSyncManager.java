package org.fides.client.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.connector.EncryptedOutputStreamData;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.encryption.KeyGenerator;
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.FileCompareResult;
import org.fides.client.files.data.KeyFile;

/**
 * Handles the synchronizing of files. It expects a fully functional and connected {@link EncryptionManager} and a
 * functional {@link FileManager}.
 * 
 * @author Koen
 *
 */
public class FileSyncManager {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(FileSyncManager.class);

	private final FileManager fileManager;

	private final EncryptionManager encManager;

	/**
	 * 
	 * @param fileManager
	 *            The {@link FileManager} to use
	 * @param encManager
	 *            The {@link EncryptionManager} to use
	 */
	public FileSyncManager(FileManager fileManager, EncryptionManager encManager) {
		super();
		this.fileManager = fileManager;
		this.encManager = encManager;
	}

	/**
	 * Compare the local files and server files and sync them
	 * 
	 * @return true is successful
	 * @throws IOException
	 */
	public synchronized boolean fileManagerCheck() {
		KeyFile keyFile;

		try {
			keyFile = encManager.requestKeyFile();
		} catch (IOException e) {
			log.error(e);
			return false;
		}
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		for (FileCompareResult result : results) {
			handleCompareResult(result);
		}

		return true;
	}

	/**
	 * Compare the local files and server files and sync them
	 * 
	 * @return true is successful
	 * @throws IOException
	 */
	public synchronized boolean checkClientFile(String fileName) {
		KeyFile keyFile;
		try {
			keyFile = encManager.requestKeyFile();
		} catch (IOException e) {
			log.error(e);
			return false;
		}

		FileCompareResult result = fileManager.checkClientSideFile(fileName, keyFile);
		if (result != null) {
			handleCompareResult(result);
		}
		return true;
	}

	/**
	 * Handles a {@link FileCompareResult}
	 * 
	 * @param result
	 *            The {@link FileCompareResult} to handle
	 */
	private void handleCompareResult(FileCompareResult result) {
		switch (result.getResultType()) {
		case LOCAL_ADDED:
			handleLocalAdded(result.getName());
			break;
		case LOCAL_REMOVED:
			handleLocalRemoved(result.getName());
			break;
		case LOCAL_UPDATED:
			handleLocalUpdated(result.getName());
			break;
		case SERVER_ADDED:
			handleServerAddedOrUpdated(result.getName(), false);
			break;
		case SERVER_REMOVED:
			handleServerRemoved(result.getName());
			break;
		case SERVER_UPDATED:
			handleServerAddedOrUpdated(result.getName(), true);
			break;
		case CONFLICTED:
			handleConflict(result.getName());
			break;
		default:
			break;
		}
	}

	/**
	 * Handle a update of a file or a file being added local.
	 * 
	 * @param fileName
	 *            The file to upload
	 */
	private void handleLocalAdded(final String fileName) {
		EncryptedOutputStreamData outData = encManager.uploadFile();
		// Get the keyfile
		KeyFile keyFile = null;
		try {
			keyFile = encManager.requestKeyFile();
		} catch (IOException e) {
			log.error(e);
			return;
		}

		// Upload the file
		MessageDigest messageDigest = FileUtil.createFileDigest();
		try (InputStream in = fileManager.readFile(fileName);
			OutputStream out = new DigestOutputStream(outData.getOutputStream(), messageDigest)) {
			IOUtils.copy(in, out);
			String hash = KeyGenerator.toHex(messageDigest.digest());
			LocalHashes.getInstance().setHash(fileName, hash);
			keyFile.addClientFile(new ClientFile(fileName, outData.getLocation(), outData.getKey(), hash));
		} catch (IOException e) {
			log.error(e);
		}

		// Update the keyfile
		encManager.uploadKeyFile(keyFile);
	}

	private void handleLocalRemoved(final String fileName) {
		// TODO handle removed local
	}

	/**
	 * Handle a update of a file or a file being updated local.
	 * 
	 * @param fileName
	 *            The file to update
	 */
	private void handleLocalUpdated(final String fileName) {
		// Get the keyfile
		KeyFile keyFile = null;
		try {
			keyFile = encManager.requestKeyFile();
		} catch (IOException e) {
			log.error(e);
			return;
		}

		// Get a stream to write to
		ClientFile clientFile = keyFile.getClientFileByName(fileName);
		OutputStream outEnc = null;
		try {
			outEnc = encManager.updateFile(clientFile);
		} catch (InvalidClientFileException e) {
			log.error(e);
			return;
		}

		// Do the update of the file
		MessageDigest messageDigest = FileUtil.createFileDigest();
		try (InputStream in = fileManager.readFile(fileName);
			OutputStream out = new DigestOutputStream(outEnc, messageDigest)) {
			IOUtils.copy(in, out);
			String hash = KeyGenerator.toHex(messageDigest.digest());
			clientFile.setHash(hash);
		} catch (IOException e) {
			log.error(e);
		}

		// Update the keyfile
		encManager.uploadKeyFile(keyFile);

	}

	/**
	 * Handle a update of a file or a file being added from the server.
	 * 
	 * @param fileName
	 *            The name to add
	 * @param update
	 *            true if it is a file update, false when file is added
	 */
	private void handleServerAddedOrUpdated(final String fileName, boolean update) {
		// Almost the same as handleServerUpdated
		KeyFile keyFile = null;
		try {
			keyFile = encManager.requestKeyFile();
		} catch (IOException e) {
			log.error(e);
			return;
		}

		MessageDigest messageDigest = FileUtil.createFileDigest();

		OutputStream outFile;
		try {
			if (update) {
				outFile = fileManager.updateFile(fileName);
			} else {
				outFile = fileManager.addFile(fileName);
			}
		} catch (FileNotFoundException e) {
			log.error(e);
			return;
		}

		try (InputStream in = encManager.requestFile(keyFile.getClientFileByName(fileName));
			OutputStream out = new DigestOutputStream(outFile, messageDigest)) {
			IOUtils.copy(in, out);

			String hexHash = KeyGenerator.toHex(messageDigest.digest());
			LocalHashes.getInstance().setHash(fileName, hexHash);
		} catch (IOException e) {
			log.error(e);
		} catch (InvalidClientFileException e) {
			log.error(e);
		}
	}

	private void handleServerRemoved(final String fileName) {
		// TODO handle removed on server
	}

	private void handleConflict(final String fileName) {
		// TODO handle conflict
	}

	public FileManager getFileManager() {
		return fileManager;
	}

	public EncryptionManager getEncManager() {
		return encManager;
	}

}
