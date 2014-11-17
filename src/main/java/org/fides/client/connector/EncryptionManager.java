package org.fides.client.connector;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import org.fides.client.algorithm.KeyGenerator;
import org.fides.client.files.KeyFile;

/**
 * The {@link EncryptionManager} handles the encryption and decryption of an {@link InputStream} before it is passed on
 * to the {@link ServerConnector}
 * 
 * @author Koen
 * @author Thijs
 *
 */
public class EncryptionManager {

	/** The algorithm used for encryption and decryption */
	private static final String ALGORITHM = "AES";

	/** The mode of operation and padding for the cipher */
	private static final String ALGORITHM_MODE = "/CBC/PKCS5Padding";

	/** The IV used to initiate the cipher */
	private static final byte[] IV = { 0x46, 0x69, 0x64, 0x65, 0x73, 0x2, 0x69, 0x73, 0x20, 0x53, 0x65, 0x63, 0x75, 0x72, 0x65, 0x21 };

	/** Size of the salt used in generating the master key, it should NEVER change */
	private static final int SALT_SIZE = 16;

	private ServerConnector connector;

	private String password;

	private Cipher cipher;

	private KeyGenerator keyGenerator;

	/**
	 * Constructor
	 * 
	 * @param connector
	 *            The {@link ServerConnector} to use
	 * @param password
	 *            The user's password creating the master key for encryption of the {@link KeyFile}
	 * @throws NoSuchAlgorithmException
	 *             Thrown if the key's algorithm is incorrect
	 * @throws NoSuchPaddingException
	 *             Thrown if the padding is incorrect
	 */
	public EncryptionManager(ServerConnector connector, String password) throws NoSuchAlgorithmException, NoSuchPaddingException {
		this.connector = connector;
		this.password = password;
		this.cipher = Cipher.getInstance(ALGORITHM + ALGORITHM_MODE);
		this.keyGenerator = new KeyGenerator(32);
	}

	/**
	 * Requests the {@link KeyFile} from the {@link IServerConnector} and decrypts it
	 * 
	 * @return The decrypted {@link KeyFile}
	 * @throws IOException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws ClassNotFoundException
	 * @throws InvalidAlgorithmParameterException
	 */
	public KeyFile requestKeyFile() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, ClassNotFoundException, InvalidAlgorithmParameterException {
		InputStream in = connector.requestKeyFile();
		DataInputStream din = new DataInputStream(in);

		byte[] saltBytes = new byte[SALT_SIZE];
		din.read(saltBytes);
		int pbkdf2Rounds = din.readInt();

		Key key = keyGenerator.generateKey(password, saltBytes, pbkdf2Rounds);

		ObjectInputStream inDecrypted = new ObjectInputStream(getDecryptionStream(din, key));
		KeyFile keyFile = (KeyFile) inDecrypted.readObject();

		return keyFile;
	}

	/**
	 * Encrypts the {@link KeyFile} and sends it to the {@link IServerConnector}
	 * 
	 * @param keyFile
	 *            The {@link KeyFile} to encrypt and send
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws IOException
	 */
	public void uploadKeyFile(KeyFile keyFile) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
		OutputStream out = connector.uploadKeyFile();

		Key key = keyGenerator.generateKey(password, keyGenerator.getSalt(SALT_SIZE), keyGenerator.getRounds());

		OutputStream outEncrypted = getEncryptionStream(out, key);
		ObjectOutputStream objectOut = new ObjectOutputStream(outEncrypted);
		objectOut.writeObject(keyFile);
	}

	/**
	 * Decrypts an {@link InputStream} from the {@link IServerConnector} of a requested file
	 * 
	 * @param location
	 *            The location of the file on the server
	 * @return An {@link InputStream} of the file
	 */
	public InputStream requestFile(String location) {
		return null;
	}

	/**
	 * Encrypts a file and sends it to the {@link IServerConnector}
	 * 
	 * @return a pair of a location and an {@link OutputStream} that writes to the location the server
	 */
	public LocationOutputStreamPair uploadFile() {
		return null;
	}

	/**
	 * Encrypts a updated file and sends it to the {@link IServerConnector} so the server can update it
	 * 
	 * @param location
	 *            The location of the existing file on the server
	 * @return The {@link OutputStream} used for writing
	 */
	public OutputStream updateFile(String location) {
		return null;
	}

	private InputStream getDecryptionStream(InputStream in, Key key) throws InvalidKeyException, InvalidAlgorithmParameterException {
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV));
		return new CipherInputStream(in, cipher);
	}

	private OutputStream getEncryptionStream(OutputStream out, Key key) throws InvalidKeyException, InvalidAlgorithmParameterException {
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV));
		return new CipherOutputStream(out, cipher);
	}

}
