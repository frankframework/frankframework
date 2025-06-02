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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.json.JsonException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.SenderException;
import org.frankframework.json.JsonUtil;
import org.frankframework.larva.output.HtmlScenarioOutputRenderer;
import org.frankframework.larva.output.LarvaHtmlWriter;
import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.larva.output.PlainTextScenarioOutputRenderer;
import org.frankframework.larva.output.TestExecutionObserver;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.stream.FileMessage;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CleanerProvider;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.FileUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.ProcessUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TemporaryDirectoryUtils;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

/**
 * @author Jaco de Groot
 */
@Log4j2
public class LarvaTool {
	public static final int RESULT_ERROR = 0;
	public static final int RESULT_OK = 1;
	public static final int RESULT_AUTOSAVED = 2;

	private final @Getter ApplicationContext applicationContext;
	private final @Getter LarvaConfig larvaConfig;
	/**
	 * Larva API calls that do not provide an output-writer can collect relevant error and warning messages for the
	 * user to be returned via the API from the message list.
	 */
	private final @Getter List<LarvaMessage> messages = new ArrayList<>();
	private final @Getter ScenarioLoader scenarioLoader;
	private @Getter @Setter LarvaWriter writer;

	public static LarvaTool createInstance(ServletContext servletContext) {
		return createInstance(getApplicationContext(servletContext));
	}

	public static LarvaTool createInstance(ApplicationContext applicationContext) {
		LarvaConfig larvaConfig = new LarvaConfig();
		return new LarvaTool(applicationContext, larvaConfig);
	}

	public LarvaTool(ApplicationContext applicationContext, LarvaConfig larvaConfig) {
		this.applicationContext = applicationContext;
		this.larvaConfig = larvaConfig;
		this.scenarioLoader = new ScenarioLoader(this);

		XMLUnit.setIgnoreWhitespace(true);
	}

	private static ApplicationContext getApplicationContext(ServletContext servletContext) {
		return FrankApplicationInitializer.getIbisContext(servletContext).getApplicationContext();
	}

	// Invoked by LarvaServlet
	public static TestRunStatus runScenarios(ServletContext servletContext, HttpServletRequest request, Writer out) {
		LarvaHtmlConfig larvaHtmlConfig = new LarvaHtmlConfig(request);
		LarvaTool larvaTool = new LarvaTool(getApplicationContext(servletContext), larvaHtmlConfig);
		LarvaHtmlWriter larvaHtmlWriter = new LarvaHtmlWriter(larvaHtmlConfig, out);
		TestExecutionObserver testExecutionObserver = new HtmlScenarioOutputRenderer(larvaHtmlConfig, larvaHtmlWriter);
		return larvaTool.runScenarios(request.getParameter(LarvaHtmlConfig.REQUEST_PARAM_EXECUTE), testExecutionObserver, larvaHtmlWriter);
	}

	public static TestRunStatus runScenarios(ApplicationContext applicationContext, Writer writer, String execute) {
		LarvaTool larvaTool = createInstance(applicationContext);
		LarvaWriter larvaWriter = new LarvaWriter(larvaTool.getLarvaConfig(), writer);
		TestExecutionObserver testExecutionObserver = new PlainTextScenarioOutputRenderer(larvaWriter);
		return larvaTool.runScenarios(execute, testExecutionObserver, larvaWriter);
	}

	/**
	 * @return {@link TestRunStatus} with result of test
	 */
	public TestRunStatus runScenarios(String execute, TestExecutionObserver testExecutionObserver, LarvaWriter writer) {
		this.writer = writer;
		log.debug("Initialize scenarios root directories");
		TestRunStatus testRunStatus = createTestRunStatus();
		String currentScenariosRootDirectory = testRunStatus.initScenarioDirectories();

		if (testRunStatus.getScenarioDirectories().isEmpty()) {
			errorMessage("No scenarios root directories found");
			throw new LarvaException("No scenarios root directories found");
		}
		log.debug("Read scenarios from directory '" + StringEscapeUtils.escapeJava(currentScenariosRootDirectory) + "'");
		testRunStatus.readScenarioFiles(scenarioLoader);

		testExecutionObserver.startTestSuiteExecution(testRunStatus);

		if (execute == null) {
			return testRunStatus;
		}

		List<Scenario> scenarioFiles = testRunStatus.getScenariosToRun(execute);
		if (scenarioFiles.isEmpty()) {
			errorMessage("No scenarios found");
		}

		log.debug("Initialize statistics variables");
		long startTime = System.currentTimeMillis();
		log.debug("Execute scenario('s)");
		ScenarioRunner scenarioRunner = createScenarioRunner(testExecutionObserver, testRunStatus);
		// If only one scenario is executed, do not use multithreading, because they mostly use the same resources
		if (scenarioFiles.size() == 1) {
			scenarioRunner.setMultipleThreads(false);
		}
		scenarioRunner.runScenarios(scenarioFiles, currentScenariosRootDirectory);
		flushOutput();

		long executionTime = System.currentTimeMillis() - startTime;
		printScenarioExecutionStatistics(testExecutionObserver, testRunStatus, executionTime);

		testExecutionObserver.endTestSuiteExecution(testRunStatus);
		CleanerProvider.logLeakStatistics();

		return testRunStatus;
	}

