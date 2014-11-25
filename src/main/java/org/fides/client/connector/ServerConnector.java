package org.fides.client.connector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * TODO: Tom, description of this class
 * 
 * @author Jesse
 * @author Niels
 * @author Tom
 *
 */
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
    // For testing purposes only
    Properties systemProps = System.getProperties();
    systemProps.put("javax.net.ssl.trustStore", "./truststore.ts");
    systemProps.put("javax.net.ssl.trustStorePassword", "");
    System.setProperties(systemProps);
  }

  /**
   * Connect to the server with the given ip and port
   *
   * @param ip
   *          The server IP
   * @param port
   *          The port
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
   * 
   * @return true if connected
   */
  public boolean isConnected() {
    if (sslsocket != null) {
      return sslsocket.isConnected();
    }
    return false;
  }

  /**
   * Login user with given username and passwordHash
   * 
   * @param username
   *          name of the user
   * @param passwordHash
   *          to login
   * @return true if succeeded
   */
  public boolean login(String username, String passwordHash) {
    if (isConnected()) {
      try {
        OutputStream outToServer = sslsocket.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);

        InputStream inToServer = sslsocket.getInputStream();
        DataInputStream in = new DataInputStream(inToServer);

        JsonObject user = new JsonObject();
        user.addProperty("action", "login");
        user.addProperty("username", username);
        user.addProperty("passwordHash", passwordHash);

        out.writeUTF(new Gson().toJson(user));

        JsonObject userAnswer = new Gson().fromJson(in.readUTF(), JsonObject.class);

        if (userAnswer.has("succesfull")) {
          return userAnswer.get("succesfull").getAsBoolean();
        } else {
          return false;
        }

      } catch (IOException e) {
        System.err.println("IOException connection failed: " + e);
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
   *          the given username
   * @param passwordHash
   *          of the account
   * @return if registered succeeded
   */
  public boolean register(String username, String passwordHash) {
    System.out.println("reg: conencted:" + isConnected());
    if (isConnected()) {
      try {
        OutputStream outToServer = sslsocket.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);

        InputStream inToServer = sslsocket.getInputStream();
        DataInputStream in = new DataInputStream(inToServer);

        JsonObject user = new JsonObject();
        user.addProperty("action", "createUser");
        user.addProperty("username", username);
        user.addProperty("passwordHash", passwordHash);

        out.writeUTF(new Gson().toJson(user));

        JsonObject userAnswer = new Gson().fromJson(in.readUTF(), JsonObject.class);

        if (userAnswer.has("succesfull")) {
          return userAnswer.get("succesfull").getAsBoolean();
        } else {
          return false;
        }

      } catch (IOException e) {
        System.err.println("IOException connection failed: " + e);
      }
    }

    return false;
  }

  /**
   * Disconnect the current connection
   * 
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
   * 
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
