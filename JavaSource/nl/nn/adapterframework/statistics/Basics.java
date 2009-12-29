/*
 * $Log: Basics.java,v $
 * Revision 1.1  2009-12-29 14:25:18  L190409
 * moved statistics to separate package
 *
 */
package nl.nn.adapterframework.statistics;

import java.text.DecimalFormat;

import nl.nn.adapterframework.util.XmlBuilder;

/**
 * @author  Gerrit van Brakel
 * @since   4.9.9
 * @version Id
 */
public class Basics implements ItemList {

	public static final int NUM_BASIC_ITEMS=6;   

	
	protected long count = 0;
	protected long min = Integer.MAX_VALUE;
	protected long max = 0;
	protected long total = 0;
	protected long totalSquare=0;
	
	public void reset() {
		count = 0;
		min = Integer.MAX_VALUE;
		max = 0;
		total = 0;
		totalSquare=0;
	}

	public void mark(Basics other) {
		min = Integer.MAX_VALUE;
		max = 0;
		count = other.count;
		total = other.total;
		totalSquare=other.totalSquare;
	}
	
	public void addValue(long value) {
		++count;
		if (value < min) {
			min = value;
		}
		if (value > max) {
			max = value;
		}
		total += value;
		totalSquare += value * value;
	}
	
	public void checkMinMax(long value) {
		if (value < min) {
			min = value;
		}
		if (value > max) {
			max = value;
		}
	}

	public void addRecord(Basics record) {
		count+=record.getCount();
		if (record.getMin() < min) {
			min = record.getMin();
		}
		if (record.getMax() > max) {
			max = record.getMax();
		}
		total += record.getTotal();
		totalSquare += record.getTotalSquare();
	}
	
	private double calculateVariance(long count, long total, long totalSquare) {
		double result;
		if (count>1) {
			result=(totalSquare-((total*total)/count))/(count-1);

		}
		else result=Double.NaN;
		return result;
	}


	public int getItemCount() {
		return NUM_BASIC_ITEMS;
	}

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

	public int getItemType(int index) {
		switch (index) {
			case 0: return ITEM_TYPE_INTEGER;
			case 1: return ITEM_TYPE_TIME;
			case 2: return ITEM_TYPE_TIME;
			case 3: return ITEM_TYPE_TIME;
			case 4: return ITEM_TYPE_TIME;
			case 5: return ITEM_TYPE_TIME;
			default : throw new IllegalArgumentException("item index ["+index+"] outside allowed range [0,"+(NUM_BASIC_ITEMS-1)+"]");
		}
	}

	public Object getItemValue(int index) {
		switch (index) {
			case 0: return new Long(getCount());
			case 1: if (getCount() == 0) return null; else return new Long(getMin());
			case 2: if (getCount() == 0) return null; else return new Long(getMax());
			case 3: if (getCount() == 0) return null; else return new Double(getAverage());
			case 4: if (getCount() == 0) return null; else return new Double(getStdDev());
			case 5: if (getCount() == 0) return null; else return new Long(getTotal());
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

	public long getTotal() {
		return total;
	}
	public long getIntervalTotal(Basics mark) {
		return total-mark.getTotal();
	}

	public long getTotalSquare() {
		return totalSquare;
	}
	public long getIntervalTotalSquare(Basics mark) {
		return totalSquare-mark.getTotalSquare();
	}

	public double getAverage() {
		if (count == 0) {
			return 0;
		}
		return (total / (double)count);
	}
	public double getIntervalAverage(Basics mark) {
		long intervalCount=getIntervalCount(mark);
		if (intervalCount==0) {
			return 0;
		}
		return getIntervalTotal(mark)/(double)(intervalCount);
	}

	public double getVariance() {
		return calculateVariance(count, total, totalSquare);
	}
	public double getIntervalVariance(Basics mark) {
		return calculateVariance(count-mark.getCount(), total-mark.getTotal(), totalSquare-mark.getTotalSquare());
	}

	public double getStdDev() {
		return Math.sqrt(getVariance());
	}

}
