/*
   Copyright 2013-2018, 2020 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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
package org.frankframework.http;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Logger;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.http.mime.MultipartUtils;
import org.frankframework.http.rest.ApiCacheManager;
import org.frankframework.http.rest.IApiCache;
import org.frankframework.stream.Message;
import org.frankframework.util.HttpUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StringUtil;

/**
 * Singleton class that knows about the RestListeners that are active.
 * <br/>
 * This class is to be used as a facade for different services that implement
 * the <code>ServiceClient</code> interface.<br/>
 * This class is exposed as a webservice, to be able to provide a single point
 * of entry to all adapters that have a ServiceListener as a IReceiver.
 */
public class RestServiceDispatcher {
	protected Logger log = LogUtil.getLogger(this);
	protected Logger secLog = LogUtil.getLogger("SEC");

	private static final String WILDCARD="*";
	private static final String KEY_LISTENER="listener";
	private static final String KEY_ETAG_KEY="etagKey";
	private static final String KEY_CONTENT_TYPE_KEY="contentTypekey";

	private final Map<String,Map<String,Map<String,Object>>> patternClients=new ConcurrentHashMap<>();

	private static RestServiceDispatcher self = null;
	private static final IApiCache cache = ApiCacheManager.getInstance();

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

		return patternClients.containsKey(lookupUriPattern) ? lookupUriPattern : null;
	}

	public Map<String,Object> getMethodConfig(String matchingPattern, String method) {
		Map<String,Object> methodConfig;
		Map<String,Map<String,Object>> patternEntry=patternClients.get(matchingPattern);

		methodConfig = patternEntry.get(method);
		if (methodConfig==null) {
			methodConfig = patternEntry.get(WILDCARD);
		}
		return methodConfig;
	}

	public List<String> getAvailableMethods(String matchingPattern) {
		Map<String,Map<String,Object>> patternEntry=patternClients.get(matchingPattern);
		Iterator<Entry<String,Map<String,Object>>> it = patternEntry.entrySet().iterator();
		List<String> methods = new ArrayList<>();
		while (it.hasNext()) {
			Entry<String,Map<String,Object>> pair = it.next();
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
	public Message dispatchRequest(String restPath, String uri, HttpServletRequest httpServletRequest, String contentType, String request, PipeLineSession context, HttpServletResponse httpServletResponse) throws ListenerException {
		String method = httpServletRequest.getMethod();
		log.trace("searching listener for uri [{}] method [{}]", uri, method);

		String matchingPattern = findMatchingPattern(uri);
		if (matchingPattern==null) {
			throw new ListenerException("no REST listener configured for uri ["+uri+"]");
		}

		Map<String,Object> methodConfig = getMethodConfig(matchingPattern, method);

		if (methodConfig==null) {
			throw new ListenerException("No REST listener specified for uri ["+uri+"] method ["+method+"]");
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
		RestListener listener=(RestListener)methodConfig.get(KEY_LISTENER);
		String etagKey=(String)methodConfig.get(KEY_ETAG_KEY);
		String contentTypeKey=(String)methodConfig.get(KEY_CONTENT_TYPE_KEY);

		final Principal principal = httpServletRequest.getUserPrincipal();
		if (principal != null) {
			context.put("principal", principal.getName());
		}

		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put("listener", listener.getName())) {
			boolean writeToSecLog = false;
			if (listener.isRetrieveMultipart() && MultipartUtils.isMultipart(httpServletRequest)) {
				try {
					InputStreamDataSource dataSource = new InputStreamDataSource(httpServletRequest.getContentType(), httpServletRequest.getInputStream()); // The entire InputStream will be read here!
					MimeMultipart mimeMultipart = new MimeMultipart(dataSource);

					for (int i = 0; i < mimeMultipart.getCount(); i++) {
						BodyPart bodyPart = mimeMultipart.getBodyPart(i);
						String fieldName = MultipartUtils.getFieldName(bodyPart);
						PartMessage bodyPartMessage = new PartMessage(bodyPart);

						log.trace("setting parameter [{}] to [{}]", fieldName, bodyPartMessage);
						context.put(fieldName, bodyPartMessage);

						if (MultipartUtils.isBinary(bodyPart)) { // Process form file field (input type="file").
							String fieldNameName = fieldName + "Name";
							String fileName = MultipartUtils.getFileName(bodyPart);
							log.trace("setting parameter [{}] to [{}]", fieldNameName, fileName);
							context.put(fieldNameName, fileName);
						}
					}
				} catch (MessagingException | IOException e) {
					throw new ListenerException(e);
				}
			}
			writeToSecLog = listener.isWriteToSecLog();
			if (writeToSecLog) {
				context.put("writeSecLogMessage", listener.isWriteSecLogMessage());
			}
			boolean authorized;
			if (principal == null) {
				authorized = true;
			} else {
				String authRoles = listener.getAuthRoles();
				authorized = StringUtil.splitToStream(authRoles, ",;")
						.anyMatch(httpServletRequest::isUserInRole);
			}
			if (!authorized) {
				throw new ListenerException("Not allowed for uri [" + uri + "]");
			}

			if (etagKey!=null) context.put(etagKey,etag);
			if (contentTypeKey!=null) context.put(contentTypeKey,contentType);
			log.trace("dispatching request, uri [{}] listener pattern [{}] method [{}] etag [{}] contentType [{}]", uri, matchingPattern, method, etag, contentType);
			context.put(PipeLineSession.HTTP_REQUEST_KEY, httpServletRequest);
			if (httpServletResponse!=null) context.put(PipeLineSession.HTTP_RESPONSE_KEY, httpServletResponse);

			if (writeToSecLog) {
				secLog.info(HttpUtils.getExtendedCommandIssuedBy(httpServletRequest));
			}

			// Caching: check for etags
			if(uri.startsWith("/")) uri = uri.substring(1);
			if(uri.contains("?")) {
				uri = uri.split("\\?")[0];
			}
			String etagCacheKey = restPath+"_"+uri;

			if(cache != null && cache.containsKey(etagCacheKey)) {
				String cachedEtag = (String) cache.get(etagCacheKey);

				if(ifNoneMatch != null && ifNoneMatch.equalsIgnoreCase(cachedEtag) && "GET".equalsIgnoreCase(method)) {
					// Exit with 304
					context.put(PipeLineSession.EXIT_CODE_CONTEXT_KEY, 304);
					if(log.isDebugEnabled()) log.trace("aborting request with status 304, matched if-none-match [{}]", ifNoneMatch);
					return null;
				}
				if(ifMatch != null && !ifMatch.equalsIgnoreCase(cachedEtag) && !"GET".equalsIgnoreCase(method)) {
					// Exit with 412
					context.put(PipeLineSession.EXIT_CODE_CONTEXT_KEY, 412);
					if(log.isDebugEnabled()) log.trace("aborting request with status 412, matched if-match [{}] method [{}]", ifMatch, method);
					return null;
				}
			}

			Message result=listener.processRequest(new Message(request), context);
			// Caching: pipeline has been processed, save etag
			if(!Message.isNull(result) && cache != null && context.containsKey("etag")) { // In case the eTag has manually been set and the pipeline exited in error state...
				cache.put(etagCacheKey, context.get("etag"));
			}

			if (Message.isNull(result) && !context.containsKey(PipeLineSession.EXIT_CODE_CONTEXT_KEY)) {
				log.warn("result is null!");
			}
			return result;
		}
	}

	public void registerServiceClient(RestListener listener, String uriPattern,
			String method, String etagSessionKey, String contentTypeSessionKey, boolean validateEtag) {
		uriPattern = unifyUriPattern(uriPattern);
		if (StringUtils.isEmpty(method)) {
			method=WILDCARD;
		}

		Map<String,Map<String,Object>> patternEntry = patternClients.computeIfAbsent(uriPattern, p -> new ConcurrentHashMap<>());
		patternEntry.computeIfAbsent(method, m -> {
			Map<String, Object> listenerConfig = new HashMap<>();
			listenerConfig.put(KEY_LISTENER, listener);
			listenerConfig.put("validateEtag", validateEtag);
			if (StringUtils.isNotEmpty(etagSessionKey)) listenerConfig.put(KEY_ETAG_KEY, etagSessionKey);
			if (StringUtils.isNotEmpty(contentTypeSessionKey)) listenerConfig.put(KEY_CONTENT_TYPE_KEY, contentTypeSessionKey);
			return listenerConfig;
		});
	}

	public void unregisterServiceClient(String uriPattern, String method) {
		uriPattern = unifyUriPattern(uriPattern);
		Map<String,Map<String,Object>> patternEntry = patternClients.get(uriPattern);
		if (patternEntry == null) {
			return;
		}
		if (StringUtils.isEmpty(method)) {
			method=WILDCARD;
		}
		patternEntry.remove(method);
		// removing patternEntry from patternClients is not thread safe
	}

	public Set<String> getUriPatterns() {
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
