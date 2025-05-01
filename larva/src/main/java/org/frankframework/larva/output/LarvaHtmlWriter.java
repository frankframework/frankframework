package org.frankframework.larva.output;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.Getter;

import org.frankframework.larva.LarvaHtmlConfig;
import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.util.XmlEncodingUtils;

public class LarvaHtmlWriter extends LarvaWriter {

	/**
	 * Counter to make unique IDs for the HTML message boxes
	 */
	private final @Getter AtomicInteger messageCounter = new AtomicInteger();

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
		int messageNr = messageCounter.incrementAndGet();
		writeHtml(LarvaLogLevel.ERROR, "<div class='container'>", false);
		writeHtml(LarvaLogLevel.ERROR, writeCommands("messagebox" + messageNr, true, null), false);
		writeHtml(LarvaLogLevel.ERROR, "<h5>Stack trace:</h5>", false);
		writeHtml(LarvaLogLevel.ERROR, "<textarea cols='100' rows='10' id='messagebox" + messageNr + "'>" + XmlEncodingUtils.encodeChars(XmlEncodingUtils.replaceNonValidXmlCharacters(stringWriter.toString())) + "</textarea>", false);
		writeHtml(LarvaLogLevel.ERROR, "</div>", true);
	}

	public void writeMessageBox(LarvaLogLevel logLevel, String cssClass, String header, String inputId, String message) {
		if (!getLarvaConfig().getLogLevel().shouldLog(logLevel)) {
			return;
		}
		writeHtml(logLevel, "<div class='"+cssClass+"'>", false);
		writeHtml(logLevel, writeCommands(inputId, true, null), false);
		writeHtml(logLevel, "<h5>"+header+":</h5>", false);
		writeHtml(logLevel, "<textarea cols='100' rows='10' id='" + inputId + "'>" + XmlEncodingUtils.encodeChars(XmlEncodingUtils.replaceNonValidXmlCharacters(message)) + "</textarea>", false);
		writeHtml(logLevel, "</div>", true);
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
