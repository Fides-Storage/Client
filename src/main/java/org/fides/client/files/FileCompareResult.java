package org.fides.client.files;

/**
 * A result of the {@link FileManager#compareFiles(KeyFile)} when comparing the local files and the files on the server
 * 
 * @author Koen
 *
 */
public class FileCompareResult {

	private String name;

	private CompareResultType resultType;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            The name of the file
	 * @param resultType
	 *            The type of result
	 */
	public FileCompareResult(String name, CompareResultType resultType) {
		super();
		this.name = name;
		this.resultType = resultType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CompareResultType getResultType() {
		return resultType;
	}

	public void setResultType(CompareResultType resultType) {
		this.resultType = resultType;
	}

	@Override
	public String toString() {
		return "FileCompareResult [name=" + name + ", resultType=" + resultType + "]";
	}

}
