package org.fides.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.fides.client.files.ClientFile;
import org.fides.client.files.FileCompareResult;
import org.fides.client.files.FileManager;
import org.fides.client.files.KeyFile;

/**
 * Hello world!
 *
 */
public class App {

	private static final String LOCAL_HASHSES_FILE = "./hashes.prop";

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Hello World!");

		FileManagerCheck();
	}

	public static void FileManagerCheck() {
		// TODO make it the real code, not half test code
		Properties localHashes = new Properties();
		try {
			File file = new File(LOCAL_HASHSES_FILE);
			if (file.exists()) {
				localHashes.loadFromXML(new FileInputStream(LOCAL_HASHSES_FILE));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileManager manager = new FileManager(localHashes);
		KeyFile keyFile = new KeyFile();
		keyFile.addClientFile(new ClientFile("Server.file", "gasdfa", null, null));
		keyFile.addClientFile(new ClientFile("SomeFiles2.txt", "gasdfa", null, null));

		Collection<FileCompareResult> results = manager.compareFiles(keyFile);
		System.out.println(results);

		System.out.println(UserSettings.getInstance().getFileDirectory());
	}
}
