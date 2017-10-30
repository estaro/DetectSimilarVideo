package io.github.estaro.dsv.bean;

import com.orangesignal.csv.annotation.CsvColumn;
import com.orangesignal.csv.annotation.CsvEntity;

@CsvEntity(header = false)
public class CachedComparison {

	@CsvColumn(position = 0)
	public String file1;

	@CsvColumn(position = 1)
	public String file2;

	@CsvColumn(position = 2)
	public Long playtimeDiff;

	@CsvColumn(position = 3)
	public Double hist;

	@CsvColumn(position = 4)
	public Double feature;

}
