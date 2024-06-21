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

@Getter
@Setter
public class TestConfig {
	private boolean silent = false;
	private int timeout;
	private Writer out;
	private Writer silentOut = null;
	private StringWriter htmlBuffer = new StringWriter();
	private StringWriter logBuffer = new StringWriter();
	private LarvaLogLevel logLevel = LarvaLogLevel.WRONG_PIPELINE_MESSAGES;

	private boolean autoScroll = true;
	private boolean useHtmlBuffer = false;
	private boolean useLogBuffer = true;
	private boolean multiThreaded = true;

	private int messageCounter = 0;
	private int scenarioCounter = 1;

	public void incrementMessageCounter() {
		messageCounter++;
	}

	public void incrementScenarioCounter() {
		scenarioCounter++;
	}

	public void flushWriters() {
		try {
			if (out != null) {
				synchronized (out) {
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
			synchronized (silentOut) {
				try {
					silentOut.write(message);
				} catch (Exception ignored) {
				}
			}
		}
	}

}
