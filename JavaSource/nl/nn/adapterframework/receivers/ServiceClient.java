package nl.nn.adapterframework.receivers;

/**
 * The interface clients (users) of a service may use.
 *
 * Creation date: (14-03-2003 16:07:46)
 * @author Johan Verrips IOS
 */
public interface ServiceClient {
	public static final String version="$Id: ServiceClient.java,v 1.1 2004-02-04 08:36:20 a1909356#db2admin Exp $";
	
	public String processRequest(String message);
/**
 * Does a processRequest with a correlationId from the client. This is usefull for logging purposes,
 * as the correlationId is logged also.
 * @since 4.0
 */	
public String processRequest(String correlationId, String message);
}
