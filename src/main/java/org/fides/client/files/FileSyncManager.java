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
import org.fides.client.tools.LocalHashes;

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

		keyFile = encManager.requestKeyFile();
		if (keyFile == null) {
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
		KeyFile keyFile = encManager.requestKeyFile();
		if (keyFile == null) {
			return false;
		}

		FileCompareResult result = fileManager.checkClientSideFile(fileName, keyFile);
		if (result != null) {
			return handleCompareResult(result);
		}
		return false;
	}

	/**
	 * Handles a {@link FileCompareResult}
	 * 
	 * @param result
	 *            The {@link FileCompareResult} to handle
	 * @return true if successfully handled, otherwise false
	 */
	private boolean handleCompareResult(FileCompareResult result) {
		boolean succesful = false;
		switch (result.getResultType()) {
		case LOCAL_ADDED:
			succesful = handleLocalAdded(result.getName());
			break;
		case LOCAL_REMOVED:
			succesful = handleLocalRemoved(result.getName());
			break;
		case LOCAL_UPDATED:
			succesful = handleLocalUpdated(result.getName());
			break;
		case SERVER_ADDED:
			// False because it is a new file
			succesful = handleServerAddedOrUpdated(result.getName(), false);
			break;
		case SERVER_REMOVED:
			succesful = handleServerRemoved(result.getName());
			break;
		case SERVER_UPDATED:
			// True because it is an update
			succesful = handleServerAddedOrUpdated(result.getName(), true);
			break;
		case CONFLICTED:
			succesful = handleConflict(result.getName());
			break;
		default:
			log.error("Invalid CompareResult");
			break;
		}
		return succesful;
	}

	/**
	 * Handle a update of a file or a file being added local.
	 * 
	 * @param fileName
	 *            The file to upload
	 * @return true if successfully handled, otherwise false
	 */
	private boolean handleLocalAdded(final String fileName) {
		EncryptedOutputStreamData outData = encManager.uploadFile();
		// Get the keyfile
		KeyFile keyFile = encManager.requestKeyFile();
		if (keyFile == null) {
			return false;
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
			return false;
		}

		// Update the keyfile
		encManager.uploadKeyFile(keyFile);
		return true;
	}

	private boolean handleLocalRemoved(final String fileName) {
		// TODO handle removed local
		return false;
	}

	/**
	 * Handle a update of a file or a file being updated local.
	 * 
	 * @param fileName
	 *            The file to update
	 * @return true if successfully handled, otherwise false
	 */
	private boolean handleLocalUpdated(final String fileName) {
		// Get the keyfile
		KeyFile keyFile = encManager.requestKeyFile();
		if (keyFile == null) {
			return false;
		}

		boolean succesful = false;

		// Get a stream to write to
		OutputStream outEnc = null;
		InputStream in = null;
		OutputStream out = null;
		try {
			ClientFile clientFile = keyFile.getClientFileByName(fileName);
			outEnc = encManager.updateFile(clientFile);

			MessageDigest messageDigest = FileUtil.createFileDigest();
			in = fileManager.readFile(fileName);
			out = new DigestOutputStream(outEnc, messageDigest);
			IOUtils.copy(in, out);
			String hash = KeyGenerator.toHex(messageDigest.digest());
			clientFile.setHash(hash);

			succesful = true;
		} catch (InvalidClientFileException e) {
			log.error(e);
			succesful = false;
		} catch (IOException e) {
			log.error(e);
			succesful = false;
		}

		// Update the keyfile
		if (succesful) {
			encManager.uploadKeyFile(keyFile);
		}
		return succesful;

	}

	/**
	 * Handle a update of a file or a file being added from the server.
	 * 
	 * @param fileName
	 *            The name to add
	 * @param update
	 *            true if it is a file update, false when file is added
	 * @return true if successfully handled, otherwise false
	 */
	private boolean handleServerAddedOrUpdated(final String fileName, boolean update) {
		// Almost the same as handleServerUpdated
		KeyFile keyFile = encManager.requestKeyFile();
		if (keyFile == null) {
			return false;
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
			return false;
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
		return true;
	}

	private boolean handleServerRemoved(final String fileName) {
		// TODO handle removed on server
		return false;
	}

	private boolean handleConflict(final String fileName) {
		// TODO handle conflict
		return false;
	}

	public FileManager getFileManager() {
		return fileManager;
	}

	public EncryptionManager getEncManager() {
		return encManager;
	}

}
