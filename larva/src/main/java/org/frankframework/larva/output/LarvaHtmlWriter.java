package org.frankframework.larva.output;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.frankframework.larva.LarvaHtmlConfig;
import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.util.XmlEncodingUtils;

public class LarvaHtmlWriter extends LarvaWriter {

	/**
	 * Counter to make unique IDs for the HTML message boxes
	 */
	private final AtomicInteger messageCounter = new AtomicInteger();

	public LarvaHtmlWriter(LarvaHtmlConfig larvaHtmlConfig, Writer writer) {
		super(larvaHtmlConfig, writer);
	}

	public LarvaHtmlWriter(LarvaHtmlConfig larvaHtmlConfig, OutputStream out) {
		super(larvaHtmlConfig, out);
	}

	public void writeHtml(@Nullable LarvaLogLevel logLevel, @Nonnull String html, boolean scroll) {
		super.doWriteMessage(logLevel, false, html + "\n");
		if (scroll && ((LarvaHtmlConfig)getLarvaConfig()).isAutoScroll()) {
			super.doWriteMessage(null, false, "<script type=\"text/javascript\"><!--\nscrollToBottom();\n--></script>\n");
		}
	}

	@Override
	protected void doWriteMessage(@Nullable LarvaLogLevel logLevel, boolean isLogMessage, @Nonnull String message) {
		super.doWriteMessage(logLevel, isLogMessage, encodeForHtml(message) + "<br/>");
	}

	@Nonnull
	public static String encodeForHtml(@Nonnull String message) {
		return XmlEncodingUtils.encodeChars(XmlEncodingUtils.replaceNonValidXmlCharacters(message));
	}

	@Override
	public void errorMessage(@Nonnull String message) {
		writeHtml(LarvaLogLevel.ERROR, "<h1 class='error'>" + encodeForHtml(message) + "</h1>", true);
	}

	@Override
	public void errorMessage(@Nonnull String message, @Nonnull Throwable t) {
		errorMessage(message);
		writeHtml(LarvaLogLevel.ERROR, "<ol>", false);
		writeExceptionMessagesRecursive(t);
		writeHtml(LarvaLogLevel.ERROR, "</ol>", false);

		// Write only the toplevel exception stacktrace, it will contain all relevant nested traces
		writeException(t);
	}

	private void writeExceptionMessagesRecursive(@Nullable Throwable t) {
		if (t == null) return;
		writeHtml(LarvaLogLevel.ERROR, "<li>" + encodeForHtml(t.getMessage()) + "</li>", false);
		writeExceptionMessagesRecursive(t.getCause());
	}

	private void writeException(Throwable t) {
		// Write stacktrace into a textarea
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		t.printStackTrace(printWriter);
		printWriter.close();
		String id = "messagebox";
		writeMessageBox(LarvaLogLevel.ERROR, "container", "Stack trace", id, stringWriter.toString());
	}

	public int getNextMessageNr() {
		return messageCounter.incrementAndGet();
	}

	public void writeMessageBox(LarvaLogLevel logLevel, String cssClass, String header, String inputIdPrefix, String message) {
		if (!shouldWriteLevel(logLevel)) {
			return;
		}
		String inputId = inputIdPrefix + getNextMessageNr();
		String template = """
				<div class='%s'>
				  %s
				  <h5>%s:</h5>
				  <textarea cols='100' rows='10' id='%s'>%s</textarea>
				</div>
				""";
		writeHtml(logLevel, template.formatted(cssClass,
				writeCommands(inputId, true, null), encodeForHtml(header), inputId,
				encodeForHtml(message)), true);
	}

	public void writeStepMessageBox(LarvaLogLevel logLevel, String cssClass, String stepName, String header, String inputIdPrefix, String message) {
		if (!shouldWriteLevel(logLevel)) {
			return;
		}
		String inputId = inputIdPrefix + getNextMessageNr();
		String template = """
				<div class='%s'>
				  <h4>Step '%s'</h4>
				  %s
				  <h5>%s:</h5>
				  <textarea cols='100' rows='10' id='%s'>%s</textarea>
				</div>
				""";
		writeHtml(logLevel, template.formatted(cssClass, encodeForHtml(stepName),
				writeCommands(inputId, true, null), encodeForHtml(header), inputId,
				encodeForHtml(message)), true);
	}

	public void writeStepMessageWithDiffBox(LarvaLogLevel logLevel, String cssClass, String stepName, String stepOutputFilename, String inputIdPrefix, String headerExtra, String description, String actualMessage, String expectedMessage) {
		if (!shouldWriteLevel(logLevel)) {
			return;
		}
		String formName = inputIdPrefix + getNextMessageNr() + "Wpm";
		String resultBoxId = formName + "ResultBox";
		String expectedBoxId = formName + "ExpectedBox";
		String diffBoxId = formName + "DiffBox";

		// For iehack in form, see: // http://stackoverflow.com/questions/153527/setting-the-character-encoding-in-form-submit-for-internet-explorer
		String template = """
				<div class='%s'>
				  <form name='%s' method='post' action='saveResultToFile.jsp' target='saveResultWindow' accept-charset='UTF-8'>
				    <input type='hidden' name='iehack' value='&#9760;' />
				    <h4>Step '%s'</h4>
				    <hr/>
				    <div class='resultContainer'>
				      %s
				      <h5>Result (%s):</h5>
				      <textarea name='resultBox' id='%s'>%s</textarea>
				    </div>
				    <div class='expectedContainer'>
				      %s
				      <input type='hidden' name='expectedFileName' value='%s' />
				      <input type='hidden' name='cmd' />
				      <h5>Expected (%s):</h5>
				      <textarea name='expectedBox' id='%s'>%s</textarea>
				    </div>
				    <hr/>
				    <div class='differenceContainer'>
				      %s
				      <h5>Differences:</h5>
				      <pre id='%s' class='diffBox'></pre>
				    </div>
				    <h5>Difference description:</h5>
				    <p class='diffMessage'>%s</p>
				  </form>
				</div>
				""";
		String btn1 = "<a class=\"['" + resultBoxId + "','" + expectedBoxId + "']|indentCompare|" + diffBoxId + "\" href=\"javascript:void(0)\">compare</a>";
		String btn2 = "<a href='javascript:void(0);' class='" + formName + "|indentWindiff'>windiff</a>";
		writeHtml(logLevel, template.formatted(cssClass, formName, encodeForHtml(stepName),
				writeCommands(resultBoxId, true, "<a href='javascript:void(0);' class='" + formName + "|saveResults'>save</a>"),
				encodeForHtml(headerExtra), resultBoxId, encodeForHtml(actualMessage),
				writeCommands(expectedBoxId, true, null),
				stepOutputFilename,
				encodeForHtml(headerExtra), expectedBoxId, encodeForHtml(expectedMessage),
				writeCommands(diffBoxId, false, btn1 + btn2),
				diffBoxId, encodeForHtml(description)
		), true);
	}

	@Override
	public void warningMessage(String message) {
		writeHtml(LarvaLogLevel.ERROR, "<h2 class='warning'>" + encodeForHtml(message) + "</h2>", true);
	}

	@Override
	public void infoMessage(String message) {
		writeHtml(LarvaLogLevel.ERROR, "<br/><h2 class='info'>" + encodeForHtml(message) + "</h2>", true);
	}

	public static String writeCommands(String target, boolean textArea, String customCommand) {
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
}
