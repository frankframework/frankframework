/*
 * $Log: ParameterValue.java,v $
 * Revision 1.5  2009-09-07 13:27:17  L190409
 * added nested exception to new exception thrown
 *
 * Revision 1.4  2007/02/12 13:59:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.3  2004/10/14 16:08:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced logging
 *
 * Revision 1.2  2004/10/12 15:08:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added asCollection() method
 *
 * Revision 1.1  2004/10/05 09:52:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved parameter code  to package parameters
 *
 * Revision 1.1  2004/05/21 07:58:47  unknown <unknown@ibissource.org>
 * Moved PipeParameter to core
 *
 */
package nl.nn.adapterframework.parameters;

import java.util.Collection;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

/**
 * 
 * 
 * @author John Dekker
 * @version Id
 */
public class ParameterValue {
	public static final String version="$RCSfile: ParameterValue.java,v $ $Revision: 1.5 $ $Date: 2009-09-07 13:27:17 $";
	protected Logger log = LogUtil.getLogger(this);

	private Object value;
	private Parameter definition;
	
	ParameterValue(Parameter type, Object value) {
		this.definition = type;
		this.value = value;
	}
	
	/**
	 * @return the type description of the parameter
	 */
	public Parameter getDefinition() {
		return definition;
	}

	/**
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}

	public void setDefinition(Parameter parameterDef) {
		this.definition = parameterDef;
	}

	/**
	 * @param object
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

	public Collection asCollection() throws ParameterException {
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
