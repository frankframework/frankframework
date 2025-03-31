/*
   Copyright 2013, 2014, 2017-2020 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.DbmsException;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.documentbuilder.ArrayBuilder;
import org.frankframework.documentbuilder.INodeBuilder;
import org.frankframework.documentbuilder.ObjectBuilder;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterType;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.Base64Pipe.Direction;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.xml.SaxElementBuilder;

/**
 * Database-oriented utility functions.
 *
 * @author Gerrit van Brakel
 * @since 4.1
 */
@Log4j2
public class JdbcUtil {

	private static final String DATEFORMAT = AppConstants.getInstance().getString("jdbc.dateFormat", "yyyy-MM-dd");
	public static final DateTimeFormatter DATEFORMAT_DATE_TIME_FORMATTER = DateFormatUtils.getDateTimeFormatterWithOptionalComponents(DATEFORMAT);
	private static final String TIMESTAMPFORMAT = AppConstants.getInstance().getString("jdbc.timestampFormat", "yyyy-MM-dd HH:mm:ss");
	public static final DateTimeFormatter TIMESTAMP_DATE_TIME_FORMATTER = DateFormatUtils.getDateTimeFormatterWithOptionalComponents(TIMESTAMPFORMAT);

	@Deprecated
	public static String warningsToString(SQLWarning warnings) {
		XmlBuilder warningsElem = warningsToXmlBuilder(warnings);
		if (warningsElem != null) {
			return warningsElem.asXmlString();
		}
		return null;
	}

	@Deprecated
	public static void warningsToXml(SQLWarning warnings, XmlBuilder parent) {
		XmlBuilder warningsElem = warningsToXmlBuilder(warnings);
		if (warningsElem != null) {
			parent.addSubElement(warningsElem);
		}
	}

	@Deprecated
	public static XmlBuilder warningsToXmlBuilder(SQLWarning warnings) {
		if (warnings != null) {
			XmlBuilder warningsElem = new XmlBuilder("warnings");
			while (warnings != null) {
				XmlBuilder warningElem = new XmlBuilder("warning");
				warningElem.addAttribute("errorCode", String.valueOf(warnings.getErrorCode()));
				warningElem.addAttribute("sqlState", warnings.getSQLState());
				String message = warnings.getMessage();

				Throwable cause = warnings.getCause();
				if (cause != null) {
					warningElem.addAttribute("cause", cause.getClass().getName());
					if (message == null) {
						message = cause.getMessage();
					} else {
						message = message + ": " + cause.getMessage();
					}
				}

				warningElem.addAttribute("message", message);
				warningsElem.addSubElement(warningElem);
				warnings = warnings.getNextWarning();
			}
			return warningsElem;
		}
		return null;
	}

	static void warningsToXml(SQLWarning warnings, SaxElementBuilder parent) throws SAXException {
		if (warnings != null) {
			try (SaxElementBuilder elementBuilder = parent.startElement("warnings")) {
				while (warnings != null) {
					try (SaxElementBuilder warning = elementBuilder.startElement("warning")) {
						warning.addAttribute("errorCode", String.valueOf(warnings.getErrorCode()));
						warning.addAttribute("sqlState", warnings.getSQLState());

						String message = warnings.getMessage();

						Throwable cause = warnings.getCause();
						if (cause != null) {
							warning.addAttribute("cause", cause.getClass().getName());
							if (message == null) {
								message = cause.getMessage();
							} else {
								message = message + ": " + cause.getMessage();
							}
						}
						warning.addAttribute("message", message);
						warnings = warnings.getNextWarning();
					}
				}
			}
		}
	}

	static void warningsToDocument(SQLWarning warnings, ObjectBuilder parent) throws SAXException {
		if (warnings != null) {
			try (ArrayBuilder arrayBuilder = parent.addArrayField("warnings", "warning")) {
				while (warnings != null) {
					try (INodeBuilder nodeBuilder = arrayBuilder.addElement()) {
						try (ObjectBuilder warning = nodeBuilder.startObject()) {
							warning.add("errorCode", String.valueOf(warnings.getErrorCode()));
							warning.add("sqlState", warnings.getSQLState());

							String message = warnings.getMessage();

							Throwable cause = warnings.getCause();
							if (cause != null) {
								warning.add("cause", cause.getClass().getName());
								if (message == null) {
									message = cause.getMessage();
								} else {
									message = message + ": " + cause.getMessage();
								}
							}
							warning.add("message", message);
						}
					}
					warnings = warnings.getNextWarning();
				}
			}
		}
	}

