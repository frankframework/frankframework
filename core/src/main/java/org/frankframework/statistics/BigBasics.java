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
package org.frankframework.statistics;

import java.text.DecimalFormat;

import org.frankframework.util.XmlBuilder;

/**
 * Extension to Basics, allowing for large numbers, like message sizes.
 *
 * @author  Gerrit van Brakel
 */
public class BigBasics extends Basics {

	private static final long HALF_MAX_LONG=Long.MAX_VALUE>>1;

	protected int shift=0;

	public BigBasics() {
		super();
	}

	protected BigBasics(long count, long sum, long sumOfSquares, int shift) {
		super(count, sum, sumOfSquares);
		this.shift = shift;
	}

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

	@Override
	public void reset() {
		super.reset();
		shift=0;
	}

	@Override
	public BigBasics takeSnapshot() {
		return new BigBasics(count, sum, sumOfSquares, shift);
	}

	@Override
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

	@Override
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

	@Override
	public Object getItemValue(int index) {
		if (index==5) {
			if (getCount() == 0) return null;
			return new Double(getSum());
		}
		return super.getItemValue(index);
	}

	@Override
	public double getVariance() {
		return calculateVariance(count, sum, sumOfSquares, shift);
	}

	public int getShift() {
		return shift;
	}

	@Override
	public double getAverage() {
		if (shift==0 || count == 0) {
			return super.getAverage();
		}
		return (1L<<shift)*(sum / (double)count);
	}

	@Override
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
	@Override
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
	@Override
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

	@Override
	public double getIntervalVariance(Basics mark) {
		return calculateVariance(count-mark.getCount(), getIntervalSum(mark), getIntervalSumOfSquares(mark), shift);
	}


	@Override
	protected XmlBuilder toXml(String elementName, String name, DecimalFormat timeFormat, DecimalFormat percentageFormat) {
		// TODO Auto-generated method stub
		return super.toXml(elementName, name, timeFormat, percentageFormat);
	}
}
