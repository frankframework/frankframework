package nl.nn.adapterframework.larva;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Jaco de Groot, Murat Kaan Meral
 */
public class TestTool {
//	@Autowired
	private static IbisContext ibisContext;
	private MessageListener messageListener;
	public static final String TESTTOOL_CORRELATIONID = "Test Tool correlation id";
	protected static final String TESTTOOL_BIFNAME = "Test Tool bif name";
	public static final String TESTTOOL_DUMMY_MESSAGE = "<TestTool>Dummy message</TestTool>";
	public static final String TESTTOOL_CLEAN_UP_REPLY = "<TestTool>Clean up reply</TestTool>";
	public static final int RESULT_ERROR = 0;
	public static final int RESULT_OK = 1;
	static final int RESULT_AUTOSAVED = 2;
	// dirty solution by Marco de Reus:
	private static String zeefVijlNeem = "";
	private static Writer silentOut = null;
	static boolean autoSaveDiffs = false;
	static final int DEFAULT_TIMEOUT = 30000;
	static int globalTimeout = DEFAULT_TIMEOUT;

	public static void setGlobalTimeout(int globalTimeout) {
		TestTool.globalTimeout = globalTimeout;
	}

	public TestTool(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	/*
	 * public static IbisContext getIbisContext(ServletContext application) {
	 * AppConstants appConstants = AppConstants.getInstance(); String ibisContextKey
	 * = appConstants.getResolvedProperty(ConfigurationServlet.KEY_CONTEXT);
	 * IbisContext ibisContext =
	 * (IbisContext)application.getAttribute(ibisContextKey); return ibisContext; }
	 */
	public static IbisContext getIbisContext(ServletContext application) {
		AppConstants appConstants = AppConstants.getInstance();
		String ibisContextKey = appConstants.getResolvedProperty(ConfigurationServlet.KEY_CONTEXT);
		ibisContext = (IbisContext) application.getAttribute(ibisContextKey);
		return ibisContext;
	}

	public static IbisContext getIbisContext() {
		return ibisContext;
	}

	public static AppConstants getAppConstants(IbisContext ibisContext) {
		// Load AppConstants using a class loader to get an instance that has
		// resolved application.server.type in ServerSpecifics*.properties,
		// SideSpecifics*.properties and StageSpecifics*.properties filenames
		// See IbisContext.setDefaultApplicationServerType() and userstory
		// 'Refactor ConfigurationServlet en AppConstants' too.
		IbisManager ibisManager = ibisContext.getIbisManager();
		List<Configuration> li = ibisManager.getConfigurations();
		Configuration configuration = li.get(0);
		return AppConstants.getInstance(configuration.getClassLoader());
	}

	/**
	 * 
	 * @return negative: error condition 0: all scenarios passed positive: number of
	 *         scenarios that failed
	 */
	public int runScenarios(String paramExecute, int waitBeforeCleanUp, String currentScenariosRootDirectory, int numberOfThreads, int testTimeout) {
		AppConstants appConstants = getAppConstants(ibisContext);
		appConstants = TestPreparer.getAppConstantsFromDirectory(currentScenariosRootDirectory, appConstants);
		String asd = appConstants.getResolvedProperty("larva.diffs.autosave");
		if (asd != null) {
			autoSaveDiffs = Boolean.parseBoolean(asd);
		}

		if(testTimeout < 0) {
			testTimeout = Integer.MAX_VALUE;
		}

		messageListener.debugMessage(
				"General", "Execute scenario(s) if execute parameter present and scenarios root directory did not change");

		// Get Ready For Execution
		int[] scenarioResults = { 0, 0, 0 }; // [0] is for errors, [1] is for ok, [2] is for autosaved. positions


		if (paramExecute != null) {
			String paramExecuteCanonicalPath;
			String scenariosRootDirectoryCanonicalPath;
			try {
				paramExecuteCanonicalPath = new File(paramExecute).getCanonicalPath();
				scenariosRootDirectoryCanonicalPath = new File(currentScenariosRootDirectory).getCanonicalPath();
			} catch (IOException e) {
				paramExecuteCanonicalPath = paramExecute;
				scenariosRootDirectoryCanonicalPath = currentScenariosRootDirectory;
				messageListener.errorMessage("General", "Could not get canonical path: " + e.getMessage(), e);
			}
			if (paramExecuteCanonicalPath.startsWith(scenariosRootDirectoryCanonicalPath)) {
				//Map<String, List<File>> scenarioFiles = TestPreparer.readScenarioFiles(appConstants, paramExecute,
				//		(numberOfThreads > 1));

				messageListener.debugMessage("General", "Initialize statistics variables");
				long startTime = System.currentTimeMillis();
				messageListener.debugMessage("General", "Execute scenario('s)");
				Map<String, List<File>> scenarioFiles = TestPreparer.readScenarioFiles(paramExecute, (numberOfThreads > 1), appConstants);
				Iterator<Entry<String, List<File>>> scenarioFilesIterator = scenarioFiles.entrySet().iterator();

				int scenariosTotal = TestPreparer.getNumberOfScenarios(scenarioFiles); // TODO: FIND A BETTER SOLUTION
																						// FOR THIS!

				ExecutorService threadPool = Executors.newFixedThreadPool(numberOfThreads);
				// Start Executing Scenario by Scenario
				while (scenarioFilesIterator.hasNext()) {
					Map.Entry<String, List<File>> scenarioEntry = scenarioFilesIterator.next();
					List<File> scenarioFileList = scenarioEntry.getValue();

					ScenarioTester scenarioThread = new ScenarioTester(ibisContext, messageListener, scenarioFileList, currentScenariosRootDirectory,
							appConstants, scenarioResults, waitBeforeCleanUp, scenariosTotal);
					messageListener.debugMessage("General", "Added a new.");

					threadPool.execute(scenarioThread);
				}
				try {
					messageListener.debugMessage("General", "Starting to await termination.");
					threadPool.shutdown();
					if(!threadPool.awaitTermination(testTimeout, TimeUnit.SECONDS)) {
						messageListener.errorMessage("General", "Timeout has occurred!");
					}
					messageListener.debugMessage("General", "Finished await termination.");
				} catch (InterruptedException e) {
					messageListener.errorMessage("General", "Scenario testing was interrupted: \n" + e.getMessage());
				}
				// End of scenarios

				long executeTime = System.currentTimeMillis() - startTime;

				messageListener.debugMessage("General", "Print statistics information");
				scenariosTotal = scenarioResults[0] + scenarioResults[1] + scenarioResults[2];
				if (scenariosTotal == 0) {
					messageListener.scenariosTotalMessage("No scenarios found");
				} else {
					messageListener.debugMessage("General", "Print statistics information");
					if (scenarioResults[1] == scenariosTotal) {
						if (scenariosTotal == 1) {
							messageListener.scenariosPassedTotalMessage(
									"All scenarios passed (1 scenario executed in " + executeTime + " ms)");
						} else {
							messageListener.scenariosPassedTotalMessage("All scenarios passed (" + scenariosTotal
									+ " scenarios executed in " + executeTime + " ms)");
						}
					} else if (scenarioResults[0] == scenariosTotal) {
						if (scenariosTotal == 1) {
							messageListener.scenariosFailedTotalMessage(
									"All scenarios failed (1 scenario executed in " + executeTime + " ms)");
						} else {
							messageListener.scenariosFailedTotalMessage("All scenarios failed (" + scenariosTotal
									+ " scenarios executed in " + executeTime + " ms)");
						}
					} else {
						if (scenariosTotal == 1) {
							messageListener.scenariosTotalMessage("1 scenario executed in " + executeTime + " ms");
						} else {
							messageListener.scenariosTotalMessage(
									scenariosTotal + " scenarios executed in " + executeTime + " ms");
						}
						if (scenarioResults[1] == 1) {
							messageListener.scenariosPassedTotalMessage("1 scenario passed");
						} else {
							messageListener.scenariosPassedTotalMessage(scenarioResults[1] + " scenarios passed");
						}
						if (autoSaveDiffs) {
							if (scenarioResults[2] == 1) {
								messageListener.scenariosAutosavedTotalMessage("1 scenario passed after autosave");
							} else {
								messageListener.scenariosAutosavedTotalMessage(
										scenarioResults[2] + " scenarios passed after autosave");
							}
						}
						if (scenarioResults[0] == 1) {
							messageListener.scenariosFailedTotalMessage("1 scenario failed");
						} else {
							messageListener.scenariosFailedTotalMessage(scenarioResults[0] + " scenarios failed");
						}
					}
				}
			}
		}
		return scenarioResults[0];
	}

	// Used by saveResultToFile.jsp
	public static void writeFile(String fileName, String content) throws IOException {
		String encoding = getEncoding(fileName, content);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(fileName), encoding);
		outputStreamWriter.write(content);
		outputStreamWriter.close();
	}

