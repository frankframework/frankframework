/*
   Copyright 2013-2015 Nationale-Nederlanden

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
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
/**
 * Singleton class that knows about the RestListeners that are active.
 * <br/>
 * This class is to be used as a facade for different services that implement
 * the <code>ServiceClient</code> interface.<br/>
 * This class is exposed as a webservice, to be able to provide a single point
 * of entry to all adapters that have a ServiceListener as a IReceiver.
 */
public class RestServiceDispatcher  {
	protected Logger log = LogUtil.getLogger(this);
	protected Logger secLog = LogUtil.getLogger("SEC");
	
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

	public String findMatchingPattern(String uri) {
		String matchingPattern=null;
		for (Iterator it=patternClients.keySet().iterator();it.hasNext();) {
			String uriPattern=(String)it.next();
			if (log.isTraceEnabled()) log.trace("comparing uri to pattern ["+uriPattern+"] ");
			if (uri.startsWith(uriPattern)) {
				matchingPattern=uriPattern;
				break;
			}
		}
		return matchingPattern;
	}
	
	public Map getMethodConfig(String matchingPattern, String method) {
		Map methodConfig;
		Map patternEntry=(Map)patternClients.get(matchingPattern);
		
		methodConfig = (Map)patternEntry.get(method);
		if (methodConfig==null) {
			methodConfig = (Map)patternEntry.get(WILDCARD);
		}
		return methodConfig;
	}
	
