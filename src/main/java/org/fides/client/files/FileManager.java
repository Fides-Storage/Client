package org.fides.client.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.CompareResultType;
import org.fides.client.files.data.FileCompareResult;
import org.fides.client.files.data.KeyFile;
import org.fides.client.tools.LocalHashes;
import org.fides.client.tools.UserProperties;

/**
 * Manages the saving an loading of files and compares what files are missing, removed or changed.
 */
public class FileManager {
	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(FileManager.class);

	/**
	 * Compares the local files and the files on a server ({@link KeyFile})
	 *
	 * @param keyFile
	 *            The {@link KeyFile} originating from the server
	 * @return The collection of {@link FileCompareResult} with the differences between a server({@link KeyFile})
	 */
	public Collection<FileCompareResult> compareFiles(KeyFile keyFile) {
		UserProperties settings = UserProperties.getInstance();
		Collection<FileCompareResult> results = new HashSet<>();
		// Get all the names of the local stored files
		List<File> files = new ArrayList<>();
		File directory = settings.getFileDirectory();
		filesInDirectory(directory, files);
		Set<String> clientFileNames = filesToLocalNames(files, directory);

		// We don't the files need it anymore, only the names
		files.clear();

		// Get all the name of the server stored files
		Set<String> serverFileNames = new HashSet<>();
		for (ClientFile clientFile : keyFile.getAllClientFiles().values()) {
			serverFileNames.add(clientFile.getName());
		}

		for (String serverName : serverFileNames) {
			FileCompareResult result = checkServerSideFile(serverName, clientFileNames, keyFile);
			if (result != null) {
				results.add(result);
			}
		}
		for (String clientName : clientFileNames) {
			FileCompareResult result = checkClientSideFile(clientName, serverFileNames, keyFile);
			if (result != null) {
				results.add(result);
			}
		}

		return results;
	}

	/**
	 * The compare check for a file on the server
	 *
	 * @param serverName
	 *            The name of the file on the server
	 * @param keyFile
	 *            The {@link KeyFile} with the server files
	 * @return The {@link FileCompareResult} from the check, can be null if no result
	 */
	public FileCompareResult checkServerSideFile(String serverName, KeyFile keyFile) {
		List<File> files = new ArrayList<>();
		File directory = UserProperties.getInstance().getFileDirectory();
		filesInDirectory(directory, files);
		Set<String> clientFileNames = filesToLocalNames(files, directory);
		return checkServerSideFile(serverName, clientFileNames, keyFile);
	}

	/**
	 * The compare check for a file on the server
	 *
	 * @param serverName
	 *            The name of the file on the server
	 * @param clientFileNames
	 *            The list with files on the client
	 * @param keyFile
	 *            The {@link KeyFile} with the server files
	 * @return The {@link FileCompareResult} from the check, can be null if no result
	 */
	private FileCompareResult checkServerSideFile(String serverName, Collection<String> clientFileNames, KeyFile keyFile) {
		FileCompareResult result = null;
		// Does the file exist on the server
		if (keyFile.getClientFileByName(serverName) != null) {
			// Server has the file
			if (clientFileNames.contains(serverName)) {
				// We both have the file
				result = checkMatchingFile(serverName, keyFile);
			} else if (LocalHashes.getInstance().containsHash(serverName)) {
				// Did exist local (its removed local)
				result = new FileCompareResult(serverName, CompareResultType.LOCAL_REMOVED);
			} else {
				// Did not exist here (its added on the server)
				result = new FileCompareResult(serverName, CompareResultType.SERVER_ADDED);
			}
		}

		return result;
	}

	/**
	 * The compare check for a local file
	 *
	 * @param clientName
	 *            The name of the local file
	 * @param keyFile
	 *            The {@link KeyFile} with the server files
	 * @return The {@link FileCompareResult} from the check, can be null if no result
	 */
	public FileCompareResult checkClientSideFile(String clientName, KeyFile keyFile) {
		Set<String> serverFileNames = new HashSet<>();
		for (ClientFile clientFile : keyFile.getAllClientFiles().values()) {
			serverFileNames.add(clientFile.getName());
		}
		return checkClientSideFile(clientName, serverFileNames, keyFile);
	}

