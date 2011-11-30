/*
 * $Log: JmsMessageBrowserIteratorItem.java,v $
 * Revision 1.4  2011-11-30 13:51:51  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2010/01/04 15:05:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added label
 *
 * Revision 1.1  2009/12/23 17:09:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified MessageBrowsing interface to reenable and improve export of messages
 *
 */
package nl.nn.adapterframework.jms;

import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;

import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;

public class JmsMessageBrowserIteratorItem implements IMessageBrowsingIteratorItem {

	private Message msg;
	
	public JmsMessageBrowserIteratorItem(Message msg) {
		super();
		this.msg=msg;
	}

	public String getId() throws ListenerException {
		try {
			return msg.getJMSMessageID();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	public String getOriginalId() throws ListenerException {
		try {
			return msg.getStringProperty(JmsTransactionalStorage.FIELD_ORIGINAL_ID);
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	public String getCorrelationId() throws ListenerException {
		try {
			return msg.getJMSCorrelationID();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	public Date getInsertDate() throws ListenerException {
		try {
			Date date=new Date(msg.getLongProperty(JmsTransactionalStorage.FIELD_RECEIVED_DATE));
			if (date!=null) {
				return date;
			}
			return new Date(msg.getJMSTimestamp());
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	public Date getExpiryDate() throws ListenerException {
		try {
			return new Date(msg.getJMSExpiration());
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	public String getType() throws ListenerException {
		try {
			return msg.getStringProperty(JmsTransactionalStorage.FIELD_TYPE);
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	public String getHost() throws ListenerException {
		try {
			return msg.getStringProperty(JmsTransactionalStorage.FIELD_HOST);
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	public String getCommentString() throws ListenerException {
		try {
			return msg.getStringProperty(JmsTransactionalStorage.FIELD_COMMENTS);
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	public String getLabel() throws ListenerException {
		try {
			return msg.getStringProperty(JmsTransactionalStorage.FIELD_LABEL);
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	public void release() {
		// close never required, as message is serializable
	}


}
