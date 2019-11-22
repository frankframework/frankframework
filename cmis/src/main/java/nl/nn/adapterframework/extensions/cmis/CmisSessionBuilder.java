/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.cmis;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.chemistry.opencmis.client.SessionParameterMap;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.DateTimeFormat;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class CmisSessionBuilder {
	private final Logger log = LogUtil.getLogger(this);

	private String bindingType = null;
	/**
	 * 'atompub', 'webservices' or 'browser'
	 */
	private enum BindingTypes {
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
	private String certificate = null;
	private String certificateAuthAlias = null;
	private String certificatePassword = null;
	private String truststore = null;
	private String truststoreAuthAlias = null;
	private String truststorePassword = null;
	private String keystoreType = "pkcs12";
	private String keyManagerAlgorithm = "PKIX";
	private String truststoreType = "jks";
	private String trustManagerAlgorithm = "PKIX";

	/** PROXY **/
	private String proxyHost;
	private int proxyPort = 80;
	private String proxyAuthAlias;
	private String proxyUserName;
	private String proxyPassword;

	public final static String OVERRIDE_WSDL_URL = "http://fake.url";
	public final static String OVERRIDE_WSDL_KEY = "override_wsdl_key";
	private String overrideEntryPointWSDL;

	private ClassLoader classLoader = this.getClass().getClassLoader();

	private int maxConnections = 0;
	private int timeout = 0;

	public CmisSessionBuilder() {
	}
	public static CmisSessionBuilder create() {
		return new CmisSessionBuilder();
	}

	public CmisSessionBuilder(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	public static CmisSessionBuilder create(ClassLoader classLoader) {
		return new CmisSessionBuilder(classLoader);
	}

	/**
	 * @return a {@link Session} connected to the CMIS repository
	 * @throws CmisSessionException when the CmisSessionBuilder fails to connect to cmis repository
	 */
	public Session build() throws CmisSessionException {
		CredentialFactory cf = new CredentialFactory(authAlias, username, password);
		return build(cf.getUsername(), cf.getPassword());
	}

	/**
	 * @param userName to connect or empty when no username
	 * @param password 
	 * @return a {@link Session} connected to the CMIS repository
	 * @throws CmisSessionException when the CmisSessionBuilder fails to connect to cmis repository
	 */
	public Session build(String userName, String password) throws CmisSessionException {
		if (StringUtils.isEmpty(url) && overrideEntryPointWSDL == null) {
			throw new CmisSessionException("no url configured");
		}
		if (StringUtils.isEmpty(repository)) {
			throw new CmisSessionException("no repository configured");
		}
		if(overrideEntryPointWSDL != null && !"webservices".equals(getBindingType())) {
			throw new CmisSessionException("illegal value for bindingtype [" + getBindingType() + "], overrideEntryPointWSDL only supports webservices");
		}

		log.debug("connecting to url ["+ url +"] repository ["+ repository +"]");

		SessionParameterMap parameterMap = new SessionParameterMap();

		if(StringUtils.isNotEmpty(userName))
			parameterMap.setUserAndPassword(userName, password);

		if (getBindingType().equalsIgnoreCase("atompub")) {
			parameterMap.setAtomPubBindingUrl(url);
			parameterMap.setUsernameTokenAuthentication(false);
		} else if (getBindingType().equalsIgnoreCase("browser")) {
			parameterMap.setBrowserBindingUrl(url);
			parameterMap.setBasicAuthentication();
			//Add parameter dateTimeFormat to send dates in ISO format instead of milliseconds.
			parameterMap.put(SessionParameter.BROWSER_DATETIME_FORMAT, DateTimeFormat.EXTENDED.value());
		} else {
			parameterMap.setUsernameTokenAuthentication(true);
			// OpenCMIS requires an entrypoint url (wsdl), if this url has been secured and is not publicly accessible,
			// we can manually override this wsdl by reading it from the classpath.
			//TODO: Does this work with any binding type?
			if(overrideEntryPointWSDL != null) {
				URL url = ClassUtils.getResourceURL(classLoader, overrideEntryPointWSDL);
				if(url != null) {
					try {
						parameterMap.put(OVERRIDE_WSDL_KEY, Misc.streamToString(url.openStream()));

						//We need to setup a fake URL in order to initialize the CMIS Session
						parameterMap.setWebServicesBindingUrl(OVERRIDE_WSDL_URL);
					}
					catch (IOException e) {
						//eg. if the named charset is not supported
						throw new CmisSessionException("error reading overrideEntryPointWSDL["+overrideEntryPointWSDL+"]");
					}
				}
				else {
					throw new CmisSessionException("cannot find overrideEntryPointWSDL["+overrideEntryPointWSDL+"]");
				}
			}
			else {
				parameterMap.setWebServicesBindingUrl(url);

				parameterMap.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
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
		}
		parameterMap.setRepositoryId(repository);

		//SSL
		if (certificate!=null || truststore!=null || allowSelfSignedCertificates) {
			CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
			CredentialFactory truststoreCf  = new CredentialFactory(truststoreAuthAlias,  null, truststorePassword);

			parameterMap.put("certificateUrl", certificate);
			parameterMap.put("certificatePassword", certificateCf.getPassword());
			parameterMap.put("keystoreType", keystoreType);
			parameterMap.put("keyManagerAlgorithm", keyManagerAlgorithm);
			parameterMap.put("truststoreUrl", truststore);
			parameterMap.put("truststorePassword", truststoreCf.getPassword());
			parameterMap.put("truststoreType", truststoreType);
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
			parameterMap.put("proxyUserName", pcf.getUsername());
			parameterMap.put("proxyPassword", pcf.getPassword());
		}

		if(maxConnections > 0)
			parameterMap.put("maxConnections", maxConnections);

		if(timeout > 0)
			parameterMap.put(SessionParameter.CONNECT_TIMEOUT, timeout);

		// Custom IBIS HttpSender to support ssl connections and proxies
		parameterMap.setHttpInvoker(nl.nn.adapterframework.extensions.cmis.CmisHttpInvoker.class);

		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
		Session session = sessionFactory.createSession(parameterMap);
		log.debug("connected with repository [" + getRepositoryInfo(session) + "]");

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

	public CmisSessionBuilder setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		this.allowSelfSignedCertificates = allowSelfSignedCertificates;
		return this;
	}

	public CmisSessionBuilder setVerifyHostname(boolean verifyHostname) {
		this.verifyHostname = verifyHostname;
		return this;
	}

	public CmisSessionBuilder setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		this.ignoreCertificateExpiredException = ignoreCertificateExpiredException;
		return this;
	}

	public CmisSessionBuilder setCertificateUrl(String certificate) {
		this.certificate = certificate;
		return this;
	}

	public CmisSessionBuilder setCertificateAuthAlias(String certificateAuthAlias) {
		this.certificateAuthAlias = certificateAuthAlias;
		return this;
	}


	public CmisSessionBuilder setCertificatePassword(String certificatePassword) {
		this.certificatePassword = certificatePassword;
		return this;
	}

	public CmisSessionBuilder setTruststore(String truststore) {
		this.truststore = truststore;
		return this;
	}

	public CmisSessionBuilder setTruststoreAuthAlias(String truststoreAuthAlias) {
		this.truststoreAuthAlias = truststoreAuthAlias;
		return this;
	}

	public CmisSessionBuilder setTruststorePassword(String truststorePassword) {
		this.truststorePassword = truststorePassword;
		return this;
	}

	public CmisSessionBuilder setKeystoreType(String keystoreType) {
		this.keystoreType = keystoreType;
		return this;
	}

	public CmisSessionBuilder setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
		return this;
	}

	public CmisSessionBuilder setTruststoreType(String truststoreType) {
		this.truststoreType = truststoreType;
		return this;
	}

	public CmisSessionBuilder setTrustManagerAlgorithm(String getTrustManagerAlgorithm) {
		this.trustManagerAlgorithm = getTrustManagerAlgorithm;
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

	public CmisSessionBuilder setProxyUserName(String string) {
		proxyUserName = string;
		return this;
	}

	public CmisSessionBuilder setProxyPassword(String string) {
		proxyPassword = string;
		return this;
	}

	public CmisSessionBuilder setUrl(String url) throws ConfigurationException {
		if(StringUtils.isEmpty(url))
			throw new ConfigurationException("url must be set");

		this.url = url;
		return this;
	}

	public CmisSessionBuilder setRepository(String repository) throws ConfigurationException {
		if(StringUtils.isEmpty(repository))
			throw new ConfigurationException("repository must be set");

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
	 * @param bindingType See {@link BindingTypes BindingTypes} for possible binding types
	 */
	public CmisSessionBuilder setBindingType(String bindingType) throws ConfigurationException {
		try {
			BindingTypes type = BindingTypes.valueOf(bindingType.toUpperCase());

			this.bindingType = type.name();
		}
		catch(IllegalArgumentException e) {
			throw new ConfigurationException("illegal value for bindingType ["+bindingType+"] must be one of " + Arrays.asList(BindingTypes.values()));
		}

		return this;
	}

	private String getBindingType() {
		if(bindingType != null)
			return bindingType.toLowerCase();

		return null;
	}

	/**
	 * the maximum number of concurrent connections, 0 uses default
	 */
	public CmisSessionBuilder setMaxConnections(int i) throws ConfigurationException {
		if(i < 0)
			throw new ConfigurationException("illegal value ["+i+"] for maxConnections, must be 0 or larger");

		maxConnections = i;
		return this;
	}

	/**
	 * the maximum number of concurrent connections, 0 uses default
	 */
	public CmisSessionBuilder setTimeout(int i) throws ConfigurationException {
		if(i < 1)
			throw new ConfigurationException("illegal value ["+i+"] for timeout, must be 1 or larger");

		timeout = i;
		return this;
	}

	@Override
	public String toString() {
		return (new ReflectionToStringBuilder(this) {
			protected boolean accept(Field f) {
				return super.accept(f) && !f.getName().contains("password");
			}
		}).toString();
	}
}
