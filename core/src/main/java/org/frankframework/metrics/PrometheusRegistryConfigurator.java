/*
   Copyright 2022 - 2024 WeAreFrank!

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

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

public class PrometheusRegistryConfigurator extends AbstractMetricsRegistryConfigurator<PrometheusConfig> {

	private class Config extends AbstractMeterRegistryConfig implements PrometheusConfig {}

	@Override
	protected PrometheusConfig createConfig() {
		return new Config();
	}

	@Override
	protected MeterRegistry createRegistry(PrometheusConfig config) {
		return new PrometheusMeterRegistry(config) {

			@Override
			public DistributionSummary newDistributionSummary(Id id, DistributionStatisticConfig config, double scale) {
				return super.newDistributionSummary(id, overrideDefaults(config), scale);
			}
		};
	}

	/**
	 * Since whoever made the Prometheus registry doesn't want anyone to use ServiceLevelObjects, we have to always disable publishing them.
	 * It's strange that Prometheus disables SLO but does NOT default percentilesHistogram to false.
	 * 
	 * See https://github.com/micrometer-metrics/micrometer/issues/4854
	 */
	private DistributionStatisticConfig overrideDefaults(DistributionStatisticConfig config) {
		return DistributionStatisticConfig.builder()
				.expiry(Duration.ofDays(7))
				.percentilesHistogram(false)
				.build()
				.merge(config);
	}
}
