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

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
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
 */
public class JmsQueueBrowserIterator implements IMessageBrowsingIterator {

	private final JMSFacade    facade;
	private final Session session;
	private final QueueBrowser queueBrowser;
	private final Enumeration  enm;
		
	public JmsQueueBrowserIterator(JMSFacade facade, Queue destination, String selector) throws JMSException, NamingException, JmsException {
		this.facade=facade;
		this.session=facade.createSession();
		if (StringUtils.isEmpty(selector)) {
			this.queueBrowser=session.createBrowser(destination);
		} else {
			this.queueBrowser=session.createBrowser(destination, selector);
		}
		this.enm=queueBrowser.getEnumeration(); 
	}

	@Override
	public boolean hasNext() {
		return enm.hasMoreElements();
	}

	@Override
	public IMessageBrowsingIteratorItem next() {
		return new JmsMessageBrowserIteratorItem((Message)enm.nextElement());
	}

	@Override
	public void close() throws ListenerException {
		try {
			queueBrowser.close();
		} catch (JMSException e) {
			throw new ListenerException("error closing queuebrowser",e);
		}
		facade.closeSession(session);
	} 

}