	/**
	 * The compare check for a local file
	 *
	 * @param clientName
	 *            The name of the local file
	 * @param serverFileNames
	 *            The list with files on the client
	 * @param keyFile
	 *            The {@link KeyFile} with the server files
	 * @return The {@link FileCompareResult} from the check, can be null if no result
	 */
	private FileCompareResult checkClientSideFile(String clientName, Collection<String> serverFileNames, KeyFile keyFile) {
		FileCompareResult result = null;
		// Does the local file exist
		File file = new File(UserProperties.getInstance().getFileDirectory(), clientName);
		if (file.exists() && file.isFile()) {
			// I have the file
			if (serverFileNames.contains(clientName)) {
				// We both have the file
				result = checkMatchingFile(clientName, keyFile);
			} else if (LocalHashes.getInstance().containsHash(clientName)) {
				// Did exist local (its remove on the server)
				result = new FileCompareResult(clientName, CompareResultType.SERVER_REMOVED);
			} else {
				// Did not exist here (its added local)
				result = new FileCompareResult(clientName, CompareResultType.LOCAL_ADDED);
			}
		} else if (LocalHashes.getInstance().containsHash(clientName)) {
			result = new FileCompareResult(clientName, CompareResultType.LOCAL_REMOVED);
		}

		return result;
	}

	/**
	 * The compare check for when a file exists on the server and client
	 *
	 * @param fileName
	 *            The name of the file (local and on server)
	 * @param keyFile
	 *            The {@link KeyFile} with the server files
	 * @return The {@link FileCompareResult} from the check, can be null if no result
	 */
	private FileCompareResult checkMatchingFile(String fileName, KeyFile keyFile) {
		// We both have the file
		FileCompareResult result = null;
		String fileHash = FileUtil.generateFileHash(new File(UserProperties.getInstance().getFileDirectory(), fileName));
		String savedHash = LocalHashes.getInstance().getHash(fileName);

		if (StringUtils.isBlank(savedHash)) {
			// Client has the file, server has the file, but it is not stored
			if (!fileHash.equals(keyFile.getClientFileByName(fileName).getHash())) {
				// The local and server file are different, but it did not exist before
				result = new FileCompareResult(fileName, CompareResultType.CONFLICTED);
			}
			// Else it is local and on the server and in the keyFile, do nothing
		} else {
			boolean serverChanged = !savedHash.equals(keyFile.getClientFileByName(fileName).getHash());
			boolean localChanged = !savedHash.equals(fileHash);

			if (localChanged && serverChanged) {
				// Both server and client are changed
				result = new FileCompareResult(fileName, CompareResultType.CONFLICTED);
			} else if (localChanged) {
				// Client are changed
				result = new FileCompareResult(fileName, CompareResultType.LOCAL_UPDATED);
			} else if (serverChanged) {
				// Server are changed
				result = new FileCompareResult(fileName, CompareResultType.SERVER_UPDATED);
			}
		}
		// Else nothing changed
		return result;
	}

	/**
	 * Creates a new file at the correct location of the given name and returns it as an OutputStream.
	 *
	 * @param fileName
	 *            The name of the file
	 * @return The {@link OutputStream} to write to the file
	 * @throws IOException
	 */
	public OutputStream addFile(String fileName) throws IOException {
		UserProperties settings = UserProperties.getInstance();
		File file = new File(settings.getFileDirectory(), fileName);
		if (file.exists()) {
			LOG.error("File does already exist: " + file);
			throw new IOException("File does already exist: " + file);
		}
		File parent = file.getParentFile();
		if (!parent.exists()) {
			boolean created = parent.mkdirs();
			if (!created) {
				LOG.error("File parent can not be created: " + parent);
				throw new IOException("File parent can not be created: " + parent);
			}
		}

		boolean fileCreated = file.createNewFile();
		if (!file.canWrite() || !fileCreated) {
			LOG.error("File can not be written: " + file);
			throw new IOException("File can not be written: " + file);
		}
		return new FileOutputStream(file);
	}

