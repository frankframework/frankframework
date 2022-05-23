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

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import nl.nn.adapterframework.util.EnumUtils;

public class StatsDRegistryConfigurator extends MetricsRegistryConfiguratorBase {

	private final String FLAVOR_PROPERTY="flavor";

	public StatsDRegistryConfigurator() {
		super("statsd");
	}

	@Override
	protected MeterRegistry createRegistry() {

		StatsdConfig config = new StatsdConfig() {
			@Override
			public String get(String s) {
				return getProperty(s);
			}

			@Override
			public StatsdFlavor flavor() {
				return EnumUtils.parse(StatsdFlavor.class, getProperty(FLAVOR_PROPERTY));
			}
		};
		return new StatsdMeterRegistry(config, Clock.SYSTEM);
	}
}
