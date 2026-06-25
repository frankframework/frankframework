/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021, 2023-2026 WeAreFrank!

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
package org.frankframework.parameters;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.frankframework.core.ParameterException;
import org.frankframework.stream.Message;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.XmlUtils;

/**
 *
 *
 * @author John Dekker
 */
public class ParameterValue {
	private static final Logger LOG = LogManager.getLogger(ParameterValue.class);

	private final @Nullable Object value;
	private @NonNull IParameter definition;

	protected ParameterValue(@NonNull IParameter type, @Nullable Object value) {
		this.definition = type;
		this.value = value;
	}

	/**
	 * Returns the description of the IParameter
	 */
	public @NonNull IParameter getDefinition() {
		return definition;
	}

	/**
	 * Returns the name of the IParameter
	 */
	public String getName() {
		return definition.getName();
	}

	/**
	 * Returns the value of the IParameter
	 */
	public @Nullable Object getValue() {
		return value;
	}
	public @NonNull Message asMessage() {
		if (value instanceof Message message) return message;
		return Message.asMessage(value);
	}

	public void setDefinition(@NonNull IParameter parameterDef) {
		this.definition = parameterDef;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a boolean
	 */
	public boolean asBooleanValue(boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof Boolean b) {
			return b;
		}

		String valueAsString = Objects.requireNonNull(valueAsString(), "Value as string cannot be null here").trim();
		return "true".equalsIgnoreCase(valueAsString) || "!false".equalsIgnoreCase(valueAsString);
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to an int
	 */
	public int asIntegerValue(int defaultValue) {
		if (value instanceof Integer i) {
			return i;
		}

		String valueAsString = valueAsString();
		if (valueAsString == null) {
			return defaultValue;
		}
		return Integer.parseInt(valueAsString.trim());
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a long
	 */
	public long asLongValue(long defaultValue) {
		if (value instanceof Long l) {
			return l;
		}
		String valueAsString = valueAsString();
		if (valueAsString == null) {
			return defaultValue;
		}
		return Long.parseLong(valueAsString.trim());
	}

	/**
	 * @return convert the value to a string
	 */
	public @Nullable String asStringValue() {
		return asStringValue(null);
	}
	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a string
	 */
	public @Nullable String asStringValue(@Nullable String defaultValue) {
		return value != null ? valueAsString() : defaultValue;
	}

	private @Nullable String valueAsString() {
		if (value == null) {
			return null;
		}
		if (value instanceof Message message) {
			try {
				return message.asString();
			} catch (IOException e) {
				throw new IllegalStateException("cannot open stream", e);
			}
		}

		if (getDefinition() instanceof DateParameter dateParameter && value instanceof Date date) {
			final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateParameter.getFormatString()).withZone(ZoneId.systemDefault());
			return dtf.format(date.toInstant());
		}

		return value.toString();
	}

	public @NonNull List<Node> asCollection() throws ParameterException {
		if (value == null) {
			return List.of();
		}
		try {
			LOG.debug("rendering IParameter [{}] value [{}] as Collection", ()->getDefinition().getName(), ()->value);
			Element holder = XmlUtils.buildElement("<root>"+value+"</root>");
			return XmlUtils.getChildTags(holder, "*");
		} catch (DomBuilderException e) {
			throw new ParameterException(getDefinition().getName(), "IParameter ["+getDefinition().getName()+"] cannot create Collection from ["+value+"]", e);
		}
	}
}
