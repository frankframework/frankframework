/*
   Copyright 2024 WeAreFrank!

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

import lombok.Getter;

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
	@ConfigurationWarning("ParameterType NODE is deprecated, use DOMDOC instead")
	@Deprecated(since = "9.0.0", forRemoval = true)
	NODE(XmlParameter.class, true),

	/** Renders XML as a DOM document; similar to <code>node</code>
		with the distinction that there is always a common root node (required for XSLT 2.0) */
	DOMDOC(XmlParameter.class, true),

	/** Converts the result to a Date, by default using formatString <code>yyyy-MM-dd</code>.
	 * When applied as a JDBC parameter, the method setDate() is used */
	DATE(DateParameter.class, true),

	/** Converts the result to a Date, by default using formatString <code>HH:mm:ss</code>.
	 * When applied as a JDBC parameter, the method setTime() is used */
	TIME(DateParameter.class, true),

	/** Converts the result to a Date, by default using formatString <code>yyyy-MM-dd HH:mm:ss</code>.
	 * When applied as a JDBC parameter, the method setTimestamp() is used */
	DATETIME(DateParameter.class, true),

	/** Similar to <code>DATETIME</code>, except for the formatString that is <code>yyyy-MM-dd HH:mm:ss.SSS</code> by default */
	TIMESTAMP(DateParameter.class, true),

	/** Converts the result from a XML formatted dateTime to a Date.
	 * When applied as a JDBC parameter, the method setTimestamp() is used */
	XMLDATETIME(DateParameter.class, true),

	/** Converts the result to a Number, using decimalSeparator and groupingSeparator.
	 * When applied as a JDBC parameter, the method setDouble() is used */
	NUMBER(NumberParameter.class, true),

	/** Converts the result to an Integer */
	INTEGER(NumberParameter.class, true),

	/** Converts the result to a Boolean */
	BOOLEAN(BooleanParameter.class, true),

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

	private final @Getter Class<? extends IParameter> typeClass;
	public final boolean requiresTypeConversion;

	ParameterType() {
		this(false);
	}

	ParameterType(boolean requiresTypeConverion) {
		this(Parameter.class, requiresTypeConverion);
	}

	ParameterType(Class<? extends IParameter> typeClass, boolean requiresTypeConverion) {
		this.requiresTypeConversion = requiresTypeConverion;
		this.typeClass = typeClass;
	}
}
