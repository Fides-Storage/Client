package org.fides.client.connector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.fides.components.Actions;
import org.fides.components.Responses;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

/**
 * The ServerConnector unit test
 */
public class ServerConnectorTest {

	private ServerConnector connector;

	private ByteArrayOutputStream mockedOutputStream;

	/**
	 * Runs before every test. DataOutputStream will be mocked here, it is used in every test
	 */
	@Before
	public void setupTest() {
		connector = mock(ServerConnector.class);
		Mockito.when(connector.isConnected()).thenReturn(true);

		mockedOutputStream = new ByteArrayOutputStream();
		DataOutputStream mockedDataOutputStream = new DataOutputStream(mockedOutputStream);
		Whitebox.setInternalState(connector, "out", mockedDataOutputStream);

		try {
			mockedOutputStream.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}

	}

	/**
	 * Test whether the user can be registered.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRegistrationOfUser() throws IOException {
		ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
		DataOutputStream writeJson = new DataOutputStream(byteArrayOutput);
		Mockito.when(connector.register(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		writeJson.writeUTF("{" + Responses.SUCCESSFUL + ":true}");
		byteArrayOutput.close();
		writeJson.close();

		DataInputStream mockedDataInputStream = new DataInputStream(new ByteArrayInputStream(byteArrayOutput.toByteArray()));
		Whitebox.setInternalState(connector, "in", mockedDataInputStream);

		// Asserts true will be returned by the register method with the given user name and password
		assertTrue(connector.register("usernameTest", "passwordTest"));

		// Asserts that the expected Json text is equal to the actual Json text
		ByteArrayInputStream readMockedOutputStreamAsByteArray = new ByteArrayInputStream(mockedOutputStream.toByteArray());
		DataInputStream readByteArrayAsData = new DataInputStream(readMockedOutputStreamAsByteArray);

		String answer = readByteArrayAsData.readUTF();
		assertTrue(answer.contains("\"" + Actions.ACTION + "\":\"" + Actions.CREATE_USER + "\""));
		assertTrue(answer.contains("\"" + Actions.Properties.PASSWORD_HASH + "\":\"passwordTest\""));
		assertTrue(answer.contains("\"" + Actions.Properties.USERNAME_HASH + "\":\"usernameTest\""));
	}

	/**
	 * Test whether the user can log in with a user name and password
	 * 
	 * @throws IOException
	 */
	@Test
	public void testLoginOfUser() throws IOException {
		ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
		DataOutputStream writeJson = new DataOutputStream(byteArrayOutput);
		Mockito.when(connector.login(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		writeJson.writeUTF("{" + Responses.SUCCESSFUL + ":true}");
		byteArrayOutput.close();
		writeJson.close();

		DataInputStream mockedDataInputStream = new DataInputStream(new ByteArrayInputStream(byteArrayOutput.toByteArray()));
		Whitebox.setInternalState(connector, "in", mockedDataInputStream);

		// Asserts true will be returned by the login method with the given user name and password
		assertTrue(connector.login("usernameTest", "passwordTest"));

		// Asserts that the expected Json text is equal to the actual Json text
		ByteArrayInputStream readMockedOutputStreamAsByteArray = new ByteArrayInputStream(mockedOutputStream.toByteArray());
		DataInputStream readByteArrayAsData = new DataInputStream(readMockedOutputStreamAsByteArray);

		String answer = readByteArrayAsData.readUTF();
		assertTrue(answer.contains("\"" + Actions.ACTION + "\":\"" + Actions.LOGIN + "\""));
		assertTrue(answer.contains("\"" + Actions.Properties.PASSWORD_HASH + "\":\"passwordTest\""));
		assertTrue(answer.contains("\"" + Actions.Properties.USERNAME_HASH + "\":\"usernameTest\""));

	}

	/**
	 * Tests when an already existing user name is used, false will be returned
	 */
	@Test
	public void testRegisterWithInvalidUsername() {
		ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
		DataOutputStream writeJson = new DataOutputStream(byteArrayOutput);
		Mockito.when(connector.register(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		try {
			writeJson.writeUTF("{" + Responses.SUCCESSFUL + ":false}");
			byteArrayOutput.close();
			writeJson.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		DataInputStream mockedDataInputStream = new DataInputStream(new ByteArrayInputStream(byteArrayOutput.toByteArray()));
		Whitebox.setInternalState(connector, "in", mockedDataInputStream);

		// Asserts false, will be returned when trying to register with an already existing user name
		assertFalse(connector.register("usernameTest", "passwordTest"));
	}

	/**
	 * Tests when an invalid user name and password combination is used, false will be returned
	 */
	@Test
	public void testLoginWithInvalidUser() {
		ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
		DataOutputStream writeJson = new DataOutputStream(byteArrayOutput);
		Mockito.when(connector.login(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		try {
			writeJson.writeUTF("{" + Responses.SUCCESSFUL + ":false}");
			byteArrayOutput.close();
			writeJson.close();

		} catch (IOException e) {
			fail(e.getMessage());
		}
		DataInputStream mockedDataInputStream = new DataInputStream(new ByteArrayInputStream(byteArrayOutput.toByteArray()));
		Whitebox.setInternalState(connector, "in", mockedDataInputStream);

		// Asserts false, will be returned when trying to login with an invalid user name and password combination
		assertFalse(connector.login("usernameTest", "passwordTest"));
	}

	/**
	 * Test whether the IOException will be caught and false will be returned properly
	 */
	@Test
	public void testRegisterCatchIOException() {
		ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
		DataOutputStream writeJson = new DataOutputStream(byteArrayOutput);
		Mockito.when(connector.register(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		try {
			byteArrayOutput.close();
			writeJson.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		DataInputStream mockedDataInputStream = new DataInputStream(new ByteArrayInputStream(byteArrayOutput.toByteArray()));
		Whitebox.setInternalState(connector, "in", mockedDataInputStream);

		// Assert false when no data is given by the server
		assertFalse(connector.register("usernameTest", "passwordTest"));
	}

	/**
	 * Test whether the IOException will be caught and false will be returned properly
	 */
	@Test
	public void testLoginCatchIOException() {
		ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
		DataOutputStream writeJson = new DataOutputStream(byteArrayOutput);
		Mockito.when(connector.login(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		try {
			byteArrayOutput.close();
			writeJson.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		DataInputStream mockedDataInputStream = new DataInputStream(new ByteArrayInputStream(byteArrayOutput.toByteArray()));
		Whitebox.setInternalState(connector, "in", mockedDataInputStream);

		// Assert false when no data is given by the server
		assertFalse(connector.login("usernameTest", "passwordTest"));
	}
}
