/*
   Copyright 2013, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
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

import javax.jms.JMSException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Misc;
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
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>all parameters present are applied to the statement to be executed</td></tr>
 * </table>
 * <br/>
 * <h3>Note on using packages</h3>
 * The package processor makes some assumptions about the datatypes:
 * <ul>
 *   <li>elements that start with a single quote are assumed to be Strings</li>
 *   <li>elements thta contain a dash ('-') are assumed to be dates (yyyy-MM-dd) or timestamps (yyyy-MM-dd HH:mm:ss)</li>
 *   <li>elements containing a dot ('.') are assumed to be floats</li>
 *   <li>all other elements are assumed to be integers</li>
 * </ul>
 * </p>
 * 
 * Queries that return no data (queryType 'other') return a message indicating the number of rows processed
 * 
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public abstract class JdbcQuerySenderBase<H> extends JdbcSenderBase<H> {

	private final static String UNP_START = "?{";
	private final static String UNP_END = "}";

	private String queryType = "other";
	private int maxRows=-1; // return all rows
	private int startRow=1;
	private boolean scalar=false;
	private boolean scalarExtended=false;
	private boolean synchronous=true;
	private int blobColumn=1;
	private int clobColumn=1;
	private String blobSessionKey=null;
	private String clobSessionKey=null;
	private String nullValue="";
	private String columnsReturned=null;
	private String resultQuery=null;
	private boolean trimSpaces=true;
	// TODO blobCharset should set to null! Clobs are for character data, blobs for binary. When blobs contain character data,
	// blobCharset can be set to "UTF-8", or set blobBase64Direction to 'encode'.
	// In a later version of the framework it will be possible to return binary data from a sender.
	private String blobCharset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING; 
	private boolean closeInputstreamOnExit=true;
	private boolean closeOutputstreamOnExit=true;
	private String blobBase64Direction=null;
	private String streamCharset = null;
	private boolean blobsCompressed=true;
	private boolean blobSmartGet=false;
	private boolean useNamedParams=false;
	private boolean includeFieldDefinition=XmlUtils.isIncludeFieldDefinitionByDefault();
	private String rowIdSessionKey=null;
	private String packageContent = "db2";
	protected String[] columnsReturnedList=null;
	private boolean streamResultToServlet=false;
	private String sqlDialect = AppConstants.getInstance().getString("jdbc.sqlDialect", null);
	private boolean lockRows=false;
	private int lockWait=-1;
	private boolean avoidLocking=false;
	
	private String convertedResultQuery;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		String dir=getBlobBase64Direction();
		if (StringUtils.isNotEmpty(dir) && !dir.equalsIgnoreCase("encode") && !dir.equalsIgnoreCase("decode")) {
			throw new ConfigurationException(getLogPrefix()+"illegal value for direction ["+dir+"], must be 'encode' or 'decode' or empty");
		}

		if (StringUtils.isNotEmpty(getColumnsReturned())) {
			List<String> tempList = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(getColumnsReturned(),",");
			while (st.hasMoreTokens()) {
				String column = st.nextToken();
				tempList.add(column);
			}
			columnsReturnedList = new String[tempList.size()];
			for (int i=0; i<tempList.size(); i++) {
				columnsReturnedList[i] = tempList.get(i);
			}
		}
		if (getBatchSize()>0 && !"other".equals(getQueryType())) {
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

	private PreparedStatement prepareQueryWithColunmsReturned(Connection con, String query, String[] columnsReturned) throws SQLException {
		return con.prepareStatement(query,columnsReturned);
	}

	@Override
	public void open() throws SenderException {
		super.open();
		if (StringUtils.isNotEmpty(getResultQuery())) {
			try (Connection connection = getConnection()) {
				QueryExecutionContext resultContext = new QueryExecutionContext(getResultQuery(), "select", null);
				convertQuery(connection, resultContext);
				if (log.isDebugEnabled()) log.debug("converted result query into [" + resultContext.getQuery() + "]");
				convertedResultQuery = resultContext.getQuery();
			} catch (JdbcException | SQLException e) {
				throw new SenderException("Cannot convert result query",e);
			}
		}
	}

	protected void convertQuery(Connection connection, QueryExecutionContext queryExecutionContext) throws JdbcException, SQLException {
		if (StringUtils.isNotEmpty(getSqlDialect()) && !getSqlDialect().equalsIgnoreCase(getDbmsSupport().getDbmsName())) {
			if (log.isDebugEnabled()) {
				log.debug(getLogPrefix() + "converting query [" + queryExecutionContext.getQuery().trim() + "] from [" + getSqlDialect() + "] to [" + getDbmsSupport().getDbmsName() + "]");
			}
			getDbmsSupport().convertQuery(queryExecutionContext, getSqlDialect());
		}
	}

	protected PreparedStatement prepareQuery(Connection con, QueryExecutionContext queryExecutionContext) throws SQLException, JdbcException {
		convertQuery(con, queryExecutionContext);
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
		if (columnsReturned!=null) {
			return prepareQueryWithColunmsReturned(con,query,columnsReturned);
		}
		boolean resultSetUpdateable = isLockRows() || "updateBlob".equalsIgnoreCase(queryExecutionContext.getQueryType()) || "updateClob".equalsIgnoreCase(queryExecutionContext.getQueryType());
		return con.prepareStatement(query,ResultSet.TYPE_FORWARD_ONLY,resultSetUpdateable?ResultSet.CONCUR_UPDATABLE:ResultSet.CONCUR_READ_ONLY);
	}

	protected CallableStatement getCallWithRowIdReturned(Connection con, String query) throws SQLException {
		String callQuery = "BEGIN " + query + " RETURNING ROWID INTO ?; END;";
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() +"preparing statement for query ["+callQuery+"]");
		}
		return con.prepareCall(callQuery);
	}

	protected ResultSet getReturnedColumns(String[] columns, PreparedStatement st) throws SQLException {
		return st.getGeneratedKeys();
	}

	public QueryExecutionContext getQueryExecutionContext(Connection connection, Message message, IPipeLineSession session) throws SenderException, SQLException, ParameterException, JdbcException {
		ParameterList newParameterList = paramList != null ? (ParameterList) paramList.clone() : new ParameterList();
		String query=getQuery(message);
		if (isUseNamedParams()) {
			query = adjustQueryAndParameterListForNamedParameters(newParameterList, query);
		}
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext(query, getQueryType(), newParameterList);
		queryExecutionContext.setConnection(connection);
		log.debug(getLogPrefix() + "obtaining prepared statement to execute");
		PreparedStatement statement = getStatement(connection, queryExecutionContext);
		log.debug(getLogPrefix() + "obtained prepared statement to execute");
		queryExecutionContext.setStatement(statement);
		statement.setQueryTimeout(getTimeout());
		if (convertedResultQuery!=null) {
			queryExecutionContext.setResultQueryStatement(connection.prepareStatement(convertedResultQuery));
		}
		return queryExecutionContext;
	}


	protected Connection getConnectionForSendMessage(H blockHandle) throws JdbcException, TimeOutException {
		if (isConnectionsArePooled()) {
			return getConnectionWithTimeout(getTimeout());
		}
		return connection;
	}
	protected void closeConnectionForSendMessage(Connection connection, IPipeLineSession session) throws JdbcException, TimeOutException {
		if (isConnectionsArePooled() && connection!=null) {
			try {
				connection.close();
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "caught exception closing sender after sending message, ID=["+(session==null?null:session.getMessageId())+"]", e));
			}
		}
	}
	
	protected QueryExecutionContext prepareStatementSet(H blockHandle, Connection connection, Message message, IPipeLineSession session) throws SenderException {
		try {
			QueryExecutionContext result = getQueryExecutionContext(connection, message, session);
			if (getBatchSize()>0) {
				result.getStatement().clearBatch();
			}
			return result;
		} catch (JdbcException|ParameterException|SQLException e) {
			throw new SenderException(getLogPrefix() + "cannot getQueryExecutionContext",e);
		}
	}
	protected void closeStatementSet(QueryExecutionContext queryExecutionContext, IPipeLineSession session) {
		try (PreparedStatement statement = queryExecutionContext.getStatement()) {
			if (getBatchSize()>0) {
				statement.executeBatch();
			}
		} catch (SQLException e) {
			log.warn(new SenderException(getLogPrefix() + "got exception closing SQL statement",e ));
		}
		try (Statement statement = queryExecutionContext.getResultQueryStatement()) {
			// only close statement
		} catch (SQLException e) {
			log.warn(new SenderException(getLogPrefix() + "got exception closing result SQL statement",e ));
		}
	}
	

	protected Message sendMessageOnConnection(Connection connection, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		try (JdbcSession jdbcSession = isAvoidLocking()?getDbmsSupport().prepareSessionForNonLockingRead(connection):null) {
			QueryExecutionContext queryExecutionContext = prepareStatementSet(null, connection, message, session);
			try {
				return executeStatementSet(queryExecutionContext, message, session);
			} finally {
				closeStatementSet(queryExecutionContext, session);
			}
		} catch (SenderException|TimeOutException e) {
			throw e;
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}
	

	protected Message executeStatementSet(QueryExecutionContext queryExecutionContext, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		try {
			PreparedStatement statement=queryExecutionContext.getStatement();
			JdbcUtil.applyParameters(getDbmsSupport(), statement, queryExecutionContext.getParameterList(), message, session);
			if ("select".equalsIgnoreCase(queryExecutionContext.getQueryType())) {
				Object blobSessionVar=null;
				Object clobSessionVar=null;
				if (session!=null && StringUtils.isNotEmpty(getBlobSessionKey())) {
					blobSessionVar=session.get(getBlobSessionKey());
				}
				if (session!=null && StringUtils.isNotEmpty(getClobSessionKey())) {
					clobSessionVar=session.get(getClobSessionKey());
				}
				if (isStreamResultToServlet()) {
					HttpServletResponse response = (HttpServletResponse) session.get(IPipeLineSession.HTTP_RESPONSE_KEY);
					String contentType = (String) session.get("contentType");
					String contentDisposition = (String) session.get("contentDisposition");
					return executeSelectQuery(statement,blobSessionVar,clobSessionVar, response, contentType, contentDisposition);
				} else {
					return executeSelectQuery(statement,blobSessionVar,clobSessionVar);
				}
			} 
			if ("updateBlob".equalsIgnoreCase(queryExecutionContext.getQueryType())) {
				if (StringUtils.isEmpty(getBlobSessionKey())) {
					return executeUpdateBlobQuery(statement,message.asInputStream());
				} 
				return executeUpdateBlobQuery(statement,session==null?null:session.get(getBlobSessionKey()));
			} 
			if ("updateClob".equalsIgnoreCase(queryExecutionContext.getQueryType())) {
				if (StringUtils.isEmpty(getClobSessionKey())) {
					return executeUpdateClobQuery(statement,message.asReader());
				} 
				return executeUpdateClobQuery(statement,session==null?null:session.get(getClobSessionKey()));
			} 
			if ("package".equalsIgnoreCase(queryExecutionContext.getQueryType())) {
				return executePackageQuery(queryExecutionContext);
			}
			Message result = executeOtherQuery(queryExecutionContext, message, session);
			if (getBatchSize()>0 && ++queryExecutionContext.iteration>=getBatchSize()) {
				int results[]=statement.executeBatch();
				int numRowsAffected=0;
				for (int i:results) {
					numRowsAffected+=i;
				}
				result = new Message("<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>");
				statement.clearBatch();
				queryExecutionContext.iteration=0;
			}
			return result;
		} catch (SenderException e) {
			if (e.getCause() instanceof SQLException) {
				SQLException sqle = (SQLException) e.getCause();
				if  (sqle.getErrorCode() == 1013) {
					throw new TimeOutException("Timeout of ["+getTimeout()+"] sec expired");
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
					if (Parameter.TYPE_INPUTSTREAM.equals(newParameterList.getParameter(i).getType())) {
						log.debug(getLogPrefix() + "Closing inputstream for parameter [" + newParameterList.getParameter(i).getName() + "]");
						try {
							InputStream inputStream = (InputStream) newParameterList.getParameter(i).getValue(null, message, session, true);
							inputStream.close();
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
			log.debug(getLogPrefix() + "Adjusting list of parameters ["	+ parameterListToString(parameterList) + "]");
		}

		StringBuffer buffer = new StringBuffer();
		int startPos = query.indexOf(UNP_START);
		if (startPos == -1)
			return query;
		char[] messageChars = query.toCharArray();
		int copyFrom = 0;
		ParameterList oldParameterList = new ParameterList();
		oldParameterList = (ParameterList) parameterList.clone();
		parameterList.clear();
		while (startPos != -1) {
			buffer.append(messageChars, copyFrom, startPos - copyFrom);
			int nextStartPos = query.indexOf(UNP_START, startPos + UNP_START.length());
			if (nextStartPos == -1) {
				nextStartPos = query.length();
			}
			int endPos =
					query.indexOf(UNP_END, startPos + UNP_START.length());

			if (endPos == -1 || endPos > nextStartPos) {
				log.warn(getLogPrefix() + "Found a start delimiter without an end delimiter at position ["	+ startPos + "] in ["+ query+ "]");
				buffer.append(messageChars, startPos, nextStartPos - startPos);
				copyFrom = nextStartPos;
			} else {
				String namedParam = query.substring(startPos + UNP_START.length(),endPos);
				Parameter param = oldParameterList.findParameter(namedParam);
				if (param!=null) {
					parameterList.add(param);
					buffer.append("?");
					copyFrom = endPos + UNP_END.length();
				} else {
					log.warn(getLogPrefix() + "Parameter ["	+ namedParam + "] is not found");
					buffer.append(messageChars, startPos, nextStartPos - startPos);
					copyFrom = nextStartPos;
				}
			}
			startPos = query.indexOf(UNP_START, copyFrom);
		}
		buffer.append(messageChars, copyFrom, messageChars.length - copyFrom);

		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() + "Adjusted list of parameters ["	+ parameterListToString(parameterList) + "]");
		}

		return buffer.toString();
	}

	private String parameterListToString(ParameterList parameterList) {
		String parameterListString = "";
		for (int i = 0; i < parameterList.size(); i++) {
			String key = parameterList.getParameter(i).getName();
			if (i ==0) {
				parameterListString = key;
			} else {
				parameterListString = parameterListString + ", " + key;
			}
		}
		return parameterListString;
	}

	protected Message getResult(ResultSet resultset) throws JdbcException, SQLException, IOException, JMSException {
		return getResult(resultset,null,null);
	}

	protected Message getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar) throws JdbcException, SQLException, IOException, JMSException {
		return getResult(resultset, blobSessionVar, clobSessionVar, null, null, null);
	}

	protected Message getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar, HttpServletResponse response, String contentType, String contentDisposition) throws JdbcException, SQLException, IOException, JMSException {
		String result=null;
		if (isScalar()) {
			if (resultset.next()) {
				//result = resultset.getString(1);
				ResultSetMetaData rsmeta = resultset.getMetaData();
				if (JdbcUtil.isBlobType(resultset, 1, rsmeta)) {
					if (response==null) {
						if (blobSessionVar!=null) {
							JdbcUtil.streamBlob(getDbmsSupport(), resultset, 1, getBlobCharset(), isBlobsCompressed(), getBlobBase64Direction(), blobSessionVar, isCloseOutputstreamOnExit());
							return new Message("");
						}
					} else {
						InputStream inputStream = JdbcUtil.getBlobInputStream(getDbmsSupport(), resultset, 1, isBlobsCompressed());
						if (StringUtils.isNotEmpty(contentType)) {
							response.setHeader("Content-Type", contentType); 
						}
						if (StringUtils.isNotEmpty(contentDisposition)) {
							response.setHeader("Content-Disposition", contentDisposition); 
						}

						if(getBlobBase64Direction() != null) {
							if ("decode".equalsIgnoreCase(getBlobBase64Direction())) {
								inputStream = new Base64InputStream (inputStream);
							}
							else if ("encode".equalsIgnoreCase(getBlobBase64Direction())) {
								inputStream = new Base64InputStream (inputStream, true);
							}
						}

						OutputStream outputStream = response.getOutputStream();
						Misc.streamToStream(inputStream, outputStream);
						log.debug(getLogPrefix() + "copied blob input stream [" + inputStream + "] to output stream [" + outputStream + "]");
						return new Message("");
					}
				}
				if (clobSessionVar!=null && JdbcUtil.isClobType(resultset, 1, rsmeta)) {
					JdbcUtil.streamClob(getDbmsSupport(), resultset, 1, clobSessionVar, isCloseOutputstreamOnExit());
					return new Message("");
				}
				result = JdbcUtil.getValue(getDbmsSupport(), resultset, 1, rsmeta, getBlobCharset(), isBlobsCompressed(), getNullValue(), isTrimSpaces(), isBlobSmartGet(), StringUtils.isEmpty(getBlobCharset()));
				if (resultset.wasNull()) {
					if (isScalarExtended()) {
						result = "[null]";
					} else {
						result = null;
					}
				} else {
					if (result.length()==0) {
						if (isScalarExtended()) {
							result="[empty]";
						}
					}
				}
			} else {
				if (isScalarExtended()) {
					result="[absent]";
				}
			}
		} else {
			// Create XML and give the maxlength as a parameter
			DB2XMLWriter db2xml = new DB2XMLWriter();
			db2xml.setNullValue(getNullValue());
			db2xml.setTrimSpaces(isTrimSpaces());
			db2xml.setBlobCharset(getBlobCharset());
			db2xml.setDecompressBlobs(isBlobsCompressed());
			db2xml.setGetBlobSmart(isBlobSmartGet());
			result = db2xml.getXML(getDbmsSupport(), resultset, getMaxRows(), isIncludeFieldDefinition());
		}
		return new Message(result);
	}
	

	private BlobOutputStream getBlobOutputStream(PreparedStatement statement, int blobColumn, boolean compressBlob) throws SQLException, JdbcException, IOException {
		log.debug(getLogPrefix() + "executing an update BLOB command");
		ResultSet rs = statement.executeQuery();
		XmlBuilder result=new XmlBuilder("result");
		JdbcUtil.warningsToXml(statement.getWarnings(),result);
		rs.next();
		Object blobUpdateHandle=getDbmsSupport().getBlobUpdateHandle(rs, blobColumn);
		OutputStream dbmsOutputStream = JdbcUtil.getBlobOutputStream(getDbmsSupport(), blobUpdateHandle, rs, blobColumn, compressBlob);
		return new BlobOutputStream(getDbmsSupport(), blobUpdateHandle, blobColumn, dbmsOutputStream, rs, result);
	}

	protected Message executeUpdateBlobQuery(PreparedStatement statement, Object message) throws SenderException{
		BlobOutputStream blobOutputStream=null;
		if (message!=null) {
			try {
				try {
					blobOutputStream = getBlobOutputStream(statement, blobColumn, isBlobsCompressed());
					InputStream inputStream = null;
					if (message instanceof Reader) {
						inputStream = new ReaderInputStream((Reader)message, getBlobCharset());
					} else if (message instanceof InputStream) {
						if (StringUtils.isNotEmpty(getStreamCharset())) {
							inputStream = new ReaderInputStream(StreamUtil.getCharsetDetectingInputStreamReader((InputStream)message, getBlobCharset()));
						} else {
							inputStream = (InputStream)message;
						}
					} else if (message instanceof byte[]) {
						inputStream = new ByteArrayInputStream((byte[])message);
					} else {
						inputStream = new ReaderInputStream(new StringReader(message.toString()), getBlobCharset());
					}
					Misc.streamToStream(inputStream,blobOutputStream,isCloseInputstreamOnExit());
				} finally {
					if (blobOutputStream!=null) {
						blobOutputStream.close();
					}
				}
			} catch (SQLException sqle) {
				throw new SenderException(getLogPrefix() + "got exception executing an update BLOB command",sqle );
			} catch (JdbcException e) {
				throw new SenderException(getLogPrefix() + "got exception executing an update BLOB command",e );
			} catch (IOException e) {
				throw new SenderException(getLogPrefix() + "got exception executing an update BLOB command",e );
			}
		}
		return blobOutputStream==null ? null : new Message(blobOutputStream.getWarnings().toXML());
	}

	private ClobWriter getClobWriter(PreparedStatement statement, int clobColumn) throws SQLException, JdbcException {
		log.debug(getLogPrefix() + "executing an update CLOB command");
		ResultSet rs = statement.executeQuery();
		XmlBuilder result=new XmlBuilder("result");
		JdbcUtil.warningsToXml(statement.getWarnings(),result);
		rs.next();
		Object clobUpdateHandle=getDbmsSupport().getClobUpdateHandle(rs, clobColumn);
		Writer dbmsWriter = getDbmsSupport().getClobWriter(rs, clobColumn, clobUpdateHandle);
		return new ClobWriter(getDbmsSupport(), clobUpdateHandle, clobColumn, dbmsWriter, rs, result);
	}

	protected Message executeUpdateClobQuery(PreparedStatement statement, Object message) throws SenderException{
		ClobWriter clobWriter=null;
		if (message!=null) {
			try {
				try {
					clobWriter = getClobWriter(statement, getClobColumn());
					Reader reader=null;
					if (message instanceof Reader) {
						reader = (Reader)message;
					} else if (message instanceof InputStream) {
						InputStream inStream = (InputStream)message;
						if (StringUtils.isNotEmpty(getStreamCharset())) {
							reader = StreamUtil.getCharsetDetectingInputStreamReader(inStream,getStreamCharset());
						} else {
							reader = StreamUtil.getCharsetDetectingInputStreamReader(inStream);
						}
					}
					if (reader!=null) {
						Misc.readerToWriter(reader, clobWriter, isCloseInputstreamOnExit());
					} else {
						clobWriter.write(message.toString());
					}
				} finally {
					if (clobWriter!=null) {
						clobWriter.close();
					}
				}
			} catch (SQLException sqle) {
				throw new SenderException(getLogPrefix() + "got exception executing an update CLOB command",sqle );
			} catch (JdbcException e) {
				throw new SenderException(getLogPrefix() + "got exception executing an update CLOB command",e );
			} catch (IOException e) {
				throw new SenderException(getLogPrefix() + "got exception executing an update CLOB command",e );
			}
		}
		return clobWriter==null ? null : new Message(clobWriter.getWarnings().toXML());
	}

	public boolean canProvideOutputStream() {
		return false; // FixedQuerySender returns true for updateBlob and updateClob
	}

	@Override
	public boolean supportsOutputStreamPassThrough() {
		return false;
	}

	@Override
	public MessageOutputStream provideOutputStream(IPipeLineSession session, IForwardTarget next) throws StreamingException {
		if (!canProvideOutputStream()) {
			return null;
		}
		final Connection connection;
		QueryExecutionContext queryExecutionContext;
		try {
			connection = getConnectionWithTimeout(getTimeout());
			queryExecutionContext = getQueryExecutionContext(connection, null, session);
		} catch (JdbcException | ParameterException | SQLException | SenderException | TimeOutException e) {
			throw new StreamingException(getLogPrefix() + "cannot getQueryExecutionContext",e);
		}
		try {
			PreparedStatement statement=queryExecutionContext.getStatement();
			if (queryExecutionContext.getParameterList() != null) {
				JdbcUtil.applyParameters(getDbmsSupport(), statement, queryExecutionContext.getParameterList().getValues(new Message(""), session));
			}
			if ("updateBlob".equalsIgnoreCase(queryExecutionContext.getQueryType())) {
				return new MessageOutputStream(this, getBlobOutputStream(statement, blobColumn, isBlobsCompressed()), next) {
					@Override
					public void afterClose() throws SQLException {
						connection.close();
						log.warn(getLogPrefix()+"warnings: "+((BlobOutputStream)requestStream).getWarnings().toXML());
					}
				};
			}
			if ("updateClob".equalsIgnoreCase(queryExecutionContext.getQueryType())) {
				return new MessageOutputStream(this, getClobWriter(statement, getClobColumn()), next) {
					@Override
					public void afterClose() throws SQLException {
						connection.close();
						log.warn(getLogPrefix()+"warnings: "+((ClobWriter)requestStream).getWarnings().toXML());
					}
				};
			} 
			throw new IllegalStateException(getLogPrefix()+"illegal queryType ["+queryExecutionContext.getQueryType()+"], must be 'updateBlob' or 'updateClob'");
		} catch (JdbcException | SQLException | IOException | ParameterException e) {
			throw new StreamingException(getLogPrefix() + "cannot update CLOB or BLOB",e);
		}
	}

	protected Message executeSelectQuery(PreparedStatement statement, Object blobSessionVar, Object clobSessionVar) throws SenderException{
		return executeSelectQuery(statement, blobSessionVar, clobSessionVar, null, null, null);
	}

	protected Message executeSelectQuery(PreparedStatement statement, Object blobSessionVar, Object clobSessionVar, HttpServletResponse response, String contentType, String contentDisposition) throws SenderException{
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
				return getResult(resultset,blobSessionVar,clobSessionVar, response, contentType, contentDisposition);
			}
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",sqle );
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",e );
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",e );
		} catch (JMSException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",e );
		}
	}

	protected Message executePackageQuery(QueryExecutionContext queryExecutionContext) throws SenderException, JdbcException, IOException, JMSException {
		Connection connection = queryExecutionContext.getConnection();
		String query = queryExecutionContext.getQuery();
		Object[] paramArray = new Object[10];
		String callMessage = fillParamArray(paramArray, query);
		ResultSet resultset = null;
		try {
			CallableStatement pstmt = connection.prepareCall(callMessage);
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
				return getResult(getReturnedColumns(getColumnsReturnedList(),queryExecutionContext.getStatement()));
			}
			if (isScalar()) {
				return new Message(Integer.toString(numRowsAffected));
			}
			return new Message("<result><rowsupdated>"+ numRowsAffected	+ "</rowsupdated></result>");
		} catch (SQLException sqle) {
			throw new SenderException(
				getLogPrefix() + "got exception executing a package SQL command",
				sqle);
		} finally {
			try {
				if (resultset != null) {
					resultset.close();
				}
			} catch (SQLException e) {
				log.warn(
					new SenderException(
						getLogPrefix() + "got exception closing resultset",
						e));
			}
		}
	}

	protected Message executeOtherQuery(QueryExecutionContext queryExecutionContext, Message message, IPipeLineSession session) throws SenderException {
		Connection connection = queryExecutionContext.getConnection(); 
		PreparedStatement statement = queryExecutionContext.getStatement(); 
		String query = queryExecutionContext.getQuery();
		ParameterList parameterList = queryExecutionContext.getParameterList();
		PreparedStatement resStmt = queryExecutionContext.getResultQueryStatement();
		return executeOtherQuery(connection, statement, query, resStmt, message, session, parameterList);
	}
	
	protected Message executeOtherQuery(Connection connection, PreparedStatement statement, String query, PreparedStatement resStmt, Message message, IPipeLineSession session, ParameterList parameterList) throws SenderException {
		try {
			int numRowsAffected = 0;
			if (StringUtils.isNotEmpty(getRowIdSessionKey())) {
				CallableStatement cstmt = getCallWithRowIdReturned(connection, query);
				int ri = 1;
				if (parameterList != null) {
					ParameterValueList parameters = parameterList.getValues(message, session);
					JdbcUtil.applyParameters(getDbmsSupport(), cstmt, parameters);
					ri = parameters.size() + 1;
				}
				cstmt.registerOutParameter(ri, Types.VARCHAR);
				log.debug(getLogPrefix() + "executing a SQL command");
				numRowsAffected = cstmt.executeUpdate();
				String rowId = cstmt.getString(ri);
				if (session!=null) session.put(getRowIdSessionKey(), rowId);
			} else {
				log.debug(getLogPrefix() + "executing a SQL command");
				if (getBatchSize()>0) {
					statement.addBatch();
				} else {
					numRowsAffected = statement.executeUpdate();
				}
			}
			if (resStmt!=null) {
				if (log.isDebugEnabled()) log.debug("obtaining result from [" + convertedResultQuery + "]");
				try (ResultSet rs = resStmt.executeQuery()) {
					return getResult(rs);
				}
			}
			if (getColumnsReturnedList()!=null) {
				return getResult(getReturnedColumns(getColumnsReturnedList(),statement));
			}
			if (isScalar()) {
				return new Message(Integer.toString(numRowsAffected));
			}
			return new Message("<result>"+(getBatchSize()>0?"addedToBatch":"<rowsupdated>" + numRowsAffected + "</rowsupdated>")+"</result>");
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
		// Watch out, this cannot handle nested parentheses
//		String packageCall = message.substring(startHaakje, eindHaakje + 1);
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
									java.util.Date nDate = (java.util.Date)sdf.parseObject(element.toString());
									Timestamp sqlTimestamp = new Timestamp(nDate.getTime());
									paramArray[idx] = sqlTimestamp;
									 
								} else {
									String pattern = "yyyy-MM-dd";
									SimpleDateFormat sdf = new SimpleDateFormat(pattern);
									java.util.Date nDate;
									nDate = sdf.parse(element.toString());
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
			StringBuffer newMessage = new StringBuffer(message.substring(0, startHaakje + 1));
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
		public String getPackageContent() {
			return packageContent;
		}

	/**
	 * Controls wheter output is expected from the query. 
	 * Possible values: 
	 * <ul>
	 * <li>select:</li> output is expected
	 * <li><i>anything else</i>:</li> no output is expected, the number of rows affected is returned
	 * </ul>
	 */
	@IbisDoc({"1", "One of: <ul>" 
				+ "<li><code>select</code> for queries that return data</li>" 
				+ "<li><code>updateBlob</code> for queries that update a BLOB</li>" 
				+ "<li><code>updateClob</code> for queries that update a CLOB</li>" 
				+ "<li><code>package</code> to execute Oracle PL/SQL package</li>" 
				+ "<li><code>other</code> or anything else for queries that return no data.</li>" 
				+ "</ul>", "<code>other</code>"})
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}
	public String getQueryType() {
		return queryType;
	}

	@IbisDoc({"2", "When <code>true</code>, the value of the first column of the first row (or the startrow) is returned as the only result, as a simple non-xml value", "false"})
	public void setScalar(boolean b) {
		scalar = b;
	}
	public boolean isScalar() {
		return scalar;
	}

	@IbisDoc({"3", "When <code>true</code> and <code>scalar</code> is also <code>true</code>, but returns no value, one of the following is returned: <ul>" 
				+ "<li>'[absent]' no row is found</li>" 
				+ "<li>'[null]' a row is found, but the value is a SQL-NULL</li>" 
				+ "<li>'[empty]' a row is found, but the value is a empty string</li>" 
				+ "</ul>", "false"})
	public void setScalarExtended(boolean b) {
		scalarExtended = b;
	}
	public boolean isScalarExtended() {
		return scalarExtended;
	}

	@IbisDoc({"4", "The maximum number of rows to be returned from the output of <code>select</code> queries", "-1 (unlimited)"})
	public void setMaxRows(int i) {
		maxRows = i;
	}
	public int getMaxRows() {
		return maxRows;
	}

	@IbisDoc({"5", "The number of the first row to be returned from the output of <code>select</code> queries. Rows before this are skipped from the output.", "1"})
	public void setStartRow(int i) {
		startRow = i;
	}
	public int getStartRow() {
		return startRow;
	}

	@IbisDoc({"6", "Value used in result as contents of fields that contain no value (sql-null)", "<i>empty string</i>"})
	public void setNullValue(String string) {
		nullValue = string;
	}
	public String getNullValue() {
		return nullValue;
	}

	@IbisDoc({"7", "Query that can be used to obtain result of side-effect of update-query, like generated value of sequence. Example: SELECT mysequence.currval FROM dual", ""})
	public void setResultQuery(String string) {
		resultQuery = string;
	}
	public String getResultQuery() {
		return resultQuery;
	}

	@IbisDoc({"8", "Comma separated list of columns whose values are to be returned. Works only if the driver implements jdbc 3.0 getGeneratedKeys()", ""})
	public void setColumnsReturned(String string) {
		columnsReturned = string;
	}
	public String getColumnsReturned() {
		return columnsReturned;
	}
	public String[] getColumnsReturnedList() {
		return columnsReturnedList;
	}

	@IbisDoc({"9", "When <code>true</code>, every string in the message which equals <code>"+UNP_START+"paramname+UNP_END+</code> will be replaced by the setter method for the corresponding parameter. The parameters don't need to be in the correct order and unused parameters are skipped.", "false"})
	public void setUseNamedParams(boolean b) {
		useNamedParams = b;
	}
	public boolean isUseNamedParams() {
		return useNamedParams;
	}

	@IbisDoc({"10", "when <code>true</code>, the result contains besides the returned rows also a header with information about the fetched fields", "application default (true)"})
	public void setIncludeFieldDefinition(boolean b) {
		includeFieldDefinition = b;
	}
	public boolean isIncludeFieldDefinition() {
		return includeFieldDefinition;
	}

	@IbisDoc({"11", "Remove trailing blanks from all result values.", "true"})
	public void setTrimSpaces(boolean b) {
		trimSpaces = b;
	}
	public boolean isTrimSpaces() {
		return trimSpaces;
	}

	@IbisDoc({"12", "If specified, the rowid of the processed row is put in the pipelinesession under the specified key (only applicable for <code>querytype=other</code>). <b>Note:</b> If multiple rows are processed a SqlException is thrown.", ""})
	public void setRowIdSessionKey(String string) {
		rowIdSessionKey = string;
	}
	public String getRowIdSessionKey() {
		return rowIdSessionKey;
	}


	@IbisDoc({"13", "If set, the result is streamed to the HttpServletResponse object of the RestServiceDispatcher (instead of passed as a String)", "false"})
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}
	public boolean isStreamResultToServlet() {
		return streamResultToServlet;
	}

	@IbisDoc({"14", "If set, the SQL dialect in which the queries are written and should be translated from to the actual SQL dialect", ""})
	public void setSqlDialect(String string) {
		sqlDialect = string;
	}
	public String getSqlDialect() {
		return sqlDialect;
	}

	@IbisDoc({"15", "When set <code>true</code>, exclusive row-level locks are obtained on all the rows identified by the select statement (e.g. by appending ' FOR UPDATE NOWAIT SKIP LOCKED' to the end of the query)", "false"})
	public void setLockRows(boolean b) {
		lockRows = b;
	}
	public boolean isLockRows() {
		return lockRows;
	}

	@IbisDoc({"16", "when set and >=0, ' FOR UPDATE WAIT #' is used instead of ' FOR UPDATE NOWAIT SKIP LOCKED'", "-1"})
	public void setLockWait(int i) {
		lockWait = i;
	}
	public int getLockWait() {
		return lockWait;
	}





	@IbisDoc({ "", "true" })
	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}
	@Override
	public boolean isSynchronous() {
		return synchronous;
	}





	@IbisDoc({"20", "Only for querytype 'updateBlob': column that contains the BLOB to be updated", "1"})
	public void setBlobColumn(int i) {
		blobColumn = i;
	}
	public int getBlobColumn() {
		return blobColumn;
	}

	@IbisDoc({"21", "For querytype 'updateBlob': key of session variable that contains the data (String or InputStream) to be loaded to the BLOB. When empty, the input of the pipe, which then must be a String, is used.<br/>"+
					"For querytype 'select': key of session variable that contains the OutputStream, Writer or Filename to write the BLOB to", ""})
	public void setBlobSessionKey(String string) {
		blobSessionKey = string;
	}
	public String getBlobSessionKey() {
		return blobSessionKey;
	}

	@IbisDoc({"22", "controls whether blobdata is stored compressed in the database", "true"})
	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}
	public boolean isBlobsCompressed() {
		return blobsCompressed;
	}

	@IbisDoc({"23", "controls whether the streamed blobdata will need to be base64 <code>encode</code> or <code>decode</code> or not.", ""})
	public void setBlobBase64Direction(String string) {
		blobBase64Direction = string;
	}
	
	public String getBlobBase64Direction() {
		return blobBase64Direction;
	}
	
	@IbisDoc({"24", "Charset that is used to read and write BLOBs. This assumes the blob contains character data. " + 
				"If blobCharset and blobSessionKey are not set, blobs are base64 encoded after being read to accommodate for the fact that senders need to return a String", "UTF-8"})
	public void setBlobCharset(String string) {
		blobCharset = string;
	}
	public String getBlobCharset() {
		return blobCharset;
	}

	@IbisDoc({"25", "Controls automatically whether blobdata is stored compressed and/or serialized in the database", "false"})
	public void setBlobSmartGet(boolean b) {
		blobSmartGet = b;
	}
	public boolean isBlobSmartGet() {
		return blobSmartGet;
	}


	@IbisDoc({"30", "Only for querytype 'updateClob': column that contains the CLOB to be updated", "1"})
	public void setClobColumn(int i) {
		clobColumn = i;
	}
	public int getClobColumn() {
		return clobColumn;
	}

	@IbisDoc({"31", "For querytype 'updateClob': key of session variable that contains the CLOB (String or InputStream) to be loaded to the CLOB. When empty, the input of the pipe, which then must be a String, is used.<br/>"+
					"For querytype 'select': key of session variable that contains the OutputStream, Writer or Filename to write the CLOB to", ""})
	public void setClobSessionKey(String string) {
		clobSessionKey = string;
	}
	public String getClobSessionKey() {
		return clobSessionKey;
	}

	@IbisDoc({"40", "When set to <code>false</code>, the Inputstream is not closed after it has been used to read a BLOB or CLOB", "true"})
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}
	public boolean isCloseInputstreamOnExit() {
		return closeInputstreamOnExit;
	}

	@IbisDoc({"41", "When set to <code>false</code>, the Outputstream is not closed after BLOB or CLOB has been written to it", "true"})
	public void setCloseOutputstreamOnExit(boolean b) {
		closeOutputstreamOnExit = b;
	}
	public boolean isCloseOutputstreamOnExit() {
		return closeOutputstreamOnExit;
	}


	@IbisDoc({"42", "Charset used when reading a stream (that is e.g. going to be written to a BLOB or CLOB). When empty, the stream is copied directly to the BLOB, without conversion", ""})
	 public void setStreamCharset(String string) {
		streamCharset = string;
	}
	public String getStreamCharset() {
		return streamCharset;
	}

	@IbisDoc({"43", "If true, then select queries are executed in a way that avoids taking locks, e.g. with isolation mode 'read committed' instead of 'repeatable read'.", "false"})
	public void setAvoidLocking(boolean avoidLocking) {
		this.avoidLocking = avoidLocking;
	}
	public boolean isAvoidLocking() {
		return avoidLocking;
	}
	
	public int getBatchSize() {
		return 0;
	}


}
