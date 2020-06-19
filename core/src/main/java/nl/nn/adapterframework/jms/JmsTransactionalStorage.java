/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.jms;

import java.io.Serializable;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDocRef;

/**
 * JMS implementation of <code>ITransactionalStorage</code>.
 * 
 * @author  Gerrit van Brakel
 * @since   4.1
 */
public class JmsTransactionalStorage<S extends Serializable> extends JmsMessageBrowser<S, ObjectMessage> implements ITransactionalStorage<S> {

	public static final String FIELD_TYPE="type";
	public static final String FIELD_ORIGINAL_ID="originalId";
	public static final String FIELD_RECEIVED_DATE="receivedDate";
	public static final String FIELD_COMMENTS="comments";
	public static final String FIELD_SLOTID="SlotId";
	public static final String FIELD_HOST="host";
	public static final String FIELD_LABEL="label";

	private String slotId=null;
	private String type=null;
	private boolean active=true;   

	private final String ITRANSACTIONALSTORAGE = "nl.nn.adapterframework.core.ITransactionalStorage";

	public JmsTransactionalStorage() {
		super();
		setTransacted(true);
		setPersistent(true);
		setDestinationType("QUEUE");
	}

	@Override
	public void configure() throws ConfigurationException {
		// nothing special
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
	public S browseMessage(String messageId) throws ListenerException {
		try {
			ObjectMessage msg=browseJmsMessage(messageId);
			return(S)msg.getObject();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public S getMessage(String messageId) throws ListenerException {
		try {
			ObjectMessage msg=getJmsMessage(messageId);
		return (S)msg.getObject();
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
	@IbisDocRef({"1", ITRANSACTIONALSTORAGE})
	public void setSlotId(String string) {
		slotId = string;
	}
	@Override
	public String getSlotId() {
		return slotId;
	}

	@Override
	@IbisDocRef({"2", ITRANSACTIONALSTORAGE})
	public void setType(String string) {
		type = string;
	}
	@Override
	public String getType() {
		return type;
	}

	public void setActive(boolean b) {
		active = b;
	}
	@Override
	public boolean isActive() {
		return active;
	}
	
}
