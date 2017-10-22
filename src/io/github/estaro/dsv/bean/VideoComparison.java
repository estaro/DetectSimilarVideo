package io.github.estaro.dsv.bean;

public class VideoComparison {

	private VideoMetadata video1;

	private VideoMetadata video2;

	private Double hist;

	private Double feature;


	public long getPlayTimeDiff ()  {
		return Math.abs(video1.getPlayTime() - video2.getPlayTime());
	}

	public VideoMetadata getVideo1() {
		return video1;
	}

	public void setVideo1(VideoMetadata video1) {
		this.video1 = video1;
	}

	public VideoMetadata getVideo2() {
		return video2;
	}

	public void setVideo2(VideoMetadata video2) {
		this.video2 = video2;
	}

	public Double getHist() {
		return hist;
	}

	public void setHist(Double hist) {
		this.hist = hist;
	}

	public Double getFeature() {
		return feature;
	}

	public void setFeature(Double feature) {
		this.feature = feature;
	}


}
