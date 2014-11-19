package org.fides.client.encryption;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import org.apache.commons.lang3.StringUtils;
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
import org.fides.client.files.ClientFile;
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
	 * @throws NoSuchAlgorithmException
	 *             Thrown if the key's algorithm is incorrect
	 */
	public EncryptionManager(ServerConnector connector, String password) throws NoSuchAlgorithmException {
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
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws ClassNotFoundException
	 * @throws InvalidAlgorithmParameterException
	 */
	public KeyFile requestKeyFile() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, ClassNotFoundException {
		InputStream in = connector.requestKeyFile();
		if (in == null) {
			throw new NullPointerException();
		}

		DataInputStream din = new DataInputStream(in);

		byte[] saltBytes = new byte[SALT_SIZE];
		int pbkdf2Rounds = din.readInt();
		din.read(saltBytes, 0, SALT_SIZE);

		Key key = KeyGenerator.generateKey(password, saltBytes, pbkdf2Rounds, KEY_SIZE);

		ObjectInputStream inDecrypted = new ObjectInputStream(getDecryptionStream(din, key));
		KeyFile keyFile = (KeyFile) inDecrypted.readObject();

		return keyFile;
	}

	/**
	 * Encrypts the {@link KeyFile} and sends it to the {@link ServerConnector}
	 * 
	 * @param keyFile
	 *            The {@link KeyFile} to encrypt and send
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws IOException
	 */
	public void uploadKeyFile(final KeyFile keyFile) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
		if (keyFile == null) {
			throw new NullPointerException();
		}

		OutputStream out = connector.uploadKeyFile();
		if (out == null) {
			throw new NullPointerException();
		}
		DataOutputStream dout = new DataOutputStream(out);

		byte[] saltBytes = KeyGenerator.getSalt(SALT_SIZE);
		int pbkdf2Rounds = KeyGenerator.getRounds();

		Key key = KeyGenerator.generateKey(password, saltBytes, pbkdf2Rounds, KEY_SIZE);

		dout.writeInt(pbkdf2Rounds);
		dout.write(saltBytes, 0, SALT_SIZE);

		OutputStream outEncrypted = getEncryptionStream(dout, key);
		ObjectOutputStream objectOut = new ObjectOutputStream(outEncrypted);
		objectOut.writeObject(keyFile);
		objectOut.close();
	}

	/**
	 * Decrypts an {@link InputStream} from the {@link ServerConnector} of a requested file
	 * 
	 * @param location
	 *            The location of the file on the server
	 * @param keyFile
	 *            The keyfile containing
	 * @return An {@link InputStream} of the file
	 * @throws FileNotFoundException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 */
	public InputStream requestFile(final String location, final KeyFile keyFile) throws FileNotFoundException, InvalidKeyException, InvalidAlgorithmParameterException {
		if (location == null || keyFile == null) {
			throw new NullPointerException();
		}

		InputStream in = connector.requestFile(location);
		ClientFile clientFile = keyFile.getClientFileByLocation(location);
		if (clientFile == null) {
			throw new FileNotFoundException();
		}

		Key key = clientFile.getKey();
		return getDecryptionStream(in, key);
	}

	/**
	 * Encrypts a file and sends it to the {@link ServerConnector}
	 * 
	 * @return a pair of a location and an {@link OutputStream} that writes to the location the server
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 */
	public EncryptedOutputStreamData uploadFile() throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
		Key key = KeyGenerator.generateRandomKey(ALGORITHM, KEY_SIZE);
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
	 * @param location
	 *            The location of the existing file on the server
	 * @return The {@link OutputStream} used for writing
	 * @throws FileNotFoundException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 */
	public OutputStream updateFile(final String location, final KeyFile keyFile) throws FileNotFoundException, InvalidKeyException, InvalidAlgorithmParameterException {
		if (location == null || keyFile == null) {
			throw new NullPointerException();
		}

		ClientFile clientFile = keyFile.getClientFileByLocation(location);
		if (clientFile == null) {
			throw new FileNotFoundException();
		}

		OutputStream out = connector.updateFile(location);
		if (out == null) {
			throw new NullPointerException();
		}
		OutputStream encryptedOut = getEncryptionStream(out, clientFile.getKey());

		return encryptedOut;
	}

	private InputStream getDecryptionStream(InputStream in, Key key) throws InvalidKeyException, InvalidAlgorithmParameterException {
		BufferedBlockCipher cipher = createCipher();

		KeyParameter keyParam = new KeyParameter(key.getEncoded());
		CipherParameters params = new ParametersWithIV(keyParam, IV);
		cipher.reset();
		cipher.init(false, params);

		return new CipherInputStream(in, cipher);
	}

	private OutputStream getEncryptionStream(OutputStream out, Key key) throws InvalidKeyException, InvalidAlgorithmParameterException {
		BufferedBlockCipher cipher = createCipher();

		KeyParameter keyParam = new KeyParameter(key.getEncoded());
		CipherParameters params = new ParametersWithIV(keyParam, IV);
		cipher.reset();
		cipher.init(true, params);

		return new CipherOutputStream(out, cipher);
	}

	/** Create the cipher to use */
	private BufferedBlockCipher createCipher() {
		return new PaddedBufferedBlockCipher(new CBCBlockCipher(new CamelliaEngine()), new PKCS7Padding());
	}

}
