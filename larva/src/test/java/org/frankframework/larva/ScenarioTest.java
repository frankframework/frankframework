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
	void getStepsWithReadlineStep() throws IOException {
		// Arrange
		// The "L" in readLine / writeLine can now be capitalized optionally.
		String scenarioSteps = """
				action.className=org.frankframework.test.TestAction
				
				step1.action.read=in.txt
				step2.action.write=out.txt
				step3.action.writeline=outline
				step4.action.readline=inline
				step5.action.read=in.txt
				step6.action.writeLine=outline
				step7.action.readLine=inline
				
				ignore.action.read=ignore.txt
				""";
		Scenario scenario = createScenario(scenarioSteps);
		config.setAllowReadlineSteps(false);

		// Act
		List<Step> steps = scenario.getSteps(config);

		// Assert
		assertEquals(5, steps.size());
		assertEquals("step1.action.read", steps.get(0).getBaseKey());
		assertEquals("step2.action.write", steps.get(1).getBaseKey());
		assertEquals("step3.action.writeline", steps.get(2).getBaseKey());
		assertEquals("step5.action.read", steps.get(3).getBaseKey());
		assertEquals("step6.action.writeLine", steps.get(4).getBaseKey());

		// Arrange
		config.setAllowReadlineSteps(true);

		// Act
		List<Step> steps2 = scenario.getSteps(config);

		// Assert
		assertEquals(7, steps2.size());
		assertEquals("step1.action.read", steps2.get(0).getBaseKey());
		assertEquals("step2.action.write", steps2.get(1).getBaseKey());
		assertEquals("step3.action.writeline", steps2.get(2).getBaseKey());
		assertEquals("step4.action.readline", steps2.get(3).getBaseKey());
		assertEquals("step5.action.read", steps2.get(4).getBaseKey());
		assertEquals("step6.action.writeLine", steps2.get(5).getBaseKey());
		assertEquals("step7.action.readLine", steps2.get(6).getBaseKey());
	}

	@Test
	void getStepsDoNotAllowDuplicateStepNumbers() throws IOException {
		String scenarioSteps = """
						step1.action.read=in.txt
						step01.action.write=out.txt
				""";
		// Arrange
		Scenario scenario = createScenario(scenarioSteps);

		// Act
		assertThrows(LarvaException.class, () -> scenario.getSteps(config) );
	}

	@Test
	void getStepsStepNrsPrefixedWith0() throws IOException {
		// Arrange
		// Steps can now use numbers starting with 0 so all 6 steps returned for this scenario
		String scenarioSteps = """
						step01.action.read=in.txt
						step02.action.write=out.txt
						step03.action.writeline=outline
						step04.action.readline=inline
				""";
		Scenario scenario = createScenario(scenarioSteps);
		config.setAllowReadlineSteps(false);

		// Act
		List<Step> steps = scenario.getSteps(config);

		// Assert
		assertEquals(3, steps.size());
	}

	@Test
	void getStepsSortStepsNotInOrder() throws IOException {
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
		List<Step> steps = scenario.getSteps(config);

		// Assert
		assertEquals(5, steps.size());
		assertEquals("step1.action.read", steps.get(0).getBaseKey());
		assertEquals("step2.action.write", steps.get(1).getBaseKey());
		assertEquals("step3.action.writeline", steps.get(2).getBaseKey());
		assertEquals("step4.action.read", steps.get(3).getBaseKey());
		assertEquals("step5.action.read", steps.get(4).getBaseKey());
	}

	@Test
	void getStepsAllowSkippingStepNrs() throws IOException {
		// Arrange
		// Step numbers can now be skipped, so step5 is now picked up as a valid step
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
		List<Step> steps = scenario.getSteps(config);

		// Assert
		assertEquals(4, steps.size());
		assertEquals("step1.action.read", steps.get(0).getBaseKey());
		assertEquals("step2.action.write", steps.get(1).getBaseKey());
		assertEquals("step3.action.writeline", steps.get(2).getBaseKey());
		assertEquals("step5.action.read", steps.get(3).getBaseKey());
	}

	private Scenario createScenario(String scenarioSteps) throws IOException {
		Properties scenarioProperties = new Properties();
		scenarioProperties.load(new StringReader(scenarioSteps));
		return new Scenario(new File("dummy.properties"), "test", "test", scenarioProperties);
	}
}
