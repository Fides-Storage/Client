package org.fides.client;

import static org.junit.Assert.assertEquals;
import org.fides.client.algorithm.KeyGenerator;
import org.junit.Test;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Unit test for simple App.
 */
public class KeyGeneratorTest {

	@Test
	public void GenerateHashFromPassword() {
		final String password = "P@S$w0Rd";
		final String salt = "86fd5b82ba4863649c9af91fca43c53b";
		final String correctHash = "cfe2eda0e288b7fb60431d4eaaceee0554caeab591e6c1adecd2b82c5660fd92";
		final int rounds = 1000;
		final int keySize = 32;

		KeyGenerator keyGenerator = new KeyGenerator(keySize);
		byte[] generatedHash = new byte[0];
		try {
			generatedHash = keyGenerator.generateKey(password, KeyGenerator.fromHex(salt), rounds).getEncoded();
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		assertEquals(correctHash, KeyGenerator.toHex(generatedHash));
		assertEquals(keySize, generatedHash.length);
	}

	@Test
	public void GenerateSalt() {
		final int saltSize = 16;

		byte[] generatedSalt = KeyGenerator.getSalt(saltSize);
		assertEquals(saltSize, generatedSalt.length);
	}

}