	public static String getValue(final IDbmsSupport dbmsSupport, final ResultSet rs, final int colNum, final ResultSetMetaData rsmeta, String blobCharset, boolean decompressBlobs, String nullValue, boolean trimSpaces, boolean getBlobSmart, boolean encodeBlobBase64) throws IOException, SQLException {
		if (dbmsSupport.isBlobType(rsmeta, colNum)) {
			if (dbmsSupport.isRowVersionTimestamp(rsmeta, colNum)) {
				return rs.getString(colNum);
			}
			try {
				return JdbcUtil.getBlobAsString(dbmsSupport, rs, colNum, blobCharset, decompressBlobs, getBlobSmart, encodeBlobBase64);
			} catch (JdbcException e) {
				log.debug("Caught JdbcException, assuming no blob found", e);
				return nullValue;
			}
		}
		if (dbmsSupport.isClobType(rsmeta, colNum)) {
			try {
				return JdbcUtil.getClobAsString(dbmsSupport, rs, colNum, false);
			} catch (JdbcException e) {
				log.debug("Caught JdbcException, assuming no clob found", e);
				return nullValue;
			}
		}
		int columnType = rsmeta.getColumnType(colNum);
		switch (columnType) {
			// return "undefined" for types that cannot be rendered to strings easily
			case Types.LONGVARBINARY:
			case Types.VARBINARY:
			case Types.BINARY:
			case Types.BLOB:
			case Types.ARRAY:
			case Types.DISTINCT:
			case Types.REF:
			case Types.STRUCT:
				return "undefined";
			case Types.BOOLEAN:
			case Types.BIT: {
				boolean value = rs.getBoolean(colNum);
				return Boolean.toString(value);
			}
			// return as specified date format
			case Types.TIMESTAMP:
			case Types.DATE: {
				try {
					if (columnType == Types.TIMESTAMP && !TIMESTAMPFORMAT.isEmpty()) {
						return TIMESTAMP_DATE_TIME_FORMATTER.format(rs.getTimestamp(colNum).toLocalDateTime());

					} else if (columnType == Types.DATE && !DATEFORMAT.isEmpty()) {
						return DATEFORMAT_DATE_TIME_FORMATTER.format(rs.getDate(colNum).toLocalDate());
					}
				} catch (Exception e) {
					// Do nothing, the default: will handle it
				}
			}
			// $FALL-THROUGH$
			default: {
				Object value = rs.getObject(colNum);
				if (value == null) {
					return nullValue;
				}
				if (trimSpaces) {
					return value.toString().trim();
				}
				return value.toString();
			}
		}
	}

	public static InputStream getBlobInputStream(final IDbmsSupport dbmsSupport, final ResultSet rs, int column, boolean blobIsCompressed) throws SQLException, JdbcException {
		return getBlobInputStream(dbmsSupport.getBlobInputStream(rs, column), blobIsCompressed);
	}

	public static InputStream getBlobInputStream(final IDbmsSupport dbmsSupport, final ResultSet rs, String column, boolean blobIsCompressed) throws SQLException, JdbcException {
		return getBlobInputStream(dbmsSupport.getBlobInputStream(rs, column), blobIsCompressed);
	}

	private static InputStream getBlobInputStream(InputStream blobInputStream, boolean blobIsCompressed) {
		if (blobInputStream == null) {
			return null;
		}
		if (blobIsCompressed) {
			return new InflaterInputStream(blobInputStream);
		}
		return blobInputStream;
	}

	public static Reader getBlobReader(final IDbmsSupport dbmsSupport, final ResultSet rs, int column, String charset, boolean blobIsCompressed) throws IOException, JdbcException, SQLException {
		return getBlobReader(getBlobInputStream(dbmsSupport, rs, column, blobIsCompressed), charset);
	}

	private static Reader getBlobReader(final InputStream blobInputStream, String charset) throws IOException {
		if (blobInputStream == null) {
			return null;
		}
		if (charset == null) {
			charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}
		return StreamUtil.getCharsetDetectingInputStreamReader(blobInputStream, charset);
	}

