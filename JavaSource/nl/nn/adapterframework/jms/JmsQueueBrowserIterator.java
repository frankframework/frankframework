/*
 * $Log: JmsQueueBrowserIterator.java,v $
 * Revision 1.4  2009-12-23 17:09:57  L190409
 * modified MessageBrowsing interface to reenable and improve export of messages
 *
 * Revision 1.3  2005/12/20 16:59:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented support for connection-pooling
 *
 * Revision 1.2  2005/07/28 07:37:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added selector
 *
 * Revision 1.1  2005/07/19 15:12:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * adapted to an implementation extending IMessageBrowser
 *
 */
package nl.nn.adapterframework.jms;

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;

import org.apache.commons.lang.StringUtils;

/**
 * Helper class for browsing queues.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class JmsQueueBrowserIterator implements IMessageBrowsingIterator {

	private final JMSFacade    facade;
	private final QueueSession session;
	private final QueueBrowser queueBrowser;
	private final Enumeration  enm;
		
	public JmsQueueBrowserIterator(JMSFacade facade, Queue destination, String selector) throws JMSException, NamingException, JmsException {
		this.facade=facade;
		this.session=(QueueSession)(facade.createSession());
		if (StringUtils.isEmpty(selector)) {
			this.queueBrowser=session.createBrowser(destination);
		} else {
			this.queueBrowser=session.createBrowser(destination, selector);
		}
		this.enm=queueBrowser.getEnumeration(); 
	}

	public boolean hasNext() {
		return enm.hasMoreElements();
	}

	public IMessageBrowsingIteratorItem next() {
		return new JmsMessageBrowserIteratorItem((Message)enm.nextElement());
	}

	public void close() throws ListenerException {
		try {
			queueBrowser.close();
		} catch (JMSException e) {
			throw new ListenerException("error closing queuebrowser",e);
		}
		facade.closeSession(session);
	} 

}
