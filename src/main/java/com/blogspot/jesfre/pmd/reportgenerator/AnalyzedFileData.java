package com.blogspot.jesfre.pmd.reportgenerator;

public class AnalyzedFileData {
	private String originalFileName;
	private String fileLocation;
	private long revisionNumber;

	public AnalyzedFileData() {
	}

	public AnalyzedFileData(String originalFileName, String repositoryPath, long revisionNumber) {
		super();
		this.originalFileName = originalFileName;
		this.fileLocation = repositoryPath;
		this.revisionNumber = revisionNumber;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}

	public String getFileLocation() {
		return fileLocation;
	}

	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	public long getRevisionNumber() {
		return revisionNumber;
	}

	public void setRevisionNumber(long revisionNumber) {
		this.revisionNumber = revisionNumber;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AnalyzedFileData [originalFileName=");
		builder.append(originalFileName);
		builder.append(", fileLocation=");
		builder.append(fileLocation);
		builder.append(", revisionNumber=");
		builder.append(revisionNumber);
		builder.append("]");
		return builder.toString();
	}
}