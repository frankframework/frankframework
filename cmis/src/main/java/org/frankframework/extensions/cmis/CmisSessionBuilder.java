/*
   Copyright 2019 Nationale-Nederlanden, 2022, 2024 WeAreFrank!

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
package org.frankframework.extensions.cmis;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.chemistry.opencmis.client.SessionParameterMap;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.bindings.CmisBindingFactory;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.DateTimeFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.core.IScopeProvider;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.http.AbstractHttpSession;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;


public class CmisSessionBuilder {
	private final Logger log = LogUtil.getLogger(this);

	private BindingTypes bindingType = null;

	public enum BindingTypes {
		ATOMPUB, WEBSERVICES, BROWSER;
	}

	private String repository = null;
	private String url = null;

	/** Authentication **/
	private String authAlias;
	private String username;
	private String password;

	/** SSL TLS **/
	private boolean allowSelfSignedCertificates = false;
	private boolean verifyHostname = true;
	private boolean ignoreCertificateExpiredException = false;
	private String keystore = null;
	private String keystoreAuthAlias = null;
	private String keystorePassword = null;
	private String keystoreAlias = null;
	private String keystoreAliasAuthAlias = null;
	private String keystoreAliasPassword = null;
	private String truststore = null;
	private String truststoreAuthAlias = null;
	private String truststorePassword = null;
	private KeystoreType keystoreType = KeystoreType.PKCS12;
	private String keyManagerAlgorithm = "PKIX";
	private KeystoreType truststoreType = KeystoreType.JKS;
	private String trustManagerAlgorithm = "PKIX";

	/** PROXY **/
	private String proxyHost;
	private int proxyPort = 80;
	private String proxyAuthAlias;
	private String proxyUserName;
	private String proxyPassword;

	public static final String OVERRIDE_WSDL_URL = "http://fake.url";
	public static final String OVERRIDE_WSDL_KEY = "override_wsdl_key";
	private String overrideEntryPointWSDL;

	private IScopeProvider scopeProvider = null;

	private int maxConnections = 0;
	private int timeout = 0;

	public CmisSessionBuilder() {
	}
	public static CmisSessionBuilder create() {
		return new CmisSessionBuilder();
	}

	public CmisSessionBuilder(IScopeProvider scopeProvider) {
		this.scopeProvider = scopeProvider;
	}
	public static CmisSessionBuilder create(IScopeProvider scopeProvider) {
		return new CmisSessionBuilder(scopeProvider);
	}

	/**
	 * @return a {@link Session} connected to the CMIS repository
	 * @throws CmisSessionException when the CmisSessionBuilder fails to connect to cmis repository
	 */
	public CloseableCmisSession build() throws CmisSessionException {
		CredentialFactory cf = new CredentialFactory(authAlias, username, password);
		return build(cf.getUsername(), cf.getPassword(), Collections.emptyMap());
	}

	/**
	 * @param userName to connect or empty when no username
	 * @param password
	 * @return a {@link Session} connected to the CMIS repository
	 * @throws CmisSessionException when the CmisSessionBuilder fails to connect to cmis repository
	 */
	public CloseableCmisSession build(@Nullable String userName, @Nullable String password, @Nonnull Map<String, String> headers) throws CmisSessionException {
		if (StringUtils.isEmpty(url) && overrideEntryPointWSDL == null) {
			throw new CmisSessionException("no url configured");
		}
		if (StringUtils.isEmpty(repository)) {
			throw new CmisSessionException("no repository configured");
		}
		if (getBindingType() == null) {
			throw new CmisSessionException("no bindingType configured");
		}
		if(overrideEntryPointWSDL != null && getBindingType() != BindingTypes.WEBSERVICES) {
			throw new CmisSessionException("illegal value for bindingtype [" + getBindingType() + "], overrideEntryPointWSDL only supports webservices");
		}

		log.debug("connecting to url [{}] repository [{}]", url, repository);

		SessionParameterMap parameterMap = new SessionParameterMap();

		if(StringUtils.isNotEmpty(userName))
			parameterMap.setUserAndPassword(userName, password);

		if (getBindingType() == BindingTypes.ATOMPUB) {
			parameterMap.setAtomPubBindingUrl(url);
			parameterMap.setUsernameTokenAuthentication(false);
			parameterMap.put(SessionParameter.BINDING_SPI_CLASS, CmisCustomAtomPubSpi.class.getName());
			log.debug("Added custom BINDING_SPI_CLASS CmisCustomAtomPubSpi to new CmisSession");

			// Apply defaults that should be set by CmisBindingFactory but are now skipped with custom binding type
			parameterMap.put(SessionParameter.AUTH_SOAP_USERNAMETOKEN, "false");
		} else if (getBindingType() == BindingTypes.BROWSER) {
			parameterMap.setBrowserBindingUrl(url);
			parameterMap.setBasicAuthentication();
			//Add parameter dateTimeFormat to send dates in ISO format instead of milliseconds.
			parameterMap.put(SessionParameter.BROWSER_DATETIME_FORMAT, DateTimeFormat.EXTENDED.value());
			parameterMap.put(SessionParameter.BINDING_SPI_CLASS, CmisCustomBrowserBindingSpi.class.getName());
			log.debug("Added custom BINDING_SPI_CLASS CmisCustomBrowserBindingSpi to new CmisSession");

			// Apply defaults that should be set by CmisBindingFactory but are now skipped with custom binding type
			parameterMap.put(SessionParameter.BROWSER_SUCCINCT, "true");
			parameterMap.put(SessionParameter.AUTH_SOAP_USERNAMETOKEN, "false");

		} else { // BindingTypes.WEBSERVICES
			parameterMap.setUsernameTokenAuthentication(true);
			parameterMap.put(SessionParameter.BINDING_SPI_CLASS, CmisCustomWebServicesSpi.class.getName());
			log.debug("Added custom BINDING_SPI_CLASS CmisCustomWebServicesSpi to new CmisSession");

			// OpenCMIS requires an entrypoint url (wsdl), if this url has been secured and is not publicly accessible,
			// we can manually override this wsdl by reading it from the classpath.
			//TODO: Does this work with any binding type?
			if(overrideEntryPointWSDL != null) {
				URL url = ClassLoaderUtils.getResourceURL(scopeProvider, overrideEntryPointWSDL);
				if(url != null) {
					try {
						parameterMap.put(OVERRIDE_WSDL_KEY, StreamUtil.streamToString(url.openStream()));

						//We need to setup a fake URL in order to initialize the CMIS Session
						parameterMap.setWebServicesBindingUrl(OVERRIDE_WSDL_URL);
					}
					catch (IOException e) {
						//eg. if the named charset is not supported
						throw new CmisSessionException("error reading overrideEntryPointWSDL["+overrideEntryPointWSDL+"]");
					}
				} else {
					throw new CmisSessionException("cannot find overrideEntryPointWSDL["+overrideEntryPointWSDL+"]");
				}
			} else {
				parameterMap.setWebServicesBindingUrl(url);

				parameterMap.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, url + "/RepositoryService.svc?wsdl");
				parameterMap.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, url + "/NavigationService.svc?wsdl");
				parameterMap.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, url + "/ObjectService.svc?wsdl");
				parameterMap.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, url + "/VersioningService.svc?wsdl");
				parameterMap.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, url + "/DiscoveryService.svc?wsdl");
				parameterMap.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, url + "/RelationshipService.svc?wsdl");
				parameterMap.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, url + "/MultiFilingService.svc?wsdl");
				parameterMap.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, url + "/PolicyService.svc?wsdl");
				parameterMap.put(SessionParameter.WEBSERVICES_ACL_SERVICE, url + "/ACLService.svc?wsdl");
			}
			if (!parameterMap.containsKey(SessionParameter.AUTH_SOAP_USERNAMETOKEN)) {
				parameterMap.put(SessionParameter.AUTH_SOAP_USERNAMETOKEN, "true");
			}
		}
		parameterMap.setRepositoryId(repository);

		//SSL
		if (keystore!=null || truststore!=null || allowSelfSignedCertificates) {
			CredentialFactory keystoreCf = new CredentialFactory(keystoreAuthAlias, null, keystorePassword);
			CredentialFactory keystoreAliasCf = StringUtils.isNotEmpty(keystoreAliasAuthAlias) || StringUtils.isNotEmpty(keystoreAliasPassword)
							?  new CredentialFactory(keystoreAliasAuthAlias, null, keystoreAliasPassword)
							: keystoreCf;
			CredentialFactory truststoreCf = new CredentialFactory(truststoreAuthAlias,  null, truststorePassword);

			parameterMap.put("keystoreUrl", keystore);
			parameterMap.put("keystorePassword", keystoreCf.getPassword());
			parameterMap.put("keystoreType", keystoreType.name());
			parameterMap.put("keystoreAlias", keystoreAlias);
			parameterMap.put("keystoreAliasPassword", keystoreAliasCf.getPassword());
			parameterMap.put("keyManagerAlgorithm", keyManagerAlgorithm);
			parameterMap.put("truststoreUrl", truststore);
			parameterMap.put("truststorePassword", truststoreCf.getPassword());
			parameterMap.put("truststoreType", truststoreType.name());
			parameterMap.put("trustManagerAlgorithm", trustManagerAlgorithm);
		}

		// SSL+
		parameterMap.put("isAllowSelfSignedCertificates", "" + allowSelfSignedCertificates);
		parameterMap.put("isVerifyHostname", "" + verifyHostname);
		parameterMap.put("isIgnoreCertificateExpiredException", "" + ignoreCertificateExpiredException);

		// PROXY
		if (StringUtils.isNotEmpty(proxyHost)) {
			CredentialFactory pcf = new CredentialFactory(proxyAuthAlias, proxyUserName, proxyPassword);
			parameterMap.put("proxyHost", proxyHost);
			parameterMap.put("proxyPort", "" + proxyPort);
			parameterMap.put("proxyUsername", pcf.getUsername());
			parameterMap.put("proxyPassword", pcf.getPassword());
		}

		if(maxConnections > 0)
			parameterMap.put("maxConnections", maxConnections);

		if(timeout > 0)
			parameterMap.put(SessionParameter.READ_TIMEOUT, timeout);

		// Custom CMIS HttpSender to support ssl connections and proxies
		parameterMap.setHttpInvoker(CmisHttpInvoker.class);

		// Since we specify our own Binding SPI classes to fix a leak, we have to set binding-type for the binding factory to "CUSTOM".
		parameterMap.put(SessionParameter.BINDING_TYPE, BindingType.CUSTOM.value());

		// Set up session parameters that would normally be done by the CmisBindingFactory but have to be
		// done here manually b/c of using "custom bindings".
		if (!parameterMap.containsKey(SessionParameter.AUTHENTICATION_PROVIDER_CLASS)) {
			parameterMap.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS, CmisBindingFactory.STANDARD_AUTHENTICATION_PROVIDER);
		}
		if (!parameterMap.containsKey(SessionParameter.TYPE_DEFINITION_CACHE_CLASS)) {
			parameterMap.put(SessionParameter.TYPE_DEFINITION_CACHE_CLASS, CmisBindingFactory.DEFAULT_TYPE_DEFINITION_CACHE_CLASS);
		}
		if (!parameterMap.containsKey(SessionParameter.AUTH_HTTP_BASIC)) {
			parameterMap.put(SessionParameter.AUTH_HTTP_BASIC, "true");
		}

		// Add a list of headers, ensure only headers (using the CmisSender HEADER_PREFIX) are added.
		headers.entrySet().stream()
			.filter(entry -> entry.getKey().startsWith(CmisSender.HEADER_PARAM_PREFIX))
			.forEach(entry -> parameterMap.put(entry.getKey(), entry.getValue()));

		CloseableCmisSession session = new CloseableCmisSession(parameterMap);
		session.connect();
		log.debug("connected with repository [{}]", ()->getRepositoryInfo(session));

		return session;
	}

	public static String getRepositoryInfo(Session cmisSession) {
		RepositoryInfo ri = cmisSession.getRepositoryInfo();
		String id = ri.getId();
		String productName = ri.getProductName();
		String productVersion = ri.getProductVersion();
		String cmisVersion = ri.getCmisVersion().value();
		return "id [" + id + "] cmis version [" + cmisVersion + "] product ["
				+ productName + "] version [" + productVersion + "]";
	}

	public CmisSessionBuilder setOverrideEntryPointWSDL(String overrideEntryPointWSDL) {
		// never return an empty string, always null!
		if(!overrideEntryPointWSDL.isEmpty())
			this.overrideEntryPointWSDL = overrideEntryPointWSDL;

		return this;
	}

	public CmisSessionBuilder setKeystore(String string) {
		keystore = string;
		return this;
	}

	public CmisSessionBuilder setKeystoreType(KeystoreType value) {
		keystoreType = value;
		return this;
	}

	public CmisSessionBuilder setKeystoreAuthAlias(String string) {
		keystoreAuthAlias = string;
		return this;
	}

	public CmisSessionBuilder setKeystorePassword(String string) {
		keystorePassword = string;
		return this;
	}

	public CmisSessionBuilder setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
		return this;
	}

	public CmisSessionBuilder setKeystoreAlias(String string) {
		keystoreAlias = string;
		return this;
	}
	public CmisSessionBuilder setKeystoreAliasAuthAlias(String string) {
		keystoreAliasAuthAlias = string;
		return this;
	}
	public CmisSessionBuilder setKeystoreAliasPassword(String string) {
		keystoreAliasPassword = string;
		return this;
	}

	public CmisSessionBuilder setTruststore(String string) {
		truststore = string;
		return this;
	}

	public CmisSessionBuilder setTruststoreAuthAlias(String string) {
		truststoreAuthAlias = string;
		return this;
	}

	public CmisSessionBuilder setTruststorePassword(String string) {
		truststorePassword = string;
		return this;
	}

	public CmisSessionBuilder setTruststoreType(KeystoreType value) {
		truststoreType = value;
		return this;
	}

	public CmisSessionBuilder setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		this.trustManagerAlgorithm = trustManagerAlgorithm;
		return this;
	}

	public CmisSessionBuilder setVerifyHostname(boolean b) {
		verifyHostname = b;
		return this;
	}

	public CmisSessionBuilder setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		this.allowSelfSignedCertificates = allowSelfSignedCertificates;
		return this;
	}

	public CmisSessionBuilder setIgnoreCertificateExpiredException(boolean b) {
		ignoreCertificateExpiredException = b;
		return this;
	}

	public CmisSessionBuilder setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
		return this;
	}

	public CmisSessionBuilder setProxyPort(int i) {
		proxyPort = i;
		return this;
	}

	public CmisSessionBuilder setProxyAuthAlias(String string) {
		proxyAuthAlias = string;
		return this;
	}

	public CmisSessionBuilder setProxyUsername(String string) {
		proxyUserName = string;
		return this;
	}

	public CmisSessionBuilder setProxyPassword(String string) {
		proxyPassword = string;
		return this;
	}

	public CmisSessionBuilder setUrl(String url) {
		if(StringUtils.isEmpty(url))
			throw new IllegalArgumentException("url must be set");

		this.url = url;
		return this;
	}

	public CmisSessionBuilder setRepository(String repository) {
		if(StringUtils.isEmpty(repository))
			throw new IllegalArgumentException("repository must be set");

		this.repository = repository;
		return this;
	}

	public CmisSessionBuilder setAuthAlias(String string) {
		authAlias = string;
		return this;
	}

	public CmisSessionBuilder setUsername(String string) {
		username = string;
		return this;
	}

	public CmisSessionBuilder setPassword(String string) {
		password = string;
		return this;
	}

	/**
	 * @param bindingType See {@link CmisSessionBuilder.BindingTypes} for possible binding types
	 */
	public CmisSessionBuilder setBindingType(BindingTypes bindingType) {
		this.bindingType = bindingType;
		return this;
	}

	private BindingTypes getBindingType() {
		return bindingType;
	}

	/**
	 * the maximum number of concurrent connections, 0 uses default
	 */
	public CmisSessionBuilder setMaxConnections(int i) {
		if(i < 0)
			throw new IllegalArgumentException("illegal value ["+i+"] for maxConnections, must be 0 or larger");

		maxConnections = i;
		return this;
	}

	/**
	 * READ_TIMEOUT timeout in MS.
	 * Defaults to 10000, inherited from {@link AbstractHttpSession#setTimeout(int)}.
	 */
	public CmisSessionBuilder setTimeout(int i) {
		if(i < 1)
			throw new IllegalArgumentException("illegal value ["+i+"] for timeout, must be 1 or larger");

		timeout = i;
		return this;
	}

	@Override
	public String toString() {
		return StringUtil.reflectionToString(this);
	}

	public String getKeystore() {
		return keystore;
	}
	public KeystoreType getKeystoreType() {
		return keystoreType;
	}
	public String getKeystoreAuthAlias() {
		return keystoreAuthAlias;
	}
	public String getKeystorePassword() {
		return keystorePassword;
	}
	public String getKeystoreAlias() {
		return keystoreAlias;
	}
	public String getKeystoreAliasAuthAlias() {
		return keystoreAliasAuthAlias;
	}
	public String getKeystoreAliasPassword() {
		return keystoreAliasPassword;
	}
	public String getKeyManagerAlgorithm() {
		return keyManagerAlgorithm;
	}
	public String getTruststore() {
		return truststore;
	}
	public KeystoreType getTruststoreType() {
		return truststoreType;
	}
	public String getTruststoreAuthAlias() {
		return truststoreAuthAlias;
	}
	public String getTruststorePassword() {
		return truststorePassword;
	}
	public String getTrustManagerAlgorithm() {
		return trustManagerAlgorithm;
	}
	public boolean isVerifyHostname() {
		return verifyHostname;
	}
	public boolean isAllowSelfSignedCertificates() {
		return allowSelfSignedCertificates;
	}
	public boolean isIgnoreCertificateExpiredException() {
		return ignoreCertificateExpiredException;
	}
}
