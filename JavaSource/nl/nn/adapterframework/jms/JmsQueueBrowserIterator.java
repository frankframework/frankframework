/*
 * $Log: JmsQueueBrowserIterator.java,v $
 * Revision 1.2  2005-07-28 07:37:33  europe\L190409
 * added selector
 *
 * Revision 1.1  2005/07/19 15:12:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * adapted to an implementation extending IMessageBrowser
 *
 */
package nl.nn.adapterframework.jms;

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;
import javax.naming.NamingException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.ListenerException;

/**
 * Helper class for browsing queues.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class JmsQueueBrowserIterator implements IMessageBrowsingIterator {

	private QueueSession session;
	private QueueBrowser queueBrowser;
	private Enumeration  enum;
		
	public JmsQueueBrowserIterator(QueueSession session, Queue destination, String selector) throws JMSException, NamingException {
		this.session=session;
		if (StringUtils.isEmpty(selector)) {
			this.queueBrowser=session.createBrowser(destination);
		} else {
			this.queueBrowser=session.createBrowser(destination, selector);
		}
		this.enum=queueBrowser.getEnumeration(); 
	}

	public boolean hasNext() {
		return enum.hasMoreElements();
	}

	public Object next() {
		return enum.nextElement();
	}

	public void close() throws ListenerException {
		try {
			queueBrowser.close();
		} catch (JMSException e) {
			throw new ListenerException("error closing queuebrowser",e);
		}
		try {
			session.close();
		} catch (JMSException e) {
			throw new ListenerException("error closing browser session",e);
		}
	} 

}
