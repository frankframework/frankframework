/*
   Copyright 2015 Nationale-Nederlanden, 2021-2026 WeAreFrank!

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
package org.frankframework.jdbc;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.ITransactionalStorage;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.ExcludeFromType;
import org.frankframework.doc.Mandatory;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TimeProvider;

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
 * <pre>{@code
 * <SenderPipe name="Send">
 *     <MessageStoreSender
 * 	     slotId="${instance.name}/TestMessageStore"
 * 		 onlyStoreWhenMessageIdUnique="false" />
 * </SenderPipe>
 * }</pre>
 *
 * @ff.parameter messageId messageId to check for duplicates, when this parameter isn't present the messageId is read from sessionKey messageId
 * @ff.parameter correlationId correlationId to check for duplicates, when this parameter isn't present the correlationId is read from sessionKey correlationId
 *
 * @author Jaco de Groot
 */
@ExcludeFromType(ITransactionalStorage.class)
public class MessageStoreSender extends JdbcTransactionalStorage<Serializable> implements ISenderWithParameters {
	static final String PARAM_MESSAGEID = "messageId";
	static final String PARAM_CORRELATION_ID = "correlationId";

	private final @Nonnull ParameterList paramList = new ParameterList();
	private @Getter String sessionKeys = null;

	public MessageStoreSender() {
		setOnlyStoreWhenMessageIdUnique(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		paramList.configure();
		setType(StorageType.MESSAGESTORAGE.getCode());
		if (StringUtils.isBlank(getSlotId())) {
			throw new ConfigurationException("[slotId] has to be configured");
		}
		super.configure();
	}

	@Override
	public boolean isSynchronous() {
		return false;
	}

	@Override
	public void addParameter(IParameter p) {
		paramList.add(p);
	}

	@Override
	public @Nonnull ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		ParameterValueList parameterValues = resolveParameterValues(message, session);

		// the messageId to be inserted can also be specified via the parameter messageId, but defaults to the messageId of the session
		String messageId = resolveParameter(PARAM_MESSAGEID, session.getMessageId(), parameterValues);

		// the correlationId to be inserted can also be specified via the parameter correlationId, but defaults to the correlationId of the session
		String correlationId = resolveParameter(PARAM_CORRELATION_ID, session.getCorrelationId(), parameterValues);

		Serializable messageToStore;
		if (StringUtils.isBlank(sessionKeys)) {
			messageToStore = message; // if no session keys are specified, message is stored without escaping, for compatibility with normal messagestore operation.
		} else {
			Map<String, Object> sessionValuesToStore = StringUtil.splitToStream(sessionKeys)
					.filter(session::containsKey)
					.map(key -> Map.entry(key, session.get(key)))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			MessageWrapper<Serializable> messageWrapper = new MessageWrapper<>(message, messageId, correlationId);
			messageWrapper.getContext().putAll(sessionValuesToStore);
			messageToStore = messageWrapper;
		}
		return new SenderResult(storeMessage(messageId, correlationId, TimeProvider.nowAsDate(), null, null, messageToStore));
	}

	/**
	 * Resolves the parameter values one of, or both, messageId and correlationId parameters are present, returns null otherwise.
	 */
	private ParameterValueList resolveParameterValues(Message message, PipeLineSession session) throws SenderException {
		if (!paramList.hasParameter(PARAM_MESSAGEID) && !paramList.hasParameter(PARAM_CORRELATION_ID)) {
			return new ParameterValueList();
		}

		try {
			return paramList.getValues(message, session);
		} catch (ParameterException e) {
			throw new SenderException("Could not resolve parameters", e);
		}
	}

	/**
	 * If the given parameterName is not in the parameterList, fall back to the default value. If it is, resolve the value
	 */
	private String resolveParameter(String parameterName, String defaultValue, ParameterValueList parameterValues) {
		ParameterValue parameterValue = parameterValues.get(parameterName);

		if (parameterValue == null) {
			return defaultValue;
		}

		return parameterValue.asStringValue();
	}

	/**
	 * Comma separated list of sessionKey's to be stored together with the message. Please note: corresponding {@link MessageStoreListener} must have the
	 * same value for this attribute.
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

	/**
	 * Set the slotId, an identifier to keep separate the messages inserted
	 * by different MessageStoreSenders.
	 * <br/>
	 * This field should be set.
	 *
	 * @param string The {@code slotID} value for this MessageStoreSender.
	 */
	@Mandatory
	@Override
	public void setSlotId(String string) {
		super.setSlotId(string);
	}
}
