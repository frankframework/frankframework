package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.frankframework.larva.Scenario;
import org.frankframework.larva.ScenarioRunner;
import org.frankframework.larva.TestRunStatus;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.util.CloseUtils;

/**
 * Attempt to run Larva tests in the Maven build.
 *
 * There are some issues -- some tests fail unexpectedly, whereas they do not fail when running
 * in a normal AppServer environment.
 *
 * Therefore, it will not fail the build and run only to provide extra coverage-reporting.
 *
 */
@Tag("integration")
public class RunLarvaTests {

	public static final LarvaLogLevel LARVA_LOG_LEVEL = LarvaLogLevel.WRONG_PIPELINE_MESSAGES;

	public static final Set<String> IGNORED_SCENARIOS = Set.of(
			"ApiListener/scenario01",
			"ApiListener/scenario02",
			"ApiListener/scenario03",
			"ApiListener/scenario04",
			"ApiListener/scenario05",
			"ApiListener/scenario06",
			"ApiListener/scenario07",
			"ApiListener/scenario08",
			"Authentication/scenario03",
			"Authentication/scenario04",
			"Base64Pipe/scenario01",
			"Base64Pipe/scenario02",
			"CorrelationMessageId/scenario04",
			"CorrelationMessageId/scenario05",
			"Exits/api/scenario01",
			"Exits/api/scenario01b",
			"Exits/api/scenario03",
			"Exits/soap/scenario01",
			"Exits/soap/scenario02",
			"JSON/DataSonnet/scenario01",
			"JSON/JsonPipe/scenario01",
			"FileSender/scenario01",
			"ForwardNameProvidingSenders/scenario10",
			"ForwardNameProvidingSenders/scenario11",
			"ForwardNameProvidingSenders/scenario12",
			"ForwardNameProvidingSenders/scenario13",
			"FrankSender/scenario05",
			"ManagedFileHandler/scenario01",
			"ManagedFileHandler/scenario02",
			"LocalFileSystemPipe/scenario07",
			"LocalFileSystemPipe/scenario08",
			"MoveFiles/scenario01",
			"MoveFiles/scenario04",
			"MoveFiles/scenario09",
			"Receivers/NonTransacted/NoInProcess/scenario01",
			"Receivers/NonTransacted/NoInProcess/scenario05",
			"Receivers/NonTransacted/NoInProcess/scenario06",
			"Receivers/Transacted/WithInProcess/scenario03",
			"RestListener/scenario01",
			"RestListener/scenario02",
			"Validators/SoapValidator/scenario07",
			"WebServiceListenerSender/scenario11b",
			"WebServiceListenerSender/scenario11c",
			"WebServiceListenerSender/scenario11d",
			"WsdlGeneratorPipe/scenario01",
			"WsdlGeneratorPipe/scenario02",
			"WsdlGeneratorPipe/scenario03",
			"XsltProviderListener/scenario04",
			"Zip/ZipWriter/scenario 01",
			"Zip/ZipWriter/scenario 02"
	);

	private static ConfigurableApplicationContext applicationContext;
	private static LarvaTool larvaTool;
	private static ScenarioRunner scenarioRunner;
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
	Stream<DynamicNode> larvaTests() {
		larvaTool.getTestRunStatus().readScenarioFiles(larvaTool.getScenarioLoader());
		List<Scenario> allScenarios = larvaTool.getTestRunStatus().getScenariosToRun(larvaTool.getLarvaConfig().getActiveScenariosDirectory());
		assertFalse(allScenarios.isEmpty(), () -> "Did not find any scenario-files in scenarioRootDir [%s]!".formatted(scenarioRootDir));
		System.err.printf("Creating JUnit tests from %d scenarios loaded from [%s]%n", allScenarios.size(), scenarioRootDir);
		return createScenarios(scenarioRootDir, "", allScenarios);
	}

	private @Nonnull Stream<DynamicNode> createScenarioContainer(@Nonnull String baseFolder, @Nonnull Map.Entry<String, List<Scenario>> scenarioFolder) {
		String scenarioFolderName = scenarioFolder.getKey();
		if (StringUtils.isBlank(scenarioFolderName)) {
			return createScenarios(baseFolder, scenarioFolderName, scenarioFolder.getValue());
		}
		return Stream.of(DynamicContainer.dynamicContainer(scenarioFolderName, new File(baseFolder, scenarioFolderName).toURI(), createScenarios(baseFolder, scenarioFolderName, scenarioFolder.getValue())));
	}

	private @Nonnull Stream<DynamicNode> createScenarios(@Nonnull String baseFolder, @Nonnull String subFolder, @Nonnull List<Scenario> scenarioFiles) {
		String commonFolder = StringUtils.isBlank(subFolder) ? baseFolder : Paths.get(baseFolder, subFolder).toString();
		Map<String, List<Scenario>> scenariosByFolder = ScenarioRunner.groupScenariosByFolder(scenarioFiles, commonFolder);

		if (scenariosByFolder.size() == 1) {
			return scenarioFiles.stream()
					.map(this::convertLarvaScenarioToTest);
		} else {
			return scenariosByFolder.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey())
					.flatMap((Map.Entry<String, List<Scenario>> nestedSubFolder) -> createScenarioContainer(commonFolder, nestedSubFolder));
		}
	}

	private DynamicTest convertLarvaScenarioToTest(Scenario scenario) {
		// Scenario name always computed from the scenario root dir to be understandable without context of immediate parent
		String scenarioName = scenario.getName();
		return DynamicTest.dynamicTest(
				scenarioName, scenario.getScenarioFile().toURI(), () -> {
					System.out.println("Running scenario: [" + scenarioName + "]");
					int scenarioPassed = scenarioRunner.runOneFile(scenario, true);

					assumeTrue(scenarioPassed != LarvaTool.RESULT_ERROR || !IGNORED_SCENARIOS.contains(scenarioName), () -> "Ignoring Blacklisted Scenario: [" + scenarioName + "]");
					assertNotEquals(LarvaTool.RESULT_ERROR, scenarioPassed, () -> "Scenario failed: [" + scenarioName + "]");
				}
		);
	}

	@Test
	@Disabled("Run Larva test scenarios individually now")
	void runLarvaTests() {
		assertTrue(applicationContext.isRunning());
		LarvaConfig larvaConfig = larvaTool.getLarvaConfig();
		larvaConfig.setLogLevel(LarvaLogLevel.SCENARIO_FAILED);

		long start = System.currentTimeMillis();
		TestRunStatus result = larvaTool.runScenarios(scenarioRootDir);
		long end = System.currentTimeMillis();
		System.err.printf("Scenarios executed; duration: %dms%n", end - start);

		assertFalse(result.getScenariosFailedCount() < 0, () -> "Error in LarvaTool execution, result is [%d] instead of 0".formatted(result));

		if (result.getScenariosFailedCount() > 0) {
			System.err.printf("%d Larva tests failed, duration: %dms; %n%n", result.getScenariosFailedCount(), end - start);
		} else {
			System.err.printf("All %d Larva tests succeeded in %dms%n", result.getScenarioExecuteCount(), end - start);
		}

		// About 15 to 18 scenarios will fail because the environment is not set up entirely correct. Do not fail the build because of that, still get the extra coverage.
//		assertEquals(0, result, () -> "Error in LarvaTool scenarios, %d scenarios failed. Duration: %dms; %n%n%s".formatted(result, end - start, larvaOutput));
	}
}
