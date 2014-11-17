package org.fides.client.algorithm;

import java.math.BigInteger;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The KeyGenerator is a helper class for generating hashes from passwords
 * These hashes will be generated with PBKDF2
 */
public class KeyGenerator {

	private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";

	private static int hashByteSize;

	private static int pbkdf2Iterations = 1000;

	private	static SecureRandom random = new SecureRandom();

	/**
	 * Constructor of the KeyGenerator class
	 *
	 * @param keysize is the size of the hash in bytes.
	 */
	public KeyGenerator(int keysize) {
		hashByteSize = keysize;
	}

	/**
	 * Returns a random generated salt
	 *
	 * @param saltByteSize is the size of the salt
	 * @return a salt as byte array
	 */
	public static byte[] getSalt(int saltByteSize) {
		// Generate a random salt
		byte[] salt = new byte[saltByteSize];
		random.nextBytes(salt);
		return salt;
	}

	/**
	 * Returns the default number of rounds used by PBKDF2
	 *
	 * @return the default number of rounds
	 */
	public static int getRounds() {
		return pbkdf2Iterations;
	}

	/**
	 * Returns a Key which was generated by the given passwordt, salt en rounds
	 *
	 * @param password the password to hash
	 * @param rounds   the amount of rounds pfkdf2 should use
	 * @param salt     the salt for pbkdf2
	 * @return a Key which was generated with pbkdf2
	 */
	public Key generateKey(String password, byte[] salt, int rounds) throws NoSuchAlgorithmException, InvalidKeySpecException {
		pbkdf2Iterations = rounds;
		return pbkdf2(password.toCharArray(), salt, pbkdf2Iterations, hashByteSize);
	}

	/**
	 *
	 * @return a random generated Key
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public Key generateRandomKey(String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException {
		javax.crypto.KeyGenerator generator = javax.crypto.KeyGenerator.getInstance(algorithm);
		generator.init(hashByteSize * 8);
		return generator.generateKey();
	}

	/**
	 * Computes the PBKDF2 hash of a password.
	 *
	 * @param password   the password to hash.
	 * @param salt       the salt
	 * @param iterations the iteration count (slowness factor)
	 * @param bytes      the length of the hash to compute in bytes
	 * @return the PBDKF2 hash of the password
	 */
	private static Key pbkdf2(char[] password, byte[] salt, int iterations, int bytes)
		throws NoSuchAlgorithmException, InvalidKeySpecException {
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
		return skf.generateSecret(spec);
	}

	/**
	 * Converts a byte array into a hexadecimal string.
	 *
	 * @param array the byte array to convert
	 * @return a length*2 character string encoding the byte array
	 */
	public static String toHex(byte[] array) {
		BigInteger bi = new BigInteger(1, array);
		String hex = bi.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if (paddingLength > 0) {
			return String.format("%0" + paddingLength + "d", 0) + hex;
		}
		else {
			return hex;
		}
	}

	/**
	 * Converts a string of hexadecimal characters into a byte array.
	 *
	 * @param hex the hex string
	 * @return the hex string decoded into a byte array
	 */
	public static byte[] fromHex(String hex) {
		byte[] binary = new byte[hex.length() / 2];
		for (int i = 0; i < binary.length; i++) {
			binary[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return binary;
	}
}
