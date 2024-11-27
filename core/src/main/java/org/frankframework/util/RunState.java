/*
   Copyright 2013 Nationale-Nederlanden, 2021 - 2024 WeAreFrank!

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

/**
 * Enumeration of states for IManagable
 * @author Gerrit van Brakel
 */
public enum RunState {

	ERROR,
	STARTING,
	EXCEPTION_STARTING,
	STARTED,
	STOPPING,
	EXCEPTION_STOPPING,
	STOPPED;

	public boolean isStopped() {
		switch (this) {
			case STARTING:
			case EXCEPTION_STARTING:
			case STARTED:
			case STOPPING:
				return false;
			case STOPPED:
			case EXCEPTION_STOPPING:
			case ERROR:
				return true;
			default:
				throw new IllegalStateException("Unhandled receiver run-state [" + this + "]");
		}
	}
}
