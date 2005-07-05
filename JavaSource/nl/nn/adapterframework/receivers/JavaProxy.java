/*
 * $Log: JavaProxy.java,v $
 * Revision 1.5  2005-07-05 13:18:28  europe\L190409
 * allow for ServiceClient2 extensions
 *
 * Revision 1.4  2004/08/23 07:38:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed JavaPusher to JavaListener
 *
 * Revision 1.3  2004/08/13 06:47:26  unknown <unknown@ibissource.org>
 * Allow usage of JavaPusher without JNDI
 *
 * Revision 1.2  2004/08/12 10:58:43  unknown <unknown@ibissource.org>
 * Replaced JavaReceiver by the JavaPusher that is to be used in a GenericPushingReceiver
 *
 * Revision 1.1  2004/04/26 06:21:38  unknown <unknown@ibissource.org>
 * Add java receiver
 *
 */
package nl.nn.adapterframework.receivers;

import java.io.Serializable;
import java.util.HashMap;

import nl.nn.adapterframework.core.ListenerException;


/**
 *
 * The JavaProxy enables the usage of Ibis directly from java. The serviceName
 * property must equal the name of the JavaListener in the Ibis configuration file.
 * 
 * If you package the ibis.jar in the .ear and not in your .war then you must
 * set the WAR class loader policy of the server in which the .ear is deployed to
 * application.
 *
 * @author J. Dekker
 * @version Id
 * @since 4.2
 */
public class JavaProxy implements ServiceClient2, Serializable {
	public static final String version="$RCSfile: JavaProxy.java,v $ $Revision: 1.5 $ $Date: 2005-07-05 13:18:28 $";
	private String serviceName;
	private boolean usesListener;
	
	/**
	 * @param serviceName
	 * @deprecated
	 */
	public JavaProxy(String serviceName) {
		this.serviceName = serviceName;
		this.usesListener = false;
	}

	/**
	 * @param pusher
	 */
	public JavaProxy(JavaListener listener) {
		this.serviceName = listener.getName();
		this.usesListener = true;
	}
	
	/* 
	 * @see nl.nn.adapterframework.receivers.ServiceClient#processRequest(java.lang.String, java.lang.String)
	 */
	public String processRequest(String correlationId, String message) {
		if (usesListener)
			return JavaListener.getListener(getServiceName()).processRequest(correlationId, message);
		return ServiceDispatcher.getInstance().dispatchRequest(serviceName, correlationId, message);
	}

	/* 
	 * @see nl.nn.adapterframework.receivers.ServiceClient#processRequest(java.lang.String)
	 */
	public String processRequest(String message) {
		if (usesListener)
			return JavaListener.getListener(getServiceName()).processRequest(message);
		return ServiceDispatcher.getInstance().dispatchRequest(serviceName, message);
	}

	public String processRequestWithExceptions(String correlationId, String message, HashMap requestContext) throws ListenerException {
		if (usesListener)
			return JavaListener.getListener(getServiceName()).processRequest(message);
		return ServiceDispatcher.getInstance().dispatchRequestWithExceptions(serviceName, correlationId, message, requestContext);
	}


	/**
	 * @return name of the service under which the JavaReceiver is registered
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * @param serviceName under which the JavaReceiver is registered
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * @param serviceName
	 * @return JavaProxy for a JavaPusher if registered under name or null
	 */
	public static JavaProxy getProxy(String serviceName) {
		JavaListener pusher = JavaListener.getListener(serviceName);
		if (pusher == null)
			return null;
		return new JavaProxy(pusher);
	}

}
