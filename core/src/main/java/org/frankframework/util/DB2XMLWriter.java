/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package org.frankframework.util;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.logging.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.frankframework.core.SenderException;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.jdbc.StoredProcedureResultWrapper;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.xml.SaxDocumentBuilder;
import org.frankframework.xml.SaxElementBuilder;
import org.frankframework.xml.XmlWriter;

/**
 * Transforms a java.sql.Resultset to a XML stream.
 * Example of a result:
 * <pre>{@code
 * <result>
 *     <fielddefinition>
 *         <field name="FIELDNAME"
 *             type="columnType"
 *             columnDisplaySize=""
 *             precision=""
 *             scale=""
 *             isCurrency=""
 *             columnTypeName=""
 *             columnClassName=""/>
 *         <field ...../>
 *     </fielddefinition>
 *     <rowset>
 *         <row number="1">
 *             <field name="FIELDNAME">value</field>
 *             <field name="FIELDNAME" null="true"></field>
 *             <field name="FIELDNAME">value</field>
 *             <field name="FIELDNAME">value</field>
 *         </row>
 *     </rowset>
 * </result>
 * }</pre>
 * Note: that the fieldname and columntype are always capital case!
 *
 * @author Johan Verrips
 **/
public class DB2XMLWriter {
	protected static Logger log = LogUtil.getLogger(DB2XMLWriter.class);

	private String docname = "result";
	private String recordname = "rowset";
	private String nullValue = "";
	private boolean trimSpaces=true;
	private boolean decompressBlobs=false;
	private boolean getBlobSmart=false;
	private String blobCharset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private static final boolean convertFieldnamesToUppercase = AppConstants.getInstance().getBoolean("jdbc.convertFieldnamesToUppercase", false);

	public static String getFieldType (int type) {
		return JDBCType.valueOf(type).getName();
	}

	/**
	 * Retrieve the Resultset as a well-formed XML string
	 */
	public String getXML(@Nonnull IDbmsSupport dbmsSupport, @Nullable ResultSet rs) {
		return getXML(dbmsSupport, rs, Integer.MAX_VALUE);
	}

	/**
	 * Retrieve the Resultset as a well-formed XML string
	 */
	public String getXML(@Nonnull IDbmsSupport dbmsSupport, @Nullable ResultSet rs, int maxRows) {
		return getXML(dbmsSupport, rs, maxRows, true);
	}

	public String getXML(@Nonnull IDbmsSupport dbmsSupport, @Nullable ResultSet rs, int maxRows, boolean includeFieldDefinition) {
		try {
			XmlWriter xmlWriter = new XmlWriter();
			getXML(dbmsSupport, rs, maxRows, includeFieldDefinition, xmlWriter, true);
			return xmlWriter.toString();
		} catch (SAXException e) {
			log.warn("cannot convert ResultSet to XML", e);
			return buildErrorString(e);
		}
	}

	public void getXML(@Nonnull IDbmsSupport dbmsSupport, @Nullable ResultSet rs, int maxRows, boolean includeFieldDefinition, ContentHandler handler, boolean prettyPrint) throws SAXException {
		// If a negative value is passed, retrieve all rows of each result set
		if (maxRows < 0) {
			maxRows = Integer.MAX_VALUE;
		}

		try (SaxDocumentBuilder root = new SaxDocumentBuilder(docname, handler, prettyPrint)) {
			if (null == rs) {
				return;
			}
			try {
				Statement stmt = rs.getStatement();
				if (stmt != null) {
					JdbcUtil.warningsToXml(stmt.getWarnings(), root);
				}
			} catch (SQLException e1) {
				log.warn("exception obtaining statement warnings", e1);
			}
			processResultSet(dbmsSupport, rs, maxRows, includeFieldDefinition, root);
		}
	}

	public String getXML(@Nonnull IDbmsSupport dbmsSupport, @Nonnull CallableStatement callableStatement, boolean alsoGetResultSets, @Nonnull Map<Integer, IParameter> outputParameters, int maxRows, boolean includeFieldDefinition) {
		try {
			XmlWriter xmlWriter = new XmlWriter();
			getXML(dbmsSupport, callableStatement, alsoGetResultSets, outputParameters, maxRows, includeFieldDefinition, xmlWriter, true);
			return xmlWriter.toString();
		} catch (SAXException e) {
			log.warn("cannot convert CallableStatement to XML", e);
			return buildErrorString(e);
		}
	}

	private static String buildErrorString(Exception e) {
		return "<error>" + XmlEncodingUtils.encodeCharsAndReplaceNonValidXmlCharacters(e.getMessage()) + "</error>";
	}

	private static void addErrorXml(Exception e, SaxElementBuilder parent) throws SAXException {
		SaxElementBuilder errorElement = parent.startElement("error");
		errorElement.addValue(XmlEncodingUtils.encodeCharsAndReplaceNonValidXmlCharacters(e.getMessage()));
		errorElement.endElement();
	}

