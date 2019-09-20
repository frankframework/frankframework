/**
 * 
 */
package nl.nn.adapterframework.testtool;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.testtool.controller.FileController;
import nl.nn.adapterframework.testtool.controller.JdbcFixedQueryController;
import nl.nn.adapterframework.testtool.controller.JmsController;
import nl.nn.adapterframework.testtool.controller.ListenerController;
import nl.nn.adapterframework.testtool.controller.SenderController;
import nl.nn.adapterframework.testtool.controller.WebServiceController;
import nl.nn.adapterframework.util.AppConstants;

/**
 * @author Murat Kaan Meral
 *
 */
public class ScenarioTester extends Thread {

	private static final String STEP_SYNCHRONIZER = "Step synchronizer";
	private List<File> scenarioFile;
	private String currentScenariosRootDirectory;
	private AppConstants appConstants;
	private int waitBeforeCleanUp;
	private int scenariosTotal;
	private int[] scenarioResults;
	
	@Autowired
	static IbisContext ibisContext;
	
	public ScenarioTester(List<File> scenarioFile, String currentScenariosRootDirectory, AppConstants appConstants, int[] scenarioResults, int waitBeforeCleanUp, int scenariosTotal) {
		this.scenarioFile = scenarioFile;
		this.currentScenariosRootDirectory = currentScenariosRootDirectory;
		this.appConstants = appConstants;
		this.waitBeforeCleanUp = waitBeforeCleanUp;
		this.scenariosTotal = scenariosTotal;
		this.scenarioResults = scenarioResults;
	}
	
	@Override
	public void run() {
		for(File scenario : scenarioFile) {
			testScenario(scenario);
		}
	}

	public void testScenario(File scenarioFile) {
		int scenarioPassed = TestTool.RESULT_ERROR;
		String scenarioDirectory = scenarioFile.getParentFile().getAbsolutePath() + File.separator;
		String longName = scenarioFile.getAbsolutePath();
		String shortName = longName.substring(currentScenariosRootDirectory.length() - 1, longName.length() - ".properties".length());
		
		
		MessageListener.debugMessage("Read property file " + scenarioFile.getName());
		Properties properties = TestPreparer.readProperties(appConstants, scenarioFile);
		List<String> steps = null;

		if (properties != null) {
			MessageListener.debugMessage("Read steps from property file");
			steps = TestPreparer.getSteps(properties);
			if (steps != null) {
				synchronized(STEP_SYNCHRONIZER) {
					MessageListener.debugMessage("Open queues");
					Map<String, Map<String, Object>> queues = openQueues(scenarioDirectory, steps, properties, ibisContext, appConstants);
					if (queues != null) {
						MessageListener.debugMessage("Execute steps");
						boolean allStepsPassed = true;
						boolean autoSaved = false;
						Iterator<String> iterator = steps.iterator();
						
						
						// Execute Steps
						while (allStepsPassed && iterator.hasNext()) {
							String step = (String)iterator.next();
							String stepDisplayName = shortName + " - " + step + " - " + properties.get(step);
							MessageListener.debugMessage("Execute step '" + stepDisplayName + "'");
							int stepPassed = executeStep(step, properties, stepDisplayName, queues);
							if (stepPassed==TestTool.RESULT_OK) {
								MessageListener.stepMessage("Step '" + stepDisplayName + "' passed");
							} else if (stepPassed==TestTool.RESULT_AUTOSAVED) {
								MessageListener.stepMessage("Step '" + stepDisplayName + "' passed after autosave");
								autoSaved = true;
							} else {
								MessageListener.stepMessage("Step '" + stepDisplayName + "' failed");
								allStepsPassed = false;
							}
						}
						
						
						
						if (allStepsPassed) {
							if (autoSaved) {
								scenarioPassed = TestTool.RESULT_AUTOSAVED;
							} else {
								scenarioPassed = TestTool.RESULT_OK;
							}
						}
						MessageListener.debugMessage("Wait " + waitBeforeCleanUp + " ms before clean up");
						try {
							Thread.sleep(waitBeforeCleanUp);
						} catch(InterruptedException e) {
						}
						MessageListener.debugMessage("Close queues");
						boolean remainingMessagesFound = closeQueues(queues, properties);
						if (remainingMessagesFound) {
							MessageListener.stepMessage("Found one or more messages on queues or in database after scenario executed");
							scenarioPassed = TestTool.RESULT_ERROR;
						}
					}
				}
			}
		}
		
		// Reporting of the result
		synchronized (scenarioResults) {
			scenarioResults[scenarioPassed]++;
			switch (scenarioPassed) {
			case TestTool.RESULT_OK:
				MessageListener.scenarioMessage("Scenario '" + shortName + " - "
						+ properties.getProperty("scenario.description") + "' passed (" + scenarioResults[0] + "/"
						+ scenarioResults[1] + "/" + scenariosTotal + ")");
				break;
			case TestTool.RESULT_AUTOSAVED:
				MessageListener.scenarioMessage("Scenario '" + shortName + " - "
						+ properties.getProperty("scenario.description") + "' passed after autosave");
				break;
			case TestTool.RESULT_ERROR:
				MessageListener.scenarioFailedMessage("Scenario '" + shortName + " - "
						+ properties.getProperty("scenario.description") + "' failed (" + scenarioResults[0] + "/"
						+ scenarioResults[1] + "/" + scenariosTotal + ")");
				break;
			default:
				MessageListener.errorMessage("Could not retrieve the result of the test " + shortName);
				break;
			}
		}
	}
	/*
	public static List<File> readScenarioFiles(AppConstants appConstants, String scenariosDirectory) {
		List<File> scenarioFiles = new ArrayList<File>();
		MessageListener.debugMessage("List all files in directory '" + scenariosDirectory + "'");
		File[] files = new File(scenariosDirectory).listFiles();
		if (files == null) {
			MessageListener.debugMessage("Could not read files from directory '" + scenariosDirectory + "'");
		} else {
			MessageListener.debugMessage("Sort files");
			Arrays.sort(files);
			MessageListener.debugMessage("Filter out property files containing a 'scenario.description' property");
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.getName().endsWith(".properties")) {
					Properties properties = TestPreparer.readProperties(appConstants, file);
					if (properties != null && properties.get("scenario.description") != null) {
						String active = properties.getProperty("scenario.active", "true");
						String unstable = properties.getProperty("adapter.unstable", "false");
						if (active.equalsIgnoreCase("true") && unstable.equalsIgnoreCase("false")) {
							scenarioFiles.add(file);
						}
					}
				} else if (file.isDirectory() && (!file.getName().equals("CVS"))) {
					scenarioFiles.addAll(readScenarioFiles(appConstants, file.getAbsolutePath()));
				}
			}
		}
		MessageListener.debugMessage(scenarioFiles.size() + " scenario files found");
		return scenarioFiles;
	}
	*/
	
