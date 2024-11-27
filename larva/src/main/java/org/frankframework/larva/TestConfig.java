/*
   Copyright 2024 WeAreFrank!

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

import java.io.StringWriter;
import java.io.Writer;

import lombok.Getter;
import lombok.Setter;

public class TestConfig {
	private @Getter @Setter boolean silent = false;
	private @Getter @Setter int timeout;
	private @Getter @Setter Writer out;
	private @Getter @Setter Writer silentOut = null;
	private @Getter @Setter StringWriter htmlBuffer = new StringWriter();
	private @Getter @Setter StringWriter logBuffer = new StringWriter();
	private @Getter @Setter LarvaLogLevel logLevel = LarvaLogLevel.WRONG_PIPELINE_MESSAGES;

	private @Getter @Setter boolean autoScroll = true;
	private @Getter @Setter boolean useHtmlBuffer = false;
	private @Getter @Setter boolean useLogBuffer = true;
	private @Getter @Setter boolean multiThreaded = false;

	private @Getter @Setter int messageCounter = 0;
	private @Getter @Setter int scenarioCounter = 1;

	public void incrementMessageCounter() {
		messageCounter++;
	}

	public void incrementScenarioCounter() {
		scenarioCounter++;
	}

	public void flushWriters() {
		try {
			if (out != null) {
				if (multiThreaded) {
					synchronized (out) {
						out.flush();
					}
				} else {
					out.flush();
				}
			}
			if (silentOut != null) {
				silentOut.flush();
			}
		} catch (Exception ignored) {
		}
	}

	public void writeSilent(String message) {
		if (!silent) {
			return;
		}
		if (silentOut != null) {
			try {
				if (multiThreaded) {
					synchronized (silentOut) {
						silentOut.write(message);
					}
				} else silentOut.write(message);
			} catch (Exception ignored) {
			}
		}
	}

}
