/*
   Copyright 2013, 2014, 2017-2020 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.io.BufferedWriter;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.Base64Pipe.Direction;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.document.ArrayBuilder;
import nl.nn.adapterframework.stream.document.INodeBuilder;
import nl.nn.adapterframework.stream.document.ObjectBuilder;
import nl.nn.adapterframework.xml.SaxElementBuilder;

/**
 * Database-oriented utility functions.
 *
 * @author  Gerrit van Brakel
 * @since   4.1
 */
public class JdbcUtil {
	protected static Logger log = LogUtil.getLogger(JdbcUtil.class);

	private static final String DATEFORMAT = AppConstants.getInstance().getString("jdbc.dateFormat", "yyyy-MM-dd");
	private static final String TIMESTAMPFORMAT = AppConstants.getInstance().getString("jdbc.timestampFormat", "yyyy-MM-dd HH:mm:ss");

	@Deprecated
	public static String warningsToString(SQLWarning warnings) {
		XmlBuilder warningsElem = warningsToXmlBuilder(warnings);
		if (warningsElem!=null) {
			return warningsElem.toXML();
		}
		return null;
	}

	@Deprecated
	public static void warningsToXml(SQLWarning warnings, XmlBuilder parent) {
		XmlBuilder warningsElem=warningsToXmlBuilder(warnings);
		if (warningsElem!=null) {
			parent.addSubElement(warningsElem);
		}
	}

	@Deprecated
	public static XmlBuilder warningsToXmlBuilder(SQLWarning warnings) {
		if (warnings!=null) {
			XmlBuilder warningsElem = new XmlBuilder("warnings");
			while (warnings!=null) {
				XmlBuilder warningElem = new XmlBuilder("warning");
				warningElem.addAttribute("errorCode",""+warnings.getErrorCode());
				warningElem.addAttribute("sqlState",""+warnings.getSQLState());
				String message=warnings.getMessage();

				Throwable cause=warnings.getCause();
				if (cause!=null) {
					warningElem.addAttribute("cause",cause.getClass().getName());
					if (message==null) {
						message=cause.getMessage();
					} else {
						message=message+": "+cause.getMessage();
					}
				}

				warningElem.addAttribute("message",message);
				warningsElem.addSubElement(warningElem);
				warnings=warnings.getNextWarning();
			}
			return warningsElem;
		}
		return null;
	}


	public static void warningsToXml(SQLWarning warnings, SaxElementBuilder parent) throws SAXException {
		if (warnings!=null) {
			try (SaxElementBuilder elementBuilder = parent.startElement("warnings")) {
				while (warnings!=null) {
					try (SaxElementBuilder warning = elementBuilder.startElement("warning")) {
						warning.addAttribute("errorCode",""+warnings.getErrorCode());
						warning.addAttribute("sqlState",""+warnings.getSQLState());

						String message=warnings.getMessage();

						Throwable cause=warnings.getCause();
						if (cause!=null) {
							warning.addAttribute("cause",cause.getClass().getName());
							if (message==null) {
								message=cause.getMessage();
							} else {
								message=message+": "+cause.getMessage();
							}
						}
						warning.addAttribute("message",message);
						warnings=warnings.getNextWarning();
					}
				}
			}
		}
	}

	public static void warningsToDocument(SQLWarning warnings, ObjectBuilder parent) throws SAXException {
		if (warnings!=null) {
			try (ArrayBuilder arrayBuilder = parent.addArrayField("warnings", "warning")) {
				while (warnings!=null) {
					try (INodeBuilder nodeBuilder=arrayBuilder.addElement()) {
						try (ObjectBuilder warning=nodeBuilder.startObject()) {
							warning.add("errorCode",""+warnings.getErrorCode());
							warning.add("sqlState",""+warnings.getSQLState());

							String message=warnings.getMessage();

							Throwable cause=warnings.getCause();
							if (cause!=null) {
								warning.add("cause",cause.getClass().getName());
								if (message==null) {
									message=cause.getMessage();
								} else {
									message=message+": "+cause.getMessage();
								}
							}
							warning.add("message",message);
						}
					}
					warnings=warnings.getNextWarning();
				}
			}
		}
	}


