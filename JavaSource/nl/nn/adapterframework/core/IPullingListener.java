package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;

import java.util.HashMap;
/**
 * Defines listening behaviour of pulling receivers.
 * Pulling receivers are receivers that poll for a message, as opposed to pushing receivers
 * that are 'message driven'
 * 
 * <p>$Id: IPullingListener.java,v 1.2 2004-02-04 10:01:58 a1909356#db2admin Exp $</p>
 * 
 * @author Gerrit van Brakel
 */
public interface IPullingListener {
		public static final String version="$Id: IPullingListener.java,v 1.2 2004-02-04 10:01:58 a1909356#db2admin Exp $";

/**
 * Called to perform actions (like committing or sending a reply) after a message has been processed by the Pipeline. 
 */
void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap threadContext) throws ListenerException;
/**
 * Close all resources used for listening
 * Called once, after for each thread that listens for messages {@link #closeThread(HashMap)} is called.
 */
void close() throws ListenerException;
/**
 * Finalizes a message receiving thread.
 * Called once for each thread that listens for messages, just before
 * {@link #close()} is called.
 */
void closeThread(HashMap threadContext) throws ListenerException;
/**
 * <code>configure()</code> is called once at startup of the framework in the configure method of the owner of this listener. 
 * Purpose of this method is to reduce creating connections to databases etc. in the {@link #getRawMessage(HashMap) getRawMessage()} method.
 * As much as possible class-instantiating should take place in the
 * <code>configure()</code> or <code>open()</code> method, to improve performance.
 */ 
public void configure() throws ConfigurationException;
/**
 * Extracts ID-string from message obtained from {@link #getRawMessage(HashMap)}. May also extract
 * other parameters from the message and put those in the threadContext.
 * @return ID-string of message for adapter.
 */
String getIdFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException;
/**
 * Retrieves messages from queue or other channel, but does no processing on it.
 * Multiple objects may try to call this method at the same time, from different threads. 
 * Implementations of this method should therefore be thread-safe, or <code>synchronized</code>.
 * <p>Any thread-specific properties should be stored in and retrieved from the threadContext.
 */
Object getRawMessage(HashMap threadContext) throws ListenerException;
/**
 * Extracts string from message obtained from {@link #getRawMessage(HashMap)}. May also extract
 * other parameters from the message and put those in the threadContext.
 * @return input message for adapter.
 */
String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException;
/**
 * Prepares the listener for receiving messages.
 * <code>open()</code> is called once each time the listener is started.
 * After that, {@link #openThread()} is called for each thread that will listen for messages.
 */
void open() throws ListenerException;
/**
 * Prepares a thread for receiving messages.
 * Called once for each thread that will listen for messages.
 * @return the threadContext for this thread. The threadContext is a HashMap in which
 * thread-specific data can be stored. 
 */
HashMap openThread() throws ListenerException;
}
