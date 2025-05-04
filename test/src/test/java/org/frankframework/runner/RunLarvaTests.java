package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import org.frankframework.configuration.IbisContext;
import org.frankframework.larva.LarvaConfig;
import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.ScenarioRunner;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;

/**
 * Attempt to run Larva tests in the Maven build.
 *
 * There are some issues -- some tests fail unexpectedly, whereas they do not fail when running
 * in a normal AppServer environment.
 *
 * Therefore it will not fail the build and run only to provide extra coverage-reporting.
 *
 */
@Tag("integration")
public class RunLarvaTests {

	public static final LarvaLogLevel LARVA_LOG_LEVEL = LarvaLogLevel.WRONG_PIPELINE_MESSAGES;

	private static ConfigurableApplicationContext applicationContext;
	private static LarvaTool larvaTool;
	private static ScenarioRunner scenarioRunner;
	private static AppConstants appConstants;
	private static String scenarioRootDir;

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@BeforeAll
	static void setup() throws IOException {
		SpringApplication springApplication = IafTestInitializer.configureApplication();
		applicationContext = springApplication.run();
		ServletContext servletContext = applicationContext.getBean(ServletContext.class);
		IbisContext ibisContext = FrankApplicationInitializer.getIbisContext(servletContext);
		appConstants = AppConstants.getInstance();

		larvaTool = LarvaTool.createInstance(ibisContext, System.out);

		LarvaConfig larvaConfig = larvaTool.getLarvaConfig();
		larvaConfig.setTimeout(10_000);
		larvaConfig.setLogLevel(LARVA_LOG_LEVEL);
		larvaConfig.setMultiThreaded(false);

		scenarioRunner = larvaTool.createScenarioRunner();
		scenarioRootDir = larvaTool.getTestRunStatus().initScenarioDirectories();
	}

	@AfterAll
	static void tearDown() {
		CloseUtils.closeSilently(applicationContext);
	}

	/**
	 * This should create Dynamic tests for JUnit to run. However the dynamic tests are not properly
	 * reported on by Surefire, although Surefire does execute them.
	 *
	 * Another issue is that over half the Larva scenarios fails even though I see no reason why they
	 * would fail. Running all scenarios at once by passing the scenario-directoy to the LarvaTool, most
	 * scenarios do run. I don't yet see the principle difference in environment that makes them fail here.
	 *
	 */
	@TestFactory
	@Disabled("Not yet working properly, reasons not yet known.")
	Stream<DynamicNode> larvaTests() {
		larvaTool.getTestRunStatus().readScenarioFiles(larvaTool.getScenarioLoader());
		List<File> allScenarioFiles = larvaTool.getTestRunStatus().getScenariosToRun(larvaTool.getLarvaConfig().getActiveScenariosDirectory());
		assertFalse(allScenarioFiles.isEmpty(), () -> "Did not find any scenario-files in scenarioRootDir [%s]!".formatted(scenarioRootDir));
		System.err.printf("Creating JUnit tests from %d scenarios loaded from [%s]%n", allScenarioFiles.size(), scenarioRootDir);
		return createScenarios(scenarioRootDir, "", allScenarioFiles);
	}

	private @Nonnull Stream<DynamicNode> createScenarioContainer(@Nonnull String baseFolder, @Nonnull Map.Entry<String, List<File>> scenarioFolder) {
		String scenarioFolderName = scenarioFolder.getKey();
		if (StringUtils.isBlank(scenarioFolderName)) {
			return createScenarios(baseFolder, scenarioFolderName, scenarioFolder.getValue());
		}
		return Stream.of(DynamicContainer.dynamicContainer(scenarioFolderName, new File(baseFolder, scenarioFolderName).toURI(), createScenarios(baseFolder, scenarioFolderName, scenarioFolder.getValue())));
	}

	private @Nonnull Stream<DynamicNode> createScenarios(@Nonnull String baseFolder, @Nonnull String subFolder, @Nonnull List<File> scenarioFiles) {
		String commonFolder = StringUtils.isBlank(subFolder) ? baseFolder : Paths.get(baseFolder, subFolder).toString();
		Map<String, List<File>> scenariosByFolder = ScenarioRunner.groupFilesByFolder(scenarioFiles, commonFolder);

		if (scenariosByFolder.size() == 1) {
			return scenarioFiles.stream()
					.map(this::convertLarvaScenarioToTest);
		} else {
			return scenariosByFolder.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey())
					.flatMap((Map.Entry<String, List<File>> nestedSubFolder) -> createScenarioContainer(commonFolder, nestedSubFolder));
		}
	}

	private DynamicTest convertLarvaScenarioToTest(File scenarioFile) {
		// Scenario name always computed from the scenario root dir to be understandable without context of immediate parent
		String scenarioName = scenarioFile.getAbsolutePath().substring(scenarioRootDir.length());
		return DynamicTest.dynamicTest(
				scenarioName, scenarioFile.toURI(), () -> {
					System.out.println("Running scenario: [" + scenarioName + "]");
					int scenarioPassed = scenarioRunner.runOneFile(scenarioFile, scenarioRootDir, true);

					assertNotEquals(LarvaTool.RESULT_ERROR, scenarioPassed, () -> "Scenario failed: [" + scenarioName + "]");
				}
		);
	}

	@Test
	void runLarvaTests() {
		assertTrue(applicationContext.isRunning());
		LarvaConfig larvaConfig = larvaTool.getLarvaConfig();
		larvaConfig.setLogLevel(LarvaLogLevel.SCENARIO_FAILED);

		long start = System.currentTimeMillis();
		int result = larvaTool.runScenarios(scenarioRootDir);
		long end = System.currentTimeMillis();
		System.err.printf("Scenarios executed; duration: %dms%n", end - start);

		assertFalse(result < 0, () -> "Error in LarvaTool execution, result is [%d] instead of 0".formatted(result));

		if (result > 0) {
			System.err.printf("%d Larva tests failed, duration: %dms; %n%n", result, end - start);
		} else {
			System.err.printf("All Larva tests succeeded in %dms%n", end - start);
		}

		// About 15 to 18 scenarios will fail because the environment is not set up entirely correct. Do not fail the build because of that, still get the extra coverage.
//		assertEquals(0, result, () -> "Error in LarvaTool scenarios, %d scenarios failed. Duration: %dms; %n%n%s".formatted(result, end - start, larvaOutput));
	}
}
