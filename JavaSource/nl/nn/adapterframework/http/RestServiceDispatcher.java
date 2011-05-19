/*
 * $Log: RestServiceDispatcher.java,v $
 * Revision 1.1  2011-05-19 15:11:27  L190409
 * first version of Rest-provider support
 *
 */
package nl.nn.adapterframework.http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
/**
 * Singleton class that knows about the RestListeners that are active.
 * <br/>
 * This class is to be used as a facade for different services that implement
 * the <code>ServiceClient</code> interface.<br/>
 * This class is exposed as a webservice, to be able to provide a single point
 * of entry to all adapters that have a ServiceListener as a IReceiver.
 * @version Id
 */
public class RestServiceDispatcher  {
	protected Logger log = LogUtil.getLogger(this);
	
	private final String WILDCARD="*";
	private final String KEY_LISTENER="listener";
	private final String KEY_ETAG_KEY="etagKey";
	private final String KEY_CONTENT_TYPE_KEY="contentTypekey";

	private SortedMap patternClients=new TreeMap(new RestUriComparator());
	
	private static RestServiceDispatcher self=null;
	
	public static synchronized RestServiceDispatcher getInstance(){
		 if( self == null ) {
            self = new RestServiceDispatcher();
        }
        return self;
	}


	/**
	 * Dispatch a request.
	 * @param serviceName the name of the IReceiver object
	 * @param correlationId the correlationId of this request;
	 * @param request the <code>String</code> with the request/input
	 * @return String with the result of processing the <code>request</code> throught the <code>serviceName</code>
     */
	public String dispatchRequest(String uri, String method, String etag, String contentType, String request, Map context) throws ListenerException {
		
		if (log.isDebugEnabled()) log.debug("searching listener for uri ["+uri+"] method ["+method+"]");
		
		String uriPattern=null;
		for (Iterator it=patternClients.keySet().iterator();it.hasNext();) {
			uriPattern=(String)it.next();
			if (log.isDebugEnabled()) log.debug("comparing uri to pattern ["+uriPattern+"]");
			if (uri.startsWith(uriPattern)) {
				break;
			}
		}
		if (uriPattern==null) {
			throw new ListenerException("no REST listener configured for uri ["+uri+"]");
		}
		Map patternEntry=(Map)patternClients.get(uriPattern);
		
		Map methodConfig = (Map)patternEntry.get(method);
		if (methodConfig==null) {
			methodConfig = (Map)patternEntry.get(WILDCARD);
		}
		if (methodConfig==null) {
			throw new ListenerException("No RestListeners specified for uri ["+uri+"] method ["+method+"]");
		}
		if (context==null) {
			context=new HashMap();
		}
		context.put("uri", uri);
		context.put("method", method);
		context.put("etag", etag);
		context.put("contentType", contentType);
		ServiceClient listener=(ServiceClient)methodConfig.get(KEY_LISTENER);
		String etagKey=(String)methodConfig.get(KEY_ETAG_KEY);
		String contentTypeKey=(String)methodConfig.get(KEY_CONTENT_TYPE_KEY);
		Map requestContext=new HashMap();
		if (etagKey!=null) requestContext.put(etagKey,etag);
		if (contentTypeKey!=null) requestContext.put(contentTypeKey,contentType);
		String result=listener.processRequest(null, request, requestContext);
		if (result==null) {
			log.warn("result is null!");
		}			
		return result;
	}
	
	public  void registerServiceClient(String name, ServiceClient listener,
			String uriPattern, String method, 
			String etagSessionKey, String contentTypeSessionKey) throws ConfigurationException {
		
		if (StringUtils.isEmpty(method)) {
			method=WILDCARD;
		}
		
		Map patternEntry=(Map)patternClients.get(uriPattern);
		if (patternEntry==null) {
			patternEntry=new HashMap();
			patternClients.put(uriPattern, patternEntry);
		}
		Map listenerConfig = (Map)patternEntry.get(method);
		if (listenerConfig!=null) { 
			throw new ConfigurationException("RestListener for uriPattern ["+uriPattern+"] method ["+method+"] already configured");
		}
		listenerConfig = new HashMap();
		patternEntry.put(method,listenerConfig);
		listenerConfig.put(KEY_LISTENER, listener);
		if (StringUtils.isNotEmpty(etagSessionKey)) listenerConfig.put(KEY_ETAG_KEY, etagSessionKey);
		if (StringUtils.isNotEmpty(contentTypeSessionKey)) listenerConfig.put(KEY_CONTENT_TYPE_KEY, contentTypeSessionKey);
	}
}
