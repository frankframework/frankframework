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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.pipes.StreamPipe;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Extension to StreamPipe for API Management.
 *
 * @author Peter Leeuwenburgh
 */

public class ApiStreamPipe extends StreamPipe {
	private String jmsRealm;

	private DirectQuerySender dummyQuerySender;

	@Override
	public void configure() throws ConfigurationException {
		setExtractFirstStringPart(true);
		super.configure();

		IbisContext ibisContext = getAdapter().getConfiguration()
				.getIbisManager().getIbisContext();
		dummyQuerySender = (DirectQuerySender) ibisContext
				.createBeanAutowireByName(DirectQuerySender.class);
		dummyQuerySender.setJmsRealm(jmsRealm);
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
					throw new PipeRunException(this, getLogPrefix(session)
							+ "Exception getting MessageID", e);
				}
				if (StringUtils.isEmpty(messageId)) {
					throw new PipeRunException(this,
							getLogPrefix(session)
									+ "Could not find messageId in request ["
									+ firstStringPart + "]");
				} else {
					Connection conn = null;
					try {
						try {
							conn = dummyQuerySender.getConnection();
						} catch (JdbcException e) {
							throw new PipeRunException(this,
									getLogPrefix(session)
											+ "Exception getting connection",
									e);
						}
						String slotId = AppConstants.getInstance()
								.getResolvedProperty("instance.name") + "/"
								+ session.get("operation");
						return retrieveMessageFromStore(conn, slotId, messageId,
								session);
					} finally {
						if (conn != null) {
							try {
								conn.close();
							} catch (SQLException e) {
								log.warn(
										getLogPrefix(session)
												+ "Exception closing connection",
										e);
							}
						}
					}
				}
			} else {
				return firstStringPart;
			}
		}
	}

	private String retrieveMessageFromStore(Connection conn, String slotId,
			String messageId, IPipeLineSession session)
			throws PipeRunException {
		String selectMessageKeyResult = null;
		try {
			String query = "SELECT MESSAGEKEY FROM IBISSTORE WHERE TYPE='"
					+ JdbcTransactionalStorage.TYPE_MESSAGESTORAGE
					+ "' AND SLOTID='" + slotId + "' AND MESSAGEID='"
					+ messageId + "'";
			selectMessageKeyResult = JdbcUtil.executeStringQuery(conn, query);
		} catch (Exception e) {
			throw new PipeRunException(this,
					getLogPrefix(session) + "Exception getting messageKey", e);
		}
		if (StringUtils.isEmpty(selectMessageKeyResult)) {
			throw new PipeRunException(this,
					getLogPrefix(session)
							+ "Could not find message in MessageStore for slotId ["
							+ slotId + "] and messageId [" + messageId + "]");
		} else {
			String selectMessageResult = null;
			try {
				String query = "SELECT MESSAGE FROM IBISSTORE WHERE MESSAGEKEY='"
						+ selectMessageKeyResult + "'";
				selectMessageResult = JdbcUtil.executeBlobQuery(conn, query);
			} catch (Exception e) {
				throw new PipeRunException(this,
						getLogPrefix(session) + "Exception getting message", e);
			}
			if (StringUtils.isEmpty(selectMessageResult)) {
				throw new PipeRunException(this,
						getLogPrefix(session)
								+ "Could not find message in MessageStore with messageKey ["
								+ selectMessageKeyResult + "]");
			} else {
				try {
					String query = "DELETE FROM IBISSTORE WHERE MESSAGEKEY='"
							+ selectMessageKeyResult + "'";
					JdbcUtil.executeStatement(conn, query);
				} catch (Exception e) {
					throw new PipeRunException(this, getLogPrefix(session)
							+ "Exception deleting message", e);
				}
			}
			return selectMessageResult;
		}
	}

	public String getJmsRealm() {
		return jmsRealm;
	}

	public void setJmsRealm(String jmsRealm) {
		this.jmsRealm = jmsRealm;
	}

}
