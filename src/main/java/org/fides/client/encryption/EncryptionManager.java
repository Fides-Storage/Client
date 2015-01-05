package org.fides.client.encryption;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.connector.EncryptedOutputStreamData;
import org.fides.client.connector.OutputStreamData;
import org.fides.client.connector.ServerConnector;
import org.fides.client.files.InvalidClientFileException;
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.KeyFile;
import org.fides.encryption.EncryptionUtils;
import org.fides.encryption.KeyGenerator;

/**
 * The {@link EncryptionManager} handles the encryption and decryption of an {@link InputStream} before it is passed on
 * to the {@link ServerConnector}. It expects a fully connected {@link ServerConnector}.
 * 
 */
public class EncryptionManager {
	/** Size of the salt used in generating the master key, it should NEVER change */
	public static final int SALT_SIZE = 16; // 128 bit

	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(EncryptionManager.class);

	private final ServerConnector connector;

	private final String password;

	/**
	 * Constructor for EncryptionManager. Adds an encryption library to ensure these encryptions are supported.
	 * 
	 * @param connector
	 *            The {@link ServerConnector} to use
	 * @param password
	 *            The user's password creating the master key for encryption of the {@link KeyFile}
	 */
	public EncryptionManager(ServerConnector connector, String password) {
		if (connector == null || StringUtils.isBlank(password)) {
			throw new NullPointerException();
		}
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		this.connector = connector;
		this.password = password;
	}

	/**
	 * Requests the {@link KeyFile} from the {@link ServerConnector} and decrypts it
	 * 
	 * @return The decrypted {@link KeyFile}
	 * @throws IOException
	 */
	public KeyFile requestKeyFile() {
		InputStream in = connector.requestKeyFile();
		if (in == null) {
			log.error("Server connector does not give an InputStream for a keyfile");
			return null;
		}

		DataInputStream din = null;
		ObjectInputStream inDecrypted = null;
		try {
			din = new DataInputStream(in);

			byte[] saltBytes = new byte[SALT_SIZE];
			int pbkdf2Rounds = din.readInt();
			din.read(saltBytes, 0, SALT_SIZE);

			Key key = KeyGenerator.generateKey(password, saltBytes, pbkdf2Rounds, EncryptionUtils.KEY_SIZE);

			inDecrypted = new ObjectInputStream(EncryptionUtils.getDecryptionStream(din, key));
			KeyFile keyFile;
			keyFile = (KeyFile) inDecrypted.readObject();
			return keyFile;
		} catch (IOException | ClassNotFoundException e) {
			log.error(e);
			return null;
		} finally {
			IOUtils.closeQuietly(inDecrypted);
			IOUtils.closeQuietly(din);
			IOUtils.closeQuietly(in);
		}
	}

	/**
	 * Encrypts the {@link KeyFile} and sends it to the {@link ServerConnector}
	 * 
	 * @param keyFile
	 *            The {@link KeyFile} to encrypt and send
	 * @throws IOException
	 */
	public boolean updateKeyFile(final KeyFile keyFile) {
		if (keyFile == null) {
			throw new NullPointerException("No KeyFile");
		}

		OutputStream out = connector.updateKeyFile();
		if (out == null) {
			log.error("ServerConnector does not profide an OutputStream for updating keyfile");
		} else {
			DataOutputStream dout = new DataOutputStream(out);
			OutputStream outEncrypted = null;
			try {

				byte[] saltBytes = KeyGenerator.getSalt(SALT_SIZE);
				int pbkdf2Rounds = KeyGenerator.getRounds();

				Key key = KeyGenerator.generateKey(password, saltBytes, pbkdf2Rounds, EncryptionUtils.KEY_SIZE);

				dout.writeInt(pbkdf2Rounds);
				dout.write(saltBytes, 0, SALT_SIZE);

				outEncrypted = EncryptionUtils.getEncryptionStream(dout, key);
				ObjectOutputStream objectOut = new ObjectOutputStream(outEncrypted);
				objectOut.writeObject(keyFile);
				outEncrypted.flush();
				dout.flush();
				out.flush();
				outEncrypted.close();
				return connector.checkUploadSuccessful();
			} catch (IOException e) {
				log.error(e);
			} finally {
				IOUtils.closeQuietly(outEncrypted);
				IOUtils.closeQuietly(dout);
				IOUtils.closeQuietly(out);
			}
		}
		return false;
	}

