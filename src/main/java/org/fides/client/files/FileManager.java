package org.fides.client.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fides.client.UserProperties;

/**
 * Manages the saving an loading of files and compares what files are missing, removed or changed.
 * 
 * @author Koen
 *
 */
public class FileManager {

	/**
	 * Constructor
	 */
	public FileManager() {
		// Does nothing
	}

	/**
	 * Compares the local files and the files on a server ({@link KeyFile})
	 * 
	 * @param keyFile
	 *            The {@link KeyFile} originating from the server
	 * @return The collection of {@link FileCompareResult} with the differences between a server({@link KeyFile})
	 */
	public Collection<FileCompareResult> compareFiles(KeyFile keyFile) {
		UserProperties settings = UserProperties.getInstance();
		LocalHashes localHashes = LocalHashes.getInstance();
		List<FileCompareResult> results = new ArrayList<>();
		// Get all the names of the local stored files
		List<File> files = new ArrayList<>();
		File directory = settings.getFileDirectory();
		filesInDirectory(directory, files);
		Set<String> clientFileNames = filesToNames(files, directory);

		// We don't the files need it anymore, only the names
		files.clear();
		files = null;

		// Get all the name of the server stored files
		Set<String> serverFileNames = new HashSet<>();
		for (ClientFile clientFile : keyFile.getAllClientFiles()) {
			serverFileNames.add(clientFile.getName());
		}

		// Lets start comparing
		for (String serverName : serverFileNames) {
			// Server has the file
			if (clientFileNames.contains(serverName)) {
				// We both have the file
				String fileHash = FileUtil.generateFileHash(new File(directory, serverName));
				String savedHash = localHashes.getHash(serverName);
				boolean serverChanged = !savedHash.equals(keyFile.getClientFileByName(serverName).getHash());
				boolean localChanged = !savedHash.equals(fileHash);

				if (localChanged && serverChanged) {
					// Both server and client are changed
					results.add(new FileCompareResult(serverName, CompareResultType.CONFLICTED));
				} else if (localChanged) {
					// Client are changed
					results.add(new FileCompareResult(serverName, CompareResultType.LOCAL_UPDATED));
				} else if (serverChanged) {
					// Server are changed
					results.add(new FileCompareResult(serverName, CompareResultType.SERVER_UPDATED));
				}
				// Else nothing changed
			} else if (localHashes.containsHash(serverName)) {
				// Did exist local (its removed local)
				results.add(new FileCompareResult(serverName, CompareResultType.LOCAL_REMOVED));
			} else {
				// Did not exist here (its added on the server)
				results.add(new FileCompareResult(serverName, CompareResultType.SERVER_ADDED));
			}
		}
		for (String clientName : clientFileNames) {
			// I have the file
			if (!serverFileNames.contains(clientName)) {
				// Server has not the file
				if (localHashes.containsHash(clientName)) {
					// Did exist local (its remove on the server)
					results.add(new FileCompareResult(clientName, CompareResultType.SERVER_REMOVED));
				} else {
					// Did not exist here (its added local)
					results.add(new FileCompareResult(clientName, CompareResultType.LOCAL_ADDED));
				}
			}
		}

		return results;
	}

	/**
	 * Saves the file to the correct location, returns the hash of the file (to check its integrity)
	 * 
	 * @param fileName
	 *            The name of the file
	 * @return The {@link OutputStream} to write to the file
	 * @throws FileNotFoundException
	 */
	public OutputStream addFile(String fileName) throws FileNotFoundException {
		UserProperties settings = UserProperties.getInstance();
		File file = new File(settings.getFileDirectory(), fileName);
		return new FileOutputStream(file);
	}

	/**
	 * Saves the file to the correct location, returns the hash of the file (to check its integrity)
	 * 
	 * @param fileName
	 *            The name of the file to create, in local space
	 * @return The {@link OutputStream} to write to the file
	 * @throws FileNotFoundException
	 */
	public OutputStream updateFile(String fileName) throws FileNotFoundException {
		UserProperties settings = UserProperties.getInstance();
		File file = new File(settings.getFileDirectory(), fileName);
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
		return file.delete();
	}

	/**
	 * Reads a file
	 * 
	 * @param fileName
	 *            The name of the file to read, in local space
	 * @return An {@link InputStream} reading from the file
	 * @throws FileNotFoundException
	 */
	public InputStream readFile(String fileName) throws FileNotFoundException {
		UserProperties settings = UserProperties.getInstance();
		File file = new File(settings.getFileDirectory(), fileName);
		return new FileInputStream(file);
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
		for (File file : dirFiles) {
			if (file.isDirectory()) {
				filesInDirectory(file, files);
			} else {
				files.add(file);
			}
		}
	}

	/**
	 * Transforms a {@link List} of {@link File} to a {@link List} of {@link String}. The strings are paths relative to
	 * the directory. A sample is that with a directory "C:/somedir" a file "C:/somedir/fruit/apple" would become
	 * "fruit/apple". This is used for the name stored on the server, the directory files are save on a PC can be
	 * different.
	 * 
	 * @param files
	 * @param directory
	 * @return
	 */
	private static Set<String> filesToNames(List<File> files, File directory) {
		Set<String> fileNames = new HashSet<>();
		String baseFilePath = directory.getPath() + "\\";
		for (File file : files) {
			String fileName = file.getPath();
			if (fileName.startsWith(baseFilePath)) {
				fileName = fileName.substring(baseFilePath.length());
				fileNames.add(fileName);
			} else {
				System.out.println(file.getPath() + " : " + directory.getPath());
			}
		}
		return fileNames;
	}

}
