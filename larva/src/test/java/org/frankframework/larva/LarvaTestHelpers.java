package org.frankframework.larva;

import java.io.File;

import org.jspecify.annotations.NonNull;

public class LarvaTestHelpers {
	public static @NonNull File getFileFromResource(String scenarioFileName) {
		return new File(ScenarioLoaderTest.class.getResource(scenarioFileName).getFile());
	}
}