	public static String getValue(final IDbmsSupport dbmsSupport, final ResultSet rs, final int colNum, final ResultSetMetaData rsmeta, String blobCharset, boolean decompressBlobs, String nullValue, boolean trimSpaces, boolean getBlobSmart, boolean encodeBlobBase64) throws IOException, SQLException {
		if (dbmsSupport.isBlobType(rsmeta, colNum)) {
			try {
				return JdbcUtil.getBlobAsString(dbmsSupport, rs,colNum,blobCharset,decompressBlobs,getBlobSmart,encodeBlobBase64);
			} catch (JdbcException e) {
				log.debug("Caught JdbcException, assuming no blob found",e);
				return nullValue;
			}

		}
		if (dbmsSupport.isClobType(rsmeta, colNum)) {
			try {
				return JdbcUtil.getClobAsString(dbmsSupport, rs,colNum,false);
			} catch (JdbcException e) {
				log.debug("Caught JdbcException, assuming no clob found",e);
				return nullValue;
			}
		}
		switch(rsmeta.getColumnType(colNum)) {
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
			case Types.BOOLEAN :
			case Types.BIT :
			{
				boolean value = rs.getBoolean(colNum);
				return Boolean.toString(value);
			}
			// return as specified date format
			case Types.TIMESTAMP :
			case Types.DATE :
			{
				try {
					if(rsmeta.getColumnType(colNum) == Types.TIMESTAMP && !TIMESTAMPFORMAT.isEmpty())
						return new SimpleDateFormat(TIMESTAMPFORMAT).format(rs.getTimestamp(colNum));
					else if(rsmeta.getColumnType(colNum) == Types.DATE && !DATEFORMAT.isEmpty())
						return new SimpleDateFormat(DATEFORMAT).format(rs.getDate(colNum));
				}
				catch (Exception e) {
					//Do nothing, the default: will handle it
				}
			}
			//$FALL-THROUGH$
			default: {
				String value = rs.getString(colNum);
				if (value == null) {
					return nullValue;
				}
				if (trimSpaces) {
					return value.trim();
				}
				return value;
			}
			}
		}

	public static InputStream getBlobInputStream(final IDbmsSupport dbmsSupport, final ResultSet rs, int column, boolean blobIsCompressed) throws SQLException, JdbcException {
		return getBlobInputStream(dbmsSupport.getBlobInputStream(rs, column), blobIsCompressed);
	}
	public static InputStream getBlobInputStream(final IDbmsSupport dbmsSupport, final ResultSet rs, String column, boolean blobIsCompressed) throws SQLException, JdbcException {
		return getBlobInputStream(dbmsSupport.getBlobInputStream(rs, column), blobIsCompressed);
	}
	private static InputStream getBlobInputStream(InputStream blobIntputStream, boolean blobIsCompressed) {
		if (blobIntputStream==null) {
			return null;
		}
		if (blobIsCompressed) {
			return new InflaterInputStream(blobIntputStream);
		}
		return blobIntputStream;
	}

	public static Reader getBlobReader(final IDbmsSupport dbmsSupport, final ResultSet rs, int column, String charset, boolean blobIsCompressed) throws IOException, JdbcException, SQLException {
		return getBlobReader(getBlobInputStream(dbmsSupport, rs, column, blobIsCompressed), charset);
	}
	public static Reader getBlobReader(final IDbmsSupport dbmsSupport, final ResultSet rs, String column, String charset, boolean blobIsCompressed) throws IOException, JdbcException, SQLException {
		return getBlobReader(getBlobInputStream(dbmsSupport, rs, column, blobIsCompressed),charset);
	}
	public static Reader getBlobReader(final InputStream blobIntputStream, String charset) throws IOException {
		if (blobIntputStream==null) {
			return null;
		}
		if (charset==null) {
			charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}
		return StreamUtil.getCharsetDetectingInputStreamReader(blobIntputStream, charset);
	}

