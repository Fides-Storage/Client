package org.fides.client.files.data;

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
