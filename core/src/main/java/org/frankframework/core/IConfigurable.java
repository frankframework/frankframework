/*
   Copyright 2020-2025 WeAreFrank!

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

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.lifecycle.ConfigurableLifecycle;

/**
 * To be replaced with {@link ConfigurableLifecycle}.
 */
public interface IConfigurable {

	/**
	 * Configure this component.
	 * <p>In the case of a container, this will propagate the configure signal to all
	 * components that apply.</p>
	 * @throws ConfigurationException in case it was not able to configure the component.
	 */
	void configure() throws ConfigurationException;
}
