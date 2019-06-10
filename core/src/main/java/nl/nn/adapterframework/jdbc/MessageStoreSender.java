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
import nl.nn.adapterframework.doc.IbisDescription; 
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
 * @author Jaco de Groot
 */
@IbisDescription(
	"Send messages to the ibisstore to have them processed exactly-once by another \n" + 
	"adapter which will read the messages using a {@link MessageStoreListener}. \n" + 
	"This other adapter will process the messages asynchronously and (optionally) \n" + 
	"under transaction control. Duplicate messages are ignored based on the \n" + 
	"messageId (except when onlyStoreWhenMessageIdUnique is set to false), hence \n" + 
	"the sender of the message can retry sending the message until a valid reply \n" + 
	"is received in which case it can be certain that the message is stored in the \n" + 
	"ibisstore. \n" + 
	"Add a messageLog element with class {@link DummyTransactionalStorage} to \n" + 
	"prevent the warning \"... has no messageLog...\" and enable the message \n" + 
	"browser in the console. Set it's type to A to view the messages moved to the \n" + 
	"messageLog by the {@link MessageStoreListener} or M to view the messages in \n" + 
	"the messageStore which still need to be processed. \n" + 
	"Example configuration: \n" + 
	"<code><pre> \n" + 
	"lt;sender \n" + 
	"className=\"nl.nn.adapterframework.jdbc.MessageStoreSender\" \n" + 
	"jmsRealm=\"jdbc\" \n" + 
	"slotId=\"${instance.name}/ServiceName\" \n" + 
	"sessionKeys=\"key1,key2\" \n" + 
	"> \n" + 
	"&lt;param name=\"messageId\" xpathExpression=\"/Envelope/Header/MessageID\"/> \n" + 
	"lt;/sender> \n" + 
	"lt;!-- DummyTransactionalStorage to enable messagestore browser in the console (JdbcTransactionalStorage would store an extra record in the ibisstore) --> \n" + 
	"lt;messageLog \n" + 
	"className=\"nl.nn.adapterframework.jdbc.DummyTransactionalStorage\" \n" + 
	"jmsRealm=\"jdbc\" \n" + 
	"slotId=\"${instance.name}/ServiceName\" \n" + 
	"type=\"M\" \n" + 
	"> \n" + 
	"re></code> \n" + 
	"<table border=\"1\"> \n" + 
	"<p><b>Parameters:</b> \n" + 
	"<tr><th>name</th><th>type</th><th>remarks</th></tr> \n" + 
	"<tr><td>messageId</td><td>string</td><td>messageId to check for duplicates, when this parameter isn't present the messageId it read from sessionKey messageId</td></tr> \n" + 
	"</table> \n" + 
	"</p> \n" 
)
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
