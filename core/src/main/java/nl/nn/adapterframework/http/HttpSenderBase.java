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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthProtocolState;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
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

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.encryption.AuthSSLContextFactory;
import nl.nn.adapterframework.encryption.HasKeystore;
import nl.nn.adapterframework.encryption.HasTruststore;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.http.authentication.AuthenticationScheme;
import nl.nn.adapterframework.http.authentication.HttpAuthenticationException;
import nl.nn.adapterframework.http.authentication.OAuthAccessTokenManager;
import nl.nn.adapterframework.http.authentication.OAuthAuthenticationScheme;
import nl.nn.adapterframework.http.authentication.OAuthPreferringAuthenticationStrategy;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.senders.SenderWithParametersBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
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
 * @ff.parameters Any parameters present are appended to the request (when method is <code>GET</code> as request-parameters, when method <code>POST</code> as body part) except the <code>headersParams</code> list, which are added as HTTP headers, and the <code>urlParam</code> header
 * @ff.forward "&lt;statusCode of the HTTP response&gt;" default
 *
 * @author	Niels Meijer
 * @since	7.0
 */
//TODO: Fix javadoc!

public abstract class HttpSenderBase extends SenderWithParametersBase implements HasPhysicalDestination, HasKeystore, HasTruststore {

	private static final String CONTEXT_KEY_STATUS_CODE = "Http.StatusCode";
	private static final String CONTEXT_KEY_REASON_PHRASE = "Http.ReasonPhrase";
	public static final String MESSAGE_ID_HEADER = "Message-Id";
	public static final String CORRELATION_ID_HEADER = "Correlation-Id";

	private final @Getter(onMethod = @__(@Override)) String domain = "Http";

	private @Getter String url;
	private @Getter String urlParam = "url";

	public enum HttpMethod {
		GET,POST,PUT,PATCH,DELETE,HEAD,REPORT;
	}
	private @Getter HttpMethod httpMethod = HttpMethod.GET;

	private @Getter String charSet = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private @Getter ContentType fullContentType = null;
	private @Getter String contentType = null;

	/* CONNECTION POOL */
	private @Getter int timeout = 10000;
	private @Getter int maxConnections = 10;
	private @Getter int maxExecuteRetries = 1;
	private @Getter boolean staleChecking=true;
	private @Getter int staleTimeout = 5000; // [ms]
	private @Getter int connectionTimeToLive = 900; // [s]
	private @Getter int connectionIdleTimeout = 10; // [s]
	private HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
	private @Getter HttpClientContext httpClientContext = HttpClientContext.create();
	private @Getter CloseableHttpClient httpClient;

	/* SECURITY */
	private @Getter String authAlias;
	private @Getter String username;
	private @Getter String password;
	private @Getter String authDomain;
	private @Getter String tokenEndpoint;
	private @Getter int tokenExpiry=-1;
	private @Getter String clientAuthAlias;
	private @Getter String clientId;
	private @Getter String clientSecret;
	private @Getter String scope;
	private @Getter boolean authenticatedTokenRequest;

	/* PROXY */
	private @Getter String proxyHost;
	private @Getter int    proxyPort=80;
	private @Getter String proxyAuthAlias;
	private @Getter String proxyUsername;
	private @Getter String proxyPassword;
	private @Getter String proxyRealm=null;

	/* SSL */
	private @Getter String keystore;
	private @Getter String keystoreAuthAlias;
	private @Getter String keystorePassword;
	private @Getter KeystoreType keystoreType=KeystoreType.PKCS12;
	private @Getter String keystoreAlias;
	private @Getter String keystoreAliasAuthAlias;
	private @Getter String keystoreAliasPassword;
	private @Getter String keyManagerAlgorithm=null;

	private @Getter String truststore=null;
	private @Getter String truststoreAuthAlias;
	private @Getter String truststorePassword=null;
	private @Getter KeystoreType truststoreType=KeystoreType.JKS;
	private @Getter String trustManagerAlgorithm=null;
	private @Getter boolean allowSelfSignedCertificates = false;
	private @Getter boolean verifyHostname=true;
	private @Getter boolean ignoreCertificateExpiredException=false;

