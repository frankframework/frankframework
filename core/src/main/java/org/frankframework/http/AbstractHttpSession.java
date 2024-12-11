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
package org.frankframework.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthProtocolState;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.ConnPoolControl;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.logging.log4j.Logger;

import org.frankframework.http.authentication.IOauthAuthenticator;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.Adapter;
import org.frankframework.core.AdapterAware;
import org.frankframework.core.IConfigurationAware;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.Unsafe;
import org.frankframework.encryption.AuthSSLContextFactory;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.HasTruststore;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.http.authentication.AuthenticationScheme;
import org.frankframework.http.authentication.ClientCredentialsBasicAuth;
import org.frankframework.http.authentication.ClientCredentialsQueryParameters;
import org.frankframework.http.authentication.HttpAuthenticationException;
import org.frankframework.http.authentication.IOauthAuthenticator;
import org.frankframework.http.authentication.OAuthPreferringAuthenticationStrategy;
import org.frankframework.http.authentication.ResourceOwnerPasswordCredentialsBasicAuth;
import org.frankframework.http.authentication.ResourceOwnerPasswordCredentialsQueryParameters;
import org.frankframework.http.authentication.SamlAssertionOauth;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.statistics.HasStatistics;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StringUtil;

/**
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
 * <li><code>-Djavax.net.debug=all</code></li>
 * </ul>
 * </p>
 * <p>
 * Note 3:
 * In case <code>javax.net.ssl.SSLHandshakeException: unknown certificate</code> exceptions are thrown,
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
 * In case <code>cannot create or initialize SocketFactory: (IOException) Unable to verify MAC</code> exceptions are thrown,
 * please check password or authAlias configuration of the corresponding certificate.
 * </p>
 *
 * @author Niels Meijer
 * @since 7.0
 */
public abstract class AbstractHttpSession implements ConfigurableLifecycle, HasKeystore, HasTruststore, HasStatistics, AdapterAware {
	protected final Logger log = LogUtil.getLogger(this);

	public static final String AUTHENTICATION_METHOD_KEY = "OauthAuthentication";

	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter String name;
	private @Getter @Setter ApplicationContext applicationContext;
	private @Setter MetricsInitializer configurationMetrics;
	private @Getter @Setter Adapter adapter;

	/* CONNECTION POOL */
	private @Getter int timeout = 10_000;
	private @Getter int maxConnections = 10;
	private @Getter int maxExecuteRetries = 1;
	private @Getter boolean staleChecking=true;
	private @Getter int staleTimeout = 5_000; // [ms]
	private @Getter int connectionTimeToLive = 900; // [seconds]
	private @Getter int connectionIdleTimeout = 10; // [seconds]
	private final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
	private @Getter HttpClientContext defaultHttpClientContext;
	private HttpClientContext httpClientContext;
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

	private @Getter String samlNameId;
	private @Getter String samlIssuer;
	private @Getter String samlAudience;
	private @Getter int samlAssertionExpiry; // [seconds]

	private @Getter OauthAuthenticationMethod oauthAuthenticationMethod;
	private @Getter IOauthAuthenticator authenticator;

	public enum OauthAuthenticationMethod {
		/**
		 * Requires {@literal tokenEndpoint}, {@literal clientId} and {@literal clientSecret} to be set.
		 * Implements <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.4">rfc6749</a>. The {@literal clientId} and {@literal clientSecret} are sent as basic authorization
		 * to the authorization server. The {@literal accessToken} is then used in the Authorization header to authenticate against the resource server.
		 */
		CLIENT_CREDENTIALS_BASIC_AUTH,

		/**
		 * Requires {@literal tokenEndpoint}, {@literal clientId} and {@literal clientSecret} to be set.
		 * Implements <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.4">rfc6749</a>. The {@literal clientId} and {@literal clientSecret} are sent in the form body
		 * to the authorization server. The {@literal accessToken} is then used in the Authorization header to authenticate against the resource server.
		 */
		CLIENT_CREDENTIALS_QUERY_PARAMETERS,

