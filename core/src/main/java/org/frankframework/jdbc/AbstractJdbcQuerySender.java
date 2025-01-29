/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.jms.JMSException;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.dbms.DbmsException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterType;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.Base64Pipe;
import org.frankframework.pipes.Base64Pipe.Direction;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DB2DocumentWriter;
import org.frankframework.util.DB2XMLWriter;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * This executes the query that is obtained from the (here still abstract) method getStatement.
 * Descendant classes can override getStatement to provide meaningful statements.
 * If used with parameters, the values of the parameters will be applied to the statement.
 * Each occurrence of a questionmark ('?') will be replaced by a parameter value. Parameters are applied
 * in order: The n-th questionmark is replaced by the value of the n-th parameter.
 *
 * <h3>Note on using packages</h3>
 * The package processor makes some assumptions about the data types:
 * <ul>
 *   <li>elements that start with a single quote are assumed to be Strings</li>
 *   <li>elements that contain a dash ('-') are assumed to be dates (yyyy-MM-dd) or timestamps (yyyy-MM-dd HH:mm:ss)</li>
 *   <li>elements containing a dot ('.') are assumed to be floats</li>
 *   <li>all other elements are assumed to be integers</li>
 * </ul>
 * </p>
 *
 * Queries that return no data (queryType 'other') return a message indicating the number of rows processed
 *
 * @ff.parameters all parameters present are applied to the statement to be executed
 *
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public abstract class AbstractJdbcQuerySender<H> extends AbstractJdbcSender<H> {

	public static final String UNP_START = "?{";
	public static final String UNP_END = "}";

	private @Getter QueryType queryType = QueryType.OTHER;
	private @Getter int maxRows=-1; // return all rows
	private @Getter int startRow=1;
	private @Getter boolean scalar=false;
	private @Getter boolean scalarExtended=false;
	private @Getter boolean synchronous=true;
	private @Getter int blobColumn=1;
	private @Getter int clobColumn=1;
	private @Getter String blobSessionKey=null;
	private @Getter String clobSessionKey=null;
	private @Getter String nullValue="";
	private @Getter String columnsReturned=null;
	private @Getter String resultQuery=null;
	private @Getter boolean trimSpaces=true;
	// 2020-11-18: blobCharset is set to null! Clobs are for character data, blobs for binary. When blobs contain character data,
	// blobCharset can be set to "UTF-8", or set blobBase64Direction to 'encode'.
	// By default, BLOBs are no longer read as strings
	private @Getter String blobCharset = null;
	private @Getter boolean closeInputstreamOnExit=true;
	private @Getter boolean closeOutputstreamOnExit=true;
	private @Getter Base64Pipe.Direction blobBase64Direction=null;
	private @Getter String streamCharset = null;
	private @Getter boolean blobsCompressed=true;
	private @Getter boolean blobSmartGet=false;
	private @Getter Boolean useNamedParams=null;
	private @Getter boolean includeFieldDefinition=XmlUtils.isIncludeFieldDefinitionByDefault();
	private @Getter String rowIdSessionKey=null;
	private @Getter String packageContent = "db2";
	private @Getter String[] columnsReturnedList=null;
	private @Getter boolean streamResultToServlet = false; //TODO: remove stream result to servlet
	private @Getter String sqlDialect = AppConstants.getInstance().getString("jdbc.sqlDialect", null);
	private @Getter boolean lockRows=false;
	private @Getter int lockWait=-1;
	private @Getter boolean avoidLocking=false;
	private @Getter DocumentFormat outputFormat=null;
	private @Getter boolean prettyPrint=false;

	private String convertedResultQuery;

	public enum QueryType {
		/** For queries that return data */
		SELECT,
		/** For queries that update a BLOB */
		UPDATEBLOB,
		/** For queries that update a CLOB */
		UPDATECLOB,
		/** To execute Oracle PL/SQL package */
		PACKAGE,
		/** For queries that return no data */
		OTHER,
		/** Deprecated: Use OTHER instead */
		@ConfigurationWarning("Use queryType 'OTHER' instead")
		@Deprecated(since = "8.1") INSERT,
		/** Deprecated: Use OTHER instead */
		@ConfigurationWarning("Use queryType 'OTHER' instead")
		@Deprecated(since = "8.1") DELETE,
		/** Deprecated: Use OTHER instead */
		@ConfigurationWarning("Use queryType 'OTHER' instead")
		@Deprecated(since = "8.1") UPDATE
	}

	@Override
	public void configure() throws ConfigurationException {
		if (!BooleanUtils.isFalse(getUseNamedParams())) { // only allow duplicate parameter names if useNamedParams is set explicitly to false.
			getParameterList().setNamesMustBeUnique(true);
		}
		super.configure();
		if (StringUtils.isNotEmpty(getColumnsReturned())) {
			// don't call StringUtil.split(*) b/c we want an array, not a list.
			columnsReturnedList = getColumnsReturned().trim().split("\\s*,+\\s*");
		}
		if (getBatchSize()>0 && getQueryType() != QueryType.OTHER) {
			throw new ConfigurationException("batchSize>0 only valid for queryType 'other'");
		}
	}

	/**
	 * Obtain a query to be executed.
	 * Method-stub to be overridden in descender-classes.
	 */
	protected abstract String getQuery(Message message) throws SenderException;

	@Override
	public void start() {
		super.start();
		if (StringUtils.isNotEmpty(getResultQuery())) {
			try {
				convertedResultQuery = convertQuery(getResultQuery());
				if (log.isDebugEnabled()) log.debug("converted result query into [{}]", convertedResultQuery);
			} catch (JdbcException | SQLException e) {
				throw new LifecycleException("Cannot convert result query",e);
			}
		}
	}

	@Nonnull
	protected String convertQuery(@Nonnull String query) throws SQLException, DbmsException {
		if (!StringUtils.isNotEmpty(getSqlDialect()) || getSqlDialect().equalsIgnoreCase(getDbmsSupport().getDbmsName())) {
			return query;
		}
		if (log.isDebugEnabled()) {
			log.debug("converting query [{}] from [{}] to [{}]", query::trim, this::getSqlDialect, () -> getDbmsSupport().getDbmsName());
		}
		return getDbmsSupport().convertQuery(query, getSqlDialect());
	}

	protected final PreparedStatement getStatement(@Nonnull Connection con, @Nonnull String query, @Nullable QueryType queryType) throws JdbcException, SQLException {
		PreparedStatement preparedStatement = prepareQuery(con, query, queryType);
		preparedStatement.setQueryTimeout(getTimeout());
		return preparedStatement;
	}

	protected PreparedStatement prepareQuery(@Nonnull Connection con, @Nonnull String query, @Nullable QueryType queryType) throws SQLException, JdbcException {
		String adaptedQuery = convertQuery(query);
		if (isLockRows()) {
			adaptedQuery = getDbmsSupport().prepareQueryTextForWorkQueueReading(-1, adaptedQuery, getLockWait());
		}
		if (isAvoidLocking()) {
			adaptedQuery = getDbmsSupport().prepareQueryTextForNonLockingRead(adaptedQuery);
		}
		if (log.isDebugEnabled()) {
			log.debug("preparing statement for query [{}]", adaptedQuery);
		}
		String[] columnsReturned = getColumnsReturnedList();
		if (columnsReturned != null) {
			return prepareQueryWithColumnsReturned(con, adaptedQuery, columnsReturned);
		}
		boolean resultSetUpdatable = isLockRows() || queryType == QueryType.UPDATEBLOB || queryType == QueryType.UPDATECLOB;
		int resultSetConcurrency = resultSetUpdatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY;
		return prepareQueryWithResultSet(con, adaptedQuery, resultSetConcurrency);
	}

	protected PreparedStatement prepareQueryWithResultSet(final Connection con, final String query, final int resultSetConcurrency) throws SQLException {
		return con.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, resultSetConcurrency);
	}

	protected PreparedStatement prepareQueryWithColumnsReturned(Connection con, String query, String[] columnsReturned) throws SQLException {
		return con.prepareStatement(query, columnsReturned);
	}

	protected CallableStatement getCallWithRowIdReturned(Connection con, String query) throws SQLException {
		String callQuery = "BEGIN " + query + " RETURNING ROWID INTO ?; END;";
		log.debug("preparing statement for query [{}]", () -> callQuery);
		CallableStatement callableStatement = con.prepareCall(callQuery);
		callableStatement.setQueryTimeout(getTimeout());
		return callableStatement;
	}

	protected ResultSet getReturnedColumns(PreparedStatement st) throws SQLException {
		return st.getGeneratedKeys();
	}

	public QueryExecutionContext getQueryExecutionContext(Connection connection, Message message) throws SenderException, SQLException, JdbcException {
		ParameterList newParameterList = new ParameterList(paramList);
		String query = getQuery(message);
		if (BooleanUtils.isTrue(getUseNamedParams()) || (getUseNamedParams() == null && query.contains(UNP_START))) {
			query = adjustQueryAndParameterListForNamedParameters(newParameterList, query);
		}
		log.debug("obtaining prepared statement to execute");
		PreparedStatement statement = getStatement(connection, query, getQueryType());
		log.debug("obtained prepared statement to execute");
		PreparedStatement resultQueryStatement;
		if (convertedResultQuery != null) {
			resultQueryStatement = connection.prepareStatement(convertedResultQuery);
			resultQueryStatement.setQueryTimeout(getTimeout());
		} else {
			resultQueryStatement = null;
		}
		return new QueryExecutionContext(query, convertedResultQuery, getQueryType(), newParameterList, connection, statement, resultQueryStatement);
	}


	protected Connection getConnectionForSendMessage() throws JdbcException, TimeoutException {
		if (isConnectionsArePooled()) {
			return getConnectionWithTimeout(getTimeout());
		}
		return connection;
	}

	protected void closeConnectionForSendMessage(Connection connection, PipeLineSession session) {
		if (isConnectionsArePooled() && connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				log.warn(new SenderException("caught exception closing sender after sending message, ID=["+(session==null?null:session.getMessageId())+"]", e));
			}
		}
	}

	protected void closeStatementSet(QueryExecutionContext queryExecutionContext) {
		try (PreparedStatement statement = queryExecutionContext.getStatement()) {
			if (getBatchSize()>0) {
				statement.executeBatch();
			}
		} catch (SQLException e) {
			log.warn("got exception closing SQL statement", e);
		}
		//noinspection EmptyTryBlock
		try (Statement statement = queryExecutionContext.getResultQueryStatement()) {
			// only close statement
		} catch (SQLException e) {
			log.warn("got exception closing result SQL statement", e);
		}
	}

	protected SenderResult executeStatementSet(@Nonnull QueryExecutionContext queryExecutionContext, @Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		try {
			PreparedStatement statement=queryExecutionContext.getStatement();
			JdbcUtil.applyParameters(getDbmsSupport(), statement, queryExecutionContext.getParameterList(), message, session);
			switch(queryExecutionContext.getQueryType()) {
				case SELECT:
					Object blobSessionVar = null;
					Object clobSessionVar = null;
					if (StringUtils.isNotEmpty(getBlobSessionKey())) {
						//noinspection deprecation
						blobSessionVar = session.getMessage(getBlobSessionKey()).asObject();
					}
					if (StringUtils.isNotEmpty(getClobSessionKey())) {
						//noinspection deprecation
						clobSessionVar = session.getMessage(getClobSessionKey()).asObject();
					}
					if (isStreamResultToServlet()) {
						HttpServletResponse response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);
						String contentType = session.getString("contentType");
						String contentDisposition = session.getString("contentDisposition");
						return executeSelectQuery(statement,blobSessionVar,clobSessionVar, response, contentType, contentDisposition);
					} else {
						return executeSelectQuery(statement,blobSessionVar,clobSessionVar);
					}
				case UPDATEBLOB:
					if (StringUtils.isNotEmpty(getBlobSessionKey())) {
						return new SenderResult(executeUpdateBlobQuery(statement, session.getMessage(getBlobSessionKey())));
					}
					return new SenderResult(executeUpdateBlobQuery(statement, message));
				case UPDATECLOB:
					if (StringUtils.isNotEmpty(getClobSessionKey())) {
						return new SenderResult(executeUpdateClobQuery(statement, session.getMessage(getClobSessionKey())));
					}
					return new SenderResult(executeUpdateClobQuery(statement, message));
				case PACKAGE:
					return new SenderResult(executePackageQuery(queryExecutionContext));
				case OTHER:
					Message result = executeOtherQuery(queryExecutionContext, message, session);
					if (getBatchSize()>0 && ++queryExecutionContext.iteration>=getBatchSize()) {
						int[] results=statement.executeBatch();
						int numRowsAffected = Arrays.stream(results).sum();
						result = new Message("<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>");
						statement.clearBatch();
						queryExecutionContext.iteration=0;
					}
					return new SenderResult(result);
				default:
					throw new IllegalStateException("Unsupported queryType: ["+queryExecutionContext.getQueryType()+"]");
			}
		} catch (SenderException e) {
			if (e.getCause() instanceof SQLException sqle && sqle.getErrorCode() == 1013) {
				throw new TimeoutException("Timeout of ["+getTimeout()+"] sec expired");
			}
			throw new SenderException(e);
		} catch (Throwable t) {
			throw new SenderException("got exception sending message", t);
		} finally {
			closeStatementSet(queryExecutionContext);
			ParameterList newParameterList = queryExecutionContext.getParameterList();
			if (isCloseInputstreamOnExit() && newParameterList != null) {
				//noinspection deprecation
				newParameterList.stream()
						.filter(param -> param.getType() == ParameterType.INPUTSTREAM)
						.forEach(param -> closeParameterInputStream(param, message, session));
			}
		}
	}

	private void closeParameterInputStream(IParameter param, Message message, PipeLineSession session) {
		log.debug("Closing inputstream for parameter [{}]", param::getName);
		try {
			Object object = param.getValue(null, message, session, true);
			if (object instanceof AutoCloseable closeable) {
				closeable.close();
			} else {
				log.error("unable to auto-close parameter [{}]", param::getName);
			}
		} catch (Exception e) {
			log.warn(new SenderException("got exception closing inputstream", e));
		}
	}

	protected String adjustQueryAndParameterListForNamedParameters(ParameterList parameterList, String query) {
		if (log.isDebugEnabled()) {
			log.debug("Adjusting list of parameters [{}]", ()->parameterListToString(parameterList));
		}

		StringBuilder buffer = new StringBuilder();
		int startPos = query.indexOf(UNP_START);
		if (startPos == -1)
			return query;
		char[] messageChars = query.toCharArray();
		int copyFrom = 0;
		ParameterList oldParameterList = new ParameterList(parameterList);
		parameterList.clear();
		while (startPos != -1) {
			buffer.append(messageChars, copyFrom, startPos - copyFrom);
			int nextStartPos = query.indexOf(UNP_START, startPos + UNP_START.length());
			if (nextStartPos == -1) {
				nextStartPos = query.length();
			}
			int endPos = query.indexOf(UNP_END, startPos + UNP_START.length());

			if (endPos == -1 || endPos > nextStartPos) {
				log.warn("Found a start delimiter without an end delimiter at position [{}] in [{}]", startPos, query);
				buffer.append(messageChars, startPos, nextStartPos - startPos);
				copyFrom = nextStartPos;
			} else {
				String namedParam = query.substring(startPos + UNP_START.length(),endPos);
				IParameter param = oldParameterList.findParameter(namedParam);
				if (param != null) {
					parameterList.add(param);
					buffer.append("?");
					copyFrom = endPos + UNP_END.length();
				} else {
					log.warn("Parameter [{}] is not found", namedParam);
					buffer.append(messageChars, startPos, nextStartPos - startPos);
					copyFrom = nextStartPos;
				}
			}
			startPos = query.indexOf(UNP_START, copyFrom);
		}
		buffer.append(messageChars, copyFrom, messageChars.length - copyFrom);

		if (log.isDebugEnabled()) {
			log.debug( "Adjusted list of parameters [{}]", ()->parameterListToString(parameterList));
		}

		return buffer.toString();
	}

	private String parameterListToString(ParameterList parameterList) {
		return parameterList.stream()
				.map(IParameter::getName)
				.collect(Collectors.joining(", "));
	}

	protected Message getResult(ResultSet resultset) throws JdbcException, SQLException, IOException, JMSException {
		return getResult(resultset,null,null);
	}

	protected Message getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar) throws JdbcException, SQLException, IOException {
		return getResult(resultset, blobSessionVar, clobSessionVar, null, null, null);
	}

	protected Message getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar, HttpServletResponse response, String contentType, String contentDisposition) throws JdbcException, SQLException, IOException {
		if (isScalar()) {
			String result=null;
			if (resultset.next()) {
				ResultSetMetaData rsmeta = resultset.getMetaData();
				int numberOfColumns = rsmeta.getColumnCount();
				if(numberOfColumns > 1) {
					log.warn("has set scalar=true but the resultset contains [{}] columns. Consider optimizing the query.", numberOfColumns);
				}
				if (getDbmsSupport().isBlobType(rsmeta, 1)) {
					if (response!=null) {
						if (StringUtils.isNotEmpty(contentType)) {
							response.setHeader("Content-Type", contentType);
						}
						if (StringUtils.isNotEmpty(contentDisposition)) {
							response.setHeader("Content-Disposition", contentDisposition);
						}
						JdbcUtil.streamBlob(getDbmsSupport(), resultset, 1, getBlobCharset(), isBlobsCompressed(), getBlobBase64Direction(), response.getOutputStream(), isCloseOutputstreamOnExit());
						return Message.nullMessage();
					}
					if (blobSessionVar!=null) {
						JdbcUtil.streamBlob(getDbmsSupport(), resultset, 1, getBlobCharset(), isBlobsCompressed(), getBlobBase64Direction(), blobSessionVar, isCloseOutputstreamOnExit());
						return Message.nullMessage();
					}
					if (!isBlobSmartGet()) {
						MessageBuilder messageBuilder = new MessageBuilder();
						if (StringUtils.isNotEmpty(getBlobCharset())) {
							JdbcUtil.streamBlob(getDbmsSupport(), resultset, 1, getBlobCharset(), isBlobsCompressed(), getBlobBase64Direction(), messageBuilder.asWriter(), isCloseOutputstreamOnExit());
						} else {
							JdbcUtil.streamBlob(getDbmsSupport(), resultset, 1, null, isBlobsCompressed(), getBlobBase64Direction(), messageBuilder.asOutputStream(), isCloseOutputstreamOnExit());
						}
						return messageBuilder.build();
					}
				}
				if (getDbmsSupport().isClobType(rsmeta, 1)) {
					if (clobSessionVar!=null) {
						JdbcUtil.streamClob(getDbmsSupport(), resultset, 1, clobSessionVar, isCloseOutputstreamOnExit());
						return Message.nullMessage();
					}
					MessageBuilder messageBuilder = new MessageBuilder();
					JdbcUtil.streamClob(getDbmsSupport(), resultset, 1, messageBuilder.asWriter(), isCloseOutputstreamOnExit());
					return messageBuilder.build();
				}
				result = JdbcUtil.getValue(getDbmsSupport(), resultset, 1, rsmeta, getBlobCharset(), isBlobsCompressed(), getNullValue(), isTrimSpaces(), isBlobSmartGet(), getBlobBase64Direction() == Direction.ENCODE);
				if (resultset.wasNull()) {
					if (isScalarExtended()) {
						result = "[null]";
					} else {
						result = null;
					}
				} else {
					if (result.isEmpty() && isScalarExtended()) {
						result="[empty]";
					}
				}
				if (resultset.next()) {
					log.warn("has set scalar=true but the query returned more than 1 row. Consider optimizing the query.");
				}
			} else if (isScalarExtended()) {
					result="[absent]";
			}
			return new Message(result);
		}
		try {
			MessageBuilder messageBuilder = new MessageBuilder();
			// Create XML and give the maxlength as a parameter
			if (getOutputFormat()==null) {
				DB2XMLWriter db2xml = buildDb2XMLWriter();
				db2xml.getXML(getDbmsSupport(), resultset, getMaxRows(), isIncludeFieldDefinition(), messageBuilder.asXmlWriter(), isPrettyPrint());
			} else {
				DB2DocumentWriter db2document = new DB2DocumentWriter();
				db2document.setNullValue(getNullValue());
				db2document.setTrimSpaces(isTrimSpaces());
				if (StringUtils.isNotEmpty(getBlobCharset())) db2document.setBlobCharset(getBlobCharset());
				db2document.setDecompressBlobs(isBlobsCompressed());
				db2document.setGetBlobSmart(isBlobSmartGet());
				db2document.writeDocument(getOutputFormat(), getDbmsSupport(), resultset, getMaxRows(), isIncludeFieldDefinition(), messageBuilder, isPrettyPrint());
			}
			return messageBuilder.build();
		} catch (Exception e) {
			throw new JdbcException(e);
		}
	}

	protected DB2XMLWriter buildDb2XMLWriter() {
		DB2XMLWriter db2xml = new DB2XMLWriter();
		db2xml.setNullValue(getNullValue());
		db2xml.setTrimSpaces(isTrimSpaces());
		if (StringUtils.isNotEmpty(getBlobCharset())) db2xml.setBlobCharset(getBlobCharset());
		db2xml.setDecompressBlobs(isBlobsCompressed());
		db2xml.setGetBlobSmart(isBlobSmartGet());
		return db2xml;
	}


	private BlobOutputStream getBlobOutputStream(PreparedStatement statement, int blobColumn, boolean compressBlob) throws SQLException, JdbcException {
		log.debug("executing an update BLOB command");
		ResultSet rs = statement.executeQuery();
		XmlBuilder result=new XmlBuilder("result");
		JdbcUtil.warningsToXml(statement.getWarnings(),result);
		rs.next();
		Object blobUpdateHandle=getDbmsSupport().getBlobHandle(rs, blobColumn);
		OutputStream dbmsOutputStream = JdbcUtil.getBlobOutputStream(getDbmsSupport(), blobUpdateHandle, rs, blobColumn, compressBlob);
		return new BlobOutputStream(getDbmsSupport(), blobUpdateHandle, blobColumn, dbmsOutputStream, rs, result);
	}

	protected Message executeUpdateBlobQuery(PreparedStatement statement, Message contents) throws SenderException{
		BlobOutputStream blobOutputStream=null;
		try {
			try {
				blobOutputStream = getBlobOutputStream(statement, blobColumn, isBlobsCompressed());
				if (contents!=null) {
					if (StringUtils.isNotEmpty(getStreamCharset())) {
						contents = new Message(contents.asReader(getStreamCharset()));
					}
					InputStream inputStream = contents.asInputStream(getBlobCharset());
					if (!isCloseInputstreamOnExit()) {
						inputStream = StreamUtil.dontClose(inputStream);
					}
					StreamUtil.streamToStream(inputStream,blobOutputStream);
				}
			} finally {
				if (blobOutputStream!=null) {
					blobOutputStream.close();
				}
			}
		} catch (SQLException|JdbcException|IOException e) {
			throw new SenderException("got exception executing an update BLOB command", e);
		}
		return blobOutputStream.getWarnings().asMessage();
	}

	private ClobWriter getClobWriter(PreparedStatement statement, int clobColumn) throws SQLException, JdbcException {
		log.debug("executing an update CLOB command");
		ResultSet rs = statement.executeQuery();
		XmlBuilder result=new XmlBuilder("result");
		JdbcUtil.warningsToXml(statement.getWarnings(),result);
		rs.next();
		Object clobUpdateHandle=getDbmsSupport().getClobHandle(rs, clobColumn);
		Writer dbmsWriter = getDbmsSupport().getClobWriter(rs, clobColumn, clobUpdateHandle);
		return new ClobWriter(getDbmsSupport(), clobUpdateHandle, clobColumn, dbmsWriter, rs, result);
	}

	protected Message executeUpdateClobQuery(PreparedStatement statement, Message contents) throws SenderException{
		ClobWriter clobWriter=null;
		try {
			try {
				clobWriter = getClobWriter(statement, getClobColumn());
				if (contents!=null) {
					Reader reader = contents.asReader(getStreamCharset());
					if (!isCloseInputstreamOnExit()) {
						reader = StreamUtil.dontClose(reader);
					}
					StreamUtil.readerToWriter(reader, clobWriter);
				}
			} finally {
				if (clobWriter!=null) {
					clobWriter.close();
				}
			}
		} catch (SQLException|JdbcException|IOException e) {
			throw new SenderException("got exception executing an update CLOB command", e);
		}
		return clobWriter.getWarnings().asMessage();
	}

	protected SenderResult executeSelectQuery(PreparedStatement statement, Object blobSessionVar, Object clobSessionVar) throws SenderException{
		return executeSelectQuery(statement, blobSessionVar, clobSessionVar, null, null, null);
	}

	private SenderResult executeSelectQuery(PreparedStatement statement, Object blobSessionVar, Object clobSessionVar, HttpServletResponse response, String contentType, String contentDisposition) throws SenderException{
		try {
			if (getMaxRows()>0) {
				statement.setMaxRows(getMaxRows()+ ( getStartRow()>1 ? getStartRow()-1 : 0));
			}

			log.debug("executing a SELECT SQL command");
			try (ResultSet resultset = statement.executeQuery()) {
				if (getStartRow()>1) {
					resultset.absolute(getStartRow()-1);
					log.debug("Index set at position: {}", resultset.getRow());
				}
				return new SenderResult(getResult(resultset, blobSessionVar, clobSessionVar, response, contentType, contentDisposition));
			}
		} catch (SQLException|JdbcException|IOException e) {
			throw new SenderException("got exception executing a SELECT SQL command", e );
		}
	}

	protected Message executePackageQuery(QueryExecutionContext queryExecutionContext) throws SenderException, JdbcException, IOException, JMSException {
		Connection connection = queryExecutionContext.getConnection();
		String query = queryExecutionContext.getQuery();
		Object[] paramArray = new Object[10];
		String callMessage = fillParamArray(paramArray, query);
		try (CallableStatement pstmt = connection.prepareCall(callMessage)) {
			if (getMaxRows() > 0) {
				pstmt.setMaxRows(
					getMaxRows() + (getStartRow() > 1 ? getStartRow() - 1 : 0));
			}
			int parameterIndex = 1;
			for (final Object o : paramArray) {
				if (o instanceof Timestamp timestamp) {
					pstmt.setTimestamp(parameterIndex, timestamp);
					parameterIndex++;
				}
				if (o instanceof java.sql.Date date) {
					pstmt.setDate(parameterIndex, date);
					parameterIndex++;
				}
				if (o instanceof String string) {
					pstmt.setString(parameterIndex, string);
					parameterIndex++;
				}
				if (o instanceof Integer) {
					int x = Integer.parseInt(o.toString());
					pstmt.setInt(parameterIndex, x);
					parameterIndex++;
				}
				if (o instanceof Float) {
					float x = Float.parseFloat(o.toString());
					pstmt.setFloat(parameterIndex, x);
					parameterIndex++;
				}
			}
			if (query.indexOf('?') != -1) {
				pstmt.registerOutParameter(parameterIndex, Types.CLOB); // make sure enough space is available for result...
			}
			if ("xml".equalsIgnoreCase(getPackageContent())) {
				log.debug("executing a package SQL command");
				pstmt.executeUpdate();
				String pUitvoer = pstmt.getString(parameterIndex);
				return new Message(pUitvoer);
			}
			log.debug("executing a package SQL command");
			int numRowsAffected = pstmt.executeUpdate();
			if (queryExecutionContext.getResultQueryStatement() != null) {
				PreparedStatement resStmt = queryExecutionContext.getResultQueryStatement();
				if (log.isDebugEnabled()) log.debug("obtaining result from [{}]", queryExecutionContext.getResultQuery());
				ResultSet rs = resStmt.executeQuery();
				return getResult(rs);
			}
			if (getColumnsReturnedList() != null) {
				return getResult(getReturnedColumns(queryExecutionContext.getStatement()));
			}
			if (isScalar()) {
				return new Message(Integer.toString(numRowsAffected));
			}
			return new Message("<result><rowsupdated>"+ numRowsAffected	+ "</rowsupdated></result>");
		} catch (SQLException sqle) {
			throw new SenderException("got exception executing a package SQL command", sqle);
		}
	}

	protected Message executeOtherQuery(QueryExecutionContext queryExecutionContext, Message message, PipeLineSession session) throws SenderException {
		Connection connection = queryExecutionContext.getConnection();
		PreparedStatement statement = queryExecutionContext.getStatement();
		String query = queryExecutionContext.getQuery();
		ParameterList parameterList = queryExecutionContext.getParameterList();
		PreparedStatement resStmt = queryExecutionContext.getResultQueryStatement();
		return executeOtherQuery(connection, statement, query, queryExecutionContext.getResultQuery(), resStmt, message, session, parameterList);
	}

	protected Message executeOtherQuery(@Nonnull Connection connection, @Nonnull PreparedStatement statement, @Nonnull String query, @Nullable String resultQuery, @Nullable PreparedStatement resStmt, @Nullable Message message, @Nullable PipeLineSession session, @Nullable ParameterList parameterList) throws SenderException {
		try {
			int numRowsAffected = 0;
			if (StringUtils.isNotEmpty(getRowIdSessionKey())) {
				try (CallableStatement cstmt = getCallWithRowIdReturned(connection, query)) {
					int ri = 1;
					if (parameterList != null) {
						ParameterValueList parameters = parameterList.getValues(message, session);
						JdbcUtil.applyParameters(getDbmsSupport(), cstmt, parameters, session);
						ri = parameters.size() + 1;
					}
					cstmt.registerOutParameter(ri, Types.VARCHAR);
					log.debug("executing a SQL command");
					numRowsAffected = cstmt.executeUpdate();
					String rowId = cstmt.getString(ri);
					if (session!=null) session.put(getRowIdSessionKey(), rowId);
				}
			} else {
				log.debug("executing a SQL command");
				if (getBatchSize() > 0) {
					statement.addBatch();
				} else {
					numRowsAffected = statement.executeUpdate();
				}
			}
			return getUpdateStatementResult(statement, resultQuery, resStmt, numRowsAffected);
		} catch (SQLException e) {
			throw new SenderException("got exception executing query ["+query+"]", e);
		} catch (JdbcException|IOException|JMSException e) {
			throw new SenderException("got exception executing a SQL command", e);
		} catch (ParameterException e) {
			throw new SenderException("got exception evaluating parameters", e);
		}
	}

	protected Message getUpdateStatementResult(PreparedStatement statement, String resultQuery, PreparedStatement resStmt, int numRowsAffected) throws SQLException, JdbcException, IOException, JMSException {
		if (resStmt != null) {
			log.debug("obtaining result from [{}]", resultQuery);
			try (ResultSet rs = resStmt.executeQuery()) {
				return getResult(rs);
			}
		}
		if (getColumnsReturnedList() != null) {
			return getResult(getReturnedColumns(statement));
		}
		if (isScalar()) {
			return new Message(Integer.toString(numRowsAffected));
		}
		return new Message("<result>" + (getBatchSize() > 0 ? "addedToBatch" : "<rowsupdated>" + numRowsAffected + "</rowsupdated>") + "</result>");
	}

	protected String fillParamArray(Object[] paramArray, String message) throws SenderException {
		int lengthMessage = message.length();
		int openingBracePosition = message.indexOf('(');
		int closingBracePosition = message.indexOf(')');
		int beginOutput = message.indexOf('?');
		if (openingBracePosition < 1)
			return message;
		if (beginOutput < 0)
			beginOutput = closingBracePosition;
		String packageInput = message.substring(openingBracePosition + 1, beginOutput);
		int idx;
		if (!message.contains(",") && !message.contains("?")) {
			idx = 1;
		} else {
			idx = 0;
		}
		int ix  = 1;
		String element=null;
		try {
			if (packageInput.lastIndexOf(',') > 0) {
				while ((packageInput.charAt(packageInput.length() - ix) != ',')	&& (ix < packageInput.length())) {
					ix++;
				}
				int endInputs = beginOutput - ix;
				packageInput = message.substring(openingBracePosition + 1, endInputs);
				if (idx != 1) {
					Iterator<String> iter = StringUtil.splitToStream(packageInput).iterator();
					while (iter.hasNext()) {
						element = iter.next();
						if (element.startsWith("'")) {
							int x = element.indexOf('\'');
							int y = element.lastIndexOf('\'');
							paramArray[idx] = element.substring(x + 1, y);
						} else {
							if (element.contains("-")){
								if (element.length() > 10) {
									String pattern = "yyyy-MM-dd HH:mm:ss";
									SimpleDateFormat sdf = new SimpleDateFormat(pattern);
									java.util.Date nDate = (java.util.Date)sdf.parseObject(element);
									Timestamp sqlTimestamp = new Timestamp(nDate.getTime());
									paramArray[idx] = sqlTimestamp;

								} else {
									String pattern = "yyyy-MM-dd";
									SimpleDateFormat sdf = new SimpleDateFormat(pattern);
									java.util.Date nDate;
									nDate = sdf.parse(element);
									java.sql.Date sDate = new java.sql.Date(nDate.getTime());
									paramArray[idx] = sDate;
								}
							} else {
								if (element.contains(".")) {
									paramArray[idx] = Float.parseFloat(element);
								} else {
									paramArray[idx] = Integer.parseInt(element);
								}
							}
						}
						idx++;
					}
				}
			}
			StringBuilder newMessage = new StringBuilder(message.substring(0, openingBracePosition + 1));
			if (idx >= 0) {
				//check if output parameter exists is expected in original message and append an ending ?(out-parameter)
				int parameterCount = idx + (message.contains("?") ? 1 : 0);
				if (parameterCount > 0) {
					newMessage.append("?");
					for (int i = 1; i < parameterCount; i++) {
						newMessage.append(",?");
					}
				}
				newMessage.append(message, closingBracePosition, lengthMessage);
			}
			return newMessage.toString();
		} catch (ParseException e) {
			throw new SenderException("got exception parsing a date string from element ["+element+"]", e);
		}
	}

	/**
	 * Controls if the returned package content is db2 format or xml format.
	 * Possible values:
	 * <ul>
	 * <li>select:</li> xml content s expected
	 * <li><i>anything else</i>:</li> db2 content is expected
	 * </ul>
	 */
	public void setPackageContent(String packageContent) {
		this.packageContent = packageContent;
	}

	/**
	 * Type of query to be executed
	 * @ff.default OTHER
	 */
	public void setQueryType(QueryType queryType) {
		switch (queryType) {
			case INSERT:
			case DELETE:
			case UPDATE:
				this.queryType = QueryType.OTHER;
				break;
			default:
				this.queryType = queryType;
				break;
		}
	}

	/**
	 * When <code>true</code>, the value of the first column of the first row (or the startrow) is returned as the only result, as a simple non-xml value
	 * @ff.default false
	 */
	public void setScalar(boolean b) {
		scalar = b;
	}

	/**
	 * When <code>true</code> and <code>scalar</code> is also <code>true</code>, but returns no value, one of the following is returned:
	 * <ul>
	 * <li>'[absent]' no row is found</li>
	 * <li>'[null]' a row is found, but the value is a SQL-NULL</li>
	 * <li>'[empty]' a row is found, but the value is a empty string</li>
	 * </ul>
	 * @ff.default false
	 */
	public void setScalarExtended(boolean b) {
		scalarExtended = b;
	}

	/**
	 * The maximum number of rows to be returned from the output of <code>select</code> queries, -1 means unlimited rows
	 * @ff.default -1
	 */
	public void setMaxRows(int i) {
		maxRows = i;
	}

	/**
	 * The number of the first row to be returned from the output of <code>select</code> queries. Rows before this are skipped from the output.
	 * @ff.default 1
	 */
	public void setStartRow(int i) {
		startRow = i;
	}

	/**
	 * Value used in result as contents of fields that contain no value (sql-null)
	 * @ff.default <i>empty string</i>
	 */
	public void setNullValue(String string) {
		nullValue = string;
	}

	/** Query that can be used to obtain result of side-effect of update-query, like generated value of sequence. Example: SELECT mysequence.currval FROM dual */
	public void setResultQuery(String string) {
		resultQuery = string;
	}

	/**
	 * Comma separated list of columns whose values are to be returned. Works only if the driver implements jdbc 3.0 getGeneratedKeys().
	 * Note: not all drivers support multiple values and returned field names may vary between drivers.
	 * Works for H2 and Oracle. Could work for MS_SQL with a single identity column, with name GENERATED_KEYS, if a identity has been generated. Not supported for other DBMSes.
	 */
	public void setColumnsReturned(String string) {
		columnsReturned = string;
	}

	/**
	 * Named parameters will be auto-detected by default. Every string in the query which equals <code>{@value #UNP_START}paramname{@value #UNP_END}</code> will be replaced by the value of the corresponding parameter. The parameters don't need to be in the correct order and unused parameters are skipped.
	 * @ff.default null
	 */
	public void setUseNamedParams(Boolean b) {
		useNamedParams = b;
	}

	/**
	 * when <code>true</code>, the result contains besides the returned rows also includes a header with information about the fetched fields
	 * @ff.default application default (true)
	 */
	public void setIncludeFieldDefinition(boolean b) {
		includeFieldDefinition = b;
	}

	/**
	 * Remove trailing blanks from all result values.
	 * @ff.default true
	 */
	public void setTrimSpaces(boolean b) {
		trimSpaces = b;
	}

	/** If specified, the rowid of the processed row is put in the pipelinesession under the specified key (only applicable for <code>querytype=other</code>). <b>Note:</b> If multiple rows are processed a SqlException is thrown. */
	public void setRowIdSessionKey(String string) {
		rowIdSessionKey = string;
	}


	/**
	 * If set, the result is streamed to the HttpServletResponse object of the RestServiceDispatcher (instead of passed as bytes or as a String)
	 * @ff.default false
	 */
	@Deprecated(forRemoval = true, since = "7.6.0")
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}

	/** If set, the SQL dialect in which the queries are written and should be translated from to the actual SQL dialect */
	public void setSqlDialect(String string) {
		sqlDialect = string;
	}

	/**
	 * When set <code>true</code>, exclusive row-level locks are obtained on all the rows identified by the select statement (e.g. by appending ' FOR UPDATE NOWAIT SKIP LOCKED' to the end of the query)
	 * @ff.default false
	 */
	public void setLockRows(boolean b) {
		lockRows = b;
	}

	/**
	 * when set and >=0, ' FOR UPDATE WAIT #' is used instead of ' FOR UPDATE NOWAIT SKIP LOCKED'
	 * @ff.default -1
	 */
	public void setLockWait(int i) {
		lockWait = i;
	}

	/**
	 * When <code>true</code>, the result of sendMessage is the reply of the request.
	 * @ff.default true
	 */
	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	/**
	 * Only for querytype 'updateBlob': column that contains the BLOB to be updated
	 * @ff.default 1
	 */
	public void setBlobColumn(int i) {
		blobColumn = i;
	}

	/**
	 * For querytype 'updateBlob': key of session variable that contains the data (String or InputStream) to be loaded to the BLOB. When empty, the input of the pipe, which then must be a String, is used.
	 * For querytype 'select': key of session variable that contains the OutputStream, Writer or Filename to write the BLOB to
	 */
	public void setBlobSessionKey(String string) {
		blobSessionKey = string;
	}

	/**
	 * controls whether blobdata is stored compressed in the database
	 * @ff.default true
	 */
	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}

	/** controls whether the streamed blobdata will need to be base64 <code>encode</code> or <code>decode</code> or not. */
	public void setBlobBase64Direction(Base64Pipe.Direction value) {
		blobBase64Direction = value;
	}

	/**
	 * Charset that is used to read and write BLOBs. This assumes the blob contains character data.
	 * If blobCharset and blobSmartGet are not set, BLOBs are returned as bytes. Before version 7.6, blobs were base64 encoded after being read to accommodate for the fact that senders need to return a String. This is no longer the case
	 */
	public void setBlobCharset(String string) {
		if (StringUtils.isEmpty(string)) {
			ConfigurationWarnings.add(this, log, "setting blobCharset to empty string does not trigger base64 encoding anymore, BLOBs are returned as byte arrays. If base64 encoding is really necessary, use blobBase64Direction=encode.");
		}
		blobCharset = string;
	}

	/**
	 * Controls automatically whether blobdata is stored compressed and/or serialized in the database
	 * @ff.default false
	 */
	public void setBlobSmartGet(boolean b) {
		blobSmartGet = b;
	}


	/**
	 * Only for querytype 'updateClob': column that contains the CLOB to be updated
	 * @ff.default 1
	 */
	public void setClobColumn(int i) {
		clobColumn = i;
	}

	/**
	 * For querytype 'updateClob': key of session variable that contains the CLOB (String or InputStream) to be loaded to the CLOB. When empty, the input of the pipe, which then must be a String, is used.
	 * For querytype 'select': key of session variable that contains the OutputStream, Writer or Filename to write the CLOB to
	 */
	public void setClobSessionKey(String string) {
		clobSessionKey = string;
	}

	/**
	 * When set to <code>false</code>, the Inputstream is not closed after it has been used to update a BLOB or CLOB
	 * @ff.default true
	 */
	@Deprecated(forRemoval = true, since = "7.6.0")
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}

	/**
	 * When set to <code>false</code>, the Outputstream is not closed after BLOB or CLOB has been written to it
	 * @ff.default true
	 */
	public void setCloseOutputstreamOnExit(boolean b) {
		closeOutputstreamOnExit = b;
	}


	/** Charset used when reading a stream (that is e.g. going to be written to a BLOB or CLOB). When empty, the stream is copied directly to the BLOB, without conversion */
	public void setStreamCharset(String string) {
		streamCharset = string;
	}

	/**
	 * If true, then select queries are executed in a way that avoids taking locks, e.g. with isolation mode 'read committed' instead of 'repeatable read'.
	 * @ff.default false
	 */
	public void setAvoidLocking(boolean avoidLocking) {
		this.avoidLocking = avoidLocking;
	}

	/**
	 * If true and scalar=false, multiline indented XML is produced
	 * @ff.default false
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	/**
	 * The type of output. If not set then defaults to old-style XML. If set to XML, new-style XML is used. EXPERIMENTAL: datatypes like numbers are not yet rendered correctly
	 * @ff.default false
	 */
	public void setOutputFormat(DocumentFormat outputFormat) {
		this.outputFormat = outputFormat;
	}
	public int getBatchSize() {
		return 0;
	}

}
