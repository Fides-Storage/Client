package org.fides.client.connector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.tools.CommunicationUtil;
import org.fides.client.tools.UserProperties;
import org.fides.components.Actions;
import org.fides.components.Responses;
import org.fides.components.virtualstream.VirtualInputStream;
import org.fides.components.virtualstream.VirtualOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * This class makes it possible to connect to a server and communicate with it
 * 
 */
public class ServerConnector {

	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(ServerConnector.class);

	/**
	 * connection timeout for initial connect
	 */
	private static final int CONNECTTIMEOUT = 10000;

	/**
	 * The collection to store the error messages received from the server
	 */
	private Map<String, String> errorMessages = new HashMap<>();

	/**
	 * The SSLSocket that will be used
	 */
	private SSLSocket sslsocket;

	/**
	 * The retrieved server certificates
	 */
	private Certificate[] serverCertificates;

	/**
	 * The data output stream to the server
	 */
	private DataOutputStream out;

	/**
	 * The data input stream from the server
	 */
	private DataInputStream in;

	/**
	 * If you are logged in
	 */
	private boolean loggedIn = false;

	/**
	 * The constructor for the ServerConnector, adds a certificate to the list of trusted certificates.
	 */
	public ServerConnector() {
		// TODO: For testing purposes only
		String dir = System.getProperty("user.dir");
		File file = new File(dir, "truststore.ts");
		try {
			if (file.exists()) {
				Properties systemProps = System.getProperties();
				systemProps.put("javax.net.ssl.trustStore", file.getCanonicalPath());
				systemProps.put("javax.net.ssl.trustStorePassword", "");
				System.setProperties(systemProps);
			}
		} catch (IOException e) {
			// Do nothing when file.getCanonicalPath doesn't work.
		}
	}

	/**
	 * Connect to the server with the given ip and port
	 * 
	 * @param address
	 *            The {@link InetSocketAddress} with the server's address
	 * @return true if the connection was successful
	 */
	public boolean init(InetSocketAddress address) throws UnknownHostException, ConnectException {
		try {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

			sslsocket = (SSLSocket) sslsocketfactory.createSocket();
			sslsocket.connect(address, CONNECTTIMEOUT);

			SSLSession session = sslsocket.getSession();
			serverCertificates = session.getPeerCertificates();

			out = new DataOutputStream(sslsocket.getOutputStream());
			in = new DataInputStream(sslsocket.getInputStream());

			return true;
		} catch (ConnectException | UnknownHostException e) {
			throw e;
		} catch (IOException e) {
			throw new ConnectException(e.getLocalizedMessage());
		}
	}

