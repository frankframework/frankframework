/* 
 * $Log: ServiceClient.java,v $
 * Revision 1.6  2011-11-30 13:51:54  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2011/05/19 14:58:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
