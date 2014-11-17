package org.fides.client.connector;

import java.io.InputStream;
import java.io.OutputStream;

public class ServerConnector {

	private String username;

	private String passwordHash;

	public ServerConnector(String username, String passwordHash) {

	}

	public boolean connect() {
		return false;
	}

	public boolean disconnect() {
		return false;
	}

	public InputStream requestKeyFile() {
		return null;
	}

	public OutputStream uploadKeyFile() {
		return null;
	}

	/**
	 * Returns a stream of the encrypted requested file
	 */
	public InputStream requestFile(String location) {
		return null;
	}

	public LocationOutputStreamPair uploadFile() {
		return null;
	}

	public OutputStream updateFile(String location) {
		return null;
	}

	public boolean removeFile(String location) {
		return false;
	}

}
