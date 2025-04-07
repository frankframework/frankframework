/*
   Copyright 2017-2024 WeAreFrank!

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
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.TransformerConfigurationException;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.CanUseSharedResource;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.Resource;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Forward;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.XmlUtils;

/**
 * Sender for the HTTP protocol using {@link HttpMethod HttpMethod}. By default, any response code outside the 2xx or 3xx range
 * is considered an error and the <code>exception</code> forward of the SenderPipe is followed if present and if there
 * is no forward for the specific HTTP status code. Forwards for specific HTTP codes (e.g. "200", "201", ...)
 * are returned by this sender, so they are available to the SenderPipe.
 *
 * <p><b>Expected message format:</b></p>
 * <p>GET methods expect a message looking like this:
 * <pre>
 *    param_name=param_value&another_param_name=another_param_value
 * </pre>
 * <p>POST AND PUT methods expect a message similar as GET, or looking like this:
 * <pre>
 *   param_name=param_value
 *   another_param_name=another_param_value
 * </pre>
 *
 * @ff.info When used as MTOM sender and MTOM receiver doesn't support Content-Transfer-Encoding "base64", messages without line feeds will give an error.
 * This can be fixed by setting the Content-Transfer-Encoding in the MTOM sender.
 * @ff.info The use of `multi-value` parameters can be achieved by adding multiple {@link IParameter parameters} with the same name.
 * @ff.parameters Any parameters present are appended to the request (when method is <code>GET</code> as request-parameters, when method <code>POST</code>
 * as body part) except the <code>headersParams</code> list, which are added as HTTP headers, and the <code>urlParam</code> header
 *
 * @author	Niels Meijer
 * @since	7.0
 *
 */
@Forward(name = "*", description = "statuscode of the HTTP response")
public abstract class AbstractHttpSender extends AbstractHttpSession implements HasPhysicalDestination, ISenderWithParameters, CanUseSharedResource<HttpSession> {

	private static final String CONTEXT_KEY_STATUS_CODE = "Http.StatusCode";
	private static final String CONTEXT_KEY_REASON_PHRASE = "Http.ReasonPhrase";
	public static final String MESSAGE_ID_HEADER = "Message-Id";
	public static final String CORRELATION_ID_HEADER = "Correlation-Id";

	private final @Getter String domain = "Http";

	private @Setter String sharedResourceRef;

	private @Getter String url;
	private @Getter String urlParam = "url";

	public enum HttpMethod {
		GET, POST, PUT, PATCH, DELETE, HEAD, REPORT
	}
	private @Getter HttpMethod httpMethod = HttpMethod.GET;

	private @Getter String charSet = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private @Getter ContentType fullContentType = null;
	private @Getter String contentType = null;

	private @Getter String headersParams="";
	private @Getter boolean xhtml=false;
	private @Getter String styleSheetName=null;
	private @Getter String resultStatusCodeSessionKey;
	private @Getter String parametersToSkipWhenEmpty;

	private final boolean appendMessageidHeader = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("http.headers.messageid", true);
	private final boolean appendCorrelationidHeader = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("http.headers.correlationid", true);

	private TransformerPool transformerPool=null;

	protected IParameter urlParameter;

	protected URI staticUri;

	protected Set<String> requestOrBodyParamsSet=new HashSet<>();
	protected Set<String> headerParamsSet=new LinkedHashSet<>();
	protected Set<String> parametersToSkipWhenEmptySet=new HashSet<>();

	protected @Nonnull ParameterList paramList = new ParameterList();

	@Override
	public void addParameter(IParameter p) {
		paramList.add(p);
	}

