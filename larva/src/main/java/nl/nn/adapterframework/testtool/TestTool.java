/*
   Copyright 2014-2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package nl.nn.adapterframework.testtool;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;

import jakarta.json.JsonException;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.jms.PullingJmsListener;
import nl.nn.adapterframework.lifecycle.IbisApplicationServlet;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.FileMessage;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testtool.queues.Queue;
import nl.nn.adapterframework.testtool.queues.QueueCreator;
import nl.nn.adapterframework.testtool.queues.QueueWrapper;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CaseInsensitiveComparator;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.ProcessUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * @author Jaco de Groot
 */
public class TestTool {
	private static Logger logger = LogUtil.getLogger(TestTool.class);
	public static final String LOG_LEVEL_ORDER = "[debug], [pipeline messages prepared for diff], [pipeline messages], [wrong pipeline messages prepared for diff], [wrong pipeline messages], [step passed/failed], [scenario passed/failed], [scenario failed], [totals], [error]";
	private static final String STEP_SYNCHRONIZER = "Step synchronizer";
	protected static final String TESTTOOL_CORRELATIONID = "Test Tool correlation id";
	protected static final int DEFAULT_TIMEOUT = AppConstants.getInstance().getInt("larva.timeout", 10000);
	protected static final String TESTTOOL_BIFNAME = "Test Tool bif name";
	public static final nl.nn.adapterframework.stream.Message TESTTOOL_DUMMY_MESSAGE = new nl.nn.adapterframework.stream.Message("<TestTool>Dummy message</TestTool>");
	protected static final String TESTTOOL_CLEAN_UP_REPLY = "<TestTool>Clean up reply</TestTool>";
	public static final int RESULT_ERROR = 0;
	public static final int RESULT_OK = 1;
	public static final int RESULT_AUTOSAVED = 2;
	// dirty solution by Marco de Reus:
	private static String zeefVijlNeem = "";
	private static Writer silentOut = null;
	private static boolean autoSaveDiffs = false;
	private static AtomicLong correlationIdSuffixCounter = new AtomicLong(1);

	/*
	 * if allowReadlineSteps is set to true, actual results can be compared in line by using .readline steps.
	 * Those results cannot be saved to the inline expected value, however.
	 */
	private static final boolean allowReadlineSteps = false;
	protected static int globalTimeout=DEFAULT_TIMEOUT;

	private static final String TR_STARTING_TAG="<tr>";
	private static final String TR_CLOSING_TAG="</tr>";
	private static final String TD_STARTING_TAG="<td>";
	private static final String TD_CLOSING_TAG="</td>";
	private static final String TABLE_CLOSING_TAG="</table>";

	public static void setTimeout(int newTimeout) {
		globalTimeout=newTimeout;
	}

	private static IbisContext getIbisContext(ServletContext application) {
		return IbisApplicationServlet.getIbisContext(application);
	}

	public static void runScenarios(ServletContext application, HttpServletRequest request, Writer out, String realPath) {
		runScenarios(application, request, out, false, realPath);
	}

	private static void runScenarios(ServletContext application, HttpServletRequest request, Writer out, boolean silent, String realPath) {
		String paramLogLevel = request.getParameter("loglevel");
		String paramAutoScroll = request.getParameter("autoscroll");
		String paramExecute = request.getParameter("execute");
		String paramWaitBeforeCleanUp = request.getParameter("waitbeforecleanup");
		String paramGlobalTimeout = request.getParameter("timeout");
		int timeout=globalTimeout;
		if(paramGlobalTimeout != null) {
			try {
				timeout = Integer.parseInt(paramGlobalTimeout);
			} catch(NumberFormatException e) {
			}
		}
		String paramScenariosRootDirectory = request.getParameter("scenariosrootdirectory");
		IbisContext ibisContext = getIbisContext(application);
		runScenarios(ibisContext, paramLogLevel,
				paramAutoScroll, paramExecute, paramWaitBeforeCleanUp, timeout,
				realPath, paramScenariosRootDirectory, out, silent);
	}

	public static final int ERROR_NO_SCENARIO_DIRECTORIES_FOUND=-1;
	/**
	 *
	 * @return negative: error condition
	 * 		   0: all scenarios passed
	 * 		   positive: number of scenarios that failed
	 */
	public static int runScenarios(IbisContext ibisContext, String paramLogLevel,
			String paramAutoScroll, String paramExecute, String paramWaitBeforeCleanUp,
			int timeout, String realPath, String paramScenariosRootDirectory,
			Writer out, boolean silent) {
		AppConstants appConstants = AppConstants.getInstance();
		String logLevel = "wrong pipeline messages";
		String autoScroll = "true";
		if (paramLogLevel != null && LOG_LEVEL_ORDER.indexOf("[" + paramLogLevel + "]") > -1) {
			logLevel = paramLogLevel;
		}
		if (paramAutoScroll == null && paramLogLevel != null) {
			autoScroll = "false";
		}

		Map<String, Object> writers = null;
		if (!silent) {
			writers = new HashMap<String, Object>();
			writers.put("out", out);
			writers.put("htmlbuffer", new StringWriter());
			writers.put("logbuffer", new StringWriter());
			writers.put("loglevel", logLevel);
			writers.put("autoscroll", autoScroll);
			writers.put("usehtmlbuffer", "false");
			writers.put("uselogbuffer", "true");
			writers.put("messagecounter", new Integer(0));
			writers.put("scenariocounter", new Integer(1));
		} else {
			silentOut = out;
		}

		TestTool.debugMessage("Start logging to logbuffer until form is written", writers);
		String asd = appConstants.getResolvedProperty("larva.diffs.autosave");
		if (asd!=null) {
			autoSaveDiffs = Boolean.parseBoolean(asd);
		}
		debugMessage("Initialize scenarios root directories", writers);
		List<String> scenariosRootDirectories = new ArrayList<String>();
		List<String> scenariosRootDescriptions = new ArrayList<String>();
		String currentScenariosRootDirectory = initScenariosRootDirectories(
				realPath,
				paramScenariosRootDirectory, scenariosRootDirectories,
				scenariosRootDescriptions, writers);
		if (scenariosRootDirectories.size() == 0) {
			debugMessage("Stop logging to logbuffer", writers);
			writers.put("uselogbuffer", "stop");
			errorMessage("No scenarios root directories found", writers);
			return ERROR_NO_SCENARIO_DIRECTORIES_FOUND;
		}

		debugMessage("Read scenarios from directory '" + currentScenariosRootDirectory + "'", writers);
		List<File> allScenarioFiles = readScenarioFiles(appConstants, currentScenariosRootDirectory, writers);
		debugMessage("Initialize 'wait before cleanup' variable", writers);
		int waitBeforeCleanUp = 100;
		if (paramWaitBeforeCleanUp != null) {
			try {
				waitBeforeCleanUp = Integer.parseInt(paramWaitBeforeCleanUp);
			} catch(NumberFormatException e) {
			}
		}

		debugMessage("Write html form", writers);
		printHtmlForm(scenariosRootDirectories, scenariosRootDescriptions, currentScenariosRootDirectory, appConstants, allScenarioFiles, waitBeforeCleanUp, timeout, paramExecute, autoScroll, writers);
		debugMessage("Stop logging to logbuffer", writers);
		if (writers!=null) {
			writers.put("uselogbuffer", "stop");
		}
		debugMessage("Start debugging to out", writers);
		debugMessage("Execute scenario(s) if execute parameter present and scenarios root directory did not change", writers);
		int scenariosFailed = 0;
		if (paramExecute != null) {
			String paramExecuteCanonicalPath;
			String scenariosRootDirectoryCanonicalPath;
			try {
				paramExecuteCanonicalPath = new File(paramExecute).getCanonicalPath();
				scenariosRootDirectoryCanonicalPath = new File(currentScenariosRootDirectory).getCanonicalPath();
			} catch(IOException e) {
				paramExecuteCanonicalPath = paramExecute;
				scenariosRootDirectoryCanonicalPath = currentScenariosRootDirectory;
				errorMessage("Could not get canonical path: " + e.getMessage(), e, writers);
			}
			if (paramExecuteCanonicalPath.startsWith(scenariosRootDirectoryCanonicalPath)) {

				debugMessage("Initialize XMLUnit", writers);
				XMLUnit.setIgnoreWhitespace(true);
				debugMessage("Initialize 'scenario files' variable", writers);
				debugMessage("Param execute: " + paramExecute, writers);
				List<File> scenarioFiles;
				if (paramExecute.endsWith(".properties")) {
					debugMessage("Read one scenario", writers);
					scenarioFiles = new ArrayList<File>();
					scenarioFiles.add(new File(paramExecute));
				} else {
					debugMessage("Read all scenarios from directory '" + paramExecute + "'", writers);
					scenarioFiles = readScenarioFiles(appConstants, paramExecute, writers);
				}
				boolean evenStep = false;
				debugMessage("Initialize statistics variables", writers);
				int scenariosPassed = 0;
				int scenariosAutosaved = 0;
				long startTime = System.currentTimeMillis();
				debugMessage("Execute scenario('s)", writers);
				Iterator<File> scenarioFilesIterator = scenarioFiles.iterator();
				while (scenarioFilesIterator.hasNext()) {
					// increment suffix for each scenario
					String correlationId = TESTTOOL_CORRELATIONID + "("+ correlationIdSuffixCounter.getAndIncrement() +")";
					int scenarioPassed = RESULT_ERROR;
					File scenarioFile = scenarioFilesIterator.next();

					String scenarioDirectory = scenarioFile.getParentFile().getAbsolutePath() + File.separator;
					String longName = scenarioFile.getAbsolutePath();
					String shortName = longName.substring(currentScenariosRootDirectory.length() - 1, longName.length() - ".properties".length());

					if (writers!=null) {
						if (LOG_LEVEL_ORDER.indexOf("[" + (String)writers.get("loglevel") + "]") < LOG_LEVEL_ORDER.indexOf("[scenario passed/failed]")) {
							writeHtml("<br/>", writers, false);
							writeHtml("<br/>", writers, false);
							writeHtml("<div class='scenario'>", writers, false);
						}
					}
					debugMessage("Read property file " + scenarioFile.getName(), writers);
					Properties properties = readProperties(appConstants, scenarioFile, writers);
					List<String> steps = null;

					if (properties != null) {
						debugMessage("Read steps from property file", writers);
						steps = getSteps(properties, writers);
						if (steps != null) {
							synchronized(STEP_SYNCHRONIZER) {
								debugMessage("Open queues", writers);
								Map<String, Queue> queues = QueueCreator.openQueues(scenarioDirectory, properties, ibisContext, writers, timeout, correlationId);
								if (queues != null) {
									debugMessage("Execute steps", writers);
									boolean allStepsPassed = true;
									boolean autoSaved = false;
									Iterator<String> iterator = steps.iterator();
									while (allStepsPassed && iterator.hasNext()) {
										if (evenStep) {
											writeHtml("<div class='even'>", writers, false);
											evenStep = false;
										} else {
											writeHtml("<div class='odd'>", writers, false);
											evenStep = true;
										}
										String step = (String)iterator.next();
										String stepDisplayName = shortName + " - " + step + " - " + properties.get(step);
										debugMessage("Execute step '" + stepDisplayName + "'", writers);
										int stepPassed = executeStep(step, properties, stepDisplayName, queues, writers, timeout, correlationId);
										if (stepPassed==RESULT_OK) {
											stepPassedMessage("Step '" + stepDisplayName + "' passed", writers);
										} else if (stepPassed==RESULT_AUTOSAVED) {
											stepAutosavedMessage("Step '" + stepDisplayName + "' passed after autosave", writers);
											autoSaved = true;
										} else {
											stepFailedMessage("Step '" + stepDisplayName + "' failed", writers);
											allStepsPassed = false;
										}
										writeHtml("</div>", writers, false);
									}
									if (allStepsPassed) {
										if (autoSaved) {
											scenarioPassed = RESULT_AUTOSAVED;
										} else {
											scenarioPassed = RESULT_OK;
										}
									}
									debugMessage("Wait " + waitBeforeCleanUp + " ms before clean up", writers);
									try {
										Thread.sleep(waitBeforeCleanUp);
									} catch(InterruptedException e) {
									}
									debugMessage("Close queues", writers);
									boolean remainingMessagesFound = closeQueues(queues, properties, writers, correlationId);
									if (remainingMessagesFound) {
										stepFailedMessage("Found one or more messages on queues or in database after scenario executed", writers);
										scenarioPassed = RESULT_ERROR;
									}
								}
							}
						}
					}

					if (scenarioPassed==RESULT_OK) {
						scenariosPassed++;
						scenarioPassedMessage("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed (" + scenariosFailed + "/" + scenariosPassed + "/" + scenarioFiles.size() + ")", writers);
						if (silent && LOG_LEVEL_ORDER.indexOf("[" + logLevel + "]") <= LOG_LEVEL_ORDER.indexOf("[scenario passed/failed]")) {
							try {
								out.write("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed");
							} catch (IOException e) {
							}
						}
					} else if (scenarioPassed==RESULT_AUTOSAVED) {
						scenariosAutosaved++;
						scenarioAutosavedMessage("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed after autosave", writers);
						if (silent) {
							try {
								out.write("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed after autosave");
							} catch (IOException e) {
							}
						}
					} else {
						scenariosFailed++;
						scenarioFailedMessage("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' failed (" + scenariosFailed + "/" + scenariosPassed + "/" + scenarioFiles.size() + ")", writers);
						if (silent) {
							try {
								out.write("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' failed");
							} catch (IOException e) {
							}
						}
					}

					writeHtml("</div>", writers, false);
				}
				long executeTime = System.currentTimeMillis() - startTime;
				debugMessage("Print statistics information", writers);
				int scenariosTotal = scenariosPassed + scenariosAutosaved + scenariosFailed;
				if (scenariosTotal == 0) {
					scenariosTotalMessage("No scenarios found", writers, out, silent);
				} else {
					if (writers!=null) {
						if (LOG_LEVEL_ORDER.indexOf("[" + (String)writers.get("loglevel") + "]") <= LOG_LEVEL_ORDER.indexOf("[scenario passed/failed]")) {
							writeHtml("<br/>", writers, false);
							writeHtml("<br/>", writers, false);
						}
					}
					debugMessage("Print statistics information", writers);
					if (scenariosPassed == scenariosTotal) {
						if (scenariosTotal == 1) {
							scenariosPassedTotalMessage("All scenarios passed (1 scenario executed in " + executeTime + " ms)", writers, out, silent);
						} else {
							scenariosPassedTotalMessage("All scenarios passed (" + scenariosTotal + " scenarios executed in " + executeTime + " ms)", writers, out, silent);
						}
					} else if (scenariosFailed == scenariosTotal) {
						if (scenariosTotal == 1) {
							scenariosFailedTotalMessage("All scenarios failed (1 scenario executed in " + executeTime + " ms)", writers, out, silent);
						} else {
							scenariosFailedTotalMessage("All scenarios failed (" + scenariosTotal + " scenarios executed in " + executeTime + " ms)", writers, out, silent);
						}
					} else {
						if (scenariosTotal == 1) {
							scenariosTotalMessage("1 scenario executed in " + executeTime + " ms", writers, out, silent);
						} else {
							scenariosTotalMessage(scenariosTotal + " scenarios executed in " + executeTime + " ms", writers, out, silent);
						}
						if (scenariosPassed == 1) {
							scenariosPassedTotalMessage("1 scenario passed", writers, out, silent);
						} else {
							scenariosPassedTotalMessage(scenariosPassed + " scenarios passed", writers, out, silent);
						}
						if (autoSaveDiffs) {
							if (scenariosAutosaved == 1) {
								scenariosAutosavedTotalMessage("1 scenario passed after autosave", writers, out, silent);
							} else {
								scenariosAutosavedTotalMessage(scenariosAutosaved + " scenarios passed after autosave", writers, out, silent);
							}
						}
						if (scenariosFailed == 1) {
							scenariosFailedTotalMessage("1 scenario failed", writers, out, silent);
						} else {
							scenariosFailedTotalMessage(scenariosFailed + " scenarios failed", writers, out, silent);
						}
					}
				}
				debugMessage("Start logging to htmlbuffer until form is written", writers);
				if (writers!=null) {
					writers.put("usehtmlbuffer", "start");
				}
				writeHtml("<br/>", writers, false);
				writeHtml("<br/>", writers, false);
				printHtmlForm(scenariosRootDirectories, scenariosRootDescriptions, currentScenariosRootDirectory, appConstants, allScenarioFiles, waitBeforeCleanUp, timeout, paramExecute, autoScroll, writers);
				debugMessage("Stop logging to htmlbuffer", writers);
				if (writers!=null) {
					writers.put("usehtmlbuffer", "stop");
				}
				writeHtml("", writers, true);
			}
		}
		return scenariosFailed;
	}

