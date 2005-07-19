/*
 * $Log: JmsTransactionalStorage.java,v $
 * Revision 1.3  2005-07-19 15:12:40  europe\L190409
 * adapted to an implementation extending IMessageBrowser
 *
 * Revision 1.2  2004/03/26 10:42:54  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.1  2004/03/23 18:02:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.jms;

import java.io.Serializable;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;

/**
 * JMS implementation of <code>ITransactionalStorage</code>.
 * 
 * @version Id
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class JmsTransactionalStorage extends JmsMessageBrowser implements ITransactionalStorage {
	public static final String version = "$RCSfile: JmsTransactionalStorage.java,v $ $Revision: 1.3 $ $Date: 2005-07-19 15:12:40 $";


	public JmsTransactionalStorage() {
		super();
		setTransacted(true);
		setPersistent(true);
		setDestinationType("QUEUE");
	}

	public void configure() throws ConfigurationException {
		// TODO Auto-generated method stub
	}
	
	public void open() throws ListenerException {
		try {
			super.open();
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}
	
	public void close() throws ListenerException {
		try {
			super.close();
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}
	
	

	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, Serializable message) throws SenderException {
		Session session=null;
		try {
			session = createSession();
			ObjectMessage msg = session.createObjectMessage(message);
			msg.setStringProperty("originalId",messageId);
			msg.setJMSCorrelationID(correlationId);
			msg.setLongProperty("receivedDate",receivedDate.getTime());
			msg.setStringProperty("comments",comments);
			return send(session,getDestination(),msg);
		} catch (Exception e) {
			throw new SenderException(e);
		} finally {
			try {
				if (session != null) {
					session.close();
				}
			} catch (JMSException e1) {
				log.error("exception closing after storing message",e1);
			}
		}
	}

	public Object browseMessage(String messageId) throws ListenerException {
		try {
			ObjectMessage msg=(ObjectMessage)super.browseMessage(messageId);
			return msg.getObject();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	public Object getMessage(String messageId) throws ListenerException {
		try {
			ObjectMessage msg=(ObjectMessage)super.getMessage(messageId);
		return msg.getObject();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}


	public String getOriginalId(Object iteratorItem) throws ListenerException {
		ObjectMessage msg = (ObjectMessage)iteratorItem;
		try {
			return msg.getStringProperty("originalId");
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
		
	public Date getInsertDate(Object iteratorItem) throws ListenerException {
		ObjectMessage msg = (ObjectMessage)iteratorItem;
		try {
			return new Date(msg.getLongProperty("receivedDate"));
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	public String getCommentString(Object iteratorItem) throws ListenerException {
		ObjectMessage msg = (ObjectMessage)iteratorItem;
		try {
			return msg.getStringProperty("comments");
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	
}
