/*
   Copyright 2017-2020 WeAreFrank!

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
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.senders.SenderWithParametersBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sender for the HTTP protocol using GET, POST, PUT or DELETE using httpclient 4+
 * 
 * <p><b>Parameters:</b></p>
 * <p>Any parameters present are appended to the request as request-parameters except the headersParams list which are added as http headers</p>
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
 * <p>
 * Note 1:
 * Some certificates require the &lt;java_home&gt;/jre/lib/security/xxx_policy.jar files to be upgraded to unlimited strength. Typically, in such a case, an error message like 
 * <code>Error in loading the keystore: Private key decryption error: (java.lang.SecurityException: Unsupported keysize or algorithm parameters</code> is observed.
 * For IBM JDKs these files can be downloaded from http://www.ibm.com/developerworks/java/jdk/security/50/ (scroll down to 'IBM SDK Policy files')
 * </p>
 * Replace in the directory java\jre\lib\security the following files:
 * <ul>
 * <li>local_policy.jar</li>
 * <li>US_export_policy.jar</li>
 * </ul>
 * <p>
 * Note 2:
 * To debug ssl-related problems, set the following system property:
 * <ul>
 * <li>IBM / WebSphere: <code>-Djavax.net.debug=true</code></li>
 * <li>SUN: <code>-Djavax.net.debug=all</code></li>
 * </ul>
 * </p>
 * <p>
 * Note 3:
 * In case <code>javax.net.ssl.SSLHandshakeException: unknown certificate</code>-exceptions are thrown, 
 * probably the certificate of the other party is not trusted. Try to use one of the certificates in the path as your truststore by doing the following:
 * <ul>
 *   <li>open the URL you are trying to reach in InternetExplorer</li>
 *   <li>click on the yellow padlock on the right in the bottom-bar. This opens the certificate information window</li>
 *   <li>click on tab 'Certificeringspad'</li>
 *   <li>double click on root certificate in the tree displayed. This opens the certificate information window for the root certificate</li>
 *   <li>click on tab 'Details'</li>
 *   <li>click on 'Kopieren naar bestand'</li>
 *   <li>click 'next', choose 'DER Encoded Binary X.509 (.CER)'</li>
 *   <li>click 'next', choose a filename</li>
 *   <li>click 'next' and 'finish'</li>
 * 	 <li>Start IBM key management tool ikeyman.bat, located in Program Files/IBM/WebSphere Studio/Application Developer/v5.1.2/runtimes/base_v51/bin (or similar)</li>
 *   <li>create a new key-database (Sleuteldatabase -> Nieuw...), or open the default key.jks (default password="changeit")</li>
 *   <li>add the generated certificate (Toevoegen...)</li>
 *   <li>store the key-database in JKS format</li>
 *   <li>if you didn't use the standard keydatabase, then reference the file in the truststore-attribute in Configuration.xml (include the file as a resource)</li>
 *   <li>use jks for the truststoreType-attribute</li>
 *   <li>restart your application</li>
 *   <li>instead of IBM ikeyman you can use the standard java tool <code>keytool</code> as follows: 
 *      <code>keytool -import -alias <i>yourAlias</i> -file <i>pathToSavedCertificate</i></code></li>
 * </ul>
 * <p>
 * Note 4:
 * In case <code>cannot create or initialize SocketFactory: (IOException) Unable to verify MAC</code>-exceptions are thrown,
 * please check password or authAlias configuration of the corresponding certificate. 
 * </p>
 * 
 * @author	Niels Meijer
 * @since	7.0
 */
//TODO: Fix javadoc!

public abstract class HttpSenderBase extends SenderWithParametersBase implements HasPhysicalDestination {

	private String url;
	private String urlParam = "url";
	private String methodType = "GET";
	private String charSet = Misc.DEFAULT_INPUT_STREAM_ENCODING;
	private ContentType fullContentType = null;
	private String contentType = null;

	/** CONNECTION POOL **/
	private int timeout = 10000;
	private int maxConnections = 10;
	private int maxExecuteRetries = 1;
	private SSLConnectionSocketFactory sslSocketFactory = null;
	private HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
	private HttpClientContext httpClientContext = HttpClientContext.create();
	private CloseableHttpClient httpClient;

	/** SECURITY */
	private String authAlias;
	private String userName;
	private String password;
	private String authDomain;

