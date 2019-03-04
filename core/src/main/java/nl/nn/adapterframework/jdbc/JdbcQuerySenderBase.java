/*
   Copyright 2013 Nationale-Nederlanden

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
import java.io.InputStreamReader;
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

import javax.jms.JMSException;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Misc;
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
 *   <li>elements that contain a dash ('-') are assumed to be dates (yyyy-MM-dd) or timestamps (yyyy-MM-dd HH:mm:ss)</li>
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
public abstract class JdbcQuerySenderBase extends JdbcSenderBase {

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
	private String blobCharset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
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
	}

	
	/**
	 * Obtain a prepared statement to be executed.
	 * Method-stub to be overridden in descender-classes.
	 */
	protected abstract PreparedStatement getStatement(Connection con, String correlationID, String message, boolean updateable) throws JdbcException, SQLException;
	
	private PreparedStatement prepareQueryWithColunmsReturned(Connection con, String query, String[] columnsReturned) throws SQLException {
		return con.prepareStatement(query,columnsReturned);
	}

	protected PreparedStatement prepareQuery(Connection con, String query, boolean updateable) throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() +"preparing statement for query ["+query+"]");
		}
		String[] columnsReturned = getColumnsReturnedList();
		if (columnsReturned!=null) {
			return prepareQueryWithColunmsReturned(con,query,columnsReturned);
		}
		return con.prepareStatement(query,ResultSet.TYPE_FORWARD_ONLY,updateable?ResultSet.CONCUR_UPDATABLE:ResultSet.CONCUR_READ_ONLY);
	}

	protected CallableStatement getCallWithRowIdReturned(Connection con, String correlationID, String message) throws SQLException {
		String callMessage = "BEGIN " + message + " RETURNING ROWID INTO ?; END;";
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() +"preparing statement for query ["+callMessage+"]");
		}
		return con.prepareCall(callMessage);
	}

	protected ResultSet getReturnedColumns(String[] columns, PreparedStatement st) throws SQLException {
		return st.getGeneratedKeys();
	}

	@Override
	protected String sendMessage(Connection connection, String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		PreparedStatement statement=null;
		ParameterList newParamList = new ParameterList();
		if (paramList != null) {
			newParamList = (ParameterList) paramList.clone();
		}
		if (isUseNamedParams()) {
			message = adjustParamList(newParamList, message);
		}
		try {
			boolean updateBlob="updateBlob".equalsIgnoreCase(getQueryType());
			boolean updateClob="updateClob".equalsIgnoreCase(getQueryType());
			log.debug(getLogPrefix() + "obtaining prepared statement to execute");
			statement = getStatement(connection, correlationID, message, updateBlob||updateClob);
			log.debug(getLogPrefix() + "obtained prepared statement to execute");
			statement.setQueryTimeout(getTimeout());
			if (prc != null && paramList != null) {
				applyParameters(statement, prc.getValues(newParamList));
			}
			if ("select".equalsIgnoreCase(getQueryType())) {
				Object blobSessionVar=null;
				Object clobSessionVar=null;
				if (prc!=null && StringUtils.isNotEmpty(getBlobSessionKey())) {
					blobSessionVar=prc.getSession().get(getBlobSessionKey());
				}
				if (prc!=null && StringUtils.isNotEmpty(getClobSessionKey())) {
					clobSessionVar=prc.getSession().get(getClobSessionKey());
				}
				if (isStreamResultToServlet()) {
					HttpServletResponse response = (HttpServletResponse) prc.getSession().get(IPipeLineSession.HTTP_RESPONSE_KEY);
					String contentType = (String) prc.getSession().get("contentType");
					String contentDisposition = (String) prc.getSession().get("contentDisposition");
					return executeSelectQuery(statement,blobSessionVar,clobSessionVar, response, contentType, contentDisposition);
				} else {
					return executeSelectQuery(statement,blobSessionVar,clobSessionVar);
				}
			} 
			if (updateBlob) {
				if (StringUtils.isEmpty(getBlobSessionKey())) {
					return executeUpdateBlobQuery(statement,message);
				} 
				return executeUpdateBlobQuery(statement,prc==null?null:prc.getSession().get(getBlobSessionKey()));
			} 
			if (updateClob) {
				if (StringUtils.isEmpty(getClobSessionKey())) {
					return executeUpdateClobQuery(statement,message);
				} 
				return executeUpdateClobQuery(statement,prc==null?null:prc.getSession().get(getClobSessionKey()));
			} 
			if ("package".equalsIgnoreCase(getQueryType())) {
				return executePackageQuery(connection, statement, message);
			}
			return executeOtherQuery(connection, correlationID, statement, message, prc, newParamList);
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
			try {
				if (statement!=null) {
					statement.close();
				}
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "got exception closing SQL statement",e ));
			}
			if (isCloseInputstreamOnExit()) {
				if (paramList!=null) {
					for (int i = 0; i < paramList.size(); i++) {
						if (Parameter.TYPE_INPUTSTREAM.equals(paramList.getParameter(i).getType())) {
							log.debug(getLogPrefix() + "Closing inputstream for parameter [" + paramList.getParameter(i).getName() + "]");
							try {
								InputStream inputStream = (InputStream) paramList.getParameter(i).getValue(null, prc);
								inputStream.close();
							} catch (Exception e) {
								log.warn(new SenderException(getLogPrefix() + "got exception closing inputstream", e));
							}
						}
					}
				}
			}
		}
	}

	private String adjustParamList(ParameterList paramList, String message) throws SenderException {
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() + "Adjusting list of parameters ["	+ paramListToString(paramList) + "]");
		}

		StringBuffer buffer = new StringBuffer();
		int startPos = message.indexOf(UNP_START);
		if (startPos == -1)
			return message;
		char[] messageChars = message.toCharArray();
		int copyFrom = 0;
		ParameterList oldParamList = new ParameterList();
		oldParamList = (ParameterList) paramList.clone();
		paramList.clear();
		while (startPos != -1) {
			buffer.append(messageChars, copyFrom, startPos - copyFrom);
			int nextStartPos =
				message.indexOf(
					UNP_START,
					startPos + UNP_START.length());
			if (nextStartPos == -1) {
				nextStartPos = message.length();
			}
			int endPos =
				message.indexOf(UNP_END, startPos + UNP_START.length());

			if (endPos == -1 || endPos > nextStartPos) {
				log.warn(getLogPrefix() + "Found a start delimiter without an end delimiter at position ["	+ startPos + "] in ["+ message+ "]");
				buffer.append(messageChars, startPos, nextStartPos - startPos);
				copyFrom = nextStartPos;
			} else {
				String namedParam = message.substring(startPos + UNP_START.length(),endPos);
				Parameter param = oldParamList.findParameter(namedParam);
				if (param!=null) {
					paramList.add(param);
					buffer.append("?");
					copyFrom = endPos + UNP_END.length();
				} else {
					log.warn(getLogPrefix() + "Parameter ["	+ namedParam + "] is not found");
					buffer.append(messageChars, startPos, nextStartPos - startPos);
					copyFrom = nextStartPos;
				}
			}
			startPos = message.indexOf(UNP_START, copyFrom);
		}
		buffer.append(messageChars, copyFrom, messageChars.length - copyFrom);

		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() + "Adjusted list of parameters ["	+ paramListToString(paramList) + "]");
		}

		return buffer.toString();
	}

	private String paramListToString(ParameterList paramList) {
		String paramListString = "";
		for (int i = 0; i < paramList.size(); i++) {
			String key = paramList.getParameter(i).getName();
			if (i ==0) {
				paramListString = key;
			} else {
				paramListString = paramListString + ", " + key;
			}
		}
		return paramListString;
	}

	protected String getResult(ResultSet resultset) throws JdbcException, SQLException, IOException, JMSException {
		return getResult(resultset,null,null);
	}

	protected String getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar) throws JdbcException, SQLException, IOException, JMSException {
		return getResult(resultset, blobSessionVar, clobSessionVar, null, null, null);
	}
	
	protected String getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar, HttpServletResponse response, String contentType, String contentDisposition) throws JdbcException, SQLException, IOException, JMSException {
		String result=null;
		if (isScalar()) {
			if (resultset.next()) {
				//result = resultset.getString(1);
				ResultSetMetaData rsmeta = resultset.getMetaData();
				if (JdbcUtil.isBlobType(resultset, 1, rsmeta)) {
					if (response==null) {
						if (blobSessionVar!=null) {
							JdbcUtil.streamBlob(resultset, 1, getBlobCharset(), isBlobsCompressed(), getBlobBase64Direction(), blobSessionVar, isCloseOutputstreamOnExit());
							return "";
						}
					} else {
						InputStream inputStream = JdbcUtil.getBlobInputStream(resultset, 1, isBlobsCompressed());
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
						return "";
					}
				}
				if (clobSessionVar!=null && JdbcUtil.isClobType(resultset, 1, rsmeta)) {
					JdbcUtil.streamClob(resultset, 1, clobSessionVar, isCloseOutputstreamOnExit());
					return "";
				}
				result = JdbcUtil.getValue(resultset, 1, rsmeta, getBlobCharset(), isBlobsCompressed(), getNullValue(), isTrimSpaces(), isBlobSmartGet(), StringUtils.isEmpty(getBlobCharset()));
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
			result = db2xml.getXML(resultset, getMaxRows(), isIncludeFieldDefinition());
		}
		return result;
	}
	

	protected String executeUpdateBlobQuery(PreparedStatement statement, Object message) throws SenderException{
		ResultSet rs=null;
		try {
			log.debug(getLogPrefix() + "executing an updating BLOB command");
			rs = statement.executeQuery();
			XmlBuilder result=new XmlBuilder("result");
			JdbcUtil.warningsToXml(statement.getWarnings(),result);
			rs.next();
			if (message instanceof Reader) {
				Object blobHandle=getDbmsSupport().getBlobUpdateHandle(rs, blobColumn);
				Reader inReader = (Reader)message;
				Writer writer = JdbcUtil.getBlobWriter(getDbmsSupport(), blobHandle, rs, blobColumn, getBlobCharset(), isBlobsCompressed());
				Misc.readerToWriter(inReader,writer,isCloseInputstreamOnExit());
				writer.close();
				getDbmsSupport().updateBlob(rs, blobColumn, blobHandle);
			} else if (message instanceof InputStream) {
				Object blobHandle=getDbmsSupport().getBlobUpdateHandle(rs, blobColumn);
				InputStream inStream = (InputStream)message;
				if (StringUtils.isNotEmpty(getStreamCharset())) {
					Writer writer = JdbcUtil.getBlobWriter(getDbmsSupport(), blobHandle, rs, blobColumn, getBlobCharset(), isBlobsCompressed());
					Reader reader = new InputStreamReader(inStream,getStreamCharset());
					Misc.readerToWriter(reader,writer,isCloseInputstreamOnExit());
					writer.close();
				} else {
					OutputStream outStream = JdbcUtil.getBlobOutputStream(getDbmsSupport(), blobHandle, rs, blobColumn, isBlobsCompressed());
					Misc.streamToStream(inStream,outStream,isCloseInputstreamOnExit());
					outStream.close();
				}
				getDbmsSupport().updateBlob(rs, blobColumn, blobHandle);
			} else if (message instanceof byte[]) {
				JdbcUtil.putByteArrayAsBlob(getDbmsSupport(), rs, blobColumn, (byte[])message, isBlobsCompressed());
			} else {
				JdbcUtil.putStringAsBlob(getDbmsSupport(), rs, blobColumn, (String)message, getBlobCharset(), isBlobsCompressed());
			}
			
			rs.updateRow();
			JdbcUtil.warningsToXml(rs.getWarnings(),result);
			return result.toXML();
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing an updating BLOB command",sqle );
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception executing an updating BLOB command",e );
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "got exception executing an updating BLOB command",e );
		} finally {
			try {
				if (rs!=null) {
					rs.close();
				}
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "got exception closing resultset",e));
			}
		}
	}
	
	protected String executeUpdateClobQuery(PreparedStatement statement, Object message) throws SenderException{
		ResultSet rs=null;
		try {
			log.debug(getLogPrefix() + "executing an updating CLOB command");
			rs = statement.executeQuery();
			XmlBuilder result=new XmlBuilder("result");
			JdbcUtil.warningsToXml(statement.getWarnings(),result);
			rs.next();
			if (message instanceof Reader) {
				Object clobHandle=getDbmsSupport().getClobUpdateHandle(rs, clobColumn);
				Reader inReader = (Reader)message;
				Writer writer = getDbmsSupport().getClobWriter(rs, clobColumn, clobHandle);
				Misc.readerToWriter(inReader,writer,isCloseInputstreamOnExit());
				writer.close();
				getDbmsSupport().updateClob(rs, clobColumn, clobHandle);
			} else if (message instanceof InputStream) {
				Object clobHandle=getDbmsSupport().getClobUpdateHandle(rs, clobColumn);
				InputStream inStream = (InputStream)message;
				Reader reader;
				if (StringUtils.isNotEmpty(getStreamCharset())) {
					reader = new InputStreamReader(inStream,getStreamCharset());
				} else {
					reader = new InputStreamReader(inStream);
				}
				Writer writer = getDbmsSupport().getClobWriter(rs, clobColumn, clobHandle);
				Misc.readerToWriter(reader,writer,isCloseInputstreamOnExit());
				writer.close();
				getDbmsSupport().updateClob(rs, clobColumn, clobHandle);
			} else {
				JdbcUtil.putStringAsClob(getDbmsSupport(), rs, clobColumn, (String)message);
			}
			rs.updateRow();
			JdbcUtil.warningsToXml(rs.getWarnings(),result);
			return result.toXML();
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing an updating CLOB command",sqle );
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception executing an updating CLOB command",e );
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "got exception executing an updating CLOB command",e );
		} finally {
			try {
				if (rs!=null) {
					rs.close();
				}
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "got exception closing resultset",e));
			}
		}
	}
	
	protected String executeSelectQuery(PreparedStatement statement, Object blobSessionVar, Object clobSessionVar) throws SenderException{
		return executeSelectQuery(statement, blobSessionVar, clobSessionVar, null, null, null);
	}
	
	protected String executeSelectQuery(PreparedStatement statement, Object blobSessionVar, Object clobSessionVar, HttpServletResponse response, String contentType, String contentDisposition) throws SenderException{
		ResultSet resultset=null;
		try {
			if (getMaxRows()>0) {
				statement.setMaxRows(getMaxRows()+ ( getStartRow()>1 ? getStartRow()-1 : 0));
			}

			log.debug(getLogPrefix() + "executing a SELECT SQL command");
			resultset = statement.executeQuery();

			if (getStartRow()>1) {
				resultset.absolute(getStartRow()-1);
				log.debug(getLogPrefix() + "Index set at position: " +  resultset.getRow() );
			}				
			return getResult(resultset,blobSessionVar,clobSessionVar, response, contentType, contentDisposition);
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",sqle );
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",e );
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",e );
		} catch (JMSException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",e );
		} finally {
			try {
				if (resultset!=null) {
					resultset.close();
				}
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "got exception closing resultset",e));
			}
		}
	}
	
	protected String executePackageQuery(Connection connection, PreparedStatement statement, String message) throws SenderException, JdbcException, IOException, JMSException {
		Object[] paramArray = new Object[10];
		String callMessage = fillParamArray(paramArray, message);
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
			if (message.indexOf('?') != -1) {
				pstmt.registerOutParameter(var, Types.CLOB); // make sure enough space is available for result...
			}
			if ("xml".equalsIgnoreCase(getPackageContent())) {
				log.debug(getLogPrefix() + "executing a package SQL command");
				pstmt.executeUpdate();
				String pUitvoer = pstmt.getString(var);
				return pUitvoer;
			} 
			log.debug(getLogPrefix() + "executing a package SQL command");
			int numRowsAffected = pstmt.executeUpdate();
			if (StringUtils.isNotEmpty(getResultQuery())) {
				Statement resStmt = null;
				try {
					resStmt = connection.createStatement();
					log.debug("obtaining result from ["	+ getResultQuery() + "]");
					ResultSet rs = resStmt.executeQuery(getResultQuery());
					return getResult(rs);
				} finally {
					if (resStmt != null) {
						resStmt.close();
					}
				}
			}
			if (getColumnsReturnedList() != null) {
				return getResult(getReturnedColumns(getColumnsReturnedList(),statement));
			}
			if (isScalar()) {
				return numRowsAffected + "";
			}
			return "<result><rowsupdated>"+ numRowsAffected	+ "</rowsupdated></result>";
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

	protected String executeOtherQuery(Connection connection, String correlationID, PreparedStatement statement, String message, ParameterResolutionContext prc, ParameterList newParamList) throws SenderException{
		ResultSet resultset=null;
		try {
			int numRowsAffected = 0;
			if (StringUtils.isNotEmpty(getRowIdSessionKey())) {
				CallableStatement cstmt = getCallWithRowIdReturned(connection, correlationID, message);
				int ri = 1;
				if (prc != null && paramList != null) {
					ParameterValueList parameters = prc.getValues(newParamList);
					applyParameters(cstmt, parameters);
					ri = parameters.size() + 1;
				}
				cstmt.registerOutParameter(ri, Types.VARCHAR);
				log.debug(getLogPrefix() + "executing a SQL command");
				numRowsAffected = cstmt.executeUpdate();
				String rowId = cstmt.getString(ri);
				if (prc!=null) prc.getSession().put(getRowIdSessionKey(), rowId);
			} else {
				log.debug(getLogPrefix() + "executing a SQL command");
				numRowsAffected = statement.executeUpdate();
			}
			if (StringUtils.isNotEmpty(getResultQuery())) {
				Statement resStmt = null;
				try { 
					resStmt = connection.createStatement();
					log.debug("obtaining result from ["+getResultQuery()+"]");
					ResultSet rs = resStmt.executeQuery(getResultQuery());
					return getResult(rs);
				} finally {
					if (resStmt!=null) {
						resStmt.close();
					}
				}
			}
			if (getColumnsReturnedList()!=null) {
				return getResult(getReturnedColumns(getColumnsReturnedList(),statement));
			}
			if (isScalar()) {
				return numRowsAffected+"";
			}
			return "<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>";
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing a SQL command",sqle );
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SQL command",e );
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SQL command",e );
		} catch (JMSException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SQL command",e );
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix() + "got exception evaluating parameters", e);
		} finally {
			try {
				if (resultset!=null) {
					resultset.close();
				}
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "got exception closing resultset",e));
			}
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
/*
   Copyright 2013 Nationale-Nederlanden

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
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}
	public String getQueryType() {
		return queryType;
	}

	/**
	 * Sets the maximum number of rows to be returned from the output of <code>select</code> queries.
	 * The default is 0, which will return all rows.
	 */
	@IbisDoc({"maximum number of rows returned", "-1 (unlimited)"})
	public void setMaxRows(int i) {
		maxRows = i;
	}
	public int getMaxRows() {
		return maxRows;
	}

	/**
	 * Sets the number of the first row to be returned from the output of <code>select</code> queries.
	 * Rows before this are skipped from the output.
	 */
	@IbisDoc({"the number of the first row returned from the output", "1"})
	public void setStartRow(int i) {
		startRow = i;
	}
	public int getStartRow() {
		return startRow;
	}


	public boolean isScalar() {
		return scalar;
	}

	@IbisDoc({"when true, the value of the first column of the first row (or the startrow) is returned as the only result, as a simple non-xml value", "false"})
	public void setScalar(boolean b) {
		scalar = b;
	}

	public boolean isScalarExtended() {
		return scalarExtended;
	}

	public void setScalarExtended(boolean b) {
		scalarExtended = b;
	}


	@IbisDoc({"", "true"})
	public void setSynchronous(boolean synchronous) {
	   this.synchronous=synchronous;
	}
	@Override
	public boolean isSynchronous() {
	   return synchronous;
	}

	@IbisDoc({"value used in result as contents of fields that contain no value (sql-null)", "<i>empty string</>"})
	public void setNullValue(String string) {
		nullValue = string;
	}
	public String getNullValue() {
		return nullValue;
	}



	@IbisDoc({"comma separated list of columns whose values are to be returned. works only if the driver implements jdbc 3.0 getgeneratedkeys()", ""})
	public void setColumnsReturned(String string) {
		columnsReturned = string;
	}
	public String getColumnsReturned() {
		return columnsReturned;
	}
	public String[] getColumnsReturnedList() {
		return columnsReturnedList;
	}


	@IbisDoc({"query that can be used to obtain result of side-effecto of update-query, like generated value of sequence. example: select mysequence.currval from dual", ""})
	public void setResultQuery(String string) {
		resultQuery = string;
	}
	public String getResultQuery() {
		return resultQuery;
	}


	@IbisDoc({"remove trailing blanks from all values.", "true"})
	public void setTrimSpaces(boolean b) {
		trimSpaces = b;
	}
	public boolean isTrimSpaces() {
		return trimSpaces;
	}

	@IbisDoc({"controls whether blobdata is stored compressed in the database", "true"})
	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}
	public boolean isBlobsCompressed() {
		return blobsCompressed;
	}

	@IbisDoc({"controls whether the streamed blobdata will need to be base64 <code>encode</code> or <code>decode</code> or not.", ""})
	public void setBlobBase64Direction(String string) {
		blobBase64Direction = string;
	}
	
	public String getBlobBase64Direction() {
		return blobBase64Direction;
	}
	
	@IbisDoc({"controls automatically whether blobdata is stored compressed and/or serialized in the database", "false"})
	public void setBlobSmartGet(boolean b) {
		blobSmartGet = b;
	}
	public boolean isBlobSmartGet() {
		return blobSmartGet;
	}

	public String getBlobCharset() {
		return blobCharset;
	}

	@IbisDoc({"charset used to read and write blobs", "utf-8"})
	public void setBlobCharset(String string) {
		blobCharset = string;
	}

	@IbisDoc({"only for querytype 'updateblob': column that contains the blob to be updated", "1"})
	public void setBlobColumn(int i) {
		blobColumn = i;
	}
	public int getBlobColumn() {
		return blobColumn;
	}

	@IbisDoc({"for querytype 'updateblob': key of session variable that contains the data (string or inputstream) to be loaded to the blob. when empty, the input of the pipe, which then must be a string, is used. for querytype 'select': key of session variable that contains the outputstream, writer or filename to write the blob to", ""})
	public void setBlobSessionKey(String string) {
		blobSessionKey = string;
	}
	public String getBlobSessionKey() {
		return blobSessionKey;
	}

	@IbisDoc({"only for querytype 'updateclob': column that contains the clob to be updated", "1"})
	public void setClobColumn(int i) {
		clobColumn = i;
	}
	public int getClobColumn() {
		return clobColumn;
	}

	@IbisDoc({"for querytype 'updateclob': key of session variable that contains the clob (string or inputstream) to be loaded to the clob. when empty, the input of the pipe, which then must be a string, is used. for querytype 'select': key of session variable that contains the outputstream, writer or filename to write the clob to", ""})
	public void setClobSessionKey(String string) {
		clobSessionKey = string;
	}
	public String getClobSessionKey() {
		return clobSessionKey;
	}

	@IbisDoc({"when set to <code>false</code>, the inputstream is not closed after it has been used", "true"})
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}
	public boolean isCloseInputstreamOnExit() {
		return closeInputstreamOnExit;
	}

	@IbisDoc({"when set to <code>false</code>, the outputstream is not closed after blob or clob has been written to it", "true"})
	public void setCloseOutputstreamOnExit(boolean b) {
		closeOutputstreamOnExit = b;
	}
	public boolean isCloseOutputstreamOnExit() {
		return closeOutputstreamOnExit;
	}


	@IbisDoc({"charset used when reading a stream (that is e.g. going to be written to a blob or clob). when empty, the stream is copied directly to the blob, without conversion", ""})
	 public void setStreamCharset(String string) {
		streamCharset = string;
	}
	public String getStreamCharset() {
		return streamCharset;
	}


	@IbisDoc({"when <code>true</code>, every string in the message which equals <code>paramname</code> will be replaced by the setter method for the corresponding parameter (the parameters don't need to be in the correct order and unused parameters are skipped)", "false"})
	public void setUseNamedParams(boolean b) {
		useNamedParams = b;
	}

	public boolean isUseNamedParams() {
		return useNamedParams;
	}

	public boolean isIncludeFieldDefinition() {
		return includeFieldDefinition;
	}

	@IbisDoc({"when <code>true</code>, the result contains besides the returned rows also a header with information about the fetched fields", "application default (true)"})
	public void setIncludeFieldDefinition(boolean b) {
		includeFieldDefinition = b;
	}

	public String getRowIdSessionKey() {
		return rowIdSessionKey;
	}

	@IbisDoc({"if specified, the rowid of the processed row is put in the pipelinesession under the specified key (only applicable for <code>querytype=other</code>). <b>note:</b> if multiple rows are processed a sqlexception is thrown.", ""})
	public void setRowIdSessionKey(String string) {
		rowIdSessionKey = string;
	}

	public boolean isStreamResultToServlet() {
		return streamResultToServlet;
	}

	@IbisDoc({"if set, the result is streamed to the httpservletresponse object of the restservicedispatcher (instead of passed as a string)", "false"})
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}
}
