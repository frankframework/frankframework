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