	public static void printHtmlForm(List<String> scenariosRootDirectories, List<String> scenariosRootDescriptions, String scenariosRootDirectory, AppConstants appConstants, List<File> scenarioFiles, int waitBeforeCleanUp, int timeout, String paramExecute, String autoScroll, Map<String, Object> writers) {
		if (writers!=null) {
			writeHtml("<form action=\"index.jsp\" method=\"post\">", writers, false);

			// scenario root directory drop down
			writeHtml("<table style=\"float:left;height:50px\">", writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml("<td>Scenarios root directory</td>", writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml(TD_STARTING_TAG, writers, false);
			writeHtml("<select name=\"scenariosrootdirectory\" onchange=\"updateScenarios()\">", writers, false);
			Iterator<String> scenariosRootDirectoriesIterator = scenariosRootDirectories.iterator();
			Iterator<String> scenariosRootDescriptionsIterator = scenariosRootDescriptions.iterator();
			while (scenariosRootDirectoriesIterator.hasNext()) {
				String directory = (String)scenariosRootDirectoriesIterator.next();
				String description = (String)scenariosRootDescriptionsIterator.next();
				String option = "<option value=\"" + XmlUtils.encodeChars(directory) + "\"";
				if (scenariosRootDirectory.equals(directory)) {
					option = option + " selected";
				}
				option = option + ">" + XmlUtils.encodeChars(description) + "</option>";
				writeHtml(option, writers, false);
			}
			writeHtml("</select>", writers, false);
			writeHtml(TD_CLOSING_TAG, writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TABLE_CLOSING_TAG, writers, false);

			// Use a span to make IE put table on next line with a smaller window width
			writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", writers, false);
			writeHtml("<table style=\"float:left;height:50px\">", writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml("<td>Wait before clean up (ms)</td>", writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml(TD_STARTING_TAG, writers, false);
			writeHtml("<input type=\"text\" name=\"waitbeforecleanup\" value=\"" + waitBeforeCleanUp + "\">", writers, false);
			writeHtml(TD_CLOSING_TAG, writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TABLE_CLOSING_TAG, writers, false);

			writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", writers, false);
			// timeout box
			writeHtml("<table style=\"float:left;height:50px\">", writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml("<td>Default timeout (ms)</td>", writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml(TD_STARTING_TAG, writers, false);
			writeHtml("<input type=\"text\" name=\"timeout\" value=\"" + (timeout != globalTimeout ? timeout : globalTimeout) + "\" title=\"Global timeout for larva scenarios.\">", writers, false);
			writeHtml(TD_CLOSING_TAG, writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TABLE_CLOSING_TAG, writers, false);

			// log level dropdown
			writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", writers, false);
			writeHtml("<table style=\"float:left;height:50px\">", writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml("<td>Log level</td>", writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml(TD_STARTING_TAG, writers, false);
			writeHtml("<select name=\"loglevel\">", writers, false);
			StringTokenizer tokenizer = new StringTokenizer(LOG_LEVEL_ORDER, ",");
			while (tokenizer.hasMoreTokens()) {
				String level = tokenizer.nextToken().trim();
				level = level.substring(1, level.length() - 1);
				String option = "<option value=\"" + XmlUtils.encodeChars(level) + "\"";
				if (((String)writers.get("loglevel")).equals(level)) {
					option = option + " selected";
				}
				option = option + ">" + XmlUtils.encodeChars(level) + "</option>";
				writeHtml(option, writers, false);
			}
			writeHtml("</select>", writers, false);
			writeHtml(TD_CLOSING_TAG, writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TABLE_CLOSING_TAG, writers, false);

			// Auto scroll checkbox
			writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", writers, false);
			writeHtml("<table style=\"float:left;height:50px\">", writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml("<td>Auto scroll</td>", writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml(TD_STARTING_TAG, writers, false);
			writeHtml("<input type=\"checkbox\" name=\"autoscroll\" value=\"true\"", writers, false);
			if (autoScroll.equals("true")) {
				writeHtml(" checked", writers, false);
			}
			writeHtml(">", writers, false);
			writeHtml(TD_CLOSING_TAG, writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TABLE_CLOSING_TAG, writers, false);

			// Scenario(s)
			writeHtml("<table style=\"clear:both;float:left;height:50px\">", writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml("<td>Scenario(s)</td>", writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml(TD_STARTING_TAG, writers, false);
			writeHtml("<select name=\"execute\">", writers, false);
			debugMessage("Fill execute select box.", writers);
			Set<String> addedDirectories = new HashSet<>();
			Iterator<File> scenarioFilesIterator = scenarioFiles.iterator();
			while (scenarioFilesIterator.hasNext()) {
				File scenarioFile = scenarioFilesIterator.next();
				String scenarioDirectory = scenarioFile.getParentFile().getAbsolutePath() + File.separator;
				Properties properties = readProperties(appConstants, scenarioFile, writers);
				debugMessage("Add parent directories of '" + scenarioDirectory + "'", writers);
				int i = -1;
				String scenarioDirectoryCanonicalPath;
				String scenariosRootDirectoryCanonicalPath;
				try {
					scenarioDirectoryCanonicalPath = new File(scenarioDirectory).getCanonicalPath();
					scenariosRootDirectoryCanonicalPath = new File(scenariosRootDirectory).getCanonicalPath();
				} catch(IOException e) {
					scenarioDirectoryCanonicalPath = scenarioDirectory;
					scenariosRootDirectoryCanonicalPath = scenariosRootDirectory;
					errorMessage("Could not get canonical path: " + e.getMessage(), e, writers);
				}
				if (scenarioDirectoryCanonicalPath.startsWith(scenariosRootDirectoryCanonicalPath)) {
					i = scenariosRootDirectory.length() - 1;
					while (i != -1) {
						String longName = scenarioDirectory.substring(0, i + 1);
						debugMessage("longName: '" + longName + "'", writers);
						if (!addedDirectories.contains(longName)) {
							String shortName = scenarioDirectory.substring(scenariosRootDirectory.length() - 1, i + 1);
							String option = "<option value=\"" + XmlUtils.encodeChars(longName) + "\"";
							debugMessage("paramExecute: '" + paramExecute + "'", writers);
							if (paramExecute != null && paramExecute.equals(longName)) {
								option = option + " selected";
							}
							option = option + ">" + XmlUtils.encodeChars(shortName) + "</option>";
							writeHtml(option, writers, false);
							addedDirectories.add(longName);
						}
						i = scenarioDirectory.indexOf(File.separator, i + 1);
					}
					String longName = scenarioFile.getAbsolutePath();
					String shortName = longName.substring(scenariosRootDirectory.length() - 1, longName.length() - ".properties".length());
					debugMessage("shortName: '" + shortName + "'", writers);
					String option = "<option value=\"" + XmlUtils.encodeChars(longName) + "\"";
					if (paramExecute != null && paramExecute.equals(longName)) {
						option = option + " selected";
					}
					option = option + ">" + XmlUtils.encodeChars(shortName + " - " + properties.getProperty("scenario.description")) + "</option>";
					writeHtml(option, writers, false);
				}
			}
			writeHtml("</select>", writers, false);
			writeHtml(TD_CLOSING_TAG, writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TABLE_CLOSING_TAG, writers, false);

			writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", writers, false);
			// submit button
			writeHtml("<table style=\"float:left;height:50px\">", writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml("<td>&nbsp;</td>", writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TR_STARTING_TAG, writers, false);
			writeHtml("<td align=\"right\">", writers, false);
			writeHtml("<input type=\"submit\" name=\"submit\" value=\"start\" id=\"submit\">", writers, false);
			writeHtml(TD_CLOSING_TAG, writers, false);
			writeHtml(TR_CLOSING_TAG, writers, false);
			writeHtml(TABLE_CLOSING_TAG, writers, false);

			writeHtml("</form>", writers, false);
			writeHtml("<br clear=\"all\"/>", writers, false);
		}
	}

	public static void write(String html, String type, String method, Map<String, Object> writers, boolean scroll) {
		if (writers!=null) {
			String useBuffer = (String)writers.get("use" + type + "buffer");
			if (useBuffer.equals("start")) {
				useBuffer = "true";
				writers.put("use" + type + "buffer", useBuffer);
			} else if (useBuffer.equals("stop")) {
				Writer out = (Writer)writers.get("out");
				StringWriter buffer = (StringWriter)writers.get(type + "buffer");
				try {
					out.write(buffer.toString());
				} catch(IOException e) {
				}
				useBuffer = "false";
				writers.put("use" + type + "buffer", useBuffer);
			}
			Writer writer;
			if (useBuffer.equals("true")) {
				writer = (Writer)writers.get(type + "buffer");
			} else {
				writer = (Writer)writers.get("out");
			}
			if (method == null || LOG_LEVEL_ORDER.indexOf("[" + (String)writers.get("loglevel") + "]") <= LOG_LEVEL_ORDER.indexOf("[" + method + "]")) {
				try {
					writer.write(html + "\n");
					if (scroll && "true".equals(writers.get("autoscroll"))) {
						writer.write("<script type=\"text/javascript\"><!--\n");
						writer.write("scrollToBottom();\n");
						writer.write("--></script>\n");
					}
					writer.flush();
				} catch(IOException e) {
				}
			}
		}
	}

	public static void writeHtml(String html, Map<String, Object> writers, boolean scroll) {
		write(html, "html", null, writers, scroll);
	}

	public static void writeLog(String html, String method, Map<String, Object> writers, boolean scroll) {
		write(html, "log", method, writers, scroll);
	}

	public static void debugMessage(String message, Map<String, Object> writers) {
		String method = "debug";
		logger.debug(message);
		writeLog(XmlUtils.encodeChars(XmlUtils.replaceNonValidXmlCharacters(message)) + "<br/>", method, writers, false);
	}

	public static void debugPipelineMessage(String stepDisplayName, String message, String pipelineMessage, Map<String, Object> writers) {
		if (writers!=null) {
			String method = "pipeline messages";
			int messageCounter = ((Integer)writers.get("messagecounter")).intValue();
			messageCounter ++;

			writeLog("<div class='message container'>", method, writers, false);
			writeLog("<h4>Step '" + stepDisplayName + "'</h4>", method, writers, false);
			writeLog(writeCommands("messagebox" + messageCounter, true, null), method, writers, false);
			writeLog("<h5>" + XmlUtils.encodeChars(message) + "</h5>", method, writers, false);
			writeLog("<textarea cols='100' rows='10' id='messagebox" + messageCounter + "'>" + XmlUtils.encodeChars(XmlUtils.replaceNonValidXmlCharacters(pipelineMessage)) + "</textarea>", method, writers, false);
			writeLog("</div>", method, writers, false);

			writers.put("messagecounter", new Integer(messageCounter));
		}
	}

	public static void debugPipelineMessagePreparedForDiff(String stepDisplayName, String message, String pipelineMessage, Map<String, Object> writers) {
		if (writers!=null) {
			String method = "pipeline messages prepared for diff";
			int messageCounter = ((Integer)writers.get("messagecounter")).intValue();
			messageCounter ++;

			writeLog("<div class='message container'>", method, writers, false);
			writeLog("<h4>Step '" + stepDisplayName + "'</h4>", method, writers, false);
			writeLog(writeCommands("messagebox" + messageCounter, true, null), method, writers, false);
			writeLog("<h5>" + XmlUtils.encodeChars(message) + "</h5>", method, writers, false);
			writeLog("<textarea cols='100' rows='10' id='messagebox" + messageCounter + "'>" + XmlUtils.encodeChars(pipelineMessage) + "</textarea>", method, writers, false);
			writeLog("</div>", method, writers, false);

			writers.put("messagecounter", new Integer(messageCounter));
		}
	}

	public static void wrongPipelineMessage(String message, String pipelineMessage, Map<String, Object> writers) {
		if (writers!=null) {
			String method = "wrong pipeline messages";
			int messageCounter = ((Integer)writers.get("messagecounter")).intValue();
			messageCounter ++;

			writeLog("<div class='message container'>", method, writers, false);
			writeLog(writeCommands("messagebox" + messageCounter, true, null), method, writers, false);
			writeLog("<h5>" + XmlUtils.encodeChars(message) + "</h5>", method, writers, false);
			writeLog("<textarea cols='100' rows='10' id='messagebox" + messageCounter + "'>" + XmlUtils.encodeChars(XmlUtils.replaceNonValidXmlCharacters(pipelineMessage)) + "</textarea>", method, writers, false);
			writeLog("</div>", method, writers, false);

			writers.put("messagecounter", new Integer(messageCounter));
		}
	}

	public static void wrongPipelineMessage(String stepDisplayName, String message, String pipelineMessage, String pipelineMessageExpected, Map<String, Object> writers) {
		if (writers!=null) {
			String method = "wrong pipeline messages";
			int scenarioCounter = ((Integer)writers.get("scenariocounter")).intValue();
			String formName = "scenario" + scenarioCounter + "Wpm";
			String resultBoxId = formName + "ResultBox";
			String expectedBoxId = formName + "ExpectedBox";
			String diffBoxId = formName + "DiffBox";

			writeLog("<div class='error container'>", method, writers, false);
			writeLog("<form name='"+formName+"' action='saveResultToFile.jsp' method='post' target='saveResultWindow' accept-charset='UTF-8'>", method, writers, false);
			writeLog("<input type='hidden' name='iehack' value='&#9760;' />", method, writers, false); // http://stackoverflow.com/questions/153527/setting-the-character-encoding-in-form-submit-for-internet-explorer
			writeLog("<h4>Step '" + stepDisplayName + "'</h4>", method, writers, false);

			writeLog("<hr/>", method, writers, false);

			writeLog("<div class='resultContainer'>", method, writers, false);
			writeLog(writeCommands(resultBoxId, true, "<a href='javascript:void(0);' class='" + formName + "|saveResults'>save</a>"), method, writers, false);
			writeLog("<h5>Result (raw):</h5>", method, writers, false);
			writeLog("<textarea name='resultBox' id='"+resultBoxId+"'>" + XmlUtils.encodeChars(pipelineMessage) + "</textarea>", method, writers, false);
			writeLog("</div>", method, writers, false);

			writeLog("<div class='expectedContainer'>", method, writers, false);
			writeLog(writeCommands(expectedBoxId, true, null), method, writers, true);
			writeLog("<input type='hidden' name='expectedFileName' value='"+zeefVijlNeem+"' />", method, writers, false);
			writeLog("<input type='hidden' name='cmd' />", method, writers, false);
			writeLog("<h5>Expected (raw):</h5>", method, writers, false);
			writeLog("<textarea name='expectedBox' id='"+expectedBoxId+"'>" + XmlUtils.encodeChars(pipelineMessageExpected) + "</textarea>", method, writers, false);
			writeLog("</div>", method, writers, false);

			writeLog("<hr/>", method, writers, false);

			writeLog("<div class='differenceContainer'>", method, writers, false);
			String btn1 = "<a class=\"['"+resultBoxId+"','"+expectedBoxId+"']|indentCompare|"+diffBoxId+"\" href=\"javascript:void(0)\">compare</a>";
			String btn2 = "<a href='javascript:void(0);' class='" + formName + "|indentWindiff'>windiff</a>";
			writeLog(writeCommands(diffBoxId, false, btn1+btn2), method, writers, false);
			writeLog("<h5>Differences:</h5>", method, writers, false);
			writeLog("<pre id='"+diffBoxId+"' class='diffBox'></pre>", method, writers, false);
			writeLog("</div>", method, writers, false);

			String scenario_passed_failed = "scenario passed/failed";
			if (LOG_LEVEL_ORDER.indexOf(
					"[" + (String) writers.get("loglevel") + "]") == LOG_LEVEL_ORDER
							.indexOf("[" + scenario_passed_failed + "]")) {
				writeLog("<h5 hidden='true'>Difference description:</h5>",
						scenario_passed_failed, writers, false);
				writeLog(
						"<p class='diffMessage' hidden='true'>"
								+ XmlUtils.encodeChars(message) + "</p>",
						scenario_passed_failed, writers, true);
			} else {
				writeLog("<h5>Difference description:</h5>", method, writers,
						false);
				writeLog("<p class='diffMessage'>" + XmlUtils.encodeChars(message)
						+ "</p>", method, writers, true);
				writeLog("</form>", method, writers, false);
				writeLog("</div>", method, writers, false);
			}

			scenarioCounter++;
			writers.put("scenariocounter", new Integer(scenarioCounter));
		} else {
			if (silentOut!=null) {
				try {
					silentOut.write(message);
				} catch (IOException e) {
				}
			}
		}
	}

	public static void wrongPipelineMessagePreparedForDiff(String stepDisplayName, String pipelineMessagePreparedForDiff, String pipelineMessageExpectedPreparedForDiff, Map<String, Object> writers) {
		if (writers!=null) {
			String method = "wrong pipeline messages prepared for diff";
			int scenarioCounter = ((Integer)writers.get("scenariocounter")).intValue();
			int messageCounter = ((Integer)writers.get("messagecounter")).intValue();
			String formName = "scenario" + scenarioCounter + "Wpmpfd";
			String resultBoxId = formName + "ResultBox";
			String expectedBoxId = formName + "ExpectedBox";
			String diffBoxId = formName + "DiffBox";

			writeLog("<div class='error container'>", method, writers, false);
			writeLog("<form name='"+formName+"' action='saveResultToFile.jsp' method='post' target='saveResultWindow' accept-charset='UTF-8'>", method, writers, false);
			writeLog("<input type='hidden' name='iehack' value='&#9760;' />", method, writers, false); // http://stackoverflow.com/questions/153527/setting-the-character-encoding-in-form-submit-for-internet-explorer
			writeLog("<h4>Step '" + stepDisplayName + "'</h4>", method, writers, false);
			messageCounter ++;

			writeLog("<hr/>", method, writers, false);

			writeLog("<div class='resultContainer'>", method, writers, false);
			writeLog(writeCommands(resultBoxId, true, null), method, writers, false);
			writeLog("<h5>Result (prepared for diff):</h5>", method, writers, false);
			writeLog("<textarea name='resultBox' id='"+resultBoxId+"'>" + XmlUtils.encodeChars(pipelineMessagePreparedForDiff) + "</textarea>", method, writers, false);
			writeLog("</div>", method, writers, false);

			messageCounter++;
			writeLog("<div class='expectedContainer'>", method, writers, false);
			writeLog(writeCommands(expectedBoxId, true, null), method, writers, false);
			writeLog("<input type='hidden' name='expectedFileName' value='"+zeefVijlNeem+"' />", method, writers, false);
			writeLog("<input type='hidden' name='cmd' />", method, writers, false);
			writeLog("<h5>Expected (prepared for diff):</h5>", method, writers, false);
			writeLog("<textarea name='expectedBox' id='" + expectedBoxId + "'>" + XmlUtils.encodeChars(pipelineMessageExpectedPreparedForDiff) + "</textarea>", method, writers, false);
			writeLog("</div>", method, writers, false);

			writeLog("<hr/>", method, writers, false);

			messageCounter++;
			writeLog("<div class='differenceContainer'>", method, writers, false);

			String btn1 = "<a class=\"['"+resultBoxId+"','"+expectedBoxId+"']|indentCompare|"+diffBoxId+"\" href=\"javascript:void(0)\">compare</a>";
			String btn2 = "<a href='javascript:void(0);' class='" + formName + "|indentWindiff'>windiff</a>";
			writeLog(writeCommands(diffBoxId, false, btn1+btn2), method, writers, false);
			writeLog("<h5>Differences:</h5>", method, writers, false);
			writeLog("<pre id='"+diffBoxId+"' class='diffBox'></pre>", method, writers, false);
			writeLog("</div>", method, writers, false);

			writeLog("</form>", method, writers, false);
			writeLog("</div>", method, writers, false);

			writers.put("messagecounter", new Integer(messageCounter));
		}
	}

	private static String writeCommands(String target, boolean textArea, String customCommand) {
		String commands = "";

		commands += "<div class='commands'>";
		commands += "<span class='widthCommands'><a href='javascript:void(0);' class='" + target + "|widthDown'>-</a><a href='javascript:void(0);' class='" + target + "|widthExact'>width</a><a href='javascript:void(0);' class='" + target + "|widthUp'>+</a></span>";
		commands += "<span class='heightCommands'><a href='javascript:void(0);' class='" + target + "|heightDown'>-</a><a href='javascript:void(0);' class='" + target + "|heightExact'>height</a><a href='javascript:void(0);' class='" + target + "|heightUp'>+</a></span>";
		if (textArea) {
			commands += "<a href='javascript:void(0);' class='" + target + "|copy'>copy</a> ";
			commands += "<a href='javascript:void(0);' class='" + target + "|xmlFormat'>indent</a>";
		}
		if (customCommand != null) {
			commands += " " + customCommand;
		}
		commands += "</div>";


		return commands;
	}

	public static void stepPassedMessage(String message, Map<String, Object> writers) {
		String method = "step passed/failed";
		writeLog("<h3 class='passed'>" + XmlUtils.encodeChars(message) + "</h3>", method, writers, true);
	}

	public static void stepAutosavedMessage(String message, Map<String, Object> writers) {
		String method = "step passed/failed";
		writeLog("<h3 class='autosaved'>" + XmlUtils.encodeChars(message) + "</h3>", method, writers, true);
	}

	public static void stepFailedMessage(String message, Map<String, Object> writers) {
		String method = "step passed/failed";
		writeLog("<h3 class='failed'>" + XmlUtils.encodeChars(message) + "</h3>", method, writers, true);
	}

	public static void scenarioPassedMessage(String message, Map<String, Object> writers) {
		String method = "scenario passed/failed";
		writeLog("<h2 class='passed'>" + XmlUtils.encodeChars(message) + "</h2>", method, writers, true);
	}

	public static void scenarioAutosavedMessage(String message, Map<String, Object> writers) {
		String method = "scenario passed/failed";
		writeLog("<h2 class='autosaved'>" + XmlUtils.encodeChars(message) + "</h2>", method, writers, true);
	}

	public static void scenarioFailedMessage(String message, Map<String, Object> writers) {
		String method = "scenario failed";
		writeLog("<h2 class='failed'>" + XmlUtils.encodeChars(message) + "</h2>", method, writers, true);
	}

	public static void scenariosTotalMessage(String message, Map<String, Object> writers, Writer out, boolean silent) {
		if (silent) {
			try {
				out.write(message);
			} catch (IOException e) {
			}
		} else {
			String method = "totals";
			writeLog("<h1 class='total'>" + XmlUtils.encodeChars(message) + "</h1>", method, writers, true);
		}
	}

	public static void scenariosPassedTotalMessage(String message, Map<String, Object> writers, Writer out, boolean silent) {
		if (silent) {
			try {
				out.write(message);
			} catch (IOException e) {
			}
		} else {
			String method = "totals";
			writeLog("<h1 class='passed'>" + XmlUtils.encodeChars(message) + "</h1>", method, writers, true);
		}
	}

	public static void scenariosAutosavedTotalMessage(String message, Map<String, Object> writers, Writer out, boolean silent) {
		if (silent) {
			try {
				out.write(message);
			} catch (IOException e) {
			}
		} else {
			String method = "totals";
			writeLog("<h1 class='autosaved'>" + XmlUtils.encodeChars(message) + "</h1>", method, writers, true);
		}
	}

	public static void scenariosFailedTotalMessage(String message, Map<String, Object> writers, Writer out, boolean silent) {
		if (silent) {
			try {
				out.write(message);
			} catch (IOException e) {
			}
		} else {
			String method = "totals";
			writeLog("<h1 class='failed'>" + XmlUtils.encodeChars(message) + "</h1>", method, writers, true);
		}
	}

	public static void errorMessage(String message, Map<String, Object> writers) {
		String method = "error";
		writeLog("<h1 class='error'>" + XmlUtils.encodeChars(message) + "</h1>", method, writers, true);
		if (silentOut!=null) {
			try {
				silentOut.write(message);
			} catch (IOException e) {
			}
		}
	}

	public static void errorMessage(String message, Exception exception, Map<String, Object> writers) {
		errorMessage(message, writers);
		if (writers!=null) {
			String method = "error";
			Throwable throwable = exception;
			while (throwable != null) {
				StringWriter stringWriter = new StringWriter();
				PrintWriter printWriter = new PrintWriter(stringWriter);
				throwable.printStackTrace(printWriter);
				printWriter.close();
				int messageCounter = ((Integer)writers.get("messagecounter")).intValue();
				messageCounter++;
				writeLog("<div class='container'>", method, writers, false);
				writeLog(writeCommands("messagebox" + messageCounter, true, null), method, writers, false);
				writeLog("<h5>Stack trace:</h5>", method, writers, false);
				writeLog("<textarea cols='100' rows='10' id='messagebox" + messageCounter + "'>" + XmlUtils.encodeChars(XmlUtils.replaceNonValidXmlCharacters(stringWriter.toString())) + "</textarea>", method, writers, false);
				writeLog("</div>", method, writers, false);
				writers.put("messagecounter", new Integer(messageCounter));
				throwable = throwable.getCause();
			}
		}
	}

	public static String initScenariosRootDirectories(String realPath, String paramScenariosRootDirectory, List<String> scenariosRootDirectories, List<String> scenariosRootDescriptions, Map<String, Object> writers) {
		AppConstants appConstants = AppConstants.getInstance();
		String currentScenariosRootDirectory = null;
		if (realPath == null) {
			errorMessage("Could not read webapp real path", writers);
		} else {
			if (!realPath.endsWith(File.separator)) {
				realPath = realPath + File.separator;
			}
			Map<String, String> scenariosRoots = new HashMap<String, String>();
			Map<String, String> scenariosRootsBroken = new HashMap<String, String>();
			int j = 1;
			String directory = appConstants.getResolvedProperty("scenariosroot" + j + ".directory");
			String description = appConstants.getResolvedProperty("scenariosroot" + j + ".description");
			while (directory != null) {
				if (description == null) {
					errorMessage("Could not find description for root directory '" + directory + "'", writers);
				} else if (scenariosRoots.get(description) != null) {
					errorMessage("A root directory named '" + description + "' already exist", writers);
				} else {
					String parent = realPath;
					String m2eFileName = appConstants.getResolvedProperty("scenariosroot" + j + ".m2e.pom.properties");
					if (m2eFileName != null) {
						File m2eFile = new File(realPath, m2eFileName);
						if (m2eFile.exists()) {
							debugMessage("Read m2e pom.properties: " + m2eFileName, writers);
							Properties m2eProperties = readProperties(null, m2eFile, false, writers);
							parent = m2eProperties.getProperty("m2e.projectLocation");
							debugMessage("Use m2e parent: " + parent, writers);
						}
					}
					directory = getAbsolutePath(parent, directory, true);
					if (new File(directory).exists()) {
						debugMessage("directory for ["+description+"] exists: " + directory, writers);
						scenariosRoots.put(description, directory);
					} else {
						debugMessage("directory ["+directory+"] for ["+description+"] does not exist, parent ["+parent+"]", writers);
						scenariosRootsBroken.put(description, directory);
					}
				}
				j++;
				directory = appConstants.getResolvedProperty("scenariosroot" + j + ".directory");
				description = appConstants.getResolvedProperty("scenariosroot" + j + ".description");
			}
			TreeSet<String> treeSet = new TreeSet<String>(new CaseInsensitiveComparator());
			treeSet.addAll(scenariosRoots.keySet());
			Iterator<String> iterator = treeSet.iterator();
			while (iterator.hasNext()) {
				description = (String)iterator.next();
				scenariosRootDescriptions.add(description);
				scenariosRootDirectories.add(scenariosRoots.get(description));
			}
			treeSet.clear();
			treeSet.addAll(scenariosRootsBroken.keySet());
			iterator = treeSet.iterator();
			while (iterator.hasNext()) {
				description = (String)iterator.next();
				scenariosRootDescriptions.add("X " + description);
				scenariosRootDirectories.add(scenariosRootsBroken.get(description));
			}
			debugMessage("Read scenariosrootdirectory parameter", writers);
			debugMessage("Get current scenarios root directory", writers);
			if (paramScenariosRootDirectory == null || paramScenariosRootDirectory.equals("")) {
				String scenariosRootDefault = appConstants.getResolvedProperty("scenariosroot.default");
				if (scenariosRootDefault != null) {
					currentScenariosRootDirectory = scenariosRoots.get(scenariosRootDefault);
				}
				if (currentScenariosRootDirectory == null
						&& scenariosRootDirectories.size() > 0) {
					currentScenariosRootDirectory = (String)scenariosRootDirectories.get(0);
				}
			} else {
				currentScenariosRootDirectory = paramScenariosRootDirectory;
			}
		}
		return currentScenariosRootDirectory;
	}

	public static List<File> readScenarioFiles(AppConstants appConstants, String scenariosDirectory, Map<String, Object> writers) {
		List<File> scenarioFiles = new ArrayList<File>();
		debugMessage("List all files in directory '" + scenariosDirectory + "'", writers);
		File[] files = new File(scenariosDirectory).listFiles();
		if (files == null) {
			debugMessage("Could not read files from directory '" + scenariosDirectory + "'", writers);
		} else {
			debugMessage("Sort files", writers);
			Arrays.sort(files);
			debugMessage("Filter out property files containing a 'scenario.description' property", writers);
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.getName().endsWith(".properties")) {
					Properties properties = readProperties(appConstants, file, writers);
					if (properties != null && properties.get("scenario.description") != null) {
						String active = properties.getProperty("scenario.active", "true");
						String unstable = properties.getProperty("adapter.unstable", "false");
						if (active.equalsIgnoreCase("true") && unstable.equalsIgnoreCase("false")) {
							scenarioFiles.add(file);
						}
					}
				} else if (file.isDirectory() && (!file.getName().equals("CVS"))) {
					scenarioFiles.addAll(readScenarioFiles(appConstants, file.getAbsolutePath(), writers));
				}
			}
		}
		debugMessage(scenarioFiles.size() + " scenario files found", writers);
		return scenarioFiles;
	}

	public static Properties readProperties(AppConstants appConstants, File propertiesFile, Map<String, Object> writers) {
		return readProperties(appConstants, propertiesFile, true, writers);
	}

	public static Properties readProperties(AppConstants appConstants, File propertiesFile, boolean root, Map<String, Object> writers) {
		String directory = new File(propertiesFile.getAbsolutePath()).getParent();
		Properties properties = new Properties();
		FileInputStream fileInputStreamPropertiesFile = null;
		try {
			fileInputStreamPropertiesFile = new FileInputStream(propertiesFile);
			properties.load(fileInputStreamPropertiesFile);
			fileInputStreamPropertiesFile.close();
			fileInputStreamPropertiesFile = null;
			Properties includedProperties = new Properties();
			int i = 0;
			String includeFilename = properties.getProperty("include");
			if (includeFilename == null) {
				i++;
				includeFilename = properties.getProperty("include" + i);
			}
			while (includeFilename != null) {
				debugMessage("Load include file: " + includeFilename, writers);
				File includeFile = new File(getAbsolutePath(directory, includeFilename));
				Properties includeProperties = readProperties(appConstants, includeFile, false, writers);
				includedProperties.putAll(includeProperties);
				i++;
				includeFilename = properties.getProperty("include" + i);
			}
			properties.putAll(includedProperties);
			if (root) {
				properties.putAll(appConstants);
				for (Object key : properties.keySet()) {
					properties.put(key, StringResolver.substVars((String)properties.get(key), properties));
				}
				addAbsolutePathProperties(directory, properties);
			}
			debugMessage(properties.size() + " properties found", writers);
		} catch(Exception e) {
			properties = null;
			errorMessage("Could not read properties file: " + e.getMessage(), e, writers);
			if (fileInputStreamPropertiesFile != null) {
				try {
					fileInputStreamPropertiesFile.close();
				} catch(Exception e2) {
					errorMessage("Could not close file '" + propertiesFile.getAbsolutePath() + "': " + e2.getMessage(), e, writers);
				}
			}
		}
		return properties;
	}

	public static String getAbsolutePath(String parent, String child) {
		return getAbsolutePath(parent, child, false);
	}

	/**
	 * Returns the absolute pathname for the child pathname. The parent pathname
	 * is used as a prefix when the child pathname is an not absolute.
	 *
	 * @param parent  the parent pathname to use
	 * @param child   the child pathname to convert to a absolute pathname
	 */
	public static String getAbsolutePath(String parent, String child,
			boolean addFileSeparator) {
		File result;
		File file = new File(child);
		if (file.isAbsolute()) {
			result = file;
		} else {
			result = new File(parent, child);
		}
		String absPath = FilenameUtils.normalize(result.getAbsolutePath());
		if (addFileSeparator) {
			return absPath + File.separator;
		} else {
			return absPath;
		}
	}

	public static void addAbsolutePathProperties(String propertiesDirectory, Properties properties) {
		Properties absolutePathProperties = new Properties();
		Iterator<?> iterator = properties.keySet().iterator();
		while (iterator.hasNext()) {
			String property = (String)iterator.next();
			if(property.equalsIgnoreCase("configurations.directory"))
				continue;

			if (property.endsWith(".read") || property.endsWith(".write")
					|| property.endsWith(".directory")
					|| property.endsWith(".filename")
					|| property.endsWith(".valuefile")
					|| property.endsWith(".valuefileinputstream")) {
				String absolutePathProperty = property + ".absolutepath";
				String value = getAbsolutePath(propertiesDirectory, (String)properties.get(property));
				if (value != null) {
					absolutePathProperties.put(absolutePathProperty, value);
				}
			}
		}
		properties.putAll(absolutePathProperties);
	}

	public static List<String> getSteps(Properties properties, Map<String, Object> writers) {
		List<String> steps = new ArrayList<String>();
		int i = 1;
		boolean lastStepFound = false;
		while (!lastStepFound) {
			boolean stepFound = false;
			Enumeration<?> enumeration = properties.propertyNames();
			while (enumeration.hasMoreElements()) {
				String key = (String)enumeration.nextElement();
				if (key.startsWith("step" + i + ".") && (key.endsWith(".read") || key.endsWith(".write") || (allowReadlineSteps && key.endsWith(".readline")) || key.endsWith(".writeline"))) {
					if (!stepFound) {
						steps.add(key);
						stepFound = true;
						debugMessage("Added step '" + key + "'", writers);
					} else {
						errorMessage("More than one step" + i + " properties found, already found '" + steps.get(steps.size() - 1) + "' before finding '" + key + "'", writers);
					}
				}
			}
			if (!stepFound) {
				lastStepFound = true;
			}
			i++;
		}
		debugMessage(steps.size() + " steps found", writers);
		return steps;
	}

	public static boolean closeQueues(Map<String, Queue> queues, Properties properties, Map<String, Object> writers, String correlationId) {
		boolean remainingMessagesFound = false;
		Iterator<String> iterator;
		debugMessage("Close jms senders", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.jms.JmsSender".equals(properties.get(queueName + ".className"))) {
				JmsSender jmsSender = (JmsSender)((Map<?, ?>)queues.get(queueName)).get("jmsSender");
				jmsSender.close();
				debugMessage("Closed jms sender '" + queueName + "'", writers);
			}
		}
		debugMessage("Close jms listeners", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.jms.JmsListener".equals(properties.get(queueName + ".className"))) {
				PullingJmsListener pullingJmsListener = (PullingJmsListener)((Map<?, ?>)queues.get(queueName)).get("jmsListener");
				if (jmsCleanUp(queueName, pullingJmsListener, writers)) {
					remainingMessagesFound = true;
				}
				pullingJmsListener.close();
				debugMessage("Closed jms listener '" + queueName + "'", writers);
			}
		}
		debugMessage("Close jdbc connections", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String name = (String)iterator.next();
			if ("nl.nn.adapterframework.jdbc.FixedQuerySender".equals(properties.get(name + ".className"))) {
				Map<?, ?> querySendersInfo = (Map<?, ?>)queues.get(name);
				FixedQuerySender prePostFixedQuerySender = (FixedQuerySender)querySendersInfo.get("prePostQueryFixedQuerySender");
				if (prePostFixedQuerySender != null) {
					try {
						/* Check if the preResult and postResult are not equal. If so, then there is a
						 * database change that has not been read in the scenario.
						 * So set remainingMessagesFound to true and show the entry.
						 * (see also executeFixedQuerySenderRead() )
						 */
						String preResult = (String)querySendersInfo.get("prePostQueryResult");
						PipeLineSession session = new PipeLineSession();
						session.put(PipeLineSession.correlationIdKey, correlationId);
						String postResult = prePostFixedQuerySender.sendMessageOrThrow(TESTTOOL_DUMMY_MESSAGE, session).asString();
						if (!preResult.equals(postResult)) {

							String message = null;
							FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
							try {
								message = readQueryFixedQuerySender.sendMessageOrThrow(TESTTOOL_DUMMY_MESSAGE, session).asString();
							} catch(TimeoutException e) {
								errorMessage("Time out on execute query for '" + name + "': " + e.getMessage(), e, writers);
							} catch(IOException | SenderException e) {
								errorMessage("Could not execute query for '" + name + "': " + e.getMessage(), e, writers);
							}
							if (message != null) {
								wrongPipelineMessage("Found remaining message on '" + name + "'", message, writers);
							}

							remainingMessagesFound = true;

						}
						prePostFixedQuerySender.close();
					} catch(TimeoutException e) {
						errorMessage("Time out on close (pre/post) '" + name + "': " + e.getMessage(), e, writers);
					} catch(IOException | SenderException e) {
						errorMessage("Could not close (pre/post) '" + name + "': " + e.getMessage(), e, writers);
					}
				}
				FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
				readQueryFixedQuerySender.close();
			}
		}

		debugMessage("Close autoclosables", writers);
		for(String queueName : queues.keySet()) {
			Map<String, Object> value = queues.get(queueName);
			if(value instanceof QueueWrapper) {
				QueueWrapper queue = (QueueWrapper) value;
				SenderThread senderThread = queue.getSenderThread();
				if (senderThread != null) {
					debugMessage("Found remaining SenderThread", writers);
					SenderException senderException = senderThread.getSenderException();
					if (senderException != null) {
						errorMessage("Found remaining SenderException: " + senderException.getMessage(), senderException, writers);
					}
					TimeoutException timeOutException = senderThread.getTimeOutException();
					if (timeOutException != null) {
						errorMessage("Found remaining TimeOutException: " + timeOutException.getMessage(), timeOutException, writers);
					}
					String message = senderThread.getResponse();
					if (message != null) {
						wrongPipelineMessage("Found remaining message on '" + queueName + "'", message, writers);
					}
				}
				ListenerMessageHandler listenerMessageHandler = queue.getMessageHandler();
				if (listenerMessageHandler != null) {
					ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage();
					while (listenerMessage != null) {
						String message = listenerMessage.getMessage();
						wrongPipelineMessage("Found remaining request message on '" + queueName + "'", message, writers);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getRequestMessage();
					}
					listenerMessage = listenerMessageHandler.getResponseMessage();
					while (listenerMessage != null) {
						String message = listenerMessage.getMessage();
						wrongPipelineMessage("Found remaining response message on '" + queueName + "'", message, writers);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getResponseMessage();
					}
				}

				try {
					queue.close();
					debugMessage("Closed queue '" + queueName + "'", writers);
				} catch(Exception e) {
					errorMessage("Could not close '" + queueName + "': " + e.getMessage(), e, writers);
				}
			}
		}

		return remainingMessagesFound;
	}

	public static boolean jmsCleanUp(String queueName, PullingJmsListener pullingJmsListener, Map<String, Object> writers) {
		boolean remainingMessagesFound = false;
		debugMessage("Check for remaining messages on '" + queueName + "'", writers);
		long oldTimeOut = pullingJmsListener.getTimeOut();
		pullingJmsListener.setTimeOut(10);
		boolean empty = false;
		while (!empty) {
			javax.jms.Message rawMessage = null;
			Message message = null;
			Map<String, Object> threadContext = null;
			try {
				threadContext = pullingJmsListener.openThread();
				rawMessage = pullingJmsListener.getRawMessage(threadContext);
				if (rawMessage != null) {
					message = pullingJmsListener.extractMessage(rawMessage, threadContext);
					remainingMessagesFound = true;
					if (message == null) {
						errorMessage("Could not translate raw message from jms queue '" + queueName + "'", writers);
					} else {
						wrongPipelineMessage("Found remaining message on '" + queueName + "'", message.asString(), writers);
					}
				}
			} catch(ListenerException | IOException e) {
				errorMessage("ListenerException on jms clean up '" + queueName + "': " + e.getMessage(), e, writers);
			} finally {
				if (threadContext != null) {
					try {
						pullingJmsListener.closeThread(threadContext);
					} catch(ListenerException e) {
						errorMessage("Could not close thread on jms listener '" + queueName + "': " + e.getMessage(), e, writers);
					}
				}
			}
			if (rawMessage == null) {
				empty = true;
			}
		}
		pullingJmsListener.setTimeOut((int) oldTimeOut);
		return remainingMessagesFound;
	}

	private static int executeJmsSenderWrite(String stepDisplayName, Map<String, Queue> queues, Map<String, Object> writers, String queueName, String fileContent, String correlationId) {
		int result = RESULT_ERROR;

		Map<?, ?> jmsSenderInfo = (Map<?, ?>)queues.get(queueName);
		JmsSender jmsSender = (JmsSender)jmsSenderInfo.get("jmsSender");
		try {
			String providedCorrelationId = null;
			String useCorrelationIdFrom = (String)jmsSenderInfo.get("useCorrelationIdFrom");
			if (useCorrelationIdFrom != null) {
				Map<?, ?> listenerInfo = (Map<?, ?>)queues.get(useCorrelationIdFrom);
				if (listenerInfo == null) {
					errorMessage("Could not find listener '" + useCorrelationIdFrom + "' to use correlation id from", writers);
				} else {
					providedCorrelationId = (String)listenerInfo.get("correlationId");
					if (providedCorrelationId == null) {
						errorMessage("Could not find correlation id from listener '" + useCorrelationIdFrom + "'", writers);
					}
				}
			}
			if (providedCorrelationId == null) {
				providedCorrelationId = (String)jmsSenderInfo.get("jmsCorrelationId");
			}
			if (providedCorrelationId == null) {
				providedCorrelationId = correlationId;
			}
			jmsSender.sendMessageOrThrow(new nl.nn.adapterframework.stream.Message(fileContent), null);
			debugPipelineMessage(stepDisplayName, "Successfully written to '" + queueName + "':", fileContent, writers);
			result = RESULT_OK;
		} catch(TimeoutException e) {
			errorMessage("Time out sending jms message to '" + queueName + "': " + e.getMessage(), e, writers);
		} catch(SenderException e) {
			errorMessage("Could not send jms message to '" + queueName + "': " + e.getMessage(), e, writers);
		}

		return result;
	}

	private static int executeQueueWrite(String stepDisplayName, Map<String, Queue> queues, Map<String, Object> writers, String queueName, String fileContent, String correlationId, Map<String, Object> xsltParameters) {
		Queue queue = queues.get(queueName);
		if (queue==null) {
			errorMessage("Property '" + queueName + ".className' not found or not valid", writers);
			return RESULT_ERROR;
		}
		int result = RESULT_ERROR;
		try {
			result = queue.executeWrite(stepDisplayName, fileContent, correlationId, xsltParameters);
			if (result == RESULT_OK) {
				debugPipelineMessage(stepDisplayName, "Successfully wrote message to '" + queueName + "':", fileContent, writers);
				logger.debug("Successfully wrote message to '" + queueName + "'");
			}
		} catch(TimeoutException e) {
			errorMessage("Time out sending message to '" + queueName + "': " + e.getMessage(), e, writers);
		} catch(Exception e) {
			errorMessage("Could not send message to '" + queueName + "' ("+e.getClass().getSimpleName()+"): " + e.getMessage(), e, writers);
		}
		return result;
	}


	private static int executeJmsListenerRead(String step, String stepDisplayName, Properties properties, Map<String, Queue> queues, Map<String, Object> writers, String queueName, String fileName, String fileContent) {
		int result = RESULT_ERROR;

		Map jmsListenerInfo = (Map)queues.get(queueName);
		PullingJmsListener pullingJmsListener = (PullingJmsListener)jmsListenerInfo.get("jmsListener");
		Map threadContext = null;
		Message message = null;
		try {
			threadContext = pullingJmsListener.openThread();
			javax.jms.Message rawMessage = pullingJmsListener.getRawMessage(threadContext);
			if (rawMessage != null) {
				message = pullingJmsListener.extractMessage(rawMessage, threadContext);
				String correlationId = pullingJmsListener.getIdFromRawMessage(rawMessage, threadContext);
				jmsListenerInfo.put("correlationId", correlationId);
			}
		} catch(ListenerException e) {
			if (!"".equals(fileName)) {
				errorMessage("Could not read jms message from '" + queueName + "': " + e.getMessage(), e, writers);
			}
		} finally {
			if (threadContext != null) {
				try {
					pullingJmsListener.closeThread(threadContext);
				} catch(ListenerException e) {
					errorMessage("Could not close thread on jms listener '" + queueName + "': " + e.getMessage(), e, writers);
				}
			}
		}

		if (message == null || message.isEmpty()) {
			if ("".equals(fileName)) {
				result = RESULT_OK;
			} else {
				errorMessage("Could not read jms message (null returned)", writers);
			}
		} else {
			try {
				result = compareResult(step, stepDisplayName, fileName, fileContent, message.asString(), properties, writers, queueName);
			} catch (IOException e) {
				errorMessage("Could not convert jms message from '" + queueName + "' to string: " + e.getMessage(), e, writers);
			}
		}

		return result;
	}


	private static int executeQueueRead(String step, String stepDisplayName, Properties properties, Map<String, Queue> queues, Map<String, Object> writers, String queueName, String fileName, String fileContent) {
		int result = RESULT_ERROR;

		Queue queue = queues.get(queueName);
		if (queue==null) {
			errorMessage("Property '" + queueName + ".className' not found or not valid", writers);
			return RESULT_ERROR;
		}
		try {
			String message = queue.executeRead(step, stepDisplayName, properties, fileName, fileContent);
			if (message == null) {
				if ("".equals(fileName)) {
					result = RESULT_OK;
				} else {
					errorMessage("Could not read from ["+queueName+"] (null returned)", writers);
				}
			} else {
				if ("".equals(fileName)) {
					debugPipelineMessage(stepDisplayName, "Unexpected message read from '" + queueName + "':", message, writers);
				} else {
					result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties, writers, queueName);
				}
			}
		} catch (Exception e) {
			errorMessage("Could not read from ["+queueName+"] ("+e.getClass().getSimpleName()+"): " + e.getMessage(), e, writers);
		}

		return result;
	}


	private static int executeJavaListenerOrWebServiceListenerRead(String step, String stepDisplayName, Properties properties, Map<String, Queue> queues, Map<String, Object> writers, String queueName, String fileName, String fileContent, int parameterTimeout) {
		int result = RESULT_ERROR;

		Map listenerInfo = (Map)queues.get(queueName);
		ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)listenerInfo.get("listenerMessageHandler");
		if (listenerMessageHandler == null) {
			errorMessage("No ListenerMessageHandler found", writers);
		} else {
			String message = null;
			ListenerMessage listenerMessage = null;
			Long timeout = Long.parseLong(""+parameterTimeout);
			try {
				timeout = Long.parseLong((String) properties.get(queueName + ".timeout"));
				debugMessage("Timeout set to '" + timeout + "'", writers);
			} catch (Exception e) {
			}
			try {
				listenerMessage = listenerMessageHandler.getRequestMessage(timeout);
			} catch (TimeoutException e) {
				errorMessage("Could not read listenerMessageHandler message (timeout of ["+parameterTimeout+"] reached)", writers);
				return RESULT_ERROR;
			}

			if (listenerMessage != null) {
				message = listenerMessage.getMessage();
				listenerInfo.put("listenerMessage", listenerMessage);
			}
			if (message == null) {
				if ("".equals(fileName)) {
					result = RESULT_OK;
				} else {
					errorMessage("Could not read listenerMessageHandler message (null returned)", writers);
				}
			} else {
				if ("".equals(fileName)) {
					debugPipelineMessage(stepDisplayName, "Unexpected message read from '" + queueName + "':", message, writers);
				} else {
					result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties, writers, queueName);
					if (result!=RESULT_OK) {
						// Send a clean up reply because there is probably a
						// thread waiting for a reply
						Map<?, ?> context = new HashMap<Object, Object>();
						listenerMessage = new ListenerMessage(TESTTOOL_CLEAN_UP_REPLY, context);
						listenerMessageHandler.putResponseMessage(listenerMessage);
					}
				}
			}
		}

		return result;
	}

	private static int executeFixedQuerySenderRead(String step, String stepDisplayName, Properties properties, Map<String, Queue> queues, Map<String, Object> writers, String queueName, String fileName, String fileContent, String correlationId) {
		int result = RESULT_ERROR;

		Map querySendersInfo = (Map)queues.get(queueName);
		Integer waitBeforeRead = (Integer)querySendersInfo.get("readQueryWaitBeforeRead");

		if (waitBeforeRead != null) {
			try {
				Thread.sleep(waitBeforeRead.intValue());
			} catch(InterruptedException e) {
			}
		}
		boolean newRecordFound = true;
		FixedQuerySender prePostFixedQuerySender = (FixedQuerySender)querySendersInfo.get("prePostQueryFixedQuerySender");
		if (prePostFixedQuerySender != null) {
			try {
				String preResult = (String)querySendersInfo.get("prePostQueryResult");
				debugPipelineMessage(stepDisplayName, "Pre result '" + queueName + "':", preResult, writers);
				PipeLineSession session = new PipeLineSession();
				session.put(PipeLineSession.correlationIdKey, correlationId);
				String postResult = prePostFixedQuerySender.sendMessageOrThrow(TESTTOOL_DUMMY_MESSAGE, session).asString();
				debugPipelineMessage(stepDisplayName, "Post result '" + queueName + "':", postResult, writers);
				if (preResult.equals(postResult)) {
					newRecordFound = false;
				}
				/* Fill the preResult with postResult, so closeQueues is able to determine if there
				 * are remaining messages left.
				 */
				querySendersInfo.put("prePostQueryResult", postResult);
			} catch(TimeoutException e) {
				errorMessage("Time out on execute query for '" + queueName + "': " + e.getMessage(), e, writers);
			} catch(IOException | SenderException e) {
				errorMessage("Could not execute query for '" + queueName + "': " + e.getMessage(), e, writers);
			}
		}
		String message = null;
		if (newRecordFound) {
			FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
			try {
				PipeLineSession session = new PipeLineSession();
				session.put(PipeLineSession.correlationIdKey, correlationId);
				message = readQueryFixedQuerySender.sendMessageOrThrow(TESTTOOL_DUMMY_MESSAGE, session).asString();
			} catch(TimeoutException e) {
				errorMessage("Time out on execute query for '" + queueName + "': " + e.getMessage(), e, writers);
			} catch(IOException | SenderException e) {
				errorMessage("Could not execute query for '" + queueName + "': " + e.getMessage(), e, writers);
			}
		}
		if (message == null) {
			if ("".equals(fileName)) {
				result = RESULT_OK;
			} else {
				errorMessage("Could not read jdbc message (null returned) or no new message found (pre result equals post result)", writers);
			}
		} else {
			if ("".equals(fileName)) {
				debugPipelineMessage(stepDisplayName, "Unexpected message read from '" + queueName + "':", message, writers);
			} else {
				result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties, writers, queueName);
			}
		}

		return result;
	}

	public static int executeStep(String step, Properties properties, String stepDisplayName, Map<String, Queue> queues, Map<String, Object> writers, int parameterTimeout, String correlationId) {
		int stepPassed = RESULT_ERROR;
		String fileName = properties.getProperty(step);
		String fileNameAbsolutePath = properties.getProperty(step + ".absolutepath");
		int i = step.indexOf('.');
		String queueName;
		String fileContent;
		// vul globale var
		zeefVijlNeem = fileNameAbsolutePath;

		//inlezen file voor deze stap
		if ("".equals(fileName)) {
			errorMessage("No file specified for step '" + step + "'", writers);
		} else {
			if (step.endsWith("readline") || step.endsWith("writeline")) {
				fileContent = fileName;
			} else {
				if (fileName.endsWith("ignore")) {
					debugMessage("creating dummy expected file for filename '"+fileName+"'", writers);
					fileContent = "ignore";
				} else {
					debugMessage("Read file " + fileName, writers);
					fileContent = readFile(fileNameAbsolutePath, writers);
				}
			}
			if (fileContent == null) {
				errorMessage("Could not read file '" + fileName + "'", writers);
			} else {
				queueName = step.substring(i + 1, step.lastIndexOf("."));
				if (step.endsWith(".read") || (allowReadlineSteps && step.endsWith(".readline"))) {
					if ("nl.nn.adapterframework.jms.JmsListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeJmsListenerRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent);
					} else 	if ("nl.nn.adapterframework.jdbc.FixedQuerySender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeFixedQuerySenderRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent, correlationId);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeJavaListenerOrWebServiceListenerRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent, parameterTimeout);
					} else if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeJavaListenerOrWebServiceListenerRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent, parameterTimeout);
					} else if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))) {
						Map<String, Object> xsltParameters = createParametersMapFromParamProperties(properties, step, writers, false, null);
						stepPassed = executeQueueWrite(stepDisplayName, queues, writers, queueName, fileContent, correlationId, xsltParameters); // XsltProviderListener has .read and .write reversed
					} else {
						stepPassed = executeQueueRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent);
					}
				} else {
					String resolveProperties = properties.getProperty("scenario.resolveProperties");

					if( resolveProperties == null || !resolveProperties.equalsIgnoreCase("false") ){
						AppConstants appConstants = AppConstants.getInstance();
						fileContent = StringResolver.substVars(fileContent, appConstants);
					}

					if ("nl.nn.adapterframework.jms.JmsSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeJmsSenderWrite(stepDisplayName, queues, writers, queueName, fileContent, correlationId);
					} else if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeQueueRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent);  // XsltProviderListener has .read and .write reversed
					} else {
						stepPassed = executeQueueWrite(stepDisplayName, queues, writers, queueName, fileContent, correlationId, null);
					}
				}
			}
		}

		return stepPassed;
	}

	public static String readFile(String fileName, Map<String, Object> writers) {
		String result = null;
		String encoding = null;
		if (fileName.endsWith(".xml") || fileName.endsWith(".wsdl")) {
			// Determine the encoding the XML way but don't use an XML parser to
			// read the file and transform it to a string to prevent changes in
			// formatting and prevent adding an xml declaration where this is
			// not present in the file. For example, when using a
			// WebServiceSender to send a message to a WebServiceListener the
			// xml message must not contain an xml declaration.
			try (InputStream in = new FileInputStream(fileName)) {
				XMLInputFactory factory = XMLInputFactory.newInstance();
				XMLStreamReader parser = factory.createXMLStreamReader(in);
				encoding = parser.getEncoding();
				parser.close();
			} catch (IOException | XMLStreamException e) {
				errorMessage("Could not determine encoding for file '" + fileName + "': " + e.getMessage(), e, writers);
			}
		} else if (fileName.endsWith(".utf8") || fileName.endsWith(".json")) {
			encoding = "UTF-8";
		} else {
			encoding = "ISO-8859-1";
		}
		if (encoding != null) {
			Reader inputStreamReader = null;
			try {
				StringBuffer stringBuffer = new StringBuffer();
				inputStreamReader = StreamUtil.getCharsetDetectingInputStreamReader(new FileInputStream(fileName), encoding);
				char[] cbuf = new char[4096];
				int len = inputStreamReader.read(cbuf);
				while (len != -1) {
					stringBuffer.append(cbuf, 0, len);
					len = inputStreamReader.read(cbuf);
				}
				result = stringBuffer.toString();
			} catch (Exception e) {
				errorMessage("Could not read file '" + fileName + "': " + e.getMessage(), e, writers);
			} finally {
				if (inputStreamReader != null) {
					try {
						inputStreamReader.close();
					} catch(Exception e) {
						errorMessage("Could not close file '" + fileName + "': " + e.getMessage(), e, writers);
					}
				}
			}
		}
		return result;
	}

	// Used by saveResultToFile.jsp
	public static void windiff(ServletContext application, HttpServletRequest request, String expectedFileName, String result, String expected) throws IOException, SenderException {
		AppConstants appConstants = AppConstants.getInstance();
		String windiffCommand = appConstants.getResolvedProperty("larva.windiff.command");
		if (windiffCommand == null) {
			String realPath = application.getRealPath("/iaf/");
			List<String> scenariosRootDirectories = new ArrayList<String>();
			List<String> scenariosRootDescriptions = new ArrayList<String>();
			String currentScenariosRootDirectory = TestTool.initScenariosRootDirectories(
					realPath,
					null, scenariosRootDirectories,
					scenariosRootDescriptions, null);
			windiffCommand = currentScenariosRootDirectory + "..\\..\\IbisAlgemeenWasbak\\WinDiff\\WinDiff.Exe";
		}
		File tempFileResult = writeTempFile(expectedFileName, result);
		File tempFileExpected = writeTempFile(expectedFileName, expected);
		String command = windiffCommand + " " + tempFileResult + " " + tempFileExpected;
		ProcessUtil.executeCommand(command);
	}

	private static File writeTempFile(String originalFileName, String content) throws IOException {
		String encoding = getEncoding(originalFileName, content);

		String baseName = FileUtils.getBaseName(originalFileName);
		String extensie = FileUtils.getFileNameExtension(originalFileName);

		File tempFile = null;
		tempFile = File.createTempFile("ibistesttool", "."+extensie);
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

	public static int compareResult(String step, String stepDisplayName, String fileName, String expectedResult, String actualResult, Properties properties, Map<String, Object> writers, String queueName) {
		if (fileName.endsWith("ignore")) {
			debugMessage("ignoring compare for filename '"+fileName+"'", writers);
			return RESULT_OK;
		}

		int ok = RESULT_ERROR;
		String printableExpectedResult;
		String printableActualResult;
		String diffType = properties.getProperty(step + ".diffType");
		if ((diffType != null && diffType.equals(".json")) || (diffType == null && fileName.endsWith(".json"))) {
			try {
				printableExpectedResult = Misc.jsonPretty(expectedResult);
			} catch (JsonException e) {
				debugMessage("Could not prettify Json: "+e.getMessage(), writers);
				printableExpectedResult = expectedResult;
			}
			try {
				printableActualResult = Misc.jsonPretty(actualResult);
			} catch (JsonException e) {
				debugMessage("Could not prettify Json: "+e.getMessage(), writers);
				printableActualResult = actualResult;
			}
		} else {
			printableExpectedResult = XmlUtils.replaceNonValidXmlCharacters(expectedResult);
			printableActualResult = XmlUtils.replaceNonValidXmlCharacters(actualResult);
		}

		// Map all identifier based properties once
		HashMap<String, HashMap<String, HashMap<String, String>>> ignoreMap = mapPropertiesToIgnores(properties);

		String preparedExpectedResult = prepareResultForCompare(printableExpectedResult, properties, ignoreMap, writers);
		String preparedActualResult = prepareResultForCompare(printableActualResult, properties, ignoreMap, writers);


		if ((diffType != null && (diffType.equals(".xml") || diffType.equals(".wsdl")))
				|| (diffType == null && (fileName.endsWith(".xml") || fileName.endsWith(".wsdl")))) {
			// xml diff
			Diff diff = null;
			boolean identical = false;
			Exception diffException = null;
			try {
				diff = new Diff(preparedExpectedResult, preparedActualResult);
				identical = diff.identical();
			} catch(Exception e) {
				diffException = e;
			}
			if (identical) {
				ok = RESULT_OK;
				debugMessage("Strings are identical", writers);
				debugPipelineMessage(stepDisplayName, "Result", printableActualResult, writers);
				debugPipelineMessagePreparedForDiff(stepDisplayName, "Result as prepared for diff", preparedActualResult, writers);
			} else {
				debugMessage("Strings are not identical", writers);
				String message;
				if (diffException == null) {
					message = diff.toString();
				} else {
					message = "Exception during XML diff: " + diffException.getMessage();
					errorMessage("Exception during XML diff: ", diffException, writers);
				}
				wrongPipelineMessage(stepDisplayName, message, printableActualResult, printableExpectedResult, writers);
				wrongPipelineMessagePreparedForDiff(stepDisplayName, preparedActualResult, preparedExpectedResult, writers);
				if (autoSaveDiffs) {
					String filenameAbsolutePath = (String)properties.get(step + ".absolutepath");
					debugMessage("Copy actual result to ["+filenameAbsolutePath+"]", writers);
					try {
						org.apache.commons.io.FileUtils.writeStringToFile(new File(filenameAbsolutePath), actualResult);
					} catch (IOException e) {
					}
					ok = RESULT_AUTOSAVED;
				}
			}
		} else {
			// txt diff
			String formattedPreparedExpectedResult = formatString(preparedExpectedResult, writers);
			String formattedPreparedActualResult = formatString(preparedActualResult, writers);
			if (formattedPreparedExpectedResult.equals(formattedPreparedActualResult)) {
				ok = RESULT_OK;
				debugMessage("Strings are identical", writers);
				debugPipelineMessage(stepDisplayName, "Result", printableActualResult, writers);
				debugPipelineMessagePreparedForDiff(stepDisplayName, "Result as prepared for diff", preparedActualResult, writers);
			} else {
				debugMessage("Strings are not identical", writers);
				String message = null;
				StringBuilder diffActual = new StringBuilder();
				StringBuilder diffExcpected = new StringBuilder();
				int j = formattedPreparedActualResult.length();
				if (formattedPreparedExpectedResult.length() > j) {
					j = formattedPreparedExpectedResult.length();
				}
				for (int i = 0; i < j; i++) {
					if (i >= formattedPreparedActualResult.length() || i >= formattedPreparedExpectedResult.length()
							|| formattedPreparedActualResult.charAt(i) != formattedPreparedExpectedResult.charAt(i)) {
						if (message == null) {
							message = "Starting at char " + (i + 1);
						}
						if (i < formattedPreparedActualResult.length()) {
							diffActual.append(formattedPreparedActualResult.charAt(i));
						}
						if (i < formattedPreparedExpectedResult.length()) {
							diffExcpected.append(formattedPreparedExpectedResult.charAt(i));
						}
					}
				}
				if (diffActual.length() > 250) {
					diffActual.delete(250, diffActual.length());
					diffActual.append(" ...");
				}
				if (diffExcpected.length() > 250) {
					diffExcpected.delete(250, diffExcpected.length());
					diffExcpected.append(" ...");
				}
				message = message + " actual result is '" + diffActual + "' and expected result is '" + diffExcpected + "'";
				wrongPipelineMessage(stepDisplayName, message, printableActualResult, printableExpectedResult, writers);
				wrongPipelineMessagePreparedForDiff(stepDisplayName, preparedActualResult, preparedExpectedResult, writers);
				if (autoSaveDiffs) {
					String filenameAbsolutePath = (String)properties.get(step + ".absolutepath");
					debugMessage("Copy actual result to ["+filenameAbsolutePath+"]", writers);
					try {
						org.apache.commons.io.FileUtils.writeStringToFile(new File(filenameAbsolutePath), actualResult);
					} catch (IOException e) {
					}
					ok = RESULT_AUTOSAVED;
				}
			}
		}
		return ok;
	}

	public static String prepareResultForCompare(String input, Properties properties, HashMap<String, HashMap<String, HashMap<String, String>>> ignoreMap, Map<String, Object> writers) {
		String result = input;
		result = doActionBetweenKeys("decodeUnzipContentBetweenKeys", result, properties, ignoreMap, writers, (value, pp, key1, key2)-> {
			boolean replaceNewlines = !"true".equals(pp.apply("replaceNewlines"));
			return decodeUnzipContentBetweenKeys(value, key1, key2, replaceNewlines, writers);
		});

		result = doActionBetweenKeys("canonicaliseFilePathContentBetweenKeys", result, properties, ignoreMap, writers, (value, pp, key1, key2)->canonicaliseFilePathContentBetweenKeys(value,key1,key2,writers));
		result = doActionBetweenKeys("formatDecimalContentBetweenKeys", result, properties, ignoreMap, writers, (value, pp, key1, key2)->formatDecimalContentBetweenKeys(value,key1,key2,writers));
		result = doActionWithSingleKey("ignoreRegularExpressionKey", result, properties, ignoreMap, writers, (value, pp, key)->ignoreRegularExpression(value,key));
		result = doActionWithSingleKey("removeRegularExpressionKey", result, properties, ignoreMap, writers, (value, pp, key)->removeRegularExpression(value,key));

		result = doActionBetweenKeys("replaceRegularExpressionKeys", result, properties, ignoreMap, writers, (value, pp, key1, key2)->replaceRegularExpression(value,key1,key2));
		result = doActionBetweenKeys("ignoreContentBetweenKeys", result, properties, ignoreMap, writers, (value, pp, key1, key2)->ignoreContentBetweenKeys(value,key1,key2));
		result = doActionBetweenKeys("ignoreKeysAndContentBetweenKeys", result, properties, ignoreMap, writers, (value, pp, key1, key2)->ignoreKeysAndContentBetweenKeys(value,key1,key2));
		result = doActionBetweenKeys("removeKeysAndContentBetweenKeys", result, properties, ignoreMap, writers, (value, pp, key1, key2)->removeKeysAndContentBetweenKeys(value,key1,key2));

		result = doActionWithSingleKey("ignoreKey", result, properties, ignoreMap, writers, (value, pp, key)->ignoreKey(value,key));
		result = doActionWithSingleKey("removeKey", result, properties, ignoreMap, writers, (value, pp, key)->removeKey(value,key));

		result = doActionBetweenKeys("replaceKey", result, properties, ignoreMap, writers, (value, pp, key1, key2)->replaceKey(value,key1,key2));
		result = doActionBetweenKeys("replaceEverywhereKey", result, properties, ignoreMap, writers, (value, pp, key1, key2)->replaceKey(value,key1,key2));

		result = doActionBetweenKeys("ignoreCurrentTimeBetweenKeys", result, properties, ignoreMap, writers, (value, pp, key1, key2)-> {
			String pattern = pp.apply("pattern");
			String margin = pp.apply("margin");
			boolean errorMessageOnRemainingString = !"false".equals(pp.apply("errorMessageOnRemainingString"));
			return ignoreCurrentTimeBetweenKeys(value, key1, key2, pattern, margin, errorMessageOnRemainingString, false, writers);
		});

		result = doActionWithSingleKey("ignoreContentBeforeKey", result, properties, ignoreMap, writers, (value, pp, key)->ignoreContentBeforeKey(value,key));
		result = doActionWithSingleKey("ignoreContentAfterKey", result, properties, ignoreMap, writers, (value, pp, key)->ignoreContentAfterKey(value,key));
		return result;
	}

	public interface BetweenKeysAction {
		String format(String value, Function<String,String> propertyProvider, String key1, String key2);
	}
	public interface SingleKeyAction {
		String format(String value, Function<String, String> propertyProvider, String key1);
	}

	public static String doActionBetweenKeys(String key, String value, Properties properties, HashMap<String, HashMap<String, HashMap<String, String>>> ignoreMap, Map<String, Object> writers, BetweenKeysAction action) {
		String result = value;
		debugMessage("Check "+key+" properties", writers);
		boolean lastKeyIndexProcessed = false;
		int i = 1;
		while (!lastKeyIndexProcessed) {
			String keyPrefix = key + i + ".";
			String key1 = properties.getProperty(keyPrefix + "key1");
			String key2 = properties.getProperty(keyPrefix + "key2");
			if (key1 != null && key2 != null) {
				debugMessage(key + " between key1 '" + key1 + "' and key2 '" + key2 + "'", writers);
				result = action.format(result, k -> properties.getProperty(keyPrefix + k), key1, key2);
				i++;
			} else {
				lastKeyIndexProcessed = true;
			}
		}

		HashMap<String, HashMap<String, String>> keySpecMap = ignoreMap.get(key);
		if (keySpecMap!=null) {
			Iterator<Entry<String,HashMap<String,String>>> keySpecIt = keySpecMap.entrySet().iterator();
			while (keySpecIt.hasNext()) {
				Entry<String,HashMap<String,String>> spec = keySpecIt.next();
				HashMap<String, String> keyPair = (HashMap<String, String>) spec.getValue();

				String key1 = keyPair.get("key1");
				String key2 = keyPair.get("key2");

				debugMessage(key + " between key1 '" + key1 + "' and key2 '" + key2 + "'", writers);
				result = action.format(result, k -> keyPair.get(k), key1, key2);
			}
		}

		return result;
	}

	public static String doActionWithSingleKey(String keyName, String value, Properties properties, HashMap<String, HashMap<String, HashMap<String, String>>> ignoreMap, Map<String, Object> writers, SingleKeyAction action) {
		String result = value;
		debugMessage("Check "+keyName+" properties", writers);
		boolean lastKeyIndexProcessed = false;
		int i = 1;
		while (!lastKeyIndexProcessed) {
			String keyPrefix = keyName + i;
			String key = properties.getProperty(keyPrefix);
			if (key==null) {
				key = properties.getProperty(keyPrefix + ".key");
			}
			if (key != null) {
				debugMessage(keyName+ " key '" + key + "'", writers);
				result = action.format(result, k -> properties.getProperty(keyPrefix + "." + k), key);
				i++;
			} else {
				lastKeyIndexProcessed = true;
			}
		}

		HashMap<String, HashMap<String, String>> keySpecMap = ignoreMap.get(keyName);
		if (keySpecMap!=null) {
			Iterator<Entry<String,HashMap<String,String>>> keySpecIt = keySpecMap.entrySet().iterator();
			while (keySpecIt.hasNext()) {
				Entry<String,HashMap<String,String>> spec = (Map.Entry) keySpecIt.next();
				HashMap<String, String> keyPair = (HashMap<String, String>) spec.getValue();

				String key = keyPair.get("key");

				debugMessage(keyName + " key '" + key + "'", writers);
				result = action.format(result, k -> keyPair.get(k), key);

				keySpecIt.remove();
			}
		}

		return result;
	}


	public static String ignoreContentBetweenKeys(String string, String key1, String key2) {
		String result = string;
		String ignoreText = "IGNORE";
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				result = result.substring(0, i) + key1 + ignoreText + result.substring(j);
				i = result.indexOf(key1, i + key1.length() + ignoreText.length() + key2.length());
			} else {
				i = -1;
			}
		}
		return result;
	}

	public static String ignoreKeysAndContentBetweenKeys(String string, String key1, String key2) {
		String result = string;
		String ignoreText = "IGNORE";
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				result = result.substring(0, i) + ignoreText + result.substring(j + key2.length());
				i = result.indexOf(key1, i + ignoreText.length());
			} else {
				i = -1;
			}
		}
		return result;
	}

	public static String removeKeysAndContentBetweenKeys(String string, String key1, String key2) {
		String result = string;
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				result = result.substring(0, i) + result.substring(j + key2.length());
				i = result.indexOf(key1, i);
			} else {
				i = -1;
			}
		}
		return result;
	}

	public static String ignoreKey(String string, String key) {
		String result = string;
		String ignoreText = "IGNORE";
		int i = result.indexOf(key);
		while (i != -1) {
			result = result.substring(0, i) + ignoreText + result.substring(i + key.length());
			i = result.indexOf(key, i);
		}
		return result;
	}

	public static String removeKey(String string, String key) {
		String result = string;
		int i = result.indexOf(key);
		while (i != -1) {
			result = result.substring(0, i) + result.substring(i + key.length());
			i = result.indexOf(key, i);
		}
		return result;
	}

	public static String replaceKey(String string, String from, String to) {
		String result = string;
		if (!from.equals(to)) {
			int i = result.indexOf(from);
			while (i != -1) {
				result = result.substring(0, i) + to + result.substring(i + from.length());
				i = result.indexOf(from, i);
			}
		}
		return result;
	}

	public static String decodeUnzipContentBetweenKeys(String string, String key1, String key2, boolean replaceNewlines, Map<String, Object> writers) {
		String result = string;
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			debugMessage("Key 1 found", writers);
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				debugMessage("Key 2 found", writers);
				String encoded = result.substring(i + key1.length(), j);
				String unzipped = null;
				byte[] decodedBytes = null;
				Base64 decoder = new Base64();
				debugMessage("Decode", writers);
				decodedBytes = decoder.decodeBase64(encoded);
				if (unzipped == null) {
					try {
						debugMessage("Unzip", writers);
						StringBuffer stringBuffer = new StringBuffer();
						stringBuffer.append("<tt:file xmlns:tt=\"testtool\">");
						ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(decodedBytes));
						stringBuffer.append("<tt:name>" + zipInputStream.getNextEntry().getName() + "</tt:name>");
						stringBuffer.append("<tt:content>");
						byte[] buffer = new byte[1024];
						int readLength = zipInputStream.read(buffer);
						while (readLength != -1) {
							String part = new String(buffer, 0, readLength, "UTF-8");
							if (replaceNewlines) {
								part = StringUtils.replace(StringUtils.replace(part, "\r", "[CARRIAGE RETURN]"), "\n", "[LINE FEED]");
							}
							stringBuffer.append(part);
							readLength = zipInputStream.read(buffer);
						}
						stringBuffer.append("</tt:content>");
						stringBuffer.append("</tt:file>");
						unzipped = stringBuffer.toString();
					} catch(Exception e) {
						errorMessage("Could not unzip: " + e.getMessage(), e, writers);
						unzipped = encoded;
					}
				}
				result = result.substring(0, i) + key1 + unzipped + result.substring(j);
				i = result.indexOf(key1, i + key1.length() + unzipped.length() + key2.length());
			} else {
				i = -1;
			}
		}
		return result;
	}

	public static String canonicaliseFilePathContentBetweenKeys(String string, String key1, String key2, Map<String, Object> writers) {
		String result = string;
		if (key1.equals("*") && key2.equals("*")) {
			File file = new File(result);
			try {
				result = file.getCanonicalPath();
			} catch (IOException e) {
				errorMessage("Could not canonicalise filepath: " + e.getMessage(), e, writers);
			}
			result = FilenameUtils.normalize(result);
		} else {
			int i = result.indexOf(key1);
			while (i != -1 && result.length() > i + key1.length()) {
				int j = result.indexOf(key2, i + key1.length());
				if (j != -1) {
					String fileName = result.substring(i + key1.length(), j);
					File file = new File(fileName);
					try {
						fileName = file.getCanonicalPath();
					} catch (IOException e) {
						errorMessage("Could not canonicalise filepath: " + e.getMessage(), e, writers);
					}
					fileName = FilenameUtils.normalize(fileName);
					result = result.substring(0, i) + key1 + fileName + result.substring(j);
					i = result.indexOf(key1, i + key1.length() + fileName.length() + key2.length());
				} else {
					i = -1;
				}
			}
		}
		return result;
	}

	public static String ignoreCurrentTimeBetweenKeys(String string, String key1, String key2, String pattern, String margin, boolean errorMessageOnRemainingString, boolean isControlString, Map<String, Object> writers) {
		String result = string;
		String ignoreText = "IGNORE_CURRENT_TIME";
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			debugMessage("Key 1 found", writers);
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				debugMessage("Key 2 found", writers);
				String dateString = result.substring(i + key1.length(), j);
				Date date;
				boolean remainingString = false;
				try {
					SimpleDateFormat simpleDateFormat = null;
					if (pattern == null) {
						// Expect time in milliseconds
						date = new Date(Long.parseLong(dateString));
					} else {
						simpleDateFormat = new SimpleDateFormat(pattern);
						ParsePosition parsePosition = new ParsePosition(0);
						date = simpleDateFormat.parse(dateString, parsePosition);
						if (parsePosition.getIndex() != dateString.length()) {
							remainingString = true;
							i = result.indexOf(key1, j + key2.length());
							if (errorMessageOnRemainingString) {
								errorMessage("Found remaining string after parsing date with pattern '"
											 + pattern + "': "
											 + dateString.substring(parsePosition.getIndex()), writers);
							}
						}
					}
					if (!remainingString) {
						if (isControlString) {
							// Ignore the date in the control string independent on margin from current time
							result = result.substring(0, i) + key1 + ignoreText + result.substring(j);
							i = result.indexOf(key1, i + key1.length() + ignoreText.length() + key2.length());
						} else {
							// Ignore the date in the test string dependent on margin from current time
							String currentTime;
							long currentTimeMillis;
							if (pattern == null) {
								currentTime = "" + System.currentTimeMillis();
								currentTimeMillis = Long.parseLong(currentTime);
							} else {
								currentTime = simpleDateFormat.format(new Date(System.currentTimeMillis()));
								currentTimeMillis = simpleDateFormat.parse(currentTime).getTime();
							}
							if (date.getTime() >= currentTimeMillis - Long.parseLong(margin) && date.getTime() <= currentTimeMillis + Long.parseLong(margin)) {
								result = result.substring(0, i) + key1 + ignoreText + result.substring(j);
								i = result.indexOf(key1, i + key1.length() + ignoreText.length() + key2.length());
							} else {
								errorMessage("Dates differ too much. Current time: '" + currentTime + "'. Result time: '" + dateString + "'", writers);
								i = result.indexOf(key1, j + key2.length());
							}
						}
					}
				} catch(ParseException e) {
					i = -1;
					errorMessage("Could not parse margin or date: " + e.getMessage(), e, writers);
				} catch(NumberFormatException e) {
					i = -1;
					errorMessage("Could not parse long value: " + e.getMessage(), e, writers);
				}
			} else {
				i = -1;
			}
		}
		return result;
	}

	public static String formatDecimalContentBetweenKeys(String string,
		String key1, String key2, Map writers) {
		String result = string;
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				String doubleAsString = result.substring(i + key1.length(), j);
				try {
					double d = Double.parseDouble(doubleAsString);
					result = result.substring(0, i) + key1 + format(d)
							+ result.substring(j);
					i = result.indexOf(key1, i + key1.length()
							+ doubleAsString.length() + key2.length());
				} catch (NumberFormatException e) {
					i = -1;
					errorMessage(
							"Could not parse double value: " + e.getMessage(),
							e, writers);
				}
			} else {
				i = -1;
			}
		}
		return result;
	}

	private static String format(double d) {
		if (d == (long) d)
			return String.format("%d", (long) d);
		else
			return String.format("%s", d);
	}

	public static String ignoreContentBeforeKey(String string, String key) {
		int i = string.indexOf(key);
		if (i == -1) {
			return string;
		} else {
			return string.substring(i) + "IGNORE";
		}
	}

	public static String ignoreContentAfterKey(String string, String key) {
		int i = string.indexOf(key);
		if (i == -1) {
			return string;
		} else {
			return string.substring(0, i + key.length()) + "IGNORE";
		}
	}

	public static String ignoreRegularExpression(String string, String regex) {
		return string.replaceAll(regex, "IGNORE");
	}

	public static String removeRegularExpression(String string, String regex) {
		return string.replaceAll(regex, "");
	}

	public static String replaceRegularExpression(String string, String from, String to) {
		return string.replaceAll(from, to);
	}

	/**
	 * Create a Map for a specific property based on other properties that are
	 * the same except for a .param1.name, .param1.value or .param1.valuefile
	 * suffix.  The property with the .name suffix specifies the key for the
	 * Map, the property with the value suffix specifies the value for the Map.
	 * A property with a the .valuefile suffix can be used as an alternative
	 * for a property with a .value suffix to specify the file to read the
	 * value for the Map from. More than one param can be specified by using
	 * param2, param3 etc.
	 * @param properties
	 * @param property
	 * @param writers
	 * @param session TODO
	 *
	 * @return A map with parameters
	 */
	public static Map<String, Object> createParametersMapFromParamProperties(Properties properties, String property, Map<String, Object> writers, boolean createParameterObjects, PipeLineSession session) {
		debugMessage("Search parameters for property '" + property + "'", writers);
		final String _name = ".name";
		final String _param = ".param";
		final String _type = ".type";
		Map<String, Object> result = new HashMap<>();
		boolean processed = false;
		int i = 1;
		while (!processed) {
			String name = properties.getProperty(property + _param + i + _name);
			if (name != null) {
				Object value;
				String type = properties.getProperty(property + _param + i + _type);
				if ("httpResponse".equals(type)) {
					String outputFile;
					String filename = properties.getProperty(property + _param + i + ".filename");
					if (filename != null) {
						outputFile = properties.getProperty(property + _param + i + ".filename.absolutepath");
					} else {
						outputFile = properties.getProperty(property + _param + i + ".outputfile");
					}
					HttpServletResponseMock httpServletResponseMock = new HttpServletResponseMock();
					httpServletResponseMock.setOutputFile(outputFile);
					value = httpServletResponseMock;
				}
				/** Support for httpRequest parameterType is removed because it depends on Spring and Http-client libraries that contain CVEs. Upgrading these libraries requires some work.
				On the other hand, httpRequest parameter is only used in CreateRestViewPipe. It is unlikely to create a larva test for this pipe.
				Therefore, it is decided to stop supporting it. */
				/* else if ("httpRequest".equals(type)) {
					value = properties.getProperty(property + _param + i + ".value");
					if("multipart".equals(value)){
						MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
						// following line is required to avoid
						// "(FileUploadException) the request was rejected because
						// no multipart boundary was found"
						request.setContentType("multipart/mixed;boundary=gc0p4Jq0M2Yt08jU534c0p");
						List<Part> parts = new ArrayList<>();
						boolean partsProcessed = false;
						int j = 1;
						while (!partsProcessed) {
							String filename = properties.getProperty(property + _param + i + ".part" + j + ".filename");
							if (filename == null) {
								partsProcessed = true;
							} else {
								String partFile = properties.getProperty(property + _param + i + ".part" + j + ".filename.absolutepath");
								String partType = properties.getProperty(property + _param + i + ".part" + j + _type);
								String partName = properties.getProperty(property + _param + i + ".part" + j + _name);
								if ("file".equalsIgnoreCase(partType)) {
									File file = new File(partFile);
									try {
										FilePart filePart = new FilePart( "file" + j, (partName == null ? file.getName() : partName), file);
										parts.add(filePart);
									} catch (FileNotFoundException e) {
										errorMessage("Could not read file '" + partFile+ "': " + e.getMessage(), e, writers);
									}
								} else {
									String string = readFile(partFile, writers);
									StringPart stringPart = new StringPart((partName == null ? "string" + j : partName), string);
									parts.add(stringPart);
								}
								j++;
							}
						}
						Part[] allParts = new Part[parts.size()];
						allParts = parts.toArray(allParts);
						MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(allParts, new PostMethod().getParams());
						ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
						try {
							multipartRequestEntity.writeRequest(requestContent);
						} catch (IOException e) {
							errorMessage("Could not create multipart: " + e.getMessage(), e, writers);
						}
						request.setContent(requestContent.toByteArray());
						request.setContentType(multipartRequestEntity.getContentType());
						value = request;
					}
					else{
						value = new MockHttpServletRequest();
					}
				} */
				else {
					value = properties.getProperty(property + _param + i + ".value");
					if (value == null) {
						String filename = properties.getProperty(property + _param + i + ".valuefile.absolutepath");
						if (filename != null) {
							value = new FileMessage(new File(filename));
						} else {
							String inputStreamFilename = properties.getProperty(property + _param + i + ".valuefileinputstream.absolutepath");
							if (inputStreamFilename != null) {
								errorMessage("valuefileinputstream is no longer supported use valuefile instead", writers);
							}
						}
					}
				}
				if ("node".equals(type)) {
					try {
						value = XmlUtils.buildNode(Message.asString(value), true);
					} catch (DomBuilderException | IOException e) {
						errorMessage("Could not build node for parameter '" + name + "' with value: " + value, e, writers);
					}
				} else if ("domdoc".equals(type)) {
					try {
						value = XmlUtils.buildDomDocument(Message.asString(value), true);
					} catch (DomBuilderException | IOException e) {
						errorMessage("Could not build node for parameter '" + name + "' with value: " + value, e, writers);
					}
				} else if ("list".equals(type)) {
					try {
						List<String> parts = new ArrayList<>(Arrays.asList(Message.asString(value).split("\\s*(,\\s*)+")));
						List<String> list = new LinkedList<>();
						for (String part : parts) {
							list.add(part);
						}
						value = list;
					} catch (IOException e) {
						errorMessage("Could not build a list for parameter '" + name + "' with value: " + value, e, writers);
					}
				} else if ("map".equals(type)) {
					try {
						List<String> parts = new ArrayList<>(Arrays.asList(Message.asString(value).split("\\s*(,\\s*)+")));
						Map<String, String> map = new LinkedHashMap<>();
						for (String part : parts) {
							String[] splitted = part.split("\\s*(=\\s*)+", 2);
							if (splitted.length==2) {
								map.put(splitted[0], splitted[1]);
							} else {
								map.put(splitted[0], "");
							}
						}
						value = map;
					} catch (IOException e) {
						errorMessage("Could not build a map for parameter '" + name + "' with value: " + value, e, writers);
					}
				}
				if (createParameterObjects) {
					String  pattern = properties.getProperty(property + _param + i + ".pattern");
					if (value == null && pattern == null) {
						errorMessage("Property '" + property + _param + i + " doesn't have a value or pattern", writers);
					} else {
						try {
							Parameter parameter = new Parameter();
							parameter.setName(name);
							if (value != null && !(value instanceof String)) {
								parameter.setSessionKey(name);
								session.put(name, value);
							} else {
								parameter.setValue((String)value);
								parameter.setPattern(pattern);
							}
							parameter.configure();
							result.put(name, parameter);
							debugMessage("Add param with name '" + name + "', value '" + value + "' and pattern '" + pattern + "' for property '" + property + "'", writers);
						} catch (ConfigurationException e) {
							errorMessage("Parameter '" + name + "' could not be configured", writers);
						}
					}
				} else {
					if (value == null) {
						errorMessage("Property '" + property + _param + i + ".value' or '" + property + _param + i + ".valuefile' not found while property '" + property + _param + i + ".name' exist", writers);
					} else {
						result.put(name, value);
						debugMessage("Add param with name '" + name + "' and value '" + value + "' for property '" + property + "'", writers);
					}
				}
				i++;
			} else {
				processed = true;
			}
		}
		return result;
	}

	public static String formatString(String string, Map<String, Object> writers) {
		StringBuffer sb = new StringBuffer();
		try {
			Reader reader = new StringReader(string);
			BufferedReader br = new BufferedReader(reader);
			String l = null;
			while ((l = br.readLine()) != null) {
				if (sb.length()==0) {
					sb.append(l);
				} else {
					sb.append(System.getProperty("line.separator") + l);
				}
			}
			br.close();
		} catch(Exception e) {
			errorMessage("Could not read string '" + string + "': " + e.getMessage(), e, writers);
		}
		return sb.toString();
	}

	/**
	 * This method is used to provide a way to implement ignores based on an identifier.
	 * For example:
	 * ignoreContentBetweenKeys.fieldA.key1=<field name="A">
	 * ignoreContentBetweenKeys.fieldA.key2=</field>
	 *
	 * @param properties Properties to be checked
	 *
	 * @return HashMap<String, HashMap<String, HashMap<String, String>>> as HashMap<'ignoreContentBetweenKeys', Hashmap<'fieldA', HashMap<'key1', '<field name="A">'>
	*/

	public static HashMap<String, HashMap<String, HashMap<String, String>>> mapPropertiesToIgnores(Properties properties){
		HashMap<String, HashMap<String, HashMap<String, String>>> returnMap = new HashMap<>();
		Enumeration<String> enums = (Enumeration<String>) properties.propertyNames();

		// Loop through all properties
		while (enums.hasMoreElements()) {
			// Extract key
			String key = enums.nextElement();

			// Extract ignore type
			String ignore = key.split(Pattern.quote("."))[0];
			ArrayList<String> attributes = findAttributesForIgnore(ignore);

			if(attributes != null){
				// Extract identifier
				String id = key.split(Pattern.quote("."))[1];

				// Find return map for ignore
				HashMap<String, HashMap<String, String>> ignoreMap = returnMap.get(ignore);

				// Create return map for ignore if not exist
				if(ignoreMap == null) {
					ignoreMap = new HashMap<>();
					returnMap.put(ignore, ignoreMap);
				}

				// Find return map for identifier
				HashMap<String, String> idMap = ignoreMap.get(id);

				// Create return map for identifier if not exist
				if(idMap == null) {
					idMap = new HashMap<>();
					ignoreMap.put(id, idMap);
				}

				// Check attributes are provided
				if(!attributes.isEmpty()){
					// Loop through attributes to be searched for
					for (String attribute : attributes) {
						if(key.endsWith("." + attribute)){
							idMap.put(attribute, properties.getProperty(key));
						}
						else if(attribute.equals("")){
							// in case of an empty string as attribute, assume it should read the value
							// ie: ignoreKey.identifier=value
							idMap.put("value", properties.getProperty(key));
						}
					}
				}
			}
		}

		return returnMap;
	}

	/**
	 * This method is used to de-couple the need of providing a set of attributes when calling mapPropertiesByIdentifier().
	 * Caller of mapPropertiesByIdentifier() should not necescarrily know about all attributes related to an ignore.
	 *
	 * @param propertyName The name of the ignore we are checking, in the example 'ignoreContentBetweenKeys'
	 *
	 * @return ArrayList<String> attributes
	*/
	public static ArrayList<String> findAttributesForIgnore(String propertyName) {
		ArrayList<String> attributes = null;

		switch (propertyName) {
			case "decodeUnzipContentBetweenKeys":
			  	attributes = new ArrayList( Arrays.asList( new String[]{"key1", "key2", "replaceNewlines"} ) );
			  	break;
			case "canonicaliseFilePathContentBetweenKeys":
			case "replaceRegularExpressionKeys":
			case "ignoreContentBetweenKeys":
			case "ignoreKeysAndContentBetweenKeys":
			case "removeKeysAndContentBetweenKeys":
			case "replaceKey":
			case "formatDecimalContentBetweenKeys":
			case "replaceEverywhereKey":
				attributes = new ArrayList( Arrays.asList( new String[]{"key1", "key2"} ) );
				break;
			case "ignoreRegularExpressionKey":
			case "removeRegularExpressionKey":
			case "ignoreContentBeforeKey":
			case "ignoreContentAfterKey":
				attributes = new ArrayList( Arrays.asList( new String[]{"key"} ) );
				break;
			case "ignoreCurrentTimeBetweenKeys":
				attributes = new ArrayList( Arrays.asList( new String[]{"key1", "key2", "pattern", "margin", "errorMessageOnRemainingString"} ) );
				break;
			case "ignoreKey":
			case "removeKey":
				// in case of an empty string as attribute, assume it should read the value
				// ie: ignoreKey.identifier=value
				attributes = new ArrayList<String>(Arrays.asList( new String[]{"key", ""}));
				break;
		}

		return attributes;
	}
}