	public static void streamBlob(final IDbmsSupport dbmsSupport, final ResultSet rs, int columnIndex, String charset, boolean blobIsCompressed, Direction blobBase64Direction, Object target, boolean close) throws JdbcException, SQLException, IOException {
		try (InputStream blobIntputStream = getBlobInputStream(dbmsSupport, rs, columnIndex, blobIsCompressed)) {
			streamBlob(blobIntputStream, charset, blobBase64Direction, target, close);
		}
	}
	public static void streamBlob(final IDbmsSupport dbmsSupport, final ResultSet rs, String columnName, String charset, boolean blobIsCompressed, Direction blobBase64Direction, Object target, boolean close) throws JdbcException, SQLException, IOException {
		try (InputStream blobIntputStream = getBlobInputStream(dbmsSupport, rs, columnName, blobIsCompressed)) {
			streamBlob(blobIntputStream, charset, blobBase64Direction, target, close);
		}
	}

	public static void streamBlob(final InputStream blobIntputStream, String charset, Direction blobBase64Direction, Object target, boolean close) throws JdbcException, IOException {
		if (target==null) {
			throw new JdbcException("cannot stream Blob to null object");
		}
		OutputStream outputStream = getOutputStream(target);
		if (outputStream != null) {
			if (blobBase64Direction == Direction.DECODE){
				Base64InputStream base64DecodedStream = new Base64InputStream (blobIntputStream);
				StreamUtil.copyStream(base64DecodedStream, outputStream, 50000);
			}
			else if (blobBase64Direction == Direction.ENCODE){
				Base64InputStream base64EncodedStream = new Base64InputStream (blobIntputStream, true);
				StreamUtil.copyStream(base64EncodedStream, outputStream, 50000);
			}
			else {
				StreamUtil.copyStream(blobIntputStream, outputStream, 50000);
			}

			if (close) {
				outputStream.close();
			}
			return;
		}
		Writer writer = getWriter(target);
		if (writer !=null) {
			Reader reader = JdbcUtil.getBlobReader(blobIntputStream, charset);
			StreamUtil.copyReaderToWriter(reader, writer, 50000);
			if (close) {
				writer.close();
			}
			return;
		}
		throw new IOException("cannot stream Blob to ["+target.getClass().getName()+"]");
	}

	@Deprecated
	public static Writer getWriter(Object target) throws IOException {
		if (target instanceof HttpServletResponse) {
			return ((HttpServletResponse)target).getWriter();
		}
		if (target instanceof Writer) {
			return (Writer)target;
		}

		return null;
	}

	public static void streamClob(final IDbmsSupport dbmsSupport, ResultSet rs, int column, Object target, boolean close) throws JdbcException, SQLException, IOException {
		if (target==null) {
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
		throw new IOException("cannot stream Clob to ["+target.getClass().getName()+"]");
	}

	public static String getBlobAsString(final IDbmsSupport dbmsSupport, final ResultSet rs, int column, String charset, boolean blobIsCompressed, boolean blobSmartGet, boolean encodeBlobBase64) throws IOException, JdbcException, SQLException {
		try (InputStream blobStream = getBlobInputStream(dbmsSupport, rs, column, blobIsCompressed)) {
			return getBlobAsString(blobStream, Integer.toString(column), charset, blobSmartGet, encodeBlobBase64);
		} catch (ZipException | EOFException e) { 	// if any decompression exception occurs in getBlobInputStream
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
		} catch (ZipException | EOFException e) { 	// if any decompression exception occurs in getBlobInputStream
			if (blobSmartGet && blobIsCompressed) { // then 'blobSmartGet' will try again to retrieve the stream, but then without decompressing
				try (InputStream blobStream = getBlobInputStream(dbmsSupport, rs, column, false)) {
					return getBlobAsString(blobStream, column, charset, blobSmartGet, encodeBlobBase64);
				}
			}
			throw e;
		}
	}
	public static String getBlobAsString(final InputStream blobIntputStream, String column, String charset, boolean blobSmartGet, boolean encodeBlobBase64) throws IOException, JdbcException {
		if (blobIntputStream==null) {
			log.debug("no blob found in column ["+column+"]");
			return null;
		}
		if (encodeBlobBase64) {
			return StreamUtil.streamToString(new Base64InputStream(blobIntputStream,true),null,false);
		}
		if (blobSmartGet) {
			byte[] bytes = StreamUtil.streamToBytes(blobIntputStream);
			Object result = null;
			boolean objectOK = true;
			try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
				try (ObjectInputStream ois = new ObjectInputStream(bis)) {
					result = ois.readObject();
				} catch (Exception e) {
					log.debug("message in column ["+column+"] is probably not a serialized object: "+e.getClass().getName());
					objectOK=false;
				}
			}
			String rawMessage;
			if (objectOK) {
				if (result instanceof MessageWrapper) {
					rawMessage = ((MessageWrapper)result).getMessage().asString();
				} else if (result instanceof TextMessage) {
					try {
						rawMessage = ((TextMessage)result).getText();
					} catch (JMSException e) {
						throw new JdbcException(e);
					}
				} else {
					rawMessage = Message.asString(result);
				}
			} else {
				rawMessage = new String(bytes,charset);
			}

			return XmlEncodingUtils.replaceNonValidXmlCharacters(rawMessage);
		}
		return StreamUtil.readerToString(getBlobReader(blobIntputStream, charset),null, false);
	}

