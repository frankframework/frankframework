package org.frankframework.larva;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.SortedMap;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.util.AppConstants;

class TestRunStatusTest {
	private AppConstants appConstants;
	private TestRunStatus testRunStatus;
	private ScenarioLoader scenarioLoader;
	private LarvaConfig config;
	private File scenarioRoot1;

	@BeforeEach
	public void setUp() {
		appConstants = AppConstants.getInstance();
		appConstants.setProperty("active.via.appconstants", "true");

		config = new LarvaConfig();
		LarvaWriter out = new LarvaWriter(config, System.out);
		scenarioLoader = new ScenarioLoader(out);
		testRunStatus = new TestRunStatus(config, out);

		scenarioRoot1 = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios");
		appConstants.setProperty("scenariosroot1.directory", scenarioRoot1.getAbsolutePath());
		appConstants.setProperty("scenariosroot1.description", "Test Scenarios Root Directory");

		// 2nd root is via relative path
		appConstants.setProperty("scenariosroot2.directory", "test-classes/scenario-test-data/alt-scenario-root");
		appConstants.setProperty("scenariosroot2.description", "Alternative Test Scenarios Root Directory");

		// Entry without a description should be ignored.
		File scenarioRoot3 = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios-root-should-be-ignored");
		appConstants.setProperty("scenariosroot3.directory", scenarioRoot3.getAbsolutePath());

		// Entry should be ignored because the target directory does not exist.
		appConstants.setProperty("scenariosroot4.directory", "/invalid-scenarios-root");
		appConstants.setProperty("scenariosroot4.description", "Invalid Scenarios Root Directory");

		// Duplicated directory should be ignored.
		appConstants.setProperty("scenariosroot5.directory", scenarioRoot1.getAbsolutePath());
		appConstants.setProperty("scenariosroot5.description", "Duplicate entry should be ignored");

		// Duplicated description should be ignored.
		appConstants.setProperty("scenariosroot6.directory", "/duplicated-scenarios-description");
		appConstants.setProperty("scenariosroot6.description", "Test Scenarios Root Directory");

		// TODO: Add test for scenariosroot with m2e file.
	}

	@AfterEach
	public void tearDown() {
		appConstants.remove("active.via.appconstants");
		appConstants.remove("scenariosroot.default");

		appConstants.remove("scenariosroot1.directory");
		appConstants.remove("scenariosroot1.description");
		appConstants.remove("scenariosroot2.directory");
		appConstants.remove("scenariosroot2.description");
		appConstants.remove("scenariosroot3.directory");
		appConstants.remove("scenariosroot4.directory");
		appConstants.remove("scenariosroot4.description");
		appConstants.remove("scenariosroot5.directory");
		appConstants.remove("scenariosroot5.description");
		appConstants.remove("scenariosroot6.directory");
		appConstants.remove("scenariosroot6.description");
	}

	@Test
	void initScenarioDirectoriesActiveRootNotSetInConfig() {
		// Act
		String scenarioInitResult = testRunStatus.initScenarioDirectories();

		// Assert
		SortedMap<String, String> scenarioDirectories = testRunStatus.getScenarioDirectories();
		assertEquals(3, scenarioDirectories.size());
		assertEquals("Alternative Test Scenarios Root Directory", scenarioDirectories.firstKey());
		assertThat(scenarioDirectories.get(scenarioDirectories.lastKey()), CoreMatchers.endsWith(File.separator + "invalid-scenarios-root" + File.separator));

		assertEquals(config.getActiveScenariosDirectory(), scenarioInitResult);
		assertThat(scenarioInitResult, CoreMatchers.endsWith(File.separator + "alt-scenario-root" + File.separator));
	}


	@Test
	void initScenarioDirectoriesActiveRootSetInAppConstants() {
		// Arrange
		appConstants.setProperty("scenariosroot.default", "Test Scenarios Root Directory");

		// Act
		String scenarioInitResult = testRunStatus.initScenarioDirectories();

		// Assert
		SortedMap<String, String> scenarioDirectories = testRunStatus.getScenarioDirectories();
		assertEquals(3, scenarioDirectories.size());
		assertEquals("Alternative Test Scenarios Root Directory", scenarioDirectories.firstKey());
		assertThat(scenarioDirectories.get(scenarioDirectories.lastKey()), CoreMatchers.endsWith(File.separator + "invalid-scenarios-root" + File.separator));

		assertEquals(config.getActiveScenariosDirectory(), scenarioInitResult);
		assertThat(scenarioInitResult, CoreMatchers.endsWith(File.separator + "scenarios" + File.separator));
	}

	@Test
	void initScenarioDirectoriesActiveRootSetInConfig() {
		// Arrange
		config.setActiveScenariosDirectory(scenarioRoot1.getAbsolutePath());

		// Act
		String scenarioInitResult = testRunStatus.initScenarioDirectories();

		// Assert
		SortedMap<String, String> scenarioDirectories = testRunStatus.getScenarioDirectories();
		assertEquals(3, scenarioDirectories.size());
		assertEquals("Alternative Test Scenarios Root Directory", scenarioDirectories.firstKey());
		assertThat(scenarioDirectories.get(scenarioDirectories.lastKey()), CoreMatchers.endsWith(File.separator + "invalid-scenarios-root" + File.separator));

		assertEquals(config.getActiveScenariosDirectory(), scenarioInitResult);
	}

	@Test
	void getScenariosToRunAllScenarios() {
		// Arrange
		config.setActiveScenariosDirectory(scenarioRoot1.getAbsolutePath());
		testRunStatus.initScenarioDirectories();
		testRunStatus.readScenarioFiles(scenarioLoader);

		// Act
		List<Scenario> scenariosToRun = testRunStatus.getScenariosToRun(config.getActiveScenariosDirectory());

		// Assert
		assertEquals(2, scenariosToRun.size());
		assertEquals(2, testRunStatus.getScenarioExecuteCount());
		assertEquals(2, testRunStatus.getAllScenarios().size());
	}

	@Test
	void getScenariosToRunSubsetOfScenarios() {
		// Arrange
		config.setActiveScenariosDirectory(scenarioRoot1.getAbsolutePath());
		testRunStatus.initScenarioDirectories();
		testRunStatus.readScenarioFiles(scenarioLoader);
		String scenariosDirectory = Paths.get(config.getActiveScenariosDirectory(), "scenariodir1", "subdir").toString();

		// Act
		List<Scenario> scenariosToRun = testRunStatus.getScenariosToRun(scenariosDirectory);

		// Assert
		assertEquals(1, scenariosToRun.size());
		assertEquals(1, testRunStatus.getScenarioExecuteCount());
		assertEquals(2, testRunStatus.getAllScenarios().size());
	}

	@Test
	void getScenariosToRunSingleScenario() {
		// Arrange
		config.setActiveScenariosDirectory(scenarioRoot1.getAbsolutePath());
		testRunStatus.initScenarioDirectories();
		testRunStatus.readScenarioFiles(scenarioLoader);
		String scenariosDirectory = Paths.get(config.getActiveScenariosDirectory(), "scenariodir1", "subdir", "scenario01.properties").toString();

		// Act
		List<Scenario> scenariosToRun = testRunStatus.getScenariosToRun(scenariosDirectory);

		// Assert
		assertEquals(1, scenariosToRun.size());
		assertEquals(1, testRunStatus.getScenarioExecuteCount());
		assertEquals(2, testRunStatus.getAllScenarios().size());
	}
}
