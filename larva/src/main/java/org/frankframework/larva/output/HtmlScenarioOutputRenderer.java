package org.frankframework.larva.output;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.frankframework.larva.LarvaHtmlConfig;
import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.larva.LarvaTool;
import org.frankframework.util.XmlEncodingUtils;

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
	public void startTestSuiteExecution() {
		printHtmlForm();
	}

	@Override
	public void endTestSuiteExecution() {
		writeHtml("<br/><br/>");
		printHtmlForm();
		writeHtml("",  true);
		writer.flush();
	}

	@Override
	public void executionStatistics(
			@Nullable String scenariosTotalMessage,
			@Nullable String scenariosPassedMessage,
			@Nullable String scenariosFailedMessage,
			@Nullable String scenariosAutosavedMessage, int scenariosTotal, int scenariosPassed, int scenariosFailed, int scenariosAutosaved) {

		if (scenariosPassedMessage != null) {
			writeHtml(LarvaLogLevel.TOTALS, "<div class='passed'>" + LarvaHtmlWriter.encodeForHtml(scenariosPassedMessage) + "</div>", true);
		}
		if (scenariosFailedMessage != null) {
			writeHtml(LarvaLogLevel.TOTALS, "<div class='failed'>" + LarvaHtmlWriter.encodeForHtml(scenariosFailedMessage) + "</div>", true);
		}
		if (scenariosAutosavedMessage != null) {
			writeHtml(LarvaLogLevel.TOTALS, "<div class='autosaved'>" + LarvaHtmlWriter.encodeForHtml(scenariosAutosavedMessage) + "</div>", true);
		}
		if (scenariosTotalMessage != null) {
			writeHtml(LarvaLogLevel.TOTALS, "<div class='total'>" + LarvaHtmlWriter.encodeForHtml(scenariosTotalMessage) + "</div>", true);
		}
	}

	@Override
	public void startScenario(String scenarioName) {
		evenStep = false;

		writer.setBufferOutputMessages(true);
		if (!shouldLog(LarvaLogLevel.SCENARIO_PASSED_FAILED)) {
			return;
		}
		writeHtml(LarvaLogLevel.SCENARIO_PASSED_FAILED, "<br/><br/><div class='scenario'>", false);
	}

	@Override
	public void finishScenario(String scenarioName, int scenarioResult, String scenarioResultMessage) {

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
		writeHtml("</div>", true);
		writer.setBufferOutputMessages(false);
	}

	@Override
	public void startStep(String stepName) {
		if (evenStep) {
			writeHtml("<div class='even'>");
		} else {
			writeHtml("<div class='odd'>");
		}
		evenStep = !evenStep;
		writer.debugMessage("Execute step '" + stepName + "'");
	}

	@Override
	public void finishStep(String stepName, int stepResult, String stepResultMessage) {
		if (shouldLog(LarvaLogLevel.STEP_PASSED_FAILED)) {
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

			writeHtml(outputMessage.toString());
		}

		writeHtml("</div>");
	}

	@Override
	public void stepMessageSuccess(String stepName, String description, String stepResultMessage, String stepResultMessagePreparedForDiff) {

	}

	@Override
	public void stepMessageFailed(String stepName, String description, String stepExpectedResultMessage, String stepExpectedResultMessagePreparedForDiff, String stepActualResultMessage, String stepActualResultMessagePreparedForDiff) {

	}

	private boolean shouldLog(LarvaLogLevel logLevel) {
		return config.getLogLevel().shouldLog(logLevel);
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

	private void printHtmlForm() {
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
		config.getScenarioDirectories().forEach((directory, description) -> {
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

		// Use a span to make IE put table on next line with a smaller window width
		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>");
		writeHtml("<table style=\"float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml("<td>Wait before clean up (ms)</td>");
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG);
		writeHtml("<input type=\"text\" name=\"waitbeforecleanup\" value=\"" + config.getWaitBeforeCleanup() + "\"/>");
		writeHtml(TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TABLE_CLOSING_TAG);

		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>");
		// timeout box
		writeHtml("<table style=\"float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml("<td>Default timeout (ms)</td>");
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG);
		writeHtml("<input type=\"text\" name=\"timeout\" value=\"" + config.getTimeout() + "\" title=\"Global timeout for larva scenarios.\"/>");
		writeHtml(TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TABLE_CLOSING_TAG);

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
		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>");
		writeHtml("<table style=\"float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml("<td>Auto scroll</td>");
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG);
		writeHtml("<input type=\"checkbox\" name=\"autoscroll\" value=\"true\"");
		if (config.isAutoScroll()) {
			writeHtml(" checked");
		}
		writeHtml("/>");
		writeHtml(TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TABLE_CLOSING_TAG);

		// Multithreaded checkbox
		writeHtml("<span style=\"float: left; font-size: 10pt; width: 0px\">&nbsp; &nbsp; &nbsp;</span>");
		writeHtml("<table style=\"float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml("<td>Multi Threaded (experimental)</td>");
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG);
		writeHtml("<input type=\"checkbox\" name=\"multithreaded\" value=\"true\"");
		if (config.isMultiThreaded()) {
			writeHtml(" checked");
		}
		writeHtml("/>");
		writeHtml(TD_CLOSING_TAG);
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TABLE_CLOSING_TAG);

		// Scenario(s)
		writeHtml("<table style=\"clear:both;float:left;height:50px\">");
		writeHtml(TR_STARTING_TAG);
		writeHtml("<td>Scenario(s)</td>");
		writeHtml(TR_CLOSING_TAG);
		writeHtml(TR_STARTING_TAG);
		writeHtml(TD_STARTING_TAG);
		writeHtml("<select name=\"execute\">");
		writer.debugMessage("Fill execute select box.");
		Set<String> addedDirectories = new HashSet<>();
		config.getScenarioFiles().forEach((scenarioFile, description) -> {
			String scenarioDirectory = scenarioFile.getParentFile().getAbsolutePath() + File.separator;
			writer.debugMessage("Add parent directories of '" + scenarioDirectory + "'");
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
					writer.debugMessage("longName: '" + longName + "'");
					if (!addedDirectories.contains(longName)) {
						String shortName = scenarioDirectory.substring(scenariosRootDirectory.length() - 1, i + 1);
						String option = "<option value=\"" + XmlEncodingUtils.encodeChars(longName) + "\"";
						writer.debugMessage("paramExecute: '" + paramExecute + "'");
						if (paramExecute != null && paramExecute.equals(longName)) {
							option = option + " selected";
						}
						option = option + ">" + XmlEncodingUtils.encodeChars(shortName) + "</option>";
						writeHtml(option);
						addedDirectories.add(longName);
					}
					i = scenarioDirectory.indexOf(File.separator, i + 1);
				}
				String longName = scenarioFile.getAbsolutePath();
				String shortName = longName.substring(scenariosRootDirectory.length() - 1, longName.length() - ".properties".length());
				writer.debugMessage("shortName: '" + shortName + "'");
				String option = "<option value=\"" + XmlEncodingUtils.encodeChars(longName) + "\"";
				if (paramExecute != null && paramExecute.equals(longName)) {
					option = option + " selected";
				}
				option = option + ">" + XmlEncodingUtils.encodeChars(shortName + " - " + description) + "</option>";
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

}