	public static OutputStream getBlobOutputStream(IDbmsSupport dbmsSupport, Object blobUpdateHandle, final ResultSet rs, int columnIndex, boolean compressBlob) throws JdbcException, SQLException {
		OutputStream result;
		OutputStream out = dbmsSupport.getBlobOutputStream(rs, columnIndex, blobUpdateHandle);
		if (compressBlob) {
			result = new DeflaterOutputStream(out);
		} else {
			result = out;
		}
		return result;
	}

	public static OutputStream getBlobOutputStream(IDbmsSupport dbmsSupport, Object blobUpdateHandle, PreparedStatement stmt, int columnIndex, boolean compressBlob) throws JdbcException, SQLException {
		OutputStream result;
		OutputStream out = dbmsSupport.getBlobOutputStream(stmt, columnIndex, blobUpdateHandle);
		if (compressBlob) {
			result = new DeflaterOutputStream(out, true);
		} else {
			result = out;
		}
		return result;
	}

	public static Writer getBlobWriter(IDbmsSupport dbmsSupport, Object blobUpdateHandle, final ResultSet rs, int columnIndex, String charset, boolean compressBlob) throws IOException, JdbcException, SQLException {
		Writer result;
		OutputStream out = dbmsSupport.getBlobOutputStream(rs, columnIndex, blobUpdateHandle);
		if (charset==null) {
			charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}
		if (compressBlob) {
			result = new BufferedWriter(new OutputStreamWriter(new DeflaterOutputStream(out),charset));
		} else {
			result = new BufferedWriter(new OutputStreamWriter(out,charset));
		}
		return result;
	}

	public static void putStringAsBlob(IDbmsSupport dbmsSupport, final ResultSet rs, int columnIndex, String content, String charset, boolean compressBlob) throws IOException, JdbcException, SQLException {
		if (content!=null) {
			Object blobHandle=dbmsSupport.getBlobHandle(rs, columnIndex);
			try (OutputStream out = dbmsSupport.getBlobOutputStream(rs, columnIndex, blobHandle)) {
				if (charset==null) {
					charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
				}
				if (compressBlob) {
					try (DeflaterOutputStream dos = new DeflaterOutputStream(out)) {
						dos.write(content.getBytes(charset));
					}
				} else {
					out.write(content.getBytes(charset));
				}
			}
			dbmsSupport.updateBlob(rs, columnIndex, blobHandle);
		} else {
			log.warn("content to store in blob was null");
		}
	}

	public static void putByteArrayAsBlob(IDbmsSupport dbmsSupport, final ResultSet rs, int columnIndex, byte content[], boolean compressBlob) throws IOException, JdbcException, SQLException {
		if (content!=null) {
			Object blobHandle=dbmsSupport.getBlobHandle(rs, columnIndex);
			try (OutputStream out = dbmsSupport.getBlobOutputStream(rs, columnIndex, blobHandle)) {
				if (compressBlob) {
					try (DeflaterOutputStream dos = new DeflaterOutputStream(out)) {
						dos.write(content);
					}
				} else {
					out.write(content);
				}
			}
			dbmsSupport.updateBlob(rs, columnIndex, blobHandle);
		} else {
			log.warn("content to store in blob was null");
		}
	}


