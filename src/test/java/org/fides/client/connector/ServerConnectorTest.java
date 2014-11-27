package org.fides.client.connector;

import org.junit.Test;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The serverconnector unit test
 */
public class ServerConnectorTest {

	@Test
	public void testConnect() {
		ServerConnector connector = new ServerConnector();

			try {
				connector.connect(new InetSocketAddress("localhost", 4444));
				assertTrue(connector.isConnected());
				connector.disconnect();
			} catch (ConnectException | UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	}
}