	/**
	 * Returns an OutputStream to update the file.
	 *
	 * @param fileName
	 *            The name of the file to create, in local space
	 * @return The {@link OutputStream} to write to the file
	 * @throws IOException
	 */
	public OutputStream updateFile(String fileName) throws IOException {
		UserProperties settings = UserProperties.getInstance();
		File file = new File(settings.getFileDirectory(), fileName);
		if (!file.exists()) {
			LOG.error("File does not exist: " + file);
			throw new IOException("File does not exist: " + file);
		}
		if (!file.canWrite()) {
			LOG.error("File can not be written: " + file);
			throw new IOException("File can not be written: " + file);
		}
		return new FileOutputStream(file);
	}

	/**
	 * Removes a file
	 *
	 * @param fileName
	 *            The name of the file to update, in local space
	 * @return true if removed
	 */
	public boolean removeFile(String fileName) {
		UserProperties settings = UserProperties.getInstance();
		File file = new File(settings.getFileDirectory(), fileName);
		if (!file.canWrite()) {
			LOG.error("File can not be written: " + file);
			return false;
		}
		return file.delete();
	}

	/**
	 * Reads a file
	 *
	 * @param fileName
	 *            The name of the file to read, in local space
	 * @return An {@link InputStream} reading from the file
	 */
	public InputStream readFile(String fileName) {
		UserProperties settings = UserProperties.getInstance();
		File file = new File(settings.getFileDirectory(), fileName);
		if (!file.canRead()) {
			LOG.error("File can not be read: " + file);
			return null;
		}
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			LOG.debug(e);
			return null;
		}
	}

	/**
	 * Add all files and {@link File} in subdirectories to a list;
	 *
	 * @param directory
	 *            The directory to look in
	 * @param files
	 *            The {@link List} to add the {@link File} to
	 */
	private static void filesInDirectory(File directory, List<File> files) {
		File[] dirFiles = directory.listFiles();
		if (dirFiles != null) {
			for (File file : dirFiles) {
				if (file.isDirectory()) {
					filesInDirectory(file, files);
				} else {
					files.add(file);
				}
			}
		}
	}

	/**
	 * Changes a {@link List} of {@link File} to a {@link List} of relative {@link String}. The strings are paths
	 * relative to the directory. A sample is that with a directory "C:/somedir" a file "C:/somedir/fruit/apple" would
	 * become "fruit/apple". This is used for the name stored on the server, the directory files can be saved
	 * differently on different PCs
	 *
	 * @param files
	 *            The file to turn to local space
	 * @param basedir
	 *            The directory to relativize to
	 * @return A set of String with local filenames
	 */
	private static Set<String> filesToLocalNames(List<File> files, File basedir) {
		Set<String> fileNames = new HashSet<>();
		for (File file : files) {
			// File relativeFile = directory.toPath().relativize(file.toPath()).toFile();
			// fileNames.add(relativeFile.getPath().replace('\\', '/')); // we always want '/'
			fileNames.add(fileToLocalName(file, basedir));
		}
		return fileNames;
	}

	/**
	 * Changes a file to a local file name
	 *
	 * @param file
	 *            The file to turn to local space
	 * @return The local file name
	 */
	public static String fileToLocalName(File file) {
		File baseDir = UserProperties.getInstance().getFileDirectory();
		return fileToLocalName(file, baseDir);
	}

	/**
	 * Changes a file to a local file name
	 *
	 * @param file
	 *            The file to turn to local space
	 * @param baseDir
	 *            The directory to relativize to
	 * @return The local file name
	 */
	private static String fileToLocalName(File file, File baseDir) {
		File relativeFile = baseDir.toPath().relativize(file.toPath()).toFile();
		// we always want '/' and no '\' this because Windows and Unix/Linux systems do not use the same
		return relativeFile.getPath().replace('\\', '/');
	}
}