	public void getXML(@Nonnull IDbmsSupport dbmsSupport, @Nonnull CallableStatement callableStatement, boolean alsoGetResultSets, @Nonnull Map<Integer, IParameter> outputParameters, int maxRows, boolean includeFieldDefinition, @Nonnull ContentHandler handler, boolean prettyPrint) throws SAXException {
		// If a negative value is passed, retrieve all rows of each result set
		if (maxRows < 0) {
			maxRows = Integer.MAX_VALUE;
		}
		try (SaxDocumentBuilder root = new SaxDocumentBuilder("resultset", handler, prettyPrint)) {
			try {
				JdbcUtil.warningsToXml(callableStatement.getWarnings(), root);
			} catch (SQLException e1) {
				log.warn("exception obtaining statement warnings", e1);
			}

			if (alsoGetResultSets) {
				processStatementResults(dbmsSupport, callableStatement, maxRows, includeFieldDefinition, root);
			}

			processOutputParameters(dbmsSupport, callableStatement, outputParameters, maxRows, includeFieldDefinition, root);
		}
	}

	private void processOutputParameters(@Nonnull IDbmsSupport dbmsSupport, @Nonnull CallableStatement callableStatement, @Nonnull Map<Integer, IParameter> outputParameters, int maxRows, boolean includeFieldDefinition, @Nonnull SaxElementBuilder parent) throws SAXException {
		if (outputParameters.isEmpty()) {
			return;
		}
		StoredProcedureResultWrapper resultWrapper;
		ResultSetMetaData metaData;
		try {
			resultWrapper = new StoredProcedureResultWrapper(dbmsSupport, callableStatement, callableStatement.getParameterMetaData(), outputParameters);
			metaData = resultWrapper.getMetaData();
		} catch (SQLException e) {
			log.warn("Error get stored procedure result data", e);
			addErrorXml(e, parent);
			return;
		}
		int index = 1;
		for (Map.Entry<Integer, IParameter> entry : outputParameters.entrySet()) {
			SaxElementBuilder resultElement = parent.startElement(docname);
			int position = entry.getKey();
			IParameter param = entry.getValue();
			resultElement.addAttribute("param", param.getName());
			resultElement.addAttribute("type", param.getType().toString());

			try {
				callableStatement.getObject(position);
				if (callableStatement.wasNull()) {
					resultElement.addAttribute("null", "true");
				} else if (param.getType() == ParameterType.LIST) {
					ResultSet resultSet = callableStatement.getObject(position, ResultSet.class);
					processResultSet(dbmsSupport, resultSet, maxRows, includeFieldDefinition, resultElement);
				} else {
					String value = JdbcUtil.getValue(dbmsSupport, resultWrapper, index, metaData, getBlobCharset(), isDecompressBlobs(), getNullValue(), isTrimSpaces(), isGetBlobSmart(), false);
					resultElement.addValue(value);
				}
			} catch (SQLException | IOException e) {
				log.warn("Error retrieving result value", e);
				addErrorXml(e, resultElement);
			}
			resultElement.endElement();
			++index;
		}
	}

	private void processStatementResults(@Nonnull IDbmsSupport dbmsSupport, @Nonnull Statement statement, int maxRows, boolean includeFieldDefinition, @Nonnull SaxElementBuilder parent) throws SAXException {
		int resultNr = 1;
		try {
			do {
				SaxElementBuilder resultElement = parent.startElement(docname);
				resultElement.addAttribute("resultNr", String.valueOf(resultNr));
				processResultSet(dbmsSupport, statement.getResultSet(), maxRows, includeFieldDefinition, resultElement);
				resultNr++;
				resultElement.endElement();
			} while (statement.getMoreResults());
		} catch (SQLException e) {
			log.warn("Could not get next result-set from statement");
			addErrorXml(e, parent);
		}
	}

	private void processResultSet(IDbmsSupport dbmsSupport, ResultSet rs, int maxRows, boolean includeFieldDefinition, SaxElementBuilder parent) {
		int rowCounter = 0;
		try {
			ResultSetMetaData rsmeta = rs.getMetaData();
			if (includeFieldDefinition) {
				addFieldDefinitions(parent, rsmeta);
			}

			//----------------------------------------
			// Process result rows
			//----------------------------------------

			try (SaxElementBuilder queryresult = parent.startElement(recordname)) {
				while (rs.next() && rowCounter < maxRows) {
					getRowXml(queryresult, dbmsSupport, rs,rowCounter,rsmeta,getBlobCharset(),decompressBlobs,nullValue,trimSpaces, getBlobSmart);
					rowCounter++;
				}
			}
		} catch (Exception e) {
			log.error("Error occurred at row [{}]", rowCounter, e);
		}
	}

	public static void addFieldDefinitions(SaxElementBuilder root, ResultSetMetaData rsmeta) throws SAXException, SQLException {
		try (SaxElementBuilder fields = root.startElement("fielddefinition")) {
			addFieldDefinitionsToContainer(fields, rsmeta);
		}
	}