	/**
	 * Connects to the server which is set in the {@link ServerConnector#init(InetSocketAddress)} function.
	 * 
	 * @throws UnknownHostException
	 * @throws ConnectException
	 */
	public void connect() throws UnknownHostException, ConnectException {
		try {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

			sslsocket = (SSLSocket) sslsocketfactory.createSocket();
			sslsocket.connect(UserProperties.getInstance().getServerAddress());

			SSLSession session = sslsocket.getSession();
			serverCertificates = session.getPeerCertificates();

			out = new DataOutputStream(sslsocket.getOutputStream());
			in = new DataInputStream(sslsocket.getInputStream());
		} catch (ConnectException | UnknownHostException e) {
			throw e;
		} catch (IOException e) {
			throw new ConnectException(e.getLocalizedMessage());
		}
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
	 * Login user with given usernameHash and passwordHash
	 * 
	 * @param usernameHash
	 *            name of the user
	 * @param passwordHash
	 *            to login
	 * @return true if succeeded
	 */
	public boolean login(String usernameHash, String passwordHash) {
		if (isConnected() && !loggedIn) {
			try {
				Map<String, Object> properties = new HashMap<>();
				properties.put(Actions.Properties.USERNAME_HASH, usernameHash);
				properties.put(Actions.Properties.PASSWORD_HASH, passwordHash);
				CommunicationUtil.requestActionWithProperties(out, Actions.LOGIN, properties);

				JsonObject userAnswer = new Gson().fromJson(in.readUTF(), JsonObject.class);
				if (userAnswer.has(Responses.SUCCESSFUL)) {
					if (userAnswer.has(Responses.ERROR)) {
						errorMessages.put(Actions.LOGIN, userAnswer.get(Responses.ERROR).getAsString());
					}
					loggedIn = userAnswer.get(Responses.SUCCESSFUL).getAsBoolean();
				} else {
					loggedIn = false;
				}

			} catch (IOException e) {
				LOG.error("IOException connection failed: ", e);
				loggedIn = false;
			}
		}
		return loggedIn;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	/**
	 * Register the user with given usernameHash and passwordHash
	 * 
	 * @param usernameHash
	 *            the given usernameHash
	 * @param passwordHash
	 *            of the account
	 * @return if registered succeeded
	 */
	public boolean register(String usernameHash, String passwordHash) {
		if (isConnected() && !isLoggedIn()) {
			try {
				Map<String, Object> properties = new HashMap<>();
				properties.put(Actions.Properties.USERNAME_HASH, usernameHash);
				properties.put(Actions.Properties.PASSWORD_HASH, passwordHash);
				CommunicationUtil.requestActionWithProperties(out, Actions.CREATE_USER, properties);

				JsonObject userAnswer = new Gson().fromJson(in.readUTF(), JsonObject.class);
				if (userAnswer.has(Responses.SUCCESSFUL)) {
					if (userAnswer.has(Responses.ERROR)) {
						errorMessages.put(Actions.CREATE_USER, userAnswer.get(Responses.ERROR).getAsString());
					}
					return userAnswer.get(Responses.SUCCESSFUL).getAsBoolean();
				} else {
					return false;
				}

			} catch (IOException e) {
				LOG.error("IOException connection failed: ", e);
			}
		}
		return false;
	}

	/**
	 * Getter to get the error message received from the server
	 * 
	 * @param key
	 *            of the map to get the corresponding value
	 * @return the value
	 */
	public String getErrorMessage(String key) {
		return errorMessages.get(key);
	}

	/**
	 * Disconnect the current connection
	 */
	public void disconnect() {
		try {
			CommunicationUtil.requestAction(out, Actions.DISCONNECT);
			out.flush();
		} catch (IOException e) {
			LOG.error(e);
		} finally {
			loggedIn = false;
			errorMessages = new HashMap<>();
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(sslsocket);
		}
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

	/**
	 * Check if the user already has a keyFile
	 * 
	 * @return if keyFile exists
	 */
	public boolean checkIfKeyFileExists() {
		InputStream keyFileStream = requestKeyFile();

		if (keyFileStream != null) {
			try {
				if (keyFileStream.read() == -1) {
					return false;
				} else {
					LOG.debug("A KeyFile is available on the server");
					return true;
				}
			} catch (IOException e) {
				LOG.error(e);
			} finally {
				IOUtils.closeQuietly(keyFileStream);
			}
		}

		return false;
	}

	/**
	 * Requests a KeyFile from the server
	 * 
	 * @return An InputStream with the KeyFile. If something went wrong, this will be <code>null</code>
	 */
	public InputStream requestKeyFile() {
		try {
			UserProperties userProperties = UserProperties.getInstance();
			if (login(userProperties.getUsernameHash(), userProperties.getPasswordHash())) {
				CommunicationUtil.requestAction(out, Actions.GET_KEY_FILE);

				JsonObject requestResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
				if (requestResponse.has(Responses.SUCCESSFUL)) {
					if (requestResponse.get(Responses.SUCCESSFUL).getAsBoolean()) {
						return new VirtualInputStream(in);
					} else {
						errorMessages.put(Actions.GET_KEY_FILE, requestResponse.get(Responses.ERROR).getAsString());
					}
				}
			} else {
				LOG.error("ServerConnector couldn't LOG in");
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return null;

	}

	/**
	 * Requests a stream from the server for updating the KeyFile
	 * 
	 * @return An OutputStream to write the KeyFile to. If something went wrong, this will be <code>null</code>
	 */
	public OutputStream updateKeyFile() {
		try {
			UserProperties userProperties = UserProperties.getInstance();
			if (login(userProperties.getUsernameHash(), userProperties.getPasswordHash())) {
				CommunicationUtil.requestAction(out, Actions.UPDATE_KEY_FILE);

				JsonObject requestResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
				if (requestResponse.has(Responses.SUCCESSFUL)) {
					if (requestResponse.get(Responses.SUCCESSFUL).getAsBoolean()) {
						return new VirtualOutputStream(out);
					} else {
						errorMessages.put(Actions.UPDATE_KEY_FILE, requestResponse.get(Responses.ERROR).getAsString());
					}
				}
			} else {
				LOG.error("ServerConnector couldn't LOG in");
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return null;
	}

	/**
	 * Returns a stream of the encrypted requested file
	 * 
	 * @param location
	 *            The location of the requested file
	 * @return An InputStream with the content of the requested file. Returns <code>null</code> if the request failed.
	 */
	public InputStream requestFile(String location) {
		try {
			UserProperties userProperties = UserProperties.getInstance();
			if (login(userProperties.getUsernameHash(), userProperties.getPasswordHash())) {
				Map<String, Object> properties = new HashMap<>();
				properties.put(Actions.Properties.LOCATION, location);
				CommunicationUtil.requestActionWithProperties(out, Actions.GET_FILE, properties);

				JsonObject requestResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
				if (requestResponse.has(Responses.SUCCESSFUL)) {
					if (requestResponse.get(Responses.SUCCESSFUL).getAsBoolean()) {
						return new VirtualInputStream(in);
					} else {
						errorMessages.put(Actions.GET_FILE, requestResponse.get(Responses.ERROR).getAsString());
					}
				}
			} else {
				LOG.error("ServerConnector couldn't LOG in");
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return null;
	}

	/**
	 * Requests a stream from the server for uploading a file
	 * 
	 * @return An OutputStreamData containing a stream to write the file to and the location of the new file on the
	 *         server. If something went wrong, this will be <code>null</code>
	 */
	public OutputStreamData uploadFile() {
		try {
			UserProperties userProperties = UserProperties.getInstance();
			if (login(userProperties.getUsernameHash(), userProperties.getPasswordHash())) {
				CommunicationUtil.requestAction(out, Actions.UPLOAD_FILE);

				JsonObject requestResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
				if (requestResponse.has(Responses.SUCCESSFUL)) {
					if (requestResponse.get(Responses.SUCCESSFUL).getAsBoolean() && requestResponse.has(Actions.Properties.LOCATION)) {
						String location = requestResponse.get(Actions.Properties.LOCATION).getAsString();
						return new OutputStreamData(new VirtualOutputStream(out), location);
					} else {
						errorMessages.put(Actions.UPLOAD_FILE, requestResponse.get(Responses.ERROR).getAsString());
					}
				}
			} else {
				LOG.error("ServerConnector couldn't LOG in");
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return null;
	}

	/**
	 * Requests a stream from the server for updating a file
	 * 
	 * @param location
	 *            The location of the file you want to update
	 * @return An OutputStream to write the file to. If something went wrong, this will be <code>null</code>
	 */
	public OutputStream updateFile(String location) {
		try {
			UserProperties userProperties = UserProperties.getInstance();
			if (login(userProperties.getUsernameHash(), userProperties.getPasswordHash())) {
				Map<String, Object> properties = new HashMap<>();
				properties.put(Actions.Properties.LOCATION, location);
				CommunicationUtil.requestActionWithProperties(out, Actions.UPDATE_FILE, properties);

				JsonObject requestResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
				if (requestResponse.has(Responses.SUCCESSFUL)) {
					if (requestResponse.get(Responses.SUCCESSFUL).getAsBoolean()) {
						return new VirtualOutputStream(out);
					} else {
						errorMessages.put(Actions.UPDATE_FILE, requestResponse.get(Responses.ERROR).getAsString());
					}
				}
			} else {
				LOG.error("ServerConnector couldn't LOG in");
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return null;
	}

	/**
	 * Requests a stream from the server for removing a file
	 * 
	 * @param location
	 *            the location of the file to remove
	 * @return true if the file is successfully removed, false otherwise
	 */
	public boolean removeFile(String location) {
		try {
			UserProperties userProperties = UserProperties.getInstance();
			if (login(userProperties.getUsernameHash(), userProperties.getPasswordHash())) {
				Map<String, Object> properties = new HashMap<>();
				properties.put(Actions.Properties.LOCATION, location);
				CommunicationUtil.requestActionWithProperties(out, Actions.REMOVE_FILE, properties);

				JsonObject requestResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
				if (requestResponse.has(Responses.SUCCESSFUL)) {
					if (requestResponse.get(Responses.SUCCESSFUL).getAsBoolean()) {
						return true;
					} else {
						errorMessages.put(Actions.REMOVE_FILE, requestResponse.get(Responses.ERROR).getAsString());
					}
				}
			} else {
				LOG.error("ServerConnector couldn't LOG in");
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return false;
	}

	/**
	 * After an upload or update, this function has to be called. This function tells the server whether the upload was
	 * successful on the client side, and if it was successful it will check if the upload was successful on the server
	 * side.
	 * 
	 * @param uploadSuccessful
	 *            true if the upload was successful on the client side
	 * 
	 * @return true if the last upload or update was successful, otherwise false
	 */
	public boolean confirmUpload(boolean uploadSuccessful) {
		try {
			String message = in.readUTF();
			JsonObject response = new Gson().fromJson(message, JsonObject.class);
			if (response.has(Responses.SUCCESSFUL)) {
				if (response.get(Responses.SUCCESSFUL).getAsBoolean()) {
					LOG.debug("Upload serverside was successful");
					JsonObject returnJsonObject = new JsonObject();
					returnJsonObject.addProperty(Responses.SUCCESSFUL, uploadSuccessful);
					out.writeUTF(new Gson().toJson(returnJsonObject));
					LOG.debug("Upload clientside was successful: " + uploadSuccessful);
					return uploadSuccessful;
				} else {
					errorMessages.put(Actions.UPLOAD_FILE, response.get(Responses.ERROR).getAsString());
				}
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		LOG.debug("The last upload was not successful");
		return false;
	}

	/**
	 * Retrieves a set from the containing the locations the user has access to. returns null when failed
	 * 
	 * @return The set with locations
	 */
	public Set<String> requestLocations() {
		try {
			UserProperties userProperties = UserProperties.getInstance();
			if (login(userProperties.getUsernameHash(), userProperties.getPasswordHash())) {

				CommunicationUtil.requestAction(out, Actions.REQUEST_LOCATIONS);

				JsonObject requestResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);

				if (requestResponse.has(Responses.SUCCESSFUL)) {
					if (requestResponse.get(Responses.SUCCESSFUL).getAsBoolean()) {
						JsonElement locationJelement = requestResponse.get(Responses.LOCATIONS);

						if (locationJelement != null) {
							Type collectionType = new TypeToken<Set<String>>() {
							}.getType();
							Set<String> locations = new Gson().fromJson(locationJelement, collectionType);
							return locations;
						}
						errorMessages.put(Actions.GET_FILE, requestResponse.get(Responses.ERROR).getAsString());
					}
				}

			} else {
				LOG.error("ServerConnector couldn't log in");
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return null;
	}
}