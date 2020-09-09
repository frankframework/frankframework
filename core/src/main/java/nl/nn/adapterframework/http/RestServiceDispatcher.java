/*
   Copyright 2013-2018, 2020 Nationale-Nederlanden

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
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.http.rest.ApiCacheManager;
import nl.nn.adapterframework.http.rest.IApiCache;
import nl.nn.adapterframework.pipes.CreateRestViewPipe;
import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
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
	private final String SVG_FILE_NO_IMAGE_AVAILABLE = "/IAF_WebControl/GenerateFlowDiagram/svg/no_image_available.svg";

	private static AppConstants appConstants = AppConstants.getInstance();
	private static String etagCacheType = appConstants.getProperty("etag.cache.type", "ehcache");
	private boolean STRUTS_CONSOLE_ENABLED = appConstants.getBoolean("strutsConsole.enabled", false);

	private ConcurrentSkipListMap patternClients=new ConcurrentSkipListMap(new RestUriComparator());

	private static RestServiceDispatcher self = null;
	private static IApiCache cache = ApiCacheManager.getInstance();

	public static synchronized RestServiceDispatcher getInstance() {
		if( self == null ) {
			self = new RestServiceDispatcher();
		}
		return self;
	}

	public String findMatchingPattern(String uri) {
		if (uri==null) {
			return null;
		}

		String lookupUriPattern;
		int index = uri.indexOf('/', 1);
		if (index >= 1) {
			lookupUriPattern = uri.substring(0, index);
		} else {
			lookupUriPattern = uri;
		}

		String matchingPattern=null;
		for (Iterator it=patternClients.keySet().iterator();it.hasNext();) {
			String uriPattern=(String)it.next();
			if (log.isTraceEnabled()) log.trace("comparing uri to pattern ["+uriPattern+"] ");
			if (lookupUriPattern.equals(uriPattern)) {
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
	 * @param request the <code>String</code> with the request/input
	 * @return String with the result of processing the <code>request</code> through the <code>serviceName</code>
	 */
	public String dispatchRequest(String restPath, String uri, HttpServletRequest httpServletRequest, String contentType, String request, IPipeLineSession context, HttpServletResponse httpServletResponse, ServletContext servletContext) throws ListenerException {
		String method = httpServletRequest.getMethod();
		if (log.isTraceEnabled()) log.trace("searching listener for uri ["+uri+"] method ["+method+"]");
		
		String matchingPattern = findMatchingPattern(uri);
		if (matchingPattern==null) {
			if (uri != null && (uri.equals("/showFlowDiagram")
					|| uri.startsWith("/showFlowDiagram/"))) {
				log.info("no REST listener configured for uri ["+uri+"], so using 'no image available'");
				noImageAvailable(httpServletResponse);
				return "";
			}
			if (uri != null && STRUTS_CONSOLE_ENABLED && (uri.equals("/showConfigurationStatus") || uri.startsWith("/showConfigurationStatus/")) ) {
				log.info("no REST listener configured for uri [" + uri + "], if REST listener does exist then trying to restart");
				if (RestListenerUtils.restartShowConfigurationStatus(servletContext)) {
					httpServletResponse.setHeader("REFRESH", "0");
					return "";
				} else {
					return retrieveNoIbisContext(httpServletRequest, servletContext);
				}
			}
			throw new ListenerException("no REST listener configured for uri ["+uri+"]");
		}
		
		Map methodConfig = getMethodConfig(matchingPattern, method);
		
		if (methodConfig==null) {
			throw new ListenerException("No REST listener specified for uri ["+uri+"] method ["+method+"]");
		}
		if (context==null) {
			context=new PipeLineSessionBase();
		}
		context.put("restPath", restPath);
		context.put("uri", uri);
		context.put("method", method);

		String etag = null;
		String ifNoneMatch = httpServletRequest.getHeader("If-None-Match");
		if(ifNoneMatch != null && !ifNoneMatch.isEmpty()) {
			context.put("if-none-match", ifNoneMatch);
			etag = ifNoneMatch;
		}
		String ifMatch = httpServletRequest.getHeader("If-Match");
		if(ifMatch != null && !ifMatch.isEmpty()) {
			context.put("if-match", ifMatch);
			etag = ifMatch;
		}

		context.put("contentType", contentType);
		context.put("userAgent", httpServletRequest.getHeader("User-Agent"));
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
					    			log.trace("setting parameter ["+fieldName+"] to ["+fieldValue+"]");
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
					                	log.trace("setting parameter ["+fieldName+"] to input stream of file ["+fileName+"]");
						    			context.put(fieldName, inputStream);
					                } else {
						    			log.trace("setting parameter ["+fieldName+"] to ["+null+"]");
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
			if (httpServletRequest!=null) context.put(IPipeLineSession.HTTP_REQUEST_KEY, httpServletRequest);
			if (httpServletResponse!=null) context.put(IPipeLineSession.HTTP_RESPONSE_KEY, httpServletResponse);
			if (servletContext!=null) context.put(IPipeLineSession.SERVLET_CONTEXT_KEY, servletContext);

			if (writeToSecLog) {
				secLog.info(HttpUtils.getExtendedCommandIssuedBy(httpServletRequest));
			}

			//Caching: check for etags
			if(uri.startsWith("/")) uri = uri.substring(1);
			if(uri.indexOf("?") > -1) {
				uri = uri.split("\\?")[0];
			}
			String etagCacheKey = restPath+"_"+uri;

			if(cache != null && cache.containsKey(etagCacheKey)) {
				String cachedEtag = (String) cache.get(etagCacheKey);

				if(ifNoneMatch != null && ifNoneMatch.equalsIgnoreCase(cachedEtag) && method.equalsIgnoreCase("GET")) {
					//Exit with 304
					context.put("exitcode", 304);
					if(log.isDebugEnabled()) log.trace("aborting request with status 304, matched if-none-match ["+ifNoneMatch+"]");
					return null;
				}
				if(ifMatch != null && !ifMatch.equalsIgnoreCase(cachedEtag) && !method.equalsIgnoreCase("GET")) {
					//Exit with 412
					context.put("exitcode", 412);
					if(log.isDebugEnabled()) log.trace("aborting request with status 412, matched if-match ["+ifMatch+"] method ["+method+"]");
					return null;
				}
			}

			String result=listener.processRequest(null, request, context);

			//Caching: pipeline has been processed, save etag
			if(result != null && cache != null && context.containsKey("etag")) { //In case the eTag has manually been set and the pipeline exited in error state...
				cache.put(etagCacheKey, context.get("etag"));
			}

			if (result == null && !context.containsKey("exitcode")) {
				log.warn("result is null!");
			}
			return result;
		} finally {
			if (listener instanceof RestListener) {
				Thread.currentThread().setName(ctName);
			}
		}
	}

	private void noImageAvailable(HttpServletResponse httpServletResponse)
			throws ListenerException {
		URL svgSource = ClassUtils.getResourceURL(this, SVG_FILE_NO_IMAGE_AVAILABLE);
		if (svgSource == null) {
			throw new ListenerException("cannot find resource ["
					+ SVG_FILE_NO_IMAGE_AVAILABLE + "]");
		}
		try {
			httpServletResponse.setContentType("image/svg+xml");
			InputStream inputStream = null;
			try {
				inputStream = svgSource.openStream();
				Misc.streamToStream(inputStream,
						httpServletResponse.getOutputStream());
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		} catch (IOException e) {
			throw new ListenerException(e);
		}
	}

	public String retrieveNoIbisContext(HttpServletRequest httpServletRequest,
			ServletContext servletContext) throws ListenerException {
		try {
			CreateRestViewPipe pipe = new CreateRestViewPipe();
			pipe.setStyleSheetName("xml/xsl/web/noIbisContext.xsl");
			//pipe.setXslt2(true);
			PipeForward pipeForward = new PipeForward();
			pipeForward.setName("success");
			pipe.registerForward(pipeForward);
			pipe.configure();
			pipe.start();
			IPipeLineSession session = new PipeLineSessionBase();
			session.put(IPipeLineSession.HTTP_REQUEST_KEY, httpServletRequest);
			session.put(IPipeLineSession.SERVLET_CONTEXT_KEY, servletContext);
			String result = pipe.doPipe(Message.asMessage("<dummy/>"), session).getResult().asString();
			pipe.stop();
			return result;
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}
	
	public void registerServiceClient(ServiceClient listener, String uriPattern,
			String method, String etagSessionKey, String contentTypeSessionKey, boolean validateEtag) throws ConfigurationException {
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
		listenerConfig.put("validateEtag", validateEtag);
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
