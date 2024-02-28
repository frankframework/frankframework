/*
   Copyright 2017, 2020 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.extensions.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.pipes.StreamPipe;
import org.frankframework.util.AppConstants;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.XmlUtils;

/**
 * Extension to StreamPipe for API Management.
 * <p>
 * In {@link StreamPipe} for parameter <code>httpRequest</code> and attribute
 * <code>extractFirstStringPart=true</code> the first part is returned to the pipeline.
 * In this class the first part is checked. If it contains a 'MessageID' with namespace "http://www.w3.org/2005/08/addressing",
 * then the message to return to the pipeline is retrieved from the MessageStore.
 * </p><p>
 * This class is created for applications which can not perform one multipart call with a business request in the first (string) part
 * and one or more filestreams in the next (file) parts. Instead of one multipart call, two calls are performed:
 * <ol>
 *    <li>text/xml call with the business request. The API Management application returns on this call an unique messageId (which is
 *     saved in the MessageStore together with the business request)</li>
 *    <li>multipart call with in the first (string) part the unique messageId and in the following (file) parts the filestreams</li>
 * </ol>
 * </p>
 *
 * @author Peter Leeuwenburgh
 */
@Deprecated
public class ApiStreamPipe extends StreamPipe {
	private String jmsRealm;

	private FixedQuerySender dummyQuerySender;

	@Override
	public void configure() throws ConfigurationException {
		setExtractFirstStringPart(true);
		super.configure();
	}

	@Override
	protected String adjustFirstStringPart(String firstStringPart, PipeLineSession session) throws PipeRunException {
		if (firstStringPart == null) {
			return "";
		}
		boolean retrieveMessage = false;
		if (XmlUtils.isWellFormed(firstStringPart, "MessageID")) {
			String rootNamespace = XmlUtils.getRootNamespace(firstStringPart);
			if ("http://www.w3.org/2005/08/addressing".equals(rootNamespace)) {
				retrieveMessage = true;
			}
		}
		if (retrieveMessage) {
			String messageId;
			try {
				messageId = XmlUtils.evaluateXPathNodeSetFirstElement(firstStringPart, "MessageID");
			} catch (Exception e) {
				throw new PipeRunException(this, "Exception getting MessageID", e);
			}
			if (StringUtils.isEmpty(messageId)) {
				throw new PipeRunException(this, "Could not find messageId in request [" + firstStringPart + "]");
			}
			// TODO: create dummyQuerySender should be put in configure(), but gives an error
			dummyQuerySender = createBean(FixedQuerySender.class);
			dummyQuerySender.setJmsRealm(jmsRealm);
			dummyQuerySender.setQuery("SELECT count(*) FROM ALL_TABLES");
			try {
				dummyQuerySender.configure();
			} catch (ConfigurationException e) {
				throw new PipeRunException(this, "Exception configuring dummy query sender", e);
			}

			String slotId = AppConstants.getInstance().getProperty("instance.name") + "/" + session.get("operation");
			String selectMessageKeyResult = null;
			try {
				selectMessageKeyResult = selectMessageKey(slotId, messageId);
			} catch (Exception e) {
				throw new PipeRunException(this, "Exception getting messageKey", e);
			}
			if (StringUtils.isEmpty(selectMessageKeyResult)) {
				throw new PipeRunException(this, "Could not find message in MessageStore for slotId [" + slotId + "] and messageId [" + messageId + "]");
			}
			String selectMessageResult = null;
			try {
				selectMessageResult = selectMessage(selectMessageKeyResult);
			} catch (Exception e) {
				throw new PipeRunException(this, "Exception getting message", e);
			}
			if (StringUtils.isEmpty(selectMessageResult)) {
				throw new PipeRunException(this, "Could not find message in MessageStore with messageKey [" + selectMessageKeyResult + "]");
			}
			try {
				deleteMessage(selectMessageKeyResult);
			} catch (Exception e) {
				throw new PipeRunException(this, "Exception deleting message", e);
			}
			return selectMessageResult;
		}
		return firstStringPart;
	}

	private String selectMessageKey(String slotId, String messageId) throws JdbcException {
		String query = "SELECT MESSAGEKEY FROM IBISSTORE WHERE TYPE='?' AND SLOTID='?' AND MESSAGEID='?'";
		try (Connection connection = dummyQuerySender.getConnection(); PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setString(1, IMessageBrowser.StorageType.MESSAGESTORAGE.getCode());
			stmt.setString(2, slotId);
			stmt.setString(3, messageId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return rs.getString(1);
			}
		} catch (Exception e) {
			throw new JdbcException("could not execute query [" + query + "]", e);
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
		try (Connection connection = dummyQuerySender.getConnection(); PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.execute();
		} catch (Exception e) {
			throw new JdbcException("could not execute query [" + query + "]", e);
		}
	}

	public String getJmsRealm() {
		return jmsRealm;
	}

	public void setJmsRealm(String jmsRealm) {
		this.jmsRealm = jmsRealm;
	}

}
