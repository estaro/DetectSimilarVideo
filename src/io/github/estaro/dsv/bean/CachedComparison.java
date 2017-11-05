package io.github.estaro.dsv.bean;

import java.io.Serializable;

public class CachedComparison implements Serializable {

	public String file1;

	public String file2;

	public Long playtimeDiff;

	public Double hist;

	public Double feature;

	public Integer skip;

	public String getKey() {
		return file1 + "<--->" + file2;
	}

}
