package nl.nn.adapterframework.testtool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.DocumentException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.ProcessUtil;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

/**
 * @author Jaco de Groot, Murat Kaan Meral
 */

/*
 * 
 * TODO: SEPERATE PREPARING - TESTING AND REPORTING!!!!
 * 
 */
//////// Todo: seems to be executing all scenarios instead of paramExecute. Check that out!

public class TestTool {
//	@Autowired
	private static IbisContext ibisContext;

	private static Logger logger = LogUtil.getLogger(TestTool.class);
	protected static final int DEFAULT_TIMEOUT = 30000;
	public static final String TESTTOOL_CORRELATIONID = "Test Tool correlation id";
	protected static final String TESTTOOL_BIFNAME = "Test Tool bif name";
	public static final String TESTTOOL_DUMMY_MESSAGE = "<TestTool>Dummy message</TestTool>";
	public static final String TESTTOOL_CLEAN_UP_REPLY = "<TestTool>Clean up reply</TestTool>";
	public static final int RESULT_ERROR = 0;
	public static final int RESULT_OK = 1;
	public static final int RESULT_AUTOSAVED = 2;
	// dirty solution by Marco de Reus:
	private static String zeefVijlNeem = "";
	private static Writer silentOut = null;
	protected static boolean autoSaveDiffs = false;
	static int globalTimeout = DEFAULT_TIMEOUT;

	public static void setTimeout(int newTimeout) {
		globalTimeout = newTimeout;
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

	public static final int ERROR_NO_SCENARIO_DIRECTORIES_FOUND = -1;

	/**
	 * 
	 * @return negative: error condition 0: all scenarios passed positive: number of
	 *         scenarios that failed
	 */
	public static int runScenarios(String paramExecute, int waitBeforeCleanUp, String currentScenariosRootDirectory, int numberOfThreads) {
		AppConstants appConstants = AppConstants.getInstance();
		String asd = appConstants.getResolvedProperty("larva.diffs.autosave");
		if (asd != null) {
			autoSaveDiffs = Boolean.parseBoolean(asd);
		}

		MessageListener.debugMessage(
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
				MessageListener.errorMessage("General", "Could not get canonical path: " + e.getMessage(), e);
			}
			if (paramExecuteCanonicalPath.startsWith(scenariosRootDirectoryCanonicalPath)) {

				MessageListener.debugMessage("General", "Initialize XMLUnit");
				XMLUnit.setIgnoreWhitespace(true);
				MessageListener.debugMessage("General", "Initialize 'scenario files' variable");
				MessageListener.debugMessage("General", "Param execute: " + paramExecute);

				//Map<String, List<File>> scenarioFiles = TestPreparer.readScenarioFiles(appConstants, paramExecute,
				//		(numberOfThreads > 1));

				MessageListener.debugMessage("General", "Initialize statistics variables");
				long startTime = System.currentTimeMillis();
				MessageListener.debugMessage("General", "Execute scenario('s)");
				Map<String, List<File>> scenarioFiles = TestPreparer.readScenarioFiles(paramExecute, (numberOfThreads > 1), appConstants);
				Iterator<Entry<String, List<File>>> scenarioFilesIterator = scenarioFiles.entrySet().iterator();

				int scenariosTotal = TestPreparer.getNumberOfScenarios(scenarioFiles); // TODO: FIND A BETTER SOLUTION
																						// FOR THIS!

				ExecutorService threadPool = Executors.newFixedThreadPool(numberOfThreads);
				// Start Executing Scenario by Scenario
				while (scenarioFilesIterator.hasNext()) {
					Map.Entry<String, List<File>> scenarioEntry = scenarioFilesIterator.next();
					List<File> scenarioFileList = scenarioEntry.getValue();

					ScenarioTester scenarioThread = new ScenarioTester(scenarioFileList, currentScenariosRootDirectory,
							appConstants, scenarioResults, waitBeforeCleanUp, scenariosTotal);
					MessageListener.debugMessage("General", "Added a new.");

					threadPool.execute(scenarioThread);
				}
				try {
					MessageListener.debugMessage("General", "Starting to await termination.");
					threadPool.shutdown();
					threadPool.awaitTermination(globalTimeout * scenariosTotal, TimeUnit.MILLISECONDS);
					MessageListener.debugMessage("General", "Finished await termination.");
				} catch (InterruptedException e) {
					MessageListener.errorMessage("General", "Scenario testing was interrupted: \n" + e.getMessage());
				}
				// End of scenarios

				long executeTime = System.currentTimeMillis() - startTime;

				MessageListener.debugMessage("General", "Print statistics information");

				if (scenariosTotal == 0) {
					MessageListener.scenariosTotalMessage("No scenarios found");
				} else {
					MessageListener.debugMessage("General", "Print statistics information");
					if (scenarioResults[1] == scenariosTotal) {
						if (scenariosTotal == 1) {
							MessageListener.scenariosPassedTotalMessage(
									"All scenarios passed (1 scenario executed in " + executeTime + " ms)");
						} else {
							MessageListener.scenariosPassedTotalMessage("All scenarios passed (" + scenariosTotal
									+ " scenarios executed in " + executeTime + " ms)");
						}
					} else if (scenarioResults[0] == scenariosTotal) {
						if (scenariosTotal == 1) {
							MessageListener.scenariosFailedTotalMessage(
									"All scenarios failed (1 scenario executed in " + executeTime + " ms)");
						} else {
							MessageListener.scenariosFailedTotalMessage("All scenarios failed (" + scenariosTotal
									+ " scenarios executed in " + executeTime + " ms)");
						}
					} else {
						if (scenariosTotal == 1) {
							MessageListener.scenariosTotalMessage("1 scenario executed in " + executeTime + " ms");
						} else {
							MessageListener.scenariosTotalMessage(
									scenariosTotal + " scenarios executed in " + executeTime + " ms");
						}
						if (scenarioResults[1] == 1) {
							MessageListener.scenariosPassedTotalMessage("1 scenario passed");
						} else {
							MessageListener.scenariosPassedTotalMessage(scenarioResults[1] + " scenarios passed");
						}
						if (autoSaveDiffs) {
							if (scenarioResults[2] == 1) {
								MessageListener.scenariosAutosavedTotalMessage("1 scenario passed after autosave");
							} else {
								MessageListener.scenariosAutosavedTotalMessage(
										scenarioResults[2] + " scenarios passed after autosave");
							}
						}
						if (scenarioResults[0] == 1) {
							MessageListener.scenariosFailedTotalMessage("1 scenario failed");
						} else {
							MessageListener.scenariosFailedTotalMessage(scenarioResults[0] + " scenarios failed");
						}
					}
				}
			}
		}
		return scenarioResults[0];
	}