	public static void streamBlob(final IDbmsSupport dbmsSupport, final ResultSet rs, int columnIndex, String charset, boolean blobIsCompressed, Direction blobBase64Direction, Object target, boolean close) throws JdbcException, SQLException, IOException {
		try (InputStream blobInputStream = getBlobInputStream(dbmsSupport, rs, columnIndex, blobIsCompressed)) {
			streamBlob(blobInputStream, charset, blobBase64Direction, target, close);
		}
	}

	private static void streamBlob(final InputStream blobInputStream, String charset, Direction blobBase64Direction, Object target, boolean close) throws JdbcException, IOException {
		if (target == null) {
			throw new JdbcException("cannot stream Blob to null object");
		}
		OutputStream outputStream = getOutputStream(target);
		if (outputStream != null) {
			if (blobBase64Direction == Direction.DECODE) {
				Base64InputStream base64DecodedStream = new Base64InputStream(blobInputStream);
				StreamUtil.copyStream(base64DecodedStream, outputStream, 50000);
			} else if (blobBase64Direction == Direction.ENCODE) {
				Base64InputStream base64EncodedStream = new Base64InputStream(blobInputStream, true);
				StreamUtil.copyStream(base64EncodedStream, outputStream, 50000);
			} else {
				StreamUtil.copyStream(blobInputStream, outputStream, 50000);
			}

			if (close) {
				outputStream.close();
			}
			return;
		}
		Writer writer = getWriter(target);
		if (writer != null) {
			Reader reader = JdbcUtil.getBlobReader(blobInputStream, charset);
			StreamUtil.copyReaderToWriter(reader, writer, 50000);
			if (close) {
				writer.close();
			}
			return;
		}
		throw new IOException("cannot stream Blob to [" + target.getClass().getName() + "]");
	}

	@Deprecated
	private static Writer getWriter(Object target) throws IOException {
		if (target instanceof HttpServletResponse response) {
			return response.getWriter();
		}
		if (target instanceof Writer writer) {
			return writer;
		}

		return null;
	}

	public static void streamClob(final IDbmsSupport dbmsSupport, ResultSet rs, int column, Object target, boolean close) throws DbmsException, SQLException, IOException {
		if (target == null) {
			throw new NullPointerException("cannot stream Clob to null object");
		}
		Writer writer = getWriter(target);
		if (writer != null) {
			try (Reader reader = dbmsSupport.getClobReader(rs, column)) {
				StreamUtil.copyReaderToWriter(reader, writer, 50000);
			}
			if (close) {
				writer.close();
			}
			return;
		}
		OutputStream outputStream = getOutputStream(target);
		if (outputStream != null) {
			try (Reader reader = dbmsSupport.getClobReader(rs, column)) {
				try (Writer streamWriter = new OutputStreamWriter(outputStream, StreamUtil.DEFAULT_CHARSET)) {
					StreamUtil.copyReaderToWriter(reader, streamWriter, 50000);
				}
			}
			if (close) {
				outputStream.close();
			}
			return;
		}
		throw new IOException("cannot stream Clob to [" + target.getClass().getName() + "]");
	}

	public static String getBlobAsString(final IDbmsSupport dbmsSupport, final ResultSet rs, int column, String charset, boolean blobIsCompressed, boolean blobSmartGet, boolean encodeBlobBase64) throws IOException, JdbcException, SQLException {
		try (InputStream blobStream = getBlobInputStream(dbmsSupport, rs, column, blobIsCompressed)) {
			return getBlobAsString(blobStream, Integer.toString(column), charset, blobSmartGet, encodeBlobBase64);
		} catch (ZipException | EOFException e) {    // if any decompression exception occurs in getBlobInputStream
			if (blobSmartGet && blobIsCompressed) { // then 'blobSmartGet' will try again to retrieve the stream, but then without decompressing
				try (InputStream blobStream = getBlobInputStream(dbmsSupport, rs, column, false)) {
					return getBlobAsString(blobStream, Integer.toString(column), charset, blobSmartGet, encodeBlobBase64);
				}
			}
			throw e;
		}
	}

