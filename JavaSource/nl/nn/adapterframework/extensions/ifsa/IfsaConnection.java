/*
 * $Log: IfsaConnection.java,v $
 * Revision 1.3  2005-07-19 12:34:50  europe\L190409
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
import javax.naming.NamingException;

import nl.nn.adapterframework.jms.JmsConnection;

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
public class IfsaConnection extends JmsConnection {
	public static final String version="$RCSfile: IfsaConnection.java,v $ $Revision: 1.3 $ $Date: 2005-07-19 12:34:50 $";

	protected boolean preJms22Api=false;

	public IfsaConnection(String applicationId, IFSAContext context, IFSAQueueConnectionFactory connectionFactory, HashMap connectionMap, boolean preJms22Api) {
		super(applicationId,context,connectionFactory,connectionMap);
		this.preJms22Api=preJms22Api;
		log.debug("created new IfsaConnection for ["+applicationId+"] context ["+context+"] connectionfactory ["+connectionFactory+"]");
	}

	private boolean hasStaticReplyQueue() throws IfsaException {
		/*
		 * if we don't know if we're using a dynamic reply queue, we can
		 * check this using the function IsClientTransactional
		 * Yes -> we're using a static reply queue
		 * No -> dynamic reply queue
		 */
		return ((IFSAQueueConnectionFactory)getConnectionFactory()).IsClientTransactional();  // Static
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
			if (hasStaticReplyQueue()) { // Static
				replyQueue = (Queue) ((IFSAContext)getContext()).lookupReply(getId());
				log.debug("got static reply queue [" +replyQueue.getQueueName()+"]");            
			} else { // Temporary Dynamic
				replyQueue =  session.createTemporaryQueue();
				log.debug("got dynamic reply queue [" +replyQueue.getQueueName()+"]");
			}
			return replyQueue;
		} catch (Exception e) {
			throw new IfsaException(e);
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
			correlationId=sentMessage.getJMSCorrelationID();
			replyQueue=(Queue)sentMessage.getJMSReplyTo();
		} catch (JMSException e) {
			throw new IfsaException(e);
		}
		
		try {
	
			if (hasStaticReplyQueue()) {
				String selector="JMSCorrelationID='" + correlationId + "'";
				queueReceiver = session.createReceiver(replyQueue, selector);
				log.debug("** transactional client - selector ["+selector+"]");
			} else {
				queueReceiver = session.createReceiver(replyQueue);
				log.debug("** non-transactional client" );
			}	
		} catch (JMSException e) {
			throw new IfsaException(e);
		}
		return queueReceiver;
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
			String result = string.substring(0,lastFrom);
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
