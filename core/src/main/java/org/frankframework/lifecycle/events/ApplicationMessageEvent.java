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

import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.ConfigurationUtils;
import org.frankframework.util.ClassUtils;

public class ApplicationMessageEvent extends MessageEvent<ApplicationContext> {

	@Serial
	private static final long serialVersionUID = 1L;

	public ApplicationMessageEvent(ApplicationContext source, String message) {
		this(source, message, MessageEventLevel.INFO);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message, MessageEventLevel level) {
		this(source, message, level, null);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message, Exception e) {
		this(source, message, MessageEventLevel.ERROR, e);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message, MessageEventLevel level, Exception e) {
		super(source, message, level, e);
	}

	@Override
	protected String getMessagePrefix() {
		StringBuilder m = new StringBuilder();
		m.append("Application [").append(getSource().getId()).append("] ");

		String version = ConfigurationUtils.getApplicationVersion();
		if (version != null) {
			m.append("[").append(version).append("] ");
		}
		return m.toString();
	}

	@Override
	protected String getExceptionMessage(Throwable e) {
		return ": (" + ClassUtils.nameOf(e) + ") " + e.getMessage();
	}
}
