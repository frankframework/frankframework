/*
 * $Log: ServiceDispatcherBean.java,v $
 * Revision 1.8  2011-11-30 13:51:54  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2011/06/20 13:23:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reintroduced ServiceDispatcherBean, added warning
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.ListenerException;

/**
 * Wrapper around the {@link ServiceDispatcher} to work around
 * the problem that the <code>ServiceDispatcher</code> is a singleton
 * and cannot be instantiated.
 * N.B. This class is used by the deprecated old-style webservices, using ServiceDispatcher_ServiceProxy.
 * Please consider using a call using serviceNamespaceURI instead.
 * 
 * @author Johan Verrips IOS
 * @version Id
 */
public class ServiceDispatcherBean {
	
	/**
	 * ServiceDispatcherBean constructor comment.
	 */
	public ServiceDispatcherBean() {
		super();
	}
	
	public static String dispatchRequest(String serviceName, String request) {

		try {
			return ServiceDispatcher.getInstance().dispatchRequest(serviceName, null, request, null);
		} catch (ListenerException e) {
			return e.getMessage();
		}
	}
	
	public static String dispatchRequest(String serviceName, String correlationID, String request) {

		try {
			return ServiceDispatcher.getInstance().dispatchRequest(serviceName, correlationID, request, null);
		} catch (ListenerException e) {
			return e.getMessage();
		}
	}
}
