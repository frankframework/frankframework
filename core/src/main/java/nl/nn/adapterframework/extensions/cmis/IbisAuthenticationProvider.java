package nl.nn.adapterframework.extensions.cmis;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.http.AuthSSLProtocolSocketFactory;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.chemistry.opencmis.client.bindings.spi.AbstractAuthenticationProvider;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.cookies.CmisCookieManager;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.Base64;
import org.apache.chemistry.opencmis.commons.impl.DateTimeHelper;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.chemistry.opencmis.commons.impl.XMLUtils;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Standard authentication provider class.
 * 
 * Adds a basic authentication HTTP header and a WS-Security UsernameToken SOAP
 * header.
 */
public class IbisAuthenticationProvider extends AbstractAuthenticationProvider {

	private static final long serialVersionUID = 1L;

	protected static final String WSSE_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
	protected static final String WSU_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

	private CmisCookieManager cookieManager;
	private Map<String, List<String>> fixedHeaders = new HashMap<String, List<String>>();

	protected Logger log = LogUtil.getLogger(this);
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

	@Override
	public void setSession(BindingSession session) {
		super.setSession(session);

		boolean sendBasicAuth = getSendBasicAuth();

		if (getHandleCookies() && cookieManager == null) {
			cookieManager = new CmisCookieManager(session.getSessionId());
		}

		// basic authentication
		if (sendBasicAuth) {
			// get user and password
			String user = getUser();
			String password = getPassword();

			// if no user is set, don't set basic auth header
			if (user != null) {
				fixedHeaders.put("Authorization", createBasicAuthHeaderValue(user, password));
			}
		}

		boolean sendBearerToken = getSendBearerToken();

		// send bearer token
		if (sendBearerToken) {
			String token = getBearerToken();

			// if no token is set, don't set bearer header
			if (token != null) {
				fixedHeaders.put("Authorization", Collections.singletonList("Bearer " + token));
			}
		}

		// proxy authentication
		if (getProxyUser() != null) {
			// get proxy user and password
			String proxyUser = getProxyUser();
			String proxyPassword = getProxyPassword();

			fixedHeaders.put("Proxy-Authorization", createBasicAuthHeaderValue(proxyUser, proxyPassword));
		}

		// other headers
		addSessionParameterHeadersToFixedHeaders();
	}

	@Override
	public Map<String, List<String>> getHTTPHeaders(String url) {
		Map<String, List<String>> result = new HashMap<String, List<String>>(fixedHeaders);

		// cookies
		if (cookieManager != null) {
			Map<String, List<String>> cookies = cookieManager.get(url, result);
			if (!cookies.isEmpty()) {
				result.putAll(cookies);
			}
		}

		return result.isEmpty() ? null : result;
	}

	@Override
	public void putResponseHeaders(String url, int statusCode, Map<String, List<String>> headers) {
		if (cookieManager != null) {
			cookieManager.put(url, headers);
		}
	}

