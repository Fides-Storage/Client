package org.fides.client.files.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The file contains a collection of {@link ClientFile} containing the location of files on the server and the key to
 * decrypt them.
 *
 */
public class KeyFile implements Serializable {

	private static final long serialVersionUID = -3100793225474334464L;

	private final Map<String, ClientFile> clientFiles = new HashMap<>();

	/**
	 * Return a {@link ClientFile} with the given name
	 * 
	 * @param name
	 *            The name of the file
	 * @return The {@link ClientFile} if existing
	 */
	public ClientFile getClientFileByName(String name) {
		return clientFiles.get(name);
	}

	/**
	 * Return a {@link ClientFile} with the certain location
	 * 
	 * @param location
	 *            The location of the file
	 * @return The {@link ClientFile} if existing
	 */
	public ClientFile getClientFileByLocation(String location) {
		for (ClientFile clientFile : clientFiles.values()) {
			if (clientFile.getLocation().equals(location)) {
				return clientFile;
			}
		}
		return null;
	}

	/**
	 * Removes a {@link ClientFile} from the {@link KeyFile}
	 * 
	 * @param name
	 *            The name of the {@link ClientFile} to remove
	 */
	public void removeClientFileByName(String name) {
		clientFiles.remove(name);
	}

	/**
	 * Add a {@link ClientFile} to the {@link KeyFile}.
	 * 
	 * @param clientFile
	 *            THe client file to add
	 */
	public void addClientFile(ClientFile clientFile) {
		clientFiles.put(clientFile.getName(), clientFile);
	}

	/**
	 * Returns the {@link Map} of {@link ClientFile} as and unmodifiable map
	 * 
	 * @return The {@link Map} of {@link ClientFile} as and unmodifiable map
	 */
	public Map<String, ClientFile> getAllClientFiles() {
		return Collections.unmodifiableMap(clientFiles);
	}

}
