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
package org.frankframework.larva;

import java.time.Instant;

import lombok.Getter;

public class LarvaMessage {
	private final @Getter LarvaLogLevel logLevel;
	private final @Getter String message;
	private final @Getter Exception exception;
	private final @Getter Instant timestamp = Instant.now();

	public LarvaMessage(LarvaLogLevel logLevel, String message) {
		this.logLevel = logLevel;
		this.message = message;
		this.exception = null;
	}

	public LarvaMessage(LarvaLogLevel logLevel, String message, Exception exception) {
		this.logLevel = logLevel;
		this.message = message;
		this.exception = exception;
	}
}
