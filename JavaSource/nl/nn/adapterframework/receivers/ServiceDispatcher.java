/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: ServiceDispatcher.java,v $
 * Revision 1.15  2012-02-01 12:55:42  europe\m168309
 * added received message to debug logging
 *
 * Revision 1.14  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.12  2011/05/19 15:00:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * simplified, now uses a single dispatch-method
 *
 * Revision 1.11  2008/08/13 13:43:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed reference to ServiceListener
 *
 * Revision 1.10  2007/10/08 12:24:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.9  2007/02/12 14:03:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.8  2005/09/20 13:28:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added warning for double registered listeners
 *
 * Revision 1.7  2005/08/30 16:05:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * throw exception if requested service does not exist
 *
 * Revision 1.6  2005/07/05 13:17:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow for ServiceClient2 extensions
 *
 */
package nl.nn.adapterframework.receivers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
/**
 * Singleton class that knows about the ServiceListeners that are active.
 * <br/>
 * This class is to be used as a facade for different services that implement
 * the <code>ServiceClient</code> interface.<br/>
 * This class is exposed as a webservice, to be able to provide a single point
 * of entry to all adapters that have a ServiceListener as a IReceiver.
 *
 * @version $Id$
 * @author Johan Verrips
 * @see ServiceClient
 * @see ServiceListener
 */
public class ServiceDispatcher  {
	protected Logger log = LogUtil.getLogger(this);
	
	private Map registeredListeners=new HashMap();
	private static ServiceDispatcher self=null;

	
    /**
     * Use this method to get hold of the <code>ServiceDispatcher</code>
     * @return an instance of this class
     */
	public static synchronized ServiceDispatcher getInstance() {
		if (self == null) {
			self = new ServiceDispatcher();
		}
		return (self);
	}

	/**
	 * Dispatch a request.
	 * 
	 * @since 4.3
	 */
	public String dispatchRequest(String serviceName, String correlationId, String request, Map requestContext) throws ListenerException {
		if (log.isDebugEnabled()) {
			log.debug("dispatchRequest for service ["+serviceName+"] correlationId ["+correlationId+"] message ["+request+"]");
		}
		ServiceClient client=(ServiceClient)registeredListeners.get(serviceName);
		if (client==null) {
            throw new ListenerException("service ["+serviceName+"] is not registered");
		}
		String result=client.processRequest(correlationId, request, requestContext);
		if (result==null) {
			log.warn("result is null!");
		}			
		return result;
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

	public  void registerServiceClient(String name, ServiceClient listener) throws ListenerException{
		if (isRegisteredServiceListener(name)) {
			log.warn("listener ["+name+"] already registered with ServiceDispatcher");
		}
		registeredListeners.put(name, listener);
		log.info("Listener ["+name+"] registered at ServiceDispatcher");
	}
}
