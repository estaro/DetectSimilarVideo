package io.github.estaro.dsv.bean;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TableItem {
	private final StringProperty dir1 = new SimpleStringProperty();

	private final StringProperty dir2 = new SimpleStringProperty();

	private final DoubleProperty time = new SimpleDoubleProperty();

	private final DoubleProperty hist = new SimpleDoubleProperty();

	private final DoubleProperty feature = new SimpleDoubleProperty();

	private VideoComparison org;

	public VideoComparison getOrg() {
		return org;
	}

	public TableItem(VideoComparison compared) {
		setDir1(compared.getFilename1());
		setDir2(compared.getFilename2());
		setFeature(compared.getFeature());
		setHist(compared.getHist());
		setTime(compared.getPlaytime());
		org = compared;
	}

	public final StringProperty dir1Property() {
		return this.dir1;
	}

	public final String getDir1() {
		return this.dir1Property().get();
	}

	public final void setDir1(final String dir1) {
		this.dir1Property().set(dir1);
	}

	public final StringProperty dir2Property() {
		return this.dir2;
	}

	public final String getDir2() {
		return this.dir2Property().get();
	}

	public final void setDir2(final String dir2) {
		this.dir2Property().set(dir2);
	}

	public final DoubleProperty timeProperty() {
		return this.time;
	}

	public final double getTime() {
		return this.timeProperty().get();
	}

	public final void setTime(final double time) {
		this.timeProperty().set(time);
	}

	public final DoubleProperty histProperty() {
		return this.hist;
	}

	public final double getHist() {
		return this.histProperty().get();
	}

	public final void setHist(final double hist) {
		this.histProperty().set(hist);
	}

	public final DoubleProperty featureProperty() {
		return this.feature;
	}

	public final double getFeature() {
		return this.featureProperty().get();
	}

	public final void setFeature(final double feature) {
		this.featureProperty().set(feature);
	}

}
