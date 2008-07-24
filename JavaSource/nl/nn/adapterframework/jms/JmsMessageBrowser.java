/*
 * $Log: JmsMessageBrowser.java,v $
 * Revision 1.7  2008-07-24 12:18:36  europe\L190409
 * added messageCount
 *
 * Revision 1.6  2007/10/10 08:32:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * additional selector specifications possible
 *
 * Revision 1.5.4.1  2007/09/21 13:23:34  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Add method to ITransactionalStorage to check if original message ID can be found in it
 * * Check for presence of original message id in ErrorStorage before processing, so it can be removed from queue if it has already once been recorded as unprocessable (but the TX in which it ran could no longer be committed).
 *
 * Revision 1.5  2005/12/20 16:59:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented support for connection-pooling
 *
 * Revision 1.4  2005/07/28 07:36:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	public static final String version = "$RCSfile: JmsMessageBrowser.java,v $ $Revision: 1.7 $ $Date: 2008-07-24 12:18:36 $";

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
		try {
			return new JmsQueueBrowserIterator(this,(Queue)getDestination(),getSelector());
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	public int getMessageCount() throws ListenerException {
		QueueBrowser queueBrowser=null;
		QueueSession session = null;
		try {
			session = (QueueSession)(createSession());
			if (StringUtils.isEmpty(getSelector())) {
				queueBrowser=session.createBrowser((Queue)getDestination());
			} else {
				queueBrowser=session.createBrowser((Queue)getDestination(), getSelector());
			}
			int count=0;
			for (Enumeration enum=queueBrowser.getEnumeration();enum.hasMoreElements();enum.nextElement()) {
				count++;
			}
			return count;
		} catch (Exception e) {
			throw new ListenerException("cannot determin messagecount",e);
		} finally {
			try {
				if (queueBrowser!=null) {
					queueBrowser.close();
				}
			} catch (JMSException e) {
				throw new ListenerException("error closing queuebrowser",e);
			}
			closeSession(session);
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
			closeSession(session);
		}
	}

	public Object browseMessage(String messageId) throws ListenerException {
		return browseMessage("JMSMessageID", messageId);
	}
	
    
    protected Object browseMessage(Map selectors) throws ListenerException {
		QueueSession session=null;
		Object msg = null;
		QueueBrowser queueBrowser=null;
		try {
			session = (QueueSession)createSession();
			queueBrowser = session.createBrowser((Queue)getDestination(),getCombinedSelector(selectors));
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
			closeSession(session);
		}
    }
    
    protected Object browseMessage(String selectorKey, String selectorValue) throws ListenerException {
        Map selectorMap = new HashMap();
        selectorMap.put(selectorKey, selectorValue);
        return browseMessage(selectorMap);
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
			closeSession(session);
		}
	}

	protected String getCombinedSelector(String messageId) {
        Map selectorMap = new HashMap();
        selectorMap.put("JMSMessageID", messageId);
        return getCombinedSelector(selectorMap);
	}

	protected String getCombinedSelector(Map selectors) {
        StringBuffer result = new StringBuffer();
        for (Iterator it = selectors.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            if (result.length() > 0) {
                result.append(" AND ");
            }
            result.append(entry.getKey()).append("='").
                    append(entry.getValue()).append("'");
        }

		if (StringUtils.isNotEmpty(getSelector())) {
			result.append(" AND ").append(getSelector());
		}
		return result.toString();
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

