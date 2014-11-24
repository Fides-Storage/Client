package org.fides.client.connector;

import org.junit.Test;

import java.net.UnknownHostException;

import static org.junit.Assert.fail;

/**
 * The serverconnector unit test
 */
public class ServerConnectorTest {

	/**
	 * This will test the test function in the ServerConnector class
	 */
	@Test
	public void testConnect() {
		ServerConnector serverConnector = new ServerConnector();
		try {
			serverConnector.connect("localhost", 4444);
		} catch (UnknownHostException e) {
			fail("Connection to the given server failed");
		}
	}
}
