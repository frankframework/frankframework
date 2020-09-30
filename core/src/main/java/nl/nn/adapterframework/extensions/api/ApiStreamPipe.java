/*
   Copyright 2017, 2020 Nationale-Nederlanden, 2020 WeAreFrank!

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
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.pipes.StreamPipe;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Extension to StreamPipe for API Management.
 * <p>
 * In {@link nl.nn.adapterframework.pipes.StreamPipe} for parameter <code>httpRequest</code> and attribute 
 * <code>extractFirstStringPart=true</code> the first part is returned to the pipeline.
 * In this class the first part is checked. If it contains a 'MessageID' with namespace "http://www.w3.org/2005/08/addressing",
 * then the message to return to the pipeline is retrieved from the MessageStore.
 * <p>
 * This class is created for applications which can not perform one multipart call with a business request in the first (string) part
 * and one or more filestreams in the next (file) parts. Instead of one multipart call, two calls are performed:
 * <ol>
 *    <li>text/xml call with the business request. The API Management application returns on this call an unique messageId (which is
 *     saved in the MessageStore together with the business request)</li>
 *    <li>multipart call with in the first (string) part the unique messageId and in the following (file) parts the filestreams</li>
 * </ol>
 * <p>
 * @author Peter Leeuwenburgh
 */

public class ApiStreamPipe extends StreamPipe {
	private String jmsRealm;

	private FixedQuerySender dummyQuerySender;

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
					// TODO: create dummyQuerySender should be put in
					// configure(), but gives an error
					IbisContext ibisContext = getAdapter().getConfiguration()
							.getIbisManager().getIbisContext();
					dummyQuerySender = (FixedQuerySender) ibisContext
							.createBeanAutowireByName(FixedQuerySender.class);
					dummyQuerySender.setJmsRealm(jmsRealm);
					dummyQuerySender
							.setQuery("SELECT count(*) FROM ALL_TABLES");
					try {
						dummyQuerySender.configure();
					} catch (ConfigurationException e) {
						throw new PipeRunException(this,
								"Exception configuring dummy query sender", e);
					}

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

	private String selectMessageKey(String slotId, String messageId) throws JdbcException {
		String query = "SELECT MESSAGEKEY FROM IBISSTORE WHERE TYPE='" + ITransactionalStorage.StorageType.MESSAGESTORAGE.getCode() + "' AND SLOTID='" + slotId + "' AND MESSAGEID='" + messageId + "'";
		Connection conn = dummyQuerySender.getConnection();
		try {
			return JdbcUtil.executeStringQuery(conn, query);
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					log.warn("Could not close connection", e);
				}
			}
		}
	}

	private String selectMessage(String messageKey) throws JdbcException {
		String query = "SELECT MESSAGE FROM IBISSTORE WHERE MESSAGEKEY='" + messageKey + "'";
		Connection conn = dummyQuerySender.getConnection();
		try {
			return JdbcUtil.executeBlobQuery(dummyQuerySender.getDbmsSupport(), conn, query);
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					log.warn("Could not close connection", e);
				}
			}
		}
	}

	private void deleteMessage(String messageKey) throws JdbcException {
		String query = "DELETE FROM IBISSTORE WHERE MESSAGEKEY='" + messageKey + "'";
		Connection conn = dummyQuerySender.getConnection();
		try {
			JdbcUtil.executeStatement(conn, query);
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					log.warn("Could not close connection", e);
				}
			}
		}
	}

	public String getJmsRealm() {
		return jmsRealm;
	}

	public void setJmsRealm(String jmsRealm) {
		this.jmsRealm = jmsRealm;
	}

}
