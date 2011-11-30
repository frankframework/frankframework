/*
 * $Log: BigBasics.java,v $
 * Revision 1.3  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2011/08/22 14:31:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for size statistics
 *
 */
package nl.nn.adapterframework.statistics;

import java.text.DecimalFormat;

import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Extension to Basics, allowing for large numbers, like message sizes.
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class BigBasics extends Basics {

	private final static long HALF_MAX_LONG=Long.MAX_VALUE>>1;
	
	protected int shift=0;
	
	public void checkSizes() {
		if (sumOfSquares > HALF_MAX_LONG) {
			shiftRight();
		}
	}
	
	public void shiftRight() {
		shift++;
		sum >>= 1;
		sumOfSquares >>= 2;
	}
	
	public void reset() {
		super.reset();
		shift=0;
	}

	public void mark(Basics other) {
		super.mark(other);
		if (other instanceof BigBasics) {
			shift=((BigBasics)other).shift;
		} else {
			shift=0;
		}
	}

	protected void addSums(long value) {
		//checkSizes();
		if (value>0) {
			long shiftedValue = value >> shift;
			long newSum;
			while ((newSum = sum+shiftedValue)<0) {
				shiftRight();
				shiftedValue >>= 1;
			}
			sum=newSum;
			if (value <= Integer.MAX_VALUE) {
				long squaredShifted=(value * value) >> (2 * shift);
				long newSumOfSquares;
				while ((newSumOfSquares = sumOfSquares+squaredShifted)<0) {
					shiftRight();
					squaredShifted >>= 2;
				}
				sumOfSquares = newSumOfSquares;
			} else {
				while (shiftedValue > Integer.MAX_VALUE) {
					shiftedValue >>= 1;
					shiftRight();
				}
				long squaredShifted=shiftedValue * shiftedValue;
				long newSumOfSquares;
				while ((newSumOfSquares = sumOfSquares+squaredShifted)<0) {
					shiftRight();
					squaredShifted >>= 2;
				}
				sumOfSquares = newSumOfSquares;
			}
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
		checkSizes();
		if (record instanceof BigBasics) {
			BigBasics bigRecord=(BigBasics)record;
			bigRecord.checkSizes();
			while (shift<bigRecord.getShift()) {
				shiftRight();
			}
			int shiftDif=shift-bigRecord.getShift();
			if (shiftDif>0) {
				sum += record.getSum() >> shiftDif;
				sumOfSquares += record.getSumOfSquares() >> (2 * shiftDif);
			} else {
				sum += record.getSum();
				sumOfSquares += record.getSumOfSquares();
			}
		} else {
			sum += record.getSum() >> shift;
			sumOfSquares += record.getSumOfSquares() >> (2 * shift);
		}
	}

	private double calculateVariance(long count, long sum, long sumOfSquares, int shift) {
		double result;
		if (count>1) {
			if (sum>Integer.MAX_VALUE) {
				double dsum=sum;
				result=(sumOfSquares-((dsum*dsum)/count))/(count-1);
			} else {
				result=(sumOfSquares-((sum*sum)/count))/(count-1);
			}
			if (shift>0) {
				result=result*(1L<<(2*shift));
			}
		} else {
			result=Double.NaN;
		}
		return result;
	}
	
	public Object getItemValue(int index) {
		if (index==5) {
			if (getCount() == 0) return null; 
			return new Double(getSum());
		}
		return super.getItemValue(index);
	}

	public double getVariance() {
		return calculateVariance(count, sum, sumOfSquares, shift);
	}

	public int getShift() {
		return shift;
	}

	
	public double getAverage() {
		if (shift==0 || count == 0) {
			return super.getAverage();
		}
		return (1L<<shift)*(sum / (double)count);
	}

	public double getIntervalAverage(Basics mark) {
		long intervalCount=getIntervalCount(mark);
		if (intervalCount==0) {
			return 0;
		}
		return (1L<<shift)*(getIntervalSum(mark)/(double)(intervalCount));
	}

	/*
	 * Result is shifted. 
	 */
	public long getIntervalSum(Basics mark) {
		long markSum=mark.getSum();
		if (mark instanceof BigBasics) {
			BigBasics bbmark = (BigBasics)mark;
			int markShift=bbmark.getShift();
			if (markShift>getShift()) {
				throw new RuntimeException("Cannot have mark shift ["+markShift+"] further than base ["+getShift()+"]");
			}
			markSum>>=getShift()-markShift;
		} else {
			markSum>>=getShift();
		}
		return getSum()-markSum;
	}

	/*
	 * Result is shifted. 
	 */
	public long getIntervalSumOfSquares(Basics mark) {
		long markSumOfSquares=mark.getSumOfSquares();
		if (mark instanceof BigBasics) {
			BigBasics bbmark = (BigBasics)mark;
			int markShift=bbmark.getShift();
			if (markShift>getShift()) {
				throw new RuntimeException("Cannot have mark shift ["+markShift+"] further than base ["+getShift()+"]");
			}
			markSumOfSquares>>=2*(getShift()-markShift);
		} else {
			markSumOfSquares>>=2*getShift();
		}
		return getSumOfSquares()-markSumOfSquares;
	}

	
	public double getIntervalVariance(Basics mark) {
		return calculateVariance(count-mark.getCount(), getIntervalSum(mark), getIntervalSumOfSquares(mark), shift);
	}


	protected XmlBuilder toXml(String elementName, String name, DecimalFormat timeFormat, DecimalFormat percentageFormat) {
		// TODO Auto-generated method stub
		return super.toXml(elementName, name, timeFormat, percentageFormat);
	}

	
}
