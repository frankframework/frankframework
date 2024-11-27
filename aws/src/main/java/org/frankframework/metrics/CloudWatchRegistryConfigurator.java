/*
   Copyright 2022-2023 WeAreFrank!

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

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

public class CloudWatchRegistryConfigurator extends AbstractMetricsRegistryConfigurator<CloudWatchConfig> {

	private class Config extends AbstractMeterRegistryConfig implements CloudWatchConfig {}

	@Override
	protected CloudWatchConfig createConfig() {
		return new Config();
	}

	@Override
	protected MeterRegistry createRegistry(CloudWatchConfig config) {
		return new CloudWatchMeterRegistry(config, Clock.SYSTEM, CloudWatchAsyncClient.create());
	}
}
