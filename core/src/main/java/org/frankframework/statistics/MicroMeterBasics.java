/*
   Copyright 2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.statistics;

import java.text.DecimalFormat;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import lombok.Setter;
import org.frankframework.statistics.MicroMeterBasics.MicroMeterSnapshot;
import org.frankframework.util.XmlBuilder;

/**
 * Container for basic statistical estimators, based on MicroMeter DistributionSummary.
 *
 * @author  Gerrit van Brakel
 */
public class MicroMeterBasics implements IBasics<MicroMeterSnapshot> {

	public static final int NUM_BASIC_ITEMS=6;

	private @Setter DistributionSummary distributionSummary;
	private HistogramSnapshot snapshot;

	protected long min = Long.MAX_VALUE;
	protected long sumOfSquares=0;

	protected class MicroMeterSnapshot {
		protected HistogramSnapshot histogramSnapshot;
		protected long min = Long.MAX_VALUE;
		protected long max = 0;
		protected long sumOfSquares;
	}


	@Override
	public MicroMeterSnapshot takeSnapshot() {
		MicroMeterSnapshot result = new MicroMeterSnapshot();
		result.histogramSnapshot = distributionSummary!=null ? distributionSummary.takeSnapshot() : null;
		result.sumOfSquares= sumOfSquares;
		return result;
	}

	@Override
	public void addValue(long value) {
		if (distributionSummary!=null) {
			distributionSummary.record(value);
		}
		checkMinMax(value);
		addSums(value);
	}

	@Override
	public void checkMinMax(long value) {
		if (value < min) {
			min = value;
		}
	}

	@Override
	public long getIntervalCount(MicroMeterSnapshot mark) {
		return getCount() - mark.histogramSnapshot.count();
	}

	@Override
	public long getIntervalMin(MicroMeterSnapshot mark) {
		return mark.min;
	}

	@Override
	public long getIntervalMax(MicroMeterSnapshot mark) {
		return mark.max;
	}

	@Override
	public void updateIntervalMinMax(MicroMeterSnapshot mark, long value) {
		if (mark.min>value) {
			mark.min = value;
		}
		if (mark.max<value) {
			mark.max = value;
		}
	}
	@Override
	public long getIntervalSum(MicroMeterSnapshot mark) {
		return getSum() - Math.round(mark.histogramSnapshot.total());
	}

	@Override
	public long getIntervalSumOfSquares(MicroMeterSnapshot mark) {
		return getSumOfSquares() - mark.sumOfSquares;
	}

	@Override
	public double getIntervalAverage(MicroMeterSnapshot mark) {
		long intervalCount=getIntervalCount(mark);
		if (intervalCount==0) {
			return 0;
		}
		return getIntervalSum(mark)/(double)(intervalCount);
	}

	@Override
	public double getIntervalVariance(MicroMeterSnapshot mark) {
		return calculateVariance(getIntervalCount(mark), getIntervalSum(mark), getIntervalSumOfSquares(mark));
	}



	protected void addSums(long value) {
		sumOfSquares += value * value;
	}

	private double calculateVariance(long count, long sum, long sumOfSquares) {
		double result;
		if (count>1) {
			result=(sumOfSquares-((sum*sum)/count))/(count-1);
		} else result=Double.NaN;
		return result;
	}


	@Override
	public int getItemCount() {
		return NUM_BASIC_ITEMS;
	}

	@Override
	public String getItemName(int index) {
		switch (index) {
			case 0: return ITEM_NAME_COUNT;
			case 1: return ITEM_NAME_MIN;
			case 2: return ITEM_NAME_MAX;
			case 3: return ITEM_NAME_AVERAGE;
			case 4: return ITEM_NAME_STDDEV;
			case 5: return ITEM_NAME_SUM;
			default : throw new IllegalArgumentException("item index ["+index+"] outside allowed range [0,"+(NUM_BASIC_ITEMS-1)+"]");
		}
	}

	@Override
	public Type getItemType(int index) {
		switch (index) {
			case 0: return Type.INTEGER;
			case 1: return Type.TIME;
			case 2: return Type.TIME;
			case 3: return Type.TIME;
			case 4: return Type.TIME;
			case 5: return Type.TIME;
			default : throw new IllegalArgumentException("item index ["+index+"] outside allowed range [0,"+(NUM_BASIC_ITEMS-1)+"]");
		}
	}

	@Override
	public Object getItemValue(int index) {
		switch (index) {
			case 0: return new Long(getCount());
			case 1: if (getCount() == 0) return null; else return new Long(getMin());
			case 2: if (getCount() == 0) return null; else return new Long(getMax());
			case 3: if (getCount() == 0) return null; else return new Double(getAverage());
			case 4: if (getCount() == 0) return null; else return new Double(getStdDev());
			case 5: if (getCount() == 0) return null; else return new Long(getSum());
			default : throw new IllegalArgumentException("item index ["+index+"] outside allowed range [0,"+(NUM_BASIC_ITEMS-1)+"]");
		}
	}

	protected XmlBuilder toXml(String elementName, String name, DecimalFormat timeFormat, DecimalFormat percentageFormat) {
		return ItemUtil.toXml(this, elementName, name, timeFormat, percentageFormat);
	}



	@Override
	public long getCount() {
		return distributionSummary!=null ? distributionSummary.count() : 0;
	}
	public long getIntervalCount(MicroMeterBasics mark) {
		return getCount()-mark.getCount();
	}

	@Override
	public long getMax() {
		return distributionSummary!=null ? Math.round(distributionSummary.max()) : 0;
	}

	@Override
	public long getMin() {
		return min;
	}

	@Override
	public long getSum() {
		return distributionSummary!=null ? Math.round(distributionSummary.totalAmount()) : 0;
	}
	@Override
	public long getSumOfSquares() {
		return sumOfSquares;
	}

	public long getIntervalSum(MicroMeterBasics mark) {
		return getSum()-Math.round(mark.snapshot.total());
	}
	public long getIntervalSumOfSquares(MicroMeterBasics mark) {
		return sumOfSquares-mark.getSumOfSquares();
	}

	@Override
	public double getAverage() {
		return distributionSummary!=null ? distributionSummary.mean() : 0;
	}
	public double getIntervalAverage(MicroMeterBasics mark) {
		long intervalCount=getIntervalCount(mark);
		if (intervalCount==0) {
			return 0;
		}
		return getIntervalSum(mark)/(double)(intervalCount);
	}

	@Override
	public double getVariance() {
		if (distributionSummary==null) {
			return Double.NaN;
		}
		HistogramSnapshot snapshot = distributionSummary.takeSnapshot();
		return calculateVariance(snapshot.count(), Math.round(snapshot.total()), sumOfSquares);
	}
	public double getIntervalVariance(MicroMeterBasics mark) {
		if (distributionSummary==null) {
			return Double.NaN;
		}
		HistogramSnapshot snapshot = distributionSummary.takeSnapshot();
		return calculateVariance(snapshot.count()-mark.getCount(), Math.round(snapshot.total())-mark.getSum(), sumOfSquares-mark.getSumOfSquares());
	}

	@Override
	public double getStdDev() {
		return Math.sqrt(getVariance());
	}

}
