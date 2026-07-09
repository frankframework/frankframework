/*
   Copyright 2022-2026 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import org.frankframework.aws.AwsUtil;

public class CloudWatchRegistryConfigurator extends AbstractMetricsRegistryConfigurator<CloudWatchConfig> {
	private static final Region DEFAULT_REGION = Region.EU_WEST_1;

	private class Config extends AbstractMeterRegistryConfig implements CloudWatchConfig {}

	@Override
	protected CloudWatchConfig createConfig() {
		return new Config();
	}

	@Override
	protected MeterRegistry createRegistry(CloudWatchConfig config) {
		AwsCredentialsProvider credentialProvider = AwsUtil.createCredentialProviderChain(getCredentialFactory());

		String regionValue = getProperty(config.prefix()+"."+"region"); // management.metrics.export.cloudwatch.region
		Region region = StringUtils.isNotBlank(regionValue) ? Region.of(regionValue) : DEFAULT_REGION;

		CloudWatchAsyncClient client = CloudWatchAsyncClient.builder()
				.credentialsProvider(credentialProvider)
				.region(region)
				.build();
		return new CloudWatchMeterRegistry(config, Clock.SYSTEM, client);
	}
}
