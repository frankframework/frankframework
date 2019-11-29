package nl.nn.adapterframework.larva;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.larva.test.IbisTester;
import nl.nn.adapterframework.util.AppConstants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.transform.TransformerConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class UnitTester {

	static IbisTester tester = null;
	List<File> scenarios;
	String rootDirectory;
	AppConstants appConstants;
	int waitBeforeCleanup;

	@BeforeClass
	public static void initTest() throws ConfigurationException, TransformerConfigurationException, IOException {
		System.out.println("Starting IBIS");
		tester = new IbisTester();
		tester.initTester();
		System.out.println("Setting Properties");
		System.setProperty("HelloWorld.job.active", "false");
		System.setProperty("junit.active", "true");
		System.setProperty("configurations.names", "${instance.name},NotExistingConfig");
	}

	@AfterClass
	public static void closeTest() {
		tester.getIbisContext().destroy();
	}

	UnitTester(List<File> scenarios, String rootDirectory, AppConstants appConstants, int waitBeforeCleanup) {

		System.out.println("Constructor!");
		this.scenarios = scenarios;
		this.rootDirectory = rootDirectory;
		this.appConstants = appConstants;
		this.waitBeforeCleanup = waitBeforeCleanup;
	}

	@Test
	public void doTest() throws InterruptedException {
		// Create message listener and make sure it prints to system.out
		MessageListener messageListener = new MessageListener();
		List<String> logLevels = messageListener.getLogLevels();
		try {
			messageListener.setSysOut(logLevels.get(logLevels.size() - 1), true, true);
		}catch (Exception ignored){}
		int[] results = {0, 0, 0};
		int numberOfScenarios = scenarios.size();
		// Run tester and wait for it to end
		ScenarioTester scenarioTester = new ScenarioTester(tester.getIbisContext(), messageListener, scenarios, rootDirectory, appConstants, results, waitBeforeCleanup, numberOfScenarios);
		scenarioTester.join();
		// Check none of them failed and all passed.
		assertEquals(results[TestTool.RESULT_ERROR], 0);
		assertEquals(results[TestTool.RESULT_OK] + results[TestTool.RESULT_AUTOSAVED], numberOfScenarios);
	}
}