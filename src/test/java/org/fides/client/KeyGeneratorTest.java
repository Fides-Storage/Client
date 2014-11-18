package org.fides.client;

import static org.junit.Assert.assertEquals;

import org.fides.client.algorithm.KeyGenerator;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Unit test for simple App.
 */
public class KeyGeneratorTest {

	private final int keySize = 32;

	/**
	 * Test the generation of a Key based on a password
	 */
	@Test
	public void generateHashFromPassword() {
		final String password = "P@S$w0Rd";
		final String salt = "86fd5b82ba4863649c9af91fca43c53b";
		final String correctHash = "cfe2eda0e288b7fb60431d4eaaceee0554caeab591e6c1adecd2b82c5660fd92";
		final int rounds = 1000;

		KeyGenerator keyGenerator = new KeyGenerator(keySize);
		byte[] generatedHash = new byte[0];
		try {
			generatedHash = keyGenerator.generateKey(password, KeyGenerator.fromHex(salt), rounds).getEncoded();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		assertEquals(correctHash, KeyGenerator.toHex(generatedHash));
		assertEquals(keySize, generatedHash.length);
	}

	/**
	 * Test the generation of a salt
	 */
	@Test
	public void generateSalt() {
		final int saltSize = 16;

		byte[] generatedSalt = KeyGenerator.getSalt(saltSize);
		assertEquals(saltSize, generatedSalt.length);
	}

	/**
	 * Test the generation of a randomly generated Key
	 */
	@Test
	public void generateRandomKey() {

		KeyGenerator keyGenerator = new KeyGenerator(keySize);
		byte[] generatedHash = new byte[0];
		try {
			generatedHash = keyGenerator.generateRandomKey("AES").getEncoded();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}

		assertEquals(keySize, generatedHash.length);
	}

}
