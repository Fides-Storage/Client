package org.fides.client.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A custom tool to use for copying.
 * 
 * @author Thijs
 * 
 */
public class CopyTool {

	private static final int EOF = -1;

	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	/**
	 * Copy bytes from an {@link InputStream} to an {@link OutputStream}. This copy function works the same as the
	 * IOUtils.copy, except it stops as soon as the stopBoolean is true.
	 * 
	 * @param input
	 *            the {@link InputStream} to read from
	 * @param output
	 *            the {@link OutputStream} to write to
	 * @param stopBoolean
	 *            the boolean which notifies the copyUntil it has to stop
	 * @return the number of bytes copied, or -1 if Integer.MAX_VALUE
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws CopyInterruptedException
	 *             if the copy was interrupted with the stopBoolean
	 */
	public static long copyUntil(InputStream input, OutputStream output, AtomicBoolean stopBoolean) throws IOException, CopyInterruptedException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n = 0;
		while (EOF != (n = input.read(buffer)) && !stopBoolean.get()) {
			output.write(buffer, 0, n);
			count += n;
		}
		if (stopBoolean.get()) {
			throw new CopyInterruptedException("The copyUntil got interrupted by the stopBoolean.");
		}
		if (count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) count;
	}
}
