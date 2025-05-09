package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScenarioTest {

	private LarvaConfig config;

	@BeforeEach
	void setUp() {
		config = new LarvaConfig();
	}

	@Test
	void getSteps1() throws IOException {
		// Arrange
		String scenarioSteps = """
				action.className=org.frankframework.test.TestAction
				
				step1.action.read=in.txt
				step2.action.write=out.txt
				step3.action.writeline=outline
				step4.action.readline=inline
				step5.action.read=in.txt
				
				ignore.action.read=ignore.txt
				""";
		Scenario scenario = createScenario(scenarioSteps);
		config.setAllowReadlineSteps(false);

		// Act
		List<String> steps = scenario.getSteps(config);

		// Assert
		assertEquals(3, steps.size());
		assertEquals("step1.action.read", steps.get(0));
		assertEquals("step2.action.write", steps.get(1));
		assertEquals("step3.action.writeline", steps.get(2));

		// Arrange
		config.setAllowReadlineSteps(true);

		// Act
		List<String> steps2 = scenario.getSteps(config);

		// Assert
		assertEquals(5, steps2.size());
		assertEquals("step1.action.read", steps2.get(0));
		assertEquals("step2.action.write", steps2.get(1));
		assertEquals("step3.action.writeline", steps2.get(2));
		assertEquals("step4.action.readline", steps2.get(3));
		assertEquals("step5.action.read", steps2.get(4));
	}

	@Test
	void getSteps2() throws IOException {
		String scenarioSteps = """
						step1.action.read=in.txt
						step1.action.write=out.txt
				""";
		// Arrange
		Scenario scenario = createScenario(scenarioSteps);

		// Act
		assertThrows(LarvaException.class, () -> scenario.getSteps(config) );
	}

	@Test
	void getSteps3() throws IOException {
		// Arrange
		// Steps cannot currently use numbers starting with 0 so no steps returned for this scenario
		String scenarioSteps = """
						step01.action.read=in.txt
						step02.action.write=out.txt
						step03.action.writeline=outline
						step04.action.readline=inline
				""";
		Scenario scenario = createScenario(scenarioSteps);

		// Act
		List<String> steps = scenario.getSteps(config);

		// Assert
		assertEquals(0, steps.size());
	}

	@Test
	void getSteps4() throws IOException {
		// Arrange
		// Steps not in order should be returned sorted
		String scenarioSteps = """
				action.className=org.frankframework.test.TestAction
				
				step5.action.read=in.txt
				step2.action.write=out.txt
				step3.action.writeline=outline
				step1.action.read=in.txt
				
				ignore.action.read=ignore.txt
				step4.action.read=in.txt
				""";
		Scenario scenario = createScenario(scenarioSteps);

		// Act
		List<String> steps = scenario.getSteps(config);

		// Assert
		assertEquals(5, steps.size());
		assertEquals("step1.action.read", steps.get(0));
		assertEquals("step2.action.write", steps.get(1));
		assertEquals("step3.action.writeline", steps.get(2));
		assertEquals("step4.action.read", steps.get(3));
		assertEquals("step5.action.read", steps.get(4));
	}

	@Test
	void getSteps5() throws IOException {
		// Arrange
		// Step numbers cannot be skipped
		String scenarioSteps = """
				action.className=org.frankframework.test.TestAction
				
				step1.action.read=in.txt
				step2.action.write=out.txt
				step3.action.writeline=outline
				step5.action.read=in.txt
				
				ignore.action.read=ignore.txt
				""";
		Scenario scenario = createScenario(scenarioSteps);

		// Act
		List<String> steps = scenario.getSteps(config);

		// Assert
		assertEquals(3, steps.size());
		assertEquals("step1.action.read", steps.get(0));
		assertEquals("step2.action.write", steps.get(1));
		assertEquals("step3.action.writeline", steps.get(2));
	}

	private Scenario createScenario(String scenarioSteps) throws IOException {
		Properties scenarioProperties = new Properties();
		scenarioProperties.load(new StringReader(scenarioSteps));
		return new Scenario(new File("dummy.properties"), "test", "test", scenarioProperties);
	}
}
