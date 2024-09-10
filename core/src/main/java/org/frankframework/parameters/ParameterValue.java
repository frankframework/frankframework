/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021, 2023 WeAreFrank!

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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	private Object value;
	private IParameter definition;

	protected ParameterValue(IParameter type, Object value) {
		this.definition = type;
		this.value = value;
	}

	/**
	 * Returns the description of the IParameter
	 */
	public IParameter getDefinition() {
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
	public Object getValue() {
		return value;
	}
	public Message asMessage() {
		if (value instanceof Message message) return message;
		return Message.asMessage(value);
	}

	public void setDefinition(IParameter IParameterDef) {
		this.definition = IParameterDef;
	}

	/**
	 * Sets value for the IParameter
	 */
	public void setValue(Object value) {
		this.value = value;
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

		return Boolean.parseBoolean(valueAsString());
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to an int
	 */
	public int asIntegerValue(int defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof Integer i) {
			return i;
		}

		return Integer.parseInt(valueAsString());
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a long
	 */
	public long asLongValue(long defaultValue) {
		return value != null ? Long.parseLong(valueAsString()) : defaultValue;
	}

	/**
	 * @return convert the value to a string
	 */
	public String asStringValue() {
		return asStringValue(null);
	}
	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a string
	 */
	public String asStringValue(String defaultValue) {
		return value != null ? valueAsString() : defaultValue;
	}

	private String valueAsString() {
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

	public Collection<Node> asCollection() throws ParameterException {
		if (value == null) {
			return Collections.emptyList();
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
