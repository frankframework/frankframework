package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * The <code>ISender</code> is responsible for sending a message to
 * some destination.
 * 
 * <p>$Id: ISender.java,v 1.2 2004-02-04 10:01:58 a1909356#db2admin Exp $</p>
 */
public interface ISender extends INamedObject {
		public static final String version="$Id: ISender.java,v 1.2 2004-02-04 10:01:58 a1909356#db2admin Exp $";

/**
 * Stop/close the sender and deallocate resources.
 */ 
public void close() throws SenderException;
/**
 * <code>configure()</code> is called once at startup of the framework in the configure method of the owner of this sender. 
 * Purpose of this method is to reduce creating connections to databases etc. in the {@link #sendMessage(String,String) sendMessage()} method.
 * As much as possible class-instantiating should take place in the
 * <code>configure()</code> or <code>open()</code> method, to improve performance.
 */ 
public void configure() throws ConfigurationException;
/**
 * When <code>true</code>, the result of sendMessage is the reply of the request.
 */
boolean isSynchronous();
/**
 * This method will be called to start the sender. After this
 * method is called the sendMessage method may be called
 */ 
public void open() throws SenderException;
/**
 * Send a message to some destination (as configured in the Sender
 * object). This method may only be called after the <code>configure() </code>
 * method is called.
 * <p>
 * If isSynchronous() returns true, then:
 * <ul>
 *   <li>the result represents the reply message;</li>
 *   <li>the correlationID may be ignored</li>
 *   <li>the return value, if not null, is the reply-message</li>
 *   <li>a {link TimeOutException} may be thrown if a timeout occurs waiting for a reply</li>
 * </ul>
 * <p>
 * Multiple objects may try to call this method at the same time, from different threads. 
 * Implementations of this method should therefore be thread-safe, or <code>synchronized</code>.
 */ 
public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException;
 /**
 * Set the functional name of this <code>sender</code>
 */
 public void setName(String name);
}
