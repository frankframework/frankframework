/*
   Copyright 2016 Nationale-Nederlanden

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

import java.util.List;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Execute (insert, update and delete) jdbc properties.
 * 
 * @author Peter Leeuwenburgh
 */

public class ExecuteJdbcProperties extends TimeoutGuardPipe {
	IbisContext ibisContext;

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
		} else if (method.equalsIgnoreCase("POST")) {
			return doPost(session);
		} else {
			throw new PipeRunException(this,
					getLogPrefix(session) + "Illegal value for method ["
							+ method + "], must be 'GET' or 'POST'");
		}
	}

	private String doGet(IPipeLineSession session) throws PipeRunException {
		return retrieveFormInput(session);
	}

	private String doPost(IPipeLineSession session) throws PipeRunException {
		String action = (String) session.get("action");
		String name = (String) session.get("name");
		String value = (String) session.get("value");
		FixedQuerySender qs = (FixedQuerySender) ibisContext
				.createBeanAutowireByName(FixedQuerySender.class);
		String form_jmsRealm = (String) session.get("jmsRealm");

		if (StringUtils.isEmpty(name)) {
			throw new PipeRunException(this,
					getLogPrefix(session) + "Name should not be empty");
		}
		String result = null;

		String remoteUser = (String) session.get("principal");

		if ("insert".equalsIgnoreCase(action)) {
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(form_jmsRealm);
				qs.setQueryType("insert");
				if (StringUtils.isEmpty(remoteUser)) {
					qs.setQuery(
							"INSERT INTO IBISPROP (NAME, VALUE, LASTMODDATE) VALUES (?, ?, CURRENT_TIMESTAMP)");
				} else {
					qs.setQuery(
							"INSERT INTO IBISPROP (NAME, VALUE, LASTMODDATE, LASTMODBY) VALUES (?, ?, CURRENT_TIMESTAMP, ?)");
				}
				Parameter param = new Parameter();
				param.setName("name");
				param.setValue(name);
				qs.addParameter(param);
				param = new Parameter();
				param.setName("value");
				param.setValue(value);
				qs.addParameter(param);
				if (StringUtils.isNotEmpty(remoteUser)) {
					param = new Parameter();
					param.setName("lastmodby");
					param.setValue(remoteUser);
					qs.addParameter(param);
				}
				qs.configure();
				qs.open();
				ParameterResolutionContext prc = new ParameterResolutionContext(
						"", session);
				result = qs.sendMessage("", "", prc);
			} catch (Throwable t) {
				throw new PipeRunException(this,
						getLogPrefix(session)
								+ "Error occured on executing jdbc insert query",
						t);
			} finally {
				qs.close();
			}
		} else if ("update".equalsIgnoreCase(action)) {
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(form_jmsRealm);
				qs.setQueryType("update");
				qs.setQuery(
						"UPDATE IBISPROP SET VALUE=?, LASTMODDATE=CURRENT_TIMESTAMP, LASTMODBY=? WHERE NAME=?");
				Parameter param = new Parameter();
				param.setName("value");
				param.setValue(value);
				qs.addParameter(param);
				param = new Parameter();
				param.setName("lastmodby");
				if (StringUtils.isNotEmpty(remoteUser)) {
					param.setValue(remoteUser);
				}
				qs.addParameter(param);
				param = new Parameter();
				param.setName("name");
				param.setValue(name);
				qs.addParameter(param);
				qs.configure();
				qs.open();
				ParameterResolutionContext prc = new ParameterResolutionContext(
						"", session);
				result = qs.sendMessage("", "", prc);
			} catch (Throwable t) {
				throw new PipeRunException(this,
						getLogPrefix(session)
								+ "Error occured on executing jdbc update query",
						t);
			} finally {
				qs.close();
			}
		} else if ("delete".equalsIgnoreCase(action)) {
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(form_jmsRealm);
				qs.setQueryType("delete");
				qs.setQuery("DELETE IBISPROP WHERE NAME=?");
				Parameter param = new Parameter();
				param.setName("name");
				param.setValue(name);
				qs.addParameter(param);
				qs.configure();
				qs.open();
				ParameterResolutionContext prc = new ParameterResolutionContext(
						"", session);
				result = qs.sendMessage("", "", prc);
			} catch (Throwable t) {
				throw new PipeRunException(this,
						getLogPrefix(session)
								+ "Error occured on executing jdbc delete query",
						t);
			} finally {
				qs.close();
			}
		} else {
			throw new PipeRunException(this,
					getLogPrefix(session) + "Unknown action [" + action + "]");
		}

		session.put("result", result);
		return "<dummy/>";
	}

	private String retrieveFormInput(IPipeLineSession session) {
		List<String> jmsRealms = JmsRealmFactory.getInstance()
				.getRegisteredRealmNamesAsList();
		if (jmsRealms.size() == 0)
			jmsRealms.add("no realms defined");
		XmlBuilder jmsRealmsXML = new XmlBuilder("jmsRealms");
		for (int i = 0; i < jmsRealms.size(); i++) {
			if (StringUtils.isNotEmpty(JmsRealmFactory.getInstance()
					.getJmsRealm(jmsRealms.get(i)).getDatasourceName())) {
				XmlBuilder jmsRealmXML = new XmlBuilder("jmsRealm");
				jmsRealmXML.setValue(jmsRealms.get(i));
				jmsRealmsXML.addSubElement(jmsRealmXML);
			}
		}
		return jmsRealmsXML.toXML();
	}
}