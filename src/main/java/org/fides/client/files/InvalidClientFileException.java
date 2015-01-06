package org.fides.client.files;

import org.fides.client.files.data.ClientFile;

/**
 * An {@link Exception} thrown when no {@link ClientFile} or an invalid {@link ClientFile} is given
 * 
 */
public class InvalidClientFileException extends Exception {

	private static final long serialVersionUID = -5396351565109464521L;

	/**
	 * Constructor for InvalidClientFileException without a message.
	 */
	public InvalidClientFileException() {
		super();
	}

	/**
	 * Constructor for InvalidClientFileException with a message.
	 * 
	 * @param message
	 *            The message
	 */
	public InvalidClientFileException(String message) {
		super(message);
	}

}
