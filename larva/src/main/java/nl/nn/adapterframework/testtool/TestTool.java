package nl.nn.adapterframework.testtool;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.zip.ZipInputStream;

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
import org.apache.logging.log4j.Logger;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.DocumentException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import com.sun.syndication.io.XmlReader;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
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
import nl.nn.adapterframework.lifecycle.IbisApplicationServlet;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.senders.DelaySender;
import nl.nn.adapterframework.senders.IbisJavaSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CaseInsensitiveComparator;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.ProcessUtil;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * @author Jaco de Groot
 */
public class TestTool {
	private static Logger logger = LogUtil.getLogger(TestTool.class);
	public static final String LOG_LEVEL_ORDER = "[debug], [pipeline messages prepared for diff], [pipeline messages], [wrong pipeline messages prepared for diff], [wrong pipeline messages], [step passed/failed], [scenario passed/failed], [scenario failed], [totals], [error]";
	private static final String STEP_SYNCHRONIZER = "Step synchronizer";
	protected static final int DEFAULT_TIMEOUT = 30000;
	protected static final String TESTTOOL_CORRELATIONID = "Test Tool correlation id";
	protected static final String TESTTOOL_BIFNAME = "Test Tool bif name";
	protected static final nl.nn.adapterframework.stream.Message TESTTOOL_DUMMY_MESSAGE = new nl.nn.adapterframework.stream.Message("<TestTool>Dummy message</TestTool>");
	protected static final String TESTTOOL_CLEAN_UP_REPLY = "<TestTool>Clean up reply</TestTool>";
	private static final int RESULT_ERROR = 0;
	private static final int RESULT_OK = 1;
	private static final int RESULT_AUTOSAVED = 2;
	// dirty solution by Marco de Reus:
	private static String zeefVijlNeem = "";
	private static Writer silentOut = null;
	private static boolean autoSaveDiffs = false;
	
	private static int globalTimeout=DEFAULT_TIMEOUT;

	public static void setTimeout(int newTimeout) {
		globalTimeout=newTimeout;
	}
	
	public static IbisContext getIbisContext(ServletContext application) {
		return IbisApplicationServlet.getIbisContext(application);
	}

	public static AppConstants getAppConstants(IbisContext ibisContext) {
		// Load AppConstants using a class loader to get an instance that has
		// resolved application.server.type in ServerSpecifics*.properties,
		// SideSpecifics*.properties and StageSpecifics*.properties filenames
		// See IbisContext.setDefaultApplicationServerType() and userstory
		// 'Refactor ConfigurationServlet en AppConstants' too.
		Configuration configuration = ibisContext.getIbisManager().getConfigurations().get(0);
		AppConstants appConstants = AppConstants.getInstance(configuration.getClassLoader());
		return appConstants;
	}

	public static void runScenarios(ServletContext application, HttpServletRequest request, Writer out) {
		runScenarios(application, request, out, false);
	}

	public static void runScenarios(ServletContext application, HttpServletRequest request, Writer out, boolean silent) {
		String paramLogLevel = request.getParameter("loglevel");
		String paramAutoScroll = request.getParameter("autoscroll");
		String paramExecute = request.getParameter("execute");
		String paramWaitBeforeCleanUp = request.getParameter("waitbeforecleanup");
		String servletPath = request.getServletPath();
		int i = servletPath.lastIndexOf('/');
		String realPath = application.getRealPath(servletPath.substring(0, i));
		String paramScenariosRootDirectory = request.getParameter("scenariosrootdirectory");
		IbisContext ibisContext = getIbisContext(application);
		AppConstants appConstants = getAppConstants(ibisContext);
		runScenarios(ibisContext, appConstants, paramLogLevel,
				paramAutoScroll, paramExecute, paramWaitBeforeCleanUp,
				realPath, paramScenariosRootDirectory, out, silent);
	}

