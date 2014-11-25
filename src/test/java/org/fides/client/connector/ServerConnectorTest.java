package org.fides.client.connector;

import org.junit.Test;

import java.net.UnknownHostException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
			Thread.sleep(3000);

			assertFalse(serverConnector.isConnected());
		} catch (UnknownHostException e) {
			fail("Connection to the given server failed");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
