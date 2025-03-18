/*
   Copyright 2014-2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.larva;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.json.JsonException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Logger;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;

import org.frankframework.configuration.ClassNameRewriter;
import org.frankframework.configuration.IbisContext;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.larva.queues.Queue;
import org.frankframework.larva.queues.QueueCreator;
import org.frankframework.larva.queues.QueueWrapper;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.FileMessage;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CleanerProvider;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.FileUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.ProcessUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringResolver;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TemporaryDirectoryUtils;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

/**
 * @author Jaco de Groot
 */
public class LarvaTool {
	private static final Logger logger = LogUtil.getLogger(LarvaTool.class);
	public static final int ERROR_NO_SCENARIO_DIRECTORIES_FOUND = -1;
	protected static final Message TESTTOOL_CLEAN_UP_REPLY = new Message("<LarvaTool>Clean up reply</LarvaTool>");
	public static final int RESULT_ERROR = 0;
	public static final int RESULT_OK = 1;
	public static final int RESULT_AUTOSAVED = 2;
	private static final String LEGACY_PACKAGE_NAME_LARVA = "org.frankframework.testtool.";
	private static final String CURRENT_PACKAGE_NAME_LARVA = "org.frankframework.larva.";
	// dirty solution by Marco de Reus:
	private String stepOutputFilename = "";
	private static boolean autoSaveDiffs = false;
	private final TestConfig config = new TestConfig();

	/*
	 * if allowReadlineSteps is set to true, actual results can be compared in line by using .readline steps.
	 * Those results cannot be saved to the inline expected value, however.
	 */
	protected static final boolean allowReadlineSteps = false;
	protected static int globalTimeoutMillis = AppConstants.getInstance().getInt("larva.timeout", 10_000);

	private static final String TR_STARTING_TAG="<tr>";
	private static final String TR_CLOSING_TAG="</tr>";
	private static final String TD_STARTING_TAG="<td>";
	private static final String TD_CLOSING_TAG="</td>";
	private static final String TABLE_CLOSING_TAG="</table>";

	public static void setTimeout(int newTimeout) {
		globalTimeoutMillis = newTimeout;
	}

	private static IbisContext getIbisContext(ServletContext application) {
		return FrankApplicationInitializer.getIbisContext(application);
	}

	// Invoked by LarvaServlet
	public static void runScenarios(ServletContext application, HttpServletRequest request, Writer out) {
		runScenarios(getIbisContext(application), request, out, false);
	}

	// Invoked by the IbisTester class
	public static void runScenarios(IbisContext ibisContext, HttpServletRequest request, Writer out, boolean silent) {
		String paramLogLevel = request.getParameter("loglevel");
		String paramAutoScroll = request.getParameter("autoscroll");
		String paramMultiThreaded = request.getParameter("multithreaded");
		String paramExecute = request.getParameter("execute");
		String paramWaitBeforeCleanUp = request.getParameter("waitbeforecleanup");
		String paramGlobalTimeout = request.getParameter("timeout");
		int timeout = globalTimeoutMillis;
		if(paramGlobalTimeout != null) {
			try {
				timeout = Integer.parseInt(paramGlobalTimeout);
			} catch(NumberFormatException e) {
				// Ignore error, use default
			}
		}
		String paramScenariosRootDirectory = request.getParameter("scenariosrootdirectory");
		LarvaTool larvaTool = new LarvaTool();
		larvaTool.runScenarios(ibisContext, paramLogLevel, paramAutoScroll, paramMultiThreaded, paramExecute, paramWaitBeforeCleanUp, timeout,
				paramScenariosRootDirectory, out, silent);
	}

	/**
	 * @return negative: error condition
	 * 		   0: all scenarios passed
	 * 		   positive: number of scenarios that failed
	 */
	public int runScenarios(IbisContext ibisContext, String paramLogLevel, String paramAutoScroll, String paramMultithreaded, String paramExecute,
							String paramWaitBeforeCleanUp, int timeout, String paramScenariosRootDirectory, Writer out, boolean silent) {
		config.setTimeout(timeout);
		config.setSilent(silent);
		AppConstants appConstants = AppConstants.getInstance();
		LarvaLogLevel logLevel = config.getLogLevel();
		if (paramLogLevel != null) {
			logLevel = LarvaLogLevel.parse(paramLogLevel, logLevel);
		}
		if (paramAutoScroll == null && paramLogLevel != null) {
			config.setAutoScroll(false);
		}
		if (StringUtils.isNotEmpty(paramMultithreaded) && Boolean.parseBoolean(paramMultithreaded) && paramLogLevel != null) {
			config.setMultiThreaded(true);
		}

		if (!silent) {
			config.setOut(out);
			config.setLogLevel(logLevel);
		} else {
			config.setSilentOut(out);
		}

		debugMessage("Start logging to logbuffer until form is written");
		String autoSave = appConstants.getProperty("larva.diffs.autosave");
		if (autoSave!=null) {
			autoSaveDiffs = Boolean.parseBoolean(autoSave);
		}
		debugMessage("Initialize scenarios root directories");
		List<String> scenariosRootDirectories = new ArrayList<>();
		List<String> scenariosRootDescriptions = new ArrayList<>();
		String currentScenariosRootDirectory = initScenariosRootDirectories(
				paramScenariosRootDirectory, scenariosRootDirectories,
				scenariosRootDescriptions);
		if (scenariosRootDirectories.isEmpty()) {
			debugMessage("Stop logging to logbuffer");
			if (!config.isSilent()) {
				config.setUseLogBuffer(false);
			}
			errorMessage("No scenarios root directories found");
			return ERROR_NO_SCENARIO_DIRECTORIES_FOUND;
		}

		debugMessage("Read scenarios from directory '" + StringEscapeUtils.escapeJava(currentScenariosRootDirectory) + "'");
		List<File> allScenarioFiles = readScenarioFiles(appConstants, currentScenariosRootDirectory);
		debugMessage("Initialize 'wait before cleanup' variable");
		int waitBeforeCleanUp = 100;
		if (paramWaitBeforeCleanUp != null) {
			try {
				waitBeforeCleanUp = Integer.parseInt(paramWaitBeforeCleanUp);
			} catch(NumberFormatException e) {
				// Ignore the error, use default
			}
		}

		debugMessage("Write html form");
		printHtmlForm(scenariosRootDirectories, scenariosRootDescriptions, currentScenariosRootDirectory, appConstants, allScenarioFiles, waitBeforeCleanUp, timeout, paramExecute);
		debugMessage("Stop logging to logbuffer");
		config.setUseLogBuffer(false);
		debugMessage("Start debugging to out");
		debugMessage("Execute scenario(s) if execute parameter present and scenarios root directory did not change");
		if (paramExecute == null) {
			config.flushWriters();
			return 0;
		}
		int scenariosFailed = 0;
		String paramExecuteCanonicalPath;
		String scenariosRootDirectoryCanonicalPath;
		try {
			paramExecuteCanonicalPath = new File(paramExecute).getCanonicalPath();
			scenariosRootDirectoryCanonicalPath = new File(currentScenariosRootDirectory).getCanonicalPath();
		} catch(IOException e) {
			paramExecuteCanonicalPath = paramExecute;
			scenariosRootDirectoryCanonicalPath = currentScenariosRootDirectory;
			errorMessage("Could not get canonical path: " + e.getMessage(), e);
		}
		if (paramExecuteCanonicalPath.startsWith(scenariosRootDirectoryCanonicalPath)) {
			debugMessage("Initialize XMLUnit");
			XMLUnit.setIgnoreWhitespace(true);
			debugMessage("Initialize 'scenario files' variable");
			debugMessage("Param execute: " + paramExecute);
			List<File> scenarioFiles;
			if (paramExecute.endsWith(".properties")) {
				debugMessage("Read one scenario");
				scenarioFiles = new ArrayList<>();
				scenarioFiles.add(new File(paramExecute));
			} else {
				debugMessage("Read all scenarios from directory '" + paramExecute + "'");
				scenarioFiles = readScenarioFiles(appConstants, paramExecute);
			}
			boolean evenStep = false;
			debugMessage("Initialize statistics variables");
			long startTime = System.currentTimeMillis();
			debugMessage("Execute scenario('s)");
			ScenarioRunner scenarioRunner = new ScenarioRunner(this, ibisContext, config, appConstants, evenStep, waitBeforeCleanUp, logLevel);
			// If only one scenario is executed, do not use multithreading, because they mostly use the same resources
			if (paramScenariosRootDirectory != null && !paramScenariosRootDirectory.equals(paramExecute)) {
				scenarioRunner.setMultipleThreads(false);
			}
			scenarioRunner.runScenario(scenarioFiles, currentScenariosRootDirectory);
			config.flushWriters();
			scenariosFailed = scenarioRunner.getScenariosFailed().get();

			long executeTime = System.currentTimeMillis() - startTime;
			debugMessage("Print statistics information");
			int scenariosTotal = scenarioRunner.getScenariosPassed().get() + scenarioRunner.getScenariosAutosaved().get() + scenarioRunner.getScenariosFailed().get();
			if (scenariosTotal == 0) {
				scenariosTotalMessage("No scenarios found");
				config.flushWriters();
				return 0;
			}
			if (!config.isSilent() && logLevel.shouldLog(LarvaLogLevel.SCENARIO_PASSED_FAILED)) {
				writeHtml("<br/><br/>", false);
			}
			debugMessage("Print statistics information");
			String formattedTime = getFormattedTime(executeTime);
			if (scenarioRunner.getScenariosPassed().get() == scenariosTotal) {
				if (scenariosTotal == 1) {
					scenariosPassedTotalMessage("All scenarios passed (1 scenario executed in " + formattedTime + ")");
				} else {
					scenariosPassedTotalMessage("All scenarios passed (" + scenariosTotal + " scenarios executed in " + formattedTime + ")");
				}
			} else if (scenarioRunner.getScenariosFailed().get() == scenariosTotal) {
				if (scenariosTotal == 1) {
					scenariosFailedTotalMessage("All scenarios failed (1 scenario executed in " + formattedTime + ")");
				} else {
					scenariosFailedTotalMessage("All scenarios failed (" + scenariosTotal + " scenarios executed in " + formattedTime + ")");
				}
			} else {
				if (scenariosTotal == 1) {
					scenariosTotalMessage("1 scenario executed in " + formattedTime);
				} else {
					scenariosTotalMessage(scenariosTotal + " scenarios executed in " + formattedTime);
				}
				if (scenarioRunner.getScenariosPassed().get() == 1) {
					scenariosPassedTotalMessage("1 scenario passed");
				} else {
					scenariosPassedTotalMessage(scenarioRunner.getScenariosPassed() + " scenarios passed");
				}
				if (autoSaveDiffs) {
					if (scenarioRunner.getScenariosAutosaved().get() == 1) {
						scenariosAutosavedTotalMessage("1 scenario passed after autosave");
					} else {
						scenariosAutosavedTotalMessage(scenarioRunner.getScenariosAutosaved() + " scenarios passed after autosave");
					}
				}
				if (scenarioRunner.getScenariosFailed().get() == 1) {
					scenariosFailedTotalMessage("1 scenario failed");
				} else {
					scenariosFailedTotalMessage(scenarioRunner.getScenariosFailed() + " scenarios failed");
				}
			}
		}
		debugMessage("Start logging to htmlbuffer until form is written");
		if (!config.isSilent())
			config.setUseHtmlBuffer(true);
		writeHtml("<br/><br/>",  false);
		printHtmlForm(scenariosRootDirectories, scenariosRootDescriptions, currentScenariosRootDirectory, appConstants, allScenarioFiles, waitBeforeCleanUp, timeout, paramExecute);
		debugMessage("Stop logging to htmlbuffer");
		if (!config.isSilent())
			config.setUseHtmlBuffer(false);
		writeHtml("",  true);
		config.flushWriters();
		CleanerProvider.logLeakStatistics();
		return scenariosFailed;
	}

