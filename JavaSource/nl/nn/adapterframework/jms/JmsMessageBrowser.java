/*
 * $Log: JmsMessageBrowser.java,v $
 * Revision 1.1  2004-06-16 12:25:52  NNVZNL01#L180564
 * Initial version of Queue browsing functionality
 *
 *
 */
package nl.nn.adapterframework.jms;

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;
import javax.jms.Session;

import javax.naming.NamingException;

import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.MessageBrowseException;

/**
 * Get the messages on a queue without deleting them
 * @version Id
 * @author Johan Verrips
 * @see nl.nn.adapterframework.webcontrol.action.BrowseQueue
 */
public class JmsMessageBrowser extends JMSFacade implements  IMessageBrowser{
	public static final String version = "$Id: JmsMessageBrowser.java,v 1.1 2004-06-16 12:25:52 NNVZNL01#L180564 Exp $";
	QueueSession session;
	
	public JmsMessageBrowser() {
		super();
	}
	/**
	 * Implement the IMessageBrowser 
	 * @see nl.nn.adapterframework.core.IMessageBrowser#getMessageEnumeration()
	 * @since 4.1.1
	 */
	public Enumeration getMessageEnumeration() throws MessageBrowseException {
		Enumeration result = null;
		if (getDestinationType().equalsIgnoreCase("TOPIC"))
			throw new MessageBrowseException("Topics are currently not supported for browsing");
		try {
			session=getQueueBrowserSession();
			 
			QueueBrowser queueBrowser = session.createBrowser((Queue) getDestination());
			
			result = queueBrowser.getEnumeration();
		}
		catch (JMSException ex) {
			throw new MessageBrowseException(ex);
		}
		catch (NamingException ne) {
			throw new MessageBrowseException(ne);
		} 
		catch (Exception exe){
			throw new MessageBrowseException(exe);
		}
		return result; 
	}
	public void close(){
		try {
			log.debug("closing browser");
			session.close();
			super.close();
			log.debug("closed browser");
		} catch(Exception e){
			log.error("Ignoring error on closing browser:", e);
		}

		
	}



}