	/**
	 * return the Parameters
	 */
	@Override
	public @Nonnull ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public void configure() throws ConfigurationException {
		if(StringUtils.isBlank(sharedResourceRef)) {
			log.debug("configuring local HttpSession");
			super.configure();
		}

		paramList.configure();

		if (StringUtils.isNotEmpty(getHeadersParams())) {
			headerParamsSet.addAll(StringUtil.split(getHeadersParams()));
		}
		for (IParameter p: paramList) {
			String paramName = p.getName();
			if (!headerParamsSet.contains(paramName)) {
				requestOrBodyParamsSet.add(paramName);
			}
		}

		if (StringUtils.isNotEmpty(getUrlParam())) {
			headerParamsSet.remove(getUrlParam());
			requestOrBodyParamsSet.remove(getUrlParam());
			urlParameter = paramList.findParameter(getUrlParam());
		}

		if (StringUtils.isNotEmpty(getParametersToSkipWhenEmpty())) {
			if (getParametersToSkipWhenEmpty().equals("*")) {
				parametersToSkipWhenEmptySet.addAll(headerParamsSet);
				parametersToSkipWhenEmptySet.addAll(requestOrBodyParamsSet);
			} else {
				parametersToSkipWhenEmptySet.addAll(StringUtil.split(getParametersToSkipWhenEmpty()));
			}
		}

		if(StringUtils.isNotEmpty(getContentType())) {
			fullContentType = ContentType.parse(getContentType());
			if(fullContentType.getCharset() == null) {
				fullContentType = fullContentType.withCharset(getCharSet());
			}
		}

		try {
			if (urlParameter == null) {
				if (StringUtils.isEmpty(getUrl())) {
					throw new ConfigurationException("url must be specified, either as attribute, or as parameter");
				}
				staticUri = getURI(getUrl());
			}
		} catch (URISyntaxException e) {
			throw new ConfigurationException("cannot interpret url ["+getUrl()+"]", e);
		}

		if (StringUtils.isNotEmpty(getStyleSheetName())) {
			try {
				Resource stylesheet = Resource.getResource(this, getStyleSheetName());
				if (stylesheet == null) {
					throw new ConfigurationException("cannot find stylesheet ["+getStyleSheetName()+"]");
				}
				transformerPool = TransformerPool.getInstance(stylesheet);
			} catch (IOException e) {
				throw new ConfigurationException("cannot retrieve ["+ getStyleSheetName() + "]", e);
			} catch (TransformerConfigurationException te) {
				throw new ConfigurationException("got error creating transformer from file [" + getStyleSheetName() + "]", te);
			}
		}
	}

	@Override
	public Class<HttpSession> getObjectType() {
		return HttpSession.class;
	}

	@Override
	public void start() {
		if (StringUtils.isNotBlank(sharedResourceRef)) {
			try {

				HttpSession session = getSharedResource(sharedResourceRef);
				setHttpClient(session.getHttpClient());
				setHttpContext(session.getDefaultHttpClientContext());
			} catch (Exception e) {
				throw new LifecycleException("unable to create HttpClient", e);
			}
		} else {
			log.debug("starting local HttpSession");
			super.start();

			if (transformerPool != null) {
				try {
					transformerPool.open();
				} catch (Exception e) {
					throw new LifecycleException("cannot start TransformerPool", e);
				}
			}
		}
	}

	@Override
	public void stop() {
		if(StringUtils.isBlank(sharedResourceRef)) {
			super.stop();
		}

		if (transformerPool != null) {
			transformerPool.close();
		}
	}

	protected boolean appendParameters(boolean parametersAppended, StringBuilder path, ParameterValueList parameters) throws SenderException {
		if (parameters != null) {
			log.debug("appending [{}] parameters", parameters::size);
			for(ParameterValue pv : parameters) {
				if (requestOrBodyParamsSet.contains(pv.getName())) {
					String value = pv.asStringValue("");
					if (StringUtils.isNotEmpty(value) || !parametersToSkipWhenEmptySet.contains(pv.getName())) {
						try {
							if (parametersAppended) {
								path.append("&");
							} else {
								path.append("?");
								parametersAppended = true;
							}

							String parameterToAppend = pv.getDefinition().getName() +"="+ URLEncoder.encode(value, getCharSet());
							log.debug("appending parameter [{}]", parameterToAppend);
							path.append(parameterToAppend);
						} catch (UnsupportedEncodingException e) {
							throw new SenderException("["+getCharSet()+"] encoding error. Failed to add parameter ["+pv.getDefinition().getName()+"]", e);
						}
					}
				}
			}
		}
		return parametersAppended;
	}

	/**
	 * Returns the true name of the class and not <code>XsltPipe$$EnhancerBySpringCGLIB$$563e6b5d</code>.
	 * {@link ClassUtils#nameOf(Object)} makes sure the original class will be used.
	 *
	 * @return className + name of the ISender
	 */
	protected String getLogPrefix() {
		return ClassUtils.nameOf(this) + " ";
	}

	/**
	 * Custom implementation to create a {@link HttpRequestBase HttpRequest} object.
	 * @param uri endpoint to send the message to
	 * @param message to be sent
	 * @param parameters ParameterValueList that contains all the senders parameters
	 * @param session PipeLineSession to retrieve or store data from, or NULL when not set
	 * @return a {@link HttpRequestBase HttpRequest} object
	 * @throws SenderException
	 */
	protected abstract HttpRequestBase getMethod(URI uri, Message message, @Nonnull ParameterValueList parameters, PipeLineSession session) throws SenderException;