	private static void addFieldDefinitionsToContainer(SaxElementBuilder fields, ResultSetMetaData rsmeta) throws SAXException, SQLException {
		int nfields = rsmeta.getColumnCount();

		for (int j = 1; j <= nfields; j++) {
			try (SaxElementBuilder field = fields.startElement("field")) {

				String columnName = rsmeta.getColumnName(j);
				if(convertFieldnamesToUppercase)
					columnName = columnName.toUpperCase();
				field.addAttribute("name", columnName);

				//Not every JDBC implementation implements these attributes!
				try {
					field.addAttribute("type", getFieldType(rsmeta.getColumnType(j)));
				} catch (SQLException e) {
					log.debug("Could not determine columnType",e);
				}
				try {
					field.addAttribute("columnDisplaySize", "" + rsmeta.getColumnDisplaySize(j));
				} catch (SQLException e) {
					log.debug("Could not determine columnDisplaySize",e);
				}
				try {
					field.addAttribute("precision", "" + rsmeta.getPrecision(j));
				} catch (SQLException e) {
					log.warn("Could not determine precision",e);
				} catch (NumberFormatException e2) {
					if (log.isDebugEnabled()) log.debug("Could not determine precision: {}", e2.getMessage());
				}
				try {
					field.addAttribute("scale", "" + rsmeta.getScale(j));
				} catch (SQLException e) {
					log.debug("Could not determine scale",e);
				}
				try {
					field.addAttribute("isCurrency", "" + rsmeta.isCurrency(j));
				} catch (SQLException e) {
					log.debug("Could not determine isCurrency",e);
				}
				try {
					String columnTypeName = rsmeta.getColumnTypeName(j);
					if(convertFieldnamesToUppercase)
						columnTypeName = columnTypeName.toUpperCase();
					field.addAttribute("columnTypeName", columnTypeName);
				} catch (SQLException e) {
					log.debug("Could not determine columnTypeName",e);
				}
				try {
					field.addAttribute("columnClassName", rsmeta.getColumnClassName(j));
				} catch (SQLException e) {
					log.debug("Could not determine columnClassName",e);
				}
			}
		}
	}


	public static String getRowXml(IDbmsSupport dbmsSupport, ResultSet rs, int rowNumber, ResultSetMetaData rsmeta, String blobCharset, boolean decompressBlobs, String nullValue, boolean trimSpaces, boolean getBlobSmart) throws SenderException, SQLException, SAXException {
		XmlWriter writer = new XmlWriter();
		try (SaxElementBuilder parent = new SaxElementBuilder(writer)) {
			getRowXml(parent, dbmsSupport, rs, rowNumber, rsmeta, blobCharset, decompressBlobs, nullValue, trimSpaces, getBlobSmart);
		}
		return writer.toString();
	}

	public static void getRowXml(SaxElementBuilder rows, IDbmsSupport dbmsSupport, ResultSet rs, int rowNumber, ResultSetMetaData rsmeta, String blobCharset, boolean decompressBlobs, String nullValue, boolean trimSpaces, boolean getBlobSmart) throws SenderException, SQLException, SAXException {
		try (SaxElementBuilder row = rows.startElement("row")) {
			row.addAttribute("number", "" + rowNumber);
			for (int i = 1; i <= rsmeta.getColumnCount(); i++) {
				try (SaxElementBuilder resultField = row.startElement("field")) {

					String columnName = rsmeta.getColumnName(i);
					if(convertFieldnamesToUppercase)
						columnName = columnName.toUpperCase();
					resultField.addAttribute("name", columnName);

					try {
						String value = JdbcUtil.getValue(dbmsSupport, rs, i, rsmeta, blobCharset, decompressBlobs, nullValue, trimSpaces, getBlobSmart, false);
						if (rs.wasNull()) {
							resultField.addAttribute("null","true");
						}
						resultField.addValue(value);
					} catch (Exception e) {
						throw new SenderException("error getting fieldvalue column ["+i+"] fieldType ["+getFieldType(rsmeta.getColumnType(i))+ "]", e);
					}
				}
			}
			JdbcUtil.warningsToXml(rs.getWarnings(), row);
		}
	}


	public void setDocumentName(String s) {
		docname = s;
	}

	public void setRecordName(String s) {
		recordname = s;
	}

	/**
	 * Set the presentation of a <code>Null</code> value
	 */
	public void setNullValue(String s) {
		nullValue = s;
	}

	/**
	 * Get the presentation of a <code>Null</code> value
	 */
	public String getNullValue() {
		return nullValue;
	}

	public void setTrimSpaces(boolean b) {
		trimSpaces = b;
	}
	public boolean isTrimSpaces() {
		return trimSpaces;
	}

	public void setDecompressBlobs(boolean b) {
		decompressBlobs = b;
	}
	public boolean isDecompressBlobs() {
		return decompressBlobs;
	}

	public void setGetBlobSmart(boolean b) {
		getBlobSmart = b;
	}
	public boolean isGetBlobSmart() {
		return getBlobSmart;
	}

	public String getBlobCharset() {
		return blobCharset;
	}

	public void setBlobCharset(String string) {
		blobCharset = string;
	}

}
