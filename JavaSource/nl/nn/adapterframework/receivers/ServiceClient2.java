/*
 * $Log: ServiceClient2.java,v $
 * Revision 1.1.6.2  2007-10-10 14:30:44  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/08 12:24:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
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
	public static final String version="$RCSfile: ServiceClient2.java,v $ $Revision: 1.1.6.2 $ $Date: 2007-10-10 14:30:44 $";
	
	public String processRequestWithExceptions(String correlationId, String message, Map requestContext) throws ListenerException;
}
