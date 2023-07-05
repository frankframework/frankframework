/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package nl.nn.adapterframework.jdbc;

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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jms.JMSException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.ContentHandler;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.SupportsOutputStreaming;
import nl.nn.adapterframework.jta.TransactionConnectorCoordinator;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.Base64Pipe;
import nl.nn.adapterframework.pipes.Base64Pipe.Direction;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DB2DocumentWriter;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * This executes the query that is obtained from the (here still abstract) method getStatement.
 * Descendent classes can override getStatement to provide meaningful statements.
 * If used with parameters, the values of the parameters will be applied to the statement.
 * Each occurrence of a questionmark ('?') will be replaced by a parameter value. Parameters are applied
 * in order: The n-th questionmark is replaced by the value of the n-th parameter.
 *
 * <h3>Note on using packages</h3>
 * The package processor makes some assumptions about the datatypes:
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
@SupportsOutputStreaming
public abstract class JdbcQuerySenderBase<H> extends JdbcSenderBase<H> {

	public static final String UNP_START = "?{";
	public static final String UNP_END = "}";

	private QueryType queryType = QueryType.OTHER;
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
	private @Getter boolean streamResultToServlet=false;
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
		OTHER
	}

	@Override
	public void configure() throws ConfigurationException {
		if (!BooleanUtils.isFalse(getUseNamedParams())) { // only allow duplicate parameter names if useNamedParams is set explicitly to false.
			if (getParameterList()!=null) {
				getParameterList().setNamesMustBeUnique(true);
			}
		}
		super.configure();
		if (StringUtils.isNotEmpty(getColumnsReturned())) {
			List<String> tempList = new ArrayList<>();
			StringTokenizer st = new StringTokenizer(getColumnsReturned(),",");
			while (st.hasMoreTokens()) {
				String column = st.nextToken().trim();
				tempList.add(column);
			}
			columnsReturnedList = new String[tempList.size()];
			for (int i=0; i<tempList.size(); i++) {
				columnsReturnedList[i] = tempList.get(i);
			}
		}
		if (getBatchSize()>0 && getQueryTypeEnum() != QueryType.OTHER) {
			throw new ConfigurationException(getLogPrefix()+"batchSize>0 only valid for queryType 'other'");
		}
	}

	/**
	 * Obtain a query to be executed.
	 * Method-stub to be overridden in descender-classes.
	 */
	protected abstract String getQuery(Message message) throws SenderException;

	protected final PreparedStatement getStatement(Connection con, QueryExecutionContext queryExecutionContext) throws JdbcException, SQLException {
		return prepareQuery(con, queryExecutionContext);
	}

	private PreparedStatement prepareQueryWithColumnsReturned(Connection con, String query, String[] columnsReturned) throws SQLException {
		return con.prepareStatement(query,columnsReturned);
	}

	@Override
	public void open() throws SenderException {
		super.open();
		if (StringUtils.isNotEmpty(getResultQuery())) {
			try {
				QueryExecutionContext resultContext = new QueryExecutionContext(getResultQuery(), QueryType.SELECT, null);
				convertQuery(resultContext);
				if (log.isDebugEnabled()) log.debug("converted result query into [" + resultContext.getQuery() + "]");
				convertedResultQuery = resultContext.getQuery();
			} catch (JdbcException | SQLException e) {
				throw new SenderException("Cannot convert result query",e);
			}
		}
	}

	protected void convertQuery(QueryExecutionContext queryExecutionContext) throws JdbcException, SQLException {
		if (StringUtils.isNotEmpty(getSqlDialect()) && !getSqlDialect().equalsIgnoreCase(getDbmsSupport().getDbmsName())) {
			if (log.isDebugEnabled()) {
				log.debug(getLogPrefix() + "converting query [" + queryExecutionContext.getQuery().trim() + "] from [" + getSqlDialect() + "] to [" + getDbmsSupport().getDbmsName() + "]");
			}
			getDbmsSupport().convertQuery(queryExecutionContext, getSqlDialect());
		}
	}

	protected PreparedStatement prepareQuery(Connection con, QueryExecutionContext queryExecutionContext) throws SQLException, JdbcException {
		convertQuery(queryExecutionContext);
		String query = queryExecutionContext.getQuery();
		if (isLockRows()) {
			query = getDbmsSupport().prepareQueryTextForWorkQueueReading(-1, query, getLockWait());
		}
		if (isAvoidLocking()) {
			query = getDbmsSupport().prepareQueryTextForNonLockingRead(query);
		}
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() +"preparing statement for query ["+query+"]");
		}
		String[] columnsReturned = getColumnsReturnedList();
		if (columnsReturned != null) {
			return prepareQueryWithColumnsReturned(con, query, columnsReturned);
		}
		boolean resultSetUpdateable = isLockRows() || queryExecutionContext.getQueryType()==QueryType.UPDATEBLOB || queryExecutionContext.getQueryType()==QueryType.UPDATECLOB;
		return con.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, resultSetUpdateable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
	}

	protected CallableStatement getCallWithRowIdReturned(Connection con, String query) throws SQLException {
		String callQuery = "BEGIN " + query + " RETURNING ROWID INTO ?; END;";
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() +"preparing statement for query ["+callQuery+"]");
		}
		return con.prepareCall(callQuery);
	}

	protected ResultSet getReturnedColumns(PreparedStatement st) throws SQLException {
		return st.getGeneratedKeys();
	}

	public QueryExecutionContext getQueryExecutionContext(Connection connection, Message message) throws SenderException, SQLException, ParameterException, JdbcException {
		ParameterList newParameterList = paramList != null ? (ParameterList) paramList.clone() : new ParameterList();
		String query = getQuery(message);
		if (BooleanUtils.isTrue(getUseNamedParams()) || (getUseNamedParams() == null && query.contains(UNP_START))) {
			query = adjustQueryAndParameterListForNamedParameters(newParameterList, query);
		}
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, getQueryTypeEnum(), newParameterList);
		queryExecutionContext.setConnection(connection);
		log.debug(getLogPrefix() + "obtaining prepared statement to execute");
		PreparedStatement statement = getStatement(connection, queryExecutionContext);
		log.debug(getLogPrefix() + "obtained prepared statement to execute");
		queryExecutionContext.setStatement(statement);
		statement.setQueryTimeout(getTimeout());
		if (convertedResultQuery != null) {
			queryExecutionContext.setResultQueryStatement(connection.prepareStatement(convertedResultQuery));
		}
		return queryExecutionContext;
	}


	protected Connection getConnectionForSendMessage() throws JdbcException, TimeoutException {
		if (isConnectionsArePooled()) {
			return getConnectionWithTimeout(getTimeout());
		}
		return connection;
	}

	protected void closeConnectionForSendMessage(Connection connection, PipeLineSession session) throws JdbcException, TimeoutException {
		if (isConnectionsArePooled() && connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "caught exception closing sender after sending message, ID=["+(session==null?null:session.getMessageId())+"]", e));
			}
		}
	}

	protected void closeStatementSet(QueryExecutionContext queryExecutionContext, PipeLineSession session) {
		try (PreparedStatement statement = queryExecutionContext.getStatement()) {
			if (getBatchSize()>0) {
				statement.executeBatch();
			}
		} catch (SQLException e) {
			log.warn("{}got exception closing SQL statement", getLogPrefix(), e);
		}
		//noinspection EmptyTryBlock
		try (Statement statement = queryExecutionContext.getResultQueryStatement()) {
			// only close statement
		} catch (SQLException e) {
			log.warn("{}got exception closing result SQL statement", getLogPrefix(), e);
		}
	}

	protected PipeRunResult executeStatementSet(@Nonnull QueryExecutionContext queryExecutionContext, @Nonnull Message message, @Nonnull PipeLineSession session, @Nullable IForwardTarget next) throws SenderException, TimeoutException {
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
						return executeSelectQuery(statement,blobSessionVar,clobSessionVar, response, contentType, contentDisposition, session, next);
					} else {
						return executeSelectQuery(statement,blobSessionVar,clobSessionVar, session, next);
					}
				case UPDATEBLOB:
					if (StringUtils.isNotEmpty(getBlobSessionKey())) {
						return new PipeRunResult(null, executeUpdateBlobQuery(statement, session.getMessage(getBlobSessionKey())));
					}
					return new PipeRunResult(null, executeUpdateBlobQuery(statement, message));
				case UPDATECLOB:
					if (StringUtils.isNotEmpty(getClobSessionKey())) {
						return new PipeRunResult(null, executeUpdateClobQuery(statement, session.getMessage(getClobSessionKey())));
					}
					return new PipeRunResult(null, executeUpdateClobQuery(statement, message));
				case PACKAGE:
					return new PipeRunResult(null, executePackageQuery(queryExecutionContext));
				case OTHER:
					Message result = executeOtherQuery(queryExecutionContext, message, session);
					if (getBatchSize()>0 && ++queryExecutionContext.iteration>=getBatchSize()) {
						int[] results=statement.executeBatch();
						int numRowsAffected=0;
						for (int i:results) {
							numRowsAffected+=i;
						}
						result = new Message("<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>");
						statement.clearBatch();
						queryExecutionContext.iteration=0;
					}
					return new PipeRunResult(null, result);
				default:
					throw new IllegalStateException("Unsupported queryType: ["+queryExecutionContext.getQueryType()+"]");
			}
		} catch (SenderException e) {
			if (e.getCause() instanceof SQLException) {
				SQLException sqle = (SQLException) e.getCause();
				if  (sqle.getErrorCode() == 1013) {
					throw new TimeoutException("Timeout of ["+getTimeout()+"] sec expired");
				}
			}
			throw new SenderException(e);
		} catch (Throwable t) {
			throw new SenderException(getLogPrefix() + "got exception sending message", t);
		} finally {
			closeStatementSet(queryExecutionContext, session);
			ParameterList newParameterList = queryExecutionContext.getParameterList();
			if (isCloseInputstreamOnExit() && newParameterList!=null) {
				for (int i = 0; i < newParameterList.size(); i++) {
					Parameter param = newParameterList.getParameter(i);
					if (param.getType() == ParameterType.INPUTSTREAM) {
						log.debug(getLogPrefix() + "Closing inputstream for parameter [" + param.getName() + "]");
						try {Object object = newParameterList.getParameter(i).getValue(null, message, session, true);
							if(object instanceof AutoCloseable) {
								((AutoCloseable)object).close();
							}
							else {
								log.error("unable to auto-close parameter ["+param.getName()+"]");
							}
						} catch (Exception e) {
							log.warn(new SenderException(getLogPrefix() + "got exception closing inputstream", e));
						}
					}
				}
			}
		}
	}

	protected String adjustQueryAndParameterListForNamedParameters(ParameterList parameterList, String query) throws SenderException {
		if (log.isDebugEnabled()) {
			log.debug("{}Adjusting list of parameters [{}]", this::getLogPrefix, ()->parameterListToString(parameterList));
		}

		StringBuilder buffer = new StringBuilder();
		int startPos = query.indexOf(UNP_START);
		if (startPos == -1)
			return query;
		char[] messageChars = query.toCharArray();
		int copyFrom = 0;
		ParameterList oldParameterList = (ParameterList) parameterList.clone();
		parameterList.clear();
		while (startPos != -1) {
			buffer.append(messageChars, copyFrom, startPos - copyFrom);
			int nextStartPos = query.indexOf(UNP_START, startPos + UNP_START.length());
			if (nextStartPos == -1) {
				nextStartPos = query.length();
			}
			int endPos = query.indexOf(UNP_END, startPos + UNP_START.length());

			if (endPos == -1 || endPos > nextStartPos) {
				log.warn(getLogPrefix() + "Found a start delimiter without an end delimiter at position [" + startPos + "] in ["+ query+ "]");
				buffer.append(messageChars, startPos, nextStartPos - startPos);
				copyFrom = nextStartPos;
			} else {
				String namedParam = query.substring(startPos + UNP_START.length(),endPos);
				Parameter param = oldParameterList.findParameter(namedParam);
				if (param != null) {
					parameterList.add(param);
					buffer.append("?");
					copyFrom = endPos + UNP_END.length();
				} else {
					log.warn(getLogPrefix() + "Parameter [" + namedParam + "] is not found");
					buffer.append(messageChars, startPos, nextStartPos - startPos);
					copyFrom = nextStartPos;
				}
			}
			startPos = query.indexOf(UNP_START, copyFrom);
		}
		buffer.append(messageChars, copyFrom, messageChars.length - copyFrom);

		if (log.isDebugEnabled()) {
			log.debug( "{}Adjusted list of parameters [{}]", this::getLogPrefix, ()->parameterListToString(parameterList));
		}

		return buffer.toString();
	}

	private String parameterListToString(ParameterList parameterList) {
		return parameterList.stream()
				.map(Parameter::getName)
				.collect(Collectors.joining(", "));
	}

	protected Message getResult(ResultSet resultset) throws JdbcException, SQLException, IOException, JMSException {
		return getResult(resultset,null,null);
	}

	protected Message getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar) throws JdbcException, SQLException, IOException, JMSException {
		return getResult(resultset, blobSessionVar, clobSessionVar, null, null, null, null, null).getResult();
	}

	protected PipeRunResult getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar, HttpServletResponse response, String contentType, String contentDisposition, PipeLineSession session, IForwardTarget next) throws JdbcException, SQLException, IOException {
		if (isScalar()) {
			String result=null;
			if (resultset.next()) {
				//result = resultset.getString(1);
				ResultSetMetaData rsmeta = resultset.getMetaData();
				int numberOfColumns = rsmeta.getColumnCount();
				if(numberOfColumns > 1) {
					log.warn(getLogPrefix() + "has set scalar=true but the resultset contains ["+numberOfColumns+"] columns. Consider optimizing the query.");
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
						return new PipeRunResult(null, Message.nullMessage());
					}
					if (blobSessionVar!=null) {
						JdbcUtil.streamBlob(getDbmsSupport(), resultset, 1, getBlobCharset(), isBlobsCompressed(), getBlobBase64Direction(), blobSessionVar, isCloseOutputstreamOnExit());
						return new PipeRunResult(null, Message.nullMessage());
					}
					if (!isBlobSmartGet()) {
						try (MessageOutputStream target=MessageOutputStream.getTargetStream(this, session, next)) {
							if (StringUtils.isNotEmpty(getBlobCharset())) {
								JdbcUtil.streamBlob(getDbmsSupport(), resultset, 1, getBlobCharset(), isBlobsCompressed(), getBlobBase64Direction(), target.asWriter(), isCloseOutputstreamOnExit());
							} else {
								JdbcUtil.streamBlob(getDbmsSupport(), resultset, 1, null, isBlobsCompressed(), getBlobBase64Direction(), target.asStream(), isCloseOutputstreamOnExit());
							}
							return target.getPipeRunResult();
						} catch (Exception e) {
							throw new JdbcException(e);
						}
					}
				}
				if (getDbmsSupport().isClobType(rsmeta, 1)) {
					if (clobSessionVar!=null) {
						JdbcUtil.streamClob(getDbmsSupport(), resultset, 1, clobSessionVar, isCloseOutputstreamOnExit());
						return new PipeRunResult(null, Message.nullMessage());
					}
					try (MessageOutputStream target=MessageOutputStream.getTargetStream(this, session, next)) {
						JdbcUtil.streamClob(getDbmsSupport(), resultset, 1, target.asWriter(), isCloseOutputstreamOnExit());
						return target.getPipeRunResult();
					} catch (Exception e) {
						throw new JdbcException(e);
					}
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
					log.warn(getLogPrefix() + "has set scalar=true but the query returned more than 1 row. Consider optimizing the query.");
				}
			} else if (isScalarExtended()) {
					result="[absent]";
			}
			return new PipeRunResult(null, new Message(result));
		}
		try (MessageOutputStream target=MessageOutputStream.getTargetStream(this, session, next)) {
			// Create XML and give the maxlength as a parameter
			if (getOutputFormat()==null) {
				DB2XMLWriter db2xml = new DB2XMLWriter();
				db2xml.setNullValue(getNullValue());
				db2xml.setTrimSpaces(isTrimSpaces());
				if (StringUtils.isNotEmpty(getBlobCharset())) db2xml.setBlobCharset(getBlobCharset());
				db2xml.setDecompressBlobs(isBlobsCompressed());
				db2xml.setGetBlobSmart(isBlobSmartGet());
				ContentHandler handler = target.asContentHandler();
				db2xml.getXML(getDbmsSupport(), resultset, getMaxRows(), isIncludeFieldDefinition(), handler, isPrettyPrint());
			} else {
				DB2DocumentWriter db2document = new DB2DocumentWriter();
				db2document.setNullValue(getNullValue());
				db2document.setTrimSpaces(isTrimSpaces());
				if (StringUtils.isNotEmpty(getBlobCharset())) db2document.setBlobCharset(getBlobCharset());
				db2document.setDecompressBlobs(isBlobsCompressed());
				db2document.setGetBlobSmart(isBlobSmartGet());
				db2document.writeDocument(getOutputFormat(), getDbmsSupport(), resultset, getMaxRows(), isIncludeFieldDefinition(), target, isPrettyPrint());
			}
			target.close();
			return target.getPipeRunResult();
		} catch (Exception e) {
			throw new JdbcException(e);
		}
	}


	private BlobOutputStream getBlobOutputStream(PreparedStatement statement, int blobColumn, boolean compressBlob) throws SQLException, JdbcException, IOException {
		log.debug(getLogPrefix() + "executing an update BLOB command");
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
			throw new SenderException(getLogPrefix() + "got exception executing an update BLOB command", e);
		}
		return new Message(blobOutputStream.getWarnings().toXML());
	}

	private ClobWriter getClobWriter(PreparedStatement statement, int clobColumn) throws SQLException, JdbcException {
		log.debug(getLogPrefix() + "executing an update CLOB command");
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
			throw new SenderException(getLogPrefix() + "got exception executing an update CLOB command", e);
		}
		return new Message(clobWriter.getWarnings().toXML());
	}

	protected boolean canProvideOutputStream() {
		return false; // FixedQuerySender returns true for updateBlob and updateClob
	}

	@Override
	public boolean supportsOutputStreamPassThrough() {
		return false;
	}

	@Override
	public MessageOutputStream provideOutputStream(PipeLineSession session, IForwardTarget next) throws StreamingException {
		if (!canProvideOutputStream()) {
			return null;
		}
		return TransactionConnectorCoordinator.doInUnsuspendedTransationContext(()-> {
			final Connection connection;
			QueryExecutionContext queryExecutionContext;
			try {
				connection = getConnectionWithTimeout(getTimeout());
				queryExecutionContext = getQueryExecutionContext(connection, null);
			} catch (JdbcException | ParameterException | SQLException | SenderException | TimeoutException e) {
				throw new StreamingException(getLogPrefix() + "cannot getQueryExecutionContext",e);
			}
			try {
				PreparedStatement statement=queryExecutionContext.getStatement();
				if (queryExecutionContext.getParameterList() != null) {
					JdbcUtil.applyParameters(getDbmsSupport(), statement, queryExecutionContext.getParameterList(), Message.nullMessage(), session);
				}
				if (queryExecutionContext.getQueryType()==QueryType.UPDATEBLOB) {
					BlobOutputStream blobOutputStream = getBlobOutputStream(statement, blobColumn, isBlobsCompressed());
					TransactionConnectorCoordinator.onEndChildThread(()-> {
						blobOutputStream.close();
						connection.close();
						log.warn(getLogPrefix()+"warnings: "+blobOutputStream.getWarnings().toXML());
					});
					return new MessageOutputStream(this, blobOutputStream, next) {
						// perform close() on MessageOutputStream.close(), necessary when no TransactionConnector available for onEndThread()
						@Override
						public void afterClose() throws SQLException {
							if (!connection.isClosed()) {
								connection.close();
							}
							log.warn(getLogPrefix()+"warnings: "+blobOutputStream.getWarnings().toXML());
						}
					};
				}
				if (queryExecutionContext.getQueryType()==QueryType.UPDATECLOB) {
					ClobWriter clobWriter = getClobWriter(statement, getClobColumn());
					TransactionConnectorCoordinator.onEndChildThread(()-> {
						clobWriter.close();
						connection.close();
						log.warn(getLogPrefix()+"warnings: "+clobWriter.getWarnings().toXML());
					});
					return new MessageOutputStream(this, clobWriter, next) {
						// perform close() on MessageOutputStream.close(), necessary when no TransactionConnector available for onEndThread()
						@Override
						public void afterClose() throws SQLException {
							if (!connection.isClosed()) {
								connection.close();
							}
							log.warn(getLogPrefix()+"warnings: "+clobWriter.getWarnings().toXML());
						}
					};
				}
				throw new IllegalArgumentException(getLogPrefix()+"illegal queryType ["+queryExecutionContext.getQueryType()+"], must be 'updateBlob' or 'updateClob'");
			} catch (JdbcException | SQLException | IOException | ParameterException e) {
				throw new StreamingException(getLogPrefix() + "cannot update CLOB or BLOB",e);
			}
		});
	}

	protected PipeRunResult executeSelectQuery(PreparedStatement statement, Object blobSessionVar, Object clobSessionVar, PipeLineSession session, IForwardTarget next) throws SenderException{
		return executeSelectQuery(statement, blobSessionVar, clobSessionVar, null, null, null, session, next);
	}

	protected PipeRunResult executeSelectQuery(PreparedStatement statement, Object blobSessionVar, Object clobSessionVar, HttpServletResponse response, String contentType, String contentDisposition, PipeLineSession session, IForwardTarget next) throws SenderException{
		try {
			if (getMaxRows()>0) {
				statement.setMaxRows(getMaxRows()+ ( getStartRow()>1 ? getStartRow()-1 : 0));
			}

			log.debug(getLogPrefix() + "executing a SELECT SQL command");
			try (ResultSet resultset = statement.executeQuery()) {
				if (getStartRow()>1) {
					resultset.absolute(getStartRow()-1);
					log.debug(getLogPrefix() + "Index set at position: " +  resultset.getRow() );
				}
				return getResult(resultset, blobSessionVar, clobSessionVar, response, contentType, contentDisposition, session, next);
			}
		} catch (SQLException|JdbcException|IOException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command", e );
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
			int var = 1;
			for (int i=0;i<paramArray.length;i++) {
				if (paramArray[i] instanceof Timestamp) {
					pstmt.setTimestamp(var, (Timestamp) paramArray[i]);
					var++;
				}
				if (paramArray[i] instanceof java.sql.Date) {
					pstmt.setDate(var, (java.sql.Date) paramArray[i]);
					var++;
				}
				if (paramArray[i] instanceof String) {
					pstmt.setString(var, (String) paramArray[i]);
					var++;
				}
				if (paramArray[i] instanceof Integer) {
					int x = Integer.parseInt(paramArray[i].toString());
					pstmt.setInt(var, x);
					var++;
				}
				if (paramArray[i] instanceof Float) {
					float x = Float.parseFloat(paramArray[i].toString());
					pstmt.setFloat(var, x);
					var++;
				}
			}
			if (query.indexOf('?') != -1) {
				pstmt.registerOutParameter(var, Types.CLOB); // make sure enough space is available for result...
			}
			if ("xml".equalsIgnoreCase(getPackageContent())) {
				log.debug(getLogPrefix() + "executing a package SQL command");
				pstmt.executeUpdate();
				String pUitvoer = pstmt.getString(var);
				return new Message(pUitvoer);
			}
			log.debug(getLogPrefix() + "executing a package SQL command");
			int numRowsAffected = pstmt.executeUpdate();
			if (convertedResultQuery!=null) {
				PreparedStatement resStmt = queryExecutionContext.getResultQueryStatement();
				if (log.isDebugEnabled()) log.debug("obtaining result from [" + convertedResultQuery + "]");
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
			throw new SenderException(getLogPrefix() + "got exception executing a package SQL command", sqle);
		}
	}

	protected Message executeOtherQuery(QueryExecutionContext queryExecutionContext, Message message, PipeLineSession session) throws SenderException {
		Connection connection = queryExecutionContext.getConnection();
		PreparedStatement statement = queryExecutionContext.getStatement();
		String query = queryExecutionContext.getQuery();
		ParameterList parameterList = queryExecutionContext.getParameterList();
		PreparedStatement resStmt = queryExecutionContext.getResultQueryStatement();
		return executeOtherQuery(connection, statement, query, resStmt, message, session, parameterList);
	}

	protected Message executeOtherQuery(Connection connection, PreparedStatement statement, String query, PreparedStatement resStmt, Message message, PipeLineSession session, ParameterList parameterList) throws SenderException {
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
					log.debug(getLogPrefix() + "executing a SQL command");
					numRowsAffected = cstmt.executeUpdate();
					String rowId = cstmt.getString(ri);
					if (session!=null) session.put(getRowIdSessionKey(), rowId);
				}
			} else {
				log.debug(getLogPrefix() + "executing a SQL command");
				if (getBatchSize() > 0) {
					statement.addBatch();
				} else {
					numRowsAffected = statement.executeUpdate();
				}
			}
			if (resStmt != null) {
				log.debug("obtaining result from [{}]", convertedResultQuery);
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
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix() + "got exception executing query ["+query+"]",e );
		} catch (JdbcException|IOException|JMSException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SQL command",e );
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix() + "got exception evaluating parameters", e);
		}
	}

	protected String fillParamArray(Object[] paramArray, String message) throws SenderException {
		int lengthMessage = message.length();
		int startHaakje = message.indexOf('(');
		int eindHaakje	= message.indexOf(')');
		int beginOutput = message.indexOf('?');
		if (startHaakje < 1)
			return message;
		if (beginOutput < 0)
			beginOutput = eindHaakje;
		String packageInput = message.substring(startHaakje + 1, beginOutput);
		int idx = 0;
		if (message.indexOf(',') == -1) {
			if (message.indexOf('?') == -1) {
				idx = 1;
			} else {
				idx = 0;
			}
		}
		int ix  = 1;
		String element=null;
		try {
			if (packageInput.lastIndexOf(',') > 0) {
				while ((packageInput.charAt(packageInput.length() - ix) != ',')	&& (ix < packageInput.length())) {
					ix++;
				}
				int eindInputs = beginOutput - ix;
				packageInput = message.substring(startHaakje + 1, eindInputs);
				StringTokenizer st2 = new StringTokenizer(packageInput, ",");
				if (idx != 1) {
					while (st2.hasMoreTokens()) {
						element = st2.nextToken().trim();
						if (element.startsWith("'")) {
							int x = element.indexOf('\'');
							int y = element.lastIndexOf('\'');
							paramArray[idx] = element.substring(x + 1, y);
						} else {
							if (element.indexOf('-') >= 0){
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
								if (element.indexOf('.') >= 0) {
									paramArray[idx] = new Float(element);
								} else {
									paramArray[idx] = new Integer(element);
								}
							}
						}
						idx++;
					}
				}
			}
			StringBuilder newMessage = new StringBuilder(message.substring(0, startHaakje + 1));
			if (idx > 0) {
				newMessage.append("?");
			}
			for (int i = 0;i<idx; i++) {
				if (i<idx - 1) {
					newMessage.append(",?");
				}
			}
			if (idx>=0) {
				//check if output parameter exists is expected in original message and append an ending ?(out-parameter)
				if (message.indexOf('?') > 0) {
					if (idx == 0) {
						newMessage.append("?");
					} else {
						newMessage.append(",?");
					}
					newMessage.append(message.substring(eindHaakje, lengthMessage));
				} else {
					newMessage.append(message.substring(eindHaakje, lengthMessage));
				}
			}
			return newMessage.toString();
		} catch (ParseException e) {
			throw new SenderException(getLogPrefix() + "got exception parsing a date string from element ["+element+"]", e);
		}
	}

	/**
	 * Controls wheter the returned package content is db2 format or xml format.
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
	public void setQueryType(String queryType) {
		if ("insert".equalsIgnoreCase(queryType) || "delete".equalsIgnoreCase(queryType) || "update".equalsIgnoreCase(queryType)) {
			this.queryType=QueryType.OTHER;
		} else {
			this.queryType = EnumUtils.parse(QueryType.class, queryType);
		}
	}
	public QueryType getQueryTypeEnum() {
		return queryType;
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
	 * Named parameters will be auto detected by default. Every string in the query which equals <code>{@value #UNP_START}paramname{@value #UNP_END}</code> will be replaced by the value of the corresponding parameter. The parameters don't need to be in the correct order and unused parameters are skipped.
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
	@Deprecated
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
			ConfigurationWarnings.add(this, log, getLogPrefix()+"setting blobCharset to empty string does not trigger base64 encoding anymore, BLOBs are returned as byte arrays. If base64 encoding is really necessary, use blobBase64Direction=encode.");
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
	@Deprecated
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
