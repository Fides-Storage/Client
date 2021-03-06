package org.fides.client.files.data;

/**
 * A result of the KeyFile when comparing the local files and the files on the server
 * 
 */
public class FileCompareResult {

	private final String name;

	private final CompareResultType resultType;

	/**
	 * Constructor for FileCompareResult
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

	public CompareResultType getResultType() {
		return resultType;
	}

	@Override
	public String toString() {
		return "FileCompareResult [name=" + name + ", resultType=" + resultType + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((resultType == null) ? 0 : resultType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FileCompareResult other = (FileCompareResult) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (resultType != other.resultType) {
			return false;
		}
		return true;
	}

}
