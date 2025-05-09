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

import jakarta.annotation.Nullable;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public enum LarvaLogLevel {
	DEBUG("debug", 1),
	PIPELINE_MESSAGES_PREPARED_FOR_DIFF("pipeline messages prepared for diff", 2),
	PIPELINE_MESSAGES("pipeline messages", 3),
	WRONG_PIPELINE_MESSAGES_PREPARED_FOR_DIFF("wrong pipeline messages prepared for diff", 4),
	WRONG_PIPELINE_MESSAGES("wrong pipeline messages", 5),
	STEP_PASSED_FAILED("step passed/failed", 6),
	SCENARIO_PASSED_FAILED("scenario passed/failed", 7),
	SCENARIO_FAILED("scenario failed", 8),
	TOTALS("totals", 9),
	ERROR("error", 10);

	@Getter private final String name;
	private final int index;

	LarvaLogLevel(String name, int index) {
		this.name = name;
		this.index = index;
	}

	public static LarvaLogLevel parse(String value, LarvaLogLevel defaultValue) {
		for (LarvaLogLevel level : values()) {
			if (level.name.equalsIgnoreCase(value)) {
				return level;
			}
		}
		log.warn("Unknown log level found: [{}], using default value instead.", value);
		return defaultValue;
	}

	public boolean shouldLog(@Nullable LarvaLogLevel other) {
		return other == null || this.index <= other.index;
	}
}
