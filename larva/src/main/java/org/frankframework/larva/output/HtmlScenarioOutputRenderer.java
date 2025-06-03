/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.larva.output;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.larva.LarvaHtmlConfig;
import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.Scenario;
import org.frankframework.larva.Step;
import org.frankframework.larva.TestRunStatus;
import org.frankframework.util.XmlEncodingUtils;

/**
 * The HtmlScenarioOutputRenderer class is responsible for rendering HTML output during the execution
 * of tests in the Larva test tool. It implements the {@link TestExecutionObserver}
 * interface to observe and react to different phases and events of test execution. This class is primarily used to
 * provide detailed, structured HTML output for a given test suite, scenarios, steps, and their results.
 * <p>
 * The renderer operates based on the provided configuration and handles writing HTML content
 * using an accompanying LarvaHtmlWriter instance.
 * </p>
 */
@Log4j2
public class HtmlScenarioOutputRenderer implements TestExecutionObserver {

	private static final String TR_STARTING_TAG="<tr>";
	private static final String TR_CLOSING_TAG="</tr>";
	private static final String TD_STARTING_TAG="<td>";
	private static final String TD_CLOSING_TAG="</td>";
	private static final String TABLE_CLOSING_TAG="</table>";

	private final LarvaHtmlConfig config;
	private final LarvaHtmlWriter writer;

	private boolean evenStep = false;

	public HtmlScenarioOutputRenderer(LarvaHtmlConfig config, LarvaHtmlWriter writer) {
		this.config = config;
		this.writer = writer;
	}


	@Override
	public void startTestSuiteExecution(TestRunStatus testRunStatus) {
		printHtmlForm(testRunStatus);
	}

	@Override
	public void endTestSuiteExecution(TestRunStatus testRunStatus) {
		writeHtml("<br/><br/>");
		printHtmlForm(testRunStatus);
		writeHtml("",  true);
		writer.flush();
	}

	@Override
	public void executionOverview(TestRunStatus testRunStatus, long executionTime) {
		if (testRunStatus.getScenariosFailedCount() > 0) {
			writeHtml(LarvaLogLevel.SCENARIO_FAILED, "<br/><br/><div><h1 class='failed'>Failed Scenarios</h1>", false);
			testRunStatus.getFailedScenarios().forEach(scenario -> writeHtml(LarvaLogLevel.SCENARIO_FAILED, "<h2><a href='#" + scenario.getId() + "'>" + LarvaHtmlWriter.encodeForHtml(scenario.getName() + " - " + scenario.getDescription()) + "</a></h2>", false));
			writeHtml(LarvaLogLevel.SCENARIO_FAILED, "</div>", true);
		}
		writeHtml(LarvaLogLevel.TOTALS, "<br/><br/>", false);
		String scenariosPassedMessage = testRunStatus.buildScenariosPassedMessage(executionTime);
		String scenariosFailedMessage = testRunStatus.buildScenariosFailedMessage(executionTime);
		String scenariosAutosavedMessage = testRunStatus.buildScenariosAutoSavedMessage(executionTime);
		String scenariosTotalMessage = testRunStatus.buildScenariosTotalMessage(executionTime);
		if (scenariosPassedMessage != null) {
			writeHtml(LarvaLogLevel.TOTALS, "<h1 class='passed'>" + LarvaHtmlWriter.encodeForHtml(scenariosPassedMessage) + "</h1>", true);
		}
		if (scenariosFailedMessage != null) {
			writeHtml(LarvaLogLevel.TOTALS, "<h1 class='failed'>" + LarvaHtmlWriter.encodeForHtml(scenariosFailedMessage) + "</h1>", true);
		}
		if (scenariosAutosavedMessage != null) {
			writeHtml(LarvaLogLevel.TOTALS, "<h1 class='autosaved'>" + LarvaHtmlWriter.encodeForHtml(scenariosAutosavedMessage) + "</h1>", true);
		}
		if (scenariosTotalMessage != null) {
			writeHtml(LarvaLogLevel.TOTALS, "<h1 class='total'>" + LarvaHtmlWriter.encodeForHtml(scenariosTotalMessage) + "</h1>", true);
		}
	}