	private @Getter String headersParams="";
	private @Getter boolean followRedirects=true;
	private @Getter boolean ignoreRedirects=false;
	private @Getter boolean xhtml=false;
	private @Getter String styleSheetName=null;
	private @Getter String protocol=null;
	private @Getter String resultStatusCodeSessionKey;
	private @Getter String parametersToSkipWhenEmpty;

	private final boolean APPEND_MESSAGEID_HEADER = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("http.headers.messageid", true);
	private final boolean APPEND_CORRELATIONID_HEADER = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("http.headers.correlationid", true);
	private boolean disableCookies = false;

	private TransformerPool transformerPool=null;

	protected Parameter urlParameter;

	protected URI staticUri;
	private CredentialFactory credentials;
	private CredentialFactory user_cf;
	private CredentialFactory client_cf;

	protected Set<String> requestOrBodyParamsSet=new HashSet<>();
	protected Set<String> headerParamsSet=new LinkedHashSet<>();
	protected Set<String> parametersToSkipWhenEmptySet=new HashSet<>();

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

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		/**
		 * TODO find out if this really breaks proxy authentication or not.
		 */
//		httpClientBuilder.disableAuthCaching();

		Builder requestConfigBuilder = RequestConfig.custom();
		requestConfigBuilder.setConnectTimeout(getTimeout());
		requestConfigBuilder.setConnectionRequestTimeout(getTimeout());
		requestConfigBuilder.setSocketTimeout(getTimeout());

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
		} catch (URISyntaxException e) {
			throw new ConfigurationException(getLogPrefix()+"cannot interpret url ["+getUrl()+"]", e);
		}

		AuthSSLContextFactory.verifyKeystoreConfiguration(this, this);

		if (StringUtils.isNotEmpty(getAuthAlias()) || StringUtils.isNotEmpty(getUsername())) {
			user_cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
			credentials = user_cf;
		}
		client_cf = new CredentialFactory(getClientAuthAlias(), getClientId(), getClientSecret());
		if (credentials==null) {
			credentials = client_cf;
		}
		if (StringUtils.isNotEmpty(getTokenEndpoint()) && StringUtils.isEmpty(getClientAuthAlias()) && StringUtils.isEmpty(getClientId())) {
			throw new ConfigurationException("To obtain accessToken at tokenEndpoint ["+getTokenEndpoint()+"] a clientAuthAlias or ClientId and ClientSecret must be specified");
		}
		HttpHost proxy = null;
		CredentialFactory pcf = null;
		if (StringUtils.isNotEmpty(getProxyHost())) {
			proxy = new HttpHost(getProxyHost(), getProxyPort());
			pcf = new CredentialFactory(getProxyAuthAlias(), getProxyUsername(), getProxyPassword());
			requestConfigBuilder.setProxy(proxy);
			httpClientBuilder.setProxy(proxy);
		}

		try {
			setupAuthentication(pcf, proxy, requestConfigBuilder);
		} catch (HttpAuthenticationException e) {
			throw new ConfigurationException("exception configuring authentication", e);
		}

		if (StringUtils.isNotEmpty(getStyleSheetName())) {
			try {
				Resource stylesheet = Resource.getResource(this, getStyleSheetName());
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

		httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

		httpClientBuilder.setRetryHandler(new HttpRequestRetryHandler(getMaxExecuteRetries()));

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
		int timeToLive = getConnectionTimeToLive();
		if (timeToLive<=0) {
			timeToLive = -1;
		}
		SSLConnectionSocketFactory sslSocketFactory = getSSLConnectionSocketFactory();
		if(sslSocketFactory != null) {
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", sslSocketFactory)
				.build();
			connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, null, null, null, timeToLive, TimeUnit.SECONDS);
			log.debug(getLogPrefix()+"created PoolingHttpClientConnectionManager with custom SSLConnectionSocketFactory");
		}
		else {
			connectionManager = new PoolingHttpClientConnectionManager(timeToLive, TimeUnit.SECONDS);
			log.debug(getLogPrefix()+"created default PoolingHttpClientConnectionManager");
		}

		connectionManager.setMaxTotal(getMaxConnections());
		connectionManager.setDefaultMaxPerRoute(getMaxConnections());

		if (isStaleChecking()) {
			log.info(getLogPrefix()+"set up connectionManager, setting stale checking ["+isStaleChecking()+"]");
			connectionManager.setValidateAfterInactivity(getStaleTimeout());
		}

		httpClientBuilder.setConnectionManager(connectionManager);
		httpClientBuilder.evictIdleConnections(getConnectionIdleTimeout(), TimeUnit.SECONDS);

		if (transformerPool!=null) {
			try {
				transformerPool.open();
			} catch (Exception e) {
				throw new SenderException(getLogPrefix()+"cannot start TransformerPool", e);
			}
		}

		httpClient = httpClientBuilder.build();
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

	private void setupAuthentication(CredentialFactory proxyCredentials, HttpHost proxy, Builder requestConfigBuilder) throws HttpAuthenticationException {
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		if (StringUtils.isNotEmpty(credentials.getUsername()) || StringUtils.isNotEmpty(getTokenEndpoint())) {

			credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), getCredentials());

			AuthenticationScheme preferredAuthenticationScheme = getPreferredAuthenticationScheme();
			requestConfigBuilder.setTargetPreferredAuthSchemes(Arrays.asList(preferredAuthenticationScheme.getSchemeName()));
			requestConfigBuilder.setAuthenticationEnabled(true);

			if (preferredAuthenticationScheme == AuthenticationScheme.OAUTH) {
				OAuthAccessTokenManager accessTokenManager = new OAuthAccessTokenManager(getTokenEndpoint(), getScope(), client_cf, user_cf==null, isAuthenticatedTokenRequest(), this, getTokenExpiry());
				httpClientContext.setAttribute(OAuthAuthenticationScheme.ACCESSTOKEN_MANAGER_KEY, accessTokenManager);
				httpClientBuilder.setTargetAuthenticationStrategy(new OAuthPreferringAuthenticationStrategy());
			}
		}
		if (proxy!=null) {
			AuthScope scope = new AuthScope(proxy, proxyRealm, AuthScope.ANY_SCHEME);


			if (StringUtils.isNotEmpty(proxyCredentials.getUsername())) {
				Credentials credentials = new UsernamePasswordCredentials(proxyCredentials.getUsername(), proxyCredentials.getPassword());
				credentialsProvider.setCredentials(scope, credentials);
			}
			//log.trace("setting credentialProvider [" + credentialsProvider.toString() + "]");

			if(prefillProxyAuthCache()) {
				requestConfigBuilder.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC));

				AuthCache authCache = httpClientContext.getAuthCache();
				if(authCache == null)
					authCache = new BasicAuthCache();

				authCache.put(proxy, new BasicScheme());
				httpClientContext.setAuthCache(authCache);
			}

		}

		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
	}

	private void preAuthenticate() {
		if (credentials != null && !StringUtils.isEmpty(credentials.getUsername())) {
			AuthState authState = httpClientContext.getTargetAuthState();
			if (authState==null) {
				authState = new AuthState();
				httpClientContext.setAttribute(HttpClientContext.TARGET_AUTH_STATE, authState);
			}
			authState.setState(AuthProtocolState.CHALLENGED);
			authState.update(getPreferredAuthenticationScheme().createScheme(), getCredentials());
		}
	}

	private Credentials getCredentials() {
		String uname;
		if (StringUtils.isNotEmpty(getAuthDomain())) {
			uname = getAuthDomain() + "\\" + credentials.getUsername();
		} else {
			uname = credentials.getUsername();
		}

		return new UsernamePasswordCredentials(uname, credentials.getPassword());
	}

	private AuthenticationScheme getPreferredAuthenticationScheme() {
		return StringUtils.isNotEmpty(getTokenEndpoint()) ? AuthenticationScheme.OAUTH : AuthenticationScheme.BASIC;
	}

	protected SSLConnectionSocketFactory getSSLConnectionSocketFactory() throws SenderException {
		SSLConnectionSocketFactory sslSocketFactory;
		HostnameVerifier hostnameVerifier = verifyHostname ? new DefaultHostnameVerifier() : new NoopHostnameVerifier();

		try {
			javax.net.ssl.SSLSocketFactory socketfactory = AuthSSLContextFactory.createSSLSocketFactory(this, this, getProtocol());
			sslSocketFactory = new SSLConnectionSocketFactory(socketfactory, hostnameVerifier);
		} catch (Exception e) {
			throw new SenderException("cannot create or initialize SocketFactory", e);
		}
		// This method will be overwritten by the connectionManager when connectionPooling is enabled!
		// Can still be null when no default or an invalid system sslSocketFactory has been defined
		if(sslSocketFactory != null) {
			httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
		}
		return sslSocketFactory;
	}

	protected boolean appendParameters(boolean parametersAppended, StringBuilder path, ParameterValueList parameters) throws SenderException {
		if (parameters != null) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appending ["+parameters.size()+"] parameters");
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
							if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appending parameter ["+parameterToAppend+"]");
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

			log.info(getLogPrefix()+"configured httpclient for host ["+targetUri.getHost()+"]");

		} catch (Exception e) {
			throw new SenderException(e);
		}

		Message result;
		int statusCode;
		boolean success;
		String reasonPhrase;
		HttpHost targetHost = new HttpHost(targetUri.getHost(), targetUri.getPort(), targetUri.getScheme());

		TimeoutGuard tg = new TimeoutGuard(1+getTimeout()/1000, getName()) {

			@Override
			protected void abort() {
				httpRequestBase.abort();
			}

		};
		try {
			log.debug(getLogPrefix()+"executing method [" + httpRequestBase.getRequestLine() + "]");
			HttpResponse httpResponse = getHttpClient().execute(targetHost, httpRequestBase, httpClientContext);
			log.debug(getLogPrefix()+"executed method");

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
				log.warn(getLogPrefix()+"status ["+ statusline +"]");
			} else {
				log.debug(getLogPrefix()+"status ["+statusCode+"]");
			}

			result = extractResult(responseHandler, session);

			log.debug(getLogPrefix()+"retrieved result ["+result+"]");
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
			try (Message m = result){
				xhtml = XmlUtils.toXhtml(m);
			} catch (IOException e) {
				throw new SenderException("error reading http response as String", e);
			}

			if (transformerPool != null && xhtml != null) {
				log.debug(getLogPrefix() + " transforming result [" + xhtml + "]");
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

	/**
	 * Timeout in ms of obtaining a connection/result. 0 means no timeout
	 * @ff.default 10000
	 */
	public void setTimeout(int i) {
		timeout = i;
	}

	/**
	 * The maximum number of concurrent connections
	 * @ff.default 10
	 */
	public void setMaxConnections(int i) {
		maxConnections = i;
	}

	/**
	 * The maximum number of times the execution is retried
	 * @ff.default 1 (for repeatable messages) else 0
	 */
	public void setMaxExecuteRetries(int i) {
		maxExecuteRetries = i;
	}

	/** Authentication alias used for authentication to the host */
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	/** Username used for authentication to the host */
	public void setUsername(String username) {
		this.username = username;
	}
	@Deprecated
	@ConfigurationWarning("Please use attribute username instead")
	public void setUserName(String username) {
		setUsername(username);
	}

	/** Password used for authentication to the host */
	public void setPassword(String string) {
		password = string;
	}

	/**
	 * Corporate domain name. Should only be used in combination with sAMAccountName, never with an UPN.<br/>
	 * <br/>
	 * Assuming the following user:<br/>
	 * UPN: john.doe@CorpDomain.biz<br/>
	 * sAMAccountName: CORPDOMAIN\john.doe<br/>
	 * <br/>
	 * The username attribute may be set to <code>john.doe</code><br/>
	 * The AuthDomain attribute may be set to <code>CORPDOMAIN</code><br/>
	 */
	@Deprecated
	@ConfigurationWarning("Please use the UPN or the full sAM-AccountName instead")
	public void setAuthDomain(String string) {
		authDomain = string;
	}

	/**
	 * Endpoint to obtain OAuth accessToken. If <code>authAlias</code> or <code>username</code>( and <code>password</code>) are specified,
	 * then a PasswordGrant is used, otherwise a ClientCredentials grant. The obtained accessToken will be added to the regular requests
	 * in an HTTP Header 'Authorization' with a 'Bearer' prefix.
	 */
	public void setTokenEndpoint(String string) {
		tokenEndpoint = string;
	}
	/**
	 * If set to a non-negative value, then determines the time (in seconds) after which the token will be refreshed. Otherwise the token
	 * will be refreshed when it is half way its lifetime as defined by the <code>expires_in</code> clause of the token response,
	 * or when the regular server returns a 401 status with a challenge.
	 * If not specified, and the accessTokens lifetime is not found in the token response, the accessToken will not be refreshed preemptively.
	 * @ff.default -1
	 */
	public void setTokenExpiry(int value) {
		tokenExpiry = value;
	}
	/** Alias used to obtain client_id and client_secret for authentication to <code>tokenEndpoint</code> */
	public void setClientAlias(String clientAuthAlias) {
		this.clientAuthAlias = clientAuthAlias;
	}
	/** Client_id used in authentication to <code>tokenEndpoint</code> */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/** Client_secret used in authentication to <code>tokenEndpoint</code> */
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	/** Space or comma separated list of scope items requested for accessToken, e.g. <code>read write</code>. Only used when <code>tokenEndpoint</code> is specified */
	public void setScope(String string) {
		scope = string;
	}
	/** if set true, clientId and clientSecret will be added as Basic Authentication header to the tokenRequest, instead of as request parameters */
	public void setAuthenticatedTokenRequest(boolean authenticatedTokenRequest) {
		this.authenticatedTokenRequest = authenticatedTokenRequest;
	}


	/** Proxy host */
	public void setProxyHost(String string) {
		proxyHost = string;
	}

	/**
	 * Proxy port
	 * @ff.default 80
	 */
	public void setProxyPort(int i) {
		proxyPort = i;
	}

	/** Alias used to obtain credentials for authentication to proxy */
	public void setProxyAuthAlias(String string) {
		proxyAuthAlias = string;
	}

	/**
	 * Proxy username
	 * @ff.default
	 */
	public void setProxyUsername(String string) {
		proxyUsername = string;
	}
	@Deprecated
	@ConfigurationWarning("Please use \"proxyUsername\" instead")
	public void setProxyUserName(String string) {
		setProxyUsername(string);
	}

	/**
	 * Proxy password
	 * @ff.default
	 */
	public void setProxyPassword(String string) {
		proxyPassword = string;
	}

	/**
	 * Proxy realm
	 * @ff.default
	 */
	public void setProxyRealm(String string) {
		proxyRealm = StringUtils.isNotEmpty(string) ? string : null;
	}

	/**
	 * TODO: make this configurable
	 * @return false
	 */
	public boolean prefillProxyAuthCache() {
		return false;
	}

	/**
	 * Disables the use of cookies, making the sender completely stateless
	 * @ff.default false
	 */
	public void setDisableCookies(boolean disableCookies) {
		this.disableCookies = disableCookies;
	}
	public boolean areCookiesDisabled() {
		return disableCookies;
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

	/** resource URL to keystore or certificate to be used for authentication. If none specified, the JVMs default keystore will be used. */
	@Override
	public void setKeystore(String string) {
		keystore = string;
	}

	@Override
	public void setKeystoreType(KeystoreType value) {
		keystoreType = value;
	}

	@Override
	public void setKeystoreAuthAlias(String string) {
		keystoreAuthAlias = string;
	}

	@Override
	public void setKeystorePassword(String string) {
		keystorePassword = string;
	}

	@Override
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}

	@Override
	public void setKeystoreAlias(String string) {
		keystoreAlias = string;
	}
	@Override
	public void setKeystoreAliasAuthAlias(String string) {
		keystoreAliasAuthAlias = string;
	}
	@Override
	public void setKeystoreAliasPassword(String string) {
		keystoreAliasPassword = string;
	}

	@Override
	/** Resource URL to truststore to be used for authenticating peer. If none specified, the JVMs default truststore will be used. */
	public void setTruststore(String string) {
		truststore = string;
	}

	@Override
	public void setTruststoreAuthAlias(String string) {
		truststoreAuthAlias = string;
	}

	@Override
	public void setTruststorePassword(String string) {
		truststorePassword = string;
	}

	@Override
	public void setTruststoreType(KeystoreType value) {
		truststoreType = value;
	}

	@Override
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		this.trustManagerAlgorithm = trustManagerAlgorithm;
	}

	@Override
	public void setVerifyHostname(boolean b) {
		verifyHostname = b;
	}

	@Override
	public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		this.allowSelfSignedCertificates = allowSelfSignedCertificates;
	}

	@Override
	public void setIgnoreCertificateExpiredException(boolean b) {
		ignoreCertificateExpiredException = b;
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
	 * If <code>true</code>, a redirect request will be honoured, e.g. to switch to HTTPS
	 * @ff.default true
	 */
	public void setFollowRedirects(boolean b) {
		followRedirects = b;
	}

	/**
	 * If true, besides http status code 200 (OK) also the code 301 (MOVED_PERMANENTLY), 302 (MOVED_TEMPORARILY) and 307 (TEMPORARY_REDIRECT) are considered successful
	 * @ff.default false
	 */
	public void setIgnoreRedirects(boolean b) {
		ignoreRedirects = b;
	}


	/**
	 * Controls whether connections checked to be stale, i.e. appear open, but are not.
	 * @ff.default true
	 */
	public void setStaleChecking(boolean b) {
		staleChecking = b;
	}

	/**
	 * Used when StaleChecking=<code>true</code>. Timeout after which an idle connection will be validated before being used.
	 * @ff.default 5000 ms
	 */
	public void setStaleTimeout(int timeout) {
		staleTimeout = timeout;
	}

	/**
	 * Maximum Time to Live for connections in the pool. No connection will be re-used past its timeToLive value.
	 * @ff.default 900 s
	 */
	public void setConnectionTimeToLive(int timeToLive) {
		connectionTimeToLive = timeToLive;
	}

	/**
	 * Maximum Time for connection to stay idle in the pool. Connections that are idle longer will periodically be evicted from the pool
	 * @ff.default 10 s
	 */
	public void setConnectionIdleTimeout(int idleTimeout) {
		connectionIdleTimeout = idleTimeout;
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

	/**
	 * Secure socket protocol (such as 'SSL' and 'TLS') to use when a SSLContext object is generated.
	 * @ff.default SSL
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/** If set, the status code of the HTTP response is put in the specified sessionKey and the (error or okay) response message is returned.
	 * Setting this property has a side effect. If a 4xx or 5xx result code is returned and if the configuration does not implement
	 * the specific forward for the returned HTTP result code, then the success forward is followed instead of the exception forward.
	 */
	public void setResultStatusCodeSessionKey(String resultStatusCodeSessionKey) {
		this.resultStatusCodeSessionKey = resultStatusCodeSessionKey;
	}
}
