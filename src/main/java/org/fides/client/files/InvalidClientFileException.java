package org.fides.client.files;

/**
 * An {@link Exception} thrown when no {@link ClientFile} or on invalid {@link ClientFile} is given
 * 
 * @author Koen
 *
 */
public class InvalidClientFileExeption extends Exception {

	private static final long serialVersionUID = -5396351565109464521L;

	/**
	 * Emptry Constructor
	 */
	public InvalidClientFileExeption() {
		super();
	}

	/**
	 * Constructor with message
	 * 
	 * @param message
	 *            The message
	 */
	public InvalidClientFileExeption(String message) {
		super(message);
	}

}
