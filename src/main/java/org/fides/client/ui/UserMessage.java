package org.fides.client.ui;

/**
 * Small wrapper for a user message
 * 
 * @author jesse
 *
 */
public class UserMessage {

	private String message;

	private boolean error;

	/**
	 * Constructor for user message
	 * 
	 * @param message
	 * @param error
	 */
	public UserMessage(String message, boolean error) {
		this.message = message;
		this.error = error;
	}

	public boolean isError() {
		return error;
	}

	public String getMessage() {
		return message;
	}

}
