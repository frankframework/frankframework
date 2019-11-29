package nl.nn.adapterframework.larva;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.larva.controller.*;
import nl.nn.adapterframework.util.AppConstants;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

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
	private MessageListener messageListener;
	private boolean onCommandLine;

	private FileController fileController;
	private JdbcFixedQueryController jdbcFixedQueryController;
	private JmsController jmsController;
	private ListenerController listenerController;
	private SenderController senderController;
	private WebServiceController webServiceController;

	IbisContext ibisContext;

	public ScenarioTester(IbisContext ibisContext, MessageListener messageListener, List<File> scenarioFile, String currentScenariosRootDirectory, AppConstants appConstants, int[] scenarioResults, int waitBeforeCleanUp, int scenariosTotal) {
		this(ibisContext, messageListener, scenarioFile, currentScenariosRootDirectory, appConstants, scenarioResults, waitBeforeCleanUp, scenariosTotal, false);
	}

	public ScenarioTester(IbisContext ibisContext, MessageListener messageListener, List<File> scenarioFile, String currentScenariosRootDirectory, AppConstants appConstants, int[] scenarioResults, int waitBeforeCleanUp, int scenariosTotal, boolean onCommandLine) {
		this.ibisContext = ibisContext;
		this.messageListener = messageListener;
		this.scenarioFile = scenarioFile;
		this.currentScenariosRootDirectory = currentScenariosRootDirectory;
		this.appConstants = appConstants;
		this.waitBeforeCleanUp = waitBeforeCleanUp;
		this.scenariosTotal = scenariosTotal;
		this.scenarioResults = scenarioResults;
		this.onCommandLine = onCommandLine;

		fileController = new FileController(this);
		jdbcFixedQueryController = new JdbcFixedQueryController(this);
		jmsController = new JmsController(this);
		listenerController = new ListenerController(this);
		senderController = new SenderController(this);
		webServiceController = new WebServiceController(this);
		for(IAdapter adapter : ibisContext.getIbisManager().getRegisteredAdapters()) {
			System.out.println(adapter.getName() + " : " + adapter.getRunState().toString());
		}
	}
	
	@Override
	public void run() {
		try {
			for (File scenario : scenarioFile) {
				testScenario(scenario);
			}
		} catch (Exception e) {
			e.printStackTrace();
			messageListener.errorMessage("General", e.getMessage());
		}
	}

	private void testScenario(File scenarioFile) {
		int scenarioPassed = TestTool.RESULT_ERROR;
		String scenarioDirectory = scenarioFile.getParentFile().getAbsolutePath() + File.separator;
		String longName = scenarioFile.getAbsolutePath();
		String shortName = longName.substring(currentScenariosRootDirectory.length() - 1, longName.length() - ".properties".length());

		String testName = shortName.split(Pattern.quote(File.separator))[1];

		messageListener.debugMessage("General", "Read property file " + scenarioFile.getName());
		Properties properties = TestPreparer.readProperties(appConstants, scenarioFile);

		List<String> steps;

		if (properties != null) {
			try {
				String passCommandLine = properties.getProperty("larva.cmd.pass", "false");
				if(onCommandLine && Boolean.parseBoolean(passCommandLine)) {
					messageListener.scenarioMessage(testName, "Passing the scenario [" + shortName + "]");
					scenarioResults[TestTool.RESULT_OK]++;
					return;
				}
			}catch (Exception e){
			}
			messageListener.testInitiationMessage(testName, longName, properties.getProperty("scenario.description"));

			messageListener.debugMessage(testName, "Read steps from property file");
			steps = TestPreparer.getSteps(properties);
			if (steps != null) {
				synchronized(STEP_SYNCHRONIZER) {
					messageListener.debugMessage(testName, "Open queues");
					Map<String, Map<String, Object>> queues = openQueues(scenarioDirectory, steps, properties, testName, ibisContext, appConstants);
					if (queues != null) {
						messageListener.debugMessage(testName, "Execute steps");
						boolean allStepsPassed = true;
						boolean autoSaved = false;
						Iterator<String> iterator = steps.iterator();

						// Execute Steps
						while (allStepsPassed && iterator.hasNext()) {
							String step = (String)iterator.next();
							String stepDisplayName = shortName + " - " + step + " - " + properties.get(step);
							messageListener.debugMessage(testName, "Execute step '" + stepDisplayName + "'");
							int stepPassed = executeStep(step, properties, testName, stepDisplayName, queues);
							if (stepPassed==TestTool.RESULT_OK) {
								messageListener.stepMessage(testName, "Step '" + stepDisplayName + "' passed");
							} else if (stepPassed==TestTool.RESULT_AUTOSAVED) {
								messageListener.stepMessage(testName, "Step '" + stepDisplayName + "' passed after autosave");
								autoSaved = true;
							} else {
								messageListener.stepMessage(testName, "Step '" + stepDisplayName + "' failed");
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
						messageListener.debugMessage(testName, "Wait " + waitBeforeCleanUp + " ms before clean up");
						try {
							Thread.sleep(waitBeforeCleanUp);
						} catch(InterruptedException e) {
						}
						messageListener.debugMessage(testName, "Close queues");
						boolean remainingMessagesFound = closeQueues(queues, properties, testName);
						if (remainingMessagesFound) {
							messageListener.stepMessage(testName, "Found one or more messages on queues or in database after scenario executed");
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
				messageListener.testStatusMessage(testName, "OK");
				messageListener.scenarioMessage(testName, "Scenario '" + shortName + " - "
						+ properties.getProperty("scenario.description") + "' passed (" + scenarioResults[0] + "/"
						+ scenarioResults[1] + "/" + scenariosTotal + ")");
				break;
			case TestTool.RESULT_AUTOSAVED:
				messageListener.testStatusMessage(testName, "AUTOSAVED");
				messageListener.scenarioMessage(testName, "Scenario '" + shortName + " - "
						+ properties.getProperty("scenario.description") + "' passed after autosave");
				break;
			case TestTool.RESULT_ERROR:
				messageListener.testStatusMessage(testName, "ERROR");
				messageListener.scenarioFailedMessage(testName, "Scenario '" + shortName + " - "
						+ properties.getProperty("scenario.description") + "' failed (" + scenarioResults[0] + "/"
						+ scenarioResults[1] + "/" + scenariosTotal + ")");
				break;
			default:
				messageListener.errorMessage(testName, "Could not retrieve the result of the test " + shortName);
				break;
			}
		}
	}
	
	private int executeStep(String step, Properties properties, String testName, String stepDisplayName, Map<String, Map<String, Object>> queues) {
		int stepPassed = TestTool.RESULT_ERROR;
		String fileName = properties.getProperty(step);
		// PROBLEM IS HERE!
		String fileNameAbsolutePath = properties.getProperty(step + ".absolutepath");
		int i = step.indexOf('.');
		String queueName;
		String fileContent;

		if ("".equals(fileName)) {
			messageListener.errorMessage(testName, "No file specified for step '" + step + "'");
		} else {
			messageListener.debugMessage(testName, "Read file " + fileName);
			fileContent = TestPreparer.advancedReadFile(properties, fileNameAbsolutePath, messageListener, testName);
			if (fileContent == null) {
				messageListener.errorMessage(testName, "Could not read file '" + fileName + "'");
			} else {
				if (step.endsWith(".read")) {
					queueName = step.substring(i + 1, step.length() - 5);
					if ("nl.nn.adapterframework.jms.JmsListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = jmsController.read(step, stepDisplayName, properties, queues, queueName, fileName, fileContent, fileNameAbsolutePath, testName);
					} else 	if ("nl.nn.adapterframework.jdbc.FixedQuerySender".equals(properties.get(queueName + ".className"))) {
						stepPassed = jdbcFixedQueryController.read(step, stepDisplayName, properties, queues, queueName, fileName, fileContent, fileNameAbsolutePath, testName);
					} else if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = senderController.executeSenderRead(step, stepDisplayName, properties, queues, queueName, "ibisWebService", fileName, fileContent, fileNameAbsolutePath, testName);
					} else if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = senderController.executeSenderRead(step, stepDisplayName, properties, queues, queueName, "webService", fileName, fileContent, fileNameAbsolutePath, testName);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = webServiceController.read(step, stepDisplayName, properties, queues, queueName, fileName, fileContent, fileNameAbsolutePath, testName);
					} else if ("nl.nn.adapterframework.http.HttpSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = senderController.executeSenderRead(step, stepDisplayName, properties, queues, queueName, "http", fileName, fileContent, fileNameAbsolutePath, testName);
					} else if ("nl.nn.adapterframework.senders.IbisJavaSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = senderController.executeSenderRead(step, stepDisplayName, properties, queues, queueName, "ibisJava", fileName, fileContent, fileNameAbsolutePath, testName);
					} else if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = webServiceController.read(step, stepDisplayName, properties, queues, queueName, fileName, fileContent, fileNameAbsolutePath, testName);
					} else if ("nl.nn.adapterframework.larva.FileListener".equals(properties.get(queueName + ".className"))
								|| "nl.nn.adapterframework.testtool.FileListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = fileController.executeListenerRead(testName, step, stepDisplayName, properties, queues, queueName, fileName, fileContent, fileNameAbsolutePath);
					} else if ("nl.nn.adapterframework.larva.FileSender".equals(properties.get(queueName + ".className"))
								|| "nl.nn.adapterframework.testtool.FileSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = fileController.executeSenderRead(testName, step, stepDisplayName, properties, queues, queueName, fileName, fileContent, fileNameAbsolutePath);
					} else if ("nl.nn.adapterframework.larva.XsltProviderListener".equals(properties.get(queueName + ".className"))
								|| "nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = listenerController.executeXsltProviderListenerRead(stepDisplayName, properties, queues, queueName, fileContent, TestPreparer.createParametersMapFromParamProperties(properties, step, false, null), testName);
					} else {
						messageListener.errorMessage(testName, testName + ": Property '" + queueName + ".className' not found or not valid");
					}
				} else {
					queueName = step.substring(i + 1, step.length() - 6);
					if ("nl.nn.adapterframework.jms.JmsSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = jmsController.write(testName, stepDisplayName, queues, queueName, fileContent);
					} else if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = senderController.executeSenderWrite(testName, stepDisplayName, queues, queueName, "ibisWebService", fileContent);
					} else if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = senderController.executeSenderWrite(testName, stepDisplayName, queues, queueName, "webService", fileContent);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = webServiceController.write(testName, stepDisplayName, queues, queueName, fileContent);
					} else if ("nl.nn.adapterframework.http.HttpSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = senderController.executeSenderWrite(testName, stepDisplayName, queues, queueName, "http", fileContent);
					} else if ("nl.nn.adapterframework.senders.IbisJavaSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = senderController.executeSenderWrite(testName, stepDisplayName, queues, queueName, "ibisJava", fileContent);
					} else if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = webServiceController.write(testName, stepDisplayName, queues, queueName, fileContent);
					} else if ("nl.nn.adapterframework.larva.FileSender".equals(properties.get(queueName + ".className"))
								|| "nl.nn.adapterframework.testtool.FileSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = fileController.executeSenderWrite(testName, stepDisplayName, queues, queueName, fileContent);
					} else if ("nl.nn.adapterframework.larva.XsltProviderListener".equals(properties.get(queueName + ".className"))
								|| "nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = listenerController.executeXsltProviderListenerWrite(step, stepDisplayName, queues, queueName, fileName, fileContent, properties, fileNameAbsolutePath, testName);
					} else if ("nl.nn.adapterframework.senders.DelaySender".equals(properties.get(queueName + ".className"))) {
						stepPassed = senderController.executeDelaySenderWrite(testName, stepDisplayName, queues, queueName, fileContent);
					} else {
						messageListener.errorMessage(testName, "Property '" + queueName + ".className' not found or not valid");
					}
				}
			}
		}

		return stepPassed;
	}

	private Map<String, Map<String, Object>> openQueues(String scenarioDirectory, List<String> steps,
															   Properties properties, String testName, IbisContext ibisContext, AppConstants appConstants) {
		Map<String, Map<String, Object>> queues = new HashMap<String, Map<String, Object>>();
		messageListener.debugMessage(testName, "Get all queue names");
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
					messageListener.debugMessage(testName, "queuename openqueue: " + queueName);
					String className = properties.getProperty(queueName + ".className");

					if ("nl.nn.adapterframework.jms.JmsSender".equals(properties.get(queueName + ".className"))
							&& !jmsSenders.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding jmsSender queue: " + queueName);
						jmsSenders.add(queueName);
					} else if ("nl.nn.adapterframework.jms.JmsListener".equals(properties.get(queueName + ".className"))
							&& !jmsListeners.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding jmsListener queue: " + queueName);
						jmsListeners.add(queueName);
					} else if ("nl.nn.adapterframework.jdbc.FixedQuerySender".equals(
							properties.get(queueName + ".className")) && !jdbcFixedQuerySenders.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding jdbcFixedQuerySender queue: " + queueName);
						jdbcFixedQuerySenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(
							properties.get(queueName + ".className")) && !ibisWebServiceSenders.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding ibisWebServiceSender queue: " + queueName);
						ibisWebServiceSenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.WebServiceSender".equals(
							properties.get(queueName + ".className")) && !webServiceSenders.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding webServiceSender queue: " + queueName);
						webServiceSenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(
							properties.get(queueName + ".className")) && !webServiceListeners.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding webServiceListener queue: " + queueName);
						webServiceListeners.add(queueName);
					} else if ("nl.nn.adapterframework.http.HttpSender".equals(properties.get(queueName + ".className"))
							&& !httpSenders.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding httpSender queue: " + queueName);
						httpSenders.add(queueName);
					} else if ("nl.nn.adapterframework.senders.IbisJavaSender"
							.equals(properties.get(queueName + ".className")) && !ibisJavaSenders.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding ibisJavaSender queue: " + queueName);
						ibisJavaSenders.add(queueName);
					} else if ("nl.nn.adapterframework.senders.DelaySender"
							.equals(properties.get(queueName + ".className")) && !delaySenders.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding delaySender queue: " + queueName);
						delaySenders.add(queueName);
					} else if ("nl.nn.adapterframework.receivers.JavaListener"
							.equals(properties.get(queueName + ".className")) && !javaListeners.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding javaListener queue: " + queueName);
						javaListeners.add(queueName);
					} else if (("nl.nn.adapterframework.larva.FileSender".equals(properties.get(queueName + ".className"))
							||  "nl.nn.adapterframework.testtool.FileSender".equals(properties.get(queueName + ".className")))
							&& !fileSenders.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding fileSender queue: " + queueName);
						fileSenders.add(queueName);
					} else if (("nl.nn.adapterframework.larva.FileListener".equals(properties.get(queueName + ".className"))
							|| "nl.nn.adapterframework.testtool.FileListener".equals(properties.get(queueName + ".className")))
							&& !fileListeners.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding fileListener queue: " + queueName);
						fileListeners.add(queueName);
					} else if (("nl.nn.adapterframework.larva.XsltProviderListener".equals(properties.get(queueName + ".className"))
							|| "nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className")))
							&& !xsltProviderListeners.contains(queueName)) {
						messageListener.debugMessage(testName, "Adding xsltProviderListeners queue: " + queueName);
						xsltProviderListeners.add(queueName);
					}
				}
			}
		}

		// Init senders
		fileController.initSender(queues, fileSenders, properties, testName);
		jdbcFixedQueryController.initSender(queues, jdbcFixedQuerySenders, properties, appConstants, testName);
		jmsController.initSenders(queues, jmsSenders, properties, testName);
		senderController.initDelaySender(queues, delaySenders, properties, testName);
		senderController.initHttpSender(queues, httpSenders, properties, scenarioDirectory, testName);
		senderController.initIbisJavaSender(queues, ibisJavaSenders, properties, testName);
		senderController.initIbisWebSender(queues, ibisWebServiceSenders, properties, testName);
		webServiceController.initSenders(queues, webServiceSenders, properties, testName);

		// Init listeners
		fileController.initListener(queues, fileListeners, properties, testName);
		jmsController.initListeners(queues, jmsListeners, properties, TestTool.globalTimeout, testName);
		listenerController.initJavaListener(queues, javaListeners, properties, testName);
		listenerController.initXsltProviderListener(queues, xsltProviderListeners, properties, testName);
		webServiceController.initListeners(queues, webServiceListeners, properties, TestTool.globalTimeout, testName);

		return queues;
	}

	public boolean closeQueues(Map<String, Map<String, Object>> queues, Properties properties, String testName) {
		boolean remainingMessagesFound;

		jmsController.closeSenders(queues, properties, testName);
		remainingMessagesFound = jmsController.closeListeners(queues, properties, testName);

		remainingMessagesFound = jdbcFixedQueryController.closeConnection(queues, properties, testName) || remainingMessagesFound;

		senderController.closeIbisWebSender(queues, properties, testName);
		webServiceController.closeSender(queues, properties, testName);

		remainingMessagesFound = webServiceController.closeListener(queues, properties, testName) || remainingMessagesFound;

		senderController.closeIbisJavaSender(queues, properties, testName);
		senderController.closeDelaySender(queues, properties, testName);

		remainingMessagesFound = listenerController.closeJavaListener(queues, properties, testName) || remainingMessagesFound;

		fileController.closeListener(queues, properties, testName);
		listenerController.closeXsltProviderListener(queues, properties, testName);

		return remainingMessagesFound;
	}

	public MessageListener getMessageListener() {
		return messageListener;
	}
}
