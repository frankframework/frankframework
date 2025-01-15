/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import org.springframework.context.SmartLifecycle;

import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.util.RunState;

/**
 * Models starting and stopping of objects that support such behaviour.
 * Inherit's all {@link SmartLifecycle} methods and is {@link IConfigurable}.
 */
public interface ManagableLifecycle extends ConfigurableLifecycle {

	/**
	 * Returns the RunState of the object.
	 */
	RunState getRunState();

	/**
	 * Check whether this component has successfully been configured.
	 * Similar to {@link #isRunning()}, verifies if this object may be started.
	 */
	boolean isConfigured();

	/**
	 * Verifies if this object needs to be started or stopped.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	default boolean isRunning() {
		return getRunState() == RunState.STARTED;
	}
}