	public static int executeStep(String step, Properties properties, String stepDisplayName, Map<String, Map<String, Object>> queues) {
		int stepPassed = TestTool.RESULT_ERROR;
		String fileName = properties.getProperty(step);
		String fileNameAbsolutePath = properties.getProperty(step + ".absolutepath");
		int i = step.indexOf('.');
		String queueName;
		String fileContent;
		// vul globale var
		String zeefVijlNeem = fileNameAbsolutePath;
		
		//inlezen file voor deze stap
		if ("".equals(fileName)) {
			MessageListener.errorMessage("No file specified for step '" + step + "'");
		} else {
			MessageListener.debugMessage("Read file " + fileName);
			fileContent = TestPreparer.readFile(fileNameAbsolutePath);
			if (fileContent == null) {
				MessageListener.errorMessage("Could not read file '" + fileName + "'");
			} else {
				if (step.endsWith(".read")) {
					queueName = step.substring(i + 1, step.length() - 5);

					if ("nl.nn.adapterframework.jms.JmsListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = JmsController.read(step, stepDisplayName, properties, queues, queueName, fileName, fileContent);	
					} else 	if ("nl.nn.adapterframework.jdbc.FixedQuerySender".equals(properties.get(queueName + ".className"))) {
						stepPassed = JdbcFixedQueryController.read(step, stepDisplayName, properties, queues, queueName, fileName, fileContent);
					} else if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = SenderController.executeSenderRead(step, stepDisplayName, properties, queues, queueName, "ibisWebService", fileName, fileContent);
					} else if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = SenderController.executeSenderRead(step, stepDisplayName, properties, queues, queueName, "webService", fileName, fileContent);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = WebServiceController.read(step, stepDisplayName, properties, queues, queueName, fileName, fileContent);
					} else if ("nl.nn.adapterframework.http.HttpSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = SenderController.executeSenderRead(step, stepDisplayName, properties, queues, queueName, "http", fileName, fileContent);
					} else if ("nl.nn.adapterframework.senders.IbisJavaSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = SenderController.executeSenderRead(step, stepDisplayName, properties, queues, queueName, "ibisJava", fileName, fileContent);
					} else if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = WebServiceController.read(step, stepDisplayName, properties, queues, queueName, fileName, fileContent);
					} else if ("nl.nn.adapterframework.testtool.FileListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = FileController.executeListenerRead(step, stepDisplayName, properties, queues, queueName, fileName, fileContent);
					} else if ("nl.nn.adapterframework.testtool.FileSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = FileController.executeSenderRead(step, stepDisplayName, properties, queues, queueName, fileName, fileContent);
					} else if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = ListenerController.executeXsltProviderListenerRead(stepDisplayName, properties, queues, queueName, fileContent, TestPreparer.createParametersMapFromParamProperties(properties, step, false, null));
					} else {
						MessageListener.errorMessage("Property '" + queueName + ".className' not found or not valid");
					}
				} else {
					queueName = step.substring(i + 1, step.length() - 6);
					Runnable a;
					

					if ("nl.nn.adapterframework.jms.JmsSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = JmsController.write(stepDisplayName, queues, queueName, fileContent);
					} else if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = SenderController.executeSenderWrite(stepDisplayName, queues, queueName, "ibisWebService", fileContent);
					} else if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = SenderController.executeSenderWrite(stepDisplayName, queues, queueName, "webService", fileContent);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = WebServiceController.write(stepDisplayName, queues, queueName, fileContent);
					} else if ("nl.nn.adapterframework.http.HttpSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = SenderController.executeSenderWrite(stepDisplayName, queues, queueName, "http", fileContent);
					} else if ("nl.nn.adapterframework.senders.IbisJavaSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = SenderController.executeSenderWrite(stepDisplayName, queues, queueName, "ibisJava", fileContent);
					} else if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = WebServiceController.write(stepDisplayName, queues, queueName, fileContent);
					} else if ("nl.nn.adapterframework.testtool.FileSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = FileController.executeSenderWrite(stepDisplayName, queues, queueName, fileContent);
					} else if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = ListenerController.executeXsltProviderListenerWrite(step, stepDisplayName, queues, queueName, fileName, fileContent, properties);
					} else if ("nl.nn.adapterframework.senders.DelaySender".equals(properties.get(queueName + ".className"))) {
						stepPassed = SenderController.executeDelaySenderWrite(stepDisplayName, queues, queueName, fileContent);
					} else {
						MessageListener.errorMessage("Property '" + queueName + ".className' not found or not valid");
					}
				}
			}
		}

		return stepPassed;
	}


	public static Map<String, Map<String, Object>> openQueues(String scenarioDirectory, List<String> steps,
			Properties properties, IbisContext ibisContext, AppConstants appConstants) {
		Map<String, Map<String, Object>> queues = new HashMap<String, Map<String, Object>>();
		MessageListener.debugMessage("Get all queue names");
		List<String> jmsSenders = new ArrayList<String>();
		List<String> jmsListeners = new ArrayList<String>();
		List<String> jdbcFixedQuerySenders = new ArrayList<String>();
		List<String> ibisWebServiceSenders = new ArrayList<String>();
		List<String> webServiceSenders = new ArrayList<String>();
		List<String> webServiceListeners = new ArrayList<String>();
		List<String> httpSenders = new ArrayList<String>();
		List<String> ibisJavaSenders = new ArrayList<String>();
		List<String> delaySenders = new ArrayList<String>();
		List<String> javaListeners = new ArrayList<String>();
		List<String> fileSenders = new ArrayList<String>();
		List<String> fileListeners = new ArrayList<String>();
		List<String> xsltProviderListeners = new ArrayList<String>();

		Iterator iterator = properties.keySet().iterator();
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			int i = key.indexOf('.');
			if (i != -1) {
				int j = key.indexOf('.', i + 1);
				if (j != -1) {
					String queueName = key.substring(0, j);
					MessageListener.debugMessage("queuename openqueue: " + queueName);
					if ("nl.nn.adapterframework.jms.JmsSender".equals(properties.get(queueName + ".className"))
							&& !jmsSenders.contains(queueName)) {
						MessageListener.debugMessage("Adding jmsSender queue: " + queueName);
						jmsSenders.add(queueName);
					} else if ("nl.nn.adapterframework.jms.JmsListener".equals(properties.get(queueName + ".className"))
							&& !jmsListeners.contains(queueName)) {
						MessageListener.debugMessage("Adding jmsListener queue: " + queueName);
						jmsListeners.add(queueName);
					} else if ("nl.nn.adapterframework.jdbc.FixedQuerySender".equals(
							properties.get(queueName + ".className")) && !jdbcFixedQuerySenders.contains(queueName)) {
						MessageListener.debugMessage("Adding jdbcFixedQuerySender queue: " + queueName);
						jdbcFixedQuerySenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(
							properties.get(queueName + ".className")) && !ibisWebServiceSenders.contains(queueName)) {
						MessageListener.debugMessage("Adding ibisWebServiceSender queue: " + queueName);
						ibisWebServiceSenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.WebServiceSender".equals(
							properties.get(queueName + ".className")) && !webServiceSenders.contains(queueName)) {
						MessageListener.debugMessage("Adding webServiceSender queue: " + queueName);
						webServiceSenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(
							properties.get(queueName + ".className")) && !webServiceListeners.contains(queueName)) {
						MessageListener.debugMessage("Adding webServiceListener queue: " + queueName);
						webServiceListeners.add(queueName);
					} else if ("nl.nn.adapterframework.http.HttpSender".equals(properties.get(queueName + ".className"))
							&& !httpSenders.contains(queueName)) {
						MessageListener.debugMessage("Adding httpSender queue: " + queueName);
						httpSenders.add(queueName);
					} else if ("nl.nn.adapterframework.senders.IbisJavaSender"
							.equals(properties.get(queueName + ".className")) && !ibisJavaSenders.contains(queueName)) {
						MessageListener.debugMessage("Adding ibisJavaSender queue: " + queueName);
						ibisJavaSenders.add(queueName);
					} else if ("nl.nn.adapterframework.senders.DelaySender"
							.equals(properties.get(queueName + ".className")) && !delaySenders.contains(queueName)) {
						MessageListener.debugMessage("Adding delaySender queue: " + queueName);
						delaySenders.add(queueName);
					} else if ("nl.nn.adapterframework.receivers.JavaListener"
							.equals(properties.get(queueName + ".className")) && !javaListeners.contains(queueName)) {
						MessageListener.debugMessage("Adding javaListener queue: " + queueName);
						javaListeners.add(queueName);
					} else if ("nl.nn.adapterframework.testtool.FileSender"
							.equals(properties.get(queueName + ".className")) && !fileSenders.contains(queueName)) {
						MessageListener.debugMessage("Adding fileSender queue: " + queueName);
						fileSenders.add(queueName);
					} else if ("nl.nn.adapterframework.testtool.FileListener"
							.equals(properties.get(queueName + ".className")) && !fileListeners.contains(queueName)) {
						MessageListener.debugMessage("Adding fileListener queue: " + queueName);
						fileListeners.add(queueName);
					} else if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(
							properties.get(queueName + ".className")) && !xsltProviderListeners.contains(queueName)) {
						MessageListener.debugMessage("Adding xsltProviderListeners queue: " + queueName);
						xsltProviderListeners.add(queueName);
					}
				}
			}
		}

		// Init senders
		FileController.initSender(queues, fileSenders, properties);
		JdbcFixedQueryController.initSender(queues, jdbcFixedQuerySenders, properties, appConstants);
		JmsController.initSenders(queues, jmsSenders, properties);
		SenderController.initDelaySender(queues, delaySenders, properties);
		SenderController.initHttpSender(queues, httpSenders, properties, scenarioDirectory);
		SenderController.initIbisJavaSender(queues, ibisJavaSenders, properties);
		SenderController.initIbisWebSender(queues, ibisWebServiceSenders, properties);

		// Init listeners
		FileController.initListener(queues, fileListeners, properties);
		JmsController.initListeners(queues, jmsListeners, properties, TestTool.globalTimeout);
		ListenerController.initJavaListener(queues, javaListeners, properties);
		ListenerController.initXsltProviderListener(queues, xsltProviderListeners, properties);
		WebServiceController.initListeners(queues, webServiceListeners, properties, TestTool.globalTimeout);

		return queues;
	}

	public static boolean closeQueues(Map<String, Map<String, Object>> queues, Properties properties) {
		boolean remainingMessagesFound = false;

		JmsController.closeSenders(queues, properties);
		remainingMessagesFound = JmsController.closeListeners(queues, properties);

		remainingMessagesFound = JdbcFixedQueryController.closeConnection(queues, properties) ? true
				: remainingMessagesFound;

		SenderController.closeIbisWebSender(queues, properties);
		WebServiceController.closeSender(queues, properties);

		remainingMessagesFound = WebServiceController.closeListener(queues, properties) ? true : remainingMessagesFound;

		SenderController.closeIbisJavaSender(queues, properties);
		SenderController.closeDelaySender(queues, properties);

		remainingMessagesFound = ListenerController.closeJavaListener(queues, properties) ? true
				: remainingMessagesFound;

		FileController.closeListener(queues, properties);
		ListenerController.closeXsltProviderListener(queues, properties);

		return remainingMessagesFound;
	}

}
