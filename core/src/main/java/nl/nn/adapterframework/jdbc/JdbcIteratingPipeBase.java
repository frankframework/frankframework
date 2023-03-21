/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021, 2022 WeAreFrank!

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
import java.util.Map;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.StringIteratorPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.SpringUtils;


/**
 * Base class for JDBC iterating pipes.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public abstract class JdbcIteratingPipeBase extends StringIteratorPipe implements HasPhysicalDestination {

	private final @Getter(onMethod = @__(@Override)) String domain = "JDBC";
	protected MixedQuerySender querySender = new MixedQuerySender();

	protected class MixedQuerySender extends DirectQuerySender {

		private String query;

		@Override
		public void configure() throws ConfigurationException {
			//In case a query is specified, pass true as argument to suppress the SQL Injection warning else pass the Adapter
			if(query!=null) {
				super.configure(true);
			} else {
				super.configure(getAdapter());
			}
		}

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

		SpringUtils.autowireByName(getApplicationContext(), querySender);
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

	protected abstract IDataIterator<String> getIterator(IDbmsSupport dbmsSupport, Connection conn, ResultSet rs) throws SenderException;

	@Override
	protected IDataIterator<String> getIterator(Message message, PipeLineSession session, Map<String,Object> threadContext) throws SenderException {
		Connection connection = null;
		PreparedStatement statement=null;
		ResultSet rs=null;
		try {
			connection = querySender.getConnection();
			QueryExecutionContext queryExecutionContext = querySender.getQueryExecutionContext(connection, message, session);
			statement=queryExecutionContext.getStatement();
			JdbcUtil.applyParameters(querySender.getDbmsSupport(), statement, queryExecutionContext.getParameterList(), message, session);
			rs = statement.executeQuery();
			if (rs==null) {
				throw new SenderException("resultset is null");
			}
			if (!rs.next()) {
				JdbcUtil.fullClose(connection, rs);
				return null; // no results
			}
			return getIterator(querySender.getDbmsSupport(), connection, rs);
		} catch (Throwable t) {
			try {
				if (rs!=null) {
					JdbcUtil.fullClose(connection, rs);
				} else {
					JdbcUtil.fullClose(connection, statement);
				}
			} catch (Throwable t2) {
				t.addSuppressed(t2);
			}
			throw new SenderException(t);
		}
	}

	@Override
	public void addParameter(Parameter p) {
		querySender.addParameter(p);
	}

	@Override
	public String getPhysicalDestinationName() {
		return querySender.getPhysicalDestinationName();
	}

	@Deprecated
	@ConfigurationWarning("We discourage the use of jmsRealms for datasources. To specify a datasource other then the default, use the datasourceName attribute directly, instead of referring to a realm")
	public void setJmsRealm(String jmsRealmName) {
		querySender.setJmsRealm(jmsRealmName);
	}

	/** The SQL query text to be excecuted each time sendMessage() is called. When not set, the input message is taken as the query */
	public void setQuery(String query) {
		querySender.setQuery(query);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setDatasourceName(String datasourceName) {
		querySender.setDatasourceName(datasourceName);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setUseNamedParams(Boolean b) {
		querySender.setUseNamedParams(b);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setTrimSpaces(boolean b) {
		querySender.setTrimSpaces(b);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setSqlDialect(String string) {
		querySender.setSqlDialect(string);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setLockRows(boolean b) {
		querySender.setLockRows(b);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setLockWait(int i) {
		querySender.setLockWait(i);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setAvoidLocking(boolean avoidLocking) {
		querySender.setAvoidLocking(avoidLocking);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.JdbcQuerySenderBase */
	@Deprecated //BLOBs are binary, they should not contain character data
	public void setBlobCharset(String charset) {
		querySender.setBlobCharset(charset);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.JdbcQuerySenderBase */
	public void setBlobSmartGet(boolean isSmartBlob) {
		querySender.setBlobSmartGet(isSmartBlob);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.JdbcQuerySenderBase */
	public void setBlobsCompressed(boolean compressed) {
		querySender.setBlobsCompressed(compressed);
	}
}