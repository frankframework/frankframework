/*
 * $Log: PercentileEstimatorBase.java,v $
 * Revision 1.5  2007-10-08 13:35:13  europe\L190409
 * changed ArrayList to List where possible
 *
 * Revision 1.4  2006/09/07 08:37:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added sample return facility
 *
 * Revision 1.3  2005/03/10 09:52:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked percentile estimation
 *
 * Revision 1.2  2005/02/17 09:52:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * javadoc update
 *
 * Revision 1.1  2005/02/02 16:32:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modular percentile estimation
 *
 */
package nl.nn.adapterframework.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Basic interpolating percentile estimator.
 * After the array has been filled with samples, the estimates do not change anymore.
 * 
 * Interpolated values do not change anymore once the array gets filled completely.
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class PercentileEstimatorBase implements PercentileEstimator {

	protected long values[];

	private int p[];

	public PercentileEstimatorBase(String configKey, String defaultPList, int valueArraySize) {
		List pListBuffer = new ArrayList();
		StringTokenizer tok = AppConstants.getInstance().getTokenizer(configKey,defaultPList);
		
		while (tok.hasMoreTokens()) {
			pListBuffer.add(new Integer(Integer.parseInt(tok.nextToken())));
		}
		p = new int[pListBuffer.size()];
		values = new long[valueArraySize];
		for (int i = 0; i < pListBuffer.size(); i++) {
			p[i] = ((Integer) pListBuffer.get(i)).intValue();
		}
	}
 
	
	public void addValue(long value, long count, long min, long max) {
		if (count <= values.length) {
			storeFirstValue(value, count);
		}
	}
	
	protected void storeFirstValue(long value, long count) {
		// insert value in ordered array of first_values
		int i;
		for (i = (int) count - 1;
			i > 0 && values[i - 1] > value;
			i--) {
			values[i] = values[i - 1];
		}
		values[i] = value;		
	}
	
/*	
	protected int getVicinityCount(double target, double range, long count) {
		int result=0;
		for (int i=0; i<count; i++) {
			if (Math.abs(target-values[i])<= range) {
				result++;
			}
		}
		if (result==0) {
			return 1;
		}
		return result;
	}
*/
	protected double getInterpolatedPercentile(int p, long count) {
		if (count==0) {
			return Double.NaN;
		}
			
		int pos=((int)count*p)/50;
			
		if ((pos & 1)==0) {
			pos--;
		}
			
		if (pos<=0) {
			return values[0]; 
		}
		if (pos>=count*2-1) {
			return values[(int)count-1]; 
		}
		
		double fraction = (count*p-pos*50)/100.0;
		double result = values[(pos-1)/2]+(values[(pos+1)/2]-values[(pos-1)/2])*fraction;
	//	System.out.println("Interpolated p"+p+"="+result); 
		return result; 
	}

	public double getPercentileEstimate(int index, long count, long min, long max) {
		if (count<=values.length) { 
			return getInterpolatedPercentile(p[index],count);
		}
		return getInterpolatedPercentile(p[index],values.length);
	}

	public int getNumPercentiles() {
		return p.length;
	}

	public int getPercentage(int index) {
		return p[index];
	}


	public int getSampleCount(long count, long min, long max) {
		return values.length;
	}

	public XmlBuilder getSample(int index, long count, long min, long max) {
		XmlBuilder sample = new XmlBuilder("sample");
		sample.addAttribute("percentile",""+(100*index)/values.length);
		sample.addAttribute("value",""+values[index]);
		
		return sample;
	}



}
