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

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;

public class InfluxRegistryConfigurator extends AbstractMetricsRegistryConfigurator<InfluxConfig> {

	private class Config extends AbstractMeterRegistryConfig implements InfluxConfig {}

	@Override
	protected InfluxConfig createConfig() {
		return new Config();
	}

	@Override
	protected MeterRegistry createRegistry(InfluxConfig config) {
		return new InfluxMeterRegistry(config, Clock.SYSTEM);
	}

}
