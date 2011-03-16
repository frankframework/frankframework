/*
 * $Log: DirectQuerySender.java,v $
 * Revision 1.18  2011-03-16 16:42:40  L190409
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 * Revision 1.17  2010/03/25 12:57:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * javadoc: added attribute closeInputstreamOnExit
 *
 * Revision 1.16  2009/10/07 13:35:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added attribute includeFieldDefinition
 *
 * Revision 1.15  2009/07/17 12:48:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * updated javadoc
 *
 * Revision 1.14  2009/07/17 09:56:24  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * updated javadoc
 *
 * Revision 1.13  2009/03/26 14:47:36  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added LOCKROWS_SUFFIX
 *
 * Revision 1.12  2009/02/25 10:43:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added lockRows attribute
 *
 * Revision 1.11  2006/12/13 16:27:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute blobCharset
 *
 * Revision 1.10  2006/12/12 09:57:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restore jdbc package
 *
 * Revision 1.8  2006/01/05 14:21:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.7  2005/10/19 10:45:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved prepareStatement-met-columnlist to separate method, 
 * to avoid compilation problems when non JDBC 3.0 drivers are used
 *
 * Revision 1.6  2005/10/18 07:10:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update javadoc
 *
 * Revision 1.5  2005/09/29 13:59:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * provided attributes and handling for nullValue,columnsReturned and resultQuery
 *
 * Revision 1.4  2005/09/07 15:37:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/10/19 08:11:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified JavaDoc
 *
 * Revision 1.2  2004/03/26 10:43:09  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.1  2004/03/24 13:28:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * QuerySender that interprets the input message as a query, possibly with attributes.
 * Messages are expected to contain sql-text.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.DirectQuerySender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLockRows(boolean) lockRows}</td><td>When set <code>true</code>, exclusive row-level locks are obtained on all the rows identified by the SELECT statement (by appending ' FOR UPDATE NOWAIT SKIP LOCKED' to the end of the query)</td><td>false</td></tr>
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
 *     <li>anything else for queries that return no data.</li>
 * </ul></td><td>"other"</td></tr>
 * <tr><td>{@link #setMaxRows(int) maxRows}</td><td>maximum number of rows returned</td><td>-1 (unlimited)</td></tr>
 * <tr><td>{@link #setStartRow(int) startRow}</td><td>the number of the first row returned from the output</td><td>1</td></tr>
 * <tr><td>{@link #setScalar(boolean) scalar}</td><td>when true, the value of the first column of the first row (or the StartRow) is returned as the only result, as a simple non-XML value</td><td>false</td></tr>
 * <tr><td>{@link #setNullValue(String) nullValue}</td><td>value used in result as contents of fields that contain no value (SQL-NULL)</td><td><i>empty string</></td></tr>
 * <tr><td>{@link #setResultQuery(String) resultQuery}</td><td>query that can be used to obtain result of side-effecto of update-query, like generated value of sequence. Example: SELECT mysequence.currval FROM DUAL</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setTrimSpaces(boolean) trimSpaces}</td><td>remove trailing blanks from all values.</td><td>true</td></tr>
 * <tr><td>{@link #setBlobCharset(String) blobCharset}</td><td>charset used to read and write BLOBs</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>controls whether blobdata is stored compressed in the database</td><td>true</td></tr>
 * <tr><td>{@link #setColumnsReturned(String) columnsReturned}</td><td>comma separated list of columns whose values are to be returned. Works only if the driver implements JDBC 3.0 getGeneratedKeys()</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBlobSmartGet(boolean) blobSmartGet}</td><td>controls automatically whether blobdata is stored compressed and/or serialized in the database</td><td>false</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>the number of seconds the driver will wait for a Statement object to execute. If the limit is exceeded, a TimeOutException is thrown. 0 means no timeout</td><td>0</td></tr>
 * <tr><td>{@link #setUseNamedParams(boolean) useNamedParams}</td><td>when <code>true</code>, every string in the query which equals "?{<code>paramName</code>}" will be replaced by the setter method for the corresponding parameter (the parameters don't need to be in the correct order and unused parameters are skipped)</td><td>false</td></tr>
 * <tr><td>{@link #setIncludeFieldDefinition(boolean) includeFieldDefinition}</td><td>when <code>true</code>, the result contains besides the returned rows also a header with information about the fetched fields</td><td>true</td></tr>
 * <tr><td>{@link #setCloseInputstreamOnExit(boolean) closeInputstreamOnExit}</td><td>when set to <code>false</code>, the inputstream is not closed after it has been used</td><td>true</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>all parameters present are applied to the statement to be executed</td></tr>
 * </table>
 * </p>
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class DirectQuerySender extends JdbcQuerySenderBase {

	private boolean lockRows=false;

	protected PreparedStatement getStatement(Connection con, String correlationID, String message) throws SQLException, JdbcException {
		String qry = message;
		if (lockRows) {
			qry = getDbmsSupport().prepareQueryTextForWorkQueueReading(-1, qry);
		}
		return prepareQuery(con, qry);
	}

	public void setLockRows(boolean b) {
		lockRows = b;
	}

	public boolean isLockRows() {
		return lockRows;
	}
}
