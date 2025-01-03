/*
   Copyright 2021 WeAreFrank!

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
import org.springframework.context.ApplicationContextAware;

/**
 * We need to migrate away from this interface as it doesn't add anything anymore and is abused for it's many getters.
 * Classes that implement this for ConfigurationWarnings should publish events through a {@link ApplicationEventPublisher}.
 * Those that actually need the {@link Configuration} class need to be rewritten to use {@link ConfigurationAware}.
 * 
 * This interface has mostly been used to retrieve or create beans using a Spring context, as the Adapter context
 * doesn't hold any bean references yet, for now it's fine to use either.
 */
@Deprecated
public interface IConfigurationAware extends IScopeProvider, ApplicationContextAware {

	String getName();
	ApplicationContext getApplicationContext();
}
