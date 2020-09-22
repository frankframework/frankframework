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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import nl.nn.adapterframework.batch.StreamTransformerPipe;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;


/**
 * abstract base class for JDBC batch transforming pipes.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public abstract class BatchTransformerPipeBase extends StreamTransformerPipe {
	
	protected FixedQuerySender querySender;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		IbisContext ibisContext = getAdapter().getConfiguration().getIbisManager().getIbisContext();
		querySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
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

	public class ResultSetReader extends BufferedReader {
		Connection conn;
		ResultSet rs;

		ResultSetReader(Connection conn, ResultSet rs, Reader reader) {
			super(reader);
			this.conn=conn;
			this.rs=rs;
		}

		@Override
		public void close() throws IOException {
			try {
				super.close();
			} finally {
				JdbcUtil.fullClose(conn, rs);
			}
		}
		
	}

	protected abstract Reader getReader(ResultSet rs, String charset, String streamId, IPipeLineSession session) throws SenderException;

	@Override
	protected BufferedReader getReader(String streamId, Message message, IPipeLineSession session) throws PipeRunException {
		Connection connection = null;
		try {
			connection = querySender.getConnection();
			QueryExecutionContext queryExecutionContext = querySender.getQueryExecutionContext(connection, message, session);
			PreparedStatement statement=queryExecutionContext.getStatement();
			JdbcUtil.applyParameters(querySender.getDbmsSupport(), statement, queryExecutionContext.getParameterList(), message, session);
			ResultSet rs = statement.executeQuery();
			if (rs==null || !rs.next()) {
				throw new SenderException("query has empty resultset");
			}
			return new ResultSetReader(connection, rs, getReader(rs, getCharset(), streamId, session));
		} catch (Exception e) {
			throw new PipeRunException(this,"cannot open reader",e);
		}
	}

	@Override
	public void addParameter(Parameter p) {
		querySender.addParameter(p);
	}


	public void setQuery(String query) {
		querySender.setQuery(query);
	}
	public String getQuery() {
		return querySender.getQuery();
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
	

}
