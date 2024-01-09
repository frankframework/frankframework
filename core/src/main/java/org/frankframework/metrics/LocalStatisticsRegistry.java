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
package org.frankframework.metrics;

import java.time.Duration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramGauges;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class LocalStatisticsRegistry extends SimpleMeterRegistry {

	public LocalStatisticsRegistry(SimpleConfig config) {
		super(config, Clock.SYSTEM);
	}

	@Override
	protected DistributionSummary newDistributionSummary(Id id, DistributionStatisticConfig distributionStatisticConfig, double scale) {
		DistributionStatisticConfig merged = distributionStatisticConfig.merge(DistributionStatisticConfig.builder().expiry(Duration.ofHours(1)).build());
		DistributionSummary summary = new LocalDistributionSummary(id, clock, merged, scale, false);

		HistogramGauges.registerWithCommonFormat(summary, this);

		return summary;
	}
}
