package io.github.estaro.dsv.bean;

import java.io.Serializable;

public class VideoMetadata implements Serializable {

	private String filename;

	private String frameDirname;

	private String metadataLabel;

	public String getMetadataLabel() {
		return metadataLabel;
	}

	public void setMetadataLabel(String metadataLabel) {
		this.metadataLabel = metadataLabel;
	}

	public String getImagefile(int index) {
		return frameDirname + "/" + index + ".jpg";
	}

	public String getHistFilename(int index) {
		return frameDirname + "/" + index + "_hist.json";
	}

	public String getFeatFilename(int index) {
		return frameDirname + "/" + index + "_feat.json";
	}

	public String getMetaFilename() {
		return frameDirname + "/metadata.properties";
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

}