	public List getAvailableMethods(String matchingPattern) {
		Map patternEntry=(Map)patternClients.get(matchingPattern);
		Iterator it = patternEntry.entrySet().iterator();
		List methods = new ArrayList<String>();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			methods.add(pair.getKey());
		}
		return methods;
	}

	/**
	 * Dispatch a request.
	 * @param uri the name of the IReceiver object
	 * @param method the correlationId of this request;
	 * @param request the <code>String</code> with the request/input
	 * @return String with the result of processing the <code>request</code> throught the <code>serviceName</code>
     */
	public String dispatchRequest(String restPath, String uri, HttpServletRequest httpServletRequest, String etag, String contentType, String request, Map context, HttpServletResponse httpServletResponse, ServletContext servletContext) throws ListenerException {
		String method = httpServletRequest.getMethod();
		if (log.isTraceEnabled()) log.trace("searching listener for uri ["+uri+"] method ["+method+"]");
		
		String matchingPattern = findMatchingPattern(uri);
		if (matchingPattern==null) {
			throw new ListenerException("no REST listener configured for uri ["+uri+"]");
		}
		
		Map methodConfig = getMethodConfig(matchingPattern, method);
		
		if (methodConfig==null) {
			throw new ListenerException("No RestListeners specified for uri ["+uri+"] method ["+method+"]");
		}
		if (context==null) {
			context=new HashMap();
		}
		context.put("restPath", restPath);
		context.put("uri", uri);
		context.put("method", method);
		context.put("etag", etag);
		context.put("contentType", contentType);
		ServiceClient listener=(ServiceClient)methodConfig.get(KEY_LISTENER);
		String etagKey=(String)methodConfig.get(KEY_ETAG_KEY);
		String contentTypeKey=(String)methodConfig.get(KEY_CONTENT_TYPE_KEY);

		Principal principal = null;
		if (httpServletRequest != null) {
			principal = httpServletRequest.getUserPrincipal();
			if (principal != null) {
				context.put("principal", principal.getName());
			}
		}
		
		String ctName = Thread.currentThread().getName();
		try {
			boolean writeToSecLog = false;
			if (listener instanceof RestListener) {
				RestListener restListener = (RestListener) listener;
				if (restListener.isRetrieveMultipart()) {
					if (ServletFileUpload.isMultipartContent(httpServletRequest)) {
						try {
							DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
							ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);
							List<FileItem> items = servletFileUpload.parseRequest(httpServletRequest);
					        for (FileItem item : items) {
					        	if (item.isFormField()) {
					                // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
					                String fieldName = item.getFieldName();
					                String fieldValue = item.getString();
					    			log.debug("setting parameter ["+fieldName+"] to ["+fieldValue+"]");
					    			context.put(fieldName, fieldValue);
					            } else {
					                // Process form file field (input type="file").
					                String fieldName = item.getFieldName();
					                String fieldNameName = fieldName + "Name";
					                String fileName = FilenameUtils.getName(item.getName());
					    			if (log.isTraceEnabled()) log.trace("setting parameter ["+fieldNameName+"] to ["+fileName+"]");
					    			context.put(fieldNameName, fileName);
					                InputStream inputStream = item.getInputStream();
					                if (inputStream.available() > 0) {
					                	log.debug("setting parameter ["+fieldName+"] to input stream of file ["+fileName+"]");
						    			context.put(fieldName, inputStream);
					                } else {
						    			log.debug("setting parameter ["+fieldName+"] to ["+null+"]");
						    			context.put(fieldName, null);
					                }
					            }
					        }
						} catch (FileUploadException e) {
							throw new ListenerException(e);
						} catch (IOException e) {
							throw new ListenerException(e);
						}
					}
				}
				writeToSecLog = restListener.isWriteToSecLog();
				if (writeToSecLog) {
					context.put("writeSecLogMessage", restListener.isWriteSecLogMessage());
 				}
				boolean authorized = false;
				if (principal == null) {
					authorized = true;
				} else {
					String authRoles = restListener.getAuthRoles();
					if (StringUtils.isNotEmpty(authRoles)) {
						StringTokenizer st = new StringTokenizer(authRoles, ",;");
						while (st.hasMoreTokens()) {
							String authRole = st.nextToken();
							if (httpServletRequest.isUserInRole(authRole)) {
								authorized = true;
							}
						}
					}
				}
				if (!authorized) {
					throw new ListenerException("Not allowed for uri [" + uri + "]");
				}
				Thread.currentThread().setName(restListener.getName() + "["+ctName+"]");
			}
	
			if (etagKey!=null) context.put(etagKey,etag);
			if (contentTypeKey!=null) context.put(contentTypeKey,contentType);
			if (log.isTraceEnabled()) log.trace("dispatching request, uri ["+uri+"] listener pattern ["+matchingPattern+"] method ["+method+"] etag ["+etag+"] contentType ["+contentType+"]");
			if (httpServletRequest!=null) context.put("restListenerServletRequest", httpServletRequest);
			if (httpServletResponse!=null) context.put("restListenerServletResponse", httpServletResponse);
			if (servletContext!=null) context.put("restListenerServletContext", servletContext);

			if (writeToSecLog) {
				secLog.info(HttpUtils.getExtendedCommandIssuedBy(httpServletRequest));
			}

			String result=listener.processRequest(null, request, context);
			if (result==null && !context.containsKey("exitcode")) {
				log.warn("result is null!");
			}
			return result;
		} finally {
			if (listener instanceof RestListener) {
				Thread.currentThread().setName(ctName);
			}
		}
	}

	public void registerServiceClient(ServiceClient listener, String uriPattern,
			String method, String etagSessionKey, String contentTypeSessionKey) throws ConfigurationException {
		uriPattern = unifyUriPattern(uriPattern);
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

	public void unregisterServiceClient(String uriPattern) {
		uriPattern = unifyUriPattern(uriPattern);
		patternClients.remove(uriPattern);
	}

	public Set getUriPatterns() {
		return patternClients.keySet();
	}

	private String unifyUriPattern(String uriPattern) {
		if (StringUtils.isEmpty(uriPattern)) {
			uriPattern="/";
		} else {
			if (!uriPattern.startsWith("/")) {
				uriPattern="/"+uriPattern;
			}
		}
		return uriPattern;
	}
}
