package org.fides.client.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

/**
 * Utils for client
 * 
 * @author jesse
 * 
 */
public class HashUtils {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(HashUtils.class);

	/**
	 * hash function to hash password
	 */
	private static final String HASH = "SHA-256";

	/**
	 * Hash data
	 * 
	 * @param data
	 *            to hash
	 * @return hash of data
	 */
	public static String hash(String data) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance(HASH);
			messageDigest.update(data.getBytes());
			return Base64.toBase64String(messageDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			log.error(e);
		}
		return null;

	}

}
