/*
 * $Log: IMessagePusher.java,v $
 * Revision 1.1  2004-06-22 11:52:44  L190409
 * first version
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.receivers.ServiceClient;

import java.util.HashMap;
/**
 * Defines listening behaviour of message driven receivers.
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public interface IMessagePusher {
	public static final String version="$Id: IMessagePusher.java,v 1.1 2004-06-22 11:52:44 L190409 Exp $";

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
 * Set the handler that will do the processing of the message.
 * Each of the received messages must be pushed through handler.processMessage()
 */
void setHandler(ServiceClient handler);

}