	public @Nonnull TestRunStatus createTestRunStatus() {
		return new TestRunStatus(larvaConfig, this);
	}

	public @Nonnull ScenarioRunner createScenarioRunner(TestExecutionObserver testExecutionObserver, TestRunStatus testRunStatus) {
		return new ScenarioRunner(this, testExecutionObserver, testRunStatus);
	}

	private void printScenarioExecutionStatistics(TestExecutionObserver testExecutionObserver, TestRunStatus testRunStatus, long executionTime) {
		log.debug("Print statistics information");
		testExecutionObserver.executionOverview(testRunStatus, executionTime);
	}

	public void debugMessage(String message) {
		log.debug(message);
		if (writer != null) {
			writer.debugMessage(message);
		}
	}

	public void infoMessage(String message) {
		log.info(message);
		if (writer != null) {
			writer.infoMessage(message);
		}
	}

	public void warningMessage(String message) {
		log.warn(message);
		messages.add(new LarvaMessage(LarvaLogLevel.WARNING, message));
		if (writer != null) {
			writer.warningMessage(message);
		}
	}

	public void errorMessage(String message) {
		log.error(message);
		messages.add(new LarvaMessage(LarvaLogLevel.ERROR, message));
		if (writer != null) {
			writer.errorMessage(message);
		}
	}

	public void errorMessage(String message, Exception exception) {
		log.error(message, exception);
		messages.add(new LarvaMessage(LarvaLogLevel.ERROR, message, exception));
		if (writer != null) {
			writer.errorMessage(message, exception);
		}
	}

	/**
	 * Larva API calls that do not provide an output-writer can collect relevant log messages for the
	 * user to be returned via the API from the message list.
	 *
	 * @param level Minimum level to be returned
	 * @return List of LarvaMessages
	 */
	public List<LarvaMessage> getMessagesAtLevel(LarvaLogLevel level) {
		return messages.stream()
				.filter(m -> level.shouldLog(m.getLogLevel()))
				.toList();
	}

	/**
	 * Get the scenario directory selected as default Active with the current {@link LarvaConfig}
	 * @return Active Scenario Directory
	 */
	public String getActiveScenariosDirectory() {
		TestRunStatus testRunStatus = createTestRunStatus();
		return testRunStatus.initScenarioDirectories();
	}

	public void flushOutput() {
		if (writer != null) {
			writer.flush();
		}
	}