	public static final int ERROR_NO_SCENARIO_DIRECTORIES_FOUND=-1;
	/**
	 * 
	 * @return negative: error condition
	 * 		   0: all scenarios passed
	 * 		   positive: number of scenarios that failed
	 */
	public static int runScenarios(IbisContext ibisContext, AppConstants appConstants, String paramLogLevel,
			String paramAutoScroll, String paramExecute, String paramWaitBeforeCleanUp,
			String realPath, String paramScenariosRootDirectory,
			Writer out, boolean silent) {
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
				appConstants, realPath,
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
		printHtmlForm(scenariosRootDirectories, scenariosRootDescriptions, currentScenariosRootDirectory, appConstants, allScenarioFiles, waitBeforeCleanUp, paramExecute, autoScroll, writers);
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
					int scenarioPassed = RESULT_ERROR;
					File scenarioFile = (File)scenarioFilesIterator.next();
			
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
								Map<String, Map<String, Object>> queues = openQueues(scenarioDirectory, steps, properties, ibisContext, appConstants, writers);
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
										int stepPassed = executeStep(step, properties, stepDisplayName, queues, writers);
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
									boolean remainingMessagesFound = closeQueues(queues, properties, writers);
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
						if (silent) {
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
				printHtmlForm(scenariosRootDirectories, scenariosRootDescriptions, currentScenariosRootDirectory, appConstants, allScenarioFiles, waitBeforeCleanUp, paramExecute, autoScroll, writers);
				debugMessage("Stop logging to htmlbuffer", writers);
				if (writers!=null) {
					writers.put("usehtmlbuffer", "stop");
				}
				writeHtml("", writers, true);
			}
		}
		return scenariosFailed;
	}

	public static void printHtmlForm(List<String> scenariosRootDirectories, List<String> scenariosRootDescriptions, String scenariosRootDirectory, AppConstants appConstants, List<File> scenarioFiles, int waitBeforeCleanUp, String paramExecute, String autoScroll, Map<String, Object> writers) {
		if (writers!=null) {
			writeHtml("<form action=\"index.jsp\" method=\"post\">", writers, false);

			writeHtml("<table>", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>Scenario(s)</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>", writers, false);
			writeHtml("<select name=\"execute\">", writers, false);
			debugMessage("Fill execute select box.", writers);
			Set<String> addedDirectories = new HashSet<String>();
			Iterator<File> scenarioFilesIterator = scenarioFiles.iterator();
			while (scenarioFilesIterator.hasNext()) {
				File scenarioFile = (File)scenarioFilesIterator.next();
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
			writeHtml("</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("</table>", writers, false);

			writeHtml("<table align=\"left\">", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>Scenarios root directory</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>", writers, false);
			writeHtml("<select name=\"scenariosrootdirectory\">", writers, false);
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
			writeHtml("</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("</table>", writers, false);

			// Use a span to make IE put table on next line with a smaller window width
			writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", writers, false);
			writeHtml("<table align=\"left\">", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>Wait before clean up (ms)</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>", writers, false);
			writeHtml("<input type=\"text\" name=\"waitbeforecleanup\" value=\"" + waitBeforeCleanUp + "\">", writers, false);
			writeHtml("</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("</table>", writers, false);

			writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", writers, false);
			writeHtml("<table align=\"left\">", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>Log level</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>", writers, false);
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
			writeHtml("</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("</table>", writers, false);

			writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", writers, false);
			writeHtml("<table align=\"left\">", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>Auto scroll</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>", writers, false);
			writeHtml("<input type=\"checkbox\" name=\"autoscroll\" value=\"true\"", writers, false);
			if (autoScroll.equals("true")) {
				writeHtml(" checked", writers, false);
			}
			writeHtml(">", writers, false);
			writeHtml("</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("</table>", writers, false);

			writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>", writers, false);
			writeHtml("<table align=\"left\">", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td>&nbsp;</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("<tr>", writers, false);
			writeHtml("<td align=\"right\">", writers, false);
			writeHtml("<input type=\"submit\" name=\"submit\" value=\"start\">", writers, false);
			writeHtml("</td>", writers, false);
			writeHtml("</tr>", writers, false);
			writeHtml("</table>", writers, false);

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

	public static String initScenariosRootDirectories(
			AppConstants appConstants, String realPath,
			String paramScenariosRootDirectory,
			List<String> scenariosRootDirectories, List<String> scenariosRootDescriptions,
			Map<String, Object> writers) {
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
						scenariosRoots.put(description, directory);
					} else {
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
				if (key.startsWith("step" + i + ".") && (key.endsWith(".read") || key.endsWith(".write"))) {
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

	public static Map<String, Map<String, Object>> openQueues(String scenarioDirectory, List<String> steps,
			Properties properties, IbisContext ibisContext,
			AppConstants appConstants, Map<String, Object> writers) {
		Map<String, Map<String, Object>> queues = new HashMap<String, Map<String, Object>>();
		debugMessage("Get all queue names", writers);
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
			String key = (String)iterator.next();
			int i = key.indexOf('.');
			if (i != -1) {
				int j = key.indexOf('.', i + 1);
				if (j != -1) {
					String queueName = key.substring(0, j);
					debugMessage("queuename openqueue: " + queueName, writers);
					if ("nl.nn.adapterframework.jms.JmsSender".equals(properties.get(queueName + ".className"))
							&& !jmsSenders.contains(queueName)) {
						debugMessage("Adding jmsSender queue: " + queueName, writers);
						jmsSenders.add(queueName);
					} else if ("nl.nn.adapterframework.jms.JmsListener".equals(properties.get(queueName + ".className"))
							&& !jmsListeners.contains(queueName)) {
						debugMessage("Adding jmsListener queue: " + queueName, writers);
						jmsListeners.add(queueName);
					} else if ("nl.nn.adapterframework.jdbc.FixedQuerySender".equals(properties.get(queueName + ".className"))
							&& !jdbcFixedQuerySenders.contains(queueName)) {
						debugMessage("Adding jdbcFixedQuerySender queue: " + queueName, writers);
						jdbcFixedQuerySenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(properties.get(queueName + ".className"))
							&& !ibisWebServiceSenders.contains(queueName)) {
						debugMessage("Adding ibisWebServiceSender queue: " + queueName, writers);
						ibisWebServiceSenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className"))
							&& !webServiceSenders.contains(queueName)) {
						debugMessage("Adding webServiceSender queue: " + queueName, writers);
						webServiceSenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))
							&& !webServiceListeners.contains(queueName)) {
						debugMessage("Adding webServiceListener queue: " + queueName, writers);
						webServiceListeners.add(queueName);
					} else if ("nl.nn.adapterframework.http.HttpSender".equals(properties.get(queueName + ".className"))
							&& !httpSenders.contains(queueName)) {
						debugMessage("Adding httpSender queue: " + queueName, writers);
						httpSenders.add(queueName);
					} else if ("nl.nn.adapterframework.senders.IbisJavaSender".equals(properties.get(queueName + ".className"))
							&& !ibisJavaSenders.contains(queueName)) {
						debugMessage("Adding ibisJavaSender queue: " + queueName, writers);
						ibisJavaSenders.add(queueName);
					} else if ("nl.nn.adapterframework.senders.DelaySender".equals(properties.get(queueName + ".className"))
							&& !delaySenders.contains(queueName)) {
						debugMessage("Adding delaySender queue: " + queueName, writers);
						delaySenders.add(queueName);
					} else if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className"))
							&& !javaListeners.contains(queueName)) {
						debugMessage("Adding javaListener queue: " + queueName, writers);
						javaListeners.add(queueName);
					} else if ("nl.nn.adapterframework.testtool.FileSender".equals(properties.get(queueName + ".className"))
							&& !fileSenders.contains(queueName)) {
						debugMessage("Adding fileSender queue: " + queueName, writers);
						fileSenders.add(queueName);
					} else if ("nl.nn.adapterframework.testtool.FileListener".equals(properties.get(queueName + ".className"))
							&& !fileListeners.contains(queueName)) {
						debugMessage("Adding fileListener queue: " + queueName, writers);
						fileListeners.add(queueName);
					} else if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))
							&& !xsltProviderListeners.contains(queueName)) {
						debugMessage("Adding xsltProviderListeners queue: " + queueName, writers);
						xsltProviderListeners.add(queueName);
					}
				}
			}
		}

		debugMessage("Initialize jms senders", writers);
		iterator = jmsSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String queue = (String)properties.get(queueName + ".queue");
			if (queue == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find property '" + queueName + ".queue'", writers);
			} else {
				JmsSender jmsSender = (JmsSender)ibisContext.createBeanAutowireByName(JmsSender.class);
				jmsSender.setName("Test Tool JmsSender");
				jmsSender.setDestinationName(queue);
				jmsSender.setDestinationType("QUEUE");
				jmsSender.setAcknowledgeMode("auto");
				String jmsRealm = (String)properties.get(queueName + ".jmsRealm");
				if (jmsRealm!=null) {
					jmsSender.setJmsRealm(jmsRealm);
				} else {
					jmsSender.setJmsRealm("default");
				}
				String deliveryMode = properties.getProperty(queueName + ".deliveryMode");
				debugMessage("Property '" + queueName + ".deliveryMode': " + deliveryMode, writers);
				String persistent = properties.getProperty(queueName + ".persistent");
				debugMessage("Property '" + queueName + ".persistent': " + persistent, writers);
				String useCorrelationIdFrom = properties.getProperty(queueName + ".useCorrelationIdFrom");
				debugMessage("Property '" + queueName + ".useCorrelationIdFrom': " + useCorrelationIdFrom, writers);
				String replyToName = properties.getProperty(queueName + ".replyToName");
				debugMessage("Property '" + queueName + ".replyToName': " + replyToName, writers);
				if (deliveryMode != null) {
					debugMessage("Set deliveryMode to " + deliveryMode, writers);
					jmsSender.setDeliveryMode(deliveryMode);
				}
				if ("true".equals(persistent)) {
					debugMessage("Set persistent to true", writers);
					jmsSender.setPersistent(true);
				} else {
					debugMessage("Set persistent to false", writers);
					jmsSender.setPersistent(false);
				}
				if (replyToName != null) {
					debugMessage("Set replyToName to " + replyToName, writers);
					jmsSender.setReplyToName(replyToName);
				}
				Map<String, Object> jmsSenderInfo = new HashMap<String, Object>();
				jmsSenderInfo.put("jmsSender", jmsSender);
				jmsSenderInfo.put("useCorrelationIdFrom", useCorrelationIdFrom);
				String correlationId = properties.getProperty(queueName + ".jmsCorrelationId");
				if (correlationId!=null) {
					jmsSenderInfo.put("jmsCorrelationId", correlationId);
					debugMessage("Property '" + queueName + ".jmsCorrelationId': " + correlationId, writers);
				}
				queues.put(queueName, jmsSenderInfo);
				debugMessage("Opened jms sender '" + queueName + "'", writers);
			}
		}

		debugMessage("Initialize jms listeners", writers);
		iterator = jmsListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String queue = (String)properties.get(queueName + ".queue");
			String timeout = (String)properties.get(queueName + ".timeout");

			int nTimeout = globalTimeout;
			if (timeout != null && timeout.length() > 0) {
				nTimeout = Integer.parseInt(timeout);
				debugMessage("Overriding default timeout setting of "+globalTimeout+" with "+ nTimeout, writers);
			}

			if (queue == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find property '" + queueName + ".queue'", writers);
			} else {
				PullingJmsListener pullingJmsListener = (PullingJmsListener)ibisContext.createBeanAutowireByName(PullingJmsListener.class);
				pullingJmsListener.setName("Test Tool JmsListener");
				pullingJmsListener.setDestinationName(queue);
				pullingJmsListener.setDestinationType("QUEUE");
				pullingJmsListener.setAcknowledgeMode("auto");
				String jmsRealm = (String)properties.get(queueName + ".jmsRealm");
				if (jmsRealm!=null) {
					pullingJmsListener.setJmsRealm(jmsRealm);
				} else {
					pullingJmsListener.setJmsRealm("default");
				}
				// Call setJmsRealm twice as a workaround for a strange bug
				// where we get a java.lang.NullPointerException in a class of
				// the commons-beanutils.jar on the first call to setJmsRealm
				// after starting the Test Tool ear:
				// at org.apache.commons.beanutils.MappedPropertyDescriptor.internalFindMethod(MappedPropertyDescriptor.java(Compiled Code))
				// at org.apache.commons.beanutils.MappedPropertyDescriptor.internalFindMethod(MappedPropertyDescriptor.java:413)
				// ...
				// Looks like some sort of classloader problem where
				// internalFindMethod on another class is called (last line in
				// stacktrace has "Compiled Code" while other lines have
				// linenumbers).
				// Can be reproduced with for example:
				// - WebSphere Studio Application Developer (Windows) Version: 5.1.2
				// - Ibis4Juice build 20051104-1351
				// - y01\rr\getAgent1003\scenario01.properties
				pullingJmsListener.setTimeOut(nTimeout);
				String setForceMessageIdAsCorrelationId = (String)properties.get(queueName + ".setForceMessageIdAsCorrelationId");
				if ("true".equals(setForceMessageIdAsCorrelationId)) {
					pullingJmsListener.setForceMessageIdAsCorrelationId(true);
				}
				Map<String, Object> jmsListenerInfo = new HashMap<String, Object>();
				jmsListenerInfo.put("jmsListener", pullingJmsListener);
				queues.put(queueName, jmsListenerInfo);
				debugMessage("Opened jms listener '" + queueName + "'", writers);
				if (jmsCleanUp(queueName, pullingJmsListener, writers)) {
					errorMessage("Found one or more old messages on queue '" + queueName + "', you might want to run your tests with a higher 'wait before clean up' value", writers);
				}
			}
		}

		debugMessage("Initialize jdbc fixed query senders", writers);
		iterator = jdbcFixedQuerySenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			String datasourceName = (String)properties.get(name + ".datasourceName");
			String username = (String)properties.get(name + ".username");
			String password = (String)properties.get(name + ".password");
			boolean allFound = false;
			String preDelete = ""; 
			int preDeleteIndex = 1;
			String queryType = (String)properties.get(name + ".queryType");
			String getBlobSmartString = (String)properties.get(name + ".getBlobSmart");
			boolean getBlobSmart = false;
			if (getBlobSmartString != null) {
				getBlobSmart = Boolean.valueOf(getBlobSmartString).booleanValue();
			}
			if (datasourceName == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find datasourceName property for " + name, writers);
			} else {
				Map<String, Object> querySendersInfo = new HashMap<String, Object>();
				while (!allFound && queues != null) {
					preDelete = (String)properties.get(name + ".preDel" + preDeleteIndex);
					if (preDelete != null) {
						FixedQuerySender deleteQuerySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
						deleteQuerySender.setName("Test Tool pre delete query sender");
						deleteQuerySender.setDatasourceName(appConstants.getResolvedProperty("jndiContextPrefix") + datasourceName);
						deleteQuerySender.setQueryType("delete");
						deleteQuerySender.setQuery("delete from " + preDelete);
						try {
							deleteQuerySender.configure();				 		
							deleteQuerySender.open(); 						
							deleteQuerySender.sendMessage(TESTTOOL_DUMMY_MESSAGE, null);
							deleteQuerySender.close();
						} catch(ConfigurationException e) {
							closeQueues(queues, properties, writers);
							queues = null;
							errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
						} catch(TimeOutException e) {
							closeQueues(queues, properties, writers);
							queues = null;
							errorMessage("Time out on execute pre delete query for '" + name + "': " + e.getMessage(), e, writers);
						} catch(SenderException e) {
							closeQueues(queues, properties, writers);
							queues = null;
							errorMessage("Could not execute pre delete query for '" + name + "': " + e.getMessage(), e, writers);
						}
						preDeleteIndex++;
					} else {
						allFound = true;
					}	
				}
				if (queues != null) {
					String prePostQuery = (String)properties.get(name + ".prePostQuery");
					if (prePostQuery != null) {
						FixedQuerySender prePostFixedQuerySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
						prePostFixedQuerySender.setName("Test Tool query sender");
						prePostFixedQuerySender.setDatasourceName(appConstants.getResolvedProperty("jndiContextPrefix") + datasourceName);
						//prePostFixedQuerySender.setUsername(username);
						//prePostFixedQuerySender.setPassword(password);
						prePostFixedQuerySender.setQueryType("select");
						prePostFixedQuerySender.setQuery(prePostQuery);
						try {
							prePostFixedQuerySender.configure();
						} catch(ConfigurationException e) {
							closeQueues(queues, properties, writers);
							queues = null;
							errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
						}
						if (queues != null) {
							try {
								prePostFixedQuerySender.open();
							} catch(SenderException e) {
								closeQueues(queues, properties, writers);
								queues = null;
								errorMessage("Could not open (pre/post) '" + name + "': " + e.getMessage(), e, writers);
							}
						}
						if (queues != null) {
							try {
								PipeLineSessionBase session = new PipeLineSessionBase();
								session.put(IPipeLineSession.businessCorrelationIdKey, TestTool.TESTTOOL_CORRELATIONID);
								String result = prePostFixedQuerySender.sendMessage(TESTTOOL_DUMMY_MESSAGE, session).asString();
								querySendersInfo.put("prePostQueryFixedQuerySender", prePostFixedQuerySender);
								querySendersInfo.put("prePostQueryResult", result);
							} catch(TimeOutException e) {
								closeQueues(queues, properties, writers);
								queues = null;
								errorMessage("Time out on execute query for '" + name + "': " + e.getMessage(), e, writers);
							} catch(IOException | SenderException e) {
								closeQueues(queues, properties, writers);
								queues = null;
								errorMessage("Could not execute query for '" + name + "': " + e.getMessage(), e, writers);
							}
						}
					}
				}
				if (queues != null) {
					String readQuery = (String)properties.get(name + ".readQuery");
					if (readQuery != null) {
						FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
						readQueryFixedQuerySender.setName("Test Tool query sender");
						readQueryFixedQuerySender.setDatasourceName(appConstants.getResolvedProperty("jndiContextPrefix") + datasourceName);
						//readQueryFixedQuerySender.setUsername(username);
						//readQueryFixedQuerySender.setPassword(password);
						
						if ((queryType != null) && (! queryType.equals(""))) {
							readQueryFixedQuerySender.setQueryType(queryType);	
						} else {
							readQueryFixedQuerySender.setQueryType("select");	
						}
						
						readQueryFixedQuerySender.setQuery(readQuery);
						readQueryFixedQuerySender.setBlobSmartGet(getBlobSmart);
						try {
							readQueryFixedQuerySender.configure();
						} catch(ConfigurationException e) {
							closeQueues(queues, properties, writers);
							queues = null;
							errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
						}
						if (queues != null) {
							try {
								readQueryFixedQuerySender.open();
								querySendersInfo.put("readQueryQueryFixedQuerySender", readQueryFixedQuerySender);
							} catch(SenderException e) {
								closeQueues(queues, properties, writers);
								queues = null;
								errorMessage("Could not open '" + name + "': " + e.getMessage(), e, writers);
							}
						}
					}
				}
				if (queues != null) {
					String waitBeforeRead = (String)properties.get(name + ".waitBeforeRead");
					if (waitBeforeRead != null) {
						try {
							querySendersInfo.put("readQueryWaitBeforeRead", new Integer(waitBeforeRead));
						} catch(NumberFormatException e) {
							errorMessage("Value of '" + name + ".waitBeforeRead' not a number: " + e.getMessage(), e, writers);
						}
					}
					queues.put(name, querySendersInfo);
					debugMessage("Opened jdbc connection '" + name + "'", writers);
				}
			}
		}

		debugMessage("Initialize ibis web service senders", writers);
		iterator = ibisWebServiceSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
	
			String ibisHost = (String)properties.get(name + ".ibisHost");
			String ibisInstance = (String)properties.get(name + ".ibisInstance");
			String serviceName = (String)properties.get(name + ".serviceName");
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));

			if (ibisHost == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find ibisHost property for " + name, writers);
			} else if (ibisInstance == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find ibisInstance property for " + name, writers);
			} else if (serviceName == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find serviceName property for " + name, writers);
			} else {
				IbisWebServiceSender ibisWebServiceSender = new IbisWebServiceSender();
				ibisWebServiceSender.setName("Test Tool IbisWebServiceSender");
				ibisWebServiceSender.setIbisHost(ibisHost);
				ibisWebServiceSender.setIbisInstance(ibisInstance);
				ibisWebServiceSender.setServiceName(serviceName);
				try {
					ibisWebServiceSender.configure();
				} catch(ConfigurationException e) {
					errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
					closeQueues(queues, properties, writers);
					queues = null;
				}
				try {
					ibisWebServiceSender.open();
				} catch (SenderException e) {
					closeQueues(queues, properties, writers);
					queues = null;
					errorMessage("Could not open '" + name + "': " + e.getMessage(), e, writers);
				}
				if (queues != null) {
					Map<String, Object> ibisWebServiceSenderInfo = new HashMap<String, Object>();
					ibisWebServiceSenderInfo.put("ibisWebServiceSender", ibisWebServiceSender);
					ibisWebServiceSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
					queues.put(name, ibisWebServiceSenderInfo);
					debugMessage("Opened ibis web service sender '" + name + "'", writers);
				}
			}
		}

		debugMessage("Initialize web service senders", writers);
		iterator = webServiceSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
			String url = (String)properties.get(name + ".url");
			String userName = (String)properties.get(name + ".userName");
			String password = (String)properties.get(name + ".password");
			String soap = (String)properties.get(name + ".soap");
			String allowSelfSignedCertificates = (String)properties.get(name + ".allowSelfSignedCertificates");
			if (url == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find url property for " + name, writers);
			} else {
				WebServiceSender webServiceSender = new WebServiceSender();
				webServiceSender.setName("Test Tool WebServiceSender");
				webServiceSender.setUrl(url);
				webServiceSender.setUserName(userName);
				webServiceSender.setPassword(password);
				if (soap != null) {
					webServiceSender.setSoap(new Boolean(soap));
				}
				if (allowSelfSignedCertificates != null) {
					webServiceSender.setAllowSelfSignedCertificates(new Boolean(allowSelfSignedCertificates));
				}
				String serviceNamespaceURI = (String)properties.get(name + ".serviceNamespaceURI");
				if (serviceNamespaceURI != null) {
					webServiceSender.setServiceNamespaceURI(serviceNamespaceURI);
				}
				String serviceNamespace = (String)properties.get(name + ".serviceNamespace");
				if (serviceNamespace != null) {
					webServiceSender.setServiceNamespace(serviceNamespace);
				}
				try {
					webServiceSender.configure();
				} catch(ConfigurationException e) {
					errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
					closeQueues(queues, properties, writers);
					queues = null;
				}
				if (queues != null) {
					try {
						webServiceSender.open();
					} catch (SenderException e) {
						closeQueues(queues, properties, writers);
						queues = null;
						errorMessage("Could not open '" + name + "': " + e.getMessage(), e, writers);
					}
					if (queues != null) {
						Map<String, Object> webServiceSenderInfo = new HashMap<String, Object>();
						webServiceSenderInfo.put("webServiceSender", webServiceSender);
						webServiceSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
						queues.put(name, webServiceSenderInfo);
						debugMessage("Opened web service sender '" + name + "'", writers);
					}
				}
			}
		}

		debugMessage("Initialize web service listeners", writers);
		iterator = webServiceListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			String serviceNamespaceURI = (String)properties.get(name + ".serviceNamespaceURI");

			if (serviceNamespaceURI == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find property '" + name + ".serviceNamespaceURI'", writers);
			} else {
				ListenerMessageHandler listenerMessageHandler = new ListenerMessageHandler();
				listenerMessageHandler.setRequestTimeOut(globalTimeout);
				listenerMessageHandler.setResponseTimeOut(globalTimeout);
				try {
					long requestTimeOut = Long.parseLong((String)properties.get(name + ".requestTimeOut"));
					listenerMessageHandler.setRequestTimeOut(requestTimeOut);
					debugMessage("Request time out set to '" + requestTimeOut + "'", writers);
				} catch(Exception e) {
				}
				try {
					long responseTimeOut = Long.parseLong((String)properties.get(name + ".responseTimeOut"));
					listenerMessageHandler.setResponseTimeOut(responseTimeOut);
					debugMessage("Response time out set to '" + responseTimeOut + "'", writers);
				} catch(Exception e) {
				}
				WebServiceListener webServiceListener = new WebServiceListener();
				webServiceListener.setName("Test Tool WebServiceListener");
				webServiceListener.setServiceNamespaceURI(serviceNamespaceURI);
				webServiceListener.setHandler(listenerMessageHandler);
				try {
					webServiceListener.open();
				} catch (ListenerException e) {
					closeQueues(queues, properties, writers);
					queues = null;
					errorMessage("Could not open web service listener '" + name + "': " + e.getMessage(), e, writers);
				}
				Map<String, Object> webServiceListenerInfo = new HashMap<String, Object>();
				webServiceListenerInfo.put("webServiceListener", webServiceListener);
				webServiceListenerInfo.put("listenerMessageHandler", listenerMessageHandler);
				queues.put(name, webServiceListenerInfo);
				ServiceDispatcher serviceDispatcher = ServiceDispatcher.getInstance();
				try {
					serviceDispatcher.registerServiceClient(serviceNamespaceURI, webServiceListener);
					debugMessage("Opened web service listener '" + name + "'", writers);
				} catch(ListenerException e) {
					closeQueues(queues, properties, writers);
					queues = null;
					errorMessage("Could not open web service listener '" + name + "': " + e.getMessage(), e, writers);
				}
			}
		}

		debugMessage("Initialize http senders", writers);
		iterator = httpSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
			String url = (String)properties.get(name + ".url");
			String userName = (String)properties.get(name + ".userName");
			String password = (String)properties.get(name + ".password");
			String headerParams = (String)properties.get(name + ".headersParams");
			String xhtmlString = (String)properties.get(name + ".xhtml");
			String methodtype = (String)properties.get(name + ".methodType");
			String paramsInUrlString = (String)properties.get(name + ".paramsInUrl");
			String inputMessageParam = (String)properties.get(name + ".inputMessageParam");
			String multipartString = (String)properties.get(name + ".multipart");
 			String styleSheetName = (String)properties.get(name + ".styleSheetName");
			if (url == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find url property for " + name, writers);
			} else {
				HttpSender httpSender = null;
				IPipeLineSession session = null;
				ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
				try {
					// Use directoryClassLoader to make it possible to specify
					// styleSheetName relative to the scenarioDirectory.
					//TODO create larva classloader without basepath
					DirectoryClassLoader directoryClassLoader = new DirectoryClassLoader(originalClassLoader);
					directoryClassLoader.setDirectory(scenarioDirectory);
					directoryClassLoader.setBasePath(".");
					directoryClassLoader.configure(ibisContext, "dummy");
					Thread.currentThread().setContextClassLoader(directoryClassLoader);
					httpSender = new HttpSender();
					httpSender.setName("Test Tool HttpSender");
					httpSender.setUrl(url);
					httpSender.setUserName(userName);
					httpSender.setPassword(password);
					httpSender.setHeadersParams(headerParams);
					if (StringUtils.isNotEmpty(xhtmlString)) {
						httpSender.setXhtml(Boolean.valueOf(xhtmlString).booleanValue());
					}
					if (StringUtils.isNotEmpty(methodtype)) {
						httpSender.setMethodType(methodtype);
					}
					if (StringUtils.isNotEmpty(paramsInUrlString)) {
						httpSender.setParamsInUrl(Boolean.valueOf(paramsInUrlString).booleanValue());
					}
					if (StringUtils.isNotEmpty(inputMessageParam)) {
						httpSender.setInputMessageParam(inputMessageParam);
					}
					if (StringUtils.isNotEmpty(multipartString)) {
						httpSender.setMultipart(Boolean.valueOf(multipartString).booleanValue());
					}
					if (StringUtils.isNotEmpty(styleSheetName)) {
						httpSender.setStyleSheetName(styleSheetName);
					}
					session = new PipeLineSessionBase();
					Map<String, Object> paramPropertiesMap = createParametersMapFromParamProperties(properties, name, writers, true, session);
					Iterator<String> parameterNameIterator = paramPropertiesMap.keySet().iterator();
					while (parameterNameIterator.hasNext()) {
						String parameterName = (String)parameterNameIterator.next();
						Parameter parameter = (Parameter)paramPropertiesMap.get(parameterName);
						httpSender.addParameter(parameter);
					}
					httpSender.configure();
				} catch(ConfigurationException e) {
					errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
					closeQueues(queues, properties, writers);
					queues = null;
				} finally {
					if (originalClassLoader != null) {
						Thread.currentThread().setContextClassLoader(originalClassLoader);
					}
				}
				if (queues != null) {
					try {
						httpSender.open();
					} catch (SenderException e) {
						closeQueues(queues, properties, writers);
						queues = null;
						errorMessage("Could not open '" + name + "': " + e.getMessage(), e, writers);
					}
					if (queues != null) {
						Map<String, Object> httpSenderInfo = new HashMap<String, Object>();
						httpSenderInfo.put("httpSender", httpSender);
						httpSenderInfo.put("session", session);
						httpSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
						queues.put(name, httpSenderInfo);
						debugMessage("Opened http sender '" + name + "'", writers);
					}
				}
			}
		}

		debugMessage("Initialize ibis java senders", writers);
		iterator = ibisJavaSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			String serviceName = (String)properties.get(name + ".serviceName");
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
			if (serviceName == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find serviceName property for " + name, writers);
			} else {
				IbisJavaSender ibisJavaSender = new IbisJavaSender();
				ibisJavaSender.setName("Test Tool IbisJavaSender");
				ibisJavaSender.setServiceName(serviceName);
				IPipeLineSession session = new PipeLineSessionBase();
				Map<String, Object> paramPropertiesMap = createParametersMapFromParamProperties(properties, name, writers, true, session);
				Iterator<String> parameterNameIterator = paramPropertiesMap.keySet().iterator();
				while (parameterNameIterator.hasNext()) {
					String parameterName = (String)parameterNameIterator.next();
					Parameter parameter = (Parameter)paramPropertiesMap.get(parameterName);
					ibisJavaSender.addParameter(parameter);
				}
				try {
					ibisJavaSender.configure();
				} catch(ConfigurationException e) {
					errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
					closeQueues(queues, properties, writers);
					queues = null;
				}
				if (queues != null) {
					try {
						ibisJavaSender.open();
					} catch (SenderException e) {
						closeQueues(queues, properties, writers);
						queues = null;
						errorMessage("Could not open '" + name + "': " + e.getMessage(), e, writers);
					}
					if (queues != null) {
						Map<String, Object> ibisJavaSenderInfo = new HashMap<String, Object>();
						ibisJavaSenderInfo.put("ibisJavaSender", ibisJavaSender);
						ibisJavaSenderInfo.put("session", session);
						ibisJavaSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
						queues.put(name, ibisJavaSenderInfo);
						debugMessage("Opened ibis java sender '" + name + "'", writers);
					}
				}
			}
		}

		debugMessage("Initialize delay senders", writers);
		iterator = delaySenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
			String delayTime = (String)properties.get(name + ".delayTime");
			DelaySender delaySender = new DelaySender();
			if (delayTime!=null) {
				delaySender.setDelayTime(Long.parseLong(delayTime));
			}
			delaySender.setName("Test Tool DelaySender");
			Map<String, Object> delaySenderInfo = new HashMap<String, Object>();
			delaySenderInfo.put("delaySender", delaySender);
			delaySenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
			queues.put(name, delaySenderInfo);
			debugMessage("Opened delay sender '" + name + "'", writers);
		}
		
		debugMessage("Initialize java listeners", writers);
		iterator = javaListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			String serviceName = (String)properties.get(name + ".serviceName");
			if (serviceName == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find property '" + name + ".serviceName'", writers);
			} else {
				ListenerMessageHandler listenerMessageHandler = new ListenerMessageHandler();
				try {
					long requestTimeOut = Long.parseLong((String)properties.get(name + ".requestTimeOut"));
					listenerMessageHandler.setRequestTimeOut(requestTimeOut);
					debugMessage("Request time out set to '" + requestTimeOut + "'", writers);
				} catch(Exception e) {
				}
				try {
					long responseTimeOut = Long.parseLong((String)properties.get(name + ".responseTimeOut"));
					listenerMessageHandler.setResponseTimeOut(responseTimeOut);
					debugMessage("Response time out set to '" + responseTimeOut + "'", writers);
				} catch(Exception e) {
				}
				JavaListener javaListener = new JavaListener();
				javaListener.setName("Test Tool JavaListener");
				javaListener.setServiceName(serviceName);
				javaListener.setHandler(listenerMessageHandler);
				try {
					javaListener.open();
					Map<String, Object> javaListenerInfo = new HashMap<String, Object>();
					javaListenerInfo.put("javaListener", javaListener);
					javaListenerInfo.put("listenerMessageHandler", listenerMessageHandler);
					queues.put(name, javaListenerInfo);
					debugMessage("Opened java listener '" + name + "'", writers);
				} catch(ListenerException e) {
					closeQueues(queues, properties, writers);
					queues = null;
					errorMessage("Could not open java listener '" + name + "': " + e.getMessage(), e, writers);
				}
			}
		}

		debugMessage("Initialize file senders", writers);
		iterator = fileSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String filename  = (String)properties.get(queueName + ".filename");
			if (filename == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find filename property for " + queueName, writers);
			} else {
				FileSender fileSender = new FileSender();
				String filenameAbsolutePath = (String)properties.get(queueName + ".filename.absolutepath");
				fileSender.setFilename(filenameAbsolutePath);
				String encoding = (String)properties.get(queueName + ".encoding");
				if (encoding != null) {
					fileSender.setEncoding(encoding);
					debugMessage("Encoding set to '" + encoding + "'", writers);
				}
				String deletePathString = (String)properties.get(queueName + ".deletePath");
				if (deletePathString != null) {
					boolean deletePath = Boolean.valueOf(deletePathString).booleanValue();
					fileSender.setDeletePath(deletePath);
					debugMessage("Delete path set to '" + deletePath + "'", writers);
				}
				String createPathString = (String)properties.get(queueName + ".createPath");
				if (createPathString != null) {
					boolean createPath = Boolean.valueOf(createPathString).booleanValue();
					fileSender.setCreatePath(createPath);
					debugMessage("Create path set to '" + createPath + "'", writers);
				}
				try {
					String checkDeleteString = (String)properties.get(queueName + ".checkDelete");
					if (checkDeleteString != null) {
						boolean checkDelete = Boolean.valueOf(checkDeleteString).booleanValue();
						fileSender.setCheckDelete(checkDelete);
						debugMessage("Check delete set to '" + checkDelete + "'", writers);
					}
				} catch(Exception e) {
				}
				try {
					String runAntString = (String)properties.get(queueName + ".runAnt");
					if (runAntString != null) {
						boolean runAnt = Boolean.valueOf(runAntString).booleanValue();
						fileSender.setRunAnt(runAnt);
						debugMessage("Run ant set to '" + runAnt + "'", writers);
					}
				} catch(Exception e) {
				}
				try {
					long timeOut = Long.parseLong((String)properties.get(queueName + ".timeOut"));
					fileSender.setTimeOut(timeOut);
					debugMessage("Time out set to '" + timeOut + "'", writers);
				} catch(Exception e) {
				}
				try {
					long interval  = Long.parseLong((String)properties.get(queueName + ".interval"));
					fileSender.setInterval(interval);
					debugMessage("Interval set to '" + interval + "'", writers);
				} catch(Exception e) {
				}
				try {
					String overwriteString = (String)properties.get(queueName + ".overwrite");
					if (overwriteString != null) {
						debugMessage("OverwriteString = " + overwriteString, writers);
						boolean overwrite = Boolean.valueOf(overwriteString).booleanValue();
						fileSender.setOverwrite(overwrite);
						debugMessage("Overwrite set to '" + overwrite + "'", writers);
					}
				} catch(Exception e) {
				}
				Map<String, Object> fileSenderInfo = new HashMap<String, Object>();
				fileSenderInfo.put("fileSender", fileSender);
				queues.put(queueName, fileSenderInfo);
				debugMessage("Opened file sender '" + queueName + "'", writers);
			}
		}

		debugMessage("Initialize file listeners", writers);
		iterator = fileListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String filename  = (String)properties.get(queueName + ".filename");
			String filename2  = (String)properties.get(queueName + ".filename2");
			String directory = null;
			String wildcard = null;
			if (filename == null) {
				directory = (String)properties.get(queueName + ".directory");
				wildcard = (String)properties.get(queueName + ".wildcard");
			}
			if (filename == null && directory == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find filename or directory property for " + queueName, writers);
			} else if (directory != null && wildcard == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find wildcard property for " + queueName, writers);
			} else {
				FileListener fileListener = new FileListener();
				if (filename == null) {
					String directoryAbsolutePath = (String)properties.get(queueName + ".directory.absolutepath");;
					fileListener.setDirectory(directoryAbsolutePath);
					fileListener.setWildcard(wildcard);
				} else {
					String filenameAbsolutePath = (String)properties.get(queueName + ".filename.absolutepath");;
					fileListener.setFilename(filenameAbsolutePath);
				}
				try {
					long waitBeforeRead = Long.parseLong((String)properties.get(queueName + ".waitBeforeRead"));
					fileListener.setWaitBeforeRead(waitBeforeRead);
					debugMessage("Wait before read set to '" + waitBeforeRead + "'", writers);
				} catch(Exception e) {
				}
				try {
					long timeOut = Long.parseLong((String)properties.get(queueName + ".timeOut"));
					fileListener.setTimeOut(timeOut);
					debugMessage("Time out set to '" + timeOut + "'", writers);
				} catch(Exception e) {
				}
				try {
					long interval  = Long.parseLong((String)properties.get(queueName + ".interval"));
					fileListener.setInterval(interval);
					debugMessage("Interval set to '" + interval + "'", writers);
				} catch(Exception e) {
				}
				if (filename2!=null) {
					fileListener.setFilename2(filename2);
				}
				Map<String, Object> fileListenerInfo = new HashMap<String, Object>();
				fileListenerInfo.put("fileListener", fileListener);
				queues.put(queueName, fileListenerInfo);
				debugMessage("Opened file listener '" + queueName + "'", writers);
				if (fileListenerCleanUp(queueName, fileListener, writers)) {
					errorMessage("Found old messages on '" + queueName + "'", writers);
				}
			}
		}

		debugMessage("Initialize xslt provider listeners", writers);
		iterator = xsltProviderListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String filename  = (String)properties.get(queueName + ".filename");
			if (filename == null) {
				closeQueues(queues, properties, writers);
				queues = null;
				errorMessage("Could not find filename property for " + queueName, writers);
			} else {
				Boolean fromClasspath = new Boolean((String)properties.get(queueName + ".fromClasspath"));
				if (!fromClasspath) {
					filename = (String)properties.get(queueName + ".filename.absolutepath");
				}
				XsltProviderListener xsltProviderListener = new XsltProviderListener();
				xsltProviderListener.setFromClasspath(fromClasspath);
				xsltProviderListener.setFilename(filename);
				String xsltVersionString = (String)properties.get(queueName + ".xsltVersion");
				if (xsltVersionString != null) {
					try {
						int xsltVersion = Integer.valueOf(xsltVersionString).intValue();
						xsltProviderListener.setXsltVersion(xsltVersion);
						debugMessage("XsltVersion set to '" + xsltVersion + "'", writers);
					} catch(Exception e) {
					}
				}
				String xslt2String = (String)properties.get(queueName + ".xslt2");
				if (xslt2String != null) {
					try {
						boolean xslt2 = Boolean.valueOf(xslt2String).booleanValue();
						xsltProviderListener.setXslt2(xslt2);
						debugMessage("Xslt2 set to '" + xslt2 + "'", writers);
					} catch(Exception e) {
					}
				}
				String namespaceAwareString = (String)properties.get(queueName + ".namespaceAware");
				if (namespaceAwareString != null) {
					try {
						boolean namespaceAware = Boolean.valueOf(namespaceAwareString).booleanValue();
						xsltProviderListener.setNamespaceAware(namespaceAware);
						debugMessage("Namespace aware set to '" + namespaceAware + "'", writers);
					} catch(Exception e) {
					}
				}
				try {
					xsltProviderListener.init();
					Map<String, Object> xsltProviderListenerInfo = new HashMap<String, Object>();
					xsltProviderListenerInfo.put("xsltProviderListener", xsltProviderListener);
					queues.put(queueName, xsltProviderListenerInfo);
					debugMessage("Opened xslt provider listener '" + queueName + "'", writers);
				} catch(ListenerException e) {
					closeQueues(queues, properties, writers);
					queues = null;
					errorMessage("Could not create xslt provider listener for '" + queueName + "': " + e.getMessage(), e, writers);
				}
			}
		}

		return queues;
	}



	public static boolean closeQueues(Map<String, Map<String, Object>> queues, Properties properties, Map<String, Object> writers) {
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
						PipeLineSessionBase session = new PipeLineSessionBase();
						session.put(IPipeLineSession.businessCorrelationIdKey, TestTool.TESTTOOL_CORRELATIONID);
						String postResult = prePostFixedQuerySender.sendMessage(TESTTOOL_DUMMY_MESSAGE, session).asString();
						if (!preResult.equals(postResult)) {
							
							String message = null;
							FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
							try {
								message = readQueryFixedQuerySender.sendMessage(TESTTOOL_DUMMY_MESSAGE, session).asString();
							} catch(TimeOutException e) {
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
					} catch(TimeOutException e) {
						errorMessage("Time out on close (pre/post) '" + name + "': " + e.getMessage(), e, writers);
					} catch(IOException | SenderException e) {
						errorMessage("Could not close (pre/post) '" + name + "': " + e.getMessage(), e, writers);
					}
				}
				FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
				readQueryFixedQuerySender.close();
			}
		}
		debugMessage("Close ibis webservice senders", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(properties.get(queueName + ".className"))) {
				IbisWebServiceSender ibisWebServiceSender = (IbisWebServiceSender)((Map<?, ?>)queues.get(queueName)).get("ibisWebServiceSender");
				Map<?, ?> ibisWebServiceSenderInfo = (Map<?, ?>)queues.get(queueName);
				SenderThread senderThread = (SenderThread)ibisWebServiceSenderInfo.remove("ibisWebServiceSenderThread");
				if (senderThread != null) {
					debugMessage("Found remaining SenderThread", writers);
					SenderException senderException = senderThread.getSenderException();
					if (senderException != null) {
						errorMessage("Found remaining SenderException: " + senderException.getMessage(), senderException, writers);
					}
					IOException ioException = senderThread.getIOException();
					if (ioException != null) {
						errorMessage("Found remaining IOException: " + ioException.getMessage(), ioException, writers);
					}
					TimeOutException timeOutException = senderThread.getTimeOutException();
					if (timeOutException != null) {
						errorMessage("Found remaining TimeOutException: " + timeOutException.getMessage(), timeOutException, writers);
					}
					String message = senderThread.getResponse();
					if (message != null) {
						wrongPipelineMessage("Found remaining message on '" + queueName + "'", message, writers);
					}
				}

				try {
					ibisWebServiceSender.close();
					debugMessage("Closed ibis webservice sender '" + queueName + "'", writers);
				} catch(SenderException e) {
					errorMessage("Could not close '" + queueName + "': " + e.getMessage(), e, writers);
				}
			}
		}
		
		debugMessage("Close web service senders", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className"))) {
				WebServiceSender webServiceSender = (WebServiceSender)((Map<?, ?>)queues.get(queueName)).get("webServiceSender");
				Map<?, ?> webServiceSenderInfo = (Map<?, ?>)queues.get(queueName);
				SenderThread senderThread = (SenderThread)webServiceSenderInfo.remove("webServiceSenderThread");
				if (senderThread != null) {
					debugMessage("Found remaining SenderThread", writers);
					SenderException senderException = senderThread.getSenderException();
					if (senderException != null) {
						errorMessage("Found remaining SenderException: " + senderException.getMessage(), senderException, writers);
					}
					TimeOutException timeOutException = senderThread.getTimeOutException();
					if (timeOutException != null) {
						errorMessage("Found remaining TimeOutException: " + timeOutException.getMessage(), timeOutException, writers);
					}
					String message = senderThread.getResponse();
					if (message != null) {
						wrongPipelineMessage("Found remaining message on '" + queueName + "'", message, writers);
					}
				}
				try {
					webServiceSender.close();
				} catch (SenderException e) {
					//Ignore
				}
				debugMessage("Closed webservice sender '" + queueName + "'", writers);
			}
		}

		debugMessage("Close web service listeners", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))) {
				Map<?, ?> webServiceListenerInfo = (Map<?, ?>)queues.get(queueName);
				WebServiceListener webServiceListener = (WebServiceListener)webServiceListenerInfo.get("webServiceListener");
				webServiceListener.close();
				debugMessage("Closed web service listener '" + queueName + "'", writers);
				ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)webServiceListenerInfo.get("listenerMessageHandler");
				if (listenerMessageHandler != null) {
					ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage(0);
					while (listenerMessage != null) {
						String message = listenerMessage.getMessage();
						wrongPipelineMessage("Found remaining request message on '" + queueName + "'", message, writers);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getRequestMessage(0);
					}
					listenerMessage = listenerMessageHandler.getResponseMessage(0);
					while (listenerMessage != null) {
						String message = listenerMessage.getMessage();
						wrongPipelineMessage("Found remaining response message on '" + queueName + "'", message, writers);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getResponseMessage(0);
					}
				}
			}
		}

		debugMessage("Close ibis java senders", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.senders.IbisJavaSender".equals(properties.get(queueName + ".className"))) {
				IbisJavaSender ibisJavaSender = (IbisJavaSender)((Map<?, ?>)queues.get(queueName)).get("ibisJavaSender");

				Map<?, ?> ibisJavaSenderInfo = (Map<?, ?>)queues.get(queueName);
				SenderThread ibisJavaSenderThread = (SenderThread)ibisJavaSenderInfo.remove("ibisJavaSenderThread");
				if (ibisJavaSenderThread != null) {
					debugMessage("Found remaining SenderThread", writers);
					SenderException senderException = ibisJavaSenderThread.getSenderException();
					if (senderException != null) {
						errorMessage("Found remaining SenderException: " + senderException.getMessage(), senderException, writers);
					}
					TimeOutException timeOutException = ibisJavaSenderThread.getTimeOutException();
					if (timeOutException != null) {
						errorMessage("Found remaining TimeOutException: " + timeOutException.getMessage(), timeOutException, writers);
					}
					String message = ibisJavaSenderThread.getResponse();
					if (message != null) {
						wrongPipelineMessage("Found remaining message on '" + queueName + "'", message, writers);
					}
				}

				try {
				ibisJavaSender.close();
					debugMessage("Closed ibis java sender '" + queueName + "'", writers);
				} catch(SenderException e) {
					errorMessage("Could not close '" + queueName + "': " + e.getMessage(), e, writers);
				}
			}
		}

		debugMessage("Close delay senders", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.senders.DelaySender".equals(properties.get(queueName + ".className"))) {
				DelaySender delaySender = (DelaySender)((Map<?, ?>)queues.get(queueName)).get("delaySender");
				try {
					delaySender.close();
					debugMessage("Closed delay sender '" + queueName + "'", writers);
				} catch(SenderException e) {
					errorMessage("Could not close delay sender '" + queueName + "': " + e.getMessage(), e, writers);
				}
			}
		}
		
		debugMessage("Close java listeners", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className"))) {
				Map<?, ?> javaListenerInfo = (Map<?, ?>)queues.get(queueName);
				JavaListener javaListener = (JavaListener)javaListenerInfo.get("javaListener");
				try {
					javaListener.close();
					debugMessage("Closed java listener '" + queueName + "'", writers);
				} catch(ListenerException e) {
					errorMessage("Could not close java listener '" + queueName + "': " + e.getMessage(), e, writers);
				}
				ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)javaListenerInfo.get("listenerMessageHandler");
				if (listenerMessageHandler != null) {
					ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage(0);
					while (listenerMessage != null) {
						String message = listenerMessage.getMessage();
						wrongPipelineMessage("Found remaining request message on '" + queueName + "'", message, writers);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getRequestMessage(0);
					}
					listenerMessage = listenerMessageHandler.getResponseMessage(0);
					while (listenerMessage != null) {
						String message = listenerMessage.getMessage();
						wrongPipelineMessage("Found remaining response message on '" + queueName + "'", message, writers);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getResponseMessage(0);
					}
				}
			}
		}

		debugMessage("Close file listeners", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.testtool.FileListener".equals(properties.get(queueName + ".className"))) {
				FileListener fileListener = (FileListener)((Map<?, ?>)queues.get(queueName)).get("fileListener");
				fileListenerCleanUp(queueName, fileListener, writers);
				debugMessage("Closed file listener '" + queueName + "'", writers);
			}
		}

		debugMessage("Close xslt provider listeners", writers);
		iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))) {
				XsltProviderListener xsltProviderListener = (XsltProviderListener)((Map<?, ?>)queues.get(queueName)).get("xsltProviderListener");
				xsltProviderListenerCleanUp(queues, queueName, writers);
				debugMessage("Closed xslt provider listener '" + queueName + "'", writers);
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

	public static boolean fileListenerCleanUp(String queueName, FileListener fileListener, Map<String, Object> writers) {
		boolean remainingMessagesFound = false;
		debugMessage("Check for remaining messages on '" + queueName + "'", writers);
		if (fileListener.getFilename2()!=null) {
			return false;
		}
		long oldTimeOut = fileListener.getTimeOut();
		fileListener.setTimeOut(0);
		boolean empty = false;
		fileListener.setTimeOut(0);
		try {
			String message = fileListener.getMessage();
			if (message != null) {
				remainingMessagesFound = true;
				wrongPipelineMessage("Found remaining message on '" + queueName + "'", message, writers);
			}
		} catch(TimeOutException e) {
		} catch(ListenerException e) {
			errorMessage("Could read message from file listener '" + queueName + "': " + e.getMessage(), e, writers);
		}
		fileListener.setTimeOut(oldTimeOut);
		return remainingMessagesFound;
	}

	public static boolean xsltProviderListenerCleanUp(Map<String, Map<String, Object>> queues, String queueName, Map<String, Object> writers) {
		boolean remainingMessagesFound = false;
		Map<?, ?> xsltProviderListenerInfo = (Map<?, ?>)queues.get(queueName);
		XsltProviderListener xsltProviderListener = (XsltProviderListener)xsltProviderListenerInfo.get("xsltProviderListener");
		String message = xsltProviderListener.getResult();
		if (message != null) {
			remainingMessagesFound = true;
			wrongPipelineMessage("Found remaining message on '" + queueName + "'", message, writers);
		}
		return remainingMessagesFound;
	}
	
	private static int executeJmsSenderWrite(String stepDisplayName, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileContent) {
		int result = RESULT_ERROR;
		
		Map<?, ?> jmsSenderInfo = (Map<?, ?>)queues.get(queueName);
		JmsSender jmsSender = (JmsSender)jmsSenderInfo.get("jmsSender");
		try {
			String correlationId = null;
			String useCorrelationIdFrom = (String)jmsSenderInfo.get("useCorrelationIdFrom");
			if (useCorrelationIdFrom != null) {
				Map<?, ?> listenerInfo = (Map<?, ?>)queues.get(useCorrelationIdFrom);
				if (listenerInfo == null) {
					errorMessage("Could not find listener '" + useCorrelationIdFrom + "' to use correlation id from", writers);
				} else {
					correlationId = (String)listenerInfo.get("correlationId");
					if (correlationId == null) {
						errorMessage("Could not find correlation id from listener '" + useCorrelationIdFrom + "'", writers);
					}
				}
			}
			if (correlationId == null) {
				correlationId = (String)jmsSenderInfo.get("jmsCorrelationId");
			}
			if (correlationId == null) {
				correlationId = TESTTOOL_CORRELATIONID;
			}
			jmsSender.sendMessage(new nl.nn.adapterframework.stream.Message(fileContent), null);
			debugPipelineMessage(stepDisplayName, "Successfully written to '" + queueName + "':", fileContent, writers);
			result = RESULT_OK;
		} catch(TimeOutException e) {
			errorMessage("Time out sending jms message to '" + queueName + "': " + e.getMessage(), e, writers);
		} catch(SenderException e) {
			errorMessage("Could not send jms message to '" + queueName + "': " + e.getMessage(), e, writers);
		}
		
		return result;
	}

	private static int executeSenderWrite(String stepDisplayName, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String senderType, String fileContent) {
		int result = RESULT_ERROR;
		Map senderInfo = (Map)queues.get(queueName);
		ISender sender = (ISender)senderInfo.get(senderType + "Sender");
		Boolean convertExceptionToMessage = (Boolean)senderInfo.get("convertExceptionToMessage");
		IPipeLineSession session = (IPipeLineSession)senderInfo.get("session");
		SenderThread senderThread = new SenderThread(sender, fileContent, session, convertExceptionToMessage.booleanValue());
		senderThread.start();
		senderInfo.put(senderType + "SenderThread", senderThread);
		debugPipelineMessage(stepDisplayName, "Successfully started thread writing to '" + queueName + "':", fileContent, writers);
		logger.debug("Successfully started thread writing to '" + queueName + "'");
		result = RESULT_OK;
		return result;
	}
	
	private static int executeJavaOrWebServiceListenerWrite(String stepDisplayName, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileContent) {
		int result = RESULT_ERROR;

		Map<?, ?> listenerInfo = (Map<?, ?>)queues.get(queueName);
		ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)listenerInfo.get("listenerMessageHandler");
		if (listenerMessageHandler == null) {
			errorMessage("No ListenerMessageHandler found", writers);
		} else {
			String correlationId = null;
			Map<?, ?> context = new HashMap<Object, Object>();
			ListenerMessage requestListenerMessage = (ListenerMessage)listenerInfo.get("listenerMessage");
			if (requestListenerMessage != null) {
				correlationId = requestListenerMessage.getCorrelationId();
				context = requestListenerMessage.getContext();
			}
			ListenerMessage listenerMessage = new ListenerMessage(correlationId, fileContent, context);
			listenerMessageHandler.putResponseMessage(listenerMessage);
			debugPipelineMessage(stepDisplayName, "Successfully put message on '" + queueName + "':", fileContent, writers);
			logger.debug("Successfully put message on '" + queueName + "'");
			result = RESULT_OK;
		}

		return result;
	}
	
	private static int executeFileSenderWrite(String stepDisplayName, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileContent) {
		int result = RESULT_ERROR;
		Map<?, ?> fileSenderInfo = (Map<?, ?>)queues.get(queueName);
		FileSender fileSender = (FileSender)fileSenderInfo.get("fileSender");
		try {
			fileSender.sendMessage(fileContent);
			debugPipelineMessage(stepDisplayName, "Successfully written to '" + queueName + "':", fileContent, writers);
			result = RESULT_OK;
		} catch(Exception e) {
			errorMessage("Exception writing to file: " + e.getMessage(), e, writers);
		}
		return result;
	}

	private static int executeDelaySenderWrite(String stepDisplayName, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileContent) {
		int result = RESULT_ERROR;
		Map<?, ?> delaySenderInfo = (Map<?, ?>)queues.get(queueName);
		DelaySender delaySender = (DelaySender)delaySenderInfo.get("delaySender");
		try {
			delaySender.sendMessage(new nl.nn.adapterframework.stream.Message(fileContent), null);
			debugPipelineMessage(stepDisplayName, "Successfully written to '" + queueName + "':", fileContent, writers);
			result = RESULT_OK;
		} catch(Exception e) {
			errorMessage("Exception writing to file: " + e.getMessage(), e, writers);
		}
		return result;
	}

	private static int executeXsltProviderListenerWrite(String step, String stepDisplayName, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileName, String fileContent, Properties properties) {
		int result = RESULT_ERROR;
		Map<?, ?> xsltProviderListenerInfo = (Map<?, ?>)queues.get(queueName);
		XsltProviderListener xsltProviderListener = (XsltProviderListener)xsltProviderListenerInfo.get("xsltProviderListener");
		String message = xsltProviderListener.getResult();
		if (message == null) {
			if ("".equals(fileName)) {
				result = RESULT_OK;
			} else {
				errorMessage("Could not read result (null returned)", writers);
			}
		} else {
			result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties, writers, queueName);
		}
		return result;
	}
	
	private static int executeJmsListenerRead(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileName, String fileContent) {
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

	private static int executeSenderRead(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String senderType, String fileName, String fileContent) {
		int result = RESULT_ERROR;
	
		Map<?, ?> senderInfo = (Map<?, ?>)queues.get(queueName);
		SenderThread senderThread = (SenderThread)senderInfo.remove(senderType + "SenderThread");
		if (senderThread == null) {
			errorMessage("No SenderThread found, no " + senderType + "Sender.write request?", writers);
		} else {
			SenderException senderException = senderThread.getSenderException();
			if (senderException == null) {
				IOException ioException = senderThread.getIOException();
				if (ioException == null) {
					TimeOutException timeOutException = senderThread.getTimeOutException();
					if (timeOutException == null) {
						String message = senderThread.getResponse();
						if (message == null) {
							if ("".equals(fileName)) {
								result = RESULT_OK;
							} else {
								errorMessage("Could not read " + senderType + "Sender message (null returned)", writers);
							}
						} else {
							if ("".equals(fileName)) {
								debugPipelineMessage(stepDisplayName, "Unexpected message read from '" + queueName + "':", message, writers);
							} else {
								result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties, writers, queueName);
							}
						}
					} else {
						errorMessage("Could not read " + senderType + "Sender message (TimeOutException): " + timeOutException.getMessage(), timeOutException, writers);
					}
				} else {
					errorMessage("Could not read " + senderType + "Sender message (IOException): " + ioException.getMessage(), ioException, writers);
				}
			} else {
				errorMessage("Could not read " + senderType + "Sender message (SenderException): " + senderException.getMessage(), senderException, writers);
			}
		}
		
		return result;
	}

	private static int executeJavaListenerOrWebServiceListenerRead(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileName, String fileContent) {
		int result = RESULT_ERROR;

		Map listenerInfo = (Map)queues.get(queueName);
		ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)listenerInfo.get("listenerMessageHandler");
		if (listenerMessageHandler == null) {
			errorMessage("No ListenerMessageHandler found", writers);
		} else {
			String message = null;
			ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage();
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
						String correlationId = null;
						Map<?, ?> context = new HashMap<Object, Object>();
						listenerMessage = new ListenerMessage(correlationId, TESTTOOL_CLEAN_UP_REPLY, context);
						listenerMessageHandler.putResponseMessage(listenerMessage);
					}
				}
			}
		}
		
		return result;
	}

	private static int executeFixedQuerySenderRead(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileName, String fileContent) {
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
				PipeLineSessionBase session = new PipeLineSessionBase();
				session.put(IPipeLineSession.businessCorrelationIdKey, TestTool.TESTTOOL_CORRELATIONID);
				String postResult = prePostFixedQuerySender.sendMessage(TESTTOOL_DUMMY_MESSAGE, session).asString();
				debugPipelineMessage(stepDisplayName, "Post result '" + queueName + "':", postResult, writers);
				if (preResult.equals(postResult)) {
					newRecordFound = false;
				}
				/* Fill the preResult with postResult, so closeQueues is able to determine if there
				 * are remaining messages left.
				 */
				querySendersInfo.put("prePostQueryResult", postResult);
			} catch(TimeOutException e) {
				errorMessage("Time out on execute query for '" + queueName + "': " + e.getMessage(), e, writers);
			} catch(IOException | SenderException e) {
				errorMessage("Could not execute query for '" + queueName + "': " + e.getMessage(), e, writers);
			}
		}
		String message = null;
		if (newRecordFound) {
			FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
			try {
				PipeLineSessionBase session = new PipeLineSessionBase();
				session.put(IPipeLineSession.businessCorrelationIdKey, TestTool.TESTTOOL_CORRELATIONID);
				message = readQueryFixedQuerySender.sendMessage(TESTTOOL_DUMMY_MESSAGE, session).asString();
			} catch(TimeOutException e) {
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

	private static int executeFileListenerRead(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileName, String fileContent) {
		int result = RESULT_ERROR;
		Map<?, ?> fileListenerInfo = (Map<?, ?>)queues.get(queueName);
		FileListener fileListener = (FileListener)fileListenerInfo.get("fileListener");
		String message = null;
		try {
			message = fileListener.getMessage();
		} catch(Exception e) {
			if (!"".equals(fileName)) {
				errorMessage("Could not read file from '" + queueName + "': " + e.getMessage(), e, writers);
			}
		}
		if (message == null) {
			if ("".equals(fileName)) {
				result = RESULT_OK;
			} else {
				errorMessage("Could not read file (null returned)", writers);
			}
		} else {
			result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties, writers, queueName);
		}
		return result;	
	}

	private static int executeFileSenderRead(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileName, String fileContent) {
		int result = RESULT_ERROR;
		Map<?, ?> fileSenderInfo = (Map<?, ?>)queues.get(queueName);
		FileSender fileSender = (FileSender)fileSenderInfo.get("fileSender");
		String message = null;
		try {
			message = fileSender.getMessage();
		} catch(Exception e) {
			if (!"".equals(fileName)) {
				errorMessage("Could not read file from '" + queueName + "': " + e.getMessage(), e, writers);
			}
		}
		if (message == null) {
			if ("".equals(fileName)) {
				result = RESULT_OK;
			} else {
				errorMessage("Could not read file (null returned)", writers);
			}
		} else {
			result = compareResult(step, stepDisplayName, fileName, fileContent, message, properties, writers, queueName);
		}
		return result;	
	}

	private static int executeXsltProviderListenerRead(String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, Map<String, Object> writers, String queueName, String fileContent, Map<String, Object> xsltParameters) {
		int result = RESULT_ERROR;
		Map<?, ?> xsltProviderListenerInfo = (Map<?, ?>)queues.get(queueName);
		if (xsltProviderListenerInfo == null) {
			errorMessage("No info found for xslt provider listener '" + queueName + "'", writers);
		} else {
			XsltProviderListener xsltProviderListener = (XsltProviderListener)xsltProviderListenerInfo.get("xsltProviderListener");
			if (xsltProviderListener == null) {
				errorMessage("XSLT provider listener not found for '" + queueName + "'", writers);
			} else {
				try {
					xsltProviderListener.processRequest(fileContent, xsltParameters);
					result = RESULT_OK;
				} catch(ListenerException e) {
					errorMessage("Could not transform xml: " + e.getMessage(), e, writers);
				}
				debugPipelineMessage(stepDisplayName, "Result:", fileContent, writers);
			}
		}
		return result;	
	}

	public static int executeStep(String step, Properties properties, String stepDisplayName, Map<String, Map<String, Object>> queues, Map<String, Object> writers) {
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
			debugMessage("Read file " + fileName, writers);
			fileContent = readFile(fileNameAbsolutePath, writers);
			if (fileContent == null) {
				errorMessage("Could not read file '" + fileName + "'", writers);
			} else {
				if (step.endsWith(".read")) {
					queueName = step.substring(i + 1, step.length() - 5);

					if ("nl.nn.adapterframework.jms.JmsListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeJmsListenerRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent);	
					} else 	if ("nl.nn.adapterframework.jdbc.FixedQuerySender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeFixedQuerySenderRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent);
					} else if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeSenderRead(step, stepDisplayName, properties, queues, writers, queueName, "ibisWebService", fileName, fileContent);
					} else if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeSenderRead(step, stepDisplayName, properties, queues, writers, queueName, "webService", fileName, fileContent);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeJavaListenerOrWebServiceListenerRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent);
					} else if ("nl.nn.adapterframework.http.HttpSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeSenderRead(step, stepDisplayName, properties, queues, writers, queueName, "http", fileName, fileContent);
					} else if ("nl.nn.adapterframework.senders.IbisJavaSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeSenderRead(step, stepDisplayName, properties, queues, writers, queueName, "ibisJava", fileName, fileContent);
					} else if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeJavaListenerOrWebServiceListenerRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent);
					} else if ("nl.nn.adapterframework.testtool.FileListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeFileListenerRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent);
					} else if ("nl.nn.adapterframework.testtool.FileSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeFileSenderRead(step, stepDisplayName, properties, queues, writers, queueName, fileName, fileContent);
					} else if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeXsltProviderListenerRead(stepDisplayName, properties, queues, writers, queueName, fileContent, createParametersMapFromParamProperties(properties, step, writers, false, null));
					} else {
						errorMessage("Property '" + queueName + ".className' not found or not valid", writers);
					}
				} else {
					queueName = step.substring(i + 1, step.length() - 6);

					String resolveProperties = properties.getProperty("scenario.resolveProperties");

					if( resolveProperties == null || !resolveProperties.equalsIgnoreCase("false") ){
						AppConstants appConstants = AppConstants.getInstance();
						fileContent = StringResolver.substVars(fileContent, appConstants);
					}

					if ("nl.nn.adapterframework.jms.JmsSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeJmsSenderWrite(stepDisplayName, queues, writers, queueName, fileContent);
					} else if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeSenderWrite(stepDisplayName, queues, writers, queueName, "ibisWebService", fileContent);
					} else if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeSenderWrite(stepDisplayName, queues, writers, queueName, "webService", fileContent);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeJavaOrWebServiceListenerWrite(stepDisplayName, queues, writers, queueName, fileContent);
					} else if ("nl.nn.adapterframework.http.HttpSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeSenderWrite(stepDisplayName, queues, writers, queueName, "http", fileContent);
					} else if ("nl.nn.adapterframework.senders.IbisJavaSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeSenderWrite(stepDisplayName, queues, writers, queueName, "ibisJava", fileContent);
					} else if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeJavaOrWebServiceListenerWrite(stepDisplayName, queues, writers, queueName, fileContent);
					} else if ("nl.nn.adapterframework.testtool.FileSender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeFileSenderWrite(stepDisplayName, queues, writers, queueName, fileContent);
					} else if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeXsltProviderListenerWrite(step, stepDisplayName, queues, writers, queueName, fileName, fileContent, properties);
					} else if ("nl.nn.adapterframework.senders.DelaySender".equals(properties.get(queueName + ".className"))) {
						stepPassed = executeDelaySenderWrite(stepDisplayName, queues, writers, queueName, fileContent);
					} else {
						errorMessage("Property '" + queueName + ".className' not found or not valid", writers);
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
			// AFAIK the Java 1.4 standard XML api doesn't provide a method
			// to determine the encoding used by an XML file, thus use the
			// XmlReader from ROME (https://rome.dev.java.net/).
			try {
				XmlReader xmlReader = new XmlReader(new File(fileName));
				encoding = xmlReader.getEncoding();
				xmlReader.close();
			} catch (IOException e) {
				errorMessage("Could not determine encoding for file '" + fileName + "': " + e.getMessage(), e, writers);
			}
		} else if (fileName.endsWith(".utf8")) {
			encoding = "UTF-8";
		} else {
			encoding = "ISO-8859-1";
		}
		if (encoding != null) {
			InputStreamReader inputStreamReader = null;
			try {
				StringBuffer stringBuffer = new StringBuffer();
				inputStreamReader = new InputStreamReader(new FileInputStream(fileName), encoding);
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
	public static void windiff(ServletContext application, HttpServletRequest request, String expectedFileName, String result, String expected) throws IOException, DocumentException, SenderException {
		IbisContext ibisContext = getIbisContext(application);
		AppConstants appConstants = getAppConstants(ibisContext);
		String windiffCommand = appConstants.getResolvedProperty("larva.windiff.command");
		if (windiffCommand == null) {
			String servletPath = request.getServletPath();
			int i = servletPath.lastIndexOf('/');
			String realPath = application.getRealPath(servletPath.substring(0, i));
			List<String> scenariosRootDirectories = new ArrayList<String>();
			List<String> scenariosRootDescriptions = new ArrayList<String>();
			String currentScenariosRootDirectory = TestTool.initScenariosRootDirectories(
					appConstants, realPath,
					null, scenariosRootDirectories,
					scenariosRootDescriptions, null);
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
		int ok = RESULT_ERROR;
		String printableExpectedResult = XmlUtils.replaceNonValidXmlCharacters(expectedResult);
		String printableActualResult = XmlUtils.replaceNonValidXmlCharacters(actualResult);
		String preparedExpectedResult = printableExpectedResult;
		String preparedActualResult = printableActualResult;
		debugMessage("Check decodeUnzipContentBetweenKeys properties", writers);
		boolean decodeUnzipContentBetweenKeysProcessed = false;
		int i = 1;
		while (!decodeUnzipContentBetweenKeysProcessed) {
			String key1 = properties.getProperty("decodeUnzipContentBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("decodeUnzipContentBetweenKeys" + i + ".key2");
			boolean replaceNewlines = false;
			if ("true".equals(properties.getProperty("decodeUnzipContentBetweenKeys" + i + ".replaceNewlines"))) {
				replaceNewlines = true;
			}
			if (key1 != null && key2 != null) {
				debugMessage("Decode and unzip content between key1 '" + key1 + "' and key2 '" + key2 + "' (replaceNewlines is " + replaceNewlines + ")", writers);
				preparedExpectedResult = decodeUnzipContentBetweenKeys(preparedExpectedResult, key1, key2, replaceNewlines, writers);
				preparedActualResult = decodeUnzipContentBetweenKeys(preparedActualResult, key1, key2, replaceNewlines, writers);
				i++;
			} else {
				decodeUnzipContentBetweenKeysProcessed = true;
			}
		}
		debugMessage("Check canonicaliseFilePathContentBetweenKeys properties", writers);
		boolean canonicaliseFilePathContentBetweenKeysProcessed = false;
		i = 1;
		while (!canonicaliseFilePathContentBetweenKeysProcessed) {
			String key1 = properties.getProperty("canonicaliseFilePathContentBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("canonicaliseFilePathContentBetweenKeys" + i + ".key2");
			if (key1 != null && key2 != null) {
				debugMessage("Canonicalise filepath content between key1 '" + key1 + "' and key2 '" + key2 + "'", writers);
				preparedExpectedResult = canonicaliseFilePathContentBetweenKeys(preparedExpectedResult, key1, key2, writers);
				preparedActualResult = canonicaliseFilePathContentBetweenKeys(preparedActualResult, key1, key2, writers);
				i++;
			} else {
				canonicaliseFilePathContentBetweenKeysProcessed = true;
			}
		}
		debugMessage("Check ignoreRegularExpressionKey properties", writers);
		boolean ignoreRegularExpressionKeyProcessed = false;
		i = 1;
		while (!ignoreRegularExpressionKeyProcessed) {
			String key = properties.getProperty("ignoreRegularExpressionKey" + i + ".key");
			if (key != null) {
				debugMessage("Ignore regular expression key '" + key + "'", writers);
				preparedExpectedResult = ignoreRegularExpression(preparedExpectedResult, key);
				preparedActualResult = ignoreRegularExpression(preparedActualResult, key);
				i++;
			} else {
				ignoreRegularExpressionKeyProcessed = true;
			}
		}
		debugMessage("Check removeRegularExpressionKey properties", writers);
		boolean removeRegularExpressionKeyProcessed = false;
		i = 1;
		while (!removeRegularExpressionKeyProcessed) {
			String key = properties.getProperty("removeRegularExpressionKey" + i + ".key");
			if (key != null) {
				debugMessage("Remove regular expression key '" + key + "'", writers);
				preparedExpectedResult = removeRegularExpression(preparedExpectedResult, key);
				preparedActualResult = removeRegularExpression(preparedActualResult, key);
				i++;
			} else {
				removeRegularExpressionKeyProcessed = true;
			}
		}
		debugMessage("Check replaceRegularExpressionKeys properties", writers);
		boolean replaceRegularExpressionKeysProcessed = false;
		i = 1;
		while (!replaceRegularExpressionKeysProcessed) {
			String key1 = properties.getProperty("replaceRegularExpressionKeys" + i + ".key1");
			String key2 = properties.getProperty("replaceRegularExpressionKeys" + i + ".key2");
			if (key1 != null && key2 != null) {
				debugMessage("Replace regular expression from '" + key1 + "' to '" + key2 + "'", writers);
				preparedExpectedResult = replaceRegularExpression(preparedExpectedResult, key1, key2);
				preparedActualResult = replaceRegularExpression(preparedActualResult, key1, key2);
				i++;
			} else {
				replaceRegularExpressionKeysProcessed = true;
			}
		}
		debugMessage("Check ignoreContentBetweenKeys properties", writers);
		boolean ignoreContentBetweenKeysProcessed = false;
		i = 1;
		while (!ignoreContentBetweenKeysProcessed) {
			String key1 = properties.getProperty("ignoreContentBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("ignoreContentBetweenKeys" + i + ".key2");
			if (key1 != null && key2 != null) {
				debugMessage("Ignore content between key1 '" + key1 + "' and key2 '" + key2 + "'", writers);
				preparedExpectedResult = ignoreContentBetweenKeys(preparedExpectedResult, key1, key2);
				preparedActualResult = ignoreContentBetweenKeys(preparedActualResult, key1, key2);
				i++;
			} else {
				ignoreContentBetweenKeysProcessed = true;
			}
		}
		debugMessage("Check ignoreKeysAndContentBetweenKeys properties", writers);
		boolean ignoreKeysAndContentBetweenKeysProcessed = false;
		i = 1;
		while (!ignoreKeysAndContentBetweenKeysProcessed) {
			String key1 = properties.getProperty("ignoreKeysAndContentBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("ignoreKeysAndContentBetweenKeys" + i + ".key2");
			if (key1 != null && key2 != null) {
				debugMessage("Ignore keys and content between key1 '" + key1 + "' and key2 '" + key2 + "'", writers);
				preparedExpectedResult = ignoreKeysAndContentBetweenKeys(preparedExpectedResult, key1, key2);
				preparedActualResult = ignoreKeysAndContentBetweenKeys(preparedActualResult, key1, key2);
				i++;
			} else {
				ignoreKeysAndContentBetweenKeysProcessed = true;
			}
		}
		debugMessage("Check removeKeysAndContentBetweenKeys properties", writers);
		boolean removeKeysAndContentBetweenKeysProcessed = false;
		i = 1;
		while (!removeKeysAndContentBetweenKeysProcessed) {
			String key1 = properties.getProperty("removeKeysAndContentBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("removeKeysAndContentBetweenKeys" + i + ".key2");
			if (key1 != null && key2 != null) {
				debugMessage("Remove keys and content between key1 '" + key1 + "' and key2 '" + key2 + "'", writers);
				preparedExpectedResult = removeKeysAndContentBetweenKeys(preparedExpectedResult, key1, key2);
				preparedActualResult = removeKeysAndContentBetweenKeys(preparedActualResult, key1, key2);
				i++;
			} else {
				removeKeysAndContentBetweenKeysProcessed = true;
			}
		}
		debugMessage("Check ignoreKey properties", writers);
		boolean ignoreKeyProcessed = false;
		i = 1;
		while (!ignoreKeyProcessed) {
			String key = properties.getProperty("ignoreKey" + i);
			if (key != null) {
				debugMessage("Ignore key '" + key + "'", writers);
				preparedExpectedResult = ignoreKey(preparedExpectedResult, key);
				preparedActualResult = ignoreKey(preparedActualResult, key);
				i++;
			} else {
				ignoreKeyProcessed = true;
			}
		}
		debugMessage("Check removeKey properties", writers);
		boolean removeKeyProcessed = false;
		i = 1;
		while (!removeKeyProcessed) {
			String key = properties.getProperty("removeKey" + i);
			if (key != null) {
				debugMessage("Remove key '" + key + "'", writers);
				preparedExpectedResult = removeKey(preparedExpectedResult, key);
				preparedActualResult = removeKey(preparedActualResult, key);
				i++;
			} else {
				removeKeyProcessed = true;
			}
		}
		debugMessage("Check replaceKey properties", writers);
		boolean replaceKeyProcessed = false;
		i = 1;
		while (!replaceKeyProcessed) {
			String key1 = properties.getProperty("replaceKey" + i + ".key1");
			String key2 = properties.getProperty("replaceKey" + i + ".key2");
			if (key1 != null && key2 != null) {
				debugMessage("Replace key from '" + key1 + "' to '" + key2 + "'", writers);
				preparedExpectedResult = replaceKey(preparedExpectedResult, key1, key2);
				preparedActualResult = replaceKey(preparedActualResult, key1, key2);
				i++;
			} else {
				replaceKeyProcessed = true;
			}
		}
		debugMessage("Check replaceEverywhereKey properties", writers);
		boolean replaceEverywhereKeyProcessed = false;
		i = 1;
		while (!replaceEverywhereKeyProcessed) {
			String key1 = properties.getProperty("replaceEverywhereKey" + i + ".key1");
			String key2 = properties.getProperty("replaceEverywhereKey" + i + ".key2");
			if (key1 != null && key2 != null) {
				debugMessage("Replace key from '" + key1 + "' to '" + key2 + "'", writers);
				preparedExpectedResult = replaceKey(preparedExpectedResult, key1, key2);
				preparedActualResult = replaceKey(preparedActualResult, key1, key2);
				i++;
			} else {
				replaceEverywhereKeyProcessed = true;
			}
		}
		debugMessage("Check ignoreCurrentTimeBetweenKeys properties", writers);
		boolean ignoreCurrentTimeBetweenKeysProcessed = false;
		i = 1;
		while (!ignoreCurrentTimeBetweenKeysProcessed) {
			String key1 = properties.getProperty("ignoreCurrentTimeBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("ignoreCurrentTimeBetweenKeys" + i + ".key2");
			String pattern = properties.getProperty("ignoreCurrentTimeBetweenKeys" + i + ".pattern");
			String margin = properties.getProperty("ignoreCurrentTimeBetweenKeys" + i + ".margin");
			boolean errorMessageOnRemainingString = true;
			if ("false".equals(properties.getProperty("ignoreCurrentTimeBetweenKeys" + i + ".errorMessageOnRemainingString"))) {
				errorMessageOnRemainingString = false;
			}
			if (key1 != null && key2 != null && margin != null) {
				debugMessage("Ignore current time between key1 '" + key1 + "' and key2 '" + key2 + "' (errorMessageOnRemainingString is " + errorMessageOnRemainingString + ")", writers);
				debugMessage("For result string", writers);
				preparedActualResult = ignoreCurrentTimeBetweenKeys(preparedActualResult, key1, key2, pattern, margin, errorMessageOnRemainingString, false, writers);
				debugMessage("For expected string", writers);
				preparedExpectedResult = ignoreCurrentTimeBetweenKeys(preparedExpectedResult, key1, key2, pattern, margin, errorMessageOnRemainingString, true, writers);
				i++;
			} else {
				ignoreCurrentTimeBetweenKeysProcessed = true;
			}
		}
		debugMessage("Check ignoreContentBeforeKey properties", writers);
		boolean ignoreContentBeforeKeyProcessed = false;
		i = 1;
		while (!ignoreContentBeforeKeyProcessed) {
			String key = properties.getProperty("ignoreContentBeforeKey" + i);
			if (key == null) {
				key = properties.getProperty("ignoreContentBeforeKey" + i + ".key");
			}
			if (key != null) {
				debugMessage("Ignore content before key '" + key + "'", writers);
				preparedExpectedResult = ignoreContentBeforeKey(preparedExpectedResult, key);
				preparedActualResult = ignoreContentBeforeKey(preparedActualResult, key);
				i++;
			} else {
				ignoreContentBeforeKeyProcessed = true;
			}
		}
		debugMessage("Check ignoreContentAfterKey properties", writers);
		boolean ignoreContentAfterKeyProcessed = false;
		i = 1;
		while (!ignoreContentAfterKeyProcessed) {
			String key = properties.getProperty("ignoreContentAfterKey" + i);
			if (key == null) {
				key = properties.getProperty("ignoreContentAfterKey" + i + ".key");
			}
			if (key != null) {
				debugMessage("Ignore content after key '" + key + "'", writers);
				preparedExpectedResult = ignoreContentAfterKey(preparedExpectedResult, key);
				preparedActualResult = ignoreContentAfterKey(preparedActualResult, key);
				i++;
			} else {
				ignoreContentAfterKeyProcessed = true;
			}
		}
		debugMessage("Check ignoreContentAfterKey properties", writers);
		String diffType = properties.getProperty(step + ".diffType");
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
				if (formattedPreparedExpectedResult.length() > i) {
					j = formattedPreparedExpectedResult.length();
				}
				for (i = 0; i < j; i++) {
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
	 * @param propertiesDirectory suffix for filenames specified by properties
	 *                            with a .valuefile suffix. Can be left empty.
	 *  
	 * @return A map with parameters
	 */
	private static Map<String, Object> createParametersMapFromParamProperties(Properties properties, String property, Map<String, Object> writers, boolean createParameterObjects, IPipeLineSession session) {
		debugMessage("Search parameters for property '" + property + "'", writers);
		Map<String, Object> result = new HashMap<String, Object>();
		boolean processed = false;
		int i = 1;
		while (!processed) {
			String name = properties.getProperty(property + ".param" + i + ".name");
			if (name != null) {
				Object value;
				String type = properties.getProperty(property + ".param" + i + ".type");
				if ("httpResponse".equals(type)) {
					String outputFile;
					String filename = properties.getProperty(property + ".param" + i + ".filename");
					if (filename != null) {
						outputFile = properties.getProperty(property + ".param" + i + ".filename.absolutepath");
					} else {
						outputFile = properties.getProperty(property + ".param" + i + ".outputfile");
					}
					HttpServletResponseMock httpServletResponseMock = new HttpServletResponseMock();
					httpServletResponseMock.setOutputFile(outputFile);
					value = httpServletResponseMock;
				} else if ("httpRequest".equals(type)) {
					value = properties.getProperty(property + ".param" + i + ".value");
					if("multipart".equals(value)){
						MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
						// following line is required to avoid
						// "(FileUploadException) the request was rejected because
						// no multipart boundary was found"
						request.setContentType(
								"multipart/mixed;boundary=gc0p4Jq0M2Yt08jU534c0p");
						List<Part> parts = new ArrayList<Part>();
						boolean partsProcessed = false;
						int j = 1;
						while (!partsProcessed) {
							String filename = properties.getProperty(property
									+ ".param" + i + ".part" + j + ".filename");
							if (filename == null) {
								partsProcessed = true;
							} else {
								String partFile = properties.getProperty(property
										+ ".param" + i + ".part" + j + ".filename.absolutepath");
								String partType = properties.getProperty(property
										+ ".param" + i + ".part" + j + ".type");
								String partName = properties.getProperty(property
										+ ".param" + i + ".part" + j + ".name");
								if ("file".equalsIgnoreCase(partType)) {
									File file = new File(partFile);
									try {
										FilePart filePart = new FilePart(
												"file" + j,
												(partName == null
														? file.getName()
														: partName),
												file);
										parts.add(filePart);
									} catch (FileNotFoundException e) {
										errorMessage(
												"Could not read file '" + partFile
														+ "': " + e.getMessage(),
												e, writers);
									}
								} else {
									String string = readFile(partFile, writers);
									StringPart stringPart = new StringPart(
											(partName == null ? "string" + j
													: partName),
											string);
									parts.add(stringPart);
								}
								j++;
							}
						}
						Part allParts[] = new Part[parts.size()];
						allParts = parts.toArray(allParts);
						MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(
								allParts, new PostMethod().getParams());
						ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
						try {
							multipartRequestEntity.writeRequest(requestContent);
						} catch (IOException e) {
							errorMessage(
									"Could not create multipart: " + e.getMessage(),
									e, writers);
						}
						request.setContent(requestContent.toByteArray());
						request.setContentType(
								multipartRequestEntity.getContentType());
						value = request;
					}
					else{
						MockHttpServletRequest request = new MockHttpServletRequest();
						value = request;
					}
				} else {
					value = properties.getProperty(property + ".param" + i + ".value");
					if (value == null) {
						String filename = properties.getProperty(property + ".param" + i + ".valuefile.absolutepath");
						if (filename != null) {
							value = readFile(filename, writers);
						} else {
							String inputStreamFilename = properties.getProperty(property + ".param" + i + ".valuefileinputstream.absolutepath");
							if (inputStreamFilename != null) {
								try {
									value = new FileInputStream(inputStreamFilename);
								} catch(FileNotFoundException e) {
									errorMessage("Could not read file '" + inputStreamFilename + "': " + e.getMessage(), e, writers);
								}
							}
						}
					}
				}
				if (value != null && value instanceof String) {
					if ("node".equals(properties.getProperty(property + ".param" + i + ".type"))) {
						try {
							value = XmlUtils.buildNode((String)value, true);
						} catch (DomBuilderException e) {
							errorMessage("Could not build node for parameter '" + name + "' with value: " + value, e, writers);
						}
					} else if ("domdoc".equals(properties.getProperty(property + ".param" + i + ".type"))) {
						try {
							value = XmlUtils.buildDomDocument((String)value, true);
						} catch (DomBuilderException e) {
							errorMessage("Could not build node for parameter '" + name + "' with value: " + value, e, writers);
						}
					} else if ("list".equals(properties.getProperty(property + ".param" + i + ".type"))) {
						List<String> parts = new ArrayList<String>(Arrays.asList(((String)value).split("\\s*(,\\s*)+")));
						List<String> list = new LinkedList<String>();
						for (String part : parts) {
							list.add(part);
						}
						value = list;
					} else if ("map".equals(properties.getProperty(property + ".param" + i + ".type"))) {
						List<String> parts = new ArrayList<String>(Arrays.asList(((String)value).split("\\s*(,\\s*)+")));
						Map<String, String> map = new LinkedHashMap<String, String>();
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
				}
				if (createParameterObjects) {
					String  pattern = properties.getProperty(property + ".param" + i + ".pattern");
					if (value == null && pattern == null) {
						errorMessage("Property '" + property + ".param" + i + " doesn't have a value or pattern", writers);
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
						errorMessage("Property '" + property + ".param" + i + ".value' or '" + property + ".param" + i + ".valuefile' or '" + property + ".param" + i + ".valuefileinputstream' not found while property '" + property + ".param" + i + ".name' exist", writers);
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
}
