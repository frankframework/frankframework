package nl.nn.adapterframework.receivers;

/**
 * The interface clients (users) of a service may use.
 *
 * @version Id
 * @author Johan Verrips IOS
 */
public interface ServiceClient {
	public static final String version="$Id: ServiceClient.java,v 1.3 2004-03-26 10:43:03 NNVZNL01#L180564 Exp $";
	
	public String processRequest(String message);
/**
 * Does a processRequest with a correlationId from the client. This is usefull for logging purposes,
 * as the correlationId is logged also.
 * @since 4.0
 */	
public String processRequest(String correlationId, String message);
}
