/*
 * $Log: IListener.java,v $
 * Revision 1.1  2004-07-15 07:38:22  L190409
 * introduction of IListener as common root for Pulling and Pushing listeners
 *
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.receivers.ServiceClient;

import java.util.HashMap;

/**
 * Base-interface for IPullingListener and IMessagePusher
 * 
 * @version Id
 * @author Gerrit van Brakel
 * @since 4.2
 */
public interface IListener extends INamedObject {
	public static final String version="$Id: IListener.java,v 1.1 2004-07-15 07:38:22 L190409 Exp $";

/**
 * <code>configure()</code> is called once at startup of the framework in the configure method of the owner of this listener. 
 * Purpose of this method is to reduce creating connections to databases etc. in the {@link #getRawMessage(HashMap) getRawMessage()} method.
 * As much as possible class-instantiating should take place in the
 * <code>configure()</code> or <code>open()</code> method, to improve performance.
 */ 
public void configure() throws ConfigurationException;

/**
 * Prepares the listener for receiving messages.
 * <code>open()</code> is called once each time the listener is started.
 */
void open() throws ListenerException;

/**
 * Close all resources used for listening.
 * Called once once each time the listener is stopped.
 */
void close() throws ListenerException;

/**
 * Extracts ID-string from message obtained from {@link #getRawMessage(HashMap)}. May also extract
 * other parameters from the message and put those in the threadContext.
 * @return ID-string of message for adapter.
 */
String getIdFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException;

/**
 * Extracts string from message obtained from {@link #getRawMessage(HashMap)}. May also extract
 * other parameters from the message and put those in the threadContext.
 * @return input message for adapter.
 */
String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException;

}
