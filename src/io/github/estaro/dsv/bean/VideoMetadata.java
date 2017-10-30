package io.github.estaro.dsv.bean;

import java.util.List;

import org.opencv.core.Mat;

public class VideoMetadata {

	private String filename;

	private String frameDirname;

	private Long playTime;

	private List<Mat> histgramImg;

	private List<Mat> featureImg;

	public String getImagefile(int index) {
		return frameDirname + "/" + index + ".jpg";
	}


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

	public List<Mat> getHistgramImg() {
		return histgramImg;
	}

	public void setHistgramImg(List<Mat> histgramImg) {
		this.histgramImg = histgramImg;
	}

	public List<Mat> getFeatureImg() {
		return featureImg;
	}

	public void setFeatureImg(List<Mat> featureImg) {
		this.featureImg = featureImg;
	}


}
