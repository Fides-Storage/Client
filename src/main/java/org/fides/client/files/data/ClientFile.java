package org.fides.client.files.data;

import java.io.Serializable;
import java.security.Key;

import org.apache.commons.lang3.StringUtils;

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
		if (!StringUtils.equals(other.location, location)) {
			return false;
		}
		if (!StringUtils.equals(other.name, name)) {
			return false;
		}
		return true;
	}

}