		/**
		 * Requires {@literal tokenEndpoint}, {@literal clientId}, {@literal clientSecret}, {@literal username} and {@literal password} to be set.
		 * Implements <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.3">rfc6749</a>. The {@literal clientId} and {@literal clientSecret} are sent as basic authorization
		 * to the authorization server. The {@literal username} and {@literal password} are sent in the form body to the authorization server.
		 * The {@literal accessToken} is then used in the Authorization header to authenticate against the resource server.
		 */
		RESOURCE_OWNER_PASSWORD_CREDENTIALS_BASIC_AUTH,

		/**
		 * Requires {@literal tokenEndpoint}, {@literal clientId}, {@literal clientSecret}, {@literal username} and {@literal password} to be set.
		 * Implements <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.3">rfc6749</a>. The {@literal clientId}, {@literal clientSecret}, {@literal username}
		 * and {@literal password} are sent in the form body to the authorization server.
		 * The {@literal accessToken} is then used in the Authorization header to authenticate against the resource server.
		 */
		RESOURCE_OWNER_PASSWORD_CREDENTIALS_QUERY_PARAMETERS,

		SAML_ASSERTION;

		public IOauthAuthenticator newAuthenticator(AbstractHttpSession session) throws HttpAuthenticationException {
			return switch (this) {
				case CLIENT_CREDENTIALS_BASIC_AUTH -> new ClientCredentialsBasicAuth(session);
				case CLIENT_CREDENTIALS_QUERY_PARAMETERS -> new ClientCredentialsQueryParameters(session);
				case RESOURCE_OWNER_PASSWORD_CREDENTIALS_BASIC_AUTH -> new ResourceOwnerPasswordCredentialsBasicAuth(session);
				case RESOURCE_OWNER_PASSWORD_CREDENTIALS_QUERY_PARAMETERS -> new ResourceOwnerPasswordCredentialsQueryParameters(session);
				case SAML_ASSERTION -> new SamlAssertionOauth(session);
			};
		}

		@Nullable
		public static OauthAuthenticationMethod determineOauthAuthenticationMethod(AbstractHttpSession session) {
			if (session.getTokenEndpoint() == null) {
				return null;
			}

			if (session.isAuthenticatedTokenRequest()) {
				if (StringUtils.isNotEmpty(session.getAuthAlias()) || StringUtils.isNotEmpty(session.getUsername())) {
					return RESOURCE_OWNER_PASSWORD_CREDENTIALS_BASIC_AUTH;
				} else {
					return CLIENT_CREDENTIALS_BASIC_AUTH;
				}
			} else {
				if (StringUtils.isNotEmpty(session.getAuthAlias()) || StringUtils.isNotEmpty(session.getUsername())) {
					return RESOURCE_OWNER_PASSWORD_CREDENTIALS_QUERY_PARAMETERS;
				} else {
					return CLIENT_CREDENTIALS_QUERY_PARAMETERS;
				}
			}
		}
	}

	private @Getter boolean authenticatedTokenRequest;

	/* PROXY */
	private @Getter String proxyHost;
	private @Getter int proxyPort = 80;
	private @Getter String proxyAuthAlias;
	private @Getter String proxyUsername;
	private @Getter String proxyPassword;
	private @Getter String proxyRealm=null;
	private @Getter boolean prefillProxyAuthCache;

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

	private @Getter boolean followRedirects=true;
	private @Getter boolean ignoreRedirects=false;

	private String protocol;
	private String supportedCipherSuites = null;
	private SSLConnectionSocketFactory sslSocketFactory;

	private boolean disableCookies = false;

	private CredentialFactory credentials;

	/**
	 * Makes sure only http(s) requests can be performed.
	 */
	protected URI getURI(@Nonnull String url) throws URISyntaxException {
		if(StringUtils.isBlank(url)) {
			throw new URISyntaxException("<null>", "no url provided");
		}
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

		log.info("created uri: scheme=[{}] host=[{}] path=[{}]", uri.getScheme(), uri.getHost(), uri.getPath());
		return uri.build();
	}

