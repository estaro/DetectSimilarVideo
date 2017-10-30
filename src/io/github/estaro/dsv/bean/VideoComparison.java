package io.github.estaro.dsv.bean;

public class VideoComparison {

	private VideoMetadata video1;

	private VideoMetadata video2;

	private Double hist;

	private Double feature;

	private String filename1;

	private String filename2;

	private Long playtime;

	public Long getPlaytime() {
		return playtime;
	}

	public void setPlaytime(Long playtime) {
		this.playtime = playtime;
	}

	public String getFilename1() {
		if (video1 == null) {
			return filename1;
		}
		return video1.getFilename();
	}

	public void setFilename1(String filename1) {
		this.filename1 = filename1;
	}

	public String getFilename2() {
		if (video2 == null) {
			return filename2;
		}
		return video2.getFilename();
	}

	public void setFilename2(String filename2) {
		this.filename2 = filename2;
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
