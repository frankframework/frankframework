/*
 * $Log: ISender.java,v $
 * Revision 1.7  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:57:24  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2004/03/31 12:04:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.4  2004/03/30 07:29:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:45  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * The <code>ISender</code> is responsible for sending a message to
 * some destination.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 */
public interface ISender extends INamedObject {
/**
 * <code>configure()</code> is called once at startup of the framework in the configure method of the owner of this sender. 
 * Purpose of this method is to reduce creating connections to databases etc. in the {@link #sendMessage(String,String) sendMessage()} method.
 * As much as possible class-instantiating should take place in the
 * <code>configure()</code> or <code>open()</code> method, to improve performance.
 */ 
public void configure() throws ConfigurationException;

/**
 * This method will be called to start the sender. After this
 * method is called the sendMessage method may be called
 */ 
public void open() throws SenderException;

/**
 * Stop/close the sender and deallocate resources.
 */ 
public void close() throws SenderException;

/**
 * When <code>true</code>, the result of sendMessage is the reply of the request.
 */
boolean isSynchronous();

/**
 * Send a message to some destination (as configured in the Sender
 * object). This method may only be called after the <code>configure() </code>
 * method is called.
 * <p>
 * For synchronous senders ({@link #isSynchronous()} returns <code>true</code>:
 * 
 * The following table shows the difference between synchronous and a-synchronous senders:
 * <table border="1">
 * <tr><th>@nbsp;</th><th>synchronous</th><th>a-synchronous</th></tr>
 * <tr><td>{@link #isSynchronous()} returns</td><td><code>true</code></td><td><code>false</code></td></tr>
 * <tr><td>return value of <code>sendMessage()</code> is</td><td>the reply-message</td><td>the messageId of the message sent</td></tr>
 * <tr><td>the correlationID specified with <code>sendMessage()</code></td><td>may be ignored</td><td>is sent with the message</td></tr>
 * <tr><td>a {link TimeOutException}</td><td>may be thrown if a timeout occurs waiting for a reply</td><td>should not be expected</td></tr>
 * </table>
 * <p>
 * Multiple objects may try to call this method at the same time, from different threads. 
 * Implementations of this method should therefore be thread-safe, or <code>synchronized</code>.
 */ 
public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException;
}
