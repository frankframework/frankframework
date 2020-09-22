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

import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.batch.ResultWriter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;


/**
 * Baseclass for batch {@link nl.nn.adapterframework.batch.IResultHandler resultHandler} that writes the transformed record to a LOB.
 * 
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the resultHandler will be applied to the SQL statement</td></tr>
 * </table>
 * <p/>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public abstract class Result2LobWriterBase extends ResultWriter {
	
	protected Map<String,Connection> openConnections = Collections.synchronizedMap(new HashMap<String,Connection>());
	protected Map<String,ResultSet>  openResultSets  = Collections.synchronizedMap(new HashMap<String,ResultSet>());
	protected Map<String,Object>     openLobHandles  = Collections.synchronizedMap(new HashMap<String,Object>());

	protected FixedQuerySender querySender;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		IbisContext ibisContext = getPipe().getAdapter().getConfiguration().getIbisManager().getIbisContext();
		querySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
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
	protected Writer createWriter(IPipeLineSession session, String streamId) throws Exception {
		querySender.sendMessage(new Message(streamId), session); // TODO find out why this is here. It seems to me the query will be executed twice this way. Or is it to insert an empty LOB before updating it? 
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
	public Object finalizeResult(IPipeLineSession session, String streamId, boolean error) throws Exception {
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

	
	@IbisDoc({"the sql query text", ""})
	public void setQuery(String query) {
		querySender.setQuery(query);
	}

	@IbisDoc({"can be configured from jmsrealm, too", ""})
	public void setDatasourceName(String datasourceName) {
		querySender.setDatasourceName(datasourceName);
	}

	public String getPhysicalDestinationName() {
		return querySender.getPhysicalDestinationName(); 
	}

	public void setJmsRealm(String jmsRealmName) {
		querySender.setJmsRealm(jmsRealmName);
	}

}
