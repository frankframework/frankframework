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
package nl.nn.adapterframework.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;
import nl.nn.adapterframework.util.AppConstants;

/**
 * Set management.metrics.use-global-registry=false to disable
 * 
 * @see <a href="https://docs.spring.io/spring-boot/docs/2.1.9.RELEASE/reference/html/production-ready-metrics.html">Spring Metrics</a>
 */
public class FrankRegistryConfigurator extends MetricsRegistryConfiguratorBase {
	private boolean enabled = AppConstants.getInstance().getBoolean("management.metrics.use-global-registry", true);

	public FrankRegistryConfigurator() {
		super("global-registry");
	}

	@Override
	protected MeterRegistry createRegistry() {

		SimpleConfig defaultConfig = new SimpleConfig() {
			@Override
			public String get(String s) {
				return getProperty(s);
			}

		};
		return new FrankStatisticsRegistry(defaultConfig);
	}

	@Override
	public void registerAt(CompositeMeterRegistry compositeRegistry) {
		if (enabled && !"false".equals(getProperty("enabled"))) {
			compositeRegistry.add(createRegistry());
		}
	}
}
