package org.fides.client.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.fides.client.encryption.EncryptionManager;
import org.fides.client.encryption.KeyGenerator;

/**
 * Handles the synchronizing of files
 * 
 * @author Koen
 *
 */
public class FileSyncManager {

	private FileManager fileManager;

	private EncryptionManager encManager;

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
	 * @throws IOException
	 */
	public void fileManagerCheck() throws IOException {
		KeyFile keyFile = encManager.requestKeyFile();
		Collection<FileCompareResult> results = fileManager.compareFiles(keyFile);
		for (FileCompareResult result : results) {
			handleCompareResult(result);
		}
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

	private void handleLocalAdded(final String fileName) {
		// TODO handle removed added

		// Sorry for starting this to early, this has to be done later in FS-10
		// EncryptedOutputStreamData outData = encManager.uploadFile();
		// KeyFile keyFile = null;
		// try {
		// keyFile = encManager.requestKeyFile();
		// } catch (IOException e) {
		// // TODO proper handling
		// e.printStackTrace();
		// return;
		// }
		// MessageDigest messageDigest = FileUtil.createFileDigest();
		//
		// try (InputStream in = fileManager.readFile(result.getName());
		// OutputStream out = new DigestOutputStream(outData.getOutputStream(), messageDigest)) {
		// IOUtils.copy(in, out);
		// keyFile.addClientFile(new ClientFile(result.getName(), outData.getLocation(), outData.getKey(),
		// KeyGenerator.toHex(messageDigest.digest())));
		// encManager.uploadKeyFile(keyFile);
		// } catch (IOException e) {
		// // TODO proper handling
		// e.printStackTrace();
		// }
	}

	private void handleLocalRemoved(final String fileName) {
		// TODO handle removed local
	}

	private void handleLocalUpdated(final String fileName) {
		// TODO handle updated local
	}

	/**
	 * Handle a update of a file or a file being added from the server.
	 * 
	 * @param fileName
	 *            The name to add
	 * @param update
	 *            true if it is a file update, false when file is new
	 */
	private void handleServerAddedOrUpdated(final String fileName, boolean update) {
		// Almost the same as handleServerUpdated
		KeyFile keyFile = null;
		try {
			keyFile = encManager.requestKeyFile();
		} catch (IOException e) {
			// TODO proper handling
			e.printStackTrace();
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
			// TODO proper handling
			e.printStackTrace();
			return;
		}

		try (InputStream in = encManager.requestFile(keyFile.getClientFileByName(fileName));
			OutputStream out = new DigestOutputStream(outFile, messageDigest)) {
			IOUtils.copy(in, out);

			String hexHash = KeyGenerator.toHex(messageDigest.digest());
			LocalHashes.getInstance().setHash(fileName, hexHash);
		} catch (IOException e) {
			// TODO proper handling
			e.printStackTrace();
		} catch (InvalidClientFileException e) {
			// TODO proper handling
			e.printStackTrace();
		}
	}

	private void handleServerRemoved(final String fileName) {
		// TODO handle removed on server
	}

	private void handleConflict(final String fileName) {
		// TODO handle conflict
	}

}
