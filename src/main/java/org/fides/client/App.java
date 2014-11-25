package org.fides.client;

import org.fides.client.connector.ServerConnector;
import org.fides.client.ui.UsernamePasswordScreen;

/**
 * Hello world!
 *
 */
public class App {

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ServerConnector serverConnector = new ServerConnector();

		while (true) {

			String[] data = UsernamePasswordScreen.getUsernamePassword();

			if (data == null) {
				System.exit(1);
			}

			if ((data[0]).equals("register")) {

				// checks if password and password confirmation is the same
				if (data[2].equals(data[3])) {
					// register on the server
					if (serverConnector.register(data[1], data[2])) {
						System.out.println("Register succesfull");
					} else {
						System.out.println("Register failed");
					}
				} else {
					System.out.println("Register password confirmation is not valid.");
				}
			} else if ((data[0]).equals("login")) {
				if (serverConnector.login(data[1], data[2])) {
					break;
				} else {
					System.out.println("Login failed");
				}
			}

		}

		if (serverConnector.isConnected()) {
			// TODO: Do normal work
		} else {
			System.exit(1);

		}

	}

}
