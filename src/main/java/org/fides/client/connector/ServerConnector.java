package org.fides.client.connector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.util.Properties;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * This class makes it possible to connect to a server and communicate with it
 * 
 * @author Jesse
 * @author Niels
 * @author Tom
 */
public class ServerConnector {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(ServerConnector.class);

	/**
	 * The SSLSocket that will be used
	 */
	private SSLSocket sslsocket;

	/**
	 * The retreived server certificates
	 */
	private Certificate[] serverCertificates;

	/**
	 * The data ouput stream to the server
	 */
	private DataOutputStream out;

	/**
	 * The data input stream from the server
	 */
	private DataInputStream in;

	/**
	 * The constructor for the ServerConnector
	 */
	public ServerConnector() {
		// For testing purposes only
		Properties systemProps = System.getProperties();
		systemProps.put("javax.net.ssl.trustStore", "./truststore.ts");
		systemProps.put("javax.net.ssl.trustStorePassword", "");
		System.setProperties(systemProps);
	}

	/**
	 * Connect to the server with the given ip and port
	 * 
	 * @param host
	 *            The server IP
	 * @param port
	 *            The port
	 * @return true if the connection was successful
	 * @throws UnknownHostException
	 */
	public boolean connect(String host, int port) throws UnknownHostException {
		try {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			sslsocket = (SSLSocket) sslsocketfactory.createSocket(host, port);
			// TODO Check if connection is correctly closed without a timeout

			SSLSession session = sslsocket.getSession();
			serverCertificates = session.getPeerCertificates();

			out = new DataOutputStream(sslsocket.getOutputStream());
			in = new DataInputStream(sslsocket.getInputStream());

			return true;
		} catch (IOException e) {
			log.error(e);
		}
		return false;
	}

	/**
	 * Returns if the connection is alive
	 * 
	 * @return true if connected
	 */
	public boolean isConnected() {
		return sslsocket != null && sslsocket.isConnected();
	}

	/**
	 * Login user with given username and passwordHash
	 * 
	 * @param username
	 *            name of the user
	 * @param passwordHash
	 *            to login
	 * @return true if succeeded
	 */
	public boolean login(String username, String passwordHash) {
		if (isConnected()) {
			try {
				JsonObject user = new JsonObject();
				user.addProperty("action", "login");
				user.addProperty("username", username);
				user.addProperty("passwordHash", passwordHash);

				out.writeUTF(new Gson().toJson(user));

				// TODO: Check server response for null
				JsonObject userAnswer = new Gson().fromJson(in.readUTF(), JsonObject.class);
				if (userAnswer.has("successful")) {
					return userAnswer.get("successful").getAsBoolean();
				} else {
					return false;
				}

			} catch (IOException e) {
				log.error("IOException connection failed: ", e);
			}
		}

		return false;
	}

	public boolean isLoggedIn() {
		return false;
	}

	/**
	 * Register the user with given username and passwordHash
	 * 
	 * @param username
	 *            the given username
	 * @param passwordHash
	 *            of the account
	 * @return if registered succeeded
	 */
	public boolean register(String username, String passwordHash) {
		if (isConnected()) {
			try {

				JsonObject user = new JsonObject();
				user.addProperty("action", "createUser");
				user.addProperty("username", username);
				user.addProperty("passwordHash", passwordHash);

				out.writeUTF(new Gson().toJson(user));

				// TODO: Check server response for null
				JsonObject userAnswer = new Gson().fromJson(in.readUTF(), JsonObject.class);
				if (userAnswer.has("successful")) {
					return userAnswer.get("successful").getAsBoolean();
				} else {
					return false;
				}

			} catch (IOException e) {
				log.error("IOException connection failed: ", e);
			}
		}

		return false;
	}

	/**
	 * Disconnect the current connection
	 * 
	 * @return true if disconnect was successful
	 */
	public boolean disconnect() {
		try {
			in.close();
			out.close();
			sslsocket.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Returns if the connection is inactive
	 * 
	 * @return true if disconnected
	 */
	public boolean isDisconnected() {
		return sslsocket != null && sslsocket.isClosed();
	}

	/**
	 * Get the server certificates
	 * 
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