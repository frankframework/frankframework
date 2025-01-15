/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.lifecycle;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.IConfigurable;

/**
 * Interface for Spring beans that require their {@link Lifecycle} to be managed by Spring.
 * 
 * See {@link ConfiguringLifecycleProcessor}.
 *
 * @author Niels
 */
public interface ConfigurableLifecycle extends SmartLifecycle, IConfigurable {

	@Override
	default int getPhase() {
		return Integer.MAX_VALUE; // Starts later, stops first
	}

	/**
	 * By default these beans are not started.
	 * The {@link Configuration} may do so in the {@link Configuration#configure()} method.
	 */
	@Override
	default boolean isAutoStartup() {
		return false;
	}
}
