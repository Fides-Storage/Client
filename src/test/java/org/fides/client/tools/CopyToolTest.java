package org.fides.client.tools;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;

/**
 * The test class for the CopyTool class. This class tests both the copy and the interruption of a copy.
 */
public class CopyToolTest {

	private AtomicBoolean stopBoolean;

	private AtomicBoolean emergencyBreakBoolean = new AtomicBoolean(false);

	/**
	 * Sets up the test by resetting the stopBoolean.
	 */
	@Before
	public void setup() {
		stopBoolean = new AtomicBoolean(false);
	}

	/**
	 * Tests the interruption of a copy by copying a constant stream of zeros using the copy function and interrupting
	 * it after 1 second. If the copyUntil is still running after 10 seconds, the stream will close (to prevent an
	 * overflow) and the test will fail.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testInterruption() throws IOException {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
					stopBoolean.set(true);
				} catch (InterruptedException e) {
					fail();
				}
			}
		});

		Thread emergencyBreak = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(10000);
					emergencyBreakBoolean.set(true);
				} catch (InterruptedException e) {
					// empty catch block, doesn't matter if this wait gets interrupted
					emergencyBreakBoolean.set(true);
				}
			}
		});

		InputStream tenSecondInputStream = new InputStream() {
			private int counter = 0;

			@Override
			public int read() throws IOException {
				if (!emergencyBreakBoolean.get()) {
					try {
						if (++counter >= 100) {
							Thread.sleep(10);
							counter = 0;
						}
					} catch (InterruptedException e) {
						// empty catch block, doesn't matter if this wait gets interrupted.
					}
					return 0;
				} else {
					return -1;
				}
			}
		};

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		boolean successful = false;
		try {
			thread.start();
			emergencyBreak.start();
			CopyTool.copyUntil(tenSecondInputStream, outputStream, stopBoolean);
		} catch (CopyInterruptedException e) {
			successful = true;
		}
		thread.interrupt();
		emergencyBreak.interrupt();
		assertTrue(successful);
	}

	/**
	 * Tests if copying a stream using copyUntil successfully copies it.
	 * 
	 * @throws IOException
	 * @throws CopyInterruptedException
	 */
	@Test
	public void testCopy() throws IOException, CopyInterruptedException {
		byte[] testArray = "This is a test sentence.".getBytes();
		InputStream in = new ByteArrayInputStream(testArray);
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		CopyTool.copyUntil(in, out, stopBoolean);
		out.flush();
		out.close();
		in.close();

		assertArrayEquals(testArray, out.toByteArray());
	}
}
