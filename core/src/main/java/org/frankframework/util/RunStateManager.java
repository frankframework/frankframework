/*
   Copyright 2013 Nationale-Nederlanden

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
package org.frankframework.util;

import org.apache.logging.log4j.Logger;

/**
 * Utility class to support run-state management.
 *
 * @author Gerrit van Brakel
 */
public class RunStateManager implements RunStateEnquirer {
	protected Logger log = LogUtil.getLogger(this);

	private RunState runState = RunState.STOPPED;

	@Override
	public synchronized RunState getRunState() {
		return runState;
	}

	public synchronized void setRunState(RunState newRunState) {
		if(runState != newRunState) {
			if(log.isDebugEnabled())
				log.debug("Runstate [" + this + "] set from " + runState + " to " + newRunState);

			runState = newRunState;
			notifyAll();
		}
	}

	@Override
	public String toString() {
		return super.toString() + " [" + runState + "]";
	}
}
