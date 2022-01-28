/*
   Copyright 2015-2017 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;

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
			slotId="${instance.name}/ServiceName"
			sessionKeys="key1,key2"
		/>
		&lt;!-- On error the message is moved to the errorStorage. And when moveToMessageLog="true" also to the messageLog (after manual resend the messageLog doesn't change). -->
		&lt;errorStorage
			className="nl.nn.adapterframework.jdbc.JdbcTransactionalStorage"
			jmsRealm="jdbc"
			slotId="${instance.name}/ServiceName"
		/>
 * </pre></code>
 * 
 * The following defaults apply:
 * <table>
 * 	<tr><td>tableName</td><td>IBISSTORE</td></tr>
 * 	<tr><td>keyField</td><td>MESSAGEKEY</td></tr>
 * 	<tr><td>messageField</td><td>MESSAGE</td></tr>
 * 	<tr><td>messageFieldType</td><td>BLOB</td></tr>
 * 	<tr><td>blobSmartGet</td><td>true</td></tr>
 * 	<tr><td>statusField</td><td>TYPE</td></tr>
 * 	<tr><td>timestampField</td><td>MESSAGEDATE</td></tr>
 * 	<tr><td>commentField</td><td>COMMENTS</td></tr>
 * 	<tr><td>statusValueAvailable</td><td>M</td></tr>
 * 	<tr><td>statusValueProcessed</td><td>A</td></tr>
 * 	<tr><td>statusValueError</td><td>E</td></tr>
 * </table>
 * 
 * @author Jaco de Groot
 */
public class MessageStoreListener<M> extends JdbcTableListener<M> {
	private String slotId;
	private String sessionKeys = null;
	private boolean moveToMessageLog = true;

	private List<String> sessionKeysList;

	{
		setTableName("IBISSTORE");
		setKeyField("MESSAGEKEY");
		setMessageField("MESSAGE");
		setMessageFieldType("blob");
		setBlobSmartGet(true);
		setStatusField("TYPE");
		setTimestampField("MESSAGEDATE");
		setStatusValueAvailable(IMessageBrowser.StorageType.MESSAGESTORAGE.getCode());
		setStatusValueProcessed(IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode());
		setStatusValueError(IMessageBrowser.StorageType.ERRORSTORAGE.getCode());
	}
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (sessionKeys != null) {
			sessionKeysList = new ArrayList<String>();
			StringTokenizer stringTokenizer = new StringTokenizer(sessionKeys, ",");
			while (stringTokenizer.hasMoreElements()) {
				sessionKeysList.add((String)stringTokenizer.nextElement());
			}
		}
		if (isMoveToMessageLog()) {
			String setClause = "COMMENTS = '" + Receiver.RCV_MESSAGE_LOG_COMMENTS + "', EXPIRYDATE = "+getDbmsSupport().getDateAndOffset(getDbmsSupport().getSysDate(),30);
			setUpdateStatusQuery(ProcessState.DONE, createUpdateStatusQuery(getStatusValue(ProcessState.DONE),setClause));
		} else {
			String query = "DELETE FROM "+getTableName()+" WHERE "+getKeyField()+" = ?";
			setUpdateStatusQuery(ProcessState.DONE, query);
			setUpdateStatusQuery(ProcessState.ERROR, query);
		}
	}

	@Override
	public M getRawMessage(Map<String,Object> threadContext) throws ListenerException {
		M rawMessage = super.getRawMessage(threadContext);
		if (rawMessage != null && sessionKeys != null) {
			MessageWrapper<?> messageWrapper = (MessageWrapper<?>)rawMessage;
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

	protected IMessageBrowser<M> augmentMessageBrowser(IMessageBrowser<M> browser) {
		if (browser!=null && browser instanceof JdbcTableMessageBrowser) {
			JdbcTableMessageBrowser<Object> jtmb = (JdbcTableMessageBrowser<Object>)browser;
			jtmb.setCommentField("COMMENTS");
			jtmb.setExpiryDateField("EXPIRYDATE");
			jtmb.setHostField("HOST");
		}
		return browser;
	}
	
	@Override
	public IMessageBrowser<M> getMessageBrowser(ProcessState state) {
		IMessageBrowser<M> browser = super.getMessageBrowser(state);
		if (browser!=null) {
			return augmentMessageBrowser(browser);
		}
		return null;
	}

	@Override
	public String getSelectCondition() {
		String conditionClause = super.getSelectCondition();
		if (StringUtils.isNotEmpty(conditionClause)) {
			conditionClause = "("+conditionClause+")";
		}
		String slotIdClause = StringUtils.isNotEmpty(getSlotId()) ? "SLOTID='"+slotId+"'" : null;
		return Misc.concatStrings(slotIdClause, " AND ", conditionClause);
	}
	
	@IbisDoc({"1", "Identifier for this service", ""})
	public void setSlotId(String slotId) {
		this.slotId = slotId;
	}
	public String getSlotId() {
		return slotId;
	}

	@IbisDoc({"2", "Comma separated list of sessionKey's to be read together with the message. Please note: corresponding {@link MessagestoreSender} must have the same value for this attribute", ""})
	public void setSessionKeys(String sessionKeys) {
		this.sessionKeys = sessionKeys;
	}
	public String getSessionKeys() {
		return sessionKeys;
	}

	@IbisDoc({"3", "Move to messageLog after processing, as the message is already stored in the ibisstore only some fields need to be updated. When set false, messages are deleted after being processed", "true"})
	public void setMoveToMessageLog(boolean moveToMessageLog) {
		this.moveToMessageLog = moveToMessageLog;
	}
	public boolean isMoveToMessageLog() {
		return moveToMessageLog;
	}

	@Override
	@IbisDoc({"4", "Value of status field indicating is being processed. Set to 'I' if database has no SKIP LOCKED functionality or the Receiver cannot be set to Required or RequiresNew.", ""})
	public void setStatusValueInProcess(String string) {
		super.setStatusValueInProcess(string);
	}

}
