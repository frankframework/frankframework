/*
 * $Log: JavaProxy.java,v $
 * Revision 1.1  2004-04-26 06:21:38  a1909356#db2admin
 * Add java receiver
 *
 */
package nl.nn.adapterframework.receivers;

import java.io.Serializable;


/**
 * @author J. Dekker
 * @version Id
 *
 * The java proxy enables the usage of Ibis directly from java. The serviceName
 * property must equal the name of the JavaReceiver in the Ibis configuration file.
 * 
 * If you package the ibis.jar in the .ear and not in your .war then you must
 * set the WAR class loader policy of the server in which the .ear is deployed to
 * application.
 */
public class JavaProxy implements ServiceClient, Serializable {
	public static final String version="$Id: JavaProxy.java,v 1.1 2004-04-26 06:21:38 a1909356#db2admin Exp $";
	private String serviceName;
	
	public JavaProxy(String serviceName) {
		this.serviceName = serviceName;
	}

	/* 
	 * @see nl.nn.adapterframework.receivers.ServiceClient#processRequest(java.lang.String, java.lang.String)
	 */
	public String processRequest(String correlationId, String message) {
		return ServiceDispatcher.getInstance().dispatchRequest(serviceName, correlationId, message);
	}

	/* 
	 * @see nl.nn.adapterframework.receivers.ServiceClient#processRequest(java.lang.String)
	 */
	public String processRequest(String message) {
		return ServiceDispatcher.getInstance().dispatchRequest(serviceName, message);
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

}
