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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.connector.EncryptedOutputStreamData;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.FileCompareResult;
import org.fides.client.files.data.KeyFile;
import org.fides.client.tools.CopyInterruptedException;
import org.fides.client.tools.CopyTool;
import org.fides.client.tools.LocalHashes;
import org.fides.client.tools.UserProperties;
import org.fides.tools.HashUtils;

/**
 * Handles the synchronizing of files. It expects a fully functional and connected {@link EncryptionManager} and a
 * functional {@link FileManager}.
 * 
 */
public class FileSyncManager {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(FileSyncManager.class);

	private final FileManager fileManager;

	private final EncryptionManager encManager;

	private final Object stopLock = new Object();

	private final AtomicBoolean stopBoolean = new AtomicBoolean(false);

	private final AtomicBoolean busyBoolean = new AtomicBoolean(false);

	/**
	 * Constructor for FileSyncManager
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
		boolean successful = false;
		synchronized (stopLock) {
			if (stopBoolean.get()) {
				return false;
			}
			busyBoolean.set(true);
		}

		try {
			try {
				encManager.getConnector().connect();
			} catch (ConnectException | UnknownHostException e) {
				log.error(e);
				return false;
			}
			KeyFile keyFile;

			keyFile = encManager.requestKeyFile();

			if (keyFile == null || stopBoolean.get()) {
				encManager.getConnector().disconnect();
				successful = false;
			} else {
				Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
				successful = true;
				for (FileCompareResult result : results) {
					if (stopBoolean.get()) {
						successful = false;
						break;
					}
					handleCompareResult(result, keyFile);
				}
				encManager.getConnector().disconnect();
			}
		} finally {
			synchronized (stopLock) {
				busyBoolean.set(false);
				stopLock.notifyAll();
			}
		}
		return successful;
	}

	/**
	 * Compare the local files and server files and sync them
	 * 
	 * @param fileName
	 *            the name of the file
	 * @return true is successful
	 */
	public synchronized boolean checkClientSideFile(String fileName) {
		synchronized (stopLock) {
			if (!validClientSideFile(fileName) || stopBoolean.get()) {
				return false;
			}
			busyBoolean.set(true);
		}

		boolean successful = false;
		try {

			KeyFile keyFile = null;
			try {
				encManager.getConnector().connect();
				keyFile = encManager.requestKeyFile();
			} catch (ConnectException | UnknownHostException e) {
				log.error(e);
			}

			if (keyFile == null || stopBoolean.get()) {
				encManager.getConnector().disconnect();
				return false;
			}

			FileCompareResult result = fileManager.checkClientSideFile(fileName, keyFile);
			log.debug(result);
			if (result != null && !stopBoolean.get()) {
				successful = handleCompareResult(result, keyFile);
			}

			encManager.getConnector().disconnect();
		} finally {
			synchronized (stopLock) {
				busyBoolean.set(false);
				stopLock.notifyAll();
			}
		}
		return successful;
	}

	/**
	 * Stops the {@link FileSyncManager} from starting new critical actions and waits for the current critical action to
	 * finish.
	 * 
	 * @throws InterruptedException
	 */
	public void waitForStop() throws InterruptedException {
		synchronized (stopLock) {
			// Prevent new critical actions from starting.
			stopBoolean.set(true);
			stopLock.wait();
		}
	}

