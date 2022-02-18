/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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
package nl.nn.adapterframework.statistics;

import java.text.DecimalFormat;

import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Container for basic statistical estimators.
 * 
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.9
 */
public class Basics implements ItemList {

	public static final int NUM_BASIC_ITEMS=6;   

/*
 * Capacity calculation.
 * c: number of bits in count
 * d: number of databits
 * 
 * int:  32 bits
 * long: 64 bits
 * 
 * lengths
 * count: c
 * sum: c+d
 * sumOfSquares: c+2d 
 */
	
	
	protected long count = 0;
	protected long min = Long.MAX_VALUE;
	protected long max = 0;
	protected long sum = 0;
	protected long sumOfSquares=0;
	
	public void reset() {
		count = 0;
		min = Long.MAX_VALUE;
		max = 0;
		sum = 0;
		sumOfSquares=0;
	}

	public void mark(Basics other) {
		min = Long.MAX_VALUE;
		max = 0;
		count = other.count;
		sum = other.sum;
		sumOfSquares=other.sumOfSquares;
	}
	
	public void addValue(long value) {
		++count;
		checkMinMax(value);
		addSums(value);
	}
	
	protected void checkMinMax(long value) {
		if (value < min) {
			min = value;
		}
		if (value > max) {
			max = value;
		}
	}

	protected void addSums(long value) {
		sum += value;
		sumOfSquares += value * value;
	}

	public void addRecord(Basics record) {
		count+=record.getCount();
		if (record.getMin() < min) {
			min = record.getMin();
		}
		if (record.getMax() > max) {
			max = record.getMax();
		}
		sum += record.getSum();
		sumOfSquares += record.getSumOfSquares();
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


	
	public long getCount() {
		return count;
	}
	public long getIntervalCount(Basics mark) {
		return count-mark.getCount();
	}

	public long getMax() {
		return max;
	}

	public long getMin() {
		return min;
	}

	public long getSum() {
		return sum;
	}
	public long getSumOfSquares() {
		return sumOfSquares;
	}

	public long getIntervalSum(Basics mark) {
		return sum-mark.getSum();
	}
	public long getIntervalSumOfSquares(Basics mark) {
		return sumOfSquares-mark.getSumOfSquares();
	}

	public double getAverage() {
		if (count == 0) {
			return 0;
		}
		return (sum / (double)count);
	}
	public double getIntervalAverage(Basics mark) {
		long intervalCount=getIntervalCount(mark);
		if (intervalCount==0) {
			return 0;
		}
		return getIntervalSum(mark)/(double)(intervalCount);
	}

	public double getVariance() {
		return calculateVariance(count, sum, sumOfSquares);
	}
	public double getIntervalVariance(Basics mark) {
		return calculateVariance(count-mark.getCount(), sum-mark.getSum(), sumOfSquares-mark.getSumOfSquares());
	}

	public double getStdDev() {
		return Math.sqrt(getVariance());
	}

}
