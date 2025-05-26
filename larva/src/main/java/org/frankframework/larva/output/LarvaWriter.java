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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.larva.LarvaConfig;
import org.frankframework.larva.LarvaLogLevel;

/**
 * Output specific for Larva. Wraps around an {@link OutputStream}, or a {@link Writer}.
 * <p>
 * Output can be regular output messages, or log-output that should be shown to the user.
 * Both are written to the same output stream,
 * but each can be temporarily written to a buffer instead of the output stream in order not
 * to mix up log messages with regular messages when that would not be appropriate. (This feature
 * is copied from the old Larva code).
 * </p>
 * <p>
 *     Most classes in Larva will need the LarvaWriter in order to write log messages that are used in user-feedback.
 *     Some classes will need access to the LarvaWriter to write regular messages -- mostly these will be
 *     implementations of {@link TestExecutionObserver} that will write user-output about
 *     the status of test execution.
 * </p>
 * <p>
 *     The LarvaWriter can always be access from the {@link org.frankframework.larva.LarvaTool} instance if required.
 * </p>
 */
@Log4j2
public class LarvaWriter {
	private final @Getter LarvaConfig larvaConfig;
	private final Writer writer;

	private @Getter boolean bufferLogMessages = false;
	private @Getter boolean bufferOutputMessages = false;

	// TODO: The buffering is not yet thread-safe
	private final @Getter StringWriter logBuffer = new StringWriter();
	private final @Getter StringWriter outputBuffer = new StringWriter();

	public LarvaWriter(LarvaConfig larvaConfig, Writer writer) {
		this.larvaConfig = larvaConfig;
		this.writer = writer;
	}

	public LarvaWriter(LarvaConfig larvaConfig, OutputStream out) {
		this.larvaConfig = larvaConfig;
		this.writer = new OutputStreamWriter(out);
	}

	public void flush() {
		synchronized (writer) {
			flushBuffer(outputBuffer);
			flushBuffer(logBuffer);
			try {
				writer.flush();
			} catch (IOException e) {
				log.error("Cannot flush writer", e);
			}
		}
	}

	public void setBufferLogMessages(boolean bufferLogMessages) {
		this.bufferLogMessages = bufferLogMessages;
		flushBuffer(logBuffer);
	}

	public void setBufferOutputMessages(boolean bufferOutputMessages) {
		this.bufferOutputMessages = bufferOutputMessages;
		flushBuffer(outputBuffer);
	}

	private void flushBuffer(StringWriter outputBuffer) {
		if (!outputBuffer.getBuffer().isEmpty()) {
			try {
				writer.write(outputBuffer.toString());
			} catch (IOException e) {
				log.error("Cannot write output", e);
			}
			outputBuffer.getBuffer().setLength(0);
		}
	}

	protected Writer getTargetWriter(boolean isLogMessage) {
		if (isLogMessage && bufferLogMessages) {
			return logBuffer;
		} else if (!isLogMessage && bufferOutputMessages) {
			return outputBuffer;
		} else {
			return writer;
		}
	}

	protected void doWriteMessage(LarvaLogLevel logLevel, boolean isLogMessage, String message) {
		if (shouldWriteLevel(logLevel)) {
			Writer targetWriter = getTargetWriter(isLogMessage);
			synchronized (targetWriter) {
				try {
					targetWriter.write(message);
					targetWriter.write(System.lineSeparator());
				} catch (IOException e) {
					log.error("Cannot write output", e);
				}
			}
		}
	}

	public boolean shouldWriteLevel(LarvaLogLevel logLevel) {
		return this.getLarvaConfig().getLogLevel().shouldLog(logLevel);
	}

	public void writeOutputMessage(LarvaLogLevel logLevel, String message) {
		doWriteMessage(logLevel, false, message);
	}

	public void writeLogMessage(LarvaLogLevel logLevel, String message) {
		doWriteMessage(logLevel, true, message);
	}

	public void debugMessage(String message) {
		doWriteMessage(LarvaLogLevel.DEBUG, true, "DEBUG: " + message);
	}

	public void infoMessage(String message) {
		doWriteMessage(LarvaLogLevel.INFO, true, "INFO: " + message);
	}

	public void errorMessage(String message) {
		doWriteMessage(LarvaLogLevel.ERROR, true, "ERROR: " + message);
	}

	public void warningMessage(String message) {
		doWriteMessage(LarvaLogLevel.WARNING, true, "WARNING: " + message);
	}

	public void errorMessage(String message, Throwable t) {
		Writer targetWriter = getTargetWriter(true);
		synchronized (targetWriter) {
			doWriteMessage(LarvaLogLevel.ERROR, true, "ERROR: " + message);
			t.printStackTrace(new PrintWriter(targetWriter));
		}
	}
}
