package org.fides.client.files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An {@link Exception} thrown when no {@link ClientFile} or on invalid {@link ClientFile} is given
 * 
 * @author Koen
 * 
 */
public class InvalidClientFileException extends Exception {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(InvalidClientFileException.class);

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
