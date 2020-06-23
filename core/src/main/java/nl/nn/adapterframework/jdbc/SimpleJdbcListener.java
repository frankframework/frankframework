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
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;

/**
 * Database Listener that operates on a table.
 *
 * 
 * @author  Peter Leeuwenburgh
 */

public class SimpleJdbcListener extends JdbcFacade implements IPullingListener {
	protected final static String KEYWORD_SELECT_COUNT = "select count(";

	private String selectQuery;
	private boolean trace = false;

	protected Connection connection = null;

	@Override
	public void configure() throws ConfigurationException {
		try {
			if (getDatasource() == null) {
				throw new ConfigurationException(getLogPrefix()
						+ "has no datasource");
			}
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
		if (StringUtils.isEmpty(selectQuery)
				|| !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT_COUNT)) {
			throw new ConfigurationException(getLogPrefix() + "query ["
					+ selectQuery + "] must start with keyword ["
					+ KEYWORD_SELECT_COUNT + "]");
		}
	}

	@Override
	public void open() throws ListenerException {
		if (!isConnectionsArePooled()) {
			try {
				connection = getConnection();
			} catch (JdbcException e) {
				throw new ListenerException(e);
			}
		}
	}

	@Override
	public void close() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			log.warn(getLogPrefix() + "caught exception stopping listener", e);
		} finally {
			connection = null;
			super.close();
		}
	}

	@Override
	public Map<String,Object> openThread() throws ListenerException {
		return new LinkedHashMap<String,Object>();
	}

	@Override
	public void closeThread(Map threadContext) throws ListenerException {
	}

	@Override
	public Object getRawMessage(Map threadContext) throws ListenerException {
		if (isConnectionsArePooled()) {
			Connection c = null;
			try {
				c = getConnection();
				return getRawMessage(c, threadContext);
			} catch (JdbcException e) {
				throw new ListenerException(e);
			} finally {
				if (c != null) {
					try {
						c.close();
					} catch (SQLException e) {
						log.warn(new ListenerException(getLogPrefix() + "caught exception closing listener after retrieving message", e));
					}
				}
			}

		}
		synchronized (connection) {
			return getRawMessage(connection, threadContext);
		}
	}

	protected Object getRawMessage(Connection conn, Map<String,Object> threadContext) throws ListenerException {
		String query = getSelectQuery();
		try {
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.setFetchSize(1);
				ResultSet rs = null;
				try {
					if (trace && log.isDebugEnabled())
						log.debug("executing query for [" + query + "]");
					rs = stmt.executeQuery(query);
					if (!rs.next()) {
						return null;
					}
					int count = rs.getInt(1);
					if (count == 0) {
						return null;
					}
					return "<count>" + count + "</count>";
				} finally {
					if (rs != null) {
						rs.close();
					}
				}

			} finally {
				if (stmt != null) {
					stmt.close();
				}
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix() + "caught exception retrieving message using query [" + query + "]", e);
		}

	}

	@Override
	public String getIdFromRawMessage(Object rawMessage, Map context) throws ListenerException {
		return null;
	}

	@Override
	public Message extractMessage(Object rawMessage, Map context) throws ListenerException {
		return Message.asMessage(rawMessage);
	}

	protected ResultSet executeQuery(Connection conn, String query) throws ListenerException {
		if (StringUtils.isEmpty(query)) {
			throw new ListenerException(getLogPrefix() + "cannot execute empty query");
		}
		if (trace && log.isDebugEnabled())
			log.debug("executing query [" + query + "]");
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			return stmt.executeQuery(query);
		} catch (SQLException e) {
			if (stmt != null) {
				try {
					// log.debug("closing statement for ["+query+"]");
					stmt.close();
				} catch (Throwable t) {
					log.warn(getLogPrefix() + "exception closing statement [" + query + "]", t);
				}
			}
			throw new ListenerException(getLogPrefix() + "exception executing statement [" + query + "]", e);
		}
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map context) throws ListenerException {
		// TODO Auto-generated method stub

	}

	protected void execute(Connection conn, String query) throws ListenerException {
		execute(conn, query, null);
	}

	protected void execute(Connection conn, String query, String parameter) throws ListenerException {
		if (StringUtils.isNotEmpty(query)) {
			if (trace && log.isDebugEnabled())
				log.debug("executing statement [" + query + "]");
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(query);
				stmt.clearParameters();
				if (StringUtils.isNotEmpty(parameter)) {
					log.debug("setting parameter 1 to [" + parameter + "]");
					stmt.setString(1, parameter);
				}
				stmt.execute();

			} catch (SQLException e) {
				throw new ListenerException(getLogPrefix() + "exception executing statement [" + query + "]", e);
			} finally {
				if (stmt != null) {
					try {
						// log.debug("closing statement for ["+query+"]");
						stmt.close();
					} catch (SQLException e) {
						log.warn(getLogPrefix() + "exception closing statement [" + query + "]", e);
					}
				}
			}
		}
	}

	@IbisDoc({"count query that returns the number of available records. when there are available records the pipeline is activated", ""})
	public void setSelectQuery(String string) {
		selectQuery = string;
	}

	public String getSelectQuery() {
		return selectQuery;
	}

	public boolean isTrace() {
		return trace;
	}

	public void setTrace(boolean trace) {
		this.trace = trace;
	}
}