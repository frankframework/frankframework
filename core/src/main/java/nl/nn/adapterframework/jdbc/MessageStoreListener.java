/*
   Copyright 2015 Nationale-Nederlanden

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.ReceiverBase;

import org.apache.commons.lang.text.StrTokenizer;

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
 * </pre></code>
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.MessageStoreListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSlotId(String) slotId}</td><td>identifier for this service</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSessionKeys(String) sessionKeys}</td><td>comma separated list of sessionKey's to be read together with the message. Please note: corresponding {@link MessageStoreSender} must have the same value for this attribute</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMoveToMessageLog(boolean) moveToMessageLog}</td><td>move to messagelog after processing, as the message is already stored in the ibisstore only some fields need to be updated, use a messageLog element with class {@link DummyTransactionalStorage} to enable it in the console</td><td>true</td></tr>
 * </table>
 * </p>
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
				+ "WHERE TYPE = '" + JdbcTransactionalStorage.TYPE_MESSAGESTORAGE + "' AND SLOTID = '" + slotId + "' ");
				// This class was initially developed as DelayStoreListener with
				// the following condition added. We could still add an
				// optional delay attribute but this functionality wasn't used
				// anymore and the condition is Oracle specific.
				// + "AND SYSTIMESTAMP >= MESSAGEDATE + INTERVAL '" + delay + "' SECOND");
		String query = "UPDATE IBISSTORE SET TYPE = '" + JdbcTransactionalStorage.TYPE_MESSAGELOG_RECEIVER + "', COMMENTS = '" + ReceiverBase.RCV_MESSAGE_LOG_COMMENTS + "', EXPIRYDATE = ({fn now()} + 30) WHERE MESSAGEKEY = ?";
		
//		Date date = new Date();
//		Calendar cal = Calendar.getInstance();
//		cal.setTime(date);
//		cal.add(Calendar.DAY_OF_MONTH, getRetention());
//		stmt.setTimestamp(++parPos, new Timestamp(cal.getTime().getTime()));

		
		
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
			StrTokenizer strTokenizer = StrTokenizer.getCSVInstance().reset(messageWrapper.getText());
			messageWrapper.setText((String)strTokenizer.next());
			int i = 0;
			while (strTokenizer.hasNext()) {
				threadContext.put(sessionKeysList.get(i), strTokenizer.next());
				i++;
			}
		}
		return rawMessage;
	}

	public void setSlotId(String slotId) {
		this.slotId = slotId;
	}

	public String getSlotId() {
		return slotId;
	}

	public void setSessionKeys(String sessionKeys) {
		this.sessionKeys = sessionKeys;
	}

	public String getSessionKeys() {
		return sessionKeys;
	}

	public void setMoveToMessageLog(boolean moveToMessageLog) {
		this.moveToMessageLog = moveToMessageLog;
	}

	public boolean isMoveToMessageLog() {
		return moveToMessageLog;
	}

}
