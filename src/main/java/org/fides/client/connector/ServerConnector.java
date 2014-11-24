package org.fides.client.connector;

import javax.net.ssl.SSLSocket;
import java.io.InputStream;
import java.io.OutputStream;

public class ServerConnector {

	SSLSocket sslSocket;

	public ServerConnector() {
	}

	public boolean connect() {
		return false;
	}

	public boolean isConnected() {
		return false;
	}

	public boolean login(String username, String passwordHash) {
		return false;
	}

	public boolean isLoggedIn() {
		return false;
	}

	public boolean register(String username, String passwordHash) {
		return false;
	}

	public boolean disconnect() {
		return false;
	}

	public boolean isDisconnected() {
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

	public OutputStreamData uploadFile() {
		return null;
	}

	public OutputStream updateFile(String location) {
		return null;
	}

	public boolean removeFile(String location) {
		return false;
	}

}
