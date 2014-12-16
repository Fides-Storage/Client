package org.fides.client.files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.connector.EncryptedOutputStreamData;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.FileCompareResult;
import org.fides.client.files.data.KeyFile;
import org.fides.client.tools.LocalHashes;
import org.fides.client.tools.UserProperties;
import org.fides.tools.HashUtils;

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
	 */
	public synchronized boolean fileManagerCheck() {
		try {
			encManager.getConnector().connect();
		} catch (ConnectException | UnknownHostException e) {
			log.error(e);
			return false;
		}
		KeyFile keyFile;

		keyFile = encManager.requestKeyFile();

		if (keyFile == null) {
			encManager.getConnector().disconnect();
			return false;
		}

		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		for (FileCompareResult result : results) {
			handleCompareResult(result);
		}
		encManager.getConnector().disconnect();
		return true;
	}

	/**
	 * Compare the local files and server files and sync them
	 * 
	 * @param fileName
	 *            the name of the file
	 * @return true is successful
	 */
	public synchronized boolean checkClientSideFile(String fileName) {
		if (!validClientSideFile(fileName)) {
			return false;
		}

		KeyFile keyFile = null;
		try {
			encManager.getConnector().connect();
			keyFile = encManager.requestKeyFile();
		} catch (ConnectException | UnknownHostException e) {
			log.error(e);
		}

		if (keyFile == null) {
			encManager.getConnector().disconnect();
			return false;
		}

		FileCompareResult result = fileManager.checkClientSideFile(fileName, keyFile);
		log.debug(result);
		boolean completed = false;
		if (result != null) {
			completed = handleCompareResult(result);
		}
		return completed;
	}

	/**
	 * Checks if a local file is a valid client side file. This is the case when the {@link File} does exist and is a
	 * file or the {@link LocalHashes} does contain it
	 * 
	 * @param fileName
	 *            The local space name of the file
	 * @return true if valid, else false
	 */
	protected boolean validClientSideFile(String fileName) {
		File file = new File(UserProperties.getInstance().getFileDirectory(), fileName);
		return (file.exists() && file.isFile()) || LocalHashes.getInstance().containsHash(fileName);
	}

	/**
	 * Handles a {@link FileCompareResult}
	 * 
	 * @param result
	 *            The {@link FileCompareResult} to handle
	 * @return true if successfully handled, otherwise false
	 */
	private boolean handleCompareResult(FileCompareResult result) {
		boolean successful = false;
		switch (result.getResultType()) {
		case LOCAL_ADDED:
			successful = handleLocalAdded(result.getName());
			break;
		case LOCAL_REMOVED:
			successful = handleLocalRemoved(result.getName());
			break;
		case LOCAL_UPDATED:
			successful = handleLocalUpdated(result.getName());
			break;
		case SERVER_ADDED:
			// False because it is a new file
			successful = handleServerAddedOrUpdated(result.getName(), false);
			break;
		case SERVER_REMOVED:
			successful = handleServerRemoved(result.getName());
			break;
		case SERVER_UPDATED:
			// True because it is an update
			successful = handleServerAddedOrUpdated(result.getName(), true);
			break;
		case CONFLICTED:
			successful = handleConflict(result.getName());
			break;
		default:
			log.error("Invalid CompareResult");
			break;
		}
		return successful;
	}

	/**
	 * Handle a update of a file or a file being added local.
	 * 
	 * @param fileName
	 *            The file to upload
	 * @return true if successfully handled, otherwise false
	 */
	private boolean handleLocalAdded(final String fileName) {
		// Get the keyfile
		KeyFile keyFile = encManager.requestKeyFile();

		if (keyFile == null) {
			return false;
		}

		EncryptedOutputStreamData outData = encManager.uploadFile();

		// Create a message digest for creating a file hash/checksum
		MessageDigest messageDigest = FileUtil.createFileDigest();

		// Upload the file
		try (InputStream in = fileManager.readFile(fileName);
			OutputStream out = new DigestOutputStream(outData.getOutputStream(), messageDigest)) {
			IOUtils.copy(in, out);
			out.flush();
		} catch (IOException e) {
			log.error(e);
			return false;
		}

		// Check if it is successful
		boolean successful = encManager.getConnector().checkUploadSuccessful();

		// If successful update the administration
		if (successful) {
			String hash = HashUtils.toHex(messageDigest.digest());
			LocalHashes.getInstance().setHash(fileName, hash);
			keyFile.addClientFile(new ClientFile(fileName, outData.getLocation(), outData.getKey(), hash));

			// Update the keyfile TODO: what if it fails
			encManager.updateKeyFile(keyFile);
		}

		return successful;
	}

	/**
	 * Handles a remove of a local file
	 * 
	 * @param fileName
	 *            name of the removed file
	 * @return whether the remove was successful or not
	 *
	 */
	private boolean handleLocalRemoved(final String fileName) {
		// Get the keyfile
		KeyFile keyFile = encManager.requestKeyFile();

		if (keyFile == null) {
			return false;
		}

		// Get ClientFile from keyfile
		ClientFile file = keyFile.getClientFileByName(fileName);
		try {
			// Remove the file on the server
			boolean result = encManager.removeFile(file);

			if (result) {
				// Remove file from keyfile
				keyFile.removeClientFileByName(fileName);

				// Remove the local hash
				LocalHashes.getInstance().removeHash(fileName);
			}
		} catch (InvalidClientFileException e) {
			log.debug(e);
			return false;
		}

		// Update the keyfile TODO: what if it fails
		encManager.updateKeyFile(keyFile);

		return true;
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

			// Create a digest for creating a file hash/checksum
			MessageDigest messageDigest = FileUtil.createFileDigest();

			// Copy it to the server
			in = fileManager.readFile(fileName);
			out = new DigestOutputStream(outEnc, messageDigest);
			IOUtils.copy(in, out);
			out.flush();
			// TODO Check if this is the thing that broke stuff
			out.close();

			// Check if the upload was successful
			succesful = encManager.getConnector().checkUploadSuccessful();

			if (succesful) {
				// Create a hash and save it
				String hash = HashUtils.toHex(messageDigest.digest());
				LocalHashes.getInstance().setHash(fileName, hash);
				clientFile.setHash(hash);
			}
		} catch (InvalidClientFileException e) {
			log.error(e);
			succesful = false;
		} catch (IOException e) {
			log.error(e);
			succesful = false;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}

		// Update the keyfile
		if (succesful) {
			// Update the keyfile TODO: what if it fails
			encManager.updateKeyFile(keyFile);
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

		// Create a message digest for creating a file hash/checksum
		MessageDigest messageDigest = FileUtil.createFileDigest();

		// Get the right outputstream for update or creation
		OutputStream outFile;
		try {
			if (update) {
				outFile = fileManager.updateFile(fileName);
			} else {
				outFile = fileManager.addFile(fileName);
			}
		} catch (IOException e) {
			log.error(e);
			return false;
		}

		// Update the file
		try (InputStream in = encManager.requestFile(keyFile.getClientFileByName(fileName));
			OutputStream out = new DigestOutputStream(outFile, messageDigest)) {
			IOUtils.copy(in, out);

			String hexHash = HashUtils.toHex(messageDigest.digest());
			LocalHashes.getInstance().setHash(fileName, hexHash);
		} catch (IOException e) {
			log.error(e);
		} catch (InvalidClientFileException e) {
			log.error(e);
		} finally {
			IOUtils.closeQuietly(outFile);
		}

		return true;
	}

	/**
	 * Handle a removed file on the server, this will remove the file locally
	 * 
	 * @param fileName
	 *            The filename of the removed file
	 * @return Whether the file hash been removed or not
	 */
	private boolean handleServerRemoved(final String fileName) {
		UserProperties settings = UserProperties.getInstance();
		File file = new File(settings.getFileDirectory(), fileName);
		if (file.canWrite()) {
			boolean result = fileManager.removeFile(fileName);

			deleteFolder(file.getParentFile());

			if (result) {
				// Remove the local hash
				LocalHashes.getInstance().removeHash(fileName);
			}
			// Remove the File
			return result;
		}
		return false;
	}

	/**
	 * This removes the folder and all underli
	 * @param folder
	 */
	private void deleteFolder(File folder) {
		boolean isRoot = folder.equals(UserProperties.getInstance().getFileDirectory());
		//If the folder is empty, remove the folder
		File[] fileList = folder.listFiles();
		if (fileList != null && fileList.length == 0 && folder.canWrite() && !isRoot) {
			folder.delete();
			deleteFolder(folder.getParentFile());
		}
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
