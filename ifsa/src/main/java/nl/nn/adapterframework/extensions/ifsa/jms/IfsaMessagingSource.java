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
package nl.nn.adapterframework.extensions.ifsa.jms;

import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.naming.NamingException;

import nl.nn.adapterframework.extensions.ifsa.IfsaException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.MessagingSource;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;

import com.ing.ifsa.IFSAContext;
import com.ing.ifsa.IFSAQueue;
import com.ing.ifsa.IFSAQueueConnectionFactory;

/**
 * {@link MessagingSource} for IFSA connections.
 *
 * IFSA related IBIS objects can obtain an connection from this class. The physical connection is shared
 * between all IBIS objects that have the same ApplicationID.
 *
 * @author Gerrit van Brakel
 */
public class IfsaMessagingSource extends MessagingSource {

	private static final String CLEANUP_ON_CLOSE_KEY="ifsa.cleanUpOnClose";
	private static Boolean cleanUpOnClose=null;

	private final boolean preJms22Api;
	private final boolean xaEnabled;

	public IfsaMessagingSource(String applicationId, IFSAContext context, IFSAQueueConnectionFactory connectionFactory, Map messagingSourceMap, boolean preJms22Api, boolean xaEnabled) {
		super(applicationId,context,connectionFactory,messagingSourceMap,null,false,true);
		this.preJms22Api=preJms22Api;
		this.xaEnabled=xaEnabled;
		log.debug("created new IfsaMessagingSource for ["+applicationId+"] context ["+context+"] connectionfactory ["+connectionFactory+"]");
	}


	public boolean hasDynamicReplyQueue() throws IfsaException {
		try {
			if (preJms22Api) {
				return !((IFSAQueueConnectionFactory) getConnectionFactory()).IsClientTransactional();
			} else {
				return ((IFSAContext) getContext()).hasDynamicReplyQueue();
			}
		} catch (NamingException e) {
			throw new IfsaException("could not find IfsaContext",e);
		}
	}

	public boolean canUseIfsaModeSessions() throws IfsaException {
		return hasDynamicReplyQueue() && !useSingleDynamicReplyQueue();
	}

	/**
	 * Retrieves the reply queue for a <b>client</b> connection. If the
	 * client is transactional the replyqueue is retrieved from IFSA,
	 * otherwise a temporary (dynamic) queue is created.
	 */
	public Queue getClientReplyQueue(QueueSession session) throws IfsaException {
		Queue replyQueue = null;

		try {
			/*
			 * if we don't know if we're using a dynamic reply queue, we can
			 * check this using the function IsClientTransactional
			 * Yes -> we're using a static reply queue
			 * No -> dynamic reply queue
			 */
			if (hasDynamicReplyQueue()) { // Temporary Dynamic
				replyQueue =  getDynamicReplyQueue(session);
				log.debug("got dynamic reply queue [" +replyQueue.getQueueName()+"]");
			} else { // Static
				replyQueue = (Queue) ((IFSAContext)getContext()).lookupReply(getId());
				log.debug("got static reply queue [" +replyQueue.getQueueName()+"]");
			}
			return replyQueue;
		} catch (Exception e) {
			throw new IfsaException(e);
		}
	}

	protected void releaseClientReplyQueue(Queue replyQueue) throws IfsaException {
		if (hasDynamicReplyQueue()) { // Temporary Dynamic
			try {
				releaseDynamicReplyQueue(replyQueue);
			} catch (JmsException e) {
				throw new IfsaException(e);
			}
		}
	}

	/**
	 * Gets the queueReceiver, by utilizing the <code>getInputQueue()</code> method.<br/>
	 * For serverside getQueueReceiver() the creating of the QueueReceiver is done
	 * without the <code>selector</code> information, as this is not allowed
	 * by IFSA.<br/>
	 * For a clientconnection, the receiver is done with the <code>getClientReplyQueue</code>
	 */
	public QueueReceiver getReplyReceiver(QueueSession session, Message sentMessage)
		throws IfsaException {

		QueueReceiver queueReceiver;

		String correlationId;
		Queue replyQueue;
		try {
			correlationId = sentMessage.getJMSMessageID(); // IFSA uses the messageId as correlationId
			replyQueue=(Queue)sentMessage.getJMSReplyTo();
		} catch (JMSException e) {
			throw new IfsaException(e);
		}

		try {
			if (hasDynamicReplyQueue() && !useSingleDynamicReplyQueue()) {
				queueReceiver = session.createReceiver(replyQueue);
				log.debug("created receiver on individual dynamic reply queue" );
			} else {
				String selector="JMSCorrelationID='" + correlationId + "'";
				queueReceiver = session.createReceiver(replyQueue, selector);
				log.debug("created receiver on static or shared-dynamic reply queue - selector ["+selector+"]");
			}
		} catch (JMSException e) {
			throw new IfsaException(e);
		}
		return queueReceiver;
	}

	public void closeReplyReceiver(QueueReceiver receiver) throws IfsaException {
		try {
			if (receiver!=null) {
				Queue replyQueue = receiver.getQueue();
				receiver.close();
				releaseClientReplyQueue(replyQueue);
			}
		} catch (JMSException e) {
			throw new IfsaException(e);
		}
	}

	public IFSAQueue lookupService(String serviceId) throws IfsaException {
		try {
			return (IFSAQueue) ((IFSAContext)getContext()).lookupService(serviceId);
		} catch (NamingException e) {
			throw new IfsaException("cannot lookup queue for service ["+serviceId+"]",e);
		}
	}

	public IFSAQueue lookupProviderInput() throws IfsaException {
		try {
			return (IFSAQueue) ((IFSAContext)getContext()).lookupProviderInput();
		} catch (NamingException e) {
			throw new IfsaException("cannot lookup provider queue",e);
		}
	}

	protected String replaceLast(String string, char from, char to) {
		int lastTo=string.lastIndexOf(to);
		int lastFrom=string.lastIndexOf(from);

		if (lastFrom>0 && lastTo<lastFrom) {
			String result = string.substring(0,lastFrom)+to+string.substring(lastFrom+1);
			log.info("replacing for Ifsa-compatibility ["+string+"] by ["+result+"]");
			return result;
		}
		return string;
	}

	public String polishServiceId(String serviceId) {
		if (preJms22Api) {
			return replaceLast(serviceId, '/',':');
		} else {
			return replaceLast(serviceId, ':','/');
		}
	}

	public synchronized boolean cleanUpOnClose() {
		if (cleanUpOnClose==null) {
			boolean cleanup=AppConstants.getInstance().getBoolean(CLEANUP_ON_CLOSE_KEY, true);
			cleanUpOnClose = new Boolean(cleanup);
		}
		return cleanUpOnClose.booleanValue();
	}

	protected ConnectionFactory getConnectionFactoryDelegate() throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return (QueueConnectionFactory)ClassUtils.getDeclaredFieldValue(getConnectionFactory(),"qcf");
	}

	public boolean xaCapabilityCanBeDetermined() {
		return !preJms22Api;
	}

	public boolean isXaEnabled() {
		return xaEnabled;
	}

	public boolean isXaEnabledForSure() {
		return xaCapabilityCanBeDetermined() && isXaEnabled();
	}

	public boolean isNotXaEnabledForSure() {
		return xaCapabilityCanBeDetermined() && !isXaEnabled();
	}

}
