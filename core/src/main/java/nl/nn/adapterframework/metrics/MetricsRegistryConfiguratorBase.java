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
import io.micrometer.core.instrument.config.MeterRegistryConfig;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CredentialFactory;

public abstract class MetricsRegistryConfiguratorBase<C extends MeterRegistryConfig> {

	public static final String METRICS_EXPORT_PROPERTY_PREFIX="management.metrics.export.";

	private AppConstants appConstants;
	private CredentialFactory credentialFactory;

	private C config;

	protected MetricsRegistryConfiguratorBase() {
		appConstants = AppConstants.getInstance();
	}

	protected String getProperty(String key) {
		String effectiveKey = METRICS_EXPORT_PROPERTY_PREFIX+key;
		String result = appConstants.get(effectiveKey);
		return result;
	}

	protected CredentialFactory getCredentialFactory() {
		return getCredentialFactory(config.prefix()+".username",config.prefix()+".password");
	}

	protected CredentialFactory getCredentialFactory(String usernameKey, String passwordKey) {
		if (credentialFactory==null) {
			credentialFactory = new CredentialFactory(getProperty(config.prefix()+".authAlias"), getProperty(usernameKey), getProperty(passwordKey));
		}
		return credentialFactory;
	}


	public void registerAt(CompositeMeterRegistry compositeRegistry) {
		config = createConfig();
		if ("true".equals(getProperty(config.prefix()+"."+"enabled"))) {
			compositeRegistry.add(createRegistry(config));
		}
	}

	protected abstract class MeterRegistryConfigBase implements MeterRegistryConfig {
		@Override
		public String get(String s) {
			return getProperty(s);
		}

		public String userName() {
			return getCredentialFactory().getUsername();
		}

		public String password() {
			return getCredentialFactory().getPassword();
		}
	}


	protected abstract C createConfig();

	protected abstract MeterRegistry createRegistry(C config);

}