	@Override
	public void startScenario(TestRunStatus testRunStatus, Scenario scenario) {
		evenStep = false;

		writer.setBufferOutputMessages(true);
		writeHtml(LarvaLogLevel.SCENARIO_FAILED, "<br/><br/><div id='" + scenario.getId() + "' class='scenario'>", false);
	}

	@Override
	public void finishScenario(TestRunStatus testRunStatus, Scenario scenario, int scenarioResult, String scenarioResultMessage) {

		LarvaLogLevel resultLogLevel;
		StringBuilder outputMessage = new StringBuilder();
		if (scenarioResult == LarvaTool.RESULT_OK) {
			outputMessage.append("<h2 class='passed'>");
			resultLogLevel = LarvaLogLevel.SCENARIO_PASSED_FAILED;
		} else if (scenarioResult == LarvaTool.RESULT_AUTOSAVED) {
			outputMessage.append("<h2 class='autosaved'>");
			resultLogLevel = LarvaLogLevel.SCENARIO_PASSED_FAILED;
		} else {
			outputMessage.append("<h2 class='failed'>");
			resultLogLevel = LarvaLogLevel.SCENARIO_FAILED;
		}
		outputMessage.append(LarvaHtmlWriter.encodeForHtml(scenarioResultMessage)).append("</h2>");
		writeHtml(resultLogLevel, outputMessage.toString(), false);

		if (!scenario.getMessages().isEmpty()) {
			writeHtml(LarvaLogLevel.SCENARIO_FAILED, "<div class='failed'>", false);
			writeHtml(LarvaLogLevel.SCENARIO_FAILED, "<h3 class='failed'>Warnings</h3>", false);
			writeHtml(LarvaLogLevel.SCENARIO_FAILED, "<ul>", false);
			scenario.getMessages().forEach(message -> writeHtml(LarvaLogLevel.SCENARIO_FAILED, "<li>" + LarvaHtmlWriter.encodeForHtml(message.toString()) + "</li>", false));
			writeHtml(LarvaLogLevel.SCENARIO_FAILED, "</ul>", false);
			writeHtml(LarvaLogLevel.SCENARIO_FAILED, "</div>", false);
		}

		// Close the <div> tag created in #startScenario
		writeHtml(LarvaLogLevel.SCENARIO_FAILED,"</div>", true);
		writer.setBufferOutputMessages(false);
	}

	@Override
	public void startStep(TestRunStatus testRunStatus, Scenario scenario, Step step) {
		// Create a div for the step. Will be closed in finishStep().
		if (evenStep) {
			writeHtml("<div class='even'>");
		} else {
			writeHtml("<div class='odd'>");
		}
		evenStep = !evenStep;
		writer.debugMessage("Execute step '" + step.getDisplayName() + "'");
	}

	@Override
	public void finishStep(TestRunStatus testRunStatus, Scenario scenario, Step step, int stepResult, String stepResultMessage) {
		if (writer.shouldWriteLevel(LarvaLogLevel.STEP_PASSED_FAILED)) {
			StringBuilder outputMessage = new StringBuilder();
			if (stepResult == LarvaTool.RESULT_OK) {
				outputMessage.append("<h3 class='passed'>");
			} else if (stepResult == LarvaTool.RESULT_AUTOSAVED) {
				outputMessage.append("<h3 class='autosaved'>");
			} else {
				outputMessage.append("<h3 class='failed'>");
			}
			outputMessage.append(LarvaHtmlWriter.encodeForHtml(stepResultMessage)).append("</h2>");
			writeHtml(LarvaLogLevel.STEP_PASSED_FAILED, outputMessage.toString(), false);
		}
		// Close the div created when startStep was called.
		writeHtml("</div>");
	}

	@Override
	public void stepMessage(Scenario scenario, Step step, String description, String stepMessage) {
		writer.writeStepMessageBox(LarvaLogLevel.PIPELINE_MESSAGES, "message container", step.getDisplayName(), description, "messagebox", stepMessage);
	}

