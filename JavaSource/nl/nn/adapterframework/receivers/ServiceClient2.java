/*
 * $Log: ServiceClient2.java,v $
 * Revision 1.1  2005-07-05 13:05:46  europe\L190409
 * extension to ServiceClient
 *
 */
package nl.nn.adapterframework.receivers;

import java.util.HashMap;

import nl.nn.adapterframework.core.ListenerException;

/**
 * extension to serviceClient, that allows for a requestContext to be propagated, and to result in an exception.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public interface ServiceClient2 extends ServiceClient {
	public static final String version="$RCSfile: ServiceClient2.java,v $ $Revision: 1.1 $ $Date: 2005-07-05 13:05:46 $";
	
	public String processRequestWithExceptions(String correlationId, String message, HashMap requestContext) throws ListenerException;
}
