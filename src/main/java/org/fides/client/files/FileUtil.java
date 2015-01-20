package org.fides.client.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.tools.HashUtils;

/**
 * Some utilities which have to do with files
 * 
 */
public final class FileUtil {
	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(FileUtil.class);

	private static final String HASH_ALGORITHM = "MD5";

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

		if (file.exists()) {
			MessageDigest messageDigest = createFileDigest();
			// In order to make the hash or checksum we have to read the entire file
			try (DigestInputStream dis = new DigestInputStream(new FileInputStream(file), messageDigest)) {
				byte[] bytes = new byte[1000];
				while (dis.read(bytes) != -1) {
					// Do nothing
				}
			} catch (IOException e) {
				// Should never happen
				LOG.error(e);
			}
			fileHash = HashUtils.toHex(messageDigest.digest());
		}

		return fileHash;
	}

	/**
	 * Create a {@link MessageDigest} for the use of creating hashes for files
	 * 
	 * @return A {@link MessageDigest} for hashing files
	 */
	public static MessageDigest createFileDigest() {
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			// Should never happen
			LOG.error(e);
		}
		return messageDigest;
	}

}
