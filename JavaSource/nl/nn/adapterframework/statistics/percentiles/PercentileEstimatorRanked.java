/*
 * $Log: PercentileEstimatorRanked.java,v $
 * Revision 1.3  2011-11-30 13:52:01  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/12/29 14:25:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved statistics to separate package
 *
 * Revision 1.5  2009/01/08 16:43:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in getSample()
 *
 * Revision 1.4  2008/08/27 16:25:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added clear()
 *
 * Revision 1.3  2006/09/07 08:37:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added sample return facility
 *
 * Revision 1.2  2005/04/06 13:08:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved by using extrapolation for extreme percentiles
 *
 * Revision 1.1  2005/03/10 09:52:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked percentile estimation
 *
 * Revision 1.1  2005/02/24 12:24:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version, extracted from global Ibis
 *
 */
package nl.nn.adapterframework.statistics.percentiles;

import nl.nn.adapterframework.util.XmlBuilder;


/**  
 * @author Gerrit van Brakel
 * @version Id
 */
public class PercentileEstimatorRanked extends PercentileEstimatorBase {

	private long ranks[];
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

    public void printInternals(long count, long min, long max) {
    	System.out.print("c"+count+"/lc"+local_count+" min="+min);
		for (int i=0; i<local_count; i++) {
			System.out.print(" "+i+":(v"+values[i]+",r"+ranks[i]+")");
		}
		System.out.println(" max="+max);
    }	



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

	public void clear(){
		super.clear();
		local_count=0;
	}

    	
}