	/** PROXY **/
	private String proxyHost;
	private int    proxyPort=80;
	private String proxyAuthAlias;
	private String proxyUserName;
	private String proxyPassword;
	private String proxyRealm=null;

	/** SSL **/
	private String certificate;
	private String certificateAuthAlias;
	private String certificatePassword;
	private String keystoreType="pkcs12";
	private String keyManagerAlgorithm=null;
	private String truststore=null;
	private String truststoreAuthAlias;
	private String truststorePassword=null;
	private String truststoreType="jks";
	private String trustManagerAlgorithm=null;
	private boolean allowSelfSignedCertificates = false;
	private boolean verifyHostname=true;
	private boolean ignoreCertificateExpiredException=false;

	private String headersParams="";
	private boolean followRedirects=true;
	private boolean staleChecking=true;
	private int staleTimeout = 5000;
	private boolean xhtml=false;
	private String styleSheetName=null;
	private String protocol=null;
	private String resultStatusCodeSessionKey;
	private final boolean APPEND_MESSAGEID_HEADER = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("http.headers.messageid", true);
	private boolean disableCookies = false;

	private TransformerPool transformerPool=null;

	protected Parameter urlParameter;

	protected URI staticUri;
	private CredentialFactory credentials;

	private Set<String> parametersToSkip=new HashSet<String>();

	protected void addParameterToSkip(String parameterName) {
		if (parameterName != null) {
			parametersToSkip.add(parameterName);
		}
	}

	protected boolean skipParameter(String parameterName) {
		for(String param : parametersToSkip) {
			if(param.equalsIgnoreCase(parameterName))
				return true;
		}

		return false;
	}

	/**
	 * Makes sure only http(s) requests can be performed.
	 */
	protected URI getURI(String url) throws URISyntaxException {
		URIBuilder uri = new URIBuilder(url);

		if(uri.getScheme() == null) {
			throw new URISyntaxException("", "must use an absolute url starting with http(s)://");
		}
		if (!uri.getScheme().matches("(?i)https?")) {
			throw new IllegalArgumentException(ClassUtils.nameOf(this) + " only supports web based schemes. (http or https)");
		}

		if (uri.getPath()==null) {
			uri.setPath("/");
		}

		log.info(getLogPrefix()+"created uri: scheme=["+uri.getScheme()+"] host=["+uri.getHost()+"] path=["+uri.getPath()+"]");
		return uri.build();
	}