	/**
	 * Custom implementation to extract the response and format it to a String result. <br/>
	 * It is important that the {@link HttpResponseHandler#getResponse() response}
	 * will be read or will be {@link HttpResponseHandler#close() closed}.
	 * @param responseHandler {@link HttpResponseHandler} that contains the response information
	 * @param session {@link PipeLineSession} which may be null
	 * @return a string that will be passed to the pipeline
	 */
	protected abstract Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws SenderException, IOException;

	protected boolean validateResponseCode(int statusCode) {
		boolean ok = false;
		if (StringUtils.isNotEmpty(getResultStatusCodeSessionKey())) {
			ok = true;
		} else {
			if (statusCode==200 || statusCode==201 || statusCode==202 || statusCode==204 || statusCode==206) {
				ok = true;
			} else {
				if (isIgnoreRedirects() && (statusCode==HttpServletResponse.SC_MOVED_PERMANENTLY || statusCode==HttpServletResponse.SC_MOVED_TEMPORARILY || statusCode==HttpServletResponse.SC_TEMPORARY_REDIRECT)) {
					ok = true;
				}
			}
		}
		return ok;
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		ParameterValueList pvl;
		try {
			pvl = paramList.getValues(message, session);
		} catch (ParameterException e) {
			throw new SenderException("caught exception evaluating parameters", e);
		}

		URI targetUri;
		final HttpRequestBase httpRequestBase;
		try {
			if (urlParameter != null) {
				String url = pvl.get(getUrlParam()).asStringValue();
				try {
					targetUri = getURI(url);
				} catch (URISyntaxException e) {
					throw new SenderException("cannot interpret url", e);
				}
			} else {
				targetUri = staticUri;
			}

			// Resolve HeaderParameters
			Map<String, String> headersParamsMap = new HashMap<>();
			if (!headerParamsSet.isEmpty()) {
				log.debug("appending header parameters {}", headersParams);
				for (String paramName:headerParamsSet) {
					ParameterValue paramValue = pvl.get(paramName);
					if(paramValue != null) {
						String value = paramValue.asStringValue(null);
						if (StringUtils.isNotEmpty(value) || !parametersToSkipWhenEmptySet.contains(paramName)) {
							headersParamsMap.put(paramName, value);
						}
					}
				}
			}

			httpRequestBase = getMethod(targetUri, message, pvl, session);
			if(httpRequestBase == null)
				throw new MethodNotSupportedException("could not find implementation for method ["+getHttpMethod()+"]");

			// Set all headers
			if (appendMessageidHeader && StringUtils.isNotEmpty(session.getMessageId())) {
				httpRequestBase.setHeader(MESSAGE_ID_HEADER, session.getMessageId());
			}
			if (appendCorrelationidHeader && StringUtils.isNotEmpty(session.getCorrelationId())) {
				httpRequestBase.setHeader(CORRELATION_ID_HEADER, session.getCorrelationId());
			}
			for (Map.Entry<String, String> param: headersParamsMap.entrySet()) {
				httpRequestBase.setHeader(param.getKey(), param.getValue());
			}

			log.info("configured httpclient for host [{}]", targetUri::getHost);

		} catch (Exception e) {
			throw new SenderException(e);
		}

		Message result;
		int statusCode;
		boolean success;
		String reasonPhrase;

		TimeoutGuard tg = new TimeoutGuard(1+getTimeout()/1000, getName()) {
			@Override
			protected void abort() {
				httpRequestBase.abort();
			}
		};

		try {
			log.debug("executing method [{}]", httpRequestBase::getRequestLine);
			HttpResponse httpResponse = execute(targetUri, httpRequestBase, session);
			log.debug("executed method");

			HttpResponseHandler responseHandler = new HttpResponseHandler(httpResponse);
			StatusLine statusline = httpResponse.getStatusLine();
			statusCode = statusline.getStatusCode();
			success = validateResponseCode(statusCode);
			reasonPhrase = StringUtils.isNotEmpty(statusline.getReasonPhrase()) ? statusline.getReasonPhrase() : "HTTP status-code ["+statusCode+"]";

			if (StringUtils.isNotEmpty(getResultStatusCodeSessionKey())) {
				session.put(getResultStatusCodeSessionKey(), Integer.toString(statusCode));
			}

			// Only give warnings for 4xx (client errors) and 5xx (server errors)
			if (isErrorStatus(statusCode)) {
				log.warn("status [{}]", statusline);
			} else {
				log.debug("status [{}]", statusCode);
			}

			result = extractResult(responseHandler, session);

			log.debug("retrieved result [{}]", result);
		} catch (IOException e) {
			httpRequestBase.abort();
			if (e instanceof SocketTimeoutException) {
				throw new TimeoutException(e);
			}
			throw new SenderException(e);
		} finally {
			// By forcing the use of the HttpResponseHandler the resultStream
			// will automatically be closed when it has been read.
			// See HttpResponseHandler and ReleaseConnectionAfterReadInputStream.
			// We cannot close the connection as the response might be kept
			// in a sessionKey for later use in the pipeline.
			//
			// IMPORTANT: It is possible that poorly written implementations
			// won't read or close the response.
			// This will cause the connection to become stale.

			if (tg.cancel()) {
				throw new TimeoutException("timeout of ["+getTimeout()+"] ms exceeded");
			}
		}

		if (statusCode == -1){
			throw new SenderException("Failed to recover from exception");
		}

		if (isXhtml() && !Message.isEmpty(result)) {
			Message xhtml;
			try (Message m = result) {
				xhtml = XmlUtils.toXhtml(m);
			} catch (IOException e) {
				throw new SenderException("error reading http response as String", e);
			}

			if (transformerPool != null && !xhtml.isEmpty()) {
				log.debug("transforming result [{}]", xhtml);
				try {
					xhtml = transformerPool.transform(xhtml);
				} catch (Exception e) {
					throw new SenderException("Exception on transforming input", e);
				}
			}

			result = xhtml;
		}

		if (result == null) {
			result = Message.nullMessage();
		}
		log.debug("Storing [{}]=[{}], [{}]=[{}]", CONTEXT_KEY_STATUS_CODE, statusCode, CONTEXT_KEY_REASON_PHRASE, reasonPhrase);
		result.getContext().put(CONTEXT_KEY_STATUS_CODE, statusCode);
		result.getContext().put(CONTEXT_KEY_REASON_PHRASE, reasonPhrase);

		return new SenderResult(success, result, isErrorStatus(statusCode) ? reasonPhrase : "", Integer.toString(statusCode));
	}

