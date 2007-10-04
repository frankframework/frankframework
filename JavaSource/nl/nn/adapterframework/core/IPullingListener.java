/*
 * $Log: IPullingListener.java,v $
 * Revision 1.7.6.3  2007-10-04 13:23:37  europe\L190409
 * synchronize with HEAD (4.7.0)
 *
 * Revision 1.8  2007/10/03 08:13:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.7  2004/09/08 14:15:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.6  2004/08/03 13:10:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import java.util.Map;
/**
 * Defines listening behaviour of pulling receivers.
 * Pulling receivers are receivers that poll for a message, as opposed to pushing receivers
 * that are 'message driven'
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public interface IPullingListener extends IListener {
	public static final String version = "$RCSfile: IPullingListener.java,v $ $Revision: 1.7.6.3 $ $Date: 2007-10-04 13:23:37 $";

	/**
	 * Prepares a thread for receiving messages.
	 * Called once for each thread that will listen for messages.
	 * @return the threadContext for this thread. The threadContext is a Map in which
	 * thread-specific data can be stored. 
	 */
	Map openThread() throws ListenerException;
	
	/**
	 * Finalizes a message receiving thread.
	 * Called once for each thread that listens for messages, just before
	 * {@link #close()} is called.
	 */
	void closeThread(Map threadContext) throws ListenerException;
	
	/**
	 * Retrieves messages from queue or other channel, but does no processing on it.
	 * Multiple objects may try to call this method at the same time, from different threads. 
	 * Implementations of this method should therefore be thread-safe, or <code>synchronized</code>.
	 * <p>Any thread-specific properties should be stored in and retrieved from the threadContext.
	 */
	Object getRawMessage(Map threadContext) throws ListenerException;

}
