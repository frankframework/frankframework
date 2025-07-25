/*
   Copyright 2018 Nationale-Nederlanden, 2021 - 2024 WeAreFrank!

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.bindings.impl.ClientVersion;
import org.apache.chemistry.opencmis.client.bindings.impl.CmisBindingsHelper;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.http.HttpInvoker;
import org.apache.chemistry.opencmis.client.bindings.spi.http.Output;
import org.apache.chemistry.opencmis.client.bindings.spi.http.Response;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.impl.UrlBuilder;
import org.apache.chemistry.opencmis.commons.spi.AuthenticationProvider;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.http.AbstractHttpSender.HttpMethod;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.Parameter;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.StreamUtil;

@Log4j2
public class CmisHttpInvoker implements HttpInvoker, AutoCloseable {

	private static final int HEADER_PARAM_PREFIX_LENGTH = CmisSender.HEADER_PARAM_PREFIX.length();

	private CmisHttpSender sender = null;

	//To stub during testing
	protected CmisHttpSender createSender() {
		CmisHttpSender cmisHttpSender = new CmisHttpSender() {};
		log.debug("CmisHttpInvoker [{}] created new CmisHttpSender [{}]", this, cmisHttpSender);
		return cmisHttpSender;
	}

	@Override
	public void close() {
		if (sender != null) {
			log.debug("Closing CmisHttpSender [{}] from CmisHttpInvoker [{}]", sender, this);
			sender.stop();
			sender = null;
		} else {
			log.debug("Closing CmisHttpInvoker [{}] but does not have a sender to close", this);
		}
	}

	private CmisHttpSender getInstance(BindingSession session) throws LifecycleException, ConfigurationException {
		if(sender == null) {
			log.debug("creating new CmisHttpInvoker");
			sender = createSender();

			sender.setUrlParam("url");

			//Auth
			if(session.get(SessionParameter.USER) != null)
				sender.setUsername((String) session.get(SessionParameter.USER));
			if(session.get(SessionParameter.PASSWORD) != null)
				sender.setPassword((String) session.get(SessionParameter.PASSWORD));

			//Proxy
			if(session.get("proxyHost") != null) {
				sender.setProxyHost((String) session.get("proxyHost"));
				if(session.get("proxyPort") != null)
					sender.setProxyPort(Integer.parseInt((String) session.get("proxyPort")));
				if(session.get("proxyUsername") != null)
					sender.setProxyUsername((String) session.get("proxyUsername"));
				if(session.get("proxyPassword") != null)
					sender.setProxyPassword((String) session.get("proxyPassword"));
			}

			//SSL
			if(session.get("keystoreUrl") != null)
				sender.setKeystore((String) session.get("keystoreUrl"));
			if(session.get("keystorePassword") != null)
				sender.setKeystorePassword((String) session.get("keystorePassword"));
			if(session.get("keystoreAlias") != null)
				sender.setKeystoreAlias((String) session.get("keystoreAlias"));
			if(session.get("keystoreAliasPassword") != null)
				sender.setKeystoreAliasPassword((String) session.get("keystoreAliasPassword"));
			if(session.get("keystoreType") != null)
				sender.setKeystoreType(EnumUtils.parse(KeystoreType.class, (String)session.get("keystoreType")));
			if(session.get("keyManagerAlgorithm") != null)
				sender.setKeyManagerAlgorithm((String) session.get("keyManagerAlgorithm"));
			if(session.get("truststoreUrl") != null)
				sender.setTruststore((String) session.get("truststoreUrl"));
			if(session.get("truststorePassword") != null)
				sender.setTruststorePassword((String) session.get("truststorePassword"));
			if(session.get("truststoreType") != null)
				sender.setTruststoreType(EnumUtils.parse(KeystoreType.class, (String)session.get("truststoreType")));
			if(session.get("trustManagerAlgorithm") != null)
				sender.setTrustManagerAlgorithm((String) session.get("trustManagerAlgorithm"));

			//SSL+
			if(session.get("isAllowSelfSignedCertificates") != null) {
				boolean isAllowSelfSignedCertificates = Boolean.parseBoolean((String) session.get("isAllowSelfSignedCertificates"));
				sender.setAllowSelfSignedCertificates(isAllowSelfSignedCertificates);
			}
			if(session.get("isVerifyHostname") != null) {
				boolean isVerifyHostname = Boolean.parseBoolean((String) session.get("isVerifyHostname"));
				sender.setVerifyHostname(isVerifyHostname);
			}
			if(session.get("isIgnoreCertificateExpiredException") != null) {
				boolean isIgnoreCertificateExpiredException = Boolean.parseBoolean((String) session.get("isIgnoreCertificateExpiredException"));
				sender.setIgnoreCertificateExpiredException(isIgnoreCertificateExpiredException);
			}

			//Add parameters
			Parameter parameter = new Parameter();
			parameter.setName("writer");
			parameter.setSessionKey("writer");
			sender.addParameter(parameter);
			Parameter urlParam = new Parameter();
			urlParam.setName("url");
			urlParam.setSessionKey("url");
			sender.addParameter(urlParam);

			// timeouts
			int timeout = session.get(SessionParameter.READ_TIMEOUT, -1);
			if (timeout >= 0) {
				sender.setTimeout(timeout);
			}

			int maxConnections = session.get("maxConnections", 0);
			if(maxConnections > 0) {
				sender.setMaxConnections(maxConnections);
			}

			sender.configure();
			sender.start();
		}
		return sender;
	}

	@Override
	public Response invokeGET(UrlBuilder url, BindingSession session) {
		return invoke(url, HttpMethod.GET, null, null, null, session, null, null);
	}

	@Override
	public Response invokeGET(UrlBuilder url, BindingSession session, BigInteger offset, BigInteger length) {
		return invoke(url, HttpMethod.GET, null, null, null, session, offset, length);
	}

	@Override
	public Response invokePOST(UrlBuilder url, String contentType, Output writer, BindingSession session) {
		return invoke(url, HttpMethod.POST, contentType, null, writer, session, null, null);
	}

	@Override
	public Response invokePUT(UrlBuilder url, String contentType, Map<String, String> headers, Output writer,
			BindingSession session) {
		return invoke(url, HttpMethod.PUT, contentType, headers, writer, session, null, null);
	}

	@Override
	public Response invokeDELETE(UrlBuilder url, BindingSession session) {
		return invoke(url, HttpMethod.DELETE, null, null, null, session, null, null);
	}

	private Response invoke(UrlBuilder url, HttpMethod method, String contentType, Map<String, String> headers,
			Output writer, BindingSession session, BigInteger offset, BigInteger length) {

		log.debug("Session {}: {} {}", session.getSessionId(), method, url);

		if(url.toString().equals(CmisSessionBuilder.OVERRIDE_WSDL_URL)) {
			try {
				Map<String, List<String>> headerFields = new HashMap<>();
				String wsdl = (String) session.get(CmisSessionBuilder.OVERRIDE_WSDL_KEY);
				InputStream inputStream = new ByteArrayInputStream(wsdl.getBytes(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING));
				return new Response(200, "ok", headerFields, inputStream, null);
			} catch (UnsupportedEncodingException e) {
				// This should never happen, but in case it does...
				throw new CmisConnectionException("unable to open or read WSDL", e);
			}
		}

		Response response;

		try {
			sender = getInstance(session);
			if(sender == null)
				throw new CmisConnectionException("Failed to create IbisHttpSender");

			// init headers if not exist
			if(headers == null)
				headers = new HashMap<>();

			if (contentType != null)
				headers.put("Content-Type", contentType);

			headers.put("User-Agent", (String) session.get(SessionParameter.USER_AGENT, ClientVersion.OPENCMIS_USER_AGENT));

			// offset
			if (offset != null || length != null) {
				StringBuilder sb = new StringBuilder("bytes=");

				if ((offset == null) || (offset.signum() == -1)) {
					offset = BigInteger.ZERO;
				}

				sb.append(offset);
				sb.append('-');

				if (length != null && length.signum() == 1) {
					sb.append(offset.add(length.subtract(BigInteger.ONE)));
				}

				headers.put("Range", sb.toString());
			}

			// compression
			Object compression = session.get(SessionParameter.COMPRESSION);
			if (compression != null && Boolean.parseBoolean(compression.toString())) {
				headers.put("Accept-Encoding", "gzip,deflate");
			}

			// locale
			if (session.get(CmisBindingsHelper.ACCEPT_LANGUAGE) instanceof String) {
				headers.put("Accept-Language", session.get(CmisBindingsHelper.ACCEPT_LANGUAGE).toString());
			}

			AuthenticationProvider authProvider = CmisBindingsHelper.getAuthenticationProvider(session);
			if (authProvider != null) {
				Map<String, List<String>> httpHeaders = authProvider.getHTTPHeaders(url.toString());
				if (httpHeaders != null) {
					for (Map.Entry<String, List<String>> header : httpHeaders.entrySet()) {
						if (header.getKey() != null && !header.getValue().isEmpty()) {
							String key = header.getKey();
							if ("user-agent".equalsIgnoreCase(key)) {
								headers.put("User-Agent", header.getValue().get(0));
							}
							else {
								for (String value : header.getValue()) {
									if (value != null) {
										headers.put(key, value);
									}
								}
							}
						}
					}
				}
			}

			for(String key : session.getKeys()) {
				if(key.startsWith(CmisSender.HEADER_PARAM_PREFIX)) {
					String name = StringUtils.substring(key, HEADER_PARAM_PREFIX_LENGTH);
					Object value = session.get(key);
					if(value != null) {
						headers.put(name, String.valueOf(value)); //Session values are always stored as String
					}
				}
			}

			log.trace("invoking CmisHttpSender: content-type[{}] headers[{}]", contentType, headers);

			response = sender.invoke(method, url.toString(), headers, writer);
		} catch (Exception e) {
			log.trace("exception creating or retrieving sender instance", e);
			throw new CmisConnectionException(url.toString(), -1, e);
		}

		log.trace("received result code[{}] headers[{}]", response::getResponseCode, response::getHeaders);
		return response;
	}
}