	@Override
	public void configure() throws ConfigurationException {
		// TODO find out if this really breaks proxy authentication or not.
		defaultHttpClientContext = HttpClientContext.create(); //Only create a new HttpContext when configure is called (which doesn't happen when using a SharedResource)
//		httpClientBuilder.disableAuthCaching();

		if (getMaxConnections() <= 0) {
			throw new ConfigurationException("maxConnections is set to ["+getMaxConnections()+"], which is not enough for adequate operation");
		}

		if (oauthAuthenticationMethod == null) {
			if (getTokenEndpoint() != null) {
				ConfigurationWarnings.add(this, log, "Use oauthAuthenticationMethod to explicitly set the Oauth2 method to be used. This is currently automatically determined, but will be removed in the future.");
			}
			oauthAuthenticationMethod = OauthAuthenticationMethod.determineOauthAuthenticationMethod(this);
		}

		if (oauthAuthenticationMethod != null) {
			try {
				authenticator = oauthAuthenticationMethod.newAuthenticator(this);
				authenticator.configure();
			} catch (HttpAuthenticationException e) {
				throw new ConfigurationException(e);
			}
		}

		validateProtocolsAndCiphers();

		AuthSSLContextFactory.verifyKeystoreConfiguration(this, this);

		if (StringUtils.isNotEmpty(getTokenEndpoint()) && StringUtils.isEmpty(getClientAuthAlias()) && StringUtils.isEmpty(getClientId())) {
			throw new ConfigurationException("To obtain accessToken at tokenEndpoint ["+getTokenEndpoint()+"] a clientAuthAlias or ClientId and ClientSecret must be specified");
		}

		credentials = getCredentialFactory();

		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
		requestConfigBuilder.setConnectTimeout(getTimeout());
		requestConfigBuilder.setConnectionRequestTimeout(getTimeout());
		requestConfigBuilder.setSocketTimeout(getTimeout());

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

		httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

		httpClientBuilder.setRetryHandler(new HttpRequestRetryHandler(getMaxExecuteRetries()));

		if(areCookiesDisabled()) {
			httpClientBuilder.disableCookieManagement();
		}
		httpClientBuilder.evictIdleConnections(getConnectionIdleTimeout(), TimeUnit.SECONDS);

		sslSocketFactory = getSSLConnectionSocketFactory(); //Configure it here, so we can handle exceptions

		preAuthenticate();
		configureRedirectStrategy();

		httpClientContext = defaultHttpClientContext; //Ensure a local instance is used when no SharedResource is present.
	}

	private void validateProtocolsAndCiphers() throws ConfigurationException {
		SSLParameters sslParams = null;
		try {
			sslParams = SSLContext.getDefault().getSupportedSSLParameters();
		} catch (NoSuchAlgorithmException e) {
			log.warn("no default SSLContext available", e);
		}

		if(StringUtils.isNotEmpty(protocol)) {
			try {
				SSLContext.getInstance(protocol);
			} catch (NoSuchAlgorithmException e) {
				String errorMessage = "unknown protocol ["+protocol+"]";
				if(sslParams != null) {
					errorMessage += ", must be one of ["+ String.join(", ", sslParams.getProtocols()) +"]";
				}
				throw new ConfigurationException(errorMessage, e);
			}
		}

		if(sslParams != null && StringUtils.isNotEmpty(supportedCipherSuites)) {
			List<String> allowedCipherSuites = Arrays.asList(sslParams.getCipherSuites());
			if(Collections.indexOfSubList(allowedCipherSuites, StringUtil.split(supportedCipherSuites)) == -1) {
				throw new ConfigurationException("Unsupported CipherSuite(s), must be one (or more) of "+allowedCipherSuites);
			}
		}
	}

	private CredentialFactory getCredentialFactory() {
		if (StringUtils.isNotEmpty(getAuthAlias()) || StringUtils.isNotEmpty(getUsername())) {
			return new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		}

		return new CredentialFactory(getClientAuthAlias(), getClientId(), getClientSecret());
	}