	@Override
	public Element getSOAPHeaders(Object portObject) {
		// only send SOAP header if configured
		if (!getSendUsernameToken()) {
			return null;
		}

		// get user and password
		String user = getUser();
		String password = getPassword();

		// if no user is set, don't create SOAP header
		if (user == null) {
			return null;
		}

		if (password == null) {
			password = "";
		}

		// set time
		long created = System.currentTimeMillis();
		long expires = created + 24 * 60 * 60 * 1000; // 24 hours

		// create the SOAP header
		try {
			Document document = XMLUtils.newDomDocument();

			Element wsseSecurityElement = document.createElementNS(WSSE_NAMESPACE, "Security");

			Element wsuTimestampElement = document.createElementNS(WSU_NAMESPACE, "Timestamp");
			wsseSecurityElement.appendChild(wsuTimestampElement);

			Element tsCreatedElement = document.createElementNS(WSU_NAMESPACE, "Created");
			tsCreatedElement.appendChild(document.createTextNode(DateTimeHelper.formatXmlDateTime(created)));
			wsuTimestampElement.appendChild(tsCreatedElement);

			Element tsExpiresElement = document.createElementNS(WSU_NAMESPACE, "Expires");
			tsExpiresElement.appendChild(document.createTextNode(DateTimeHelper.formatXmlDateTime(expires)));
			wsuTimestampElement.appendChild(tsExpiresElement);

			Element usernameTokenElement = document.createElementNS(WSSE_NAMESPACE, "UsernameToken");
			wsseSecurityElement.appendChild(usernameTokenElement);

			Element usernameElement = document.createElementNS(WSSE_NAMESPACE, "Username");
			usernameElement.appendChild(document.createTextNode(user));
			usernameTokenElement.appendChild(usernameElement);

			Element passwordElement = document.createElementNS(WSSE_NAMESPACE, "Password");
			passwordElement.appendChild(document.createTextNode(password));
			passwordElement.setAttribute("Type",
					"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
			usernameTokenElement.appendChild(passwordElement);

			Element createdElement = document.createElementNS(WSU_NAMESPACE, "Created");
			createdElement.appendChild(document.createTextNode(DateTimeHelper.formatXmlDateTime(created)));
			usernameTokenElement.appendChild(createdElement);

			return wsseSecurityElement;
		} catch (Exception e) {
			// shouldn't happen...
			throw new CmisRuntimeException("Could not build SOAP header: " + e.getMessage(), e);
		}
	}

	/**
	 * Returns the HTTP headers that are sent with all requests. The returned
	 * map is mutable but not synchronized!
	 */
	protected Map<String, List<String>> getFixedHeaders() {
		return fixedHeaders;
	}

	/**
	 * Adds the {@link SessionParameter.HEADER} to the fixed headers. This
	 * method should only be called from the {@link #setSession(BindingSession)}
	 * method to avoid threading issues.
	 */
	protected void addSessionParameterHeadersToFixedHeaders() {
		int x = 0;
		Object headerParam;
		while ((headerParam = getSession().get(SessionParameter.HEADER + "." + x)) != null) {
			String header = headerParam.toString();
			int colon = header.indexOf(':');
			if (colon > -1) {
				String key = header.substring(0, colon).trim();
				if (key.length() > 0) {
					String value = header.substring(colon + 1).trim();
					List<String> values = fixedHeaders.get(key);
					if (values == null) {
						fixedHeaders.put(key, Collections.singletonList(value));
					} else {
						List<String> newValues = new ArrayList<String>(values);
						newValues.add(value);
						fixedHeaders.put(key, newValues);
					}
				}
			}
			x++;
		}
	}

	/**
	 * Creates a basic authentication header value from a username and a
	 * password.
	 */
	protected List<String> createBasicAuthHeaderValue(String username, String password) {
		if (password == null) {
			password = "";
		}

		Object charset = getSession().get(SessionParameter.AUTH_HTTP_BASIC_CHARSET);
		if (charset instanceof String) {
			charset = ((String) charset).trim();
		} else {
			charset = IOUtils.UTF8;
		}

		byte[] usernamePassword;
		try {
			usernamePassword = (username + ":" + password).getBytes((String) charset);
		} catch (UnsupportedEncodingException e) {
			throw new CmisRuntimeException("Unsupported encoding '" + charset + "'!", e);
		}

		return Collections.singletonList("Basic " + Base64.encodeBytes(usernamePassword));
	}

	/**
	 * Returns if a HTTP Basic Authentication header should be sent. (All
	 * bindings.)
	 */
	protected boolean getSendBasicAuth() {
		return getSession().get(SessionParameter.AUTH_HTTP_BASIC, false);
	}

	/**
	 * Returns if an OAuth Bearer token header should be sent. (All bindings.)
	 */
	protected boolean getSendBearerToken() {
		return getSession().get(SessionParameter.AUTH_OAUTH_BEARER, false);
	}

	/**
	 * Returns if a UsernameToken should be sent. (Web Services binding only.)
	 */
	protected boolean getSendUsernameToken() {
		return getSession().get(SessionParameter.AUTH_SOAP_USERNAMETOKEN, false);
	}

	/**
	 * Returns if the authentication provider should handle cookies.
	 */
	protected boolean getHandleCookies() {
		return getSession().get(SessionParameter.COOKIES, false);
	}

	public SSLSocketFactory getSSLSocketFactory() {
//		System.out.println("Called class: "+new Exception().getStackTrace()[1].getClassName());

		BindingSession session = getSession();

		URL certificateUrl=null;
		URL truststoreUrl=null;

		String certificate = (String) session.get("certificateUrl");
		String certificatePassword = (String) session.get("certificatePassword");
		String keystoreType = (String) session.get("keystoreType");
		String keyManagerAlgorithm = (String) session.get("keyManagerAlgorithm");
		String truststore = (String) session.get("truststoreUrl");
		String truststorePassword = (String) session.get("truststorePassword");
		String truststoreType = (String) session.get("truststoreType");
		String trustManagerAlgorithm = (String) session.get("trustManagerAlgorithm");
		boolean isAllowSelfSignedCertificates = Boolean.parseBoolean((String) session.get("isAllowSelfSignedCertificates"));
		boolean isVerifyHostname = Boolean.parseBoolean((String) session.get("isVerifyHostname"));
		boolean isIgnoreCertificateExpiredException = Boolean.parseBoolean((String) session.get("isIgnoreCertificateExpiredException"));

		if (!StringUtils.isEmpty(certificate)) {
			certificateUrl = ClassUtils.getResourceURL(classLoader, certificate);
			if (certificateUrl==null) {
				throw new CmisRuntimeException("cannot find URL for certificate resource ["+certificate+"]");
			}
			log.info("resolved certificate-URL to ["+certificateUrl.toString()+"]");
		}
		if (!StringUtils.isEmpty(truststore)) {
			truststoreUrl = ClassUtils.getResourceURL(classLoader, truststore);
			if (truststoreUrl==null) {
				throw new CmisRuntimeException("cannot find URL for truststore resource ["+truststore+"]");
			}
			log.info("resolved truststore-URL to ["+truststoreUrl.toString()+"]");
		}

		if (certificateUrl!=null || truststoreUrl!=null || isAllowSelfSignedCertificates) {
			try {
				IbisSSLSocketFactory socketfactory = new IbisSSLSocketFactory(
						certificateUrl, certificatePassword, keystoreType, keyManagerAlgorithm,
						truststoreUrl, truststorePassword, truststoreType, trustManagerAlgorithm,
						isAllowSelfSignedCertificates, isVerifyHostname, isIgnoreCertificateExpiredException);

				socketfactory.initSSLContext();
				return socketfactory;
			} catch (Exception e) {
				throw new CmisRuntimeException("Failed to create or initialize IbisSSLSocketFactory, using default socketFactory instead!");
			}
		}

		return null;
	}
/*
	public HostnameVerifier getHostnameVerifier() {
		return new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
	}*/
}