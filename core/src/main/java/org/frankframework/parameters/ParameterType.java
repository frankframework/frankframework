package org.frankframework.parameters;

import org.frankframework.configuration.ConfigurationWarning;

public enum ParameterType {
	/** Renders the contents of the first node (in combination with xslt or xpath). Please note that
	 * if there are child nodes, only the contents are returned, use <code>XML</code> if the xml tags are required */
	STRING,

	/** Renders an xml-nodeset as an xml-string (in combination with xslt or xpath). This will include the xml tags */
	XML,

	/** Renders the CONTENTS of the first node as a nodeset
	 * that can be used as such when passed as xslt-parameter (only for XSLT 1.0).
	 * Please note that the nodeset may contain multiple nodes, without a common root node.
	 * N.B. The result is the set of children of what you might expect it to be... */
	NODE(true),

	/** Renders XML as a DOM document; similar to <code>node</code>
		with the distinction that there is always a common root node (required for XSLT 2.0) */
	DOMDOC(true),

	/** Converts the result to a Date, by default using formatString <code>yyyy-MM-dd</code>.
	 * When applied as a JDBC parameter, the method setDate() is used */
	DATE(true),

	/** Converts the result to a Date, by default using formatString <code>HH:mm:ss</code>.
	 * When applied as a JDBC parameter, the method setTime() is used */
	TIME(true),

	/** Converts the result to a Date, by default using formatString <code>yyyy-MM-dd HH:mm:ss</code>.
	 * When applied as a JDBC parameter, the method setTimestamp() is used */
	DATETIME(true),

	/** Similar to <code>DATETIME</code>, except for the formatString that is <code>yyyy-MM-dd HH:mm:ss.SSS</code> by default */
	TIMESTAMP(true),

	/** Converts the result from a XML formatted dateTime to a Date.
	 * When applied as a JDBC parameter, the method setTimestamp() is used */
	XMLDATETIME(true),

	/** Converts the result to a Number, using decimalSeparator and groupingSeparator.
	 * When applied as a JDBC parameter, the method setDouble() is used */
	NUMBER(true),

	/** Converts the result to an Integer */
	INTEGER(true),

	/** Converts the result to a Boolean */
	BOOLEAN(true),

	/** Only applicable as a JDBC parameter, the method setBinaryStream() is used */
	@ConfigurationWarning("use type [BINARY] instead")
	@Deprecated INPUTSTREAM,

	/** Only applicable as a JDBC parameter, the method setBytes() is used */
	@ConfigurationWarning("use type [BINARY] instead")
	@Deprecated BYTES,

	/** Forces the parameter value to be treated as binary data (e.g. when using a SQL BLOB field).
	 * When applied as a JDBC parameter, the method setBinaryStream() or setBytes() is used */
	BINARY,

	/** Forces the parameter value to be treated as character data (e.g. when using a SQL CLOB field).
	 * When applied as a JDBC parameter, the method setCharacterStream() or setString() is used */
	CHARACTER,

	/**
	 * Used for StoredProcedure OUT parameters when the database type is a {@code CURSOR} or {@link java.sql.JDBCType#REF_CURSOR}.
	 * See also {@link org.frankframework.jdbc.StoredProcedureQuerySender}.
	 * <br/>
	 * DEPRECATED: Type LIST can also be used in larva test to Convert a List to an xml-string (&lt;items&gt;&lt;item&gt;...&lt;/item&gt;&lt;item&gt;...&lt;/item&gt;&lt;/items&gt;) */
	LIST,

	/** (Used in larva only) Converts a Map&lt;String, String&gt; object to a xml-string (&lt;items&gt;&lt;item name='...'&gt;...&lt;/item&gt;&lt;item name='...'&gt;...&lt;/item&gt;&lt;/items&gt;) */
	@Deprecated MAP;

	public final boolean requiresTypeConversion;

	private ParameterType() {
		this(false);
	}

	private ParameterType(boolean requiresTypeConverion) {
		this.requiresTypeConversion = requiresTypeConverion;
	}

}