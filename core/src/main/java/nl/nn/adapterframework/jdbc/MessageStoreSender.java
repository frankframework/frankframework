/*
   Copyright 2015 Nationale-Nederlanden, 2021, 2022, 2023 WeAreFrank!

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
import java.util.Date;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

import lombok.Getter;
import lombok.SneakyThrows;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.ExcludeFromType;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StringUtil;

/**
 * Send messages to the IBISSTORE database table to have them processed exactly-once by another
 * adapter which will read the messages using a {@link MessageStoreListener}.
 * This other adapter will process the messages asynchronously and (optionally)
 * under transaction control. Duplicate messages are ignored based on the
 * messageId (except when onlyStoreWhenMessageIdUnique is set to false), hence
 * the sender of the message can retry sending the message until a valid reply
 * is received in which case it can be certain that the message is stored in the
 * database table IBISSTORE.
 * <br/><br/>
 * If you have a <code>MessageStoreSender</code> it does not make sense to add a <code>JdbcMessageLog</code>
 * or <code>JdbcErrorStorage</code> in the same sender pipe. A <code>MessageStoreSender</code>
 * acts as a message log and an error store. It can be useful however to add a message log or error store
 * to the adapter around the sender pipe, because errors may occur before the message reaches the sender pipe.
 * <br/><br/>
 * Example configuration:
 * <code><pre>
	&lt;SenderPipe name="Send"&gt;
		&lt;MessageStoreSender
			slotId="${instance.name}/TestMessageStore"
			onlyStoreWhenMessageIdUnique="false"
		/&gt;
	&lt;/SenderPipe&gt;
</pre></code>
 *
 * @ff.parameter messageId messageId to check for duplicates, when this parameter isn't present the messageId is read from sessionKey messageId
 *
 * @author Jaco de Groot
 */
@ExcludeFromType(ITransactionalStorage.class)
public class MessageStoreSender extends JdbcTransactionalStorage<String> implements ISenderWithParameters {
	public static final String PARAM_MESSAGEID = "messageId";

	private ParameterList paramList = null;
	private @Getter String sessionKeys = null;

	{
		setOnlyStoreWhenMessageIdUnique(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		if (paramList != null) {
			paramList.configure();
		}
		setType(StorageType.MESSAGESTORAGE.getCode());
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

	/**
	 * Helper method to convert a message to a string from Stream.map, without hitting the exception thrown
	 * and without the issue of ambiguous method overload for lambda reference.
	 *
	 * @param message Message to convert
	 * @return String of the message.
	 */
	@SneakyThrows
	private String messageAsString(Message message) {
		return message.asString();
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		try {
			String messageToStore;
			if (sessionKeys == null) {
				messageToStore = message.asString(); // if no session keys are specified, message is stored without escaping, for compatibility with normal messagestore operation.
			} else {
				List<String> list = new ArrayList<>();
				list.add(StringEscapeUtils.escapeCsv(message.asString()));
				StringUtil.splitToStream(sessionKeys)
						.map(session::getMessage)
						.map(this::messageAsString)
						.map(StringEscapeUtils::escapeCsv)
						.forEachOrdered(list::add);
				messageToStore = String.join(",", list);
			}
			// the messageId to be inserted in the messageStore defaults to the messageId of the session
			String messageId = session.getMessageId();
			String correlationID = session.getCorrelationId();
			if (paramList != null && paramList.findParameter(PARAM_MESSAGEID) != null) {
				try {
					// the messageId to be inserted can also be specified via the parameter messageId
					messageId = paramList.getValues(message, session).get(PARAM_MESSAGEID).asStringValue();
				} catch (ParameterException e) {
					throw new SenderException("Could not resolve parameter messageId", e);
				}
			}
			return new SenderResult(storeMessage(messageId, correlationID, new Date(), null, null, messageToStore));
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(),e);
		}
	}

	/**
	 * Comma separated list of sessionKey's to be stored together with the message. Please note: corresponding {@link MessageStoreListener} must have the same value for this attribute.
	 */
	public void setSessionKeys(String sessionKeys) {
		this.sessionKeys = sessionKeys;
	}

	/**
	 * If set to <code>true</code>, the message is stored only if the MessageId is not present in the store yet.
	 *
	 * @ff.default <code>true</code>
	 */
	@Override
	public void setOnlyStoreWhenMessageIdUnique(boolean onlyStoreWhenMessageIdUnique) {
		super.setOnlyStoreWhenMessageIdUnique(onlyStoreWhenMessageIdUnique);
	}

}
