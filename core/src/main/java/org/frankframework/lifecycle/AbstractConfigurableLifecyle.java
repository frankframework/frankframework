/*
   Copyright 2021 - 2024 WeAreFrank!

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

import org.apache.logging.log4j.Logger;

import org.frankframework.util.LogUtil;
import org.frankframework.util.RunState;

/**
 * Base class which looks at the {@link RunState} of the component before changing it's {@link ConfigurableLifecycle Lifecycle} state.
 * <br/>
 * Calling configure() should set the state to STARTING.
 * Calling start() should set the state to STARTED.
 */
public abstract class AbstractConfigurableLifecyle implements ConfigurableLifecycle {
	protected final Logger log = LogUtil.getLogger(this);
	private RunState state = RunState.STOPPED;

	public RunState getState() {
		return state;
	}

	protected void updateState(RunState state) {
		this.state = state;
	}

	public boolean inState(RunState state) {
		return getState() == state;
	}

	@Override
	public boolean isRunning() {
		return inState(RunState.STARTED);
	}
}
