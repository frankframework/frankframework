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
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.text.StrBuilder;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Send messages to the ibisstore to have them processed exactly-once by another
 * adapter which will read the messages using a {@link MessageStoreListener}.
 * This other adapter will process the messages asynchronously and (optionally)
 * under transaction control. Duplicate messages are ignored based on the
 * messageId (except when onlyStoreWhenMessageIdUnique is set to false), hence
 * the sender of the message can retry sending the message until a valid reply
 * is received in which case it can be certain that the message is stored in the
 * ibisstore.
 * 
 * Add a messageLog element with class {@link DummyTransactionalStorage} to
 * prevent the warning "... has no messageLog..." and enable the message
 * browser in the console. Set it's type to A to view the messages moved to the
 * messageLog by the {@link MessageStoreListener} or M to view the messages in
 * the messageStore which still need to be processed.
 * 
 * Example configuration:
 * <code><pre>
		&lt;sender
			className="nl.nn.adapterframework.jdbc.MessageStoreSender"
			jmsRealm="jdbc"
			slotId="${instance.name}/ServiceName"
			sessionKeys="key1,key2"
			>
			&lt;param name="messageId" xpathExpression="/Envelope/Header/MessageID"/>
		&lt;/sender>
		&lt;!-- DummyTransactionalStorage to enable messagestore browser in the console (JdbcTransactionalStorage would store an extra record in the ibisstore) -->
		&lt;messageLog
			className="nl.nn.adapterframework.jdbc.DummyTransactionalStorage"
			jmsRealm="jdbc"
			slotId="${instance.name}/ServiceName"
			type="M"
		/>
</pre></code>
 * 
 * 
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>messageId</td><td>string</td><td>messageId to check for duplicates, when this parameter isn't present the messageId it read from sessionKey messageId</td></tr>
 * </table>
 * </p>
 * 
 * @author Jaco de Groot
 */
public class MessageStoreSender extends JdbcTransactionalStorage implements ISenderWithParameters {
	private ParameterList paramList = null;
	private String sessionKeys = null;
	private boolean onlyStoreWhenMessageIdUnique = true;

	@Override
	public void configure() throws ConfigurationException {
		if (paramList != null) {
			paramList.configure();
		}
		setType(JdbcTransactionalStorage.TYPE_MESSAGESTORAGE);
		setOnlyStoreWhenMessageIdUnique(isOnlyStoreWhenMessageIdUnique());
		super.configure();
	}

	@Override
	public boolean isSynchronous() {
		return false;
	}

	@Override
	public void addParameter(Parameter p) {
		if (paramList == null) {
			paramList = new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public String sendMessage(String correlationID, String message)
			throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, null);
	}

	@Override
	public String sendMessage(String correlationID, String message,
			ParameterResolutionContext prc) throws SenderException,
			TimeOutException {
		if (sessionKeys != null) {
			List<String> list = new ArrayList<String>();
			list.add(StringEscapeUtils.escapeCsv(message));
			StringTokenizer tokenizer = new StringTokenizer(sessionKeys, ",");
			while (tokenizer.hasMoreElements()) {
				String sessionKey = (String)tokenizer.nextElement();
				list.add(StringEscapeUtils.escapeCsv((String)prc.getSession().get(sessionKey)));
			}
			StrBuilder sb = new StrBuilder();
			sb.appendWithSeparators(list, ",");
			message = sb.toString();
		}
		String messageId = prc.getSession().getMessageId();
		if (prc != null && paramList != null
				&& paramList.findParameter("messageId") != null) {
			try {
				messageId = (String)prc.getValueMap(paramList).get("messageId");
			} catch (ParameterException e) {
				throw new SenderException("Could not resolve parameter messageId", e);
			}
		}
		return storeMessage(messageId, correlationID, new Date(), null, null, message);
	}

	@IbisDoc({"comma separated list of sessionkey's to be stored together with the message. please note: corresponding {@link messagestorelistener} must have the same value for this attribute", ""})
	public void setSessionKeys(String sessionKeys) {
		this.sessionKeys = sessionKeys;
	}

	public String getSessionKeys() {
		return sessionKeys;
	}

	@IbisDoc({" ", "true"})
	@Override
	public void setOnlyStoreWhenMessageIdUnique(boolean onlyStoreWhenMessageIdUnique) {
		this.onlyStoreWhenMessageIdUnique = onlyStoreWhenMessageIdUnique;
	}

	@Override
	public boolean isOnlyStoreWhenMessageIdUnique() {
		return onlyStoreWhenMessageIdUnique;
	}

}
