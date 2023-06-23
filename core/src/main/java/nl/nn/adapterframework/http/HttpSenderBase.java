/*
   Copyright 2017-2023 WeAreFrank!

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
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sender for the HTTP protocol using GET, POST, PUT or DELETE using httpclient 4+
 *
 * <p><b>Expected message format:</b></p>
 * <p>GET methods expect a message looking like this</p>
 * <pre>
 *   param_name=param_value&another_param_name=another_param_value
 * </pre>
 * <p>POST AND PUT methods expect a message similar as GET, or looking like this</p>
 * <pre>
 *   param_name=param_value
 *   another_param_name=another_param_value
 * </pre>
 *
 * @ff.parameters Any parameters present are appended to the request (when method is <code>GET</code> as request-parameters, when method <code>POST</code> as body part) except the <code>headersParams</code> list, which are added as HTTP headers, and the <code>urlParam</code> header
 * @ff.forward "&lt;statusCode of the HTTP response&gt;" default
 *
 * @author	Niels Meijer
 * @since	7.0
 */
//TODO: Fix javadoc!

public abstract class HttpSenderBase extends HttpSessionBase implements ISenderWithParameters {

	private static final String CONTEXT_KEY_STATUS_CODE = "Http.StatusCode";
	private static final String CONTEXT_KEY_REASON_PHRASE = "Http.ReasonPhrase";
	public static final String MESSAGE_ID_HEADER = "Message-Id";
	public static final String CORRELATION_ID_HEADER = "Correlation-Id";

	private final @Getter(onMethod = @__(@Override)) String domain = "Http";
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private @Getter @Setter String name;
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter String url;
	private @Getter String urlParam = "url";

	public enum HttpMethod {
		GET,POST,PUT,PATCH,DELETE,HEAD,REPORT;
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

	private final boolean APPEND_MESSAGEID_HEADER = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("http.headers.messageid", true);
	private final boolean APPEND_CORRELATIONID_HEADER = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("http.headers.correlationid", true);

	private TransformerPool transformerPool=null;

	protected Parameter urlParameter;

	protected URI staticUri;

	protected Set<String> requestOrBodyParamsSet=new HashSet<>();
	protected Set<String> headerParamsSet=new LinkedHashSet<>();
	protected Set<String> parametersToSkipWhenEmptySet=new HashSet<>();

	protected ParameterList paramList = null;

	@Override
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	/**
	 * return the Parameters
	 */
	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (paramList!=null) {
			paramList.configure();

			if (StringUtils.isNotEmpty(getHeadersParams())) {
				StringTokenizer st = new StringTokenizer(getHeadersParams(), ",");
				while (st.hasMoreElements()) {
					String paramName = st.nextToken().trim();
					headerParamsSet.add(paramName);
				}
			}
			for (Parameter p: paramList) {
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
					StringTokenizer st = new StringTokenizer(getParametersToSkipWhenEmpty(), ",");
					while (st.hasMoreElements()) {
						String paramName = st.nextToken().trim();
						parametersToSkipWhenEmptySet.add(paramName);
					}
				}
			}
		}

		if(StringUtils.isNotEmpty(getContentType())) {
			fullContentType = ContentType.parse(getContentType());
			if(fullContentType != null && fullContentType.getCharset() == null) {
				fullContentType = fullContentType.withCharset(getCharSet());
			}
		}

