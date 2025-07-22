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
package org.frankframework.lifecycle.events;

import java.io.Serial;

import org.frankframework.core.Adapter;
import org.frankframework.util.ClassUtils;

public class AdapterMessageEvent extends MessageEvent<Adapter> {

	@Serial
	private static final long serialVersionUID = 1L;

	public AdapterMessageEvent(Adapter source, String message) {
		this(source, source, message, MessageEventLevel.INFO);
	}

	public AdapterMessageEvent(Adapter source, Object namedObject, String message) {
		this(source, namedObject, message, MessageEventLevel.INFO);
	}

	public AdapterMessageEvent(Adapter source, String message, MessageEventLevel level) {
		this(source, source, message, level);
	}

	public AdapterMessageEvent(Adapter source, Object namedObject, String message, MessageEventLevel level) {
		super(source, ClassUtils.nameOf(namedObject) + " " + message, level, null);
	}

	public AdapterMessageEvent(Adapter source, String message, Throwable e) {
		this(source, source, message, e);
	}

	public AdapterMessageEvent(Adapter source, Object namedObject, String message, Throwable e) {
		super(source, ClassUtils.nameOf(namedObject) + " " + message, MessageEventLevel.ERROR, e);
	}

	@Override
	protected String getMessagePrefix() {
		return "";
	}
}
