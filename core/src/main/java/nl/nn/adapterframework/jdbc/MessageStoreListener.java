/*
   Copyright 2015-2017, 2020 Nationale-Nederlanden

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.text.StrTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.stream.Message;

/**
 * Read messages from the ibisstore previously stored by a
 * {@link MessageStoreSender}.
 * 
 * Example configuration:
 * <code><pre>
		&lt;listener
			name="MyListener"
			className="nl.nn.adapterframework.jdbc.MessageStoreListener"
			jmsRealm="jdbc"
			slotId="${instance.name}/MyService"
			sessionKeys="key1,key2"
		/>
		&lt;!-- DummyTransactionalStorage to enable messagelog browser in the console (messages are moved to messagelog by MessageStoreListener hence JdbcTransactionalStorage isn't needed) -->
		&lt;messageLog
			className="nl.nn.adapterframework.jdbc.DummyTransactionalStorage"
			jmsRealm="jdbc"
			slotId="${instance.name}/ServiceName"
		/>
		&lt;!-- On error the message is moved to the errorStorage. And when moveToMessageLog="true" also to the messageLog (after manual resend the messageLog doesn't change). -->
		&lt;errorStorage
			className="nl.nn.adapterframework.jdbc.JdbcTransactionalStorage"
			jmsRealm="jdbc"
			slotId="${instance.name}/ServiceName"
		/>
 * </pre></code>
 * 
 * 
 * @author Jaco de Groot
 */
public class MessageStoreListener extends JdbcQueryListener {
	private String slotId;
	private String sessionKeys = null;
	private List<String> sessionKeysList;
	private boolean moveToMessageLog = true;

	public void configure() throws ConfigurationException {
		if (sessionKeys != null) {
			sessionKeysList = new ArrayList<String>();
			StringTokenizer stringTokenizer = new StringTokenizer(sessionKeys, ",");
			while (stringTokenizer.hasMoreElements()) {
				sessionKeysList.add((String)stringTokenizer.nextElement());
			}
		}
		setSelectQuery("SELECT MESSAGEKEY, MESSAGE FROM IBISSTORE "
				+ "WHERE TYPE = '" + ITransactionalStorage.TYPE_MESSAGESTORAGE + "' AND SLOTID = '" + slotId + "' ");
				// This class was initially developed as DelayStoreListener with
				// the following condition added. We could still add an
				// optional delay attribute but this functionality wasn't used
				// anymore and the condition is Oracle specific.
				// + "AND SYSTIMESTAMP >= MESSAGEDATE + INTERVAL '" + delay + "' SECOND");
		String query = "UPDATE IBISSTORE SET TYPE = '" + ITransactionalStorage.TYPE_MESSAGELOG_RECEIVER + "', COMMENTS = '" + ReceiverBase.RCV_MESSAGE_LOG_COMMENTS + "', EXPIRYDATE = ({fn now()} + 30) WHERE MESSAGEKEY = ?";
		if (!isMoveToMessageLog()) {
			query = "DELETE FROM IBISSTORE WHERE MESSAGEKEY = ?";
		}
		setUpdateStatusToProcessedQuery(query);
		setUpdateStatusToErrorQuery(query);
		setKeyField("MESSAGEKEY");
		setMessageField("MESSAGE");
		setMessageFieldType("blob");
		setBlobSmartGet(true);
		super.configure();
	}

	@Override
	public Object getRawMessage(Map threadContext) throws ListenerException {
		Object rawMessage = super.getRawMessage(threadContext);
		if (rawMessage != null && sessionKeys != null) {
			MessageWrapper messageWrapper = (MessageWrapper)rawMessage;
			try {
				StrTokenizer strTokenizer = StrTokenizer.getCSVInstance().reset(messageWrapper.getMessage().asString());
				messageWrapper.setMessage(new Message((String)strTokenizer.next()));
				int i = 0;
				while (strTokenizer.hasNext()) {
					threadContext.put(sessionKeysList.get(i), strTokenizer.next());
					i++;
				}
			} catch (IOException e) {
				throw new ListenerException("cannot convert message",e);
			}
		}
		return rawMessage;
	}

	@IbisDoc({"identifier for this service", ""})
	public void setSlotId(String slotId) {
		this.slotId = slotId;
	}

	public String getSlotId() {
		return slotId;
	}

	@IbisDoc({"comma separated list of sessionkey's to be read together with the message. please note: corresponding {@link messagestoresender} must have the same value for this attribute", ""})
	public void setSessionKeys(String sessionKeys) {
		this.sessionKeys = sessionKeys;
	}

	public String getSessionKeys() {
		return sessionKeys;
	}

	@IbisDoc({"move to messagelog after processing, as the message is already stored in the ibisstore only some fields need to be updated, use a messagelog element with class {@link dummytransactionalstorage} to enable it in the console", "true"})
	public void setMoveToMessageLog(boolean moveToMessageLog) {
		this.moveToMessageLog = moveToMessageLog;
	}

	public boolean isMoveToMessageLog() {
		return moveToMessageLog;
	}

}
