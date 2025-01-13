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
package org.frankframework.core;

import org.springframework.context.ApplicationContextAware;

import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.statistics.HasApplicationContext;
import org.frankframework.statistics.MetricsInitializer;

/**
 * Digested FrankElements such as an {@link IPipe}/{@link ISender} or {@link IListener}.
 * These elements contain a bunch of getters, but not necessarily setters.
 * 
 * Mainly used for {@link MetricsInitializer statistics} and {@link ConfigurationWarnings}.
 * 
 * NOTE: The {@link ApplicationContextAware} interface is here for ease of use. Ideally implementers should declare it them selves,
 * but since it's almost always required. For now, this keeps things backwards compatible.
 */
public interface FrankElement extends ApplicationContextAware, HasApplicationContext, HasName {

}
