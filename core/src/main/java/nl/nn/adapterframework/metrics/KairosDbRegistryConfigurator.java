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
import io.micrometer.kairos.KairosConfig;
import io.micrometer.kairos.KairosMeterRegistry;

public class KairosDbRegistryConfigurator extends MetricsRegistryConfiguratorBase {

	public KairosDbRegistryConfigurator() {
		super("kairos");
	}

	@Override
	protected MeterRegistry createRegistry() {

		KairosConfig kairosConfig = new KairosConfig() {
			@Override
			public String get(String s) {
				return getProperty(s);
			}

			@Override
			public String userName() {
				return getCredentialFactory().getUsername();
			}

			@Override
			public String password() {
				return getCredentialFactory().getPassword();
			}
		};
		return new KairosMeterRegistry(kairosConfig, Clock.SYSTEM);
	}
}
