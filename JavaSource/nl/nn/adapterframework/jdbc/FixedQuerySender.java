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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import nl.nn.adapterframework.configuration.ConfigurationException;

import org.apache.commons.lang.StringUtils;

/**
 * QuerySender that assumes a fixed query, possibly with attributes.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.FixedQuerySender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text to be excecuted each time sendMessage() is called</td><td>&nbsp;</td></tr>
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
 * <tr><td>{@link #setResultQuery(String) resultQuery}</td><td>query that can be used to obtain result of side-effect of update-query, like generated value of sequence. Example: SELECT mysequence.currval FROM DUAL</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setTrimSpaces(boolean) trimSpaces}</td><td>remove trailing blanks from all values.</td><td>true</td></tr>
 * <tr><td>{@link #setBlobCharset(String) blobCharset}</td><td>charset used to read and write BLOBs</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>controls whether blobdata is stored compressed in the database</td><td>true</td></tr>
 * <tr><td>{@link #setColumnsReturned(String) columnsReturned}</td><td>comma separated list of columns whose values are to be returned. Works only if the driver implements JDBC 3.0 getGeneratedKeys()</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBlobSmartGet(boolean) blobSmartGet}</td><td>controls automatically whether blobdata is stored compressed and/or serialized in the database</td><td>false</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>the number of seconds the driver will wait for a Statement object to execute. If the limit is exceeded, a TimeOutException is thrown. 0 means no timeout</td><td>0</td></tr>
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
 * @version $Id$
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class FixedQuerySender extends JdbcQuerySenderBase {

	private String query=null;
	private boolean lockRows=false;

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getQuery())) {
			throw new ConfigurationException(getLogPrefix()+"query must be specified");
		}
	}
		
	protected PreparedStatement getStatement(Connection con, String correlationID, String message, boolean updateable) throws JdbcException, SQLException {
		String qry = getQuery();
		if (lockRows) {
			qry = getDbmsSupport().prepareQueryTextForWorkQueueReading(-1, qry);
		}
		return prepareQuery(con, qry, updateable);
	}

	/**
	 * Sets the SQL-query text to be executed each time sendMessage() is called.
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}
	public String getQuery() {
		return query;
	}

	public void setLockRows(boolean b) {
		lockRows = b;
	}

	public boolean isLockRows() {
		return lockRows;
	}
}
