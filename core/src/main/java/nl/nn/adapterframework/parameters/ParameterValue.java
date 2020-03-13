/*
   Copyright 2013 Nationale-Nederlanden

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

import java.util.Collection;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 
 * 
 * @author John Dekker
 */
public class ParameterValue {
	protected Logger log = LogUtil.getLogger(this);

	private Object value;
	private Parameter definition;
	
	ParameterValue(Parameter type, Object value) {
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
		return value != null ? Boolean.valueOf(value.toString()).booleanValue() : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a byte
	 */
	public byte asByteValue(byte defaultValue) {
		return value != null ? Byte.valueOf(value.toString()).byteValue() : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a double
	 */
	public double asDoubleValue(double defaultValue) {
		return value != null ? Double.valueOf(value.toString()).doubleValue() : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to an int
	 */
	public int asIntegerValue(int defaultValue) {
		return value != null ? Integer.valueOf(value.toString()).intValue() : defaultValue;
	}
	
	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a long
	 */
	public long asLongValue(long defaultValue) {
		return value != null ? Long.valueOf(value.toString()).longValue() : defaultValue;
	}
	
	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a float
	 */
	public float asFloatValue(float defaultValue) {
		return value != null ? Float.valueOf(value.toString()).floatValue() : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a short
	 */
	public short asShortValue(short defaultValue) {
		return value != null ? Short.valueOf(value.toString()).shortValue() : defaultValue;
	}

	/**
	 * @param defaultValue returned if value is null
	 * @return convert the value to a string
	 */
	public String asStringValue(String defaultValue) {
		return value != null ? value.toString() : defaultValue;
	}

	public Collection<Node> asCollection() throws ParameterException {
		if (value == null) {
			return null;
		}
		try {
			log.debug("rendering Parameter ["+getDefinition().getName()+"] value ["+value+"] as Collection");
			Element holder = XmlUtils.buildElement("<root>"+value+"</root>");
			return XmlUtils.getChildTags(holder, "*");
		} catch (DomBuilderException e) {
			throw new ParameterException("Parameter ["+getDefinition().getName()+"] cannot create Collection from ["+value+"]", e);
		}
	}
}