	// Used by saveResultToFile.jsp
	public static void windiff(ServletContext application, HttpServletRequest request, String expectedFileName,
			String result, String expected) throws IOException, DocumentException, SenderException {
		AppConstants appConstants = AppConstants.getInstance();
		String windiffCommand = appConstants.getResolvedProperty("larva.windiff.command");
		if (windiffCommand == null) {
			String servletPath = request.getServletPath();
			int i = servletPath.lastIndexOf('/');
			String realPath = application.getRealPath(servletPath.substring(0, i));
			String currentScenariosRootDirectory = TestPreparer.initScenariosRootDirectories(realPath, null, appConstants);
			windiffCommand = currentScenariosRootDirectory + "..\\..\\IbisAlgemeenWasbak\\WinDiff\\WinDiff.Exe";
		}
		File tempFileResult = writeTempFile(expectedFileName, result);
		File tempFileExpected = writeTempFile(expectedFileName, expected);
		String command = windiffCommand + " " + tempFileResult + " " + tempFileExpected;
		ProcessUtil.executeCommand(command);
	}

	private static File writeTempFile(String originalFileName, String content) throws IOException, DocumentException {
		String encoding = getEncoding(originalFileName, content);

		String baseName = FileUtils.getBaseName(originalFileName);
		String extensie = FileUtils.getFileNameExtension(originalFileName);

		File tempFile = null;
		tempFile = File.createTempFile("ibistesttool", "." + extensie);
		tempFile.deleteOnExit();
		String tempFileMessage;
		if (extensie.equalsIgnoreCase("XML")) {
			tempFileMessage = XmlUtils.canonicalize(content);
		} else {
			tempFileMessage = content;
		}

		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(tempFile, true), encoding);
		outputStreamWriter.write(tempFileMessage);
		outputStreamWriter.close();

		return tempFile;
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
