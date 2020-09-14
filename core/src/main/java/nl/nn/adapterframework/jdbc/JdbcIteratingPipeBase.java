/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import javax.sql.DataSource;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.IteratingPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;


/**
 * Base class for JDBC iterating pipes.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public abstract class JdbcIteratingPipeBase extends IteratingPipe<String> implements HasPhysicalDestination {

	protected MixedQuerySender querySender = new MixedQuerySender();

	private final String FIXEDQUERYSENDER = "nl.nn.adapterframework.jdbc.FixedQuerySender";

	protected class MixedQuerySender extends DirectQuerySender {
		
		private String query;
		
		@Override
		protected String getQuery(Message message) throws SenderException {
			if (query!=null) {
				return query;
			}
			return super.getQuery(message);
		}

		public void setQuery(String query) {
			this.query = query;
		}
	}
	
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		querySender.setName("source of "+getName());
		querySender.configure();
	}
	
	@Override
	public void start() throws PipeStartException {
		try {
			querySender.open();
		} catch (SenderException e) {
			throw new PipeStartException(e);
		}
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
		querySender.close();
	}

	protected abstract IDataIterator<String> getIterator(Connection conn, ResultSet rs) throws SenderException; 

	@SuppressWarnings("finally")
	@Override
	protected IDataIterator<String> getIterator(Message message, IPipeLineSession session, Map<String,Object> threadContext) throws SenderException {
		Connection connection = null;
		PreparedStatement statement=null;
		ResultSet rs=null;
		try {
			connection = querySender.getConnection();
			QueryExecutionContext queryExecutionContext = querySender.getQueryExecutionContext(connection, message, session);
			statement=queryExecutionContext.getStatement();
			JdbcUtil.applyParameters(statement, queryExecutionContext.getParameterList(), message, session);
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

	@Override
	public void addParameter(Parameter p) {
		querySender.addParameter(p);
	}

	public void setProxiedDataSources(Map<String,DataSource> proxiedDataSources) {
		querySender.setProxiedDataSources(proxiedDataSources);
	}

	@Override
	public String getPhysicalDestinationName() {
		return querySender.getPhysicalDestinationName();
	}

	public void setJmsRealm(String jmsRealmName) {
		querySender.setJmsRealm(jmsRealmName);
	}


	@IbisDoc({"1", "The SQL query text to be excecuted each time sendMessage() is called. When not set, the input message is taken as the query", ""})
	public void setQuery(String query) {
		querySender.setQuery(query);
	}

	@IbisDocRef({"2", FIXEDQUERYSENDER})
	public void setDatasourceName(String datasourceName) {
		querySender.setDatasourceName(datasourceName);
	}

	@IbisDocRef({"3", FIXEDQUERYSENDER})
	public void setUseNamedParams(boolean b) {
		querySender.setUseNamedParams(b);
	}

	@IbisDocRef({"4", FIXEDQUERYSENDER})
	public void setTrimSpaces(boolean b) {
		querySender.setTrimSpaces(b);
	}

	@IbisDocRef({"5", FIXEDQUERYSENDER})
	public void setSqlDialect(String string) {
		querySender.setSqlDialect(string);
	}
	
	@IbisDocRef({"6", FIXEDQUERYSENDER})
	public void setLockRows(boolean b) {
		querySender.setLockRows(b);
	}

	@IbisDocRef({"7", FIXEDQUERYSENDER})
	public void setLockWait(int i) {
		querySender.setLockWait(i);
	}

	@IbisDocRef({"8", FIXEDQUERYSENDER})
	public void setAvoidLocking(boolean avoidLocking) {
		querySender.setAvoidLocking(avoidLocking);
	}

}