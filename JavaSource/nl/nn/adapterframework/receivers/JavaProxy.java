/*
 * $Log: JavaProxy.java,v $
 * Revision 1.7  2006-03-20 13:52:59  europe\L190409
 * AbsoluteSingleton instead of JNDI
 *
 * Revision 1.6  2006/02/28 08:49:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added static lookupProxy method
 *
 * Revision 1.5  2005/07/05 13:18:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import javax.naming.NamingException;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JNDIBase;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;
import nl.nn.adapterframework.dispatcher.RequestProcessor;
import nl.nn.adapterframework.dispatcher.DispatcherManager;


/**
 *
 * The JavaProxy enables the usage of Ibis directly from java. The serviceName
 * property must equal the name of the JavaListener in the Ibis configuration file.
 * 
 * If you package the ibis.jar in the .ear and not in your .war then you must
 * set the WAR class loader policy of the server in which the .ear is deployed to
 * application.
 *
 * @author  J. Dekker
 * @version Id
 * @since   4.2
 */
public class JavaProxy implements ServiceClient2, Serializable, RequestProcessor {
	public static final String version="$RCSfile: JavaProxy.java,v $ $Revision: 1.7 $ $Date: 2006-03-20 13:52:59 $";
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

	/**
	 * @param jndiName
	 * @return JavaProxy for a JavaPusher if registered in JNDI under name or null
	 */
	public static ServiceClient2 lookupProxy(String jndiName) throws NamingException {
		JNDIBase jndiBase = new JNDIBase();
		try {
			return (ServiceClient2)jndiBase.getContext().lookup(jndiName);
		} finally {
			jndiBase.closeContext();
		}
	}

	public String processRequest(String correlationId, String message, HashMap requestContext) throws Exception {
		return processRequestWithExceptions(correlationId, message, requestContext);
	}


	public static String processRequest(String clientName, String correlationId, String message, HashMap requestContext) throws Exception {
		DispatcherManager as = DispatcherManagerFactory.getDispatcherManager();
		return as.processRequest(clientName, correlationId, message, requestContext);
	}



//	{
//		// initialize
//		ServiceClient2 persistValues=null;
//		ServiceClient2 retrieveValues=null;
//		ServiceClient2 transformInitialValues=null;
//		try {
//			persistValues = JavaProxy.lookupProxy("ibis/persistValues");
//			retrieveValues = JavaProxy.lookupProxy("ibis/retrieveValues");
//			transformInitialValues = JavaProxy.lookupProxy("ibis/transformInitialValues");
//		} catch (NamingException e) {
//			e.printStackTrace(); // handel de fout af
//		}
//
//		// persist values		
//		String bericht1="...";
//		String procesId1="...";
//		HashMap requestContext1=new HashMap();
//		requestContext1.put("procesId",procesId1);
//		try {
//			persistValues.processRequestWithExceptions(null,bericht1,requestContext1);
//		} catch (ListenerException e) {
//			e.printStackTrace(); // handel de fout af
//		}
//		
//		
//		// retrieve values		
//		String procesId2="...";
//		String bericht2;
//		try {
//			bericht2 = retrieveValues.processRequestWithExceptions(null,procesId2,null);
//		} catch (ListenerException e) {
//			e.printStackTrace(); // handel de fout af
//		}
//
//		// verwerk bericht
//		String bericht3="....";
//		String dialect3="GIM";
//		String profileXml3=null;
//		HashMap requestContext3=new HashMap();
//		requestContext3.put("dialect",dialect3);
//		try {
//			profileXml3 = transformInitialValues.processRequestWithExceptions(null,bericht3,requestContext3);
//		} catch (ListenerException e) {
//			e.printStackTrace(); // handel de fout af
//		}
//		
//	}

}
