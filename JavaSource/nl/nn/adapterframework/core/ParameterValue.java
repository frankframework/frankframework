/*
 * $Log: ParameterValue.java,v $
 * Revision 1.1  2004-05-21 07:58:47  a1909356#db2admin
 * Moved PipeParameter to core
 *
 */
package nl.nn.adapterframework.core;

/**
 * 
 * 
 * @author John Dekker
 * @version Id
 */
public class ParameterValue {
	private Object value;
	private Parameter type;
	
	ParameterValue(Parameter type, Object value) {
		this.type = type;
		this.value = value;
	}
	
	/**
	 * @return the type description of the parameter
	 */
	public Parameter getType() {
		return type;
	}

	/**
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @param parameterType 
	 */
	public void setType(Parameter parameterType) {
		this.type = parameterType;
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
}
