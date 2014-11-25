package org.fides.client.connector;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ServerConnector {

	/**
	 * The SSLSocket that will be used
	 */
	private SSLSocket sslsocket;

	/**
	 * The retreived server sertificates
	 */
	private Certificate[] serverCertificates;

	/**
	 * The constructor for het ServerConnector
	 */
	public ServerConnector() {
		//TODO: remove these lines, these are for testing purposes only
		Properties systemProps = System.getProperties();
		systemProps.put("javax.net.ssl.trustStore", "./truststore.ts");
		systemProps.put("javax.net.ssl.trustStorePassword", "");
		System.setProperties(systemProps);
	}

	/**
	 * Connect to the server with the given ip and port
	 *
	 * @param ip   The server IP
	 * @param port The port
	 * @return true if the connection was successfull
	 * @throws UnknownHostException
	 */
	public boolean connect(String ip, int port) throws UnknownHostException {
		try {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			sslsocket = (SSLSocket) sslsocketfactory.createSocket(ip, port);
			sslsocket.setSoTimeout(2000);

			SSLContext context = SSLContext.getInstance("TLS");

			SSLSession session = sslsocket.getSession();
			serverCertificates = session.getPeerCertificates();
			return true;

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Returns if the connection is alive
	 * @return true if connected
	 */
	public boolean isConnected() {
		if (sslsocket != null) {
			return sslsocket.isConnected();
		}
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

	/**
	 * Disconnect the current connection
	 * @return true if disconnect was successfull
	 */
	public boolean disconnect() {
		try {
			sslsocket.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Returns if the connection is inactive
	 * @return true if disconnected
	 */
	public boolean isDisconnected() {
		if (sslsocket != null) {
			return sslsocket.isClosed();
		}
		return false;
	}

	/**
	 * Get the server certificates
	 * @return the server certificates
	 */
	public Certificate[] getServerCertificates() {
		return serverCertificates;
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
