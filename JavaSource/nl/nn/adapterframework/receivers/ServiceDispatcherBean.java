package nl.nn.adapterframework.receivers;

/**
 * Wrapper around the {@link ServiceDispatcher} to work around
 * the problem that the <code>ServiceDispatcher</code> is a singleton
 * and cannot be instantiated.
 * @version Id
 * @author Johan Verrips IOS
 */
public class ServiceDispatcherBean {
	public static final String version="$Id: ServiceDispatcherBean.java,v 1.4 2004-03-26 10:43:03 NNVZNL01#L180564 Exp $";
	
/**
 * ServiceDispatcherBean constructor comment.
 */
public ServiceDispatcherBean() {
	super();
}
	public static String dispatchRequest(String serviceName, String request) {

		return ServiceDispatcher.getInstance().dispatchRequest(serviceName, request);
	}
	public static String dispatchRequest(String serviceName, String correlationID, String request) {

		return ServiceDispatcher.getInstance().dispatchRequest(serviceName, correlationID, request);
	}
}
