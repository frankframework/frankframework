/*
   Copyright 2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.api;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.StreamPipe;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Extension to StreamPipe for API Management.
 *
 * @author Peter Leeuwenburgh
 */

public class ApiStreamPipe extends StreamPipe {
	private String jmsRealm;

	@Override
	public void configure() throws ConfigurationException {
		setExtractFirstStringPart(true);
		super.configure();
	}

	@Override
	protected String adjustFirstStringPart(String firstStringPart,
			IPipeLineSession session) throws PipeRunException {
		if (firstStringPart == null) {
			return "";
		} else {
			boolean retrieveMessage = false;
			if (XmlUtils.isWellFormed(firstStringPart, "MessageID")) {
				String rootNamespace = XmlUtils
						.getRootNamespace(firstStringPart);
				if ("http://www.w3.org/2005/08/addressing"
						.equals(rootNamespace)) {
					retrieveMessage = true;
				}
			}
			if (retrieveMessage) {
				String messageId = null;
				try {
					messageId = XmlUtils.evaluateXPathNodeSetFirstElement(
							firstStringPart, "MessageID");
				} catch (Exception e) {
					throw new PipeRunException(this,
							"Exception getting MessageID", e);
				}
				if (StringUtils.isEmpty(messageId)) {
					throw new PipeRunException(this,
							"Could not find messageId in request ["
									+ firstStringPart + "]");
				} else {
					ParameterResolutionContext prc = new ParameterResolutionContext(
							"", session);
					String slotId = AppConstants.getInstance()
							.getResolvedProperty("instance.name") + "/"
							+ session.get("operation");
					String selectMessageKeyResult = null;
					try {
						selectMessageKeyResult = selectMessageKey(slotId,
								messageId);
					} catch (Exception e) {
						throw new PipeRunException(this,
								"Exception getting messageKey", e);
					}
					if (StringUtils.isEmpty(selectMessageKeyResult)) {
						throw new PipeRunException(this,
								"Could not find message in MessageStore for slotId ["
										+ slotId + "] and messageId ["
										+ messageId + "]");
					} else {
						String selectMessageResult = null;
						try {
							selectMessageResult = selectMessage(
									selectMessageKeyResult);
						} catch (Exception e) {
							throw new PipeRunException(this,
									"Exception getting message", e);
						}
						if (StringUtils.isEmpty(selectMessageResult)) {
							throw new PipeRunException(this,
									"Could not find message in MessageStore with messageKey ["
											+ selectMessageKeyResult + "]");
						} else {
							try {
								deleteMessage(selectMessageKeyResult);
							} catch (Exception e) {
								throw new PipeRunException(this,
										"Exception deleting message", e);
							}
						}
						return selectMessageResult;
					}
				}
			} else {
				return firstStringPart;
			}
		}
	}

	private String selectMessageKey(String slotId, String messageId)
			throws ConfigurationException, SenderException, TimeOutException {
		IbisContext ibisContext = getAdapter().getConfiguration()
				.getIbisManager().getIbisContext();
		DirectQuerySender qs = (DirectQuerySender) ibisContext
				.createBeanAutowireByName(DirectQuerySender.class);
		try {
			qs.setName("selectMessageKeySender");
			qs.setJmsRealm(getJmsRealm());
			qs.setQueryType("select");
			qs.setScalar(true);
			qs.configure(true);
			qs.open();
			String query = "SELECT MESSAGEKEY FROM IBISSTORE WHERE TYPE='"
					+ JdbcTransactionalStorage.TYPE_MESSAGESTORAGE
					+ "' AND SLOTID='" + slotId + "' AND MESSAGEID='"
					+ messageId + "'";
			return qs.sendMessage("dummy", query);
		} finally {
			qs.close();
		}
	}

	private String selectMessage(String messageKey)
			throws ConfigurationException, SenderException, TimeOutException {
		IbisContext ibisContext = getAdapter().getConfiguration()
				.getIbisManager().getIbisContext();
		DirectQuerySender qs = (DirectQuerySender) ibisContext
				.createBeanAutowireByName(DirectQuerySender.class);
		try {
			qs.setName("selectMessageSender");
			qs.setJmsRealm(getJmsRealm());
			qs.setQueryType("select");
			qs.setScalar(true);
			qs.setBlobSmartGet(true);
			qs.configure(true);
			qs.open();
			String query = "SELECT MESSAGE FROM IBISSTORE WHERE MESSAGEKEY='"
					+ messageKey + "'";
			return qs.sendMessage("dummy", query);
		} finally {
			qs.close();
		}
	}

	private void deleteMessage(String messageKey)
			throws ConfigurationException, SenderException, TimeOutException {
		IbisContext ibisContext = getAdapter().getConfiguration()
				.getIbisManager().getIbisContext();
		DirectQuerySender qs = (DirectQuerySender) ibisContext
				.createBeanAutowireByName(DirectQuerySender.class);
		try {
			qs.setName("deleteMessageSender");
			qs.setJmsRealm(getJmsRealm());
			qs.setQueryType("delete");
			qs.configure(true);
			qs.open();
			String query = "DELETE FROM IBISSTORE WHERE MESSAGEKEY='"
					+ messageKey + "'";
			qs.sendMessage("dummy", query);
		} finally {
			qs.close();
		}
	}

	public String getJmsRealm() {
		return jmsRealm;
	}

	public void setJmsRealm(String jmsRealm) {
		this.jmsRealm = jmsRealm;
	}
}
