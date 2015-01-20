package org.fides.client.files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.connector.EncryptedOutputStreamData;
import org.fides.client.connector.ServerConnector;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.encryption.InvalidPasswordException;
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.FileCompareResult;
import org.fides.client.files.data.KeyFile;
import org.fides.client.tools.CopyInterruptedException;
import org.fides.client.tools.CopyTool;
import org.fides.client.tools.LocalHashes;
import org.fides.client.tools.UserProperties;
import org.fides.client.ui.PasswordScreen;
import org.fides.client.ui.UserMessage;
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
	private static final Logger LOG = LogManager.getLogger(FileSyncManager.class);

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
	 * Checks the server for files without any references, if these are found they will be removed
	 * 
	 * @return true if the actions is successfully completed
	 */
	public synchronized boolean removeGhostFiles() {
		ServerConnector connector = encManager.getConnector();
		try {
			connector.connect();
		} catch (ConnectException | UnknownHostException e) {
			LOG.error(e);
			return false;
		}

		KeyFile keyFile = null;
		try {
			keyFile = encManager.requestKeyFile();
		} catch (InvalidPasswordException e) {
			connector.disconnect();
			requestNewPassword();
		}

		if (keyFile == null) {
			connector.disconnect();
			return false;
		}

		boolean successful = false;
		Set<String> locations = connector.requestLocations();
		if (locations != null) {
			for (String location : locations) {
				if (keyFile.getClientFileByLocation(location) == null) {
					connector.removeFile(location);
				}
			}
			successful = true;
		}

		connector.disconnect();
		return successful;
	}

	/**
	 * Compare the local files and server files and sync them
	 * 
	 * @return true is successful
	 */
	public synchronized boolean fileManagerCheck() {
		synchronized (stopLock) {
			if (stopBoolean.get()) {
				return false;
			}
			busyBoolean.set(true);
		}
		try {
			boolean successful = false;
			try {
				encManager.getConnector().connect();
			} catch (ConnectException | UnknownHostException e) {
				LOG.error(e);
				return false;
			}
			KeyFile keyFile;

			try {
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
			} catch (InvalidPasswordException e) {
				encManager.getConnector().disconnect();
				requestNewPassword();
			}
			return successful;
		} finally {
			synchronized (stopLock) {
				busyBoolean.set(false);
				stopLock.notifyAll();
			}
		}
	}

	/**
	 * Whether the current password is correct or not
	 * 
	 * @return true if the password is correct or the connection could not a established
	 */
	private boolean validatePassword() {
		try {
			encManager.getConnector().connect();
			encManager.requestKeyFile();
		} catch (ConnectException | UnknownHostException e) {
			LOG.error(e);
			return true;
		} catch (InvalidPasswordException e) {
			return false;
		} finally {
			encManager.getConnector().disconnect();
		}

		return true;
	}

	/**
	 * Asks user for there password while password is incorrect
	 */
	private void requestNewPassword() {
		while (!validatePassword() && !stopBoolean.get()) {
			ArrayList<UserMessage> messages = new ArrayList<>();
			messages.add(new UserMessage("Password is incorrect, provide your password", true));
			String passwordString = PasswordScreen.getPassword(messages, false);
			if (StringUtils.isNotBlank(passwordString)) {
				encManager.setPassword(HashUtils.hash(passwordString));
			} else {
				System.exit(0);
			}
		}
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
			if (stopBoolean.get() || !validClientSideFile(fileName)) {
				return false;
			}
			busyBoolean.set(true);
		}
		boolean successful = false;

		KeyFile keyFile = null;
		try {
			encManager.getConnector().connect();
			keyFile = encManager.requestKeyFile();
			if (keyFile == null || stopBoolean.get()) {
				encManager.getConnector().disconnect();
				return false;
			}

			FileCompareResult result = fileManager.checkClientSideFile(fileName, keyFile);
			LOG.debug(result);
			if (result != null && !stopBoolean.get()) {
				successful = handleCompareResult(result, keyFile);
			}
		} catch (ConnectException | UnknownHostException e) {
			LOG.error(e);
		} catch (InvalidPasswordException e) {
			encManager.getConnector().disconnect();
			requestNewPassword();
		} finally {
			synchronized (stopLock) {
				busyBoolean.set(false);
				stopLock.notifyAll();
			}
		}
		encManager.getConnector().disconnect();
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
			if (busyBoolean.get()) {
				stopLock.wait();
			}
		}
	}

	/**
	 * Resets all locks on the FileSyncManager, re-enabling it for normal use.
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
			LOG.error("Invalid CompareResult");
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

		// Create a message digest for creating a file hash/checksum
		MessageDigest messageDigest = FileUtil.createFileDigest();

		EncryptedOutputStreamData outData = encManager.uploadFile();

		if (outData != null) {
			InputStream in = fileManager.readFile(fileName);
			OutputStream out = new DigestOutputStream(outData.getOutputStream(), messageDigest);

			if (in == null) {
				return false;
			}

			// Upload the file
			try {
				CopyTool.copyUntil(in, out, stopBoolean);
				out.flush();
				successful = true;
			} catch (IOException e) {
				LOG.error(e);
			} catch (CopyInterruptedException e) {
				LOG.debug(e);
			} finally {
				IOUtils.closeQuietly(in);
				IOUtils.closeQuietly(out);
			}

			// Check if the upload was successful
			if (encManager.getConnector().confirmUpload(successful)) {
				// Create a hash and save it to the keyfile
				String hash = HashUtils.toHex(messageDigest.digest());
				keyFile.addClientFile(new ClientFile(fileName, outData.getLocation(), outData.getKey(), hash));

				// Upload the keyfile
				if (encManager.updateKeyFile(keyFile)) {
					// If the keyfile was uploaded successfully, update the local hashes.
					LocalHashes.getInstance().setHash(fileName, hash);
					successful = true;
				} else {
					successful = false;
				}
			}
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

		boolean successful = false;

		// Get ClientFile from keyfile
		ClientFile file = keyFile.getClientFileByName(fileName);
		try {
			// Remove the file on the server
			boolean result = encManager.removeFile(file);

			if (result) {
				// Remove file from keyfile
				keyFile.removeClientFileByName(fileName);

				// Update the keyfile
				if (encManager.updateKeyFile(keyFile)) {
					// Remove the local hash
					LocalHashes.getInstance().removeHash(fileName);
					successful = true;
				}
			}
		} catch (InvalidClientFileException e) {
			LOG.debug(e);
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
			LOG.debug("Keyfile was null while trying to handle a local updated file.");
			return false;
		}

		boolean successful = false;

		// Get a stream to write to
		OutputStream outEnc;
		InputStream in = null;
		OutputStream out = null;

		ClientFile clientFile = keyFile.getClientFileByName(fileName);

		// Create a digest for creating a file hash/checksum
		MessageDigest messageDigest = FileUtil.createFileDigest();
		try {
			outEnc = encManager.updateFile(clientFile);

			// Copy it to the server
			in = fileManager.readFile(fileName);

			if (outEnc != null && in != null) {
				out = new DigestOutputStream(outEnc, messageDigest);
				CopyTool.copyUntil(in, out, stopBoolean);
				out.flush();
				out.close();
				successful = true;
			}
		} catch (InvalidClientFileException e) {
			LOG.error(e);
			return false;
		} catch (IOException e) {
			LOG.error(e);
			successful = false;
		} catch (CopyInterruptedException e) {
			LOG.debug(e);
			successful = false;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}

		// Check if the upload was successful
		if (encManager.getConnector().confirmUpload(successful)) {
			// Create a hash and save it to the keyfile
			String hash = HashUtils.toHex(messageDigest.digest());
			clientFile.setHash(hash);

			// Upload the keyfile
			if (encManager.updateKeyFile(keyFile)) {
				// If the keyfile was uploaded successfully, update the local hashes.
				LocalHashes.getInstance().setHash(fileName, hash);
				successful = true;
			} else {
				successful = false;
			}
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

		// Get the right OutputStream for update or creation
		OutputStream outFile;
		try {
			if (update) {
				outFile = fileManager.updateFile(fileName);
			} else {
				outFile = fileManager.addFile(fileName);
			}
		} catch (IOException e) {
			LOG.error(e);
			return false;
		}
		boolean successful = false;
		// Update the file
		try (InputStream in = encManager.requestFile(keyFile.getClientFileByName(fileName));
			OutputStream out = new DigestOutputStream(outFile, messageDigest)) {
			CopyTool.copyUntil(in, out, stopBoolean);
			successful = true;
		} catch (IOException | InvalidClientFileException e) {
			LOG.error(e);
		} catch (CopyInterruptedException e) {
			LOG.debug(e);
		} finally {
			IOUtils.closeQuietly(outFile);
		}
		if (successful) {
			String hexHash = HashUtils.toHex(messageDigest.digest());
			LocalHashes.getInstance().setHash(fileName, hexHash);
		} else {
			if (fileManager.removeFile(fileName) && update) {
				LocalHashes.getInstance().removeHash(fileName);
			}
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
		// TODO: handle conflict
		return false;
	}

	public FileManager getFileManager() {
		return fileManager;
	}

	public EncryptionManager getEncManager() {
		return encManager;
	}

}
