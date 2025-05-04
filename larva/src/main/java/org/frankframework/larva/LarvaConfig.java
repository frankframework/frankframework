/*
   Copyright 2025 WeAreFrank!

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
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.AppConstants;

@Log4j2
public class LarvaConfig {
	static final int GLOBAL_TIMEOUT_MILLIS = AppConstants.getInstance().getInt("larva.timeout", 10_000);

	private @Getter @Setter int timeout = GLOBAL_TIMEOUT_MILLIS;
	private @Getter @Setter int waitBeforeCleanup = 100;
	private @Getter @Setter boolean multiThreaded = false;
	private @Getter @Setter LarvaLogLevel logLevel = LarvaLogLevel.WRONG_PIPELINE_MESSAGES;
	private @Getter @Setter boolean autoSaveDiffs = AppConstants.getInstance().getBoolean("larva.diffs.autosave", false);

	/**
	 * if allowReadlineSteps is set to true, actual results can be compared in line by using .readline steps.
	 * Those results cannot be saved to the expected value defined inline, however.
	 */
	private @Getter @Setter boolean allowReadlineSteps = false;

	private @Getter @Setter String activeScenariosDirectory;
}
