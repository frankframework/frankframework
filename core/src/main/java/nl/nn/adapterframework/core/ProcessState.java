/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.core;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public enum ProcessState {

	AVAILABLE,
	INPROCESS,
	DONE,
	ERROR,
	HOLD;

	public static Set<ProcessState> getMandatoryKnownStates() {
		Set<ProcessState> knownProcessStates = new LinkedHashSet<>();
		knownProcessStates.add(AVAILABLE);
		return knownProcessStates;
	}
	
	public static Map<ProcessState,Set<ProcessState>> getTargetProcessStates(Set<ProcessState> knownProcessStates) {
		Map<ProcessState,Set<ProcessState>> targetProcessStates = new HashMap<>();
		for (ProcessState state:ProcessState.values()) {
			targetProcessStates.put(state, new LinkedHashSet<>());
		}
		if (knownProcessStates.contains(ERROR)) {
			if (knownProcessStates.contains(HOLD)) {
				targetProcessStates.get(ERROR).add(HOLD);
				targetProcessStates.get(HOLD).add(ERROR);
			}
		}
		return targetProcessStates;
	}
}
