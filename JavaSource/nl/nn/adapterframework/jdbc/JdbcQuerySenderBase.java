/*
 * $Log: JdbcQuerySenderBase.java,v $
 * Revision 1.56  2011-09-15 11:55:03  europe\l562891
 * blobBase64Direction added for streamBlob
 *
 *
 * Revision 1.55  2011/08/09 10:07:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.54  2011/08/09 10:02:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved timeout to JdbcSenderBase
 * added blob stream facility
 *
 * Revision 1.53  2011/04/27 10:01:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * used timeout attribute in getting connection too
 *
 * Revision 1.52  2011/04/13 08:38:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Indicate updatability of resultset explicitly using method-parameter
 * Blob and Clob support using DbmsSupport
 *
 * Revision 1.51  2011/03/04 15:02:42  Sanne Hoekstra <sanne.hoekstra@ibissource.org>
 * Corrected timeout exception message (from ms to sec)
 *
 * Revision 1.50  2010/03/25 12:57:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute closeInputstreamOnExit
 *
 * Revision 1.49  2010/01/28 09:47:04  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * separate method for executing "other" queries
 *
 * Revision 1.48  2009/10/22 13:42:55  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added attribute rowIdSessionKey
 *
 * Revision 1.47  2009/10/09 13:26:20  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added default includeFieldDefinition (true) for querySenders
 *
 * Revision 1.46  2009/10/07 13:35:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added attribute includeFieldDefinition
 *
 * Revision 1.45  2009/09/07 13:16:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * replaced a '&' by '&&'
 *
 * Revision 1.44  2009/08/14 13:21:20  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed bug in useNamedParams
 *
 * Revision 1.43  2009/08/14 07:19:02  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed bug in useNamedParams (not thread safe)
 *
 * Revision 1.42  2009/08/12 07:38:28  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added warning for useNamedParams
 *
 * Revision 1.41  2009/08/12 07:17:19  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added warning for useNamedParams
 *
 * Revision 1.40  2009/08/04 10:12:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected description of useNamedParams
 *
 * Revision 1.39  2009/07/17 12:39:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added attribute useNamedParams
 *
 * Revision 1.38  2009/04/01 08:23:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added timeout attribute
 *
 * Revision 1.37  2009/03/03 14:38:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support to put byte array as blob
 *
 * Revision 1.36  2008/10/20 12:52:23  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added blobSmartGet attribute
 *
 * Revision 1.35  2008/06/24 07:57:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allocate larger buffer for package result
 *
 * Revision 1.34  2008/06/19 15:13:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for binary BLOBs
 * select binary BLOB as base64
 *
 * Revision 1.33  2008/05/15 14:35:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * catch more exceptions
 *
 * Revision 1.32  2008/03/27 10:54:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes to javadoc
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

import nl.nn.adapterframework.configuration.ConfigurationException;
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
 *     <li>"package" to execute Oracle PL/SQL package</li>
 *     <li>anything else for queries that return no data.</li>
 * </ul></td><td>"other"</td></tr>
 * <tr><td>{@link #setBlobColumn(int) blobColumn}</td><td>only for queryType 'updateBlob': column that contains the blob to be updated</td><td>1</td></tr>
 * <tr><td>{@link #setClobColumn(int) clobColumn}</td><td>only for queryType 'updateClob': column that contains the clob to be updated</td><td>1</td></tr>
 * <tr><td>{@link #setBlobSessionKey(String) blobSessionKey}</td><td>for queryType 'updateBlob': key of session variable that contains the data (String or InputStream) to be loaded to the blob. When empty, the input of the pipe, which then must be a String, is used. For queryType 'select': key of session variable that contains the OutputStream, Writer or filename to write the Blob to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setClobSessionKey(String) clobSessionKey}</td><td>for queryType 'updateClob': key of session variable that contains the clob (String or InputStream) to be loaded to the clob. When empty, the input of the pipe, which then must be a String, is used. For queryType 'select': key of session variable that contains the OutputStream, Writer or filename to write the Clob to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxRows(int) maxRows}</td><td>maximum number of rows returned</td><td>-1 (unlimited)</td></tr>
 * <tr><td>{@link #setStartRow(int) startRow}</td><td>the number of the first row returned from the output</td><td>1</td></tr>
 * <tr><td>{@link #setScalar(boolean) scalar}</td><td>when true, the value of the first column of the first row (or the StartRow) is returned as the only result, as a simple non-XML value</td><td>false</td></tr>
 * <tr><td>{@link #setScalarExtended(boolean) scalarExtended}</td><td>when <code>true</code> and <code>scalar</code> is also <code>true</code>, but returns no value, one of the following is returned:
 * <ul><li>"[absent]" no row is found</li>
 *     <li>"[null]" a row is found, but the value is a SQL-NULL</li>
 *     <li>"[empty]" a row is found, but the value is a empty string</li>
 * </ul></td><td>false</td></tr>
 * <tr><td>{@link #setNullValue(String) nullValue}</td><td>value used in result as contents of fields that contain no value (SQL-NULL)</td><td><i>empty string</></td></tr>
 * <tr><td>{@link #setResultQuery(String) resultQuery}</td><td>query that can be used to obtain result of side-effecto of update-query, like generated value of sequence. Example: SELECT mysequence.currval FROM DUAL</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setTrimSpaces(boolean) trimSpaces}</td><td>remove trailing blanks from all values.</td><td>true</td></tr>
 * <tr><td>{@link #setBlobCharset(String) blobCharset}</td><td>charset used to read and write BLOBs</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setCloseInputstreamOnExit(boolean) closeInputstreamOnExit}</td><td>when set to <code>false</code>, the inputstream is not closed after it has been used</td><td>true</td></tr>
 * <tr><td>{@link #setCloseOutputstreamOnExit(boolean) closeOutputstreamOnExit}</td><td>when set to <code>false</code>, the outputstream is not closed after Blob or Clob has been written to it</td><td>true</td></tr>
 * <tr><td>{@link #setStreamCharset(String) streamCharset}</td><td>charset used when reading a stream (that is e.g. going to be written to a BLOB or CLOB). When empty, the stream is copied directly to the BLOB, without conversion</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>controls whether blobdata is stored compressed in the database</td><td>true</td></tr>
 * <tr><td>{@link #setBlobBase64Direction(String) blobBase64Direction}</td><td>controls whether the streamed blobdata will need to be base64 <code>encode</code> or <code>decode</code> or not.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setColumnsReturned(String) columnsReturned}</td><td>comma separated list of columns whose values are to be returned. Works only if the driver implements JDBC 3.0 getGeneratedKeys()</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBlobSmartGet(boolean) blobSmartGet}</td><td>controls automatically whether blobdata is stored compressed and/or serialized in the database</td><td>false</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>the number of seconds the driver will wait for a Statement object to execute. If the limit is exceeded, a TimeOutException is thrown. 0 means no timeout</td><td>0</td></tr>
 * <tr><td>{@link #setUseNamedParams(boolean) useNamedParams}</td><td>when <code>true</code>, every string in the message which equals "?{<code>paramName</code>}" will be replaced by the setter method for the corresponding parameter (the parameters don't need to be in the correct order and unused parameters are skipped)</td><td>false</td></tr>
 * <tr><td>{@link #setIncludeFieldDefinition(boolean) includeFieldDefinition}</td><td>when <code>true</code>, the result contains besides the returned rows also a header with information about the fetched fields</td><td>application default (true)</td></tr>
 * <tr><td>{@link #setRowIdSessionKey(boolean) rowIdSessionKey}</td><td>If specified, the ROWID of the processed row is put in the PipeLineSession under the specified key (only applicable for <code>queryType=other</code>). <b>Note:</b> If multiple rows are processed a SQLException is thrown.</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
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
 * @version Id
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

	public void configure() throws ConfigurationException {
		super.configure();
		
		String dir=getBlobBase64Direction();
			if (dir != null && !dir.equalsIgnoreCase("encode") && !dir.equalsIgnoreCase("decode")) {
				throw new ConfigurationException(getLogPrefix()+"illegal value for direction ["+dir+"], must be 'encode' or 'decode' or empty");
			}
		
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
			statement = getStatement(connection, correlationID, message, updateBlob||updateClob);
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
				return executeSelectQuery(statement,blobSessionVar,clobSessionVar);
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

	protected String getResult(ResultSet resultset) throws JdbcException, SQLException, IOException {
		return getResult(resultset,null,null);
	}

	protected String getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar) throws JdbcException, SQLException, IOException {
		String result=null;
		if (isScalar()) {
			if (resultset.next()) {
				//result = resultset.getString(1);
				ResultSetMetaData rsmeta = resultset.getMetaData();
				if (blobSessionVar!=null && JdbcUtil.isBlobType(resultset, 1, rsmeta)) {
					JdbcUtil.streamBlob(resultset, 1, getBlobCharset(), isBlobsCompressed(), getBlobBase64Direction(), blobSessionVar, isCloseOutputstreamOnExit());
					return "";
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
	
	protected String executeSelectQuery(PreparedStatement statement, Object blobSessionVar, Object clobSessionVar) throws SenderException{
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
			return getResult(resultset,blobSessionVar,clobSessionVar);
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
				pstmt.executeUpdate();
				String pUitvoer = pstmt.getString(var);
				return pUitvoer;
			} 
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
				numRowsAffected = cstmt.executeUpdate();
				String rowId = cstmt.getString(ri);
				if (prc!=null) prc.getSession().put(getRowIdSessionKey(), rowId);
			} else {
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
				packageInput = message.substring(startHaakje + 1, eindInputs);
				StringTokenizer st2 = new StringTokenizer(packageInput, ",");		
				if (idx != 1) {
					while (st2.hasMoreTokens()) {
						element = st2.nextToken().trim();
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

	public void setBlobBase64Direction(String string) {
		blobBase64Direction = string;
	}
	
	public String getBlobBase64Direction() {
		return blobBase64Direction;
	}
	
	public void setBlobSmartGet(boolean b) {
		blobSmartGet = b;
	}
	public boolean isBlobSmartGet() {
		return blobSmartGet;
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

	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}
	public boolean isCloseInputstreamOnExit() {
		return closeInputstreamOnExit;
	}

	public void setCloseOutputstreamOnExit(boolean b) {
		closeOutputstreamOnExit = b;
	}
	public boolean isCloseOutputstreamOnExit() {
		return closeOutputstreamOnExit;
	}


	public void setStreamCharset(String string) {
		streamCharset = string;
	}
	public String getStreamCharset() {
		return streamCharset;
	}


	public void setUseNamedParams(boolean b) {
		useNamedParams = b;
	}

	public boolean isUseNamedParams() {
		return useNamedParams;
	}

	public boolean isIncludeFieldDefinition() {
		return includeFieldDefinition;
	}

	public void setIncludeFieldDefinition(boolean b) {
		includeFieldDefinition = b;
	}

	public String getRowIdSessionKey() {
		return rowIdSessionKey;
	}

	public void setRowIdSessionKey(String string) {
		rowIdSessionKey = string;
	}
}
