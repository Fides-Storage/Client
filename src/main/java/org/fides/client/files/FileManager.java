package org.fides.client.files;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public class FileManager {

	private Map<String, String> localHashes;

	/**
	 * Saves the file to the correct location, returns the hash of the file (to check its integrity)
	 */
	public String addFile(InputStream instream, String name) {
		return null;
	}

	public Collection<FileCompareResult> compareFiles(KeyFile files) {
		return null;
	}

	public boolean removeFile(String name) {
		return false;
	}

	/**
	 * Saves the file to the correct location, returns the hash of the file (to check its integrity)
	 */
	public String updateFile(InputStream instream, String name) {
		return null;
	}

}
