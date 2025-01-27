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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;

/**
 * Set management.metrics.export.local=false to disable
 *
 * @see <a href="https://docs.spring.io/spring-boot/docs/2.1.9.RELEASE/reference/html/production-ready-metrics.html">Spring Metrics</a>
 */
public class LocalRegistryConfigurator extends AbstractMetricsRegistryConfigurator<SimpleConfig> {

	private class Config extends AbstractMeterRegistryConfig implements SimpleConfig {
		@Override
		public String prefix() {
			return "local";
		}
	}

	@Override
	protected SimpleConfig createConfig() {
		return new Config();
	}

	@Override
	protected MeterRegistry createRegistry(SimpleConfig config) {
		return new LocalStatisticsRegistry(config);
	}
}
