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

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Show configurations.
 * 
 * @author Peter Leeuwenburgh
 */

public class ShowConfig extends TimeoutGuardPipe {

	public String doPipeWithTimeoutGuarded(Object input,
			IPipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return doGet(session);
		} else {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Illegal value for method [" + method
					+ "], must be 'GET'");
		}
	}

	private String doGet(IPipeLineSession session) throws PipeRunException {
		String parm_name = (String) session.get("name");
		if (StringUtils.isEmpty(parm_name)) {
			return retrieveAllConfigs(session);
		} else {
			session.put("contentType", "application/octet-stream");
			String parm_jmsRealm = (String) session.get("jmsRealm");
			if (StringUtils.isEmpty(parm_jmsRealm)) {
				parm_jmsRealm = JmsRealmFactory.getInstance()
						.getFirstDatasourceJmsRealm();
				if (parm_jmsRealm == null) {
					throw new PipeRunException(this, getLogPrefix(session)
							+ "No datasource jmsRealm available");
				}
			}
			FixedQuerySender qs = new FixedQuerySender();
			String queryResult;
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(parm_jmsRealm);
				qs.setQueryType("select");
				qs.setQuery("SELECT CONFIG FROM IBISCONFIG WHERE NAME=?");
				Parameter param = new Parameter();
				param.setName("name");
				param.setSessionKey("name");
				qs.addParameter(param);
				qs.setScalar(true);
				qs.setScalarExtended(true);
				qs.setBlobsCompressed(false);
				qs.setStreamResultToServlet(true);
				qs.configure();
				qs.open();
				ParameterResolutionContext prc = new ParameterResolutionContext(
						"dummy", session);
				queryResult = qs.sendMessage("dummy", "dummy", prc);
			} catch (Throwable t) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "Error occured on executing jdbc query", t);
			} finally {
				try {
					qs.close();
				} catch (SenderException e) {
					log.warn("Could not close query sender", e);
				}
			}
			if (queryResult.length() == 0) {
				// means result is found and streamed
			} else {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "Could not retrieve configuration for name ["
						+ parm_name + "]");
			}
			return queryResult;
		}
	}

	private String retrieveAllConfigs(IPipeLineSession session)
			throws PipeRunException {
		List<String> jmsRealms = JmsRealmFactory.getInstance()
				.getRegisteredDatasourceRealmNamesAsList();
		XmlBuilder configsXML = new XmlBuilder("configs");
		for (int i = 0; i < jmsRealms.size(); i++) {
			XmlBuilder configXML = new XmlBuilder("config");
			String jr = jmsRealms.get(i);
			configXML.addAttribute("jmsRealm", jr);
			FixedQuerySender qs = new FixedQuerySender();
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(jr);
				qs.setQueryType("select");
				qs.setQuery("SELECT NAME, "
						+ qs.getDbmsSupport().getLength("CONFIG")
						+ " AS LEN_CONFIG, CRE_TYDST, RUSER FROM IBISCONFIG ORDER BY NAME");
				qs.setBlobSmartGet(true);
				qs.setIncludeFieldDefinition(false);
				qs.configure();
				qs.open();
				ParameterResolutionContext prc = new ParameterResolutionContext(
						"dummy", session);
				String queryResult = qs.sendMessage("dummy", "dummy", prc);
				configXML.setValue(queryResult, false);
			} catch (Throwable t) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "Error occured on executing jdbc query", t);
			} finally {
				try {
					qs.close();
				} catch (SenderException e) {
					log.warn("Could not close query sender", e);
				}
			}
			configsXML.addSubElement(configXML);
		}
		return configsXML.toXML();
	}
}