/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.lifecycle.events;

import java.io.Serial;

import org.frankframework.configuration.Configuration;

public class ConfigurationMessageEvent extends MessageEvent<Configuration> {

	@Serial
	private static final long serialVersionUID = 1L;

	public ConfigurationMessageEvent(Configuration source, String message) {
		this(source, message, MessageEventLevel.INFO);
	}

	public ConfigurationMessageEvent(Configuration source, String message, MessageEventLevel level) {
		super(source, message, level, null);
	}

	public ConfigurationMessageEvent(Configuration source, String message, Exception e) {
		super(source, message, MessageEventLevel.ERROR, e);
	}

	@Override
	protected String getMessagePrefix() {
		StringBuilder m = new StringBuilder();
		m.append("Configuration [").append(getSource().getName()).append("] ");

		String version = getSource().getVersion();
		if (version != null) {
			m.append("[").append(version).append("] ");
		}

		return m.toString();
	}
}
