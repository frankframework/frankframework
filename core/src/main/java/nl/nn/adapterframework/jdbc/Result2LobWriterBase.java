/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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

import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Setter;
import nl.nn.adapterframework.batch.IResultHandler;
import nl.nn.adapterframework.batch.ResultWriter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.SpringUtils;


/**
 * Baseclass for batch {@link IResultHandler resultHandler} that writes the transformed record to a LOB.
 *
 * @ff.parameters any parameters defined on the resultHandler will be applied to the SQL statement
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public abstract class Result2LobWriterBase extends ResultWriter implements ApplicationContextAware {
	private @Setter ApplicationContext applicationContext;

	protected Map<String,Connection> openConnections = Collections.synchronizedMap(new HashMap<String,Connection>());
	protected Map<String,ResultSet>  openResultSets  = Collections.synchronizedMap(new HashMap<String,ResultSet>());
	protected Map<String,Object>     openLobHandles  = Collections.synchronizedMap(new HashMap<String,Object>());

	protected FixedQuerySender querySender;

	protected final String FIXEDQUERYSENDER = "nl.nn.adapterframework.jdbc.FixedQuerySender";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		querySender = SpringUtils.createBean(applicationContext, FixedQuerySender.class);
		querySender.setName("querySender of "+getName());
		querySender.configure();
	}

	@Override
	public void open() throws SenderException {
		super.open();
		querySender.open();
	}

	@Override
	public void close() throws SenderException {
		try {
			super.close();
		} finally {
			querySender.close();
		}
	}

	protected abstract Object getLobHandle(IDbmsSupport dbmsSupport, ResultSet rs)                   throws SenderException;
	protected abstract Writer getWriter   (IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException;
	protected abstract void   updateLob   (IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException;

	@Override
	protected Writer createWriter(PipeLineSession session, String streamId) throws Exception {
		querySender.sendMessageOrThrow(new Message(streamId), session); // TODO find out why this is here. It seems to me the query will be executed twice this way. Or is it to insert an empty LOB before updating it?
		Connection connection=querySender.getConnection();
		openConnections.put(streamId, connection);
		Message message = new Message(streamId);
		QueryExecutionContext queryExecutionContext = querySender.getQueryExecutionContext(connection, message, session);
		PreparedStatement statement=queryExecutionContext.getStatement();
		IDbmsSupport dbmsSupport=querySender.getDbmsSupport();
		JdbcUtil.applyParameters(dbmsSupport, statement, queryExecutionContext.getParameterList(), message, session);
		ResultSet rs =statement.executeQuery();
		openResultSets.put(streamId,rs);
		Object lobHandle=getLobHandle(dbmsSupport, rs);
		openLobHandles.put(streamId, lobHandle);
		return getWriter(dbmsSupport, lobHandle, rs);
	}

	@Override
	public String finalizeResult(PipeLineSession session, String streamId, boolean error) throws Exception {
		try {
			return super.finalizeResult(session,streamId, error);
		} finally {
			Object lobHandle = openLobHandles.get(streamId);
			Connection conn = openConnections.get(streamId);
			ResultSet rs = openResultSets.get(streamId);
			if (rs!=null) {
				updateLob(querySender.getDbmsSupport(), lobHandle, rs);
				JdbcUtil.fullClose(conn, rs);
			}
		}
	}


	/** The SQL query text */
	public void setQuery(String query) {
		querySender.setQuery(query);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setDatasourceName(String datasourceName) {
		querySender.setDatasourceName(datasourceName);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public String getPhysicalDestinationName() {
		return querySender.getPhysicalDestinationName();
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setJmsRealm(String jmsRealmName) {
		querySender.setJmsRealm(jmsRealmName);
	}

}
