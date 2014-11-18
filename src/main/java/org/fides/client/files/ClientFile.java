package org.fides.client.files;

import java.io.Serializable;
import java.security.Key;

/**
 * Contains the information of the user's files on the servers
 * 
 * @author Koen
 *
 */
public class ClientFile implements Serializable {

	private static final long serialVersionUID = -2910237556924682964L;

	private String name;

	private String location;

	private Key key;

	private String hash;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            Name of the file
	 * @param location
	 *            Location of the file on the server
	 * @param key
	 *            The for encrypting and decrypting the file
	 * @param hash
	 *            The hash of the file
	 */
	public ClientFile(String name, String location, Key key, String hash) {
		super();
		this.name = name;
		this.location = location;
		this.key = key;
		this.hash = hash;
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public Key getKey() {
		return key;
	}

	public String getHash() {
		return hash;
	}

}
