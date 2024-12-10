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
package org.frankframework.http.authentication;

import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;

import java.util.HashMap;
import java.util.Map;

public class Log4jLogConsumer extends BaseConsumer<Log4jLogConsumer> {
	
	private final Logger logger;
	private final Map<String, String> mdc;
	private boolean separateOutputStreams;
	private String prefix;

	public Log4jLogConsumer(Logger logger) {
		this(logger, false);
	}

	public Log4jLogConsumer(Logger logger, boolean separateOutputStreams) {
		this.mdc = new HashMap<>();
		this.prefix = "";
		this.logger = logger;
		this.separateOutputStreams = separateOutputStreams;
	}

	public Log4jLogConsumer withPrefix(String prefix) {
		this.prefix = "[" + prefix + "] ";
		return this;
	}

	public Log4jLogConsumer withMdc(String key, String value) {
		this.mdc.put(key, value);
		return this;
	}

	public Log4jLogConsumer withMdc(Map<String, String> mdc) {
		this.mdc.putAll(mdc);
		return this;
	}

	public Log4jLogConsumer withSeparateOutputStreams() {
		this.separateOutputStreams = true;
		return this;
	}

	public void accept(OutputFrame outputFrame) {
		OutputFrame.OutputType outputType = outputFrame.getType();
		String utf8String = outputFrame.getUtf8StringWithoutLineEnding();
		Map<String, String> originalMdc = MDC.getCopyOfContextMap();
		MDC.setContextMap(this.mdc);

		try {
			switch (outputType) {
				case END:
					break;
				case STDOUT:
					if (this.separateOutputStreams) {
						this.logger.info("{}{}", this.prefix.isEmpty() ? "" : this.prefix + ": ", utf8String);
					} else {
						this.logger.info("{}{}: {}", new Object[]{this.prefix, outputType, utf8String});
					}
					break;
				case STDERR:
					if (this.separateOutputStreams) {
						this.logger.error("{}{}", this.prefix.isEmpty() ? "" : this.prefix + ": ", utf8String);
					} else {
						this.logger.info("{}{}: {}", new Object[]{this.prefix, outputType, utf8String});
					}
					break;
				default:
					throw new IllegalArgumentException("Unexpected outputType " + outputType);
			}
		} finally {
			if (originalMdc == null) {
				MDC.clear();
			} else {
				MDC.setContextMap(originalMdc);
			}

		}

	}
}
