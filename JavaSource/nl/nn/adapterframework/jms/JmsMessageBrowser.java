/*
 * $Log: JmsMessageBrowser.java,v $
 * Revision 1.4  2005-07-28 07:36:57  europe\L190409
 * added selector
 *
 * Revision 1.3  2005/07/19 15:12:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * adapted to an implementation extending IMessageBrowser
 *
 * Revision 1.2  2004/10/05 10:41:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.1  2004/06/16 12:25:52  Johan Verrips <johan.verrips@ibissource.org>
 * Initial version of Queue browsing functionality
 *
 *
 */
package nl.nn.adapterframework.jms;

import java.util.Date;
import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.ListenerException;

/**
 * Get the messages on a queue without deleting them
 * @version Id
 * @author  Johan Verrips / Gerrit van Brakel
 * @see nl.nn.adapterframework.webcontrol.action.BrowseQueue
 */
public class JmsMessageBrowser extends JMSFacade implements IMessageBrowser {
	public static final String version = "$RCSfile: JmsMessageBrowser.java,v $ $Revision: 1.4 $ $Date: 2005-07-28 07:36:57 $";

	private long timeOut = 3000;
	private String selector=null;
	
	public JmsMessageBrowser() {
		super();
		setTransacted(true);
	}

	public JmsMessageBrowser(String selector) {
		this();
		this.selector=selector;
	}
	
	public IMessageBrowsingIterator getIterator() throws ListenerException {
		QueueSession session;
		try {
			session = (QueueSession) createSession();
			return new JmsQueueBrowserIterator(session,(Queue)getDestination(),getSelector());
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}
	
	public String getId(Object iteratorItem) throws ListenerException {
		Message msg = (Message)iteratorItem;
		try {
			return msg.getJMSMessageID();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	public String getOriginalId(Object iteratorItem) throws ListenerException {
		return getId(iteratorItem);
	}

	public String getCorrelationId(Object iteratorItem) throws ListenerException {
		Message msg = (Message)iteratorItem;
		try {
			return msg.getJMSCorrelationID();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	
	public Date getInsertDate(Object iteratorItem) throws ListenerException {
		Message msg = (Message)iteratorItem;
		try {
			return new Date(msg.getJMSTimestamp());
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	
	public String getCommentString(Object iteratorItem) throws ListenerException {
		Message msg = (Message)iteratorItem;
		try {
			return "correlationId="+msg.getJMSCorrelationID();
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}
	
	public Object getMessage(String messageId) throws ListenerException {
		Session session=null;
		Object msg = null;
		MessageConsumer mc = null;
		try {
			session = createSession();
			mc = getMessageConsumer(session, getDestination(), getCombinedSelector(messageId));
			msg = mc.receive(getTimeOut());
			return msg;
		} catch (Exception e) {
			throw new ListenerException(e);
		} finally {
			try {
				if (mc != null) {
					mc.close();
				}
			} catch (JMSException e1) {
				throw new ListenerException("exception closing message consumer",e1);
			}
			try {
				if (session != null) {
					session.close();
				}
			} catch (JMSException e1) {
				throw new ListenerException("exception closing session",e1);
			}
		}
	}

	public Object browseMessage(String messageId) throws ListenerException {
		QueueSession session=null;
		Object msg = null;
		QueueBrowser queueBrowser=null;
		try {
			session = (QueueSession)createSession();
			queueBrowser = session.createBrowser((Queue)getDestination(),getCombinedSelector(messageId));
			Enumeration msgenum = queueBrowser.getEnumeration();
			if (msgenum.hasMoreElements()) {
				msg=msgenum.nextElement();
			}
			return msg;
		} catch (Exception e) {
			throw new ListenerException(e);
		} finally {
			try {
				if (queueBrowser != null) {
					queueBrowser.close();
				}
			} catch (JMSException e1) {
				throw new ListenerException("exception closing queueBrowser",e1);
			}
			try {
				if (session != null) {
					session.close();
				}
			} catch (JMSException e1) {
				throw new ListenerException("exception closing session",e1);
			}
		}
	}

	
	public void deleteMessage(String messageId) throws ListenerException {
		Session session=null;
		MessageConsumer mc = null;
		try {
			session = createSession();
			log.debug("retrieving message ["+messageId+"] in order to delete it");
			mc = getMessageConsumer(session, getDestination(), getCombinedSelector(messageId));
			mc.receive(getTimeOut());
		} catch (Exception e) {
			throw new ListenerException(e);
		} finally {
			try {
				if (mc != null) {
					mc.close();
				}
			} catch (JMSException e1) {
				throw new ListenerException("exception closing message consumer",e1);
			}
			try {
				if (session != null) {
					session.close();
				}
			} catch (JMSException e1) {
				throw new ListenerException("exception closing session",e1);
			}
		}
	}

	protected String getCombinedSelector(String messageId) {
		String result = "JMSMessageID='" + messageId + "'";
		if (StringUtils.isNotEmpty(getSelector())) {
			result += " AND "+getSelector();
		}
		return result;
	}

	public void setTimeOut(long newTimeOut) {
		timeOut = newTimeOut;
	}
	public long getTimeOut() {
		return timeOut;
	}


	public String getSelector() {
		return selector;
	}

}

