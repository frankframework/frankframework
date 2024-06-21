/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.larva;

import lombok.Getter;

public enum LarvaLogLevel {
	DEBUG("debug"),
	PIPELINE_MESSAGES_PREPARED_FOR_DIFF("pipeline messages prepared for diff"),
	PIPELINE_MESSAGES("pipeline messages"),
	WRONG_PIPELINE_MESSAGES_PREPARED_FOR_DIFF("wrong pipeline messages prepared for diff"),
	WRONG_PIPELINE_MESSAGES("wrong pipeline messages"),
	STEP_PASSED_FAILED("step passed/failed"),
	SCENARIO_PASSED_FAILED("scenario passed/failed"),
	SCENARIO_FAILED("scenario failed"),
	TOTALS("totals"),
	ERROR("error");

	@Getter final private String name;

	LarvaLogLevel(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean shouldLog(LarvaLogLevel other) {
		return this.ordinal() <= other.ordinal();
	}

	public static LarvaLogLevel parse(String value) {
		for (LarvaLogLevel level : values()) {
			if (level.getName().equals(value)) {
				return level;
			}
		}
		throw new IllegalArgumentException("Unknown log level: " + value);
	}
}
