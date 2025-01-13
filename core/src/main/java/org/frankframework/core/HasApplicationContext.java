/*
   Copyright 2013 Nationale-Nederlanden, 2022-2025 WeAreFrank!

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
package org.frankframework.core;

import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.statistics.MetricsInitializer;

/**
 * Beans that have an {@link ApplicationContext}.
 * Mainly used for {@link ConfigurationWarnings} and {@link MetricsInitializer Metrics}.
 */
public interface HasApplicationContext extends IScopeProvider {

	/*
	 * Allows the statistic to be grouped by Configuration and Adapter.
	 * Or ConfigurationWarnings to be published on the correct Context.
	 */
	ApplicationContext getApplicationContext();

	@Override
	default ClassLoader getConfigurationClassLoader() {
		return getApplicationContext().getClassLoader();
	}
}