	/**
	 * Decrypts an {@link InputStream} from the {@link ServerConnector} of a requested file
	 * 
	 * @param clientFile
	 *            The {@link ClientFile} containing the location of the file on the server and the key to decrypt it
	 * @return An {@link InputStream} of the file
	 */
	public InputStream requestFile(ClientFile clientFile) throws InvalidClientFileException {
		if (clientFile == null) {
			throw new NullPointerException();
		}
		if (clientFile.getKey() == null || StringUtils.isBlank(clientFile.getLocation())) {
			throw new InvalidClientFileException();
		}

		InputStream in = connector.requestFile(clientFile.getLocation());

		Key key = clientFile.getKey();
		return EncryptionUtils.getDecryptionStream(in, key);
	}

	/**
	 * Encrypts a file and sends it to the {@link ServerConnector}
	 * 
	 * @return a pair of a location and an {@link OutputStream} that writes to the location the server
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	public EncryptedOutputStreamData uploadFile() {
		Key key = null;
		try {
			key = KeyGenerator.generateRandomKey(EncryptionUtils.ALGORITHM, EncryptionUtils.KEY_SIZE);
		} catch (NoSuchAlgorithmException e) {
			// Should not happen
			log.error(e);
			return null;
		} catch (InvalidKeySpecException e) {
			// Should not happen, we close if it does
			log.error(e);
			return null;
		}
		OutputStreamData outStreamData = connector.uploadFile();
		if (outStreamData == null || outStreamData.getOutputStream() == null || StringUtils.isBlank(outStreamData.getLocation())) {
			throw new NullPointerException();
		}

		OutputStream encryptOut = EncryptionUtils.getEncryptionStream(outStreamData.getOutputStream(), key);
		return new EncryptedOutputStreamData(encryptOut, outStreamData.getLocation(), key);
	}

	/**
	 * Encrypts a updated file and sends it to the {@link ServerConnector} so the server can update it
	 * 
	 * @param clientFile
	 *            The {@link ClientFile} containing the location of the file on the server and the key to encrypt it
	 * @return The {@link OutputStream} used for writing
	 * @throws InvalidClientFileException
	 */
	public OutputStream updateFile(ClientFile clientFile) throws InvalidClientFileException {
		if (clientFile == null) {
			throw new NullPointerException();
		}
		if (clientFile.getKey() == null || StringUtils.isBlank(clientFile.getLocation())) {
			throw new InvalidClientFileException();
		}

		OutputStream out = connector.updateFile(clientFile.getLocation());
		if (out == null) {
			throw new NullPointerException();
		}
		OutputStream encryptedOut = EncryptionUtils.getEncryptionStream(out, clientFile.getKey());

		return encryptedOut;
	}

	/**
	 * Removes the given ClientFile on the server
	 * 
	 * @param clientFile
	 *            The {@link ClientFile} to be removed
	 * @return Whether the file has been removed or not
	 * @throws InvalidClientFileException
	 */
	public boolean removeFile(ClientFile clientFile) throws InvalidClientFileException {
		if (clientFile == null) {
			return false;
		}
		if (clientFile.getKey() == null || StringUtils.isBlank(clientFile.getLocation())) {
			throw new InvalidClientFileException();
		}

		return connector.removeFile(clientFile.getLocation());
	}

	public ServerConnector getConnector() {
		return connector;
	}
}
