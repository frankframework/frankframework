/*
   Copyright 2013 Nationale-Nederlanden

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

import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;

public class JmsMessageBrowserIteratorItem implements IMessageBrowsingIteratorItem {

	private Message msg;
	
	public JmsMessageBrowserIteratorItem(Message msg) {
		super();
		this.msg=msg;
	}

	@Override
	public String getId() throws ListenerException {
		try {
			return msg.getJMSMessageID();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public String getOriginalId() throws ListenerException {
		try {
			return msg.getStringProperty(JmsTransactionalStorage.FIELD_ORIGINAL_ID);
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public String getCorrelationId() throws ListenerException {
		try {
			return msg.getJMSCorrelationID();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public Date getInsertDate() throws ListenerException {
		try {
			if (msg.getObjectProperty(JmsTransactionalStorage.FIELD_RECEIVED_DATE)!=null) {
				return new Date(msg.getLongProperty(JmsTransactionalStorage.FIELD_RECEIVED_DATE));
			}
			return new Date(msg.getJMSTimestamp());
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public Date getExpiryDate() throws ListenerException {
		try {
			return new Date(msg.getJMSExpiration());
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public String getType() throws ListenerException {
		try {
			return msg.getStringProperty(JmsTransactionalStorage.FIELD_TYPE);
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public String getHost() throws ListenerException {
		try {
			return msg.getStringProperty(JmsTransactionalStorage.FIELD_HOST);
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public String getCommentString() throws ListenerException {
		try {
			return msg.getStringProperty(JmsTransactionalStorage.FIELD_COMMENTS);
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public String getLabel() throws ListenerException {
		try {
			return msg.getStringProperty(JmsTransactionalStorage.FIELD_LABEL);
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	public String getJMSMessageID() throws ListenerException {
		try {
			return msg.getJMSMessageID();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	public long getJMSTimestamp() throws ListenerException {
		try {
			return msg.getJMSTimestamp();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	public String getText() throws ListenerException {
		try {
			return ((TextMessage)msg).getText();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public void release() {
		// close never required, as message is serializable
	}


}
