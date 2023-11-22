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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Basic interpolating percentile estimator.
 * After the array has been filled with samples, the estimates do not change anymore.
 * 
 * Interpolated values do not change anymore once the array gets filled completely.
 * 
 * @author Gerrit van Brakel
 */
public class PercentileEstimatorBase implements PercentileEstimator {

	protected long[] values;
	private final int[] p;

	public PercentileEstimatorBase(String configKey, String defaultPList, int valueArraySize) {
		List pListBuffer = new ArrayList();
		StringTokenizer tok = AppConstants.getInstance().getTokenizedProperty(configKey,defaultPList);

		while (tok.hasMoreTokens()) {
			pListBuffer.add(new Integer(Integer.parseInt(tok.nextToken())));
		}
		p = new int[pListBuffer.size()];
		values = new long[valueArraySize];
		for (int i = 0; i < pListBuffer.size(); i++) {
			p[i] = ((Integer) pListBuffer.get(i)).intValue();
		}
	}

	@Override
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

	@Override
	public double getPercentileEstimate(int index, long count, long min, long max) {
		if (count<=values.length) {
			return getInterpolatedPercentile(p[index],count);
		}
		return getInterpolatedPercentile(p[index],values.length);
	}

	@Override
	public int getNumPercentiles() {
		return p.length;
	}

	@Override
	public int getPercentage(int index) {
		return p[index];
	}


	@Override
	public int getSampleCount(long count, long min, long max) {
		return values.length;
	}

	@Override
	public XmlBuilder getSample(int index, long count, long min, long max) {
		XmlBuilder sample = new XmlBuilder("sample");
		sample.addAttribute("percentile",""+(100*index)/values.length);
		sample.addAttribute("value",""+values[index]);

		return sample;
	}

}
