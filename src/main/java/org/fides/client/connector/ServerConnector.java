package org.fides.client.connector;

import java.io.InputStream;

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

	public byte[] requestKeyFile() {
		return null;
	}

	public void uploadKeyFile(byte[] keyFileBytes) {

	}

	/**
	 * Returns a stream of the encrypted requested file
	 */
	public InputStream requestFile(String location) {
		return null;
	}

	public String uploadFile(InputStream instream) {
		return null;
	}

	public boolean updateFile(InputStream instream, String location) {
		return false;
	}

	public boolean removeFile(String location) {
		return false;
	}

}