	protected int getPort(URI uri) {
		int port = uri.getPort();
		if (port<1) {
			try {
				URL url = uri.toURL();
				port = url.getDefaultPort();
				log.debug(getLogPrefix()+"looked up protocol for scheme ["+uri.getScheme()+"] to be port ["+port+"]");
			} catch (Exception e) {
				log.debug(getLogPrefix()+"protocol for scheme ["+uri.getScheme()+"] not found, setting port to 80",e);
				port=80; 
			}
		}
		return port;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		/**
		 * TODO find out if this really breaks proxy authentication or not.
		 */
//		httpClientBuilder.disableAuthCaching();
		httpClientBuilder.disableAutomaticRetries();

		Builder requestConfig = RequestConfig.custom();
		requestConfig.setConnectTimeout(getTimeout());
		requestConfig.setConnectionRequestTimeout(getTimeout());
		requestConfig.setSocketTimeout(getTimeout());

		if (paramList!=null) {
			paramList.configure();
			if (StringUtils.isNotEmpty(getUrlParam())) {
				urlParameter = paramList.findParameter(getUrlParam());
				if(urlParameter != null)
					addParameterToSkip(urlParameter.getName());
			}

			//Add all HeaderParams to paramIgnoreList
			StringTokenizer st = new StringTokenizer(getHeadersParams(), ",");
			while (st.hasMoreElements()) {
				addParameterToSkip(st.nextToken());
			}
		}

		if(StringUtils.isNotEmpty(getContentType())) {
			fullContentType = ContentType.parse(getContentType());
			if(fullContentType != null && fullContentType.getCharset() == null) {
				fullContentType = fullContentType.withCharset(getCharSet());
			}
		}

		if (getMaxConnections() <= 0) {
			throw new ConfigurationException(getLogPrefix()+"maxConnections is set to ["+getMaxConnections()+"], which is not enough for adequate operation");
		}
		try {
			if (urlParameter == null) {
				if (StringUtils.isEmpty(getUrl())) {
					throw new ConfigurationException(getLogPrefix()+"url must be specified, either as attribute, or as parameter");
				}
				staticUri = getURI(getUrl());
			}

			URL certificateUrl = null;
			URL truststoreUrl = null;
	
			if (!StringUtils.isEmpty(getCertificate())) {
				certificateUrl = ClassUtils.getResourceURL(getConfigurationClassLoader(), getCertificate());
				if (certificateUrl == null) {
					throw new ConfigurationException(getLogPrefix()+"cannot find URL for certificate resource ["+getCertificate()+"]");
				}
				log.info(getLogPrefix()+"resolved certificate-URL to ["+certificateUrl.toString()+"]");
			}
			if (!StringUtils.isEmpty(getTruststore())) {
				truststoreUrl = ClassUtils.getResourceURL(getConfigurationClassLoader(), getTruststore());
				if (truststoreUrl == null) {
					throw new ConfigurationException(getLogPrefix()+"cannot find URL for truststore resource ["+getTruststore()+"]");
				}
				log.info(getLogPrefix()+"resolved truststore-URL to ["+truststoreUrl.toString()+"]");
			}

			HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();
			if(!isVerifyHostname())
				hostnameVerifier = new NoopHostnameVerifier();

			// Add javax.net.ssl.SSLSocketFactory.getDefault() SSLSocketFactory if non has been set.
			// See: http://httpcomponents.10934.n7.nabble.com/Upgrading-commons-httpclient-3-x-to-HttpClient4-x-td19333.html
			// 
			// The first time this method is called, the security property "ssl.SocketFactory.provider" is examined. 
			// If it is non-null, a class by that name is loaded and instantiated. If that is successful and the 
			// object is an instance of SSLSocketFactory, it is made the default SSL socket factory.
			// Otherwise, this method returns SSLContext.getDefault().getSocketFactory(). If that call fails, an inoperative factory is returned.
			javax.net.ssl.SSLSocketFactory socketfactory = (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
			sslSocketFactory = new SSLConnectionSocketFactory(socketfactory, hostnameVerifier);

			if (certificateUrl != null || truststoreUrl != null || isAllowSelfSignedCertificates()) {
				try {
					CredentialFactory certificateCf = new CredentialFactory(getCertificateAuthAlias(), null, getCertificatePassword());
					CredentialFactory truststoreCf  = new CredentialFactory(getTruststoreAuthAlias(),  null, getTruststorePassword());

					SSLContext sslContext = AuthSSLConnectionSocket.createSSLContext(
							certificateUrl, certificateCf.getPassword(), getKeystoreType(), getKeyManagerAlgorithm(),
							truststoreUrl, truststoreCf.getPassword(), getTruststoreType(), getTrustManagerAlgorithm(),
							isAllowSelfSignedCertificates(), isVerifyHostname(), isIgnoreCertificateExpiredException(), getProtocol());

					sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
					log.debug(getLogPrefix()+"created custom SSLConnectionSocketFactory");

				} catch (Throwable t) {
					throw new ConfigurationException(getLogPrefix()+"cannot create or initialize SocketFactory",t);
				}
			}

			// This method will be overwritten by the connectionManager when connectionPooling is enabled!
			// Can still be null when no default or an invalid system sslSocketFactory has been defined
			if(sslSocketFactory != null)
				httpClientBuilder.setSSLSocketFactory(sslSocketFactory);

			credentials = new CredentialFactory(getAuthAlias(), getUserName(), getPassword());
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			if (!StringUtils.isEmpty(credentials.getUsername())) {
				String uname;
				if (StringUtils.isNotEmpty(getAuthDomain())) {
					uname = getAuthDomain() + "\\" + credentials.getUsername();
				} else {
					uname = credentials.getUsername();
				}

				credentialsProvider.setCredentials(
					new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), 
					new UsernamePasswordCredentials(uname, credentials.getPassword())
				);

				requestConfig.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC));
				requestConfig.setAuthenticationEnabled(true);
			}
			if (StringUtils.isNotEmpty(getProxyHost())) {
				HttpHost proxy = new HttpHost(getProxyHost(), getProxyPort());
				AuthScope scope = new AuthScope(proxy, getProxyRealm(), AuthScope.ANY_SCHEME);

				CredentialFactory pcf = new CredentialFactory(getProxyAuthAlias(), getProxyUserName(), getProxyPassword());

				if (StringUtils.isNotEmpty(pcf.getUsername())) {
					Credentials credentials = new UsernamePasswordCredentials(pcf.getUsername(), pcf.getPassword());
					credentialsProvider.setCredentials(scope, credentials);
				}
				log.trace("setting credentialProvider [" + credentialsProvider.toString() + "]");

				if(prefillProxyAuthCache()) {
					requestConfig.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC));