	public static String getClobAsString(final IDbmsSupport dbmsSupport, final ResultSet rs, int columnIndex, boolean xmlEncode) throws IOException, JdbcException, SQLException {
		Reader reader = dbmsSupport.getClobReader(rs, columnIndex);
		if (reader == null) {
			return null;
		}
		return StreamUtil.readerToString(reader, null, xmlEncode);
	}
	public static String getClobAsString(final IDbmsSupport dbmsSupport, final ResultSet rs, String columnName, boolean xmlEncode) throws IOException, JdbcException, SQLException {
		Reader reader = dbmsSupport.getClobReader(rs, columnName);
		if (reader == null) {
			return null;
		}
		return StreamUtil.readerToString(reader, null, xmlEncode);
	}

	public static void putStringAsClob(IDbmsSupport dbmsSupport, final ResultSet rs, int columnIndex, String content) throws IOException, JdbcException, SQLException {
		if (content!=null) {
			Object clobHandle=dbmsSupport.getClobHandle(rs, columnIndex);
			try (Writer writer = dbmsSupport.getClobWriter(rs, columnIndex, clobHandle)) {
				writer.write(content);
			}
			dbmsSupport.updateClob(rs, columnIndex, clobHandle);
		} else {
			log.warn("content to store in blob was null");
		}
	}

	public static void fullClose(Connection connection, ResultSet rs) {
		if (rs == null) {
			log.warn("resultset to close was null");
			close(connection);
			return;
		}
		try {
			if(!rs.isClosed()) {
				try (Statement statement = rs.getStatement()) {
					//No Operation, just trying to close the statement!
				}
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
	 * @param connection  the proxied/original connection the statement was created with
	 * @param statement   the statement to close
	 */
	public static void fullClose(Connection connection, Statement statement) {
		if (statement == null) {
			log.warn("statement to close was null");
			close(connection);
			return;
		}
		try {
			if(!statement.isClosed()) {
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

	private static String displayParameters(Object... params) {
		StringBuilder sb = new StringBuilder(1024);
		for (int i = 0; i < params.length; i++) {
			sb.append(" param").append(i + 1).append(" [").append(params[i]).append("]");
		}
		return sb.toString();
	}

	private static void applyParameters(PreparedStatement stmt, Object... params) throws SQLException {
		for (int i = 0; i < params.length; i++) {
			Object param = params[i];
			if (param == null) continue;

			int sqlType = deriveSqlType(param);
			stmt.setObject(i + 1, param, sqlType);
		}
	}

	private static int deriveSqlType(final Object param) {
		// NB: So far this is not exhaustive, but previously only INTEGER and VARCHAR were supported, so for now this should do.
		int sqlType;
		if (param instanceof Integer) {
			sqlType = Types.INTEGER;
		} else if (param instanceof Long) {
			sqlType = Types.BIGINT;
		} else if (param instanceof Float) {
			sqlType = Types.NUMERIC;
		} else if (param instanceof Double) {
			sqlType = Types.NUMERIC;
		} else if (param instanceof Timestamp) {
			sqlType = Types.TIMESTAMP;
		} else if (param instanceof Time) {
			sqlType = Types.TIME;
		} else if (param instanceof java.sql.Date) {
			sqlType = Types.DATE;
		} else {
			sqlType = Types.VARCHAR;
		}
		return sqlType;
	}

	/**
	 * executes query that returns a string. Returns null if no results are found.
	 */
	public static String executeStringQuery(Connection connection, String query, Object... params) throws JdbcException {
		if (log.isDebugEnabled()) log.debug("prepare and execute query [" + query + "]" + displayParameters(params));
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			applyParameters(stmt, params);
			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return rs.getString(1);
			}
		} catch (Exception e) {
			throw new JdbcException("could not obtain value using query [" + query + "]" + displayParameters(params), e);
		}
	}

	public static String executeBlobQuery(IDbmsSupport dbmsSupport, Connection connection, String query) throws JdbcException {
		if (log.isDebugEnabled()) log.debug("prepare and execute query ["+query+"]");
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return getBlobAsString(dbmsSupport, rs, 1, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING, true, true, false);
			}
		} catch (Exception e) {
			throw new JdbcException("could not obtain value using query [" + query + "]", e);
		}
	}

	/**
	 * executes query that returns an integer. Returns -1 if no results are found.
	 */
	public static int executeIntQuery(Connection connection, String query, Object... params) throws JdbcException {
		if (log.isDebugEnabled()) log.debug("prepare and execute query [" + query + "]" + displayParameters(params));
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			applyParameters(stmt, params);
			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next()) {
					return -1;
				}
				return rs.getInt(1);
			}
		} catch (Exception e) {
			throw new JdbcException("could not obtain value using query [" + query + "]" + displayParameters(params), e);
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

	public static void executeStatement(Connection connection, String query, Object... params) throws JdbcException {
		if (log.isDebugEnabled()) log.debug("prepare and execute query [" + query + "]" + displayParameters(params));
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			applyParameters(stmt, params);
			stmt.execute();
		} catch (Exception e) {
			throw new JdbcException("could not execute query [" + query + "]" + displayParameters(params), e);
		}
	}