	private boolean isErrorStatus(int statusCode) {
		return statusCode >= 400 && statusCode < 600;
	}

	@Override
	public String getPhysicalDestinationName() {
		if (urlParameter!=null) {
			return "dynamic url";
		}
		return getUrl();
	}

	/** URL or base of URL to be used. Expects all parts of the URL to already be encoded. */
	public void setUrl(String string) {
		url = string;
	}

	/**
	 * Parameter that is used to obtain URL; overrides url-attribute.
	 * @ff.default url
	 */
	public void setUrlParam(String urlParam) {
		this.urlParam = urlParam;
	}

	/**
	 * The HTTP Method used to execute the request
	 * @ff.default <code>GET</code>
	 */
	public void setMethodType(HttpMethod method) {
		this.httpMethod = method;
	}

	/**
	 * Content-Type (superset of mimetype + charset) of the request, for <code>POST</code>, <code>PUT</code> and <code>PATCH</code> methods
	 * @ff.default text/html, when postType=<code>RAW</code>
	 */
	public void setContentType(String string) {
		contentType = string;
	}

	/**
	 * Charset of the request. Typically only used on <code>PUT</code> and <code>POST</code> requests.
	 * @ff.default UTF-8
	 */
	public void setCharSet(String string) {
		charSet = string;
	}

	/** Comma separated list of parameter names which should be set as HTTP headers */
	public void setHeadersParams(String headersParams) {
		this.headersParams = headersParams;
	}

	/** Comma separated list of parameter names that should not be added as request or body parameter, or as HTTP header, if they are empty. Set to '*' for this behaviour for all parameters */
	public void setParametersToSkipWhenEmpty(String parametersToSkipWhenEmpty) {
		this.parametersToSkipWhenEmpty = parametersToSkipWhenEmpty;
	}

	/**
	 * If <code>true</code>, the HTML response is transformed to XHTML
	 * @ff.default false
	 */
	public void setXhtml(boolean xHtml) {
		xhtml = xHtml;
	}

	/** (Only used when xHtml=<code>true</code>) stylesheet to apply to the HTML response */
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}

	/** If set, the status code of the HTTP response is put in the specified sessionKey and the (error or okay) response message is returned.
	 * Setting this property has a side effect. If a 4xx or 5xx result code is returned and if the configuration does not implement
	 * the specific forward for the returned HTTP result code, then the success forward is followed instead of the exception forward.
	 */
	public void setResultStatusCodeSessionKey(String resultStatusCodeSessionKey) {
		this.resultStatusCodeSessionKey = resultStatusCodeSessionKey;
	}
}
