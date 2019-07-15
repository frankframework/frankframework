/*
   Copyright 2016, 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol.pipes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;

/**
 * Execute actions for Show configurations.
 * 
 * @author Peter Leeuwenburgh
 */

public class ShowConfigExe extends TimeoutGuardPipe {
	private IbisContext ibisContext;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ibisContext = ((Adapter) getAdapter()).getConfiguration()
				.getIbisManager().getIbisContext();
	}

	public String doPipeWithTimeoutGuarded(Object input,
			IPipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return doGet(session);
		} else {
			throw new PipeRunException(this,
					getLogPrefix(session) + "Illegal value for method ["
							+ method + "], must be 'GET'");
		}
	}

	private String doGet(IPipeLineSession session) throws PipeRunException {
		String parm_name = (String) session.get("name");
		if (StringUtils.isEmpty(parm_name)) {
			throw new PipeRunException(this,
					getLogPrefix(session) + "parameter [name] must be set");
		}

		String parm_jmsRealm = (String) session.get("jmsRealm");
		if (StringUtils.isEmpty(parm_jmsRealm)) {
			throw new PipeRunException(this,
					getLogPrefix(session) + "parameter [jmsRealm] must be set");
		}

		String parm_action = (String) session.get("action");
		if (StringUtils.isEmpty(parm_jmsRealm)) {
			throw new PipeRunException(this,
					getLogPrefix(session) + "parameter [action] must be set");
		} else if ("activate".equals(parm_action)) {
			activate(ibisContext, parm_name, parm_jmsRealm, session);
		} else if ("deactivate".equals(parm_action)) {
			deactivate(ibisContext, parm_name, parm_jmsRealm, session);
		} else {
			throw new PipeRunException(this,
					getLogPrefix(session) + "unknown value [" + parm_action
							+ "] for parameter [action]");
		}
		return "ok";
	}

	private boolean deactivate(IbisContext ibisContext, String name,
			String jmsRealm, IPipeLineSession session) throws PipeRunException {
		Connection conn = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext
				.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(jmsRealm);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");

		try {
			qs.configure();
			qs.open();
			conn = qs.getConnection();
			String query = ("UPDATE IBISCONFIG SET ACTIVECONFIG = '"
					+ (qs.getDbmsSupport().getBooleanValue(false))
					+ "' WHERE NAME=?");
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setString(1, name);
			return (stmt.executeUpdate() > 0) ? true : false;
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Error occured on deactivating config", e);
		} finally {
			qs.close();
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					log.warn("Could not close connection", e);
				}
			}
		}
	}

	private boolean activate(IbisContext ibisContext, String name,
			String jmsRealm, IPipeLineSession session) throws PipeRunException {
		Connection conn = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext
				.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(jmsRealm);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");

		try {
			qs.configure();
			qs.open();
			conn = qs.getConnection();
			String query = ("UPDATE IBISCONFIG SET ACTIVECONFIG = '"
					+ (qs.getDbmsSupport().getBooleanValue(true))
					+ "' WHERE NAME=? AND CRE_TYDST=(SELECT MAX(CRE_TYDST) FROM IBISCONFIG WHERE NAME=?)");
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setString(1, name);
			stmt.setString(2, name);
			return (stmt.executeUpdate() > 0) ? true : false;
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Error occured on activating config", e);
		} finally {
			qs.close();
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					log.warn("Could not close connection", e);
				}
			}
		}
	}
}