/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.jdbc;

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

import org.frankframework.batch.IResultHandler;
import org.frankframework.batch.ResultWriter;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.doc.ReferTo;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.SpringUtils;


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

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		querySender = SpringUtils.createBean(applicationContext);
		querySender.setName("querySender of "+getName());
		querySender.configure();
	}

	@Override
	public void open() throws SenderException {
		super.open();
		try { //TODO remove this
			querySender.start();
		} catch (LifecycleException e) {
			throw new SenderException(e);
		}
	}

	@Override
	public void close() throws SenderException {
		try {
			super.close();
		} finally {
			try { //TODO remove this
				querySender.stop();
			} catch (LifecycleException e) {
				throw new SenderException(e);
			}
		}
	}

	protected abstract Object getLobHandle(IDbmsSupport dbmsSupport, ResultSet rs)                   throws SenderException;
	protected abstract Writer getWriter   (IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException;
	protected abstract void   updateLob   (IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException;

	@Override
	protected Writer createWriter(PipeLineSession session, String streamId) throws Exception {
		querySender.sendMessageOrThrow(new Message(streamId), session).close(); // TODO find out why this is here. It seems to me the query will be executed twice this way. Or is it to insert an empty LOB before updating it?
		Connection connection=querySender.getConnection();
		openConnections.put(streamId, connection);
		Message message = new Message(streamId);
		QueryExecutionContext queryExecutionContext = querySender.getQueryExecutionContext(connection, message);
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

	@ReferTo(FixedQuerySender.class)
	public void setDatasourceName(String datasourceName) {
		querySender.setDatasourceName(datasourceName);
	}

	@ReferTo(FixedQuerySender.class)
	public String getPhysicalDestinationName() {
		return querySender.getPhysicalDestinationName();
	}
}
