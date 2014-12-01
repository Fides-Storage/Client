package org.fides.client.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.fides.client.encryption.KeyGenerator;

/**
 * //TODO: javadoc
 * @author Koen
 *
 */
public final class FileUtil {

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
				//TODO: Bufferoverflow?
				while (dis.read() != -1) {
					// Do nothing
				}
				//TODO: don't close DigestInputStream when using "Try with resources"
				dis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			fileHash = KeyGenerator.toHex(messageDigest.digest());
		}

		return fileHash;
	}

	/**
	 * Create a {@link MessageDigest} for the use of creating hashes for files
	 * 
	 * @return
	 * //TODO: javadoc
	 */
	public static MessageDigest createFileDigest() {
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// Should never happen
			e.printStackTrace();
			//TODO: Log4j?
		}
		return messageDigest;
	}

}
