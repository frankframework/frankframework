/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.adapterframework.util;

/**
 * Enumeration of states for IManagable
 * @author Gerrit van Brakel
 */
public enum RunStateEnum {

	ERROR("**ERROR**"),
	STARTING("Starting"),
	STARTED("Started"),
	STOPPING("Stopping"),
	STOPPED("Stopped");

	private final String stateDescriptor;

	private RunStateEnum(String stateDescriptor) {
		this.stateDescriptor = stateDescriptor;
	}

	public boolean isState(String state) {
		if(state == null) return false;

		return this.equals(valueOf(state.toUpperCase()));
	}

	public String getName() {
		return stateDescriptor;
	}
}
