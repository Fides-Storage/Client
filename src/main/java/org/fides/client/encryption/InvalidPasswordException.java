package org.fides.client.encryption;

/**
 * An {@link Exception} thrown when a password is incorrect
 *
 */
public class InvalidPasswordException extends Exception {

	private static final long serialVersionUID = -5396351565109464521L;

	/**
	 * Constructor for InvalidPasswordException without a message.
	 */
	public InvalidPasswordException() {
		super();
	}

}
