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
package nl.nn.adapterframework.statistics.percentiles;

import io.micrometer.core.instrument.DistributionSummary;
import lombok.Setter;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Percentile estimator, based on MicroMeter DistributionSummary.
 * 
 * @author Gerrit van Brakel
 */
public class MicroMeterPercentileEstimator implements PercentileEstimator {

	private @Setter DistributionSummary distributionSummary;
	private final double[] percentiles;

	public MicroMeterPercentileEstimator(DistributionSummary distributionSummary, double[] percentiles) {
		this.distributionSummary = distributionSummary;
		this.percentiles = percentiles;
	}

	@Override
	public int getNumPercentiles() {
		return percentiles.length;
	}

	@Override
	public int getPercentage(int index) {
		return (int)Math.round(percentiles[index]*100);
	}

	@Override
	public void addValue(long value, long count, long min, long max) {
		// ignore, handled externally
	}

	@Override
	public double getPercentileEstimate(int index, long count, long min, long max) {
		return distributionSummary.percentile(percentiles[index]);
	}

	@Override
	public int getSampleCount(long count, long min, long max) {
		return 0;
	}

	@Override
	// sample is only used in dumpToXml, probably not necessary to implement
	public XmlBuilder getSample(int index, long count, long min, long max) {
		XmlBuilder sample = new XmlBuilder("sample");
//		sample.addAttribute("percentile",""+(100*index)/values.length);
//		sample.addAttribute("value",""+values[index]);

		return sample;
	}

}
