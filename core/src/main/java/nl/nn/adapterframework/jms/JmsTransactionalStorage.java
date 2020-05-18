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
import nl.nn.adapterframework.doc.IbisDoc;

/**
 * JMS implementation of <code>ITransactionalStorage</code>.
 * 
 * @author  Gerrit van Brakel
 * @since   4.1
 */
public class JmsTransactionalStorage<M extends Serializable> extends JmsMessageBrowser<M, ObjectMessage> implements ITransactionalStorage<M> {

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
	private String hideRegex = null;
	private String hideMethod = "all";

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
	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, M message) throws SenderException {
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
	public boolean containsCorrelationId(String correlationId) throws ListenerException {
		log.warn("could not determine correct presence of a message with correlationId [" + correlationId + "], assuming it doesnot exist");
		// TODO: check presence of a message with correlationId
		return false;
	}

	@Override
	public M browseMessage(String messageId) throws ListenerException {
		try {
			ObjectMessage msg=browseJmsMessage(messageId);
			return(M)msg.getObject();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public M getMessage(String messageId) throws ListenerException {
		try {
			ObjectMessage msg=getJmsMessage(messageId);
		return (M)msg.getObject();
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
	@IbisDoc({"Optional identifier for this storage, to be able to share the physical storage between a number of receivers", ""})
	public void setSlotId(String string) {
		slotId = string;
	}
	@Override
	public String getSlotId() {
		return slotId;
	}

	@Override
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
	
	@Override
	@IbisDoc({"Regular expression to mask strings in the errorStore/logStore. Every character between to the strings in this expression will be replaced by a '*'. For example, the regular expression (?&lt;=&lt;party&gt;).*?(?=&lt;/party&gt;) will replace every character between keys<party> and </party> ", ""})
	public void setHideRegex(String hideRegex) {
		this.hideRegex = hideRegex;
	}
	@Override
	public String getHideRegex() {
		return hideRegex;
	}

	@Override
	@IbisDoc({"(Only used when hideRegex is not empty) either <code>all</code> or <code>firstHalf</code>. When <code>firstHalf</code> only the first half of the string is masked, otherwise (<code>all</code>) the entire string is masked", "all"})
	public void setHideMethod(String hideMethod) {
		this.hideMethod = hideMethod;
	}
	@Override
	public String getHideMethod() {
		return hideMethod;
	}

}
