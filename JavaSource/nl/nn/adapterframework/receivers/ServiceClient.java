/* 
 * $Log: ServiceClient.java,v $
 * Revision 1.4  2011-05-19 14:58:46  L190409
 * simplified into one single interace ServiceClient
 *
 */ 
package nl.nn.adapterframework.receivers;

import java.util.Map;

import nl.nn.adapterframework.core.ListenerException;

/**
 * The interface clients (users) of a service may use.
 */
public interface ServiceClient {
	
	public String processRequest(String correlationId, String message, Map requestContext) throws ListenerException;
}