		try {
			if (urlParameter == null) {
				if (StringUtils.isEmpty(getUrl())) {
					throw new ConfigurationException(getLogPrefix()+"url must be specified, either as attribute, or as parameter");
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
	public void open() throws SenderException {
		try {
			super.open();
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"unable to create HttpClient", e);
		}

		if (transformerPool!=null) {
			try {
				transformerPool.open();
			} catch (Exception e) {
				throw new SenderException(getLogPrefix()+"cannot start TransformerPool", e);
			}
		}
	}

	@Override
	public void close() {
		super.close();

		if (transformerPool!=null) {
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
							throw new SenderException(getLogPrefix()+"["+getCharSet()+"] encoding error. Failed to add parameter ["+pv.getDefinition().getName()+"]", e);
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
	protected abstract HttpRequestBase getMethod(URI uri, Message message, ParameterValueList parameters, PipeLineSession session) throws SenderException;

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
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		ParameterValueList pvl = null;
		try {
			if (paramList !=null) {
				pvl=paramList.getValues(message, session);
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"Sender ["+getName()+"] caught exception evaluating parameters",e);
		}

		URI targetUri;
		final HttpRequestBase httpRequestBase;
		try {
			if (urlParameter != null) {
				String url = pvl.get(getUrlParam()).asStringValue();
				targetUri = getURI(url);
			} else {
				targetUri = staticUri;
			}

			// Resolve HeaderParameters
			Map<String, String> headersParamsMap = new HashMap<>();
			if (!headerParamsSet.isEmpty() && pvl!=null) {
				log.debug("appending header parameters "+headersParams);
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

			//Set all headers
			if(session != null) {
				if (APPEND_MESSAGEID_HEADER && StringUtils.isNotEmpty(session.getMessageId())) {
					httpRequestBase.setHeader(MESSAGE_ID_HEADER, session.getMessageId());
				}
				if (APPEND_CORRELATIONID_HEADER && StringUtils.isNotEmpty(session.getCorrelationId())) {
					httpRequestBase.setHeader(CORRELATION_ID_HEADER, session.getCorrelationId());
				}
			}
			for (String param: headersParamsMap.keySet()) {
				httpRequestBase.setHeader(param, headersParamsMap.get(param));
			}

			preAuthenticate();

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
			HttpResponse httpResponse = execute(targetUri, httpRequestBase);
			log.debug("executed method");

			HttpResponseHandler responseHandler = new HttpResponseHandler(httpResponse);
			StatusLine statusline = httpResponse.getStatusLine();
			statusCode = statusline.getStatusCode();
			success = validateResponseCode(statusCode);
			reasonPhrase =  statusline.getReasonPhrase();

			if (StringUtils.isNotEmpty(getResultStatusCodeSessionKey()) && session != null) {
				session.put(getResultStatusCodeSessionKey(), Integer.toString(statusCode));
			}

			// Only give warnings for 4xx (client errors) and 5xx (server errors)
			if (statusCode >= 400 && statusCode < 600) {
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
				throw new TimeoutException(getLogPrefix()+"timeout of ["+getTimeout()+"] ms exceeded");
			}
		}

		if (statusCode == -1){
			throw new SenderException("Failed to recover from exception");
		}

		if (isXhtml() && !Message.isEmpty(result)) {
			// TODO: Streaming XHTML conversion for better performance with large result message?
			String xhtml;
			try(Message m = result) {
				xhtml = XmlUtils.toXhtml(m);
			} catch (IOException e) {
				throw new SenderException("error reading http response as String", e);
			}

			if (transformerPool != null && xhtml != null) {
				log.debug("transforming result [{}]", xhtml);
				try {
					xhtml = transformerPool.transform(Message.asSource(xhtml));
				} catch (Exception e) {
					throw new SenderException("Exception on transforming input", e);
				}
			}

			result = Message.asMessage(xhtml);
		}

		if (result == null) {
			result = Message.nullMessage();
		}
		log.debug("Storing [{}]=[{}], [{}]=[{}]", CONTEXT_KEY_STATUS_CODE, statusCode, CONTEXT_KEY_REASON_PHRASE, reasonPhrase);
		result.getContext().put(CONTEXT_KEY_STATUS_CODE, statusCode);
		result.getContext().put(CONTEXT_KEY_REASON_PHRASE, reasonPhrase);
		return new SenderResult(success, result, reasonPhrase, Integer.toString(statusCode));
	}

	@Override
	public String getPhysicalDestinationName() {
		if (urlParameter!=null) {
			return "dynamic url";
		}
		return getUrl();
	}

	/** URL or base of URL to be used */
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

	@Deprecated
	@ConfigurationWarning("Please use attribute username instead")
	public void setUserName(String username) {
		setUsername(username);
	}

	@Deprecated
	@ConfigurationWarning("Please use \"proxyUsername\" instead")
	public void setProxyUserName(String string) {
		setProxyUsername(string);
	}

	@Deprecated
	@ConfigurationWarning("Please use attribute keystore instead")
	public void setCertificate(String string) {
		setKeystore(string);
	}
	@Deprecated
	@ConfigurationWarning("has been replaced with keystoreType")
	public void setCertificateType(KeystoreType value) {
		setKeystoreType(value);
	}
	@Deprecated
	@ConfigurationWarning("Please use attribute keystoreAuthAlias instead")
	public void setCertificateAuthAlias(String string) {
		setKeystoreAuthAlias(string);
	}
	@Deprecated
	@ConfigurationWarning("Please use attribute keystorePassword instead")
	public void setCertificatePassword(String string) {
		setKeystorePassword(string);
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
