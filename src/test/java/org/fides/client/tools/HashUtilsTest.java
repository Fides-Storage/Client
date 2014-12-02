package org.fides.client.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test class for hash utilities
 * 
 * @author Niels
 * 
 */
public class HashUtilsTest {

	/**
	 * Compares the expected hash with the actual hash to be equal
	 */
	@Test
	public void testHashFunction() {
		String hashTest = HashUtils.hash("This is a test string");
		assertEquals("cXrFBpUNoMy2QEzdXnWR9yAYogy8onyKQj6cnlYmrGE=", hashTest);
	}

}
