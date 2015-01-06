package org.fides.client.tools;

/**
 * An {@link Exception} thrown when the {@link CopyTool} gets interrupted while copying.
 */
public class CopyInterruptedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3624421907264642816L;

	/**
	 * Constructor for CopyInterruptedException without a message.
	 */
	public CopyInterruptedException() {
		super();
	}

	/**
	 * Constructor for CopyInterruptedException with a message.
	 * 
	 * @param message
	 *            The message
	 */
	public CopyInterruptedException(String message) {
		super(message);
	}

}
