package org.fides.client.files;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * The file containins a collection of {@link ClientFile} containing the location of files on the server and the key to
 * decrypt them.
 * 
 * @author Koen
 *
 */
public class KeyFile implements Serializable {

	private static final long serialVersionUID = -3100793225474334464L;

	private Collection<ClientFile> clientFiles;

	/**
	 * Constructor
	 */
	public KeyFile() {
		clientFiles = new ArrayList<>();
	}

	/**
	 * Return a {@link ClientFile} with the given name
	 * 
	 * @param name
	 *            The name of the file
	 * @return The {@link ClientFile} if existing
	 */
	public ClientFile getClientFileByName(String name) {
		for (ClientFile clientFile : clientFiles) {
			if (clientFile.getName().equals(name)) {
				return clientFile;
			}
		}
		return null;
	}

	/**
	 * Return a {@link ClientFile} with the certain location
	 * 
	 * @param location
	 *            The location of the file
	 * @return The {@link ClientFile} if existing
	 */
	public ClientFile getClientFileByLocation(String location) {
		for (ClientFile clientFile : clientFiles) {
			if (clientFile.getLocation().equals(location)) {
				return clientFile;
			}
		}
		return null;
	}

	/**
	 * Add a {@link ClientFile} to the {@link KeyFile}.
	 * 
	 * @param clientFile
	 */
	public void addClientFile(ClientFile clientFile) {
		clientFiles.add(clientFile);
	}

	/**
	 * Returns the {@link Collection} of {@link ClientFile} as and unmodifiable collection
	 * 
	 * @return The {@link Collection} of {@link ClientFile} as and unmodifiable collection
	 */
	public Collection<ClientFile> getAllClientFiles() {
		return Collections.unmodifiableCollection(clientFiles);
	}

}
