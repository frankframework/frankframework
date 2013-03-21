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
/*
 * $Log: JmsQueueBrowserIterator.java,v $
 * Revision 1.6  2011-11-30 13:51:51  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2009/12/23 17:09:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * @version $Id$
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
