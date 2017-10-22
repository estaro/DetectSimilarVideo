package io.github.estaro.dsv.bean;

import java.util.List;

public class Config {

	private Integer imageWidth;

	private Integer captureCount;

	private String tempDir;

	private List<String> dirList;

	public List<String> getDirList() {
		return dirList;
	}

	public void setDirList(List<String> dirList) {
		this.dirList = dirList;
	}

	public String getTempDir() {
		return tempDir;
	}

	public void setTempDir(String tempDir) {
		this.tempDir = tempDir;
	}

	public Integer getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(Integer imageWidth) {
		this.imageWidth = imageWidth;
	}

	public Integer getCaptureCount() {
		return captureCount;
	}

	public void setCaptureCount(Integer captureCount) {
		this.captureCount = captureCount;
	}

}
