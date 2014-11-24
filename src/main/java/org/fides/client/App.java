package org.fides.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;

import org.fides.client.files.FileCompareResult;
import org.fides.client.files.FileManager;
import org.fides.client.files.KeyFile;

/**
 * Hello world!
 *
 */
public class App {

	/** Where to find the local hashes */
	private static final String LOCAL_HASHSES_FILE = "./hashes.prop";

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Hello World!");

		fileManagerCheck();
	}

	/**
	 * Check the differences between the files local and on the server
	 */
	private static void fileManagerCheck() {
		// TODO make it the real code, not half test code
		Properties localHashes = new Properties();
		try (InputStream in = new FileInputStream(LOCAL_HASHSES_FILE)) {
			File file = new File(LOCAL_HASHSES_FILE);
			if (file.exists()) {
				localHashes.loadFromXML(in);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileManager manager = new FileManager(localHashes); // TODO maybe remember it
		KeyFile keyFile = new KeyFile(); // TODO get from the server

		Collection<FileCompareResult> results = manager.compareFiles(keyFile);
		System.out.println(results);
	}

}