					AuthCache authCache = httpClientContext.getAuthCache();
					if(authCache == null)
						authCache = new BasicAuthCache();
	
					authCache.put(proxy, new BasicScheme());
					httpClientContext.setAuthCache(authCache);
				}

				requestConfig.setProxy(proxy);
				httpClientBuilder.setProxy(proxy);
			}

			httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		} catch (URISyntaxException e) {
			throw new ConfigurationException(getLogPrefix()+"cannot interpret uri ["+getUrl()+"]", e);
		}

		if (StringUtils.isNotEmpty(getStyleSheetName())) {
			try {
				Resource stylesheet = Resource.getResource(getConfigurationClassLoader(), getStyleSheetName());
				if (stylesheet == null) {
					throw new ConfigurationException(getLogPrefix() + "cannot find stylesheet ["+getStyleSheetName()+"]");
				}
				transformerPool = TransformerPool.getInstance(stylesheet);
			} catch (IOException e) {
				throw new ConfigurationException(getLogPrefix() + "cannot retrieve ["+ getStyleSheetName() + "]", e);
			} catch (TransformerConfigurationException te) {
				throw new ConfigurationException(getLogPrefix() + "got error creating transformer from file [" + getStyleSheetName() + "]", te);
			}
		}

		httpClientBuilder.setDefaultRequestConfig(requestConfig.build());

		if(areCookiesDisabled()) {
			httpClientBuilder.disableCookieManagement();
		}

		// The redirect strategy used to only redirect GET, DELETE and HEAD.
		httpClientBuilder.setRedirectStrategy(new DefaultRedirectStrategy() {
			@Override
			protected boolean isRedirectable(String method) {
				return isFollowRedirects();
			}
		});
	}

	@Override
	public void open() throws SenderException {
		// In order to support multiThreading and connectionPooling
		// If a sslSocketFactory has been defined, the connectionManager has to be initialized with the sslSocketFactory
		PoolingHttpClientConnectionManager connectionManager;
		if(sslSocketFactory != null) {
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", sslSocketFactory)
				.build();
			connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			log.debug(getLogPrefix()+"created PoolingHttpClientConnectionManager with custom SSLConnectionSocketFactory");
		}
		else {
			connectionManager = new PoolingHttpClientConnectionManager();
			log.debug(getLogPrefix()+"created default PoolingHttpClientConnectionManager");
		}

		connectionManager.setMaxTotal(getMaxConnections());
		connectionManager.setDefaultMaxPerRoute(getMaxConnections());

		log.debug(getLogPrefix()+"set up connectionManager, inactivity checking ["+connectionManager.getValidateAfterInactivity()+"]");
		boolean staleChecking = (connectionManager.getValidateAfterInactivity() >= 0);
		if (staleChecking != isStaleChecking()) {
			log.info(getLogPrefix()+"set up connectionManager, setting stale checking ["+isStaleChecking()+"]");
			connectionManager.setValidateAfterInactivity(getStaleTimeout());
		}

		httpClientBuilder.setConnectionManager(connectionManager);

		if (transformerPool!=null) {
			try {
				transformerPool.open();
			} catch (Exception e) {
				throw new SenderException(getLogPrefix()+"cannot start TransformerPool", e);
			}
		}

		httpClient = httpClientBuilder.build();
	}

	public CloseableHttpClient getHttpClient() {
		return httpClient;
	}

	@Override
	public void close() throws SenderException {
		try {
			//Close the HttpClient and ConnectionManager to release resources and potential open connections
			if(httpClient != null) {
				httpClient.close();
			}
		} catch (IOException e) {
			throw new SenderException(e);
		}

		if (transformerPool!=null) {
			transformerPool.close();
		}
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	protected boolean appendParameters(boolean parametersAppended, StringBuffer path, ParameterValueList parameters) throws SenderException {
		if (parameters != null) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appending ["+parameters.size()+"] parameters");
			for(int i=0; i < parameters.size(); i++) {
				if (skipParameter(paramList.get(i).getName())) {
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"skipping ["+paramList.get(i)+"]");
					continue;
				}
				ParameterValue pv = parameters.getParameterValue(i);
				try {
					if (parametersAppended) {
						path.append("&");
					} else {
						path.append("?");
						parametersAppended = true;
					}
	
					String parameterToAppend = pv.getDefinition().getName() +"="+ URLEncoder.encode(pv.asStringValue(""), getCharSet());
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appending parameter ["+parameterToAppend+"]");
					path.append(parameterToAppend);
				} catch (UnsupportedEncodingException e) {
					throw new SenderException(getLogPrefix()+"["+getCharSet()+"] encoding error. Failed to add parameter ["+pv.getDefinition().getName()+"]");
				}
			}
		}
		return parametersAppended;
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
	protected abstract HttpRequestBase getMethod(URI uri, Message message, ParameterValueList parameters, IPipeLineSession session) throws SenderException;

	/**
	 * Custom implementation to extract the response and format it to a String result. <br/>
	 * It is important that the {@link HttpResponseHandler#getResponse() response} 
	 * will be read or will be {@link HttpResponseHandler#close() closed}.
	 * @param responseHandler {@link HttpResponseHandler} that contains the response information
	 * @param session {@link IPipeLineSession} which may be null
	 * @return a string that will be passed to the pipeline
	 */
	protected abstract Message extractResult(HttpResponseHandler responseHandler, IPipeLineSession session) throws SenderException, IOException;

	@Override
	public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		ParameterValueList pvl = null;
		try {
			if (paramList !=null) {
				pvl=paramList.getValues(message, session);
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"Sender ["+getName()+"] caught exception evaluating parameters",e);
		}

		HttpHost httpTarget;
		URI uri;
		final HttpRequestBase httpRequestBase;
		try {
			if (urlParameter != null) {
				String url = (String) pvl.getParameterValue(getUrlParam()).getValue();
				uri = getURI(url);
			} else {
				uri = staticUri;
			}

			httpTarget = new HttpHost(uri.getHost(), getPort(uri), uri.getScheme());

			// Resolve HeaderParameters
			Map<String, String> headersParamsMap = new HashMap<String, String>();
			if (headersParams != null) {
				log.debug("appending header parameters "+headersParams);
				StringTokenizer st = new StringTokenizer(getHeadersParams(), ",");
				while (st.hasMoreElements()) {
					String paramName = st.nextToken();
					ParameterValue paramValue = pvl.getParameterValue(paramName);
					if(paramValue != null)
						headersParamsMap.put(paramName, paramValue.asStringValue(null));
				}
			}

			httpRequestBase = getMethod(uri, message, pvl, session);
			if(httpRequestBase == null)
				throw new MethodNotSupportedException("could not find implementation for method ["+getMethodType()+"]");

			//Set all headers
			if(session != null && APPEND_MESSAGEID_HEADER && StringUtils.isNotEmpty(session.getMessageId())) {
				httpRequestBase.setHeader("Message-Id", session.getMessageId());
			}
			for (String param: headersParamsMap.keySet()) {
				httpRequestBase.setHeader(param, headersParamsMap.get(param));
			}

			if (credentials != null && !StringUtils.isEmpty(credentials.getUsername())) {
				AuthCache authCache = httpClientContext.getAuthCache();
				if(authCache == null)
					authCache = new BasicAuthCache();

				if(authCache.get(httpTarget) == null)
					authCache.put(httpTarget, new BasicScheme());

				httpClientContext.setAuthCache(authCache);
			}

			log.info(getLogPrefix()+"configured httpclient for host ["+uri.getHost()+"]");

		} catch (Exception e) {
			throw new SenderException(e);
		}

		Message result = null;
		int statusCode = -1;
		int count=getMaxExecuteRetries();
		String msg = null;

		while (count-- >= 0 && statusCode == -1) {
			TimeoutGuard tg = new TimeoutGuard(1+getTimeout()/1000, getName()) {

				@Override
				protected void kill() {
					httpRequestBase.abort();
				}
				
			};
			try {
				log.debug(getLogPrefix()+"executing method [" + httpRequestBase.getRequestLine() + "]");
				HttpResponse httpResponse = getHttpClient().execute(httpTarget, httpRequestBase, httpClientContext);
				log.debug(getLogPrefix()+"executed method");

				HttpResponseHandler responseHandler = new HttpResponseHandler(httpResponse);
				StatusLine statusline = httpResponse.getStatusLine();
				statusCode = statusline.getStatusCode();

				if (StringUtils.isNotEmpty(getResultStatusCodeSessionKey()) && session != null) {
					session.put(getResultStatusCodeSessionKey(), Integer.toString(statusCode));
				}

				// Only give warnings for 4xx (client errors) and 5xx (server errors)
				if (statusCode >= 400 && statusCode < 600) {
					log.warn(getLogPrefix()+"status ["+statusline.toString()+"]");
				} else {
					log.debug(getLogPrefix()+"status ["+statusCode+"]");
				}

				result = extractResult(responseHandler, session);

				log.debug(getLogPrefix()+"retrieved result ["+result+"]");
			} catch (ClientProtocolException e) {
				StringBuilder msgBuilder = new StringBuilder(getLogPrefix()+"httpException with");
				if(e.getMessage() != null) {
					msg = e.getMessage();
					msgBuilder.append(" message [" + msg + "]");
				}
				Throwable throwable = e.getCause();
				if (throwable != null) {
					msgBuilder.append(" cause [" + throwable.toString() + "]");
				}
				msgBuilder.append(" executeRetries left [" + count + "]");

				log.warn(msgBuilder.toString());
			} catch (IOException e) {
				httpRequestBase.abort();
				if (e instanceof SocketTimeoutException) {
					throw new TimeOutException(e);
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
				// wont read or close the response.
				// This will cause the connection to become stale..
				
				if (tg.cancel()) {
					throw new TimeOutException(getLogPrefix()+"timeout of ["+getTimeout()+"] ms exceeded");
				}
			}
		}

		if (statusCode == -1){
			if (msg != null && StringUtils.contains(msg.toUpperCase(), "TIMEOUTEXCEPTION")) {
				//java.net.SocketTimeoutException: Read timed out
				throw new TimeOutException("Failed to recover from timeout exception");
			}
			throw new SenderException("Failed to recover from exception");
		}

		if (isXhtml() && !result.isEmpty()) {
			String resultString;
			try {
				resultString = result.asString();
			} catch (IOException e) {
				throw new SenderException("error reading http response as String", e);
			}

			String xhtml = XmlUtils.skipDocTypeDeclaration(resultString.trim());
			if (xhtml.startsWith("<html>") || xhtml.startsWith("<html ")) {
				CleanerProperties props = new CleanerProperties();
				HtmlCleaner cleaner = new HtmlCleaner(props);
				TagNode tagNode = cleaner.clean(xhtml);
				xhtml = new SimpleXmlSerializer(props).getAsString(tagNode);

				if (transformerPool != null) {
					log.debug(getLogPrefix() + " transforming result [" + xhtml + "]");
					try {
						xhtml = transformerPool.transform(Message.asSource(xhtml));
					} catch (Exception e) {
						throw new SenderException("Exception on transforming input", e);
					}
				}
			}
			result = Message.asMessage(xhtml);
		}

		return result;
	}

	@Override
	public String getPhysicalDestinationName() {
		if (urlParameter!=null) {
			return "dynamic url";
		}
		return getUrl();
	}


	@IbisDoc({"1", "URL or base of URL to be used", ""})
	public void setUrl(String string) {
		url = string;
	}
	public String getUrl() {
		return url;
	}

	@IbisDoc({"2", "parameter that is used to obtain url; overrides url-attribute.", "url"})
	public void setUrlParam(String urlParam) {
		this.urlParam = urlParam;
	}
	public String getUrlParam() {
		return urlParam;
	}

	@IbisDoc({"3", "type of method to be executed, either 'GET', 'POST', 'PUT', 'DELETE', 'HEAD' or 'REPORT'", "GET"})
	public void setMethodType(String string) {
		methodType = string;
	}
	public String getMethodType() {
		return methodType.toUpperCase();
	}

	/**
	 * This is a superset of mimetype + charset + optional payload metadata.
	 */
	@IbisDoc({"4", "content-type (superset of mimetype + charset) of the request, for POST and PUT methods", "text/html"})
	public void setContentType(String string) {
		contentType = string;
	}
	public String getContentType() {
		return contentType;
	}
	public ContentType getFullContentType() {
		return fullContentType;
	}

	@IbisDoc({"6", "charset of the request. Typically only used on PUT and POST requests.", "UTF-8"})
	public void setCharSet(String string) {
		charSet = string;
	}
	public String getCharSet() {
		return charSet;
	}

	@IbisDoc({"10", "timeout in ms of obtaining a connection/result. 0 means no timeout", "10000"})
	public void setTimeout(int i) {
		timeout = i;
	}
	public int getTimeout() {
		return timeout;
	}

	@IbisDoc({"11", "the maximum number of concurrent connections", "10"})
	public void setMaxConnections(int i) {
		maxConnections = i;
	}
	public int getMaxConnections() {
		return maxConnections;
	}

	@IbisDoc({"12", "the maximum number of times it the execution is retried", "1"})
	public void setMaxExecuteRetries(int i) {
		maxExecuteRetries = i;
	}
	public int getMaxExecuteRetries() {
		return maxExecuteRetries;
	}



	@IbisDoc({"20", "alias used to obtain credentials for authentication to host", ""})
	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	@IbisDoc({"21", "username used in authentication to host", ""})
	public void setUserName(String string) {
		userName = string;
	}
	public String getUserName() {
		return userName;
	}

	@IbisDoc({"22", "password used in authentication to host", " "})
	public void setPassword(String string) {
		password = string;
	}
	public String getPassword() {
		return password;
	}

	@IbisDoc({"23", "domain used in authentication to host", " "})
	public void setAuthDomain(String string) {
		authDomain = string;
	}
	public String getAuthDomain() {
		return authDomain;
	}


	@IbisDoc({"30", "proxy host", " "})
	public void setProxyHost(String string) {
		proxyHost = string;
	}
	public String getProxyHost() {
		return proxyHost;
	}

	@IbisDoc({"31", "proxy port", "80"})
	public void setProxyPort(int i) {
		proxyPort = i;
	}
	public int getProxyPort() {
		return proxyPort;
	}

	@IbisDoc({"32", "alias used to obtain credentials for authentication to proxy", ""})
	public void setProxyAuthAlias(String string) {
		proxyAuthAlias = string;
	}
	public String getProxyAuthAlias() {
		return proxyAuthAlias;
	}

	@IbisDoc({"33", "proxy username", " "})
	public void setProxyUserName(String string) {
		proxyUserName = string;
	}
	public String getProxyUserName() {
		return proxyUserName;
	}

	@IbisDoc({"34", "proxy password", " "})
	public void setProxyPassword(String string) {
		proxyPassword = string;
	}
	public String getProxyPassword() {
		return proxyPassword;
	}

	@IbisDoc({"35", "proxy realm", " "})
	public void setProxyRealm(String string) {
		proxyRealm = string;
	}
	public String getProxyRealm() {
		if(StringUtils.isEmpty(proxyRealm))
			return null;
		return proxyRealm;
	}

	/**
	 * TODO: make this configurable
	 * @return false
	 */
	public boolean prefillProxyAuthCache() {
		return false;
	}

	@IbisDoc({"36", "Disables the use of cookies, making the sender completely stateless", "false"})
	public void setDisableCookies(boolean disableCookies) {
		this.disableCookies = disableCookies;
	}
	public boolean areCookiesDisabled() {
		return disableCookies;
	}


	@IbisDoc({"40", "resource url to certificate to be used for authentication", ""})
	public void setCertificate(String string) {
		certificate = string;
	}
	public String getCertificate() {
		return certificate;
	}

	@IbisDoc({"41", "alias used to obtain certificate password", ""})
	public void setCertificateAuthAlias(String string) {
		certificateAuthAlias = string;
	}
	public String getCertificateAuthAlias() {
		return certificateAuthAlias;
	}

	@IbisDoc({"42", "certificate password", " "})
	public void setCertificatePassword(String string) {
		certificatePassword = string;
	}
	public String getCertificatePassword() {
		return certificatePassword;
	}

	@IbisDoc({"43", "", "pkcs12"})
	public void setKeystoreType(String string) {
		keystoreType = string;
	}
	public String getKeystoreType() {
		return keystoreType;
	}

	@IbisDoc({"44", "", " "})
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}
	public String getKeyManagerAlgorithm() {
		return keyManagerAlgorithm;
	}


	@IbisDoc({"50", "resource url to truststore to be used for authentication", ""})
	public void setTruststore(String string) {
		truststore = string;
	}
	public String getTruststore() {
		return truststore;
	}

	@IbisDoc({"51", "alias used to obtain truststore password", ""})
	public void setTruststoreAuthAlias(String string) {
		truststoreAuthAlias = string;
	}
	public String getTruststoreAuthAlias() {
		return truststoreAuthAlias;
	}

	@IbisDoc({"52", "truststore password", " "})
	public void setTruststorePassword(String string) {
		truststorePassword = string;
	}
	public String getTruststorePassword() {
		return truststorePassword;
	}

	@IbisDoc({"53", "type of truststore", "jks"})
	public void setTruststoreType(String string) {
		truststoreType = string;
	}
	public String getTruststoreType() {
		return truststoreType;
	}

	@IbisDoc({"54", "", " "})
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		this.trustManagerAlgorithm = trustManagerAlgorithm;
	}
	public String getTrustManagerAlgorithm() {
		return trustManagerAlgorithm;
	}

	@IbisDoc({"55", "when true, the hostname in the certificate will be checked against the actual hostname", "true"})
	public void setVerifyHostname(boolean b) {
		verifyHostname = b;
	}
	public boolean isVerifyHostname() {
		return verifyHostname;
	}

	@IbisDoc({"56", "when true, self signed certificates are accepted", "false"})
	public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		this.allowSelfSignedCertificates = allowSelfSignedCertificates;
	}
	public boolean isAllowSelfSignedCertificates() {
		return allowSelfSignedCertificates;
	}

	/**
	 * The CertificateExpiredException is ignored when set to true
	 * @IbisDoc.default false
	 */
	@IbisDoc({"57", "when true, the certificateexpiredexception is ignored", "false"})
	public void setIgnoreCertificateExpiredException(boolean b) {
		ignoreCertificateExpiredException = b;
	}
	public boolean isIgnoreCertificateExpiredException() {
		return ignoreCertificateExpiredException;
	}
	
	
	@IbisDoc({"60", "comma separated list of parameter names which should be set as http headers", ""})
	public void setHeadersParams(String headersParams) {
		this.headersParams = headersParams;
	}
	public String getHeadersParams() {
		return headersParams;
	}
	
	@IbisDoc({"61", "when true, a redirect request will be honoured, e.g. to switch to https", "true"})
	public void setFollowRedirects(boolean b) {
		followRedirects = b;
	}
	public boolean isFollowRedirects() {
		return followRedirects;
	}
	
	@IbisDoc({"62", "controls whether connections checked to be stale, i.e. appear open, but are not.", "true"})
	public void setStaleChecking(boolean b) {
		staleChecking = b;
	}
	public boolean isStaleChecking() {
		return staleChecking;
	}
	
	@IbisDoc({"63", "Used when StaleChecking=true. Timeout when stale connections should be closed.", "5000"})
	public void setStaleTimeout(int timeout) {
		staleTimeout = timeout;
	}
	public int getStaleTimeout() {
		return staleTimeout;
	}

	@IbisDoc({"65", "when true, the html response is transformed to xhtml", "false"})
	public void setXhtml(boolean xHtml) {
		xhtml = xHtml;
	}
	public boolean isXhtml() {
		return xhtml;
	}

	@IbisDoc({"66", ">(only used when <code>xhtml=true</code>) stylesheet to apply to the html response", ""})
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}
	public String getStyleSheetName() {
		return styleSheetName;
	}

	@IbisDoc({"67", "Secure socket protocol (such as 'SSL' and 'TLS') to use when a SSLContext object is generated. If empty the protocol 'SSL' is used", "SSL"})
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getProtocol() {
		return protocol;
	}

	@IbisDoc({"68", "if set, the status code of the http response is put in specified in the sessionkey and the (error or okay) response message is returned", ""})
	public void setResultStatusCodeSessionKey(String resultStatusCodeSessionKey) {
		this.resultStatusCodeSessionKey = resultStatusCodeSessionKey;
	}
	public String getResultStatusCodeSessionKey() {
		return resultStatusCodeSessionKey;
	}
}