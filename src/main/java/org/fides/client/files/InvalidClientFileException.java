package org.fides.client.files;

// TODO: unused import?
import org.fides.client.files.data.ClientFile;

/**
 * An {@link Exception} thrown when no {@link ClientFile} or an invalid {@link ClientFile} is given
 * 
 * @author Koen
 *
 */
public class InvalidClientFileException extends Exception {

	private static final long serialVersionUID = -5396351565109464521L;

	/**
	 * Empty Constructor
	 */
	public InvalidClientFileException() {
		super();
	}

	/**
	 * Constructor with message
	 * 
	 * @param message
	 *            The message
	 */
	public InvalidClientFileException(String message) {
		super(message);
	}

}