	private static String getEncoding(String fileName, String content) {
		String encoding = null;
		if (fileName.endsWith(".xml") || fileName.endsWith(".wsdl")) {
			if (content.startsWith("<?xml") && content.indexOf("?>") != -1) {
				String declaration = content.substring(0, content.indexOf("?>"));
				int encodingIndex = declaration.indexOf("encoding");
				if (encodingIndex != -1) {
					int doubleQuoteIndex1 = declaration.indexOf('"', encodingIndex);
					int doubleQuoteIndex2 = -1;
					if (doubleQuoteIndex1 != -1) {
						doubleQuoteIndex2 = declaration.indexOf('"', doubleQuoteIndex1 + 1);
					}
					int singleQuoteIndex1 = declaration.indexOf('\'', encodingIndex);
					int singleQuoteIndex2 = -1;
					if (singleQuoteIndex1 != -1) {
						singleQuoteIndex2 = declaration.indexOf('\'', singleQuoteIndex1 + 1);
					}
					if (doubleQuoteIndex2 != -1 && (singleQuoteIndex2 == -1 || doubleQuoteIndex2 < singleQuoteIndex2)) {
						encoding = declaration.substring(doubleQuoteIndex1 + 1, doubleQuoteIndex2);
					} else if (singleQuoteIndex2 != -1) {
						encoding = declaration.substring(singleQuoteIndex1 + 1, singleQuoteIndex2);
					}
				}
			}
			if (encoding == null) {
				encoding = "UTF-8";
			}
		} else if (fileName.endsWith(".utf8")) {
			encoding = "UTF-8";
		} else {
			encoding = "ISO-8859-1";
		}
		return encoding;
	}

}
