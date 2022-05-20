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

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

public class CloudWatchRegistryConfigurator extends MetricsRegistryConfiguratorBase {

	private final String NAMESPACE_PROPERTY="namespace";
	
	public CloudWatchRegistryConfigurator() {
		super("cloudwatch");
	}
	
	@Override
	protected MeterRegistry createRegistry() {
	
		CloudWatchConfig cloudWatchConfig = new CloudWatchConfig() {
			@Override
			public String get(String s) {
				return getProperty(s);
			}
	
			@Override
			public String namespace() {
				return getProperty(NAMESPACE_PROPERTY);
			}
		};
		return new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, CloudWatchAsyncClient.create());
	}
}