	public static String getBlobAsString(final IDbmsSupport dbmsSupport, final ResultSet rs, String column, String charset, boolean blobIsCompressed, boolean blobSmartGet, boolean encodeBlobBase64) throws IOException, JdbcException, SQLException {
		try (InputStream blobStream = getBlobInputStream(dbmsSupport, rs, column, blobIsCompressed)) {
			return getBlobAsString(blobStream, column, charset, blobSmartGet, encodeBlobBase64);
		} catch (ZipException | EOFException e) {    // if any decompression exception occurs in getBlobInputStream
			if (blobSmartGet && blobIsCompressed) { // then 'blobSmartGet' will try again to retrieve the stream, but then without decompressing
				try (InputStream blobStream = getBlobInputStream(dbmsSupport, rs, column, false)) {
					return getBlobAsString(blobStream, column, charset, blobSmartGet, encodeBlobBase64);
				}
			}
			throw e;
		}
	}

	private static String getBlobAsString(final InputStream blobInputStream, String column, String charset, boolean blobSmartGet, boolean encodeBlobBase64) throws IOException, JdbcException {
		if (blobInputStream == null) {
			log.debug("no blob found in column [{}]", column);
			return null;
		}
		if (encodeBlobBase64) {
			return StreamUtil.streamToString(new Base64InputStream(blobInputStream, true), null, false);
		}
		if (blobSmartGet) {
			byte[] bytes = StreamUtil.streamToBytes(blobInputStream);
			Object result = null;
			boolean objectOK = true;
			try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
				try (ObjectInputStream ois = new RenamingObjectInputStream(bis)) {
					result = ois.readObject();
				} catch (Exception e) {
					log.debug("message in column [{}] is probably not a serialized object: {}", column, e.getClass().getName());
					objectOK = false;
				}
			}
			String rawMessage;
			if (objectOK) {
				if (result instanceof MessageWrapper<?> wrapper) {
					rawMessage = wrapper.getMessage().asString();
				} else {
					rawMessage = MessageUtils.asString(result);
				}
			} else {
				rawMessage = new String(bytes, charset);
			}

			return XmlEncodingUtils.replaceNonValidXmlCharacters(rawMessage);
		}
		return StreamUtil.readerToString(getBlobReader(blobInputStream, charset), null, false);
	}

	public static OutputStream getBlobOutputStream(IDbmsSupport dbmsSupport, Object blobUpdateHandle, final ResultSet rs, int columnIndex, boolean compressBlob) throws SQLException, DbmsException {
		OutputStream result;
		OutputStream out = dbmsSupport.getBlobOutputStream(rs, columnIndex, blobUpdateHandle);
		if (compressBlob) {
			result = new DeflaterOutputStream(out);
		} else {
			result = out;
		}
		return result;
	}

	public static String getClobAsString(final IDbmsSupport dbmsSupport, final ResultSet rs, int columnIndex, boolean xmlEncode) throws IOException, JdbcException, SQLException {
		Reader reader = dbmsSupport.getClobReader(rs, columnIndex);
		if (reader == null) {
			return null;
		}
		return StreamUtil.readerToString(reader, null, xmlEncode);
	}

	public static void fullClose(Connection connection, ResultSet rs) {
		if (rs == null) {
			log.warn("resultSet to close was null");
			close(connection);
			return;
		}
		try {
			if (!rs.getStatement().isClosed()) {
				rs.getStatement().close();
			}
			if (!rs.isClosed()) {
				rs.close();
			}
		} catch (SQLException e) {
			log.warn("Could not obtain statement or connection from resultset", e);
		} finally {
			close(connection);
		}
	}

	/**
	 * Note: Depending on the connect pool used (for example with Tomcat 7) the
	 * connection retrieved from the statement will be the direct connection
	 * instead of the proxied connection. After a close on this (unproxied)
	 * connection the transaction manager isn't able to do a commit anymore.
	 * Hence, this method doesn't get it from the statement but has an extra
	 * connection parameter.
	 *
	 * @param connection the proxied/original connection the statement was created with
	 * @param statement  the statement to close
	 */
	public static void fullClose(Connection connection, Statement statement) {
		try {
			if (statement != null && !statement.isClosed()) {
				statement.close();
			}
		} catch (SQLException e) {
			log.warn("Could not close statement", e);
		} finally {
			close(connection);
		}
	}

	public static void close(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				log.warn("Could not close connection", e);
			}
		}
	}

	public static boolean isQueryResultEmpty(Connection connection, String query) throws JdbcException {
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			try (ResultSet rs = stmt.executeQuery()) {
				return !rs.next(); // rs.isAfterLast() does not work properly when rs.next() has not yet been called
			}
		} catch (SQLException e) {
			throw new JdbcException("could not obtain value using query [" + query + "]", e);
		}
	}

	public static void applyParameters(IDbmsSupport dbmsSupport, PreparedStatement statement, ParameterList parameters, Message message, PipeLineSession session) throws JdbcException, ParameterException {
		if (parameters != null) {
			applyParameters(dbmsSupport, statement, parameters.getValues(message, session), session);
		}
	}

	public static void applyParameters(IDbmsSupport dbmsSupport, PreparedStatement statement, ParameterValueList parameters, PipeLineSession session) throws JdbcException {
		boolean parameterTypeMatchRequired = dbmsSupport.isParameterTypeMatchRequired();
		if (parameters != null) {
			for (int i = 0; i < parameters.size(); i++) {
				ParameterValue parameterValue = parameters.getParameterValue(i);
				if (parameterValue.getDefinition().getMode() == Parameter.ParameterMode.OUTPUT) {
					continue;
				}
				try {
					applyParameter(statement, parameterValue, i + 1, parameterTypeMatchRequired, session);
				} catch (SQLException | IOException e) {
					throw new JdbcException("Could not set parameter [" + parameterValue.getName() +
							"] with type [" + parameterValue.getDefinition().getType() +
							"] at position " + i + ", exception: " + e.getMessage(), e);
				}
			}
		}
	}

	public static SQLType mapParameterTypeToSqlType(IDbmsSupport dbmsSupport, ParameterType parameterType) {
		switch (parameterType) {
			case DATE:
				return JDBCType.DATE;
			case TIMESTAMP:
			case DATETIME:
			case XMLDATETIME:
				return JDBCType.TIMESTAMP;
			case TIME:
				return JDBCType.TIME;
			case NUMBER:
				return JDBCType.NUMERIC;
			case INTEGER:
				return JDBCType.INTEGER;
			case BOOLEAN:
				return JDBCType.BOOLEAN;
			case STRING:
				return JDBCType.VARCHAR;
			case CHARACTER:
				return JDBCType.CLOB;
			case BINARY:
				return JDBCType.BLOB;
			case LIST:
				// Type 'LIST' is used for REF_CURSOR type OUTPUT parameters of stored procedures.
				return dbmsSupport.getCursorSqlType();
			default:
				throw new IllegalArgumentException("Parameter type [" + parameterType + "] cannot be mapped to a SQL type");
		}
	}

	private static void applyParameter(PreparedStatement statement, ParameterValue pv, int parameterIndex, boolean parameterTypeMatchRequired, PipeLineSession session) throws SQLException, IOException {
		String paramName = pv.getDefinition().getName();
		ParameterType paramType = pv.getDefinition().getType();
		Object value = pv.getValue();
		if (log.isDebugEnabled())
			log.debug("jdbc parameter [{}] applying parameter [{}] type [{}] value [{}]", parameterIndex, paramName, paramType, value);
		switch (paramType) {
			case DATE:
				if (value == null) {
					statement.setNull(parameterIndex, Types.DATE);
				} else {
					statement.setDate(parameterIndex, new java.sql.Date(((Date) value).getTime()));
				}
				break;
			case DATETIME:
			case TIMESTAMP:
			case XMLDATETIME:
				if (value == null) {
					statement.setNull(parameterIndex, Types.TIMESTAMP);
				} else {
					statement.setTimestamp(parameterIndex, new Timestamp(((Date) value).getTime()));
				}
				break;
			case TIME:
				if (value == null) {
					statement.setNull(parameterIndex, Types.TIME);
				} else {
					statement.setTime(parameterIndex, new java.sql.Time(((Date) value).getTime()));
				}
				break;
			case NUMBER:
				if (value == null) {
					statement.setNull(parameterIndex, Types.NUMERIC);
				} else {
					statement.setDouble(parameterIndex, ((Number) value).doubleValue());
				}
				break;
			case INTEGER:
				if (value == null) {
					statement.setNull(parameterIndex, Types.INTEGER);
				} else {
					statement.setInt(parameterIndex, (Integer) value);
				}
				break;
			case BOOLEAN:
				if (value == null) {
					statement.setNull(parameterIndex, Types.BOOLEAN);
				} else {
					statement.setBoolean(parameterIndex, (Boolean) value);
				}
				break;
			// noinspection deprecation
			case INPUTSTREAM:
			case BINARY: {
				Message message = Message.asMessage(value);
				message.closeOnCloseOf(session);
				if (message.requiresStream()) {
					statement.setBinaryStream(parameterIndex, message.asInputStream());
				} else {
					statement.setBytes(parameterIndex, message.asByteArray());
				}
				break;
			}
			case CHARACTER: {
				Message message = Message.asMessage(value);
				message.closeOnCloseOf(session);
				if (message.requiresStream()) {
					statement.setCharacterStream(parameterIndex, message.asReader());
				} else {
					statement.setString(parameterIndex, message.asString());
				}
				break;
			}
			// noinspection deprecation
			case BYTES: {
				Message message = Message.asMessage(value);
				message.closeOnCloseOf(session);

				statement.setBytes(parameterIndex, message.asByteArray());
				break;
			}
			default:
				Message message = Message.asMessage(value);
				message.closeOnCloseOf(session);
				setParameter(statement, parameterIndex, message.asString(), parameterTypeMatchRequired);
		}
	}

	public static void setParameter(PreparedStatement statement, int parameterIndex, String value, boolean parameterTypeMatchRequired) throws SQLException {
		setParameter(statement, parameterIndex, value, parameterTypeMatchRequired, parameterTypeMatchRequired ? statement.getParameterMetaData() : null);
	}

	public static void setParameter(PreparedStatement statement, int parameterIndex, String value, boolean parameterTypeMatchRequired, ParameterMetaData parameterMetaData) throws SQLException {
		if (!parameterTypeMatchRequired) {
			statement.setString(parameterIndex, value);
			return;
		}
		int sqlTYpe = parameterMetaData.getParameterType(parameterIndex);
		try {
			switch (sqlTYpe) {
				case Types.INTEGER:
					statement.setInt(parameterIndex, Integer.parseInt(value));
					break;
				case Types.NUMERIC:
				case Types.DOUBLE:
					statement.setDouble(parameterIndex, Double.parseDouble(value));
					break;
				case Types.BIGINT:
					statement.setLong(parameterIndex, Long.parseLong(value));
					break;
				case Types.BLOB:
					statement.setBytes(parameterIndex, value.getBytes(StreamUtil.DEFAULT_CHARSET));
					break;
				case Types.DATE:
					statement.setDate(parameterIndex, new java.sql.Date(DateFormatUtils.parseAnyDate(value).getTime()));
					break;
				case Types.TIMESTAMP:
					statement.setTimestamp(parameterIndex, new Timestamp(DateFormatUtils.parseAnyDate(value).getTime()));
					break;
				default:
					log.warn("parameter type [{}] handled as String", () -> JDBCType.valueOf(sqlTYpe).getName());
					// $FALL-THROUGH$
				case Types.CHAR:
				case Types.VARCHAR:
					statement.setString(parameterIndex, value);
					break;
			}
		} catch (DateTimeParseException | IllegalArgumentException e) { // thrown by parseAnyDate in case DATE and TIMESTAMP
			throw new SQLException("Could not convert [" + value + "] for parameter [" + parameterIndex + "]", e);
		}
	}

	public static boolean isSQLTypeNumeric(int sqlType) {
		switch (sqlType) {
			case Types.INTEGER:
			case Types.NUMERIC:
			case Types.DOUBLE:
			case Types.BIGINT:
			case Types.DECIMAL:
			case Types.FLOAT:
			case Types.REAL:
			case Types.SMALLINT:
			case Types.TINYINT:
				return true;
			default:
				return false;
		}
	}

	@Deprecated
	private static OutputStream getOutputStream(Object target) throws IOException {
		if (target instanceof OutputStream stream) {
			return stream;
		}
		if (target instanceof String string) {
			return getFileOutputStream(string);
		}
		if (target instanceof Message message && message.isRequestOfType(String.class)) {
			return getFileOutputStream(message.asString());
		}
		return null;
	}

	@Deprecated
	private static OutputStream getFileOutputStream(String filename) throws IOException {
		if (StringUtils.isEmpty(filename)) {
			throw new IOException("target string cannot be empty but must contain a filename");
		}
		try {
			return new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			FileNotFoundException fnfe = new FileNotFoundException("cannot create file [" + filename + "]");
			fnfe.initCause(e);
			throw fnfe;
		}
	}

}
