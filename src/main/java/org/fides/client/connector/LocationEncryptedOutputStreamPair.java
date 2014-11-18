package org.fides.client.connector;

import java.io.OutputStream;
import java.security.Key;

/**
 * Contains an {@link OutputStream} to a specific location on the server. This is and extension of the
 * {@link LocationOutputStreamPair}, but also contains the key for the encrypted stream
 */
public class LocationEncryptedOutputStreamPair extends LocationOutputStreamPair {

	private Key key;

	/**
	 * Constructor
	 * 
	 * @param outputStream
	 *            The encrypted outputstream writing to the server location
	 * @param location
	 *            The location on the server
	 * @param key
	 *            The key used in the encryption
	 */
	public LocationEncryptedOutputStreamPair(OutputStream outputStream, String location, Key key) {
		super(outputStream, location);
		this.key = key;
	}

	public Key getKey() {
		return key;
	}

}