	@Override
	public void stepMessageSuccess(Scenario scenario, Step step, String description, String stepResultMessage, String stepResultMessagePreparedForDiff) {
		String stepName = step.getDisplayName();
		writer.writeStepMessageBox(LarvaLogLevel.PIPELINE_MESSAGES, "message container", stepName, description, "messagebox", stepResultMessage);
		writer.writeStepMessageBox(LarvaLogLevel.PIPELINE_MESSAGES_PREPARED_FOR_DIFF, "message container", stepName, description + " as prepared for diff", "messagebox", stepResultMessagePreparedForDiff);
	}

	@Override
	public void stepMessageFailed(Scenario scenario, Step step, String description, String stepExpectedResultMessage, String stepExpectedResultMessagePreparedForDiff, String stepActualResultMessage, String stepActualResultMessagePreparedForDiff) {
		String stepName = step.getDisplayName();
		String stepDataFileName = step.isInline() ? null : step.getStepDataFile();
		writer.writeStepMessageWithDiffBox(LarvaLogLevel.WRONG_PIPELINE_MESSAGES, "error container", stepName, stepDataFileName, "scenario", "raw", description, stepActualResultMessage, stepExpectedResultMessage);
		writer.writeStepMessageWithDiffBox(LarvaLogLevel.WRONG_PIPELINE_MESSAGES_PREPARED_FOR_DIFF, "error container", stepName, stepDataFileName, "scenario", "prepared for diff", description, stepActualResultMessagePreparedForDiff, stepExpectedResultMessagePreparedForDiff);
	}

	@Override
	public void messageError(String description, String messageError) {
		writer.writeMessageBox(LarvaLogLevel.WRONG_PIPELINE_MESSAGES, "message container", description, "messagebox", messageError);
	}

	private void writeHtml(String html) {
		writeHtml(html, false);
	}

	private void writeHtml(String html, boolean scroll) {
		writer.writeHtml(null, html, scroll);
	}

	private void writeHtml(LarvaLogLevel logLevel, String html, boolean scroll) {
		writer.writeHtml(logLevel, html, scroll);
	}

