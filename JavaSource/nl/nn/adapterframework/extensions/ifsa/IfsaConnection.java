/*
 * $Log: IfsaConnection.java,v $
 * Revision 1.6  2005-10-20 15:34:09  europe\L190409
 * renamed JmsConnection into ConnectionBase
 *
 * Revision 1.5  2005/10/18 07:04:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * better handling of dynamic reply queues
 *
 * Revision 1.4  2005/08/31 16:29:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected code for static reply queues
 *
 * Revision 1.3  2005/07/19 12:34:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version-string
 *
 * Revision 1.2  2005/07/19 12:33:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implements IXAEnabled 
 * polishing of serviceIds, to work around problems with ':' and '/'
 *
 * Revision 1.1  2005/05/03 15:58:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework of shared connection code
 *
 * Revision 1.2  2005/04/26 15:16:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed most bugs
 *
 * Revision 1.1  2005/04/26 09:36:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IfsaApplicationConnection
 */
package nl.nn.adapterframework.extensions.ifsa;

import java.util.HashMap;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TemporaryQueue;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.jms.ConnectionBase;

import com.ing.ifsa.IFSAContext;
import com.ing.ifsa.IFSAQueue;
import com.ing.ifsa.IFSAQueueConnectionFactory;

/**
 * Wrapper around Application oriented IFSA connection objects.
 * 
 * IFSA related IBIS objects can obtain an connection from this class. The physical connection is shared
 * between all IBIS objects that have the same ApplicationID.
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class IfsaConnection extends ConnectionBase {
	public static final String version="$RCSfile: IfsaConnection.java,v $ $Revision: 1.6 $ $Date: 2005-10-20 15:34:09 $";

	protected boolean preJms22Api=false;
	protected boolean useSingleDynamicReplyQueue=false;
	private Queue replyQueue=null;

	public IfsaConnection(String applicationId, IFSAContext context, IFSAQueueConnectionFactory connectionFactory, HashMap connectionMap, boolean preJms22Api) {
		super(applicationId,context,connectionFactory,connectionMap);
		this.preJms22Api=preJms22Api;
		log.debug("created new IfsaConnection for ["+applicationId+"] context ["+context+"] connectionfactory ["+connectionFactory+"]");
	}

	public boolean close() throws IbisException {
		boolean didClose = super.close();
		if (didClose) {
			replyQueue=null;
		}
		return didClose;
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
	
	protected Queue getDynamicReplyQueue(QueueSession session) throws JMSException {
		Queue result;
		if (useSingleDynamicReplyQueue) {
			if (replyQueue==null) {
				synchronized (this) {
					if (replyQueue==null) {
						replyQueue=session.createTemporaryQueue();
						log.info("created dynamic replyQueue ["+replyQueue.getQueueName()+"]");
					}
				}
			}
			result = replyQueue;
		} else {
			result = session.createTemporaryQueue();
		}
		return result;
	}

	protected void releaseDynamicReplyQueue(Queue replyQueue) throws IfsaException {
		if (replyQueue!=null) {
			try {
				if (!useSingleDynamicReplyQueue) {
					if (!(replyQueue instanceof TemporaryQueue)) {
						throw new IfsaException("DynamicReplyQueue ["+replyQueue.getQueueName()+"] is not a TemporaryQueue");
					}
					TemporaryQueue queue = (TemporaryQueue)replyQueue;
					queue.delete();
				}
			} catch (JMSException e) {
				throw new IfsaException("cannot close dynamic reply queue",e);
			}
		}
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
			releaseDynamicReplyQueue(replyQueue);		
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
			if (hasDynamicReplyQueue()) {
				queueReceiver = session.createReceiver(replyQueue);
				log.debug("using dynamic reply queue" );
			} else {
				String selector="JMSCorrelationID='" + correlationId + "'";
				queueReceiver = session.createReceiver(replyQueue, selector);
				log.debug("Using static reply queue - selector ["+selector+"]");
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
				if (replyQueue!=null) {
					releaseClientReplyQueue(replyQueue);
				}
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

}
