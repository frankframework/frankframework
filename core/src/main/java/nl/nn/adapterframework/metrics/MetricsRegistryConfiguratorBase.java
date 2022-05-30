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
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CredentialFactory;

public abstract class MetricsRegistryConfiguratorBase {

	public static final String METRICS_EXPORT_PROPERTY_PREFIX="management.metrics.export.";

	private String registryPrefix;
	private AppConstants appConstants;
	private CredentialFactory credentialFactory;

	protected MetricsRegistryConfiguratorBase(String registryTypeKey) {
		registryPrefix = METRICS_EXPORT_PROPERTY_PREFIX + registryTypeKey+".";
		appConstants = AppConstants.getInstance();
	}

	protected String getProperty(String key) {
		return appConstants.get(registryPrefix+key);
	}

	protected CredentialFactory getCredentialFactory() {
		return getCredentialFactory("username","password");
	}

	protected CredentialFactory getCredentialFactory(String usernameKey, String passwordKey) {
		if (credentialFactory==null) {
			credentialFactory = new CredentialFactory(getProperty("authAlias"), getProperty(usernameKey), getProperty(passwordKey));
		}
		return credentialFactory;
	}


	public void registerAt(CompositeMeterRegistry compositeRegistry) {
		if ("true".equals(getProperty("enabled"))) {
			compositeRegistry.add(createRegistry());
		}
	}

	protected abstract MeterRegistry createRegistry();

}
