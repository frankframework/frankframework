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

import lombok.extern.log4j.Log4j2;

/**
 * Utility class to support run-state management.
 *
 * @author Gerrit van Brakel
 */
@Log4j2
public class RunStateManager implements RunStateEnquirer {

	private RunState runState = RunState.STOPPED;

	@Override
	public synchronized RunState getRunState() {
		return runState;
	}

	public synchronized void setRunState(RunState newRunState) {
		if(runState != newRunState) {
			log.debug("Runstate [{}] set from {} to {}", super.toString(), runState, newRunState);

			runState = newRunState;
			notifyAll();
		}
	}

	@Override
	public String toString() {
		return super.toString() + " [" + runState + "]";
	}
}
