/*
   Copyright 2021-2022 WeAreFrank!

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

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.util.EnumUtils;

public enum ProcessState {

	AVAILABLE("Available"),
	INPROCESS("InProcess"),
	DONE("Done"),
	ERROR("Error"),
	HOLD("Hold");

	@Getter @Setter
	private String name;

	ProcessState(String name) {
		this.name = name;
	}

	public static Set<ProcessState> getMandatoryKnownStates() {
		Set<ProcessState> knownProcessStates = new LinkedHashSet<>();
		knownProcessStates.add(AVAILABLE);
		return knownProcessStates;
	}

	public static Map<ProcessState, Set<ProcessState>> getTargetProcessStates(Set<ProcessState> knownProcessStates) {
		Map<ProcessState, Set<ProcessState>> targetProcessStates = new EnumMap<>(ProcessState.class);
		for (ProcessState state : ProcessState.values()) {
			targetProcessStates.put(state, new LinkedHashSet<>());
		}
		if (knownProcessStates.contains(ERROR)) {
			if (knownProcessStates.contains(AVAILABLE)) {
				targetProcessStates.get(ERROR).add(AVAILABLE);
			}
			if(knownProcessStates.contains(INPROCESS)) {
				targetProcessStates.get(INPROCESS).add(ERROR);
			}
			if (knownProcessStates.contains(HOLD)) {
				targetProcessStates.get(ERROR).add(HOLD);
				targetProcessStates.get(HOLD).add(ERROR);
			}
		}
		return targetProcessStates;
	}

	public static ProcessState getProcessStateFromName(String name) {
		return EnumUtils.parseFromField(ProcessState.class, "state", name, e -> e.getName());
	}

}
