package nl.nn.adapterframework.receivers;

/**
 * Wrapper around the {@link ServiceDispatcher} to work around
 * the problem that the <code>ServiceDispatcher</code> is a singleton
 * and cannot be instantiated.
 *
 * @author Johan Verrips IOS
 */
public class ServiceDispatcherBean {
	public static final String version="$Id: ServiceDispatcherBean.java,v 1.1 2004-02-04 08:36:20 a1909356#db2admin Exp $";
	
/**
 * ServiceDispatcherBean constructor comment.
 */
public ServiceDispatcherBean() {
	super();
}
	public static String dispatchRequest(String serviceName, String request) {

		return ServiceDispatcher.getInstance().dispatchRequest(serviceName, request);
	}
}
