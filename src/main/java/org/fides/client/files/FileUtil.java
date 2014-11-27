package org.fides.client.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.encryption.KeyGenerator;

/**
 * 
 * @author Koen
 * 
 */
public final class FileUtil {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(FileUtil.class);

	private FileUtil() {
	}

	/**
	 * Generate a hash for a file
	 * 
	 * @param file
	 *            The {@link File} to generate the hash for
	 * @return The hash, is null when file does not exist
	 */
	public static String generateFileHash(File file) {
		String fileHash = null;

		try {
			if (file.exists()) {
				MessageDigest messageDigest = MessageDigest.getInstance("MD5");
				// In order to make the hash or checksum we have to read the entire file
				try (DigestInputStream dis = new DigestInputStream(new FileInputStream(file), messageDigest)) {
					while (dis.read() != -1) {
						// Do nothing
					}
					dis.close();
				} catch (IOException e) {
					log.error(e);

				}
				fileHash = KeyGenerator.toHex(messageDigest.digest());
			}
		} catch (NoSuchAlgorithmException e) {
			log.debug("This should not happen since we made the choice for MD5 ourselves");
		}
		return fileHash;
	}
}