	private void printHtmlForm(TestRunStatus testRunStatus) {
		writer.setBufferLogMessages(true);
		writer.setBufferOutputMessages(true);

		writeHtml("<form action=\"index.jsp\" method=\"post\">");

		// scenario root directory drop down
		writeHtml("<table style=\"float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml("<td>Scenarios root directory</td>");
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG);
		writeHtml("<select name=\"scenariosrootdirectory\" onchange=\"updateScenarios()\">");

		String scenariosRootDirectory = config.getActiveScenariosDirectory();
		testRunStatus.getScenarioDirectories().forEach((description, directory) -> {
			String option = "<option value=\"" + XmlEncodingUtils.encodeChars(directory) + "\"";
			if (scenariosRootDirectory.equals(directory)) {
				option = option + " selected";
			}
			option = option + ">" + XmlEncodingUtils.encodeChars(description) + "</option>";
			writeHtml(option);
		});

		writeHtml("</select>");
		writeHtml(TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TABLE_CLOSING_TAG);

		// wait before cleanup timeout box
		writeIntOptionBox("Wait before clean up (ms)", "waitbeforecleanup", "Time to wait before scenario cleanup", config.getWaitBeforeCleanup());

		// timeout box
		writeIntOptionBox("Default timeout (ms)", "timeout", "Global timeout for larva scenarios.", config.getTimeout());

		// log level dropdown
		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>");
		writeHtml("<table style=\"float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml("<td>Log level</td>");
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG);
		writeHtml("<select name=\"loglevel\">");
		for (LarvaLogLevel level : LarvaLogLevel.values()) {
			String option = "<option value=\"" + XmlEncodingUtils.encodeChars(level.getName()) + "\"";
			if (config.getLogLevel() == level) {
				option = option + " selected";
			}
			option = option + ">" + XmlEncodingUtils.encodeChars(level.getName()) + "</option>";
			writeHtml(option);
		}
		writeHtml("</select>");
		writeHtml(TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TABLE_CLOSING_TAG);

		// Auto scroll checkbox
		writeCheckboxOptionBox("Auto scroll", "autoscroll", config.isAutoScroll());

		// Multithreaded checkbox
		writeCheckboxOptionBox("Multi Threaded (experimental)", "multithreaded", config.isMultiThreaded());

		// Scenario(s)
		writeHtml("<table style=\"clear:both;float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml("<td>Scenario(s)</td>");
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG);
		writeHtml("<select name=\"execute\">");
		log.debug("Fill execute select box");
		Set<String> addedDirectories = new HashSet<>();
		testRunStatus.getAllScenarios().forEach((scenarioId, scenario) -> {
			String scenarioDirectory = scenario.getScenarioFile().getParentFile().getAbsolutePath() + File.separator;
			log.debug("Add parent directories of [{}]", scenarioDirectory);
			int i;
			String scenarioDirectoryCanonicalPath;
			String scenariosRootDirectoryCanonicalPath;
			try {
				scenarioDirectoryCanonicalPath = new File(scenarioDirectory).getCanonicalPath();
				scenariosRootDirectoryCanonicalPath = new File(scenariosRootDirectory).getCanonicalPath();
			} catch (IOException e) {
				scenarioDirectoryCanonicalPath = scenarioDirectory;
				scenariosRootDirectoryCanonicalPath = scenariosRootDirectory;
				writer.errorMessage("Could not get canonical path: " + e.getMessage(), e);
			}
			if (scenarioDirectoryCanonicalPath.startsWith(scenariosRootDirectoryCanonicalPath)) {
				i = scenariosRootDirectory.length() - 1;
				String paramExecute = config.getExecute();
				while (i != -1) {
					String longName = scenarioDirectory.substring(0, i + 1);
					log.debug("longName: [{}]", longName);
					if (!addedDirectories.contains(longName)) {
						String shortName = normalizeScenarioName(scenarioDirectory.substring(scenariosRootDirectory.length() - 1, i + 1));
						String option = "<option value=\"" + XmlEncodingUtils.encodeChars(longName) + "\"";
						log.debug("paramExecute: [{}]", paramExecute);
						if (paramExecute != null && paramExecute.equals(longName)) {
							option = option + " selected";
						}
						option = option + ">" + XmlEncodingUtils.encodeChars(shortName) + "</option>";
						writeHtml(option);
						addedDirectories.add(longName);
					}
					i = scenarioDirectory.indexOf(File.separator, i + 1);
				}
				String longName = scenario.getLongName();
				String shortName = scenario.getName();
				log.debug("shortName: [{}]", shortName);
				String option = "<option value=\"" + XmlEncodingUtils.encodeChars(longName) + "\"";
				if (paramExecute != null && paramExecute.equals(longName)) {
					option = option + " selected";
				}
				option = option + ">" + normalizeScenarioName(shortName + " - " + scenario.getDescription()) + "</option>";
				writeHtml(option);
			}
		});
		writeHtml("</select>");
		writeHtml(TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TABLE_CLOSING_TAG);

		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>");
		// submit button
		writeHtml("<table style=\"float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml("<td>&nbsp;</td>");
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml("<td align=\"right\">");
		writeHtml("<input type=\"submit\" name=\"submit\" value=\"start\" id=\"submit\"/>");
		writeHtml(TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TABLE_CLOSING_TAG);

		writeHtml("</form>");
		writeHtml("<br clear=\"all\"/>");

		writer.setBufferOutputMessages(false);
		writer.setBufferLogMessages(false);
		writer.flush();
	}

	private String normalizeScenarioName(String scenarioName) {
		return XmlEncodingUtils.encodeChars(StringUtils.prependIfMissing(FilenameUtils.normalize(scenarioName, true), "/"));
	}

	private void writeCheckboxOptionBox(String description, String fieldName, boolean isChecked) {
		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>");
		writeHtml("<table style=\"float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG + description + TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG);
		writeHtml("<input type=\"checkbox\" name=\"" + fieldName + "\" value=\"true\"");
		if (isChecked) {
			writeHtml(" checked");
		}
		writeHtml("/>");
		writeHtml(TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TABLE_CLOSING_TAG);
	}

	private void writeIntOptionBox(String description, String fieldName, String title, int value) {
		// Use a span to make IE put table on next line with a smaller window width
		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>");
		writeHtml("<table style=\"float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG + description + TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG);
		writeHtml("<input type=\"text\" name=\"" + fieldName + "\" value=\"" + value + "\" title=\"" + title + "\"/>");
		writeHtml(TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TABLE_CLOSING_TAG);
	}
}
