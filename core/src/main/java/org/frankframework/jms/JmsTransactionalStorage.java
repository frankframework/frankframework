/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021, 2023 WeAreFrank!

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
package org.frankframework.jms;

import java.io.Serializable;
import java.util.Date;

import jakarta.jms.JMSException;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.core.ITransactionalStorage;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.doc.ReferTo;
import org.frankframework.receivers.RawMessageWrapper;

/**
 * Implements a message log (<code>JmsMessageLog</code>) or error store (<code>JmsErrorStorage</code>) that uses JMS technology.
 * <br/><br/>
 * <b>Message log:</b> A message log writes messages in persistent storage for logging purposes.
 * When a message log appears in a receiver, it also ensures that the same message is only processed
 * once, even if a related pushing listener receives the same message multiple times.
 * <br/><br/>
 * <b>Error store:</b> Appears in a receiver or sender pipe to store messages that could not be processed.
 * Storing a message in the error store is the last resort of the Frank!Framework. Many types of listeners and senders
 * offer a retry mechanism. Only if several tries have failed, then an optional transaction is not rolled
 * back and the message is stored in the error store. Users can retry messages in an error store using the Frank!Console. When
 * this is done, the message is processed in the same way as messages received from the original source.
 * <br/><br/>
 * How does a message log or error store see duplicate messages? The message log or error store
 * always appears in combination with a sender or listener. This sender or listener determines
 * a key based on the sent or received message. Messages with the same key are considered to
 * be the same.
 *
 * @author  Gerrit van Brakel
 * @since   4.1
 */
public class JmsTransactionalStorage<S extends Serializable> extends AbstractJmsMessageBrowser<S, ObjectMessage> implements ITransactionalStorage<S> {

	public static final String FIELD_TYPE="type";
	public static final String FIELD_ORIGINAL_ID="originalId";
	public static final String FIELD_RECEIVED_DATE="receivedDate";
	public static final String FIELD_COMMENTS="comments";
	public static final String FIELD_SLOTID="SlotId";
	public static final String FIELD_HOST="host";
	public static final String FIELD_LABEL="label";

	private String slotId=null;
	private String type=null;

	public JmsTransactionalStorage() {
		super();
		setTransacted(true);
		setPersistent(true);
		setDestinationType(DestinationType.QUEUE);
	}

	@Override
	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, S message) throws SenderException {
		Session session=null;
		try {
			session = createSession();
			ObjectMessage msg = session.createObjectMessage(message);
			msg.setStringProperty(FIELD_TYPE,getType());
			msg.setStringProperty(FIELD_ORIGINAL_ID,messageId);
			msg.setJMSCorrelationID(correlationId);
			msg.setLongProperty(FIELD_RECEIVED_DATE,receivedDate.getTime());
			msg.setStringProperty(FIELD_COMMENTS,comments);
			if (StringUtils.isNotEmpty(getSlotId())) {
				msg.setStringProperty(FIELD_SLOTID,getSlotId());
			}
			msg.setStringProperty(FIELD_LABEL,label);
			return send(session,getDestination(),msg);
		} catch (Exception e) {
			throw new SenderException(e);
		} finally {
			closeSession(session);
		}
	}

	@Override
	public boolean containsMessageId(String originalMessageId) throws ListenerException {
		Object msg = doBrowse(FIELD_ORIGINAL_ID, originalMessageId);
		return msg != null;
	}

	@Override
	public RawMessageWrapper<S> browseMessage(String storageKey) throws ListenerException {
		try {
			ObjectMessage msg=browseJmsMessage(storageKey);
			RawMessageWrapper<S> messageWrapper = new RawMessageWrapper<>((S)msg.getObject(), storageKey, null);
			messageWrapper.getContext().put(PipeLineSession.STORAGE_ID_KEY, storageKey);
			return messageWrapper;
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public RawMessageWrapper<S> getMessage(String storageKey) throws ListenerException {
		try {
			ObjectMessage msg=getJmsMessage(storageKey);
			RawMessageWrapper<S> messageWrapper = new RawMessageWrapper<>((S)msg.getObject(), storageKey, null);
			messageWrapper.getContext().put(PipeLineSession.STORAGE_ID_KEY, storageKey);
			return messageWrapper;
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public String getSelector() {
		if (StringUtils.isEmpty(getSlotId())) {
			return null;
		}
		return FIELD_SLOTID+"='"+getSlotId()+"'";
	}

	@Override
	@ReferTo(ITransactionalStorage.class)
	public void setSlotId(String string) {
		slotId = string;
	}

	@Override
	public String getSlotId() {
		return slotId;
	}

	@Override
	@ReferTo(ITransactionalStorage.class)
	public void setType(String string) {
		type = string;
	}

	@Override
	public String getType() {
		return type;
	}
}
