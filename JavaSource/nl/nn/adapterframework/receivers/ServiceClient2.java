/*
 * $Log: ServiceClient2.java,v $
 * Revision 1.1.6.1  2007-09-18 11:20:38  europe\M00035F
 * * Update a number of method-signatures to take a java.util.Map instead of HashMap
 * * Rewrite JmsListener to be instance of IPushingListener; use Spring JMS Container
 *
 * Revision 1.1  2005/07/05 13:05:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * extension to ServiceClient
 *
 */
package nl.nn.adapterframework.receivers;

import java.util.Map;

import nl.nn.adapterframework.core.ListenerException;

/**
 * extension to serviceClient, that allows for a requestContext to be propagated, and to result in an exception.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public interface ServiceClient2 extends ServiceClient {
	public static final String version="$RCSfile: ServiceClient2.java,v $ $Revision: 1.1.6.1 $ $Date: 2007-09-18 11:20:38 $";
	
	public String processRequestWithExceptions(String correlationId, String message, Map requestContext) throws ListenerException;
}
