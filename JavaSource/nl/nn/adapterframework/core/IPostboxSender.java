/*
 * $Log: IPostboxSender.java,v $
 * Revision 1.1  2004-05-21 07:59:30  a1909356#db2admin
 * Add (modifications) due to the postbox sender implementation
 *
 */
package nl.nn.adapterframework.core;

import java.util.ArrayList;

/**
 * The <code>IPostboxSender</code> is responsible for storing a message
 * in a postbox
 *
 *  @author John Dekker
 * @version Id
 */
public interface IPostboxSender extends ISender {
	/**
	 * Send a message to some destination (as configured in the Sender object). 
	 * This method may only be called after the <code>configure() </code> method is called.
	 * <p>
	 * For synchronous senders ({@link #isSynchronous()} returns <code>true</code>:
	 * 
	 * The following table shows the difference between synchronous and a-synchronous senders:
	 * <table border="1">
	 * <tr><th>@nbsp;</th><th>synchronous</th><th>a-synchronous</th></tr>
	 * <tr><td>{@link #isSynchronous()} returns</td><td><code>true</code></td><td><code>false</code></td></tr>
	 * <tr><td>return value of <code>sendMessage()</code> is</td><td>the reply-message</td><td>the messageId of the message sent</td></tr>
	 * <tr><td>the correlationID specified with <code>sendMessage()</code></td><td>may be ignored</td><td>is sent with the message</td></tr>
	 * <tr><td>the msgProperties specified with <code>sendMessage()</code></td><td>may be ignored</td><td>are set to the message just before the message is actually send</td></tr>
	 * <tr><td>a {link TimeOutException}</td><td>may be thrown if a timeout occurs waiting for a reply</td><td>should not be expected</td></tr>
	 * </table>
	 * <p>
	 * Multiple objects may try to call this method at the same time, from different threads. 
	 * Implementations of this method should therefore be thread-safe, or <code>synchronized</code>.
	 */ 
	String sendMessage(String correlationID, String message, ArrayList msgProperties) throws SenderException;
}
