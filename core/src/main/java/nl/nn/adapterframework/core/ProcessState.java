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
package nl.nn.adapterframework.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public enum ProcessState {

	AVAILABLE("Available", "fa-server", "success"),
	INPROCESS("InProcess", "fa-share", "success"),
	DONE("Done", "fa-envelope-o", "success"),
	ERROR("Error", "fa-times-circle", "danger"),
	HOLD("Hold", "fa-pause-circle", "warning");

	String name=null;
	String iconName=null;
	String type=null;

	private ProcessState(String name, String iconName, String type) {
		this.name=name;
		this.iconName = iconName;
		this.type = type;
	}
	
	public static Set<ProcessState> getMandatoryKnownStates() {
		Set<ProcessState> knownProcessStates = new LinkedHashSet<>();
		knownProcessStates.add(AVAILABLE);
		return knownProcessStates;
	}

	public static Map<ProcessState, Set<ProcessState>> getTargetProcessStates(Set<ProcessState> knownProcessStates) {
		Map<ProcessState, Set<ProcessState>> targetProcessStates = new LinkedHashMap<>();
		for (ProcessState state : ProcessState.values()) {
			targetProcessStates.put(state, new LinkedHashSet<>());
		}
		if (knownProcessStates.contains(ERROR)) {
			if (knownProcessStates.contains(AVAILABLE)) {
				targetProcessStates.get(ERROR).add(AVAILABLE);
			}
			if (knownProcessStates.contains(HOLD)) {
				targetProcessStates.get(ERROR).add(HOLD);
				targetProcessStates.get(HOLD).add(ERROR);
			}
		}
		return targetProcessStates;
	}

	public static ProcessState getProcessStateFromName(String name) {
		ProcessState[] processStates = ProcessState.values();
		for (ProcessState processState : processStates) {
			if(StringUtils.equalsIgnoreCase(processState.getName(), name)) {
				return processState;
			}
		}
		return null;
	}

	public void setIconName(String iconName) {
		this.iconName = iconName;
	}
	public String getIconName() {
		return iconName;
	}

	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
}
