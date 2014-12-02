package org.fides.client.files.data;

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

	public void setHash(String hash) {
		this.hash = hash;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ClientFile other = (ClientFile) obj;
		if (location == null) {
			if (other.location != null) {
				return false;
			}
		} else if (!location.equals(other.location)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

}