	private static String getFormattedTime(long executeTime) {
		Duration duration = Duration.ofMillis(executeTime);
		if (duration.toMinutesPart() == 0 && duration.toSecondsPart() == 0) {
			// Only milliseconds (e.g. 123ms)
			return duration.toMillisPart() + "ms";
		} else if (duration.toMinutesPart() == 0) {
			// Seconds and milliseconds (e.g. 1s 123ms)
			return duration.toSecondsPart() + "s " + duration.toMillisPart() + "ms";
		} else {
			// Minutes, seconds and milliseconds (e.g. 1m 1s 123ms)
			return duration.toMinutesPart() + "m " + duration.toSecondsPart() + "s " + duration.toMillisPart() + "ms";
		}
	}

	public void printHtmlForm(List<String> scenariosRootDirectories, List<String> scenariosRootDescriptions, String scenariosRootDirectory, AppConstants appConstants, List<File> scenarioFiles, int waitBeforeCleanUp, int timeout, String paramExecute) {
		if (config.isSilent())
			return;

		writeHtml("<form action=\"index.jsp\" method=\"post\">", false);

		// scenario root directory drop down
		writeHtml("<table style=\"float:left;height:50px\">", false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml("<td>Scenarios root directory</td>", false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml(TD_STARTING_TAG, false);
		writeHtml("<select name=\"scenariosrootdirectory\" onchange=\"updateScenarios()\">", false);
		Iterator<String> scenariosRootDirectoriesIterator = scenariosRootDirectories.iterator();
		Iterator<String> scenariosRootDescriptionsIterator = scenariosRootDescriptions.iterator();
		while (scenariosRootDirectoriesIterator.hasNext()) {
			String directory = scenariosRootDirectoriesIterator.next();
			String description = scenariosRootDescriptionsIterator.next();
			String option = "<option value=\"" + XmlEncodingUtils.encodeChars(directory) + "\"";
			if (scenariosRootDirectory.equals(directory)) {
				option = option + " selected";
			}
			option = option + ">" + XmlEncodingUtils.encodeChars(description) + "</option>";
			writeHtml(option, false);
		}
		writeHtml("</select>", false);
		writeHtml(TD_CLOSING_TAG, false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TABLE_CLOSING_TAG, false);

		// Use a span to make IE put table on next line with a smaller window width
		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", false);
		writeHtml("<table style=\"float:left;height:50px\">", false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml("<td>Wait before clean up (ms)</td>", false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml(TD_STARTING_TAG, false);
		writeHtml("<input type=\"text\" name=\"waitbeforecleanup\" value=\"" + waitBeforeCleanUp + "\"/>", false);
		writeHtml(TD_CLOSING_TAG, false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TABLE_CLOSING_TAG, false);

		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", false);
		// timeout box
		writeHtml("<table style=\"float:left;height:50px\">", false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml("<td>Default timeout (ms)</td>", false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml(TD_STARTING_TAG, false);
		writeHtml("<input type=\"text\" name=\"timeout\" value=\"" + (timeout != globalTimeoutMillis ? timeout : globalTimeoutMillis) + "\" title=\"Global timeout for larva scenarios.\"/>", false);
		writeHtml(TD_CLOSING_TAG, false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TABLE_CLOSING_TAG, false);

		// log level dropdown
		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", false);
		writeHtml("<table style=\"float:left;height:50px\">", false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml("<td>Log level</td>", false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml(TD_STARTING_TAG, false);
		writeHtml("<select name=\"loglevel\">", false);
		for (LarvaLogLevel level : LarvaLogLevel.values()) {
			String option = "<option value=\"" + XmlEncodingUtils.encodeChars(level.getName()) + "\"";
			if (config.getLogLevel() == level) {
				option = option + " selected";
			}
			option = option + ">" + XmlEncodingUtils.encodeChars(level.getName()) + "</option>";
			writeHtml(option, false);
		}
		writeHtml("</select>", false);
		writeHtml(TD_CLOSING_TAG, false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TABLE_CLOSING_TAG, false);

		// Auto scroll checkbox
		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", false);
		writeHtml("<table style=\"float:left;height:50px\">", false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml("<td>Auto scroll</td>", false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml(TD_STARTING_TAG, false);
		writeHtml("<input type=\"checkbox\" name=\"autoscroll\" value=\"true\"", false);
		if (config.isAutoScroll()) {
			writeHtml(" checked", false);
		}
		writeHtml("/>", false);
		writeHtml(TD_CLOSING_TAG, false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TABLE_CLOSING_TAG, false);

		// Multithreaded checkbox
		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", false);
		writeHtml("<table style=\"float:left;height:50px\">", false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml("<td>Multi Threaded (experimental)</td>", false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml(TD_STARTING_TAG, false);
		writeHtml("<input type=\"checkbox\" name=\"multithreaded\" value=\"true\"", false);
		if (config.isMultiThreaded()) {
			writeHtml(" checked", false);
		}
		writeHtml("/>", false);
		writeHtml(TD_CLOSING_TAG, false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TABLE_CLOSING_TAG, false);

		// Scenario(s)
		writeHtml("<table style=\"clear:both;float:left;height:50px\">", false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml("<td>Scenario(s)</td>", false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml(TD_STARTING_TAG, false);
		writeHtml("<select name=\"execute\">", false);
		debugMessage("Fill execute select box.");
		Set<String> addedDirectories = new HashSet<>();
		for (File scenarioFile : scenarioFiles) {
			String scenarioDirectory = scenarioFile.getParentFile().getAbsolutePath() + File.separator;
			Properties properties = readProperties(appConstants, scenarioFile);
			debugMessage("Add parent directories of '" + scenarioDirectory + "'");
			int i;
			String scenarioDirectoryCanonicalPath;
			String scenariosRootDirectoryCanonicalPath;
			try {
				scenarioDirectoryCanonicalPath = new File(scenarioDirectory).getCanonicalPath();
				scenariosRootDirectoryCanonicalPath = new File(scenariosRootDirectory).getCanonicalPath();
			} catch (IOException e) {
				scenarioDirectoryCanonicalPath = scenarioDirectory;
				scenariosRootDirectoryCanonicalPath = scenariosRootDirectory;
				errorMessage("Could not get canonical path: " + e.getMessage(), e);
			}
			if (scenarioDirectoryCanonicalPath.startsWith(scenariosRootDirectoryCanonicalPath)) {
				i = scenariosRootDirectory.length() - 1;
				while (i != -1) {
					String longName = scenarioDirectory.substring(0, i + 1);
					debugMessage("longName: '" + longName + "'");
					if (!addedDirectories.contains(longName)) {
						String shortName = scenarioDirectory.substring(scenariosRootDirectory.length() - 1, i + 1);
						String option = "<option value=\"" + XmlEncodingUtils.encodeChars(longName) + "\"";
						debugMessage("paramExecute: '" + paramExecute + "'");
						if (paramExecute != null && paramExecute.equals(longName)) {
							option = option + " selected";
						}
						option = option + ">" + XmlEncodingUtils.encodeChars(shortName) + "</option>";
						writeHtml(option, false);
						addedDirectories.add(longName);
					}
					i = scenarioDirectory.indexOf(File.separator, i + 1);
				}
				String longName = scenarioFile.getAbsolutePath();
				String shortName = longName.substring(scenariosRootDirectory.length() - 1, longName.length() - ".properties".length());
				debugMessage("shortName: '" + shortName + "'");
				String option = "<option value=\"" + XmlEncodingUtils.encodeChars(longName) + "\"";
				if (paramExecute != null && paramExecute.equals(longName)) {
					option = option + " selected";
				}
				option = option + ">" + XmlEncodingUtils.encodeChars(shortName + " - " + properties.getProperty("scenario.description")) + "</option>";
				writeHtml(option, false);
			}
		}
		writeHtml("</select>", false);
		writeHtml(TD_CLOSING_TAG, false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TABLE_CLOSING_TAG, false);

		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", false);
		// submit button
		writeHtml("<table style=\"float:left;height:50px\">", false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml("<td>&nbsp;</td>", false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TR_STARTING_TAG, false);
		writeHtml("<td align=\"right\">", false);
		writeHtml("<input type=\"submit\" name=\"submit\" value=\"start\" id=\"submit\"/>", false);
		writeHtml(TD_CLOSING_TAG, false);
		writeHtml(TR_CLOSING_TAG, false);
		writeHtml(TABLE_CLOSING_TAG, false);

		writeHtml("</form>", false);
		writeHtml("<br clear=\"all\"/>", false);
		config.flushWriters();
	}

	public void write(String html, boolean isHtmlType, LarvaLogLevel logLevel, boolean scroll) {
		if (config.isSilent()) {
			return;
		}

		if (isHtmlType && !config.isUseHtmlBuffer()) {
			try {
				config.getOut().write(config.getHtmlBuffer().toString());
			} catch (IOException ignored) {
				// Ignore
			}
			config.setUseHtmlBuffer(false);
		}

		Writer writer = config.getOut();
		if (!isHtmlType && config.isUseLogBuffer()) {
			writer = config.getLogBuffer();
		} else if (isHtmlType && config.isUseHtmlBuffer()) {
			writer = config.getHtmlBuffer();
		}
		if (logLevel == null || config.getLogLevel().shouldLog(logLevel)) {
			try {
				if (config.isMultiThreaded()) {
					synchronized (writer) { // Needs to be synced to prevent interleaving of messages or that the output stops
						doWriteHtml(html, scroll, writer);
					}
				} else {
					doWriteHtml(html, scroll, writer);
				}
			} catch (IOException ignored) {
				// Ignore
			}
		}
	}

	private void doWriteHtml(String html, boolean scroll, Writer writer) throws IOException {
		writer.write(html + "\n");
		if (scroll && config.isAutoScroll()) {
			writer.write("<script type=\"text/javascript\"><!--\nscrollToBottom();\n--></script>\n");
		}
	}

	public void writeHtml(String html, boolean scroll) {
		write(html, true, null, scroll);
	}

	public void writeLog(String html, LarvaLogLevel logLevel, boolean scroll) {
		write(html, false, logLevel, scroll);
	}

	public void debugMessage(String message) {
		logger.debug(message);
		writeLog(XmlEncodingUtils.encodeChars(XmlEncodingUtils.replaceNonValidXmlCharacters(message)) + "<br/>", LarvaLogLevel.DEBUG, false);
	}

	public void debugPipelineMessage(String stepDisplayName, String message, Message pipelineMessage) {
		if (config.isSilent()) return;
		String pipelineMessageString;
		try {
			pipelineMessageString = pipelineMessage.asString();
		} catch (IOException e) {
			errorMessage("Step %s Message %s; error: Cannot read pipeline message for debug".formatted(stepDisplayName, message), e);
			return;
		}
		debugPipelineMessage(stepDisplayName, message, pipelineMessageString);
	}
	public void debugPipelineMessage(String stepDisplayName, String message, String pipelineMessage) {
		if (config.isSilent()) return;
		config.incrementMessageCounter();

		writeLog("<div class='message container'>", LarvaLogLevel.PIPELINE_MESSAGES, false);
		writeLog("<h4>Step '" + stepDisplayName + "'</h4>", LarvaLogLevel.PIPELINE_MESSAGES, false);
		writeLog(writeCommands("messagebox" + config.getMessageCounter(), true, null), LarvaLogLevel.PIPELINE_MESSAGES, false);
		writeLog("<h5>" + XmlEncodingUtils.encodeChars(message) + "</h5>", LarvaLogLevel.PIPELINE_MESSAGES, false);
		writeLog("<textarea cols='100' rows='10' id='messagebox" + config.getMessageCounter() + "'>" + XmlEncodingUtils.encodeChars(XmlEncodingUtils.replaceNonValidXmlCharacters(pipelineMessage)) + "</textarea>", LarvaLogLevel.PIPELINE_MESSAGES, false);
		writeLog("</div>", LarvaLogLevel.PIPELINE_MESSAGES, false);
	}

	public void debugPipelineMessagePreparedForDiff(String stepDisplayName, String message, String pipelineMessage) {
		if (config.isSilent()) return;
		config.incrementMessageCounter();

		writeLog("<div class='message container'>", LarvaLogLevel.PIPELINE_MESSAGES_PREPARED_FOR_DIFF, false);
		writeLog("<h4>Step '" + stepDisplayName + "'</h4>", LarvaLogLevel.PIPELINE_MESSAGES_PREPARED_FOR_DIFF, false);
		writeLog(writeCommands("messagebox" + config.getMessageCounter(), true, null), LarvaLogLevel.PIPELINE_MESSAGES_PREPARED_FOR_DIFF, false);
		writeLog("<h5>" + XmlEncodingUtils.encodeChars(message) + "</h5>", LarvaLogLevel.PIPELINE_MESSAGES_PREPARED_FOR_DIFF, false);
		writeLog("<textarea cols='100' rows='10' id='messagebox" + config.getMessageCounter() + "'>" + XmlEncodingUtils.encodeChars(pipelineMessage) + "</textarea>", LarvaLogLevel.PIPELINE_MESSAGES_PREPARED_FOR_DIFF, false);
		writeLog("</div>", LarvaLogLevel.PIPELINE_MESSAGES_PREPARED_FOR_DIFF, false);
	}

	public void wrongPipelineMessage(String message, Message pipelineMessage) {
		if (config.isSilent()) return;
		config.incrementMessageCounter();

		writeLog("<div class='message container'>", LarvaLogLevel.WRONG_PIPELINE_MESSAGES, false);
		writeLog(writeCommands("messagebox" + config.getMessageCounter(), true, null), LarvaLogLevel.WRONG_PIPELINE_MESSAGES, false);
		writeLog("<h5>" + XmlEncodingUtils.encodeChars(message) + "</h5>", LarvaLogLevel.WRONG_PIPELINE_MESSAGES, false);
		writeLog("<textarea cols='100' rows='10' id='messagebox" + config.getMessageCounter() + "'>" + XmlEncodingUtils.encodeChars(XmlEncodingUtils.replaceNonValidXmlCharacters(messageToString(pipelineMessage))) + "</textarea>", LarvaLogLevel.WRONG_PIPELINE_MESSAGES, false);
		writeLog("</div>", LarvaLogLevel.WRONG_PIPELINE_MESSAGES, false);
	}

	public void wrongPipelineMessage(String stepDisplayName, String message, String pipelineMessage, String pipelineMessageExpected) {
		if (config.isSilent()) {
			config.writeSilent(message);
			return;
		}
		LarvaLogLevel method = LarvaLogLevel.WRONG_PIPELINE_MESSAGES;
		String formName = "scenario" + config.getScenarioCounter() + "Wpm";
		String resultBoxId = formName + "ResultBox";
		String expectedBoxId = formName + "ExpectedBox";
		String diffBoxId = formName + "DiffBox";

		writeLog("<div class='error container'>", method, false);
		writeLog("<form name='" + formName + "' action='saveResultToFile.jsp' method='post' target='saveResultWindow' accept-charset='UTF-8'>", method, false);
		writeLog("<input type='hidden' name='iehack' value='&#9760;' />", method, false); // http://stackoverflow.com/questions/153527/setting-the-character-encoding-in-form-submit-for-internet-explorer
		writeLog("<h4>Step '" + stepDisplayName + "'</h4>", method, false);

		writeLog("<hr/>", method, false);

		writeLog("<div class='resultContainer'>", method, false);
		writeLog(writeCommands(resultBoxId, true, "<a href='javascript:void(0);' class='" + formName + "|saveResults'>save</a>"), method, false);
		writeLog("<h5>Result (raw):</h5>", method, false);
		writeLog("<textarea name='resultBox' id='" + resultBoxId + "'>" + XmlEncodingUtils.encodeChars(pipelineMessage) + "</textarea>", method, false);
		writeLog("</div>", method, false);

		writeLog("<div class='expectedContainer'>", method, false);
		writeLog(writeCommands(expectedBoxId, true, null), method, true);
		writeLog("<input type='hidden' name='expectedFileName' value='" + stepOutputFilename + "' />", method, false);
		writeLog("<input type='hidden' name='cmd' />", method, false);
		writeLog("<h5>Expected (raw):</h5>", method, false);
		writeLog("<textarea name='expectedBox' id='" + expectedBoxId + "'>" + XmlEncodingUtils.encodeChars(pipelineMessageExpected) + "</textarea>", method, false);
		writeLog("</div>", method, false);

		writeLog("<hr/>", method, false);

		writeLog("<div class='differenceContainer'>", method, false);
		String btn1 = "<a class=\"['" + resultBoxId + "','" + expectedBoxId + "']|indentCompare|" + diffBoxId + "\" href=\"javascript:void(0)\">compare</a>";
		String btn2 = "<a href='javascript:void(0);' class='" + formName + "|indentWindiff'>windiff</a>";
		writeLog(writeCommands(diffBoxId, false, btn1 + btn2), method, false);
		writeLog("<h5>Differences:</h5>", method, false);
		writeLog("<pre id='" + diffBoxId + "' class='diffBox'></pre>", method, false);
		writeLog("</div>", method, false);

		if (config.getLogLevel() == LarvaLogLevel.SCENARIO_PASSED_FAILED) {
			writeLog("<h5 hidden='true'>Difference description:</h5>", LarvaLogLevel.SCENARIO_PASSED_FAILED, false);
			writeLog("<p class='diffMessage' hidden='true'>" + XmlEncodingUtils.encodeChars(message) + "</p>",
					LarvaLogLevel.SCENARIO_PASSED_FAILED, true);
		} else {
			writeLog("<h5>Difference description:</h5>", method, false);
			writeLog("<p class='diffMessage'>" + XmlEncodingUtils.encodeChars(message)	+ "</p>", method, true);
			writeLog("</form>", method, false);
			writeLog("</div>", method, false);
		}
		config.incrementScenarioCounter();
	}

	public void wrongPipelineMessagePreparedForDiff(String stepDisplayName, String pipelineMessagePreparedForDiff, String pipelineMessageExpectedPreparedForDiff) {
		if (config.isSilent()) return;
		LarvaLogLevel method = LarvaLogLevel.WRONG_PIPELINE_MESSAGES_PREPARED_FOR_DIFF;
		String formName = "scenario" + config.getScenarioCounter() + "Wpmpfd";
		String resultBoxId = formName + "ResultBox";
		String expectedBoxId = formName + "ExpectedBox";
		String diffBoxId = formName + "DiffBox";

		writeLog("<div class='error container'>", method, false);
		writeLog("<form name='" + formName + "' action='saveResultToFile.jsp' method='post' target='saveResultWindow' accept-charset='UTF-8'>", method, false);
		writeLog("<input type='hidden' name='iehack' value='&#9760;' />", method, false); // http://stackoverflow.com/questions/153527/setting-the-character-encoding-in-form-submit-for-internet-explorer
		writeLog("<h4>Step '" + stepDisplayName + "'</h4>", method, false);
		config.incrementMessageCounter();

		writeLog("<hr/>", method, false);

		writeLog("<div class='resultContainer'>", method, false);
		writeLog(writeCommands(resultBoxId, true, null), method, false);
		writeLog("<h5>Result (prepared for diff):</h5>", method, false);
		writeLog("<textarea name='resultBox' id='" + resultBoxId + "'>" + XmlEncodingUtils.encodeChars(pipelineMessagePreparedForDiff) + "</textarea>", method, false);
		writeLog("</div>", method, false);

		config.incrementMessageCounter();
		writeLog("<div class='expectedContainer'>", method, false);
		writeLog(writeCommands(expectedBoxId, true, null), method, false);
		writeLog("<input type='hidden' name='expectedFileName' value='" + stepOutputFilename + "' />", method, false);
		writeLog("<input type='hidden' name='cmd' />", method, false);
		writeLog("<h5>Expected (prepared for diff):</h5>", method, false);
		writeLog("<textarea name='expectedBox' id='" + expectedBoxId + "'>" + XmlEncodingUtils.encodeChars(pipelineMessageExpectedPreparedForDiff) + "</textarea>", method, false);
		writeLog("</div>", method, false);

		writeLog("<hr/>", method, false);

		config.incrementMessageCounter();
		writeLog("<div class='differenceContainer'>", method, false);

		String btn1 = "<a class=\"['" + resultBoxId + "','" + expectedBoxId + "']|indentCompare|" + diffBoxId + "\" href=\"javascript:void(0)\">compare</a>";
		String btn2 = "<a href='javascript:void(0);' class='" + formName + "|indentWindiff'>windiff</a>";
		writeLog(writeCommands(diffBoxId, false, btn1 + btn2), method, false);
		writeLog("<h5>Differences:</h5>", method, false);
		writeLog("<pre id='" + diffBoxId + "' class='diffBox'></pre>", method, false);
		writeLog("</div>", method, false);

		writeLog("</form>", method, false);
		writeLog("</div>", method, false);
	}

	private static String writeCommands(String target, boolean textArea, String customCommand) {
		StringBuilder commands = new StringBuilder();
		commands.append("<div class='commands'>");
		commands.append("<span class='widthCommands'><a href='javascript:void(0);' class='").append(target).append("|widthDown'>-</a><a href='javascript:void(0);' class='").append(target).append("|widthExact'>width</a><a href='javascript:void(0);' class='").append(target).append("|widthUp'>+</a></span>");
		commands.append("<span class='heightCommands'><a href='javascript:void(0);' class='").append(target).append("|heightDown'>-</a><a href='javascript:void(0);' class='").append(target).append("|heightExact'>height</a><a href='javascript:void(0);' class='").append(target).append("|heightUp'>+</a></span>");
		if (textArea) {
			commands.append("<a href='javascript:void(0);' class='").append(target).append("|copy'>copy</a> ");
			commands.append("<a href='javascript:void(0);' class='").append(target).append("|xmlFormat'>indent</a>");
		}
		if (customCommand != null) {
			commands.append(" ").append(customCommand);
		}
		commands.append("</div>");
		return commands.toString();
	}

	public void scenariosTotalMessage(String message) {
		if (config.isSilent()) {
			config.writeSilent(message);
		} else {
			writeLog("<h1 class='total'>" + XmlEncodingUtils.encodeChars(message) + "</h1>", LarvaLogLevel.TOTALS, true);
		}
	}

	public void scenariosPassedTotalMessage(String message) {
		if (config.isSilent()) {
			config.writeSilent(message);
		} else {
			writeLog("<h1 class='passed'>" + XmlEncodingUtils.encodeChars(message) + "</h1>", LarvaLogLevel.TOTALS, true);
		}
	}

	public void scenariosAutosavedTotalMessage(String message) {
		if (config.isSilent()) {
			config.writeSilent(message);
		} else {
			writeLog("<h1 class='autosaved'>" + XmlEncodingUtils.encodeChars(message) + "</h1>", LarvaLogLevel.TOTALS, true);
		}
	}

	public void scenariosFailedTotalMessage(String message) {
		if (config.isSilent()) {
			config.writeSilent(message);
		} else {
			writeLog("<h1 class='failed'>" + XmlEncodingUtils.encodeChars(message) + "</h1>", LarvaLogLevel.TOTALS, true);
		}
	}

	public void errorMessage(String message) {
		writeLog("<h1 class='error'>" + XmlEncodingUtils.encodeChars(message) + "</h1>", LarvaLogLevel.ERROR, true);
		config.writeSilent(message);
	}

	public void errorMessage(String message, Exception exception) {
		errorMessage(message);
		if (config.isSilent()) return;

		LarvaLogLevel method = LarvaLogLevel.ERROR;
		Throwable throwable = exception;
		while (throwable != null) {
			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			throwable.printStackTrace(printWriter);
			printWriter.close();
			config.incrementMessageCounter();
			writeLog("<div class='container'>", method, false);
			writeLog(writeCommands("messagebox" + config.getMessageCounter(), true, null), method, false);
			writeLog("<h5>Stack trace:</h5>", method, false);
			writeLog("<textarea cols='100' rows='10' id='messagebox" + config.getMessageCounter() + "'>" + XmlEncodingUtils.encodeChars(XmlEncodingUtils.replaceNonValidXmlCharacters(stringWriter.toString())) + "</textarea>", method, false);
			writeLog("</div>", method, false);
			throwable = throwable.getCause();
		}
	}

	public String initScenariosRootDirectories(String paramScenariosRootDirectory, List<String> scenariosRootDirectories, List<String> scenariosRootDescriptions) {
		AppConstants appConstants = AppConstants.getInstance();
		String currentScenariosRootDirectory = null;

		String realPath = getParentOfWebappRoot();

		if (realPath == null) {
			errorMessage("Could not read webapp real path");
			return null;
		}
		if (!realPath.endsWith(File.separator)) {
			realPath = realPath + File.separator;
		}
		Map<String, String> scenariosRoots = new HashMap<>();
		Map<String, String> scenariosRootsBroken = new HashMap<>();
		int j = 1;
		String directory = appConstants.getProperty("scenariosroot" + j + ".directory");
		String description = appConstants.getProperty("scenariosroot" + j + ".description");
		while (directory != null) {
			if (description == null) {
				errorMessage("Could not find description for root directory '" + directory + "'");
			} else if (scenariosRoots.get(description) != null) {
				errorMessage("A root directory named '" + description + "' already exist");
			} else {
				String parent = realPath;
				String m2eFileName = appConstants.getProperty("scenariosroot" + j + ".m2e.pom.properties");
				if (m2eFileName != null) {
					File m2eFile = new File(realPath, m2eFileName);
					if (m2eFile.exists()) {
						debugMessage("Read m2e pom.properties: " + m2eFileName);
						Properties m2eProperties = readProperties(null, m2eFile, false);
						parent = m2eProperties.getProperty("m2e.projectLocation");
						debugMessage("Use m2e parent: " + parent);
					}
				}
				directory = getAbsolutePath(parent, directory, true);
				if (new File(directory).exists()) {
					debugMessage("directory for [" + description + "] exists: " + directory);
					scenariosRoots.put(description, directory);
				} else {
					debugMessage("directory [" + directory + "] for [" + description + "] does not exist, parent [" + parent + "]");
					scenariosRootsBroken.put(description, directory);
				}
			}
			j++;
			directory = appConstants.getProperty("scenariosroot" + j + ".directory");
			description = appConstants.getProperty("scenariosroot" + j + ".description");
		}
		TreeSet<String> treeSet = new TreeSet<>(new CaseInsensitiveComparator());
		treeSet.addAll(scenariosRoots.keySet());
		Iterator<String> iterator = treeSet.iterator();
		while (iterator.hasNext()) {
			description = iterator.next();
			scenariosRootDescriptions.add(description);
			scenariosRootDirectories.add(scenariosRoots.get(description));
		}
		treeSet.clear();
		treeSet.addAll(scenariosRootsBroken.keySet());
		iterator = treeSet.iterator();
		while (iterator.hasNext()) {
			description = iterator.next();
			scenariosRootDescriptions.add("X " + description);
			scenariosRootDirectories.add(scenariosRootsBroken.get(description));
		}
		debugMessage("Read scenariosrootdirectory parameter");
		debugMessage("Get current scenarios root directory");
		if (paramScenariosRootDirectory == null || paramScenariosRootDirectory.isEmpty()) {
			String scenariosRootDefault = appConstants.getProperty("scenariosroot.default");
			if (scenariosRootDefault != null) {
				currentScenariosRootDirectory = scenariosRoots.get(scenariosRootDefault);
			}
			if (currentScenariosRootDirectory == null
					&& !scenariosRootDirectories.isEmpty()) {
				currentScenariosRootDirectory = scenariosRootDirectories.get(0);
			}
		} else {
			currentScenariosRootDirectory = paramScenariosRootDirectory;
		}
		return currentScenariosRootDirectory;
	}

	private String getParentOfWebappRoot() {
		String realPath = this.getClass().getResource("/").getPath();

		return new File(realPath).getParent();
	}

	public List<File> readScenarioFiles(AppConstants appConstants, String scenariosDirectory) {
		List<File> scenarioFiles = new ArrayList<>();
		debugMessage("List all files in directory '" + scenariosDirectory + "'");

		File directory = new File(scenariosDirectory);
		Path targetPath = directory.toPath().normalize();

		if (!directory.toPath().normalize().startsWith(targetPath)) {
			String message = "Scenarios directory is outside of the target directory";
			logger.warn(message);
			errorMessage(message);

			return scenarioFiles;
		}

		File[] files = directory.listFiles();

		if (files == null) {
			debugMessage("Could not read files from directory '" + scenariosDirectory + "'");
			return scenarioFiles;
		}
		debugMessage("Sort files");
		Arrays.sort(files);
		debugMessage("Filter out property files containing a 'scenario.description' property");
		for (File file : files) {
			if (file.getName().endsWith(".properties")) {
				Properties properties = readProperties(appConstants, file);
				if (properties != null && properties.get("scenario.description") != null) {
					String active = properties.getProperty("scenario.active", "true");
					String unstable = properties.getProperty("adapter.unstable", "false");
					if ("true".equalsIgnoreCase(active) && "false".equalsIgnoreCase(unstable)) {
						scenarioFiles.add(file);
					}
				}
			} else if (file.isDirectory() && (!"CVS".equals(file.getName()))) {
				scenarioFiles.addAll(readScenarioFiles(appConstants, file.getAbsolutePath()));
			}
		}
		debugMessage(scenarioFiles.size() + " scenario files found");
		return scenarioFiles;
	}

	@Nullable
	public Properties readProperties(AppConstants appConstants, File propertiesFile) {
		return readProperties(appConstants, propertiesFile, true);
	}

	@Nullable
	public Properties readProperties(AppConstants appConstants, File propertiesFile, boolean root) {
		String directory = new File(propertiesFile.getAbsolutePath()).getParent();
		Properties properties = new Properties();
		try {
			try(FileInputStream fis = new FileInputStream(propertiesFile); Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(fis)) {
				properties.load(reader);
			}

			Properties includedProperties = new Properties();
			int i = 0;
			String includeFilename = properties.getProperty("include");
			if (includeFilename == null) {
				i++;
				includeFilename = properties.getProperty("include" + i);
			}
			while (includeFilename != null) {
				debugMessage("Load include file: " + includeFilename);
				File includeFile = new File(getAbsolutePath(directory, includeFilename));
				Properties includeProperties = readProperties(appConstants, includeFile, false);
				if (includeProperties != null) {
					includedProperties.putAll(includeProperties);
				}
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
			debugMessage(properties.size() + " properties found");
		} catch(Exception e) {
			properties = null;
			errorMessage("Could not read properties file: " + e.getMessage(), e);
		}
		return fixLegacyClassnames(properties);
	}

	@Nullable
	private static Properties fixLegacyClassnames(@Nullable Properties properties) {
		if (properties == null) {
			return null;
		}
		Map<Object, Object> collected = properties.entrySet().stream()
				.map(LarvaTool::rewriteClassName)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		Properties result = new Properties();
		result.putAll(collected);
		return result;
	}

	private static Entry<Object, Object> rewriteClassName(Entry<Object, Object> e) {
		Object propertyName = e.getKey();
		if (e.getValue() == null || !propertyName.toString().endsWith(QueueCreator.CLASS_NAME_PROPERTY_SUFFIX)) {
			return e;
		}
		String newClassName = e.getValue()
				.toString()
				.replace(ClassNameRewriter.LEGACY_PACKAGE_NAME, ClassNameRewriter.ORG_FRANKFRAMEWORK_PACKAGE_NAME)
				.replace(LEGACY_PACKAGE_NAME_LARVA, CURRENT_PACKAGE_NAME_LARVA);
		return Map.entry(propertyName, newClassName);
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
		for (Object o : properties.keySet()) {
			String property = (String) o;
			if ("configurations.directory".equalsIgnoreCase(property))
				continue;

			if (property.endsWith(".read") || property.endsWith(".write")
					|| property.endsWith(".directory")
					|| property.endsWith(".filename")
					|| property.endsWith(".valuefile")
					|| property.endsWith(".valuefileinputstream")) {
				String absolutePathProperty = property + ".absolutepath";
				String value = getAbsolutePath(propertiesDirectory, (String) properties.get(property));
				if (value != null) {
					absolutePathProperties.put(absolutePathProperty, value);
				}
			}
		}
		properties.putAll(absolutePathProperties);
	}

	public boolean closeQueues(Map<String, Queue> queues, Properties properties, String correlationId) {
		boolean remainingMessagesFound = false;
		debugMessage("Close jdbc connections");
		for (Map.Entry<String, Queue> entry : queues.entrySet()) {
			String name = entry.getKey();
			if ("org.frankframework.jdbc.FixedQuerySender".equals(properties.get(name + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX))) {
				Queue querySendersInfo = entry.getValue();
				FixedQuerySender prePostFixedQuerySender = (FixedQuerySender)querySendersInfo.get("prePostQueryFixedQuerySender");
				if (prePostFixedQuerySender != null) {
					try (PipeLineSession session = new PipeLineSession()) {
						/* Check if the preResult and postResult are not equal. If so, then there is a
						 * database change that has not been read in the scenario.
						 * So set remainingMessagesFound to true and show the entry.
						 * (see also executeFixedQuerySenderRead() )
						 */
						String preResult = (String)querySendersInfo.get("prePostQueryResult");
						session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
						String postResult = prePostFixedQuerySender.sendMessageOrThrow(getQueryFromSender(prePostFixedQuerySender), session).asString();
						if (!preResult.equals(postResult)) {
							Message message = null;
							FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
							try {
								message = readQueryFixedQuerySender.sendMessageOrThrow(getQueryFromSender(readQueryFixedQuerySender), session);
							} catch(TimeoutException e) {
								errorMessage("Time out on execute query for '" + name + "': " + e.getMessage(), e);
							} catch(SenderException e) {
								errorMessage("Could not execute query for '" + name + "': " + e.getMessage(), e);
							}
							if (message != null) {
								wrongPipelineMessage("Found remaining message on '" + name + "'", message);
							}

							remainingMessagesFound = true;

						}
						prePostFixedQuerySender.stop();
					} catch(TimeoutException e) {
						errorMessage("Time out on close (pre/post) '" + name + "': " + e.getMessage(), e);
					} catch(IOException | SenderException e) {
						errorMessage("Could not close (pre/post) '" + name + "': " + e.getMessage(), e);
					}
				}
				FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
				readQueryFixedQuerySender.stop();
			}
		}

		debugMessage("Close autoclosables");
		for (Map.Entry<String, Queue> entry : queues.entrySet()) {
			String queueName = entry.getKey();
			Queue value = entry.getValue();
			if(value instanceof QueueWrapper queue) {
				SenderThread senderThread = queue.getSenderThread();
				if (senderThread != null) {
					debugMessage("Found remaining SenderThread");
					SenderException senderException = senderThread.getSenderException();
					if (senderException != null) {
						errorMessage("Found remaining SenderException: " + senderException.getMessage(), senderException);
					}
					TimeoutException timeoutException = senderThread.getTimeoutException();
					if (timeoutException != null) {
						errorMessage("Found remaining TimeOutException: " + timeoutException.getMessage(), timeoutException);
					}
					Message message = senderThread.getResponse();
					if (message != null) {
						wrongPipelineMessage("Found remaining message on '" + queueName + "'", message);
					}
				}
				ListenerMessageHandler<?> listenerMessageHandler = queue.getMessageHandler();
				if (listenerMessageHandler != null) {
					ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage();
					while (listenerMessage != null) {
						Message message = listenerMessage.getMessage();
						if (listenerMessage.getContext() != null) {
							listenerMessage.getContext().close();
						}
						wrongPipelineMessage("Found remaining request message on '" + queueName + "'", message);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getRequestMessage();
					}
					listenerMessage = listenerMessageHandler.getResponseMessage();
					while (listenerMessage != null) {
						Message message = listenerMessage.getMessage();
						if (listenerMessage.getContext() != null) {
							listenerMessage.getContext().close();
						}
						wrongPipelineMessage("Found remaining response message on '" + queueName + "'", message);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getResponseMessage();
					}
				}

				try {
					queue.close();
					debugMessage("Closed queue '" + queueName + "'");
				} catch(Exception e) {
					errorMessage("Could not close '" + queueName + "': " + e.getMessage(), e);
				}
			}
		}

		return remainingMessagesFound;
	}

	private int executeQueueWrite(String stepDisplayName, Map<String, Queue> queues, String queueName, Message fileContent, String correlationId, Map<String, Object> xsltParameters) {
		Queue queue = queues.get(queueName);
		if (queue==null) {
			errorMessage("Property '" + queueName + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX + "' not found or not valid");
			return RESULT_ERROR;
		}
		int result = RESULT_ERROR;
		try {
			result = queue.executeWrite(stepDisplayName, fileContent, correlationId, xsltParameters);
			if (result == RESULT_OK) {
				debugPipelineMessage(stepDisplayName, "Successfully wrote message to '" + queueName + "':", fileContent);
				logger.debug("Successfully wrote message to '{}'", queueName);
			}
		} catch(TimeoutException e) {
			errorMessage("Time out sending message to '" + queueName + "': " + e.getMessage(), e);
		} catch(Exception e) {
			errorMessage("Could not send message to '" + queueName + "' ("+e.getClass().getSimpleName()+"): " + e.getMessage(), e);
		}
		return result;
	}

	private int executePullingListenerRead(String step, String stepDisplayName, Properties properties, Map<String, Queue> queues, String queueName, String fileName, Message fileContent) {
		int result = RESULT_ERROR;

		Queue queue = queues.get(queueName);
		if (!(queue instanceof QueueWrapper listenerInfo)) {
			errorMessage("Property '" + queueName + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX + "' not found or not valid");
			return RESULT_ERROR;
		}
		IPullingListener pullingListener = (IPullingListener) listenerInfo.get();
		Map<String, Object> threadContext = null;
		Message message = null;
		try {
			threadContext = pullingListener.openThread();
			RawMessageWrapper rawMessage = pullingListener.getRawMessage(threadContext);
			if (rawMessage != null) {
				message = pullingListener.extractMessage(rawMessage, threadContext);
				String correlationId = rawMessage.getId(); // NB: Historically this code extracted message-ID then used that as correlation-ID.
				listenerInfo.put("correlationId", correlationId);
			}
		} catch(ListenerException e) {
			if (!"".equals(fileName)) {
				errorMessage("Could not read PullingListener message from '" + queueName + "': " + e.getMessage(), e);
			}
		} finally {
			if (threadContext != null) {
				try {
					pullingListener.closeThread(threadContext);
				} catch(ListenerException e) {
					errorMessage("Could not close thread on PullingListener '" + queueName + "': " + e.getMessage(), e);
				}
			}
		}

		if (message == null || message.isEmpty()) {
			if ("".equals(fileName)) {
				result = RESULT_OK;
			} else {
				errorMessage("Could not read PullingListener message (null returned)");
			}
		} else {
			result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties);
		}

		return result;
	}


	private int executeQueueRead(String step, String stepDisplayName, Properties properties, Map<String, Queue> queues, String queueName, String fileName, Message fileContent) {
		int result = RESULT_ERROR;

		Queue queue = queues.get(queueName);
		if (queue == null) {
			errorMessage("Property '" + queueName + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX + "' not found or not valid");
			return RESULT_ERROR;
		}
		try {
			Message message = queue.executeRead(step, stepDisplayName, properties, fileName, fileContent);
			if (message == null) {
				if ("".equals(fileName)) {
					result = RESULT_OK;
				} else {
					errorMessage("Could not read from ["+queueName+"] (null returned)");
				}
			} else {
				if ("".equals(fileName)) {
					debugPipelineMessage(stepDisplayName, "Unexpected message read from '" + queueName + "':", message);
				} else {
					result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties);
				}
			}
		} catch (Exception e) {
			errorMessage("Could not read from ["+queueName+"] ("+e.getClass().getSimpleName()+"): " + e.getMessage(), e);
		}

		return result;
	}


	private int executeJavaListenerOrWebServiceListenerRead(String step, String stepDisplayName, Properties properties, Map<String, Queue> queues, String queueName, String fileName, Message fileContent, int parameterTimeout) {

		Queue listenerInfo = queues.get(queueName);
		if (listenerInfo == null) {
			errorMessage("Property '" + queueName + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX + "' not found or not valid");
			return RESULT_ERROR;
		}
		ListenerMessageHandler<?> listenerMessageHandler = (ListenerMessageHandler<?>)listenerInfo.get("listenerMessageHandler");
		if (listenerMessageHandler == null) {
			errorMessage("No ListenerMessageHandler found");
			return RESULT_ERROR;
		}

		Message message = null;
		ListenerMessage listenerMessage;
		long timeout;
		try {
			timeout = Long.parseLong((String) properties.get(queueName + ".timeout"));
			debugMessage("Timeout set to '" + timeout + "'");
		} catch (Exception e) {
			timeout = parameterTimeout;
		}
		try {
			listenerMessage = listenerMessageHandler.getRequestMessage(timeout);
		} catch (TimeoutException e) {
			errorMessage("Could not read listenerMessageHandler message (timeout of ["+parameterTimeout+"] reached)");
			return RESULT_ERROR;
		}

		if (listenerMessage != null) {
			message = listenerMessage.getMessage();
			listenerInfo.put("listenerMessage", listenerMessage);
		}
		int result = RESULT_ERROR;
		if (message == null) {
			if ("".equals(fileName)) {
				result = RESULT_OK;
			} else {
				errorMessage("Could not read listenerMessageHandler message (null returned)");
			}
		} else {
			if ("".equals(fileName)) {
				debugPipelineMessage(stepDisplayName, "Unexpected message read from '" + queueName + "':", message);
			} else {
				result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties);
				if (result!=RESULT_OK) {
					// Send a cleanup reply because there is probably a thread waiting for a reply
					listenerMessage = new ListenerMessage(TESTTOOL_CLEAN_UP_REPLY, new PipeLineSession());
					listenerMessageHandler.putResponseMessage(listenerMessage);
				}
			}
		}
		return result;
	}

	private int executeFixedQuerySenderRead(String step, String stepDisplayName, Properties properties, Map<String, Queue> queues, String queueName, String fileName, Message fileContent, String correlationId) {
		int result = RESULT_ERROR;

		Queue querySendersInfo = queues.get(queueName);
		if (querySendersInfo == null) {
			errorMessage("Property '" + queueName + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX + "' not found or not valid");
			return RESULT_ERROR;
		}
		Integer waitBeforeRead = (Integer)querySendersInfo.get("readQueryWaitBeforeRead");

		if (waitBeforeRead != null) {
			try {
				Thread.sleep(waitBeforeRead);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}
		boolean newRecordFound = true;
		FixedQuerySender prePostFixedQuerySender = (FixedQuerySender)querySendersInfo.get("prePostQueryFixedQuerySender");
		if (prePostFixedQuerySender != null) {
			try {
				String preResult = (String)querySendersInfo.get("prePostQueryResult");
				debugPipelineMessage(stepDisplayName, "Pre result '" + queueName + "':", new Message(preResult));
				String postResult;
				try (PipeLineSession session = new PipeLineSession()) {
					session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
					postResult = messageToString(prePostFixedQuerySender.sendMessageOrThrow(getQueryFromSender(prePostFixedQuerySender), session));
				}
				debugPipelineMessage(stepDisplayName, "Post result '" + queueName + "':", postResult);
				if (preResult.equals(postResult)) {
					newRecordFound = false;
				}
				/* Fill the preResult with postResult, so closeQueues is able to determine if there
				 * are remaining messages left.
				 */
				querySendersInfo.put("prePostQueryResult", postResult);
			} catch(TimeoutException e) {
				errorMessage("Time out on execute query for '" + queueName + "': " + e.getMessage(), e);
			} catch(SenderException e) {
				errorMessage("Could not execute query for '" + queueName + "': " + e.getMessage(), e);
			}
		}
		Message message = null;
		if (newRecordFound) {
			FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender) querySendersInfo.get("readQueryQueryFixedQuerySender");
			try (PipeLineSession session = new PipeLineSession()) {
				session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
				message = readQueryFixedQuerySender.sendMessageOrThrow(getQueryFromSender(readQueryFixedQuerySender), session);
			} catch(TimeoutException e) {
				errorMessage("Time out on execute query for '" + queueName + "': " + e.getMessage(), e);
			} catch (SenderException e) {
				errorMessage("Could not execute query for '" + queueName + "': " + e.getMessage(), e);
			}
		}
		if (message == null) {
			if ("".equals(fileName)) {
				result = RESULT_OK;
			} else {
				errorMessage("Could not read jdbc message (null returned) or no new message found (pre result equals post result)");
			}
		} else {
			if ("".equals(fileName)) {
				debugPipelineMessage(stepDisplayName, "Unexpected message read from '" + queueName + "':", message);
			} else {
				result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties);
			}
		}
		return result;
	}

	public static Message getQueryFromSender(FixedQuerySender sender) {
		return new Message(sender.getQuery());
	}

	protected int executeStep(String step, Properties properties, String stepDisplayName, Map<String, Queue> queues, String correlationId) {
		int stepPassed;
		String fileName = properties.getProperty(step);
		String fileNameAbsolutePath = properties.getProperty(step + ".absolutepath");
		int i = step.indexOf('.');
		String queueName;
		Message fileContent;
		// Set output filename, dirty old solution to pass the name on to the HTML-generating functions.
		stepOutputFilename = fileNameAbsolutePath;

		// Read the scenario file for this step
		if ("".equals(fileName)) {
			errorMessage("No file specified for step '" + step + "'");
			return RESULT_ERROR;
		}
		if (step.endsWith("readline") || step.endsWith("writeline")) {
			fileContent = new Message(fileName);
		} else {
			if (fileName.endsWith("ignore")) {
				debugMessage("creating dummy expected file for filename '"+fileName+"'");
				fileContent = new Message("ignore");
			} else {
				debugMessage("Read file " + fileName);
				fileContent = readFile(fileNameAbsolutePath);
			}
		}
		if (fileContent == null) {
			errorMessage("Could not read file '" + fileName + "'");
			return RESULT_ERROR;
		}
		queueName = step.substring(i + 1, step.lastIndexOf("."));
		if (step.endsWith(".read") || (allowReadlineSteps && step.endsWith(".readline"))) {
			Queue queue = queues.get(queueName);
			if (queue instanceof QueueWrapper wrap && wrap.get() instanceof IPullingListener) {
				stepPassed = executePullingListenerRead(step, stepDisplayName, properties, queues, queueName, fileName, fileContent);
			} else if ("org.frankframework.jdbc.FixedQuerySender".equals(properties.get(queueName + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX))) {
				stepPassed = executeFixedQuerySenderRead(step, stepDisplayName, properties, queues, queueName, fileName, fileContent, correlationId);
			} else if ("org.frankframework.http.WebServiceListener".equals(properties.get(queueName + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX))) {
				stepPassed = executeJavaListenerOrWebServiceListenerRead(step, stepDisplayName, properties, queues, queueName, fileName, fileContent, config.getTimeout());
			} else if ("org.frankframework.receivers.JavaListener".equals(properties.get(queueName + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX))) {
				stepPassed = executeJavaListenerOrWebServiceListenerRead(step, stepDisplayName, properties, queues, queueName, fileName, fileContent, config.getTimeout());
			} else if ("org.frankframework.larva.XsltProviderListener".equals(properties.get(queueName + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX))) {
				Map<String, Object> xsltParameters = createParametersMapFromParamProperties(properties, step);
				stepPassed = executeQueueWrite(stepDisplayName, queues, queueName, fileContent, correlationId, xsltParameters); // XsltProviderListener has .read and .write reversed
			} else {
				stepPassed = executeQueueRead(step, stepDisplayName, properties, queues, queueName, fileName, fileContent);
			}
		} else {
			// TODO: Try if anything breaks when this block is moved to `readFile()` method.
			String resolveProperties = properties.getProperty("scenario.resolveProperties");
			if(!"false".equalsIgnoreCase(resolveProperties)){
				String fileData = messageToString(fileContent);
				if (fileData == null) {
					errorMessage("Failed to resolve properties in inputfile");
					return RESULT_ERROR;
				}
				AppConstants appConstants = AppConstants.getInstance();
				fileContent = new Message(StringResolver.substVars(fileData, appConstants), fileContent.copyContext());
			}
			if ("org.frankframework.larva.XsltProviderListener".equals(properties.get(queueName + QueueCreator.CLASS_NAME_PROPERTY_SUFFIX))) {
				stepPassed = executeQueueRead(step, stepDisplayName, properties, queues, queueName, fileName, fileContent);  // XsltProviderListener has .read and .write reversed
			} else {
				stepPassed = executeQueueWrite(stepDisplayName, queues, queueName, fileContent, correlationId, null);
			}
		}

		return stepPassed;
	}

	public Message readFile(@Nonnull String fileName) {
		String encoding;
		if (fileName.endsWith(".xml") || fileName.endsWith(".wsdl")) {
			encoding = parseEncodingFromXml(fileName);
		} else if (fileName.endsWith(".utf8") || fileName.endsWith(".json")) {
			encoding = "UTF-8";
		} else if (fileName.endsWith(".ISO-8859-1")) {
			encoding = "ISO-8859-1";
		} else {
			encoding = null;
		}
		return new FileMessage(new File(fileName), encoding);
	}

	private @Nullable String parseEncodingFromXml(@Nonnull String fileName) {
		// Determine the encoding the XML way but don't use an XML parser to
		// read the file and transform it to a string to prevent changes in
		// formatting and prevent adding a xml declaration where this is
		// not present in the file. For example, when using a
		// WebServiceSender to send a message to a WebServiceListener the
		// xml message must not contain a xml declaration.
		try (InputStream in = new FileInputStream(fileName)) {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

			XMLStreamReader parser = factory.createXMLStreamReader(in);
			String encoding = parser.getEncoding();
			parser.close();
			return encoding;
		} catch (IOException | XMLStreamException e) {
			errorMessage("Could not determine encoding for file '" + fileName + "': " + e.getMessage(), e);
			return null;
		}
	}

	// Used by saveResultToFile.jsp
	public void windiff(String expectedFileName, String result, String expected) throws IOException, SenderException {
		AppConstants appConstants = AppConstants.getInstance();
		String windiffCommand = appConstants.getProperty("larva.windiff.command");
		if (windiffCommand == null) {
			List<String> scenariosRootDirectories = new ArrayList<>();
			List<String> scenariosRootDescriptions = new ArrayList<>();
			String currentScenariosRootDirectory = initScenariosRootDirectories(
					null, scenariosRootDirectories,
					scenariosRootDescriptions);
			windiffCommand = currentScenariosRootDirectory + "..\\..\\IbisAlgemeenWasbak\\WinDiff\\WinDiff.Exe";
		}
		File tempFileResult = writeTempFile(expectedFileName, result);
		File tempFileExpected = writeTempFile(expectedFileName, expected);
		String command = windiffCommand + " " + tempFileResult + " " + tempFileExpected;
		ProcessUtil.executeCommand(command);
		Files.delete(tempFileResult.toPath());
		Files.delete(tempFileExpected.toPath());
	}

	private static File writeTempFile(String originalFileName, String content) throws IOException {
		String encoding = getEncoding(originalFileName, content);

		String extension = FileUtils.getFileNameExtension(originalFileName);

		Path tempFile = createTempFile("." + extension);
		String tempFileMessage;
		if ("XML".equalsIgnoreCase(extension)) {
			tempFileMessage = XmlUtils.canonicalize(content);
		} else {
			tempFileMessage = content;
		}

		try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(tempFile, StandardOpenOption.APPEND), encoding)) {
			outputStreamWriter.write(tempFileMessage);
		}

		return tempFile.toFile();
	}

	/**
	 * Creates a temporary file inside the ${ibis.tmpdir} using the specified extension.
	 */
	private static Path createTempFile(final String extension) throws IOException {
		final Path tempDir = TemporaryDirectoryUtils.getTempDirectory();
		final String suffix = StringUtils.isNotEmpty(extension) ? extension : ".tmp";
		final String prefix = "frank";
		LogUtil.getLogger(LarvaTool.class).debug("creating tempfile prefix [{}] suffix [{}] directory [{}]", prefix, suffix, tempDir);
		return Files.createTempFile(tempDir, prefix, suffix);
	}

	// Used by saveResultToFile.jsp
	public static void writeFile(String fileName, String content) throws IOException {
		String encoding = getEncoding(fileName, content);

		try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(fileName), encoding)) {
			outputStreamWriter.write(content);
		}
	}

	private static String getEncoding(String fileName, String content) {
		String encoding = null;
		if (fileName.endsWith(".xml") || fileName.endsWith(".wsdl")) {
			if (content.startsWith("<?xml") && content.contains("?>")) {
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

	/**
	 * Read the message into a string.
	 * If the message had no data, return an empty string.
	 *
	 * If there was an error reading the message, return null and show an error message in the Larva output. If the message was read as part of a step,
	 * the caller should return {@link #RESULT_ERROR}.
	 *
	 * @param message Message to read
	 * @return Message data, or "" if the message was empty. Returns NULL if there was an error reading the message.
	 */
	private @Nullable String messageToString(Message message) {
		try {
			message.preserve();
			String r = message.asString();
			if (r == null) {
				return "";
			}
			return r;
		} catch (IOException e) {
			errorMessage("Could not read file into string", e);
			return null;
		}
	}

	public int compareResult(String step, String stepDisplayName, String fileName, Message expectedResultMessage, Message actualResultMessage, Properties properties) {
		if (fileName.endsWith("ignore")) {
			debugMessage("ignoring compare for filename '"+fileName+"'");
			return RESULT_OK;
		}
		String expectedResult = messageToString(expectedResultMessage);
		String actualResult = messageToString(actualResultMessage);
		if (expectedResult == null || actualResult == null) {
			return RESULT_ERROR;
		}

		int ok = RESULT_ERROR;
		String printableExpectedResult;
		String printableActualResult;
		String diffType = properties.getProperty(step + ".diffType");
		if (".json".equals(diffType) || (diffType == null && fileName.endsWith(".json"))) {
			try {
				printableExpectedResult = Misc.jsonPretty(expectedResult);
			} catch (JsonException e) {
				debugMessage("Could not prettify Json: "+e.getMessage());
				printableExpectedResult = expectedResult;
			}
			try {
				printableActualResult = Misc.jsonPretty(actualResult);
			} catch (JsonException e) {
				debugMessage("Could not prettify Json: "+e.getMessage());
				printableActualResult = actualResult;
			}
		} else {
			printableExpectedResult = XmlEncodingUtils.replaceNonValidXmlCharacters(expectedResult);
			printableActualResult = XmlEncodingUtils.replaceNonValidXmlCharacters(actualResult);
		}

		// Map all identifier based properties once
		HashMap<String, HashMap<String, HashMap<String, String>>> ignoreMap = mapPropertiesToIgnores(properties);

		String preparedExpectedResult = prepareResultForCompare(printableExpectedResult, properties, ignoreMap);
		String preparedActualResult = prepareResultForCompare(printableActualResult, properties, ignoreMap);


		if (".xml".equals(diffType) || ".wsdl".equals(diffType)
				|| diffType == null && (fileName.endsWith(".xml") || fileName.endsWith(".wsdl"))) {
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
				debugMessage("Strings are identical");
				debugPipelineMessage(stepDisplayName, "Result", printableActualResult);
				debugPipelineMessagePreparedForDiff(stepDisplayName, "Result as prepared for diff", preparedActualResult);
			} else {
				debugMessage("Strings are not identical");
				String message;
				if (diffException == null) {
					message = diff.toString();
				} else {
					message = "Exception during XML diff: " + diffException.getMessage();
					errorMessage("Exception during XML diff: ", diffException);
				}
				wrongPipelineMessage(stepDisplayName, message, printableActualResult, printableExpectedResult);
				wrongPipelineMessagePreparedForDiff(stepDisplayName, preparedActualResult, preparedExpectedResult);
				if (autoSaveDiffs) {
					String filenameAbsolutePath = (String)properties.get(step + ".absolutepath");
					debugMessage("Copy actual result to ["+filenameAbsolutePath+"]");
					try {
						org.apache.commons.io.FileUtils.writeStringToFile(new File(filenameAbsolutePath), actualResult, Charset.defaultCharset());
					} catch (IOException e) {
						// Ignore
					}
					ok = RESULT_AUTOSAVED;
				}
			}
		} else {
			// txt diff
			String formattedPreparedExpectedResult = formatString(preparedExpectedResult);
			String formattedPreparedActualResult = formatString(preparedActualResult);
			if (formattedPreparedExpectedResult.equals(formattedPreparedActualResult)) {
				ok = RESULT_OK;
				debugMessage("Strings are identical");
				debugPipelineMessage(stepDisplayName, "Result", printableActualResult);
				debugPipelineMessagePreparedForDiff(stepDisplayName, "Result as prepared for diff", preparedActualResult);
			} else {
				debugMessage("Strings are not identical");
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
				wrongPipelineMessage(stepDisplayName, message, printableActualResult, printableExpectedResult);
				wrongPipelineMessagePreparedForDiff(stepDisplayName, preparedActualResult, preparedExpectedResult);
				if (autoSaveDiffs) {
					String filenameAbsolutePath = (String)properties.get(step + ".absolutepath");
					debugMessage("Copy actual result to ["+filenameAbsolutePath+"]");
					try {
						org.apache.commons.io.FileUtils.writeStringToFile(new File(filenameAbsolutePath), actualResult, Charset.defaultCharset());
					} catch (IOException e) {
						// Ignore
					}
					ok = RESULT_AUTOSAVED;
				}
			}
		}
		return ok;
	}

	public String prepareResultForCompare(String input, Properties properties, Map<String, HashMap<String, HashMap<String, String>>> ignoreMap) {
		String result = input;
		result = doActionBetweenKeys("decodeUnzipContentBetweenKeys", result, properties, ignoreMap, (value, pp, key1, key2)-> {
			boolean replaceNewlines = !"true".equals(pp.apply("replaceNewlines"));
			return decodeUnzipContentBetweenKeys(value, key1, key2, replaceNewlines);
		});

		result = doActionBetweenKeys("canonicaliseFilePathContentBetweenKeys", result, properties, ignoreMap, (value, pp, key1, key2)->canonicaliseFilePathContentBetweenKeys(value,key1,key2));
		result = doActionBetweenKeys("formatDecimalContentBetweenKeys", result, properties, ignoreMap, (value, pp, key1, key2)->formatDecimalContentBetweenKeys(value,key1,key2));
		result = doActionWithSingleKey("ignoreRegularExpressionKey", result, properties, ignoreMap, (value, pp, key)->ignoreRegularExpression(value,key));
		result = doActionWithSingleKey("removeRegularExpressionKey", result, properties, ignoreMap, (value, pp, key)->removeRegularExpression(value,key));

		result = doActionBetweenKeys("replaceRegularExpressionKeys", result, properties, ignoreMap, (value, pp, key1, key2)->replaceRegularExpression(value,key1,key2));
		result = doActionBetweenKeys("ignoreContentBetweenKeys", result, properties, ignoreMap, (value, pp, key1, key2)->ignoreContentBetweenKeys(value,key1,key2));
		result = doActionBetweenKeys("ignoreKeysAndContentBetweenKeys", result, properties, ignoreMap, (value, pp, key1, key2)->ignoreKeysAndContentBetweenKeys(value,key1,key2));
		result = doActionBetweenKeys("removeKeysAndContentBetweenKeys", result, properties, ignoreMap, (value, pp, key1, key2)->removeKeysAndContentBetweenKeys(value,key1,key2));

		result = doActionWithSingleKey("ignoreKey", result, properties, ignoreMap, (value, pp, key)->ignoreKey(value,key));
		result = doActionWithSingleKey("removeKey", result, properties, ignoreMap, (value, pp, key)->removeKey(value,key));

		result = doActionBetweenKeys("replaceKey", result, properties, ignoreMap, (value, pp, key1, key2)->replaceKey(value,key1,key2));
		result = doActionBetweenKeys("replaceEverywhereKey", result, properties, ignoreMap, (value, pp, key1, key2)->replaceKey(value,key1,key2));

		result = doActionBetweenKeys("ignoreCurrentTimeBetweenKeys", result, properties, ignoreMap, (value, pp, key1, key2)-> {
			String pattern = pp.apply("pattern");
			String margin = pp.apply("margin");
			boolean errorMessageOnRemainingString = !"false".equals(pp.apply("errorMessageOnRemainingString"));
			return ignoreCurrentTimeBetweenKeys(value, key1, key2, pattern, margin, errorMessageOnRemainingString, false);
		});

		result = doActionWithSingleKey("ignoreContentBeforeKey", result, properties, ignoreMap, (value, pp, key)->ignoreContentBeforeKey(value,key));
		result = doActionWithSingleKey("ignoreContentAfterKey", result, properties, ignoreMap, (value, pp, key)->ignoreContentAfterKey(value,key));
		return result;
	}

	public interface BetweenKeysAction {
		String format(String value, Function<String,String> propertyProvider, String key1, String key2);
	}
	public interface SingleKeyAction {
		String format(String value, Function<String, String> propertyProvider, String key1);
	}

	public String doActionBetweenKeys(String key, String value, Properties properties, Map<String, HashMap<String, HashMap<String, String>>> ignoreMap, BetweenKeysAction action) {
		String result = value;
		debugMessage("Check " + key + " properties");
		boolean lastKeyIndexProcessed = false;
		int i = 1;
		while (!lastKeyIndexProcessed) {
			String keyPrefix = key + i + ".";
			String key1 = properties.getProperty(keyPrefix + "key1");
			String key2 = properties.getProperty(keyPrefix + "key2");
			if (key1 != null && key2 != null) {
				debugMessage(key + " between key1 '" + key1 + "' and key2 '" + key2 + "'");
				result = action.format(result, k -> properties.getProperty(keyPrefix + k), key1, key2);
				i++;
			} else if (key1 != null || key2 != null) {
				throw new IllegalArgumentException("Error in Larva scenario file: Spec for [" + key + i + "] is incomplete; key1=[" + key1 + "], key2=[" + key2 + "]");
			} else {
				lastKeyIndexProcessed = true;
			}
		}

		HashMap<String, HashMap<String, String>> keySpecMap = ignoreMap.get(key);
		if (keySpecMap != null) {
			for (Entry<String, HashMap<String, String>> spec : keySpecMap.entrySet()) {
				HashMap<String, String> keyPair = spec.getValue();

				String key1 = keyPair.get("key1");
				String key2 = keyPair.get("key2");

				if (key1 == null || key2 == null) {
					throw new IllegalArgumentException("Error in Larva scenario file: Spec [" + key + "." + spec.getKey() + "] is incomplete; key1=[" + key1 + "], key2=[" + key2 + "]");
				}

				debugMessage(key + " between key1 '" + key1 + "' and key2 '" + key2 + "'");
				result = action.format(result, keyPair::get, key1, key2);
			}
		}

		return result;
	}

	public String doActionWithSingleKey(String keyName, String value, Properties properties, Map<String, HashMap<String, HashMap<String, String>>> ignoreMap, SingleKeyAction action) {
		String result = value;
		debugMessage("Check " + keyName + " properties");
		boolean lastKeyIndexProcessed = false;
		int i = 1;
		while (!lastKeyIndexProcessed) {
			String keyPrefix = keyName + i;
			String key = properties.getProperty(keyPrefix);
			if (key == null) {
				key = properties.getProperty(keyPrefix + ".key");
			}
			if (key != null) {
				debugMessage(keyName+ " key '" + key + "'");
				result = action.format(result, k -> properties.getProperty(keyPrefix + "." + k), key);
				i++;
			} else {
				lastKeyIndexProcessed = true;
			}
		}

		HashMap<String, HashMap<String, String>> keySpecMap = ignoreMap.get(keyName);
		if (keySpecMap != null) {
			Iterator<Entry<String,HashMap<String,String>>> keySpecIt = keySpecMap.entrySet().iterator();
			while (keySpecIt.hasNext()) {
				Entry<String,HashMap<String,String>> spec = keySpecIt.next();
				HashMap<String, String> keyPair = spec.getValue();

				String key = keyPair.get("key");

				debugMessage(keyName + " key '" + key + "'");
				result = action.format(result, keyPair::get, key);

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

	public String decodeUnzipContentBetweenKeys(String string, String key1, String key2, boolean replaceNewlines) {
		String result = string;
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			debugMessage("Key 1 found");
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				debugMessage("Key 2 found");
				String encoded = result.substring(i + key1.length(), j);
				String unzipped;
				byte[] decodedBytes;
				debugMessage("Decode");
				decodedBytes = Base64.decodeBase64(encoded);
				try {
					debugMessage("Unzip");
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append("<tt:file xmlns:tt=\"testtool\">");
					ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(decodedBytes));
					stringBuilder.append("<tt:name>").append(zipInputStream.getNextEntry().getName()).append("</tt:name>");
					stringBuilder.append("<tt:content>");
					byte[] buffer = new byte[1024];
					int readLength = zipInputStream.read(buffer);
					while (readLength != -1) {
						String part = new String(buffer, 0, readLength, StandardCharsets.UTF_8);
						if (replaceNewlines) {
							part = StringUtils.replace(StringUtils.replace(part, "\r", "[CARRIAGE RETURN]"), "\n", "[LINE FEED]");
						}
						stringBuilder.append(part);
						readLength = zipInputStream.read(buffer);
					}
					stringBuilder.append("</tt:content>");
					stringBuilder.append("</tt:file>");
					unzipped = stringBuilder.toString();
				} catch (Exception e) {
					errorMessage("Could not unzip: " + e.getMessage(), e);
					unzipped = encoded;
				}
				result = result.substring(0, i) + key1 + unzipped + result.substring(j);
				i = result.indexOf(key1, i + key1.length() + unzipped.length() + key2.length());
			} else {
				i = -1;
			}
		}
		return result;
	}

	public String canonicaliseFilePathContentBetweenKeys(String string, String key1, String key2) {
		String result = string;
		if ("*".equals(key1) && "*".equals(key2)) {
			File file = new File(result);
			try {
				result = file.getCanonicalPath();
			} catch (IOException e) {
				errorMessage("Could not canonicalise filepath: " + e.getMessage(), e);
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
						errorMessage("Could not canonicalise filepath: " + e.getMessage(), e);
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

	public String ignoreCurrentTimeBetweenKeys(String string, String key1, String key2, String pattern, String margin, boolean errorMessageOnRemainingString, boolean isControlString) {
		String result = string;
		String ignoreText = "IGNORE_CURRENT_TIME";
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			debugMessage("Key 1 found");
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				debugMessage("Key 2 found");
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
											 + dateString.substring(parsePosition.getIndex()));
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
								errorMessage("Dates differ too much. Current time: '" + currentTime + "'. Result time: '" + dateString + "'");
								i = result.indexOf(key1, j + key2.length());
							}
						}
					}
				} catch(ParseException e) {
					i = -1;
					errorMessage("Could not parse margin or date: " + e.getMessage(), e);
				} catch(NumberFormatException e) {
					i = -1;
					errorMessage("Could not parse long value: " + e.getMessage(), e);
				}
			} else {
				i = -1;
			}
		}
		return result;
	}

	public String formatDecimalContentBetweenKeys(String string,
		String key1, String key2) {
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
							e);
				}
			} else {
				i = -1;
			}
		}
		return result;
	}

	private static String format(double d) {
		if (d == (long) d)
			return "%d".formatted((long) d);
		else
			return "%s".formatted(d);
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
	 *
	 * @param properties Properties object from which to create the map
	 * @param property   Property name to use as base name
	 * @return A map with parameters
	 */
	public Map<String, Object> createParametersMapFromParamProperties(Properties properties, String property) {
		debugMessage("Search parameters for property '" + property + "'");
		final String _name = ".name";
		final String _param = ".param";
		final String _type = ".type";
		Map<String, Object> result = new HashMap<>();
		boolean processed = false;
		int i = 1;
		while (!processed) {
			String name = properties.getProperty(property + _param + i + _name);
			if (name != null) {
				String type = properties.getProperty(property + _param + i + _type);
				String propertyValue = properties.getProperty(property + _param + i + ".value");
				Object value = propertyValue;

				if (value == null) {
					String filename = properties.getProperty(property + _param + i + ".valuefile.absolutepath");
					if (filename != null) {
						value = new FileMessage(new File(filename));
					} else {
						String inputStreamFilename = properties.getProperty(property + _param + i + ".valuefileinputstream.absolutepath");
						if (inputStreamFilename != null) {
							errorMessage("valuefileinputstream is no longer supported use valuefile instead");
						}
					}
				}
				if ("node".equals(type)) {
					try {
						value = XmlUtils.buildNode(MessageUtils.asString(value), true);
					} catch (DomBuilderException | IOException e) {
						errorMessage("Could not build node for parameter '" + name + "' with value: " + value, e);
					}
				} else if ("domdoc".equals(type)) {
					try {
						value = XmlUtils.buildDomDocument(MessageUtils.asString(value), true);
					} catch (DomBuilderException | IOException e) {
						errorMessage("Could not build node for parameter '" + name + "' with value: " + value, e);
					}
				} else if ("list".equals(type)) {
					value = StringUtil.split(propertyValue);
				} else if ("map".equals(type)) {
					List<String> parts = StringUtil.split(propertyValue);
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
				}
				if (value == null) {
					errorMessage("Property '" + property + _param + i + ".value' or '" + property + _param + i + ".valuefile' not found while property '" + property + _param + i + ".name' exist");
				} else {
					result.put(name, value);
					debugMessage("Add param with name '" + name + "' and value '" + value + "' for property '" + property + "'");
				}
				i++;
			} else {
				processed = true;
			}
		}
		return result;
	}

	public String formatString(String string) {
		StringBuilder sb = new StringBuilder();
		try {
			Reader reader = new StringReader(string);
			BufferedReader br = new BufferedReader(reader);
			String l;
			while ((l = br.readLine()) != null) {
				if (sb.isEmpty()) {
					sb.append(l);
				} else {
					sb.append(System.lineSeparator()).append(l);
				}
			}
			br.close();
		} catch(Exception e) {
			errorMessage("Could not read string '" + string + "': " + e.getMessage(), e);
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
			List<String> attributes = findAttributesForIgnore(ignore);

			if(attributes != null){
				// Extract identifier
				String id = key.split(Pattern.quote("."))[1];

				// Find return map for ignore
				// Create return map for ignore if not exist
				HashMap<String, HashMap<String, String>> ignoreMap = returnMap.computeIfAbsent(ignore, k -> new HashMap<>());


				// Find return map for identifier
				// Create return map for identifier if not exist
				HashMap<String, String> idMap = ignoreMap.computeIfAbsent(id, k -> new HashMap<>());


				// Check attributes are provided
				if(!attributes.isEmpty()){
					// Loop through attributes to be searched for
					for (String attribute : attributes) {
						if(key.endsWith("." + attribute)){
							idMap.put(attribute, properties.getProperty(key));
						}
						else if(attribute.isEmpty()){
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
	 * Caller of mapPropertiesByIdentifier() should not necessarily know about all attributes related to an ignore.
	 *
	 * @param propertyName The name of the ignore we are checking, in the example 'ignoreContentBetweenKeys'
	*/
	public static List<String> findAttributesForIgnore(String propertyName) {
		return switch (propertyName) {
			case "decodeUnzipContentBetweenKeys" -> List.of("key1", "key2", "replaceNewlines");
			case "canonicaliseFilePathContentBetweenKeys", "replaceRegularExpressionKeys", "ignoreContentBetweenKeys", "ignoreKeysAndContentBetweenKeys",
				 "removeKeysAndContentBetweenKeys", "replaceKey", "formatDecimalContentBetweenKeys", "replaceEverywhereKey" -> List.of("key1", "key2");
			case "ignoreRegularExpressionKey", "removeRegularExpressionKey", "ignoreContentBeforeKey", "ignoreContentAfterKey" -> List.of("key");
			case "ignoreCurrentTimeBetweenKeys" -> List.of("key1", "key2", "pattern", "margin", "errorMessageOnRemainingString");
			case "ignoreKey", "removeKey" ->
				// in case of an empty string as attribute, assume it should read the value
				// ie: ignoreKey.identifier=value
					List.of("key", "");
			default -> null;
		};
	}
}