	/** The redirect strategy used to only redirect GET, DELETE and HEAD. */
	private void configureRedirectStrategy() {
		if(isFollowRedirects()) {
			httpClientBuilder.setRedirectStrategy(new DefaultRedirectStrategy(new String[] { HttpGet.METHOD_NAME, HttpHead.METHOD_NAME, HttpDelete.METHOD_NAME }));
		} else {
			httpClientBuilder.disableRedirectHandling();
		}
	}

	/**
	 * In order to support multiThreading and connectionPooling.
	 * The connectionManager has to be initialized with a sslSocketFactory.
	 * The pool must be re-created once closed.
	 *
	 */
	private PoolingHttpClientConnectionManager configureAndGetConnectionManager() {
		int timeToLive = getConnectionTimeToLive();
		if (timeToLive <= 0) {
			timeToLive = -1;
		}

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", sslSocketFactory)
				.build();

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, null, null, null, timeToLive, TimeUnit.SECONDS);
		log.debug("created PoolingHttpClientConnectionManager with custom SSLConnectionSocketFactory");

		connectionManager.setMaxTotal(getMaxConnections());
		connectionManager.setDefaultMaxPerRoute(getMaxConnections());

		if (isStaleChecking()) {
			log.info("set up connectionManager, setting stale checking [true]");
			connectionManager.setValidateAfterInactivity(getStaleTimeout());
		}

