package org.fides.client.connector;

import java.io.OutputStream;

/**
 * Contains an {@link OutputStream} to a specific location on the server
 */
public class LocationOutputStreamPair {

	private OutputStream outputStream;

	private String location;

	/**
	 * Constructor
	 * 
	 * @param outputStream
	 *            The outputstream writing to the server location
	 * @param location
	 *            The location on the server
	 */
	public LocationOutputStreamPair(OutputStream outputStream, String location) {
		super();
		this.outputStream = outputStream;
		this.location = location;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public String getLocation() {
		return location;
	}

}
