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
package nl.nn.adapterframework.parameters;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;

/**
 *
 *
 * @author John Dekker
 */
public class ParameterValue {
	private static final Logger LOG = LogManager.getLogger(ParameterValue.class);

	private Object value;
	private Parameter definition;

	protected ParameterValue(Parameter type, Object value) {
		this.definition = type;
		this.value = value;
	}

	/**
	 * Returns the description of the parameter
	 */
	public Parameter getDefinition() {
		return definition;
	}

	/**
	 * Returns the name of the parameter
	 */
	public String getName() {
		return definition.getName();
	}

	/**
	 * Returns the value of the parameter
	 */
	public Object getValue() {
		return value;
	}
	public Message asMessage() {
		return Message.asMessage(value);
	}

	public void setDefinition(Parameter parameterDef) {
		this.definition = parameterDef;
	}

	/**
	 * Sets value for the parameter
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a boolean
	 */
	public boolean asBooleanValue(boolean defaultValue) {
		return value != null ? Boolean.parseBoolean(valueAsString()) : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a byte
	 */
	public byte asByteValue(byte defaultValue) {
		return value != null ? Byte.parseByte(valueAsString()) : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a double
	 */
	public double asDoubleValue(double defaultValue) {
		return value != null ? Double.parseDouble(valueAsString()) : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to an int
	 */
	public int asIntegerValue(int defaultValue) {
		return value != null ? Integer.parseInt(valueAsString()) : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a long
	 */
	public long asLongValue(long defaultValue) {
		return value != null ? Long.parseLong(valueAsString()) : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a float
	 */
	public float asFloatValue(float defaultValue) {
		return value != null ? Float.parseFloat(valueAsString()) : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a short
	 */
	public short asShortValue(short defaultValue) {
		return value != null ? Short.parseShort(valueAsString()) : defaultValue;
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
		if (value instanceof Message) {
			Message message = (Message)value;
			try {
				return message.asString();
			} catch (IOException e) {
				throw new IllegalStateException("cannot open stream", e);
			}
		}
		return value.toString();
	}

	public Collection<Node> asCollection() throws ParameterException {
		if (value == null) {
			return Collections.emptyList();
		}
		try {
			LOG.debug("rendering Parameter [{}] value [{}] as Collection", ()->getDefinition().getName(), ()->value);
			Element holder = XmlUtils.buildElement("<root>"+value+"</root>");
			return XmlUtils.getChildTags(holder, "*");
		} catch (DomBuilderException e) {
			throw new ParameterException("Parameter ["+getDefinition().getName()+"] cannot create Collection from ["+value+"]", e);
		}
	}
}
