package org.fides.client.files;

/**
 * This enum contains the type of results is a {@link FileCompareResult} when comparing files
 */
public enum CompareResultType {
	/**
	 * The file is added on the server
	 */
	SERVER_ADDED,
	/**
	 * The file is added locally
	 */
	LOCAL_ADDED,
	/**
	 * The file is removed on the server
	 */
	SERVER_REMOVED,
	/**
	 * The local file is removed
	 */
	LOCAL_REMOVED,
	/**
	 * The file on the server is changed
	 */
	SERVER_UPDATED,
	/**
	 * The local file is changed
	 */
	LOCAL_UPDATED,
	/**
	 * Both the files on the server and locally are changed
	 */
	CONFLICTED

}
