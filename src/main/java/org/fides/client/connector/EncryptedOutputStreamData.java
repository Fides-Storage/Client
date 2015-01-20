package org.fides.client.connector;

import java.io.OutputStream;
import java.security.Key;

/**
 * Contains an {@link OutputStream} to a specific location on the server. This is and extension of the
 * {@link OutputStreamData}, but also contains the key for the encrypted stream
 */
public class EncryptedOutputStreamData extends OutputStreamData {

	private final Key key;

	/**
	 * Constructor for EncryptedOutputStreamData
	 * 
	 * @param outputStream
	 *            The encrypted OutputStream writing to the server location
	 * @param location
	 *            The location on the server
	 * @param key
	 *            The key used in the encryption
	 */
	public EncryptedOutputStreamData(OutputStream outputStream, String location, Key key) {
		super(outputStream, location);
		this.key = key;
	}

	public Key getKey() {
		return key;
	}

}
