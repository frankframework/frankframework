/*
 * $Log: IPullingListener.java,v $
 * Revision 1.6  2004-08-03 13:10:56  L190409
 * moved afterMessageProcessed to IListener
 *
 * Revision 1.5  2004/07/15 07:38:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IListener as common root for Pulling and Pushing listeners
 *
 * Revision 1.4  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:45  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;

import java.util.HashMap;
/**
 * Defines listening behaviour of pulling receivers.
 * Pulling receivers are receivers that poll for a message, as opposed to pushing receivers
 * that are 'message driven'
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public interface IPullingListener extends IListener {
		public static final String version="$Id: IPullingListener.java,v 1.6 2004-08-03 13:10:56 L190409 Exp $";

/**
 * Prepares a thread for receiving messages.
 * Called once for each thread that will listen for messages.
 * @return the threadContext for this thread. The threadContext is a HashMap in which
 * thread-specific data can be stored. 
 */
HashMap openThread() throws ListenerException;

/**
 * Finalizes a message receiving thread.
 * Called once for each thread that listens for messages, just before
 * {@link #close()} is called.
 */
void closeThread(HashMap threadContext) throws ListenerException;


/**
 * Retrieves messages from queue or other channel, but does no processing on it.
 * Multiple objects may try to call this method at the same time, from different threads. 
 * Implementations of this method should therefore be thread-safe, or <code>synchronized</code>.
 * <p>Any thread-specific properties should be stored in and retrieved from the threadContext.
 */
Object getRawMessage(HashMap threadContext) throws ListenerException;

}
