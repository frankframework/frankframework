/*
 * $Log: JdbcQuerySenderBase.java,v $
 * Revision 1.30.2.1  2007-10-10 14:30:47  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.31  2007/10/08 13:30:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.30  2007/07/19 15:09:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * handle charsets of BLOB and CLOB streams correctly
 *
 * Revision 1.29  2007/07/17 09:20:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix typo
 *
 * Revision 1.28  2007/07/10 07:18:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for loading streams to blobs and clobs
 *
 * Revision 1.27  2007/04/24 11:37:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added package execution feature
 *
 * Revision 1.26  2007/02/27 12:37:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected returned result for blob/clobs & do required trimming
 *
 * Revision 1.25  2006/12/13 16:25:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute blobCharset
 *
 * Revision 1.24  2006/12/12 09:57:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restore jdbc package
 *
 * Revision 1.22  2006/11/06 13:02:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute scalarExtended
 *
 * Revision 1.21  2006/02/09 10:42:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added clob-support (PL)
 *
 * Revision 1.20  2006/01/05 14:21:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.19  2005/10/24 09:17:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * separate statement for result query
 *
 * Revision 1.18  2005/10/19 10:45:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved prepareStatement-met-columnlist to separate method, 
 * to avoid compilation problems when non JDBC 3.0 drivers are used
 *
 * Revision 1.17  2005/10/19 09:34:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved getGeneratedKeys to separate method, 
 * to avoid compilation problems when non JDBC 3.0 drivers are used
 *
 * Revision 1.16  2005/10/18 07:09:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added updateBlob functionality
 * added trimSpaces feature
 *
 * Revision 1.15  2005/09/29 13:59:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * provided attributes and handling for nullValue,columnsReturned and resultQuery
 *
 * Revision 1.14  2005/09/08 16:00:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * make synchronous an attribute, default="true"
 *
 * Revision 1.13  2005/08/25 15:48:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * close all jdbc-objects in finally clause
 *
 * Revision 1.12  2005/07/28 07:33:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * close statement also for update-statements
 *
 * Revision 1.11  2005/07/19 12:36:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved applyParameters to JdbcFacade
 *
 * Revision 1.10  2005/06/28 09:05:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * explicit closing of resultset
 *
 * Revision 1.9  2005/06/02 13:48:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added 'scalar' attribute, to return a single value
 *
 * Revision 1.8  2005/04/26 15:20:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced JdbcSenderBase, with non-sql oriented basics
 *
 * Revision 1.7  2004/11/10 12:56:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected parameter setting routine
 *
 * Revision 1.6  2004/10/19 06:41:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified parameter handling
 *
 * Revision 1.5  2004/04/08 16:12:16  Dennis van Loon <dennis.van.loon@ibissource.org>
 * changed default value for maxRows to -1 (show All)
 *
 * Revision 1.4  2004/03/31 12:04:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.3  2004/03/26 10:43:07  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.2  2004/03/26 09:50:52  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 * Revision 1.1  2004/03/24 13:28:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;

/**
 * This executes the query that is obtained from the (here still abstract) method getStatement.
 * Descendent classes can override getStatement to provide meaningful statements.
 * If used with parameters, the values of the parameters will be applied to the statement. 
 * Each occurrence of a questionmark ('?') will be replaced by a parameter value. Parameters are applied
 * in order: The n-th questionmark is replaced by the value of the n-th parameter.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.JdbcQuerySenderBase</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>

 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceNameXA(String) datasourceNameXA}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>username used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>password used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConnectionsArePooled(boolean) connectionsArePooled}</td><td>when true, it is assumed that an connectionpooling mechanism is present. Before a message is sent, a new connection is obtained, that is closed after the message is sent. When transacted is true, connectionsArePooled is true, too</td><td>true</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>

 * <tr><td>{@link #setQueryType(String) queryType}</td><td>one of:
 * <ul><li>"select" for queries that return data</li>
 *     <li>"updateBlob" for queries that update a BLOB</li>
 *     <li>"updateClob" for queries that update a CLOB</li>
 *     <li>anything else for queries that return no data.</li>
 * </ul></td><td>"other"</td></tr>
 * <tr><td>{@link #setBlobColumn(int) blobColumn}</td><td>only for queryType 'updateBlob': column that contains the blob to be updated</td><td>1</td></tr>
 * <tr><td>{@link #setClobColumn(int) clobColumn}</td><td>only for queryType 'updateClob': column that contains the clob to be updated</td><td>1</td></tr>
 * <tr><td>{@link #setBlobSessionKey(String) blobSessionKey}</td><td>only for queryType 'updateBlob': key of session variable that contains the data (String or InputStream) to be loaded to the blob. When empty, the input of the pipe, which then must be a String, is used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setClobSessionKey(String) clobSessionKey}</td><td>only for queryType 'updateClob': key of session variable that contains the clob (String or InputStream) to be loaded to the clob. When empty, the input of the pipe, which then must be a String, is used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxRows(int) maxRows}</td><td>maximum number of rows returned</td><td>-1 (unlimited)</td></tr>
 * <tr><td>{@link #setStartRow(int) startRow}</td><td>the number of the first row returned from the output</td><td>1</td></tr>
 * <tr><td>{@link #setScalar(boolean) scalar}</td><td>when true, the value of the first column of the first row (or the StartRow) is returned as the only result, as a simple non-XML value</td><td>false</td></tr>
 * <tr><td>{@link #setScalarExtended(boolean) scalarExtended}</td><td>when true and <code>scalar</code> is also true, but returns no value, one of the following is returned:
 * <ul><li>"[absent]" no row is found</li>
 *     <li>"[null]" a row is found, but the value is a SQL-NULL</li>
 *     <li>"[empty]" a row is found, but the value is a empty string</li>
 * </ul></td><td>false</td></tr>
 * <tr><td>{@link #setNullValue(String) nullValue}</td><td>value used in result as contents of fields that contain no value (SQL-NULL)</td><td><i>empty string</></td></tr>
 * <tr><td>{@link #setResultQuery(String) resultQuery}</td><td>query that can be used to obtain result of side-effecto of update-query, like generated value of sequence. Example: SELECT mysequence.currval FROM DUAL</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setTrimSpaces(boolean) trimSpaces}</td><td>remove trailing blanks from all values.</td><td>true</td></tr>
 * <tr><td>{@link #setBlobCharset(String) blobCharset}</td><td>charset used to read and write BLOBs</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setStreamCharset(String) streamCharset}</td><td>charset used when reading a stream (that is e.g. going to be written to a BLOB or CLOB)</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>controls whether blobdata is stored compressed in the database</td><td>true</td></tr>
 * <tr><td>{@link #setColumnsReturned(String) columnsReturned}</td><td>comma separated list of columns whose values are to be returned. Works only if the driver implements JDBC 3.0 getGeneratedKeys()</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>all parameters present are applied to the statement to be executed</td></tr>
 * </table>

 * </p>
 * 
 * Queries that return no data (queryType 'other') return a message indicating the number of rows processed
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public abstract class JdbcQuerySenderBase extends JdbcSenderBase {
	public static final String version="$RCSfile: JdbcQuerySenderBase.java,v $ $Revision: 1.30.2.1 $ $Date: 2007-10-10 14:30:47 $";

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
	private String streamCharset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
	private boolean blobsCompressed=true;

	private String packageContent = "db2";
	
	protected String[] columnsReturnedList=null;



	public void configure() throws ConfigurationException {
		super.configure();
		
		if (StringUtils.isNotEmpty(getColumnsReturned())) {
			List tempList = new ArrayList();
			StringTokenizer st = new StringTokenizer(getColumnsReturned(),",");
			while (st.hasMoreTokens()) {
				String column = st.nextToken();
				tempList.add(column);
			}
			columnsReturnedList = new String[tempList.size()];
			for (int i=0; i<tempList.size(); i++) {
				columnsReturnedList[i]=(String)tempList.get(i);
			}
		}
	}

	
	/**
	 * Obtain a prepared statement to be executed.
	 * Method-stub to be overridden in descender-classes.
	 */
	protected abstract PreparedStatement getStatement(Connection con, String correlationID, String message) throws JdbcException, SQLException;
	
	private PreparedStatement prepareQueryWithColunmsReturned(Connection con, String query, String[] columnsReturned) throws SQLException {
		return con.prepareStatement(query,columnsReturned);
	}

	protected PreparedStatement prepareQuery(Connection con, String query) throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() +"preparing statement for query ["+query+"]");
		}
		String[] columnsReturned = getColumnsReturnedList();
		if (columnsReturned!=null) {
			return prepareQueryWithColunmsReturned(con,query,columnsReturned);
		}
		return con.prepareStatement(query);
	}


	protected ResultSet getReturnedColumns(String[] columns, PreparedStatement st) throws SQLException {
		return st.getGeneratedKeys();
	}

	protected String sendMessage(Connection connection, String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		PreparedStatement statement=null;
		try {
			statement = getStatement(connection, correlationID, message);
			if (prc != null && paramList != null) {
				applyParameters(statement, prc.getValues(paramList));
			}
			if ("select".equalsIgnoreCase(getQueryType())) {
				return executeSelectQuery(statement);
			} else {
				if ("updateBlob".equalsIgnoreCase(getQueryType())) {
					if (StringUtils.isEmpty(getBlobSessionKey())) {
						return executeUpdateBlobQuery(statement,message);
					} else {
						return executeUpdateBlobQuery(statement,prc.getSession().get(getBlobSessionKey()));
					}
				} else {
					if ("updateClob".equalsIgnoreCase(getQueryType())) {
						if (StringUtils.isEmpty(getClobSessionKey())) {
							return executeUpdateClobQuery(statement,message);
						} else {
							return executeUpdateClobQuery(statement,prc.getSession().get(getClobSessionKey()));
						}
					} else {
						if ("package".equalsIgnoreCase(getQueryType())) {
							return executePackageQuery(connection, statement, message);
						} else {
							int numRowsAffected = statement.executeUpdate();
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
						}
					}
				}
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix() + "got exception evaluating parameters", e);
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix() + "got exception sending message", e);
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "got exception sending message", e);
		} catch (JdbcException e) {
			throw new SenderException(e);
		} finally {
			try {
				if (statement!=null) {
					statement.close();
				}
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "got exception closing SQL statement",e ));
			}
		}
	}

	protected String getResult(ResultSet resultset) throws JdbcException, SQLException, IOException {
		String result=null;
		if (isScalar()) {
			if (resultset.next()) {
				//result = resultset.getString(1);
				ResultSetMetaData rsmeta = resultset.getMetaData();
				result = getValue(resultset, 1, rsmeta.getColumnType(1));
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
			result = db2xml.getXML(resultset, getMaxRows());
		}
		return result;
	}
	
	private String getValue(final ResultSet rs, int colNum, int type) throws JdbcException, IOException, SQLException
	 {
		 switch(type)
		 {
			 // return "undefined" for types that cannot be rendered to strings easily
			 case Types.BLOB :
					 return JdbcUtil.getBlobAsString(rs,colNum,getBlobCharset(),false,isBlobsCompressed());
			 case Types.CLOB :
					 return JdbcUtil.getClobAsString(rs,colNum,false);
			 case Types.ARRAY :
			 case Types.DISTINCT :
			 case Types.LONGVARBINARY :
			 case Types.VARBINARY :
			 case Types.BINARY :
			 case Types.REF :
			 case Types.STRUCT :
				 return "undefined";
			 default : {
				 String value = rs.getString(colNum);
				 if(value == null) {
					return getNullValue();
				 } else {
					if (isTrimSpaces()) {
						value.trim();
					}
				 }
				 return value;
			 }
		 }
	 }

	protected String executeUpdateBlobQuery(PreparedStatement statement, Object message) throws SenderException{
		ResultSet rs=null;
		try {
			rs = statement.executeQuery();
			XmlBuilder result=new XmlBuilder("result");
			JdbcUtil.warningsToXml(statement.getWarnings(),result);
			rs.next();
			if (message instanceof InputStream) {
				InputStream inStream = (InputStream)message;
				Writer writer = JdbcUtil.getBlobWriter(rs, blobColumn, getBlobCharset(), isBlobsCompressed());
				Reader reader = new InputStreamReader(inStream,getStreamCharset());
				Misc.readerToWriter(reader,writer);
				writer.close();
			} else {
				JdbcUtil.putStringAsBlob(rs, blobColumn, (String)message, getBlobCharset(), isBlobsCompressed());
			}
			JdbcUtil.warningsToXml(rs.getWarnings(),result);
			return result.toXML();
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",sqle );
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a updating BLOB",e );
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a updating BLOB",e );
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
			rs = statement.executeQuery();
			XmlBuilder result=new XmlBuilder("result");
			JdbcUtil.warningsToXml(statement.getWarnings(),result);
			rs.next();
			if (message instanceof InputStream) {
				InputStream inStream = (InputStream)message;
				Writer writer = JdbcUtil.getClobWriter(rs, clobColumn);
				Reader reader = new InputStreamReader(inStream,getStreamCharset());
				Misc.readerToWriter(reader,writer);
				writer.close();
			} else {
				JdbcUtil.putStringAsClob(rs, clobColumn, (String)message);
			}
			JdbcUtil.warningsToXml(rs.getWarnings(),result);
			return result.toXML();
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",sqle );
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a updating CLOB",e );
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a updating CLOB",e );
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
	
	protected String executeSelectQuery(PreparedStatement statement) throws SenderException{
		ResultSet resultset=null;
		try {
			if (getMaxRows()>0) {
				statement.setMaxRows(getMaxRows()+ ( getStartRow()>1 ? getStartRow()-1 : 0));
			}

			resultset = statement.executeQuery();

			if (getStartRow()>1) {
				resultset.absolute(getStartRow()-1);
				log.debug(getLogPrefix() + "Index set at position: " +  resultset.getRow() );
			}				
			return getResult(resultset);
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",sqle );
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",e );
		} catch (IOException e) {
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
	
	protected String executePackageQuery(Connection connection, PreparedStatement statement, String message) throws SenderException, JdbcException, IOException {
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
				Object object = paramArray[i];
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
				pstmt.registerOutParameter(var, Types.VARCHAR);
			}
			if ("xml".equalsIgnoreCase(getPackageContent())) {
				pstmt.executeUpdate();
				String pUitvoer = pstmt.getString(var);
				return pUitvoer;
			} else {
				int numRowsAffected = pstmt.executeUpdate();
				if (StringUtils.isNotEmpty(getResultQuery())) {
					Statement resStmt = null;
					try {
						resStmt = connection.createStatement();
						log.debug(
							"obtaining result from ["
								+ getResultQuery()
								+ "]");
						ResultSet rs =
							resStmt.executeQuery(getResultQuery());
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
			}
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

	protected String fillParamArray(Object[] paramArray, String message) throws SenderException {		
		int lengthMessage = message.length();
		int startHaakje = message.indexOf('(');
		int eindHaakje	= message.indexOf(')');
		int beginOutput = message.indexOf('?');
		if (startHaakje < 1) 
			return message;
		if (beginOutput < 0)
			beginOutput = eindHaakje;
		String packageCall = message.substring(startHaakje, eindHaakje + 1);
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
		try {		
			if (packageInput.lastIndexOf(',') > 0) {
				while ((packageInput.charAt(packageInput.length() - ix) != ',')	& (ix < packageInput.length())) {
					ix++;
				}
				int eindInputs = beginOutput - ix;
				String packageInput2 = message.substring(startHaakje + 1, eindInputs);
				packageInput = null;
				packageInput = packageInput2;
				StringTokenizer st2 = new StringTokenizer(packageInput, ",");		
				if (idx != 1) {
					while (st2.hasMoreTokens()) {
						String element = st2.nextToken().trim();
						if (element.startsWith("'")) {
							int x = element.indexOf('\'');
							int y = element.lastIndexOf('\'');
							paramArray[idx] = (String) element.substring(x + 1, y);
						} else {
							if (element.indexOf('-') >= 0){
								if (element.length() > 10) {
									String pattern = "yyyy-MM-dd HH:mm:ss";
									SimpleDateFormat sdf = new SimpleDateFormat(pattern);
									java.util.Date nDate = (java.util.Date)sdf.parseObject(element.toString());
									java.sql.Timestamp sqlTimestamp = new java.sql.Timestamp(nDate.getTime());
									paramArray[idx] = (Timestamp) sqlTimestamp;
									 
								} else {
									String pattern = "yyyy-MM-dd";
									SimpleDateFormat sdf = new SimpleDateFormat(pattern);
									java.util.Date nDate;
									nDate = sdf.parse(element.toString());
									java.sql.Date sDate = new java.sql.Date(nDate.getTime());
									paramArray[idx] = (java.sql.Date) sDate;								
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
		} catch (ParseException parse) {
			throw new SenderException(
				getLogPrefix() + "got exception parsing a date string !!",
				parse);
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
	public void setStartRow(int i) {
		startRow = i;
	}
	public int getStartRow() {
		return startRow;
	}


	public boolean isScalar() {
		return scalar;
	}

	public void setScalar(boolean b) {
		scalar = b;
	}

	public boolean isScalarExtended() {
		return scalarExtended;
	}

	public void setScalarExtended(boolean b) {
		scalarExtended = b;
	}


	public void setSynchronous(boolean synchronous) {
	   this.synchronous=synchronous;
	}
	public boolean isSynchronous() {
	   return synchronous;
	}

	public void setNullValue(String string) {
		nullValue = string;
	}
	public String getNullValue() {
		return nullValue;
	}



	public void setColumnsReturned(String string) {
		columnsReturned = string;
	}
	public String getColumnsReturned() {
		return columnsReturned;
	}
	public String[] getColumnsReturnedList() {
		return columnsReturnedList;
	}


	public void setResultQuery(String string) {
		resultQuery = string;
	}
	public String getResultQuery() {
		return resultQuery;
	}


	public void setTrimSpaces(boolean b) {
		trimSpaces = b;
	}
	public boolean isTrimSpaces() {
		return trimSpaces;
	}


	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}
	public boolean isBlobsCompressed() {
		return blobsCompressed;
	}

	public String getBlobCharset() {
		return blobCharset;
	}

	public void setBlobCharset(String string) {
		blobCharset = string;
	}

	public void setBlobColumn(int i) {
		blobColumn = i;
	}
	public int getBlobColumn() {
		return blobColumn;
	}

	public void setBlobSessionKey(String string) {
		blobSessionKey = string;
	}
	public String getBlobSessionKey() {
		return blobSessionKey;
	}

	public void setClobColumn(int i) {
		clobColumn = i;
	}
	public int getClobColumn() {
		return clobColumn;
	}

	public void setClobSessionKey(String string) {
		clobSessionKey = string;
	}
	public String getClobSessionKey() {
		return clobSessionKey;
	}


	public void setStreamCharset(String string) {
		streamCharset = string;
	}
	public String getStreamCharset() {
		return streamCharset;
	}

}