	// Used by saveResultToFile.jsp
	public void windiff(String expectedFileName, String result, String expected) throws IOException, SenderException {
		AppConstants appConstants = AppConstants.getInstance();
		String windiffCommand = appConstants.getProperty("larva.windiff.command");
		if (windiffCommand == null) {
			TestRunStatus testRunStatus = createTestRunStatus();
			String currentScenariosRootDirectory = testRunStatus.initScenarioDirectories();
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
	public @Nullable String messageToString(Message message) {
		// TODO: This should just throw instead of returning NULL, and the caller should catch instead of checking NULL return value
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

	public int compareResult(TestExecutionObserver testExecutionObserver, Scenario scenario, Step step, String fileName, Message expectedResultMessage, Message actualResultMessage) {
		Properties properties = scenario.getProperties();
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
				printableExpectedResult = JsonUtil.jsonPretty(expectedResult);
			} catch (JsonException e) {
				debugMessage("Could not prettify Json: "+e.getMessage());
				printableExpectedResult = expectedResult;
			}
			try {
				printableActualResult = JsonUtil.jsonPretty(actualResult);
			} catch (JsonException e) {
				debugMessage("Could not prettify Json: "+e.getMessage());
				printableActualResult = actualResult;
			}
		} else {
			printableExpectedResult = XmlEncodingUtils.replaceNonValidXmlCharacters(expectedResult);
			printableActualResult = XmlEncodingUtils.replaceNonValidXmlCharacters(actualResult);
		}

		// Map all identifier based properties once
		Map<String, Map<String, Map<String, String>>> ignoreMap = scenario.getIgnoreMap();

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
				testExecutionObserver.stepMessageSuccess(scenario, step, "Result", printableActualResult, preparedActualResult);
			} else {
				String message;
				if (diffException == null) {
					message = diff.toString();
				} else {
					message = "Exception during XML diff: " + diffException.getMessage();
					errorMessage("Exception during XML diff: ", diffException);
				}
				ok = reportFailedCompare(testExecutionObserver, scenario, step, message, printableExpectedResult, preparedExpectedResult, printableActualResult, preparedActualResult, actualResult, ok);
			}
		} else {
			// txt diff
			String formattedPreparedExpectedResult = formatString(preparedExpectedResult);
			String formattedPreparedActualResult = formatString(preparedActualResult);
			if (formattedPreparedExpectedResult.equals(formattedPreparedActualResult)) {
				ok = RESULT_OK;
				testExecutionObserver.stepMessageSuccess(scenario, step, "Result", printableActualResult, preparedActualResult);
			} else {
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
				ok = reportFailedCompare(testExecutionObserver, scenario, step, message, printableExpectedResult, preparedExpectedResult, printableActualResult, preparedActualResult, actualResult, ok);
			}
		}
		return ok;
	}

	private int reportFailedCompare(TestExecutionObserver testExecutionObserver, Scenario scenario, Step step, String message, String printableExpectedResult, String preparedExpectedResult, String printableActualResult, String preparedActualResult, String actualResult, int ok) {
		String filenameAbsolutePath = step.getStepDataFile();
		testExecutionObserver.stepMessageFailed(scenario, step, message, printableExpectedResult, preparedExpectedResult, printableActualResult, preparedActualResult);
		if (larvaConfig.isAutoSaveDiffs()) {
			debugMessage("Copy actual result to ["+filenameAbsolutePath+"]");
			try {
				org.apache.commons.io.FileUtils.writeStringToFile(new File(filenameAbsolutePath), actualResult, Charset.defaultCharset());
				ok = RESULT_AUTOSAVED;
			} catch (IOException e) {
				// Ignore
				errorMessage("Cannot write actual result to ["+filenameAbsolutePath+"]", e);
			}
		}
		return ok;
	}

	public String prepareResultForCompare(String input, Properties properties, Map<String, Map<String, Map<String, String>>> ignoreMap) {
		String result = input;

		// TESTDATA-dir based file paths in results can often not be compared properly because of location differences, and complicated by system-dependent file paths.
		// Changing Windows backslashes to Unix forward slashes in the rest of file paths is done with regexes configured in global.properties
		String testDataDirPropertyValue = AppConstants.getInstance().getProperty("testdata.dir");
		if (testDataDirPropertyValue != null) {
			String testDataDirNative = new File(testDataDirPropertyValue).getAbsolutePath();
			String testDataDirWindows = FilenameUtils.normalize(testDataDirPropertyValue, false);
			String testDataDirUnix = FilenameUtils.normalize(testDataDirPropertyValue, true);
			result = result.replace(testDataDirNative, "TESTDATA_DIR")
					.replace(testDataDirWindows, "TESTDATA_DIR")
					.replace(testDataDirUnix, "TESTDATA_DIR");
		}

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

	public static String doActionBetweenKeys(String key, String value, Properties properties, Map<String, Map<String, Map<String, String>>> ignoreMap, BetweenKeysAction action) {
		String result = value;
		log.debug("Check [{}] properties", key);
		boolean lastKeyIndexProcessed = false;
		int i = 1;
		while (!lastKeyIndexProcessed) {
			String keyPrefix = key + i + ".";
			String key1 = properties.getProperty(keyPrefix + "key1");
			String key2 = properties.getProperty(keyPrefix + "key2");
			if (key1 != null && key2 != null) {
				log.debug("[{}] between key1 [{}] and key2 [{}]", key, key1, key2);
				result = action.format(result, k -> properties.getProperty(keyPrefix + k), key1, key2);
				i++;
			} else if (key1 != null || key2 != null) {
				throw new IllegalArgumentException("Error in Larva scenario file: Spec for [" + key + i + "] is incomplete; key1=[" + key1 + "], key2=[" + key2 + "]");
			} else {
				lastKeyIndexProcessed = true;
			}
		}

		Map<String, Map<String, String>> keySpecMap = ignoreMap.get(key);
		if (keySpecMap != null) {
			for (Entry<String, Map<String, String>> spec : keySpecMap.entrySet()) {
				Map<String, String> keyPair = spec.getValue();

				String key1 = keyPair.get("key1");
				String key2 = keyPair.get("key2");

				if (key1 == null || key2 == null) {
					throw new IllegalArgumentException("Error in Larva scenario file: Spec [" + key + "." + spec.getKey() + "] is incomplete; key1=[" + key1 + "], key2=[" + key2 + "]");
				}

				log.debug("[{}] between key1 [{}] and key2 [{}]", key, key1, key2);
				result = action.format(result, keyPair::get, key1, key2);
			}
		}

		return result;
	}

	public static String doActionWithSingleKey(String keyName, String value, Properties properties, Map<String, Map<String, Map<String, String>>> ignoreMap, SingleKeyAction action) {
		String result = value;
		log.debug("Check [{}] properties", keyName);
		boolean lastKeyIndexProcessed = false;
		int i = 1;
		while (!lastKeyIndexProcessed) {
			String keyPrefix = keyName + i;
			String key = properties.getProperty(keyPrefix);
			if (key == null) {
				key = properties.getProperty(keyPrefix + ".key");
			}
			if (key != null) {
				log.debug("[{}] key [{}]", keyName, key);
				result = action.format(result, k -> properties.getProperty(keyPrefix + "." + k), key);
				i++;
			} else {
				lastKeyIndexProcessed = true;
			}
		}

		Map<String, Map<String, String>> keySpecMap = ignoreMap.get(keyName);
		if (keySpecMap != null) {
			Iterator<Entry<String,Map<String,String>>> keySpecIt = keySpecMap.entrySet().iterator();
			while (keySpecIt.hasNext()) {
				Entry<String,Map<String,String>> spec = keySpecIt.next();
				Map<String, String> keyPair = spec.getValue();

				String key = keyPair.get("key");

				log.debug("[{}] key [{}]", keyName, key);
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
			log.debug("Key 1 found");
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				log.debug("Key 2 found");
				String encoded = result.substring(i + key1.length(), j);
				String unzipped;
				byte[] decodedBytes;
				log.debug("Decode");
				decodedBytes = Base64.decodeBase64(encoded);
				try {
					log.debug("Unzip");
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
			log.debug("Key 1 found");
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				log.debug("Key 2 found");
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
	 * @return A map with parameters
	 */
	// Replace or merge this with LarvaActionUtils.createParametersMapFromParamProperties
	public Map<String, Object> createParametersMapFromParamProperties(Properties properties) {
		final String _name = ".name";
		final String _param = "param";
		final String _type = ".type";
		Map<String, Object> result = new HashMap<>();
		boolean processed = false;
		int i = 1;
		while (!processed) {
			String name = properties.getProperty(_param + i + _name);
			if (name != null) {
				String type = properties.getProperty(_param + i + _type);
				String propertyValue = properties.getProperty(_param + i + ".value");
				Object value = propertyValue;

				if (value == null) {
					String filename = properties.getProperty(_param + i + ".valuefile.absolutepath");
					if (filename != null) {
						value = new FileMessage(new File(filename));
					} else {
						String inputStreamFilename = properties.getProperty(_param + i + ".valuefileinputstream.absolutepath");
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
					errorMessage("Property '" + _param + i + ".value' or '" + _param + i + ".valuefile' not found while property '" + _param + i + ".name' exist");
				} else {
					result.put(name, value);
					log.debug("Add param with name [{}] and value [{}] for property '" + "'", name, value);
				}
				i++;
			} else {
				processed = true;
			}
		}
		return result;
	}

	/**
	 * Replace all linefeed-characters with the platform-specific linefeed-character for the current platform.
	 */
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
	 * @return Map<String, Map<String, Map<String, String>>> as Map<'ignoreContentBetweenKeys', Map<'fieldA', Map<'key1', '<field name="A">'>
	*/
	public static Map<String, Map<String, Map<String, String>>> mapPropertiesToIgnores(Properties properties){
		Map<String, Map<String, Map<String, String>>> returnMap = new HashMap<>();
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
				Map<String, Map<String, String>> ignoreMap = returnMap.computeIfAbsent(ignore, k -> new HashMap<>());


				// Find return map for identifier
				// Create return map for identifier if not exist
				Map<String, String> idMap = ignoreMap.computeIfAbsent(id, k -> new HashMap<>());


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
