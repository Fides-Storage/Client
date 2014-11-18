package org.fides.client.files;

import java.io.Serializable;
import java.util.Collection;

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

	public ClientFile getClientFileByName(String name) {
		for (ClientFile clientFile : clientFiles) {

		}
		return null;
	}

	public ClientFile getClientFileByLocation(String location) {
		for (ClientFile clientFile : clientFiles) {

		}
		return null;
	}

}