		return connectionManager;
	}

	@Override
	public void start() {
		buildHttpClient();
	}

	private void buildHttpClient() {
		PoolingHttpClientConnectionManager connectionManager = configureAndGetConnectionManager();
		httpClientBuilder.setConnectionManager(connectionManager);

		if (getApplicationContext() == null) {
			// If there's no applicationContext, this is probably a sender created in Larva, we're missing the spring context here
			// and we can't construct the interceptor. Besides that, it's probably not worth instrumenting either.
			httpClient = httpClientBuilder.build();
		} else {
			// Adapter is not always available, use this instead. Also see `org.frankframework.statistics.MetricsInitializer.getElementType`
			IConfigurationAware element = (adapter != null) ? adapter : this;

			registerConnectionMetrics(element, connectionManager);

			MicrometerHttpClientInterceptor interceptor = new MicrometerHttpClientInterceptor(configurationMetrics, element,
					request -> request.getRequestLine().getUri(),
					true
			);

			httpClient = httpClientBuilder
					.addInterceptorFirst(interceptor.getRequestInterceptor())
					.addInterceptorLast(interceptor.getResponseInterceptor())
					.build();
		}
	}

	/**
	 * Registers the gauges for httpClient connection metrics.
	 * @param frankElement
	 * @param connPoolControl
	 */
	private void registerConnectionMetrics(IConfigurationAware frankElement, ConnPoolControl<HttpRoute> connPoolControl) {
		configurationMetrics.createGauge(frankElement, FrankMeterType.SENDER_HTTP_POOL_MAX, () -> connPoolControl.getTotalStats().getMax());
		configurationMetrics.createGauge(frankElement, FrankMeterType.SENDER_HTTP_POOL_AVAILABLE, () -> connPoolControl.getTotalStats().getAvailable());
		configurationMetrics.createGauge(frankElement, FrankMeterType.SENDER_HTTP_POOL_LEASED, () -> connPoolControl.getTotalStats().getLeased());
		configurationMetrics.createGauge(frankElement, FrankMeterType.SENDER_HTTP_POOL_PENDING, () -> connPoolControl.getTotalStats().getPending());
	}

	protected void setHttpClient(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	protected void setHttpContext(HttpClientContext httpContext) {
		this.defaultHttpClientContext = httpContext;
	}

	@Override
	public boolean isRunning() {
		return getHttpClient() != null;
	}

	@Override
	public void stop() {
		//Close the HttpClient and ConnectionManager to release resources and potential open connections
		if(httpClient != null) {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.warn("unable to close HttpClient", e);
			}
		}
	}

	private void setupAuthentication(CredentialFactory proxyCredentials, HttpHost proxy, RequestConfig.Builder requestConfigBuilder) throws HttpAuthenticationException {
		Assert.notNull(defaultHttpClientContext, "no HttpClientContext created during configure"); //This should be set in #configure()

		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		if (StringUtils.isNotEmpty(credentials.getUsername()) || StringUtils.isNotEmpty(getTokenEndpoint())) {

			credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), getCredentials());

			AuthenticationScheme preferredAuthenticationScheme = getPreferredAuthenticationScheme();
			requestConfigBuilder.setTargetPreferredAuthSchemes(Collections.singletonList(preferredAuthenticationScheme.getSchemeName()));
			requestConfigBuilder.setAuthenticationEnabled(true);

			if (preferredAuthenticationScheme == AuthenticationScheme.OAUTH) {
				defaultHttpClientContext.setAttribute(AUTHENTICATION_METHOD_KEY, authenticator);
				httpClientBuilder.setTargetAuthenticationStrategy(new OAuthPreferringAuthenticationStrategy());
			}
		}
		if (proxy!=null) {
			AuthScope authScope = new AuthScope(proxy, proxyRealm, AuthScope.ANY_SCHEME);
			if (StringUtils.isNotEmpty(proxyCredentials.getUsername())) {
				Credentials httpCredentials = new UsernamePasswordCredentials(proxyCredentials.getUsername(), proxyCredentials.getPassword());
				credentialsProvider.setCredentials(authScope, httpCredentials);
			}
			log.trace("setting credentialProvider [{}]", credentialsProvider);

			if(isPrefillProxyAuthCache()) {
				requestConfigBuilder.setProxyPreferredAuthSchemes(List.of(AuthSchemes.BASIC));

				AuthCache authCache = defaultHttpClientContext.getAuthCache();
				if(authCache == null)
					authCache = new BasicAuthCache();

				authCache.put(proxy, new BasicScheme());
				defaultHttpClientContext.setAuthCache(authCache);
			}

		}

		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
	}

	private void preAuthenticate() {
		if (credentials != null && !StringUtils.isEmpty(credentials.getUsername())) {
			AuthState authState = defaultHttpClientContext.getTargetAuthState();
			if (authState==null) {
				authState = new AuthState();
				defaultHttpClientContext.setAttribute(HttpClientContext.TARGET_AUTH_STATE, authState);
			}
			authState.setState(AuthProtocolState.CHALLENGED);
			authState.update(getPreferredAuthenticationScheme().createScheme(), getCredentials());
		}
	}

	public Credentials getCredentials() {
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

	@Nonnull
	protected SSLConnectionSocketFactory getSSLConnectionSocketFactory() throws ConfigurationException {
		SSLConnectionSocketFactory sslConnectionSocketFactory;
		HostnameVerifier hostnameVerifier = verifyHostname ? new DefaultHostnameVerifier() : new NoopHostnameVerifier();

		final String[] supportedProtocols = StringUtils.isBlank(protocol) ? null : new String[] { protocol };
		final String[] cipherSuites;
		if(StringUtils.isNotBlank(supportedCipherSuites)) {
			cipherSuites = StringUtil.splitToStream(supportedCipherSuites).toArray(String[]::new);
		} else {
			cipherSuites = null;
		}

		try {
			javax.net.ssl.SSLSocketFactory socketFactory = AuthSSLContextFactory.createSSLSocketFactory(this, this, protocol);
			sslConnectionSocketFactory = new SSLConnectionSocketFactory(socketFactory, supportedProtocols, cipherSuites, hostnameVerifier);
		} catch (Exception e) {
			throw new ConfigurationException("cannot create or initialize SocketFactory", e);
		}

		// This method will be overwritten by the connectionManager when connectionPooling is enabled!
		// Can still be null when no default or an invalid system sslSocketFactory has been defined
		httpClientBuilder.setSSLSocketFactory(sslConnectionSocketFactory);

		return sslConnectionSocketFactory;
	}

	protected HttpResponse execute(URI targetUri, HttpRequestBase httpRequestBase, PipeLineSession session) throws IOException {
		HttpHost targetHost = new HttpHost(targetUri.getHost(), targetUri.getPort(), targetUri.getScheme());

		CloseableHttpClient client = getHttpClient();
		HttpClientContext context = httpClientContext != null ? httpClientContext : getOrCreateHttpClientContext(client, session);
		log.trace("executing request using HttpClient [{}] and HttpContext [{}]", client::hashCode, () -> context);
		return client.execute(targetHost, httpRequestBase, context);
	}

	private synchronized HttpClientContext getOrCreateHttpClientContext(CloseableHttpClient client, PipeLineSession session) {
		return (HttpClientContext) session.computeIfAbsent(PipeLineSession.SYSTEM_MANAGED_RESOURCE_PREFIX + "HttpContext" + client.hashCode(), key -> {
			HttpClientContext context = HttpClientContext.adapt(new BasicHttpContext(defaultHttpClientContext));
			context.setCookieStore(new BasicCookieStore());
			return context;
		});
	}

	/**
	 * Timeout in ms of obtaining a connection/result.
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

	/** Password used for authentication to the host */
	public void setPassword(String string) {
		password = string;
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
	@Deprecated(forRemoval = true, since = "9.0")
	@ConfigurationWarning("Use oauthAuthenticationMethod to set this behaviour")
	public void setAuthenticatedTokenRequest(boolean authenticatedTokenRequest) {
		this.authenticatedTokenRequest = authenticatedTokenRequest;
	}

	/**
	 * Only used when {@literal tokenEndpoint} has been configured. Sets the OAuth authentication method and controls which authentication flow should be used.
	 */
	public void setOauthAuthenticationMethod(OauthAuthenticationMethod oauthAuthenticationMethod) {
		this.oauthAuthenticationMethod = oauthAuthenticationMethod;
	}

	/**
	 * The nameId to be added during the creation of the SAML assertion.
	 */
	public void setSamlNameId(String samlNameId) {
		this.samlNameId = samlNameId;
	}

	/**
	 * The issuer to be added during the creation of the SAML assertion.
	 */
	public void setSamlIssuer(String samlIssuer) {
		this.samlIssuer = samlIssuer;
	}

	/**
	 * The audience to be added during the creation of the SAML assertion.
	 */
	public void setSamlAudience(String samlAudience) {
		this.samlAudience = samlAudience;
	}

	/**
	 * The time (in seconds) until the generated SAML assertion should be valid. A new assertion will be generated when the previous assertion is no longer valid.
	 */
	public void setSamlAssertionExpiry(int expiry) {
		this.samlAssertionExpiry = expiry;
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
	 * Create a pre-emptive login context for the proxy connection(s).
	 */
	public void setPrefillProxyAuthCache(boolean b) {
		this.prefillProxyAuthCache = b;
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

	/** Resource URL to truststore to be used for authenticating peer. If none specified, the JVMs default truststore will be used. */
	@Override
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
	@Unsafe
	public void setVerifyHostname(boolean b) {
		verifyHostname = b;
	}

	@Unsafe
	@Override
	public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		this.allowSelfSignedCertificates = allowSelfSignedCertificates;
	}

	@Override
	@Unsafe
	public void setIgnoreCertificateExpiredException(boolean b) {
		ignoreCertificateExpiredException = b;
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
	 * Used when <code>staleChecking</code> is <code>true</code>. Timeout after which an idle connection will be validated before being used.
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
	 * Secure socket protocol (such as 'TLSv1.2') to use when a SSLContext object is generated.
	 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html">Supported Protocols</a>.
	 * @ff.default TLSv1.2
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * Allows you to choose which CipherSuites are used when connecting to an endpoint. Works in tandem with {@code protocol} as the provided Suite may not be valid for the provided Protocol
	 * See the Java Security Standard Algorithm Names Specification for all available options. Note that these may differ depending on the JRE you're using.
	 *
	 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html">Java Security Standard Algorithm Names Specification</a>
	 */
	public void setSupportedCipherSuites(String supportedCipherSuites) {
		this.supportedCipherSuites = supportedCipherSuites;
	}
}
