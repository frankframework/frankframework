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
package nl.nn.adapterframework.statistics.percentiles;

import nl.nn.adapterframework.util.XmlBuilder;


/**
 * @author Gerrit van Brakel
 */
public class PercentileEstimatorRanked extends PercentileEstimatorBase {

	private long[] ranks;
	private int local_count;

	public PercentileEstimatorRanked(String configKey, String defaultPList, int arraySize) {
		super(configKey,defaultPList,arraySize);
		ranks = new long[arraySize];
		// prefill the ranknumbers initially
		for (int i = 0; i < arraySize; i++) {
			ranks[i]=i+1;
		}
	}

	protected void condenseValues() {
		local_count = local_count >> 1;
		for (int i=0; i<local_count; i++) {
			values[i]=values[2*i+1];
			ranks[i]=ranks[2*i+1];
		}
//		System.out.print("after  condense: ");
//		printInternals();
	}

	protected long getInterpolatedRank(long value, long rankBefore, long rankAfter, long valueBefore, long valueAfter) {
//		System.out.println("interpolating for "+value+" rb="+rankBefore+" ra="+rankAfter+" vb="+valueBefore+" va="+valueAfter);
		long rank_range=rankAfter-rankBefore-1;
		long value_range=valueAfter-valueBefore;

		return rankBefore+1+rank_range*(value-valueBefore)/value_range;
	}

	@Override
	public synchronized void addValue(long value, long count, long min, long max) {
		if (count > 2) { // make sure min and max are set and relevant
			if (local_count >= values.length) {
				condenseValues();
			}
			// find last position that is smaller or equal than the requested
			int i;
			for (i=local_count; i>0 && value < values[i-1]; i--) {
				ranks[i]=ranks[i-1]+1;
				values[i]=values[i-1];
//				System.out.println("increasing rank for "+(i-1)+" to "+ranks[i]+", moving to "+i);
			}
			values[i]=value;
			if (value<=min) {
				values[0]=min;
				ranks[0]=2;
				min=value;
			} else if (value>=max) {
				values[local_count]=max;
				ranks[local_count] = count-1;
				max=value;
			} else {
				long rankBefore;
				long rankAfter;
				long valueBefore;
				long valueAfter;

				if (i==0) {
					rankBefore=1;
					valueBefore=min;
				} else {
					rankBefore=ranks[i-1];
					valueBefore=values[i-1];
				}
				if (i>=local_count) {
					rankAfter=count;
					valueAfter=max;
				} else {
					rankAfter=ranks[i+1];
					valueAfter=values[i+1];
				}
				ranks[i] = getInterpolatedRank(value,rankBefore,rankAfter,valueBefore,valueAfter);
//				System.out.println("seting slot "+i+" to v"+values[i]+"r"+ranks[i]);
			}
			local_count++;
		}
//		printInternals(count, min, max);
	}

	protected double getInterpolatedPercentile(int p, long count, long min, long max) {
		if (count==0) {
			return Double.NaN;
		}

		/*
		 *      min|_v_|___|_v_|_v_|___|___|max
		 * i         0       1   2
		 * p   0               |              100
		 * r     1   2   3   4   5   6   7   8<------ count=8
		 * pos 0   2   4   6   8  10  12  14  16
		 *
		 *
		 * example:
		 * p=50 => pos=8
		 *
		 */

		// find double of required rank
		long pos=count*p/50;

		// find the nearest possible rank, as all ranks correspond to the odd posititions:
		// they're in the middle of their classbox
		if ((pos & 1)==0) {
			pos--;
		}

		long rankBefore;   // greatest rank for which rank*2-1 is less or equal to pos
		long rankAfter;    // smallest rank for which rank*2+1 is larger than pos

		long valueBefore;  // value corresponing to rankBefore
		long valueAfter;   // value corresponding to rankAfter

		if (count<=2) {
			rankBefore=1;
			valueBefore=min;
			rankAfter=count;
			valueAfter=max;
		} else {
			int i;
			for (i=local_count-1; (i>=0) && (ranks[i]*2-1 > pos); i--);
			if (i>=0) {
				rankBefore=ranks[i];
				valueBefore=values[i];
			} else {
				rankBefore=1;
				valueBefore=min;
			}
			i++;
			if (i<local_count) {
				rankAfter=ranks[i];
				valueAfter=values[i];
			} else {
				rankAfter=count;
				valueAfter=max;
			}
		}
		double fraction = (rankAfter==rankBefore) ? 1.0 : (count*p-(2*rankBefore-1)*50)/(100.0*(rankAfter-rankBefore));
		double result = valueBefore+(valueAfter-valueBefore)*fraction;
	//	System.out.println("Interpolated p"+p+"="+result);
		return result;
	}

	public double getPercentileEstimate(int index,long count, long min, long max) {
//		printInternals(count,min,max);
		return getInterpolatedPercentile(getPercentage(index),count,min,max);
	}

//	public void printInternals(long count, long min, long max) {
//		System.out.print("c"+count+"/lc"+local_count+" min="+min);
//		for (int i=0; i<local_count; i++) {
//			System.out.print(" "+i+":(v"+values[i]+",r"+ranks[i]+")");
//		}
//		System.out.println(" max="+max);
//	}



	public int getSampleCount(long count, long min, long max) {
		return count<3 ? (int)count : local_count+2;
	}

	public XmlBuilder getSample(int index, long count, long min, long max) {
		long value;
		long rank;

		if (index<=0) {
			value=min;
			rank=1;
		} else {
			if (index>=local_count+1) {
				value=max;
				rank=count;
			} else {
				value=values[index-1];
				rank=ranks[index-1];
			}
		}

		XmlBuilder sample = new XmlBuilder("sample");
		sample.addAttribute("value",""+value);
		sample.addAttribute("rank",""+rank);
		sample.addAttribute("percentile",""+((100*rank)-50)/count);

		return sample;
	}

}
