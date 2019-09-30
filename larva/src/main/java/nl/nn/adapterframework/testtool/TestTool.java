package nl.nn.adapterframework.testtool;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import javax.jms.Message;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.test.Test;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import com.sun.syndication.io.XmlReader;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.http.IbisWebServiceSender;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.http.WebServiceSender;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.jms.PullingJmsListener;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.senders.DelaySender;
import nl.nn.adapterframework.senders.IbisJavaSender;
import nl.nn.adapterframework.testtool.controller.FileController;
import nl.nn.adapterframework.testtool.controller.JdbcFixedQueryController;
import nl.nn.adapterframework.testtool.controller.JmsController;
import nl.nn.adapterframework.testtool.controller.ListenerController;
import nl.nn.adapterframework.testtool.controller.SenderController;
import nl.nn.adapterframework.testtool.controller.WebServiceController;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CaseInsensitiveComparator;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.ProcessUtil;
import nl.nn.adapterframework.util.StringResolver;
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
	@Autowired
	static IbisContext ibisContext;

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
		IbisContext ibisContext = (IbisContext) application.getAttribute(ibisContextKey);
		return ibisContext;
	}

	public static AppConstants getAppConstants() {
		// Load AppConstants using a class loader to get an instance that has
		// resolved application.server.type in ServerSpecifics*.properties,
		// SideSpecifics*.properties and StageSpecifics*.properties filenames
		// See IbisContext.setDefaultApplicationServerType() and userstory
		// 'Refactor ConfigurationServlet en AppConstants' too.
		IbisManager ibisManager = ibisContext.getIbisManager();
		List<Configuration> li = ibisManager.getConfigurations();
		Configuration configuration = li.get(0);
		AppConstants appConstants = AppConstants.getInstance(configuration.getClassLoader());
		return appConstants;
	}

//	public static void runScenarios(ServletContext application, HttpServletRequest request, Writer out) {
//		runScenarios(application, request, out, false);
//	}
//
//	public static void runScenarios(ServletContext application, HttpServletRequest request, Writer out,
//			boolean silent) {
//		String paramLogLevel = request.getParameter("loglevel");
//		//String paramExecute = request.getParameter("execute");
//		String paramExecute = "/home/mkmeral/eclipse-workspace/.metadata/.plugins/org.eclipse.wst.server.core/tmp0/wtpwebapps/iaf-test/testtool/WebServiceListenerSender";
//		String paramWaitBeforeCleanUp = request.getParameter("waitbeforecleanup");
//		String servletPath = request.getServletPath();
//		int i = servletPath.lastIndexOf('/');
//		String realPath = application.getRealPath(servletPath.substring(0, i));
//		String paramScenariosRootDirectory = request.getParameter("scenariosrootdirectory");
//		// int numberOfThreads =
//		// Integer.parseInt(request.getParameter("numberofthreads"));
//		int numberOfThreads = 1;
//		// IbisContext ibisContext = getIbisContext(application);
//		if (ibisContext == null)
//			ibisContext = getIbisContext(application);
//		AppConstants appConstants = getAppConstants();
//		runScenarios(appConstants, paramLogLevel, paramExecute, paramWaitBeforeCleanUp, realPath,
//				paramScenariosRootDirectory, numberOfThreads, silent);
//	}

	public static final int ERROR_NO_SCENARIO_DIRECTORIES_FOUND = -1;

	/**
	 * 
	 * @return negative: error condition 0: all scenarios passed positive: number of
	 *         scenarios that failed
	 */
	public static int runScenarios(AppConstants appConstants, String paramExecute,
			int waitBeforeCleanUp, String currentScenariosRootDirectory, int numberOfThreads) {

		MessageListener.debugMessage("Start logging to logbuffer until form is written");
		String asd = appConstants.getResolvedProperty("larva.diffs.autosave");
		if (asd != null) {
			autoSaveDiffs = Boolean.parseBoolean(asd);
		}

		MessageListener.debugMessage(
				"Execute scenario(s) if execute parameter present and scenarios root directory did not change");

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
				MessageListener.errorMessage("Could not get canonical path: " + e.getMessage(), e);
			}
			if (paramExecuteCanonicalPath.startsWith(scenariosRootDirectoryCanonicalPath)) {

				MessageListener.debugMessage("Initialize XMLUnit");
				XMLUnit.setIgnoreWhitespace(true);
				MessageListener.debugMessage("Initialize 'scenario files' variable");
				MessageListener.debugMessage("Param execute: " + paramExecute);

				//Map<String, List<File>> scenarioFiles = TestPreparer.readScenarioFiles(appConstants, paramExecute,
				//		(numberOfThreads > 1));

				MessageListener.debugMessage("Initialize statistics variables");
				long startTime = System.currentTimeMillis();
				MessageListener.debugMessage("Execute scenario('s)");
				Iterator<Entry<String, List<File>>> scenarioFilesIterator = TestPreparer.scenarioFiles.entrySet().iterator();

				int scenariosTotal = TestPreparer.getNumberOfScenarios(TestPreparer.scenarioFiles); // TODO: FIND A BETTER SOLUTION
																						// FOR THIS!

				ExecutorService threadPool = Executors.newFixedThreadPool(numberOfThreads);
				// Start Executing Scenario by Scenario
				while (scenarioFilesIterator.hasNext()) {
					Map.Entry<String, List<File>> scenarioEntry = scenarioFilesIterator.next();
					List<File> scenarioFileList = scenarioEntry.getValue();

					ScenarioTester scenarioThread = new ScenarioTester(scenarioFileList, currentScenariosRootDirectory,
							appConstants, scenarioResults, waitBeforeCleanUp, scenariosTotal);

					threadPool.execute(scenarioThread);
				}
				try {
					threadPool.awaitTermination(globalTimeout * scenariosTotal, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					MessageListener.errorMessage("Scenario testing was interrupted: \n" + e.getMessage());
				}
				// End of scenarios

				long executeTime = System.currentTimeMillis() - startTime;

				MessageListener.debugMessage("Print statistics information");

				if (scenariosTotal == 0) {
					MessageListener.scenariosTotalMessage("No scenarios found");
				} else {
					MessageListener.debugMessage("Print statistics information");
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
				MessageListener.debugMessage("Start logging to htmlbuffer until form is written");

			}
		}
		return scenarioResults[0];
	}

	// Used by saveResultToFile.jsp
	public static void windiff(ServletContext application, HttpServletRequest request, String expectedFileName,
			String result, String expected) throws IOException, DocumentException, SenderException {
		AppConstants appConstants = getAppConstants();
		String windiffCommand = appConstants.getResolvedProperty("larva.windiff.command");
		if (windiffCommand == null) {
			String servletPath = request.getServletPath();
			int i = servletPath.lastIndexOf('/');
			String realPath = application.getRealPath(servletPath.substring(0, i));
			String currentScenariosRootDirectory = TestPreparer.initScenariosRootDirectories(realPath, null);
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
