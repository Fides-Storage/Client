package org.fides.client.connector;

import java.io.OutputStream;

/**
 * Contains an {@link OutputStream} to a specific location on the server
 */
public class OutputStreamData {

	private final OutputStream outputStream;

	private final String location;

	/**
	 * Constructor
	 * 
	 * @param outputStream
	 *            The outputstream writing to the server location
	 * @param location
	 *            The location on the server
	 */
	public OutputStreamData(OutputStream outputStream, String location) {
		super();
		this.outputStream = outputStream;
		this.location = location;
	}

	public final OutputStream getOutputStream() {
		return outputStream;
	}

	public final String getLocation() {
		return location;
	}

}