	public static String selectAllFromTable(IDbmsSupport dbmsSupport, Connection conn, String tableName, String orderBy) throws SQLException {
		String query = "select * from " + tableName + (orderBy != null ? " ORDER BY " + orderBy : "");
		try (PreparedStatement stmt = conn.prepareStatement(query)) {
			try (ResultSet rs = stmt.executeQuery()) {
				DB2XMLWriter db2xml = new DB2XMLWriter();
				return db2xml.getXML(dbmsSupport, rs);
			}
		}
	}

	public static void executeStatement(IDbmsSupport dbmsSupport, Connection connection, String query, ParameterValueList parameterValues, PipeLineSession session) throws JdbcException {
		if (log.isDebugEnabled()) log.debug("prepare and execute query [" + query + "]" + displayParameters(parameterValues));
		try {
			PreparedStatement stmt = connection.prepareStatement(query);
			applyParameters(dbmsSupport, stmt, parameterValues, session);
			stmt.execute();
		} catch (Exception e) {
			throw new JdbcException("could not execute query [" + query + "]" + displayParameters(parameterValues), e);
		}
	}

	public static Object executeQuery(IDbmsSupport dbmsSupport, Connection connection, String query, ParameterValueList parameterValues, PipeLineSession session) throws JdbcException {
		if (log.isDebugEnabled()) log.debug("prepare and execute query [" + query + "]" + displayParameters(parameterValues));
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			applyParameters(dbmsSupport, stmt, parameterValues, session);
			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				int columnsCount = rs.getMetaData().getColumnCount();
				if (columnsCount == 1) {
					return rs.getObject(1);
				}
				List<Object> resultList = new ArrayList<>();
				for (int i = 1; i <= columnsCount; i++) {
					resultList.add(rs.getObject(i));
				}
				return resultList;
			}
		} catch (Exception e) {
			throw new JdbcException("could not obtain value using query [" + query + "]" + displayParameters(parameterValues), e);
		}
	}

	private static String displayParameters(ParameterValueList parameterValues) {
		if (parameterValues == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parameterValues.size(); i++) {
			sb.append("param").append(i)
					.append(" [")
					.append(parameterValues.getParameterValue(i).getValue())
					.append("]");
		}
		return sb.toString();
	}

	public static void applyParameters(IDbmsSupport dbmsSupport, PreparedStatement statement, ParameterList parameters, Message message, PipeLineSession session) throws SQLException, JdbcException, ParameterException {
		if (parameters != null) {
			applyParameters(dbmsSupport,statement, parameters.getValues(message, session), session);
		}
	}

	public static void applyParameters(IDbmsSupport dbmsSupport, PreparedStatement statement, ParameterValueList parameters, PipeLineSession session) throws SQLException, JdbcException {
		boolean parameterTypeMatchRequired = dbmsSupport.isParameterTypeMatchRequired();
		if (parameters != null) {
			for (int i = 0; i < parameters.size(); i++) {
				applyParameter(statement, parameters.getParameterValue(i), i + 1, parameterTypeMatchRequired, session);
			}
		}
	}


	private static void applyParameter(PreparedStatement statement, ParameterValue pv, int parameterIndex, boolean parameterTypeMatchRequired, PipeLineSession session) throws SQLException, JdbcException {
		String paramName=pv.getDefinition().getName();
		ParameterType paramType = pv.getDefinition().getType();
		Object value = pv.getValue();
		if (log.isDebugEnabled()) log.debug("jdbc parameter ["+parameterIndex+"] applying parameter ["+paramName+"] type ["+paramType+"] value ["+value+"]");
		switch(paramType) {
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
			//noinspection deprecation
			case INPUTSTREAM:
			case BINARY:
				try {
					Message message = Message.asMessage(value);
					message.closeOnCloseOf(session, "JDBC Blob Parameter");
					long len = message.size();
					if(message.requiresStream()) {
						if(len != -1) {
							statement.setBinaryStream(parameterIndex, message.asInputStream(), len);
						} else {
							statement.setBinaryStream(parameterIndex, message.asInputStream());
						}
					}
					else {
						statement.setBytes(parameterIndex, message.asByteArray());
					}
				} catch(IOException e) {
					throw new JdbcException("applying the parameter ["+paramName+"] failed", e);
				}
				break;
			case CHARACTER:
				try {
					Message message = Message.asMessage(value);
					message.closeOnCloseOf(session, "JDBC Clob Parameter");
					long len = message.size();
					if(message.requiresStream()) {
						if(len != -1) {
							statement.setCharacterStream(parameterIndex, message.asReader(), len);
						} else {
							statement.setCharacterStream(parameterIndex, message.asReader());
						}
					}
					else {
						statement.setString(parameterIndex, message.asString());
					}
				} catch(IOException e) {
					throw new JdbcException("applying the parameter ["+paramName+"] failed", e);
				}
				break;
			//noinspection deprecation
			case BYTES:
				try {
					statement.setBytes(parameterIndex, Message.asByteArray(value));
				} catch (IOException e) {
					throw new JdbcException("Failed to get bytes for the parameter ["+paramName+"]", e);
				}
				break;
			default:
				try {
					Message message = Message.asMessage(value);
					message.closeOnCloseOf(session, "JDBC Parameter");
					setParameter(statement, parameterIndex, message.asString(), parameterTypeMatchRequired);
				} catch (IOException e) {
					throw new JdbcException("applying the parameter ["+paramName+"] failed", e);
				}
		}
	}

	public static void setParameter(PreparedStatement statement, int parameterIndex, String value, boolean parameterTypeMatchRequired) throws SQLException {
		if (!parameterTypeMatchRequired) {
			statement.setString(parameterIndex, value);
			return;
		}
		int sqlTYpe=statement.getParameterMetaData().getParameterType(parameterIndex);
		try {
			switch(sqlTYpe) {
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
				statement.setDate(parameterIndex, new java.sql.Date(DateUtils.parseAnyDate(value).getTime()));
				break;
			case Types.TIMESTAMP:
				statement.setTimestamp(parameterIndex, new Timestamp(DateUtils.parseAnyDate(value).getTime()));
				break;
			default:
				log.warn("parameter type ["+JDBCType.valueOf(sqlTYpe).getName()+"] handled as String");
				//$FALL-THROUGH$
			case Types.CHAR:
			case Types.VARCHAR:
				statement.setString(parameterIndex, value);
				break;
			}
		} catch (CalendarParserException e) { // thrown by parseAnyDate in case DATE and TIMESTAMP
			throw new SQLException("Could not convert [" + value + "] for parameter [" + parameterIndex + "]", e);
		}
	}

	public static boolean isNumeric(int sqlTYpe) {
		switch(sqlTYpe) {
		case Types.INTEGER:
		case Types.NUMERIC:
		case Types.DOUBLE:
		case Types.BIGINT:
		case Types.DECIMAL:
		case Types.FLOAT:
		case Types.REAL:
		case Types.SMALLINT:
			return true;
		default:
			return false;
		}
	}

	@Deprecated
	private static OutputStream getOutputStream(Object target) throws IOException {
		if (target instanceof OutputStream) {
			return (OutputStream) target;
		}
		if (target instanceof String) {
			return getFileOutputStream((String)target);
		}
		if (target instanceof Message) {
			if(((Message) target).asObject() instanceof String) {
				return getFileOutputStream(((Message)target).asString());
			}
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
			FileNotFoundException fnfe = new FileNotFoundException("cannot create file ["+filename+"]");
			fnfe.initCause(e);
			throw fnfe;
		}
	}
}
