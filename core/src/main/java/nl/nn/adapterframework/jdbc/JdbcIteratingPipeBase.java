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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.IteratingPipe;
import nl.nn.adapterframework.util.JdbcUtil;

import org.apache.commons.lang.StringUtils;


/**
 * Base class for JDBC iterating pipes.
 *
 * 
 * <p><b>Configuration </b><i>(where deviating from IteratingPipe)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.jdbc.JdbcIteratingPipeBase</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text to be excecuted each time sendMessage() is called. When not set, the input message is taken as the query</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLockRows(boolean) lockRows}</td><td>When set <code>true</code>, exclusive row-level locks are obtained on all the rows identified by the SELECT statement (by appending ' FOR UPDATE NOWAIT SKIP LOCKED' to the end of the query)</td><td>false</td></tr>
 * <tr><td>{@link #setLockWait(int) lockWait}</td><td>when set and >=0, ' FOR UPDATE WAIT #' is used instead of ' FOR UPDATE NOWAIT SKIP LOCKED'</td><td>-1</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public abstract class JdbcIteratingPipeBase extends IteratingPipe {

	private String query=null;
	private boolean lockRows=false;
	private int lockWait=-1;
	
	protected JdbcQuerySenderBase querySender = new JdbcQuerySenderBase() {

		protected PreparedStatement getStatement(Connection con, String correlationID, String message, boolean updateable) throws JdbcException, SQLException {
			String qry;
			if (StringUtils.isNotEmpty(getQuery())) {
				qry = getQuery();
			} else {
				qry = message;
			}
			if (lockRows) {
				qry = getDbmsSupport().prepareQueryTextForWorkQueueReading(-1, qry, lockWait);
			}
			return prepareQuery(con, qry, updateable);
		}
	};

	public void configure() throws ConfigurationException {
		super.configure();
		querySender.setName("source of "+getName());
		querySender.configure();
	}
	
	public void start() throws PipeStartException {
		try {
			querySender.open();
		} catch (SenderException e) {
			throw new PipeStartException(e);
		}
		super.start();
	}

	public void stop() {
		super.stop();
		querySender.close();
	}

	protected void iterateInput(Object input, IPipeLineSession session, String correlationID, Map threadContext, ItemCallback callback) throws SenderException {
		if (log.isDebugEnabled()) {log.debug(getLogPrefix(session)+"result set is empty, nothing to iterate over");}
	}


	protected abstract IDataIterator getIterator(Connection conn, ResultSet rs) throws SenderException; 

	protected IDataIterator getIterator(Object input, IPipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		Connection connection = null;
		PreparedStatement statement=null;
		ResultSet rs=null;
		try {
			connection = querySender.getConnection();
			String msg = (String)input;
			statement = querySender.getStatement(connection, correlationID, msg, false);
			ParameterResolutionContext prc = new ParameterResolutionContext(msg,session);
			if (querySender.paramList != null) {
				querySender.applyParameters(statement, prc.getValues(querySender.paramList));
			}
			rs = statement.executeQuery();
			if (rs==null) {
				throw new SenderException("resultset is null");
			}
			if (!rs.next()) {
				JdbcUtil.fullClose(connection, rs);
				return null; // no results
			}
			return getIterator(connection, rs);
		} catch (Throwable t) {
			try {
				if (rs!=null) {
					JdbcUtil.fullClose(connection, rs);
				} else {
					if (statement!=null) {
						JdbcUtil.fullClose(connection, statement);
					} else {
						if (connection!=null) {
							try {
								connection.close();
							} catch (SQLException e1) {
								log.debug(getLogPrefix(session) + "caught exception closing sender after exception",e1);
							}
						}
					}
				}
			} finally {
				throw new SenderException(getLogPrefix(session),t);
			}
		}
	}

	public void addParameter(Parameter p) {
		querySender.addParameter(p);
	}


	public void setQuery(String query) {
		this.query=query;
	}
	public String getQuery() {
		return query;
	}

	public void setProxiedDataSources(Map proxiedDataSources) {
		querySender.setProxiedDataSources(proxiedDataSources);
	}

	public void setDatasourceName(String datasourceName) {
		querySender.setDatasourceName(datasourceName);
	}
	public String getDatasourceName() {
		return querySender.getDatasourceName();
	}

	public String getPhysicalDestinationName() {
		return querySender.getPhysicalDestinationName();
	}

	public void setJmsRealm(String jmsRealmName) {
		querySender.setJmsRealm(jmsRealmName);
	}
	
	public void setLockRows(boolean b) {
		lockRows = b;
	}

	public boolean isLockRows() {
		return lockRows;
	}

	public void setLockWait(int i) {
		lockWait = i;
	}

	public int getLockWait() {
		return lockWait;
	}
}