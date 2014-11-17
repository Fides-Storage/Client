package client.connector;

import java.io.InputStream;
import java.security.Key;

import org.fides.client.connector.ServerConnector;
import org.fides.client.files.ClientFile;
import org.fides.client.files.KeyFile;

public class EncryptionManager {

	private ServerConnector connector;

	private Key masterKey;

	public EncryptionManager(ServerConnector connector, Key masterKey) {

	}

	public KeyFile requestKeyFile() {
		return null;
	}

	public void uploadKeyFile(int keyFile) {

	}

	/**
	 * Returns a stream of the requested file
	 */
	public InputStream requestFile(String location) {
		return null;
	}

	public ClientFile uploadFile(InputStream instream) {
		return null;
	}

	public boolean updateFile(InputStream instream, String location) {
		return false;
	}

}
