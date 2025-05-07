package org.frankframework.larva;

import java.io.File;

import jakarta.annotation.Nonnull;

public class LarvaTestHelpers {
	public static @Nonnull File getFileFromResource(String scenarioFileName) {
		return new File(ScenarioLoaderTest.class.getResource(scenarioFileName).getFile());
	}
}
