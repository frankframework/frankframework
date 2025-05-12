package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.util.AppConstants;

public class ScenarioLoaderTest {

	private AppConstants appConstants;
	private ScenarioLoader scenarioLoader;

	@BeforeEach
	public void setUp() {
		LarvaConfig config = new LarvaConfig();
		LarvaWriter out = new LarvaWriter(config, System.out);
		LarvaTool larvaTool = new LarvaTool(null, config, out, null);

		scenarioLoader = new ScenarioLoader(larvaTool);
		appConstants = AppConstants.getInstance();
		appConstants.setProperty("active.via.appconstants", "true");
	}

	@AfterEach
	public void tearDown() {
		appConstants.remove("active.via.appconstants");
	}

	@Test
	public void testLoadActiveScenario() {
		File scenarioFile = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios/scenariodir1/active-scenario.properties");

		Properties scenarioProperties = scenarioLoader.readScenarioProperties(scenarioFile, appConstants);

		assertNotNull(scenarioProperties);
		assertEquals("true", scenarioProperties.getProperty("scenario.active"));
		assertEquals("org.frankframework.senders.EchoSender", scenarioProperties.getProperty("test.className"));
	}

	@Test
	public void testLoadInactiveScenario() {
		File scenarioFile = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios/scenariodir2/inactive-scenario.properties");

		Properties scenarioProperties = scenarioLoader.readScenarioProperties(scenarioFile, appConstants);

		assertNotNull(scenarioProperties);
		assertEquals("false", scenarioProperties.getProperty("scenario.active"));
	}

	@Test
	public  void testLoadScenarioDirectory() {
		String scenarioDirectory = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios").getAbsolutePath();

		Map<Scenario.ID, Scenario> scenarioMap = scenarioLoader.readScenarioFiles(scenarioDirectory);

		assertEquals(3, scenarioMap.size());
		File scenarioFile1 = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios/scenariodir1/active-scenario.properties");
		Scenario.ID scenarioId1 = new Scenario.ID(scenarioFile1);
		assertTrue(scenarioMap.containsKey(scenarioId1));
		assertFalse(scenarioMap.containsKey(new Scenario.ID(LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios/scenariodir2/inactive-scenario.properties"))));

		Scenario scenario1 = scenarioMap.get(scenarioId1);
		assertEquals("Active Scenario via variable resolved in include", scenario1.getDescription());
		assertEquals(scenarioFile1, scenario1.getScenarioFile());
		assertEquals("scenariodir1/active-scenario", scenario1.getName());
		assertEquals(scenarioFile1.getAbsolutePath(), scenario1.getLongName());
	}
}
