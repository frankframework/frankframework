/*
 * $Log :$
 */
package nl.nn.adapterframework.core;

import java.util.Map;

/**
 * The <code>IPostboxListener</code> is responsible for querying a message
 * from a postbox
 *
 * @author John Deker
 * @version Id
  */
public interface IPostboxListener extends IPullingListener {
	/**
	 * Retrieves the first message found from queue or other channel,  that matches the 
	 * specified message selector.
	 * This method may only be called after the <code>configure() </code> method is called.
	 * <p>
	 *
	 * @param messageSelector searcriteria for messages. Not that the format of the collector
	 * changes per listener, for example a JMSListener's messageSelector follows the JMS specification.
	 * @param threadContext context in which the method is called 
	 */ 
	Object retrieveRawMessage(String messageSelector, Map threadContext) throws ListenerException, TimeOutException;

}
