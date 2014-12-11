package org.fides.client.tools;

import static org.junit.Assert.assertEquals;

import org.fides.tools.HashUtils;
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
		assertEquals("717ac506950da0ccb6404cdd5e7591f72018a20cbca27c8a423e9c9e5626ac61", hashTest);
	}

	/**
	 * Expects a NullPointerException when null is given
	 */
	@Test(expected = NullPointerException.class)
	public void testHashFunctionWithNullArgument() {
		HashUtils.hash(null);
	}
}
