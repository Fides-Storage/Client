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
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.CamelliaEngine;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.io.CipherOutputStream;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.fides.client.connector.EncryptedOutputStreamData;
import org.fides.client.connector.OutputStreamData;
import org.fides.client.connector.ServerConnector;
import org.fides.client.files.InvalidClientFileException;
import org.fides.client.files.data.ClientFile;
import org.fides.client.files.data.KeyFile;
import org.fides.client.ui.ErrorMessageScreen;

/**
 * The {@link EncryptionManager} handles the encryption and decryption of an {@link InputStream} before it is passed on
 * to the {@link ServerConnector}. It expects a fully connected {@link ServerConnector}.
 * 
 * @author Koen
 * @author Thijs
 *
 */
public class EncryptionManager {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(EncryptionManager.class);

	/**
	 * The algorithm used for encryption and decryption, when changing it dont forgot to update the
	 * {@link EncryptionManager#createCipher()}
	 */
	private static final String ALGORITHM = "Camellia";

	/** The algorithm used for encryption and decryption */
	private static final int KEY_SIZE = 32; // 256 bit

	/** The IV used to initiate the cipher */
	private static final byte[] IV = { 0x46, 0x69, 0x64, 0x65, 0x73, 0x2, 0x69, 0x73, 0x20, 0x53, 0x65, 0x63, 0x75, 0x72, 0x65, 0x21 };

	/** Size of the salt used in generating the master key, it should NEVER change */
	private static final int SALT_SIZE = 16; // 128 bit

	private final ServerConnector connector;

	private final String password;

	/**
	 * Constructor
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
			log.error("Server connector does not give an InputStream");
			return null;
		}

		DataInputStream din = null;
		ObjectInputStream inDecrypted = null;
		try {
			din = new DataInputStream(in);

			byte[] saltBytes = new byte[SALT_SIZE];
			int pbkdf2Rounds = din.readInt();
			din.read(saltBytes, 0, SALT_SIZE);

			Key key = KeyGenerator.generateKey(password, saltBytes, pbkdf2Rounds, KEY_SIZE);

			inDecrypted = new ObjectInputStream(getDecryptionStream(din, key));
			KeyFile keyFile;
			keyFile = (KeyFile) inDecrypted.readObject();
			return keyFile;
		} catch (IOException | ClassNotFoundException e) {
			// TODO: At this point we are not sure what to do here, discuss this
			log.error(e);
			ErrorMessageScreen.showErrorMessage("The keyfile can not be retrieved.", "THe program is unable to continue.", e.getMessage());
			System.exit(1); // We can't continue
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
	public boolean uploadKeyFile(final KeyFile keyFile) {
		if (keyFile == null) {
			throw new NullPointerException("No KeyFile");
		}

		OutputStream out = connector.uploadKeyFile();
		if (out == null) {
			log.error("ServerConnector does not profide an OutputStream");
		} else {
			DataOutputStream dout = new DataOutputStream(out);
			OutputStream outEncrypted = null;
			try {

				byte[] saltBytes = KeyGenerator.getSalt(SALT_SIZE);
				int pbkdf2Rounds = KeyGenerator.getRounds();

				Key key = KeyGenerator.generateKey(password, saltBytes, pbkdf2Rounds, KEY_SIZE);

				dout.writeInt(pbkdf2Rounds);
				dout.write(saltBytes, 0, SALT_SIZE);

				outEncrypted = getEncryptionStream(dout, key);
				ObjectOutputStream objectOut = new ObjectOutputStream(outEncrypted);
				objectOut.writeObject(keyFile);
				outEncrypted.flush();
				dout.flush();
				out.flush();
				return true;
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
		return getDecryptionStream(in, key);
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
			key = KeyGenerator.generateRandomKey(ALGORITHM, KEY_SIZE);
		} catch (NoSuchAlgorithmException e) {
			// Should not happen
			ErrorMessageScreen.showErrorMessage(e.getMessage());
			log.error(e);
			System.exit(1);
		} catch (InvalidKeySpecException e) {
			// Should not happen, we close if it does
			ErrorMessageScreen.showErrorMessage(e.getMessage());
			log.error(e);
			System.exit(1);
		}
		OutputStreamData outStreamData = connector.uploadFile();
		if (outStreamData == null || outStreamData.getOutputStream() == null || StringUtils.isBlank(outStreamData.getLocation())) {
			throw new NullPointerException();
		}

		OutputStream encryptOut = getEncryptionStream(outStreamData.getOutputStream(), key);
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
		OutputStream encryptedOut = getEncryptionStream(out, clientFile.getKey());

		return encryptedOut;
	}

	public ServerConnector getConnector() {
		return connector;
	}

	/**
	 * Create a n {@link InputStream} that decrypts and encrypted {@link InputStream}
	 * 
	 * @param in
	 *            The stream to decrypt
	 * @param key
	 *            The {@link Key} to use
	 * @return An decrypting {@link InputStream}
	 */
	private InputStream getDecryptionStream(InputStream in, Key key) {
		BufferedBlockCipher cipher = createCipher();

		KeyParameter keyParam = new KeyParameter(key.getEncoded());
		CipherParameters params = new ParametersWithIV(keyParam, IV);
		cipher.reset();
		// false because are decrypting
		cipher.init(false, params);

		return new CipherInputStream(in, cipher);
	}

	/**
	 * Create a n {@link OutputStream} that encrypts and encrypted {@link OutputStream}
	 * 
	 * @param in
	 *            The stream to encrypt
	 * @param key
	 *            The {@link Key} to use
	 * @return An encrypting {@link OutputStream}
	 */
	private OutputStream getEncryptionStream(OutputStream out, Key key) {
		BufferedBlockCipher cipher = createCipher();

		KeyParameter keyParam = new KeyParameter(key.getEncoded());
		CipherParameters params = new ParametersWithIV(keyParam, IV);
		cipher.reset();
		// true because are encrypting
		cipher.init(true, params);

		return new CipherOutputStream(out, cipher);
	}

	/** Create the cipher to use */
	private BufferedBlockCipher createCipher() {
		return new PaddedBufferedBlockCipher(new CBCBlockCipher(new CamelliaEngine()), new PKCS7Padding());
	}

}
