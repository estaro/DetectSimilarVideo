package io.github.estaro.dsv.bean;

public class VideoMetadata {

	private String filename;

	private String frameDirname;

	private Long playTime;

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFrameDirname() {
		return frameDirname;
	}

	public void setFrameDirname(String frameDirname) {
		this.frameDirname = frameDirname;
	}

	public Long getPlayTime() {
		return playTime;
	}

	public void setPlayTime(Long playTime) {
		this.playTime = playTime;
	}

}
