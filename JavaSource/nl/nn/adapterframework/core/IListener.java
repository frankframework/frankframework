/*
 * $Log: IListener.java,v $
 * Revision 1.9  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.7  2008/03/27 11:54:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * document common session variables
 *
 * Revision 1.6  2007/10/03 08:09:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.5  2005/07/19 12:18:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reformat + moved some functions from pushing and pulling listeners to here
 *
 * Revision 1.4  2004/09/08 14:15:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.3  2004/08/23 13:07:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.2  2004/08/03 13:09:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved afterMessageProcessed to IListener
 *
 * Revision 1.1  2004/07/15 07:38:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IListener as common root for Pulling and Pushing listeners
 *
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;

import java.util.Map;

/**
 * Base-interface for IPullingListener and IPushingListener.
 * 
 * @author  Gerrit van Brakel
 * @since   4.2
 * @version Id
 */
public interface IListener extends INamedObject {
	public static final String version = "$RCSfile: IListener.java,v $ $Revision: 1.9 $ $Date: 2011-11-30 13:51:55 $";

	/**
	 * <code>configure()</code> is called once at startup of the framework in the <code>configure()</code> method 
	 * of the owner of this listener. 
	 * Purpose of this method is to reduce creating connections to databases etc. in the {@link nl.nn.adapterframework.core.IPullingListener#getRawMessage(Map)} method.
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
	 * Extracts ID-string from message obtained from {@link nl.nn.adapterframework.core.IPullingListener#getRawMessage(Map)}. May also extract
	 * other parameters from the message and put those in the context.
	 * <br>
	 * Common entries in the session context are:
	 * <ul>
	 * 	<li>id: messageId, identifies the current transportation of the message</li>
	 * 	<li>cid: correlationId, identifies the processing of the message in the global chain</li>
	 * 	<li>tsReceived: timestamp of reception of the message, formatted as yyyy-MM-dd HH:mm:ss.SSS</li>
	 * 	<li>tsSent: timestamp of sending of the message (only when available), formatted as yyyy-MM-dd HH:mm:ss.SSS</li>
	 * </ul>
	 * 
	 * @return Correlation ID string.
	 */
	String getIdFromRawMessage(Object rawMessage, Map context) throws ListenerException;
	
	/**
	 * Extracts string from message obtained from {@link nl.nn.adapterframework.core.IPullingListener#getRawMessage(Map)}. May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return input message for adapter.
	 */
	String getStringFromRawMessage(Object rawMessage, Map context) throws ListenerException;
	
	/**
	 * Called to perform actions (like committing or sending a reply) after a message has been processed by the 
	 * Pipeline. 
	 */
	void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map context) throws ListenerException;

}