	/**
	 * Resets all locks on the FileSyncManager, reenabling it for normal use.
	 */
	public void reenable() {
		synchronized (stopLock) {
			stopBoolean.set(false);
		}
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
	private boolean handleCompareResult(FileCompareResult result, KeyFile keyFile) {
		boolean successful = false;
		switch (result.getResultType()) {
		case LOCAL_ADDED:
			successful = handleLocalAdded(result.getName(), keyFile);
			break;
		case LOCAL_REMOVED:
			successful = handleLocalRemoved(result.getName(), keyFile);
			break;
		case LOCAL_UPDATED:
			successful = handleLocalUpdated(result.getName(), keyFile);
			break;
		case SERVER_ADDED:
			// False because it is a new file
			successful = handleServerAddedOrUpdated(result.getName(), keyFile, false);
			break;
		case SERVER_REMOVED:
			successful = handleServerRemoved(result.getName());
			break;
		case SERVER_UPDATED:
			// True because it is an update
			successful = handleServerAddedOrUpdated(result.getName(), keyFile, true);
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
	private boolean handleLocalAdded(final String fileName, final KeyFile keyFile) {

		if (keyFile == null) {
			return false;
		}

		boolean successful = false;

		EncryptedOutputStreamData outData = encManager.uploadFile();

		// Create a message digest for creating a file hash/checksum
		MessageDigest messageDigest = FileUtil.createFileDigest();

		InputStream in = fileManager.readFile(fileName);
		OutputStream out = new DigestOutputStream(outData.getOutputStream(), messageDigest);

		if (in == null || out == null) {
			return false;
		}

		// Upload the file
		try {
			CopyTool.copyUntil(in, out, stopBoolean);
			out.flush();
			successful = true;
		} catch (IOException e) {
			log.error(e);
		} catch (CopyInterruptedException e) {
			log.debug(e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}

		if (successful) {
			// Check if it is successful
			successful = encManager.getConnector().checkUploadSuccessful();
		}
		// If successful update the administration
		if (successful) {

			String hash = HashUtils.toHex(messageDigest.digest());
			LocalHashes.getInstance().setHash(fileName, hash);
			keyFile.addClientFile(new ClientFile(fileName, outData.getLocation(), outData.getKey(), hash));
			// Update the keyfile TODO: what if it fails
			encManager.updateKeyFile(keyFile);

		} else {
			successful = false;
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
	private boolean handleLocalRemoved(final String fileName, final KeyFile keyFile) {
		if (keyFile == null) {
			return false;
		}

		boolean successful = true;

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

				// Update the keyfile TODO: what if it fails
				encManager.updateKeyFile(keyFile);
			}
		} catch (InvalidClientFileException e) {
			log.debug(e);
			successful = false;
		}
		return successful;
	}

	/**
	 * Handle a update of a file or a file being updated local.
	 * 
	 * @param fileName
	 *            The file to update
	 * @return true if successfully handled, otherwise false
	 */
	private boolean handleLocalUpdated(final String fileName, final KeyFile keyFile) {
		if (keyFile == null) {
			log.debug("Keyfile was null while trying to handle a local updated file.");
			return false;
		}

		boolean successful = false;

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
			CopyTool.copyUntil(in, out, stopBoolean);
			out.flush();
			out.close();

			// Check if the upload was successful
			successful = encManager.getConnector().checkUploadSuccessful();

			if (successful) {
				// Create a hash and save it
				String hash = HashUtils.toHex(messageDigest.digest());
				LocalHashes.getInstance().setHash(fileName, hash);
				clientFile.setHash(hash);
				// Update the keyfile TODO: what if it fails
				encManager.updateKeyFile(keyFile);
			}

		} catch (InvalidClientFileException e) {
			log.error(e);
			successful = false;
		} catch (IOException e) {
			log.error(e);
			successful = false;
		} catch (CopyInterruptedException e) {
			log.debug(e);
			successful = false;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
		return successful;

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
	private boolean handleServerAddedOrUpdated(final String fileName, final KeyFile keyFile, boolean update) {
		// Almost the same as handleServerUpdated
		// KeyFile keyFile = encManager.requestKeyFile();

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
		boolean successful = false;
		// Update the file
		try (InputStream in = encManager.requestFile(keyFile.getClientFileByName(fileName));
			OutputStream out = new DigestOutputStream(outFile, messageDigest)) {
			CopyTool.copyUntil(in, out, stopBoolean);
			successful = true;
		} catch (IOException e) {
			log.error(e);
		} catch (InvalidClientFileException e) {
			log.error(e);
		} catch (CopyInterruptedException e) {
			log.debug(e);
		} finally {
			IOUtils.closeQuietly(outFile);
		}
		if (successful) {
			String hexHash = HashUtils.toHex(messageDigest.digest());
			LocalHashes.getInstance().setHash(fileName, hexHash);
		}
		return successful;
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
			return result;
		}
		return false;
	}

	/**
	 * This removes the folder and all underlying folders
	 * 
	 * @param folder
	 *            the folder to remove
	 */
	private void deleteFolder(File folder) {
		boolean isRoot = folder.equals(UserProperties.getInstance().getFileDirectory());
		// If the folder is empty, remove the folder
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
