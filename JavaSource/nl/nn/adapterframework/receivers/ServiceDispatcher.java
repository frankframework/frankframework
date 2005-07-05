/*
 * $Log: ServiceDispatcher.java,v $
 * Revision 1.6  2005-07-05 13:17:52  europe\L190409
 * allow for ServiceClient2 extensions
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.ListenerException;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
/**
 * Singleton class that knows about the ServiceListeners that are active.
 * <br/>
 * This class is to be used as a facade for different services that implement
 * the <code>ServiceClient</code> interface.<br/>
 * This class is exposed as a webservice, to be able to provide a single point
 * of entry to all adapters that have a ServiceListener as a IReceiver.
 * <p>Creation date: (24-03-2003 11:06:48)</p>
 * @version Id
 * @author Johan Verrips
 * @see ServiceClient
 * @see ServiceListener
 * @see ServiceDispatcherBean
 */
public class ServiceDispatcher  {
	public static final String version = "$RCSfile: ServiceDispatcher.java,v $ $Revision: 1.6 $ $Date: 2005-07-05 13:17:52 $";
	protected Logger log = Logger.getLogger(this.getClass());
	
	private Hashtable registeredListeners=new Hashtable();
	private static ServiceDispatcher self=null;

    /** 
     * Dispatch a request.
     * @param serviceName the name of the IReceiver object
     * @param request the <code>String</code> with the request/input
     * @return String with the result of processing the <code>request</code> throught the <code>serviceName</code>
     */
	public String dispatchRequest(String serviceName, String request){
		return dispatchRequest(serviceName, null, request);
	}

	/**
	 * Dispatch a request.
	 * @param serviceName the name of the IReceiver object
	 * @param correlationId the correlationId of this request;
	 * @param request the <code>String</code> with the request/input
	 * @return String with the result of processing the <code>request</code> throught the <code>serviceName</code>
	 * @since 4.0
	 */
	public String dispatchRequest(String serviceName, String correlationId, String request){
		if (log.isDebugEnabled()) {
			log.debug("dispatchRequest for service ["+serviceName+"] request ["+request+"]");
		}
		ServiceClient client=(ServiceClient)registeredListeners.get(serviceName);
		if (client==null) {
			String msg="service request for service ["+serviceName+"] is not registered";
			log.error(msg);
			return msg;
		}
		String result=client.processRequest(correlationId, request);
		if (result==null) {
			log.warn("result is null!");
		}			
		return result;
	}
	
	/**
     * Dispatch a request.
     * @since 4.3
     */
	public String dispatchRequestWithExceptions(String serviceName, String correlationId, String request, HashMap requestContext) throws ListenerException{
		if (log.isDebugEnabled()) {
			log.debug("dispatchRequest for service ["+serviceName+"] request ["+request+"]");
		}
		ServiceClient client=(ServiceClient)registeredListeners.get(serviceName);
		if (client==null) {
            String msg="service request for service ["+serviceName+"] is not registered";
			log.error(msg);
			return msg;
		}
		String result;
		if (client instanceof ServiceClient2) {
			result=((ServiceClient2)client).processRequestWithExceptions(correlationId, request, requestContext);
		} else { 
			result=client.processRequest(correlationId, request);
		} 
		if (result==null) {
			log.warn("result is null!");
		}			
		return result;
	}
    /**
     * Use this method to get hold of the <code>ServiceDispatcher</code>
     * @return an instance of this class
     */
	public static synchronized ServiceDispatcher getInstance(){
		 if( self == null )
        {
            self = new ServiceDispatcher();
        }
        return( self );
		
	}
    /**
     * Retrieve the names of the registered listeners in alphabetical order.
     * @return Iterator with the names.
     */
	public Iterator getRegisteredListenerNames() {
	      SortedSet sortedKeys = new TreeSet(registeredListeners.keySet());
      return sortedKeys.iterator(); 
    }
	/**
     * Check wether a servicename is registered at the <code>ServiceDispatcher</code>.
     * @param name
     * @return true if the service is registered at this dispatcher, otherwise false
     */
	public boolean isRegisteredServiceListener(String name) {
		return (registeredListeners.get(name)!=null);
	}
    /**
     * Registers a ServiceListener implementation
     * @param listener a ServiceListener implementation
     */
	public  void registerServiceListener(ServiceListener listener){
		registerServiceClient(listener.getName(), listener);
	}

	public  void registerServiceClient(String name, ServiceClient listener){
		registeredListeners.put(name, listener);
		log.info("Listener ["+name+"] registered at ServiceDispatcher");
	}
}
