/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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
import java.net.SocketTimeoutException;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.NTLMEngine;
import org.apache.http.impl.auth.NTLMEngineException;
import org.apache.http.impl.auth.NTLMScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Base64;
import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.Misc;
import org.frankframework.util.StreamUtil;

/**
 * Sender that sends a message via a WebService based on NTLM authentication.
 *
 * @author  Peter Leeuwenburgh
 */
@Deprecated(forRemoval = true, since = "8.0")
@ConfigurationWarning("NTLM authentication is unsecure and should be avoided.")
public class WebServiceNtlmSender extends AbstractSenderWithParameters implements HasPhysicalDestination {

	private final @Getter String domain = "Http";
	private String contentType = "text/xml; charset="+ StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private String url;
	private int timeout = 10000;
	private int maxConnections=10;
	private @Getter String authAlias;
	private @Getter String username;
	private @Getter String password;
	private String authDomain;
	private String proxyHost;
	private int proxyPort = 80;
	private String soapAction;

	private PoolingClientConnectionManager connectionManager;
	protected DefaultHttpClient httpClient;

	private final class JCIFSEngine implements NTLMEngine {
		private static final int TYPE_1_FLAGS = NtlmFlags.NTLMSSP_NEGOTIATE_56
				| NtlmFlags.NTLMSSP_NEGOTIATE_128
				| NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2
				| NtlmFlags.NTLMSSP_NEGOTIATE_ALWAYS_SIGN
				| NtlmFlags.NTLMSSP_REQUEST_TARGET;

		@Override
		public String generateType1Msg(final String domain, final String workstation) {
			final Type1Message type1Message = new Type1Message(TYPE_1_FLAGS, domain, workstation);
			return Base64.encode(type1Message.toByteArray());
		}

		@Override
		public String generateType3Msg(final String username, final String password, final String domain, final String workstation, final String challenge) throws NTLMEngineException {
			Type2Message type2Message;
			try {
				type2Message = new Type2Message(Base64.decode(challenge));
			} catch (final IOException exception) {
				throw new NTLMEngineException("Invalid NTLM type 2 message", exception);
			}
			final int type2Flags = type2Message.getFlags();
			final int type3Flags = type2Flags & (0xffffffff ^ (NtlmFlags.NTLMSSP_TARGET_TYPE_DOMAIN | NtlmFlags.NTLMSSP_TARGET_TYPE_SERVER));
			final Type3Message type3Message = new Type3Message(type2Message, password, domain, username, workstation, type3Flags);
			return Base64.encode(type3Message.toByteArray());
		}
	}

	private class NTLMSchemeFactory implements AuthSchemeFactory {
		@Override
		public AuthScheme newInstance(final HttpParams params) {
			return new NTLMScheme(new JCIFSEngine());
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, getTimeout());
		HttpConnectionParams.setSoTimeout(httpParameters, getTimeout());
		httpClient = new DefaultHttpClient(connectionManager, httpParameters);
		httpClient.getAuthSchemes().register("NTLM", new NTLMSchemeFactory());
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new NTCredentials(cf.getUsername(), cf.getPassword(), Misc.getHostname(), getAuthDomain()));
		if (StringUtils.isNotEmpty(getProxyHost())) {
			HttpHost proxy = new HttpHost(getProxyHost(), getProxyPort());
			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
	}

	@Override
	public void start() {
		connectionManager = new PoolingClientConnectionManager();
		connectionManager.setMaxTotal(getMaxConnections());
	}

	@Override
	public void stop() {
//		httpClient.getConnectionManager().shutdown();
		connectionManager.shutdown();
		connectionManager=null;
	}


	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		String result = null;
		HttpPost httpPost = new HttpPost(getUrl());
		try {
			StringEntity se = new StringEntity(message.asString());
			httpPost.setEntity(se);
			if (StringUtils.isNotEmpty(getContentType())) {
				log.debug("setting Content-Type header [{}]", getContentType());
				httpPost.addHeader("Content-Type", getContentType());
			}
			if (StringUtils.isNotEmpty(getSoapAction())) {
				log.debug("setting SOAPAction header [{}]", getSoapAction());
				httpPost.addHeader("SOAPAction", getSoapAction());
			}
			log.debug("executing method");
			HttpResponse httpresponse = httpClient.execute(httpPost);
			log.debug("executed method");
			StatusLine statusLine = httpresponse.getStatusLine();
			if (statusLine == null) {
				throw new SenderException("no statusline found");
			} else {
				int statusCode = statusLine.getStatusCode();
				String statusMessage = statusLine.getReasonPhrase();
				if (statusCode == HttpServletResponse.SC_OK) {
					log.debug("status code [{}] message [{}]", statusCode, statusMessage);
				} else {
					throw new SenderException("status code [" + statusCode + "] message [" + statusMessage + "]");
				}
			}
			HttpEntity httpEntity = httpresponse.getEntity();
			if (httpEntity == null) {
				log.warn("no response found");
			} else {
				log.debug("response content length [{}]", httpEntity.getContentLength());
				result = EntityUtils.toString(httpEntity);
				log.debug("retrieved result [{}]", result);
			}
		} catch (SocketTimeoutException | ConnectTimeoutException e) {
			throw new TimeoutException(e);
		} catch (Exception e) {
			throw new SenderException(e);
		} finally {
			httpPost.releaseConnection();
		}
		return new SenderResult(result);
	}

	@Override
	public String getPhysicalDestinationName() {
		return getUrl();
	}

	/**
	 * content-type of the request
	 * @ff.default text/html; charset=utf-8
	 */
	public void setContentType(String string) {
		contentType = string;
	}

	public String getContentType() {
		return contentType;
	}

	public String getUrl() {
		return url;
	}

	/** url or base of url to be used  */
	public void setUrl(String string) {
		url = string;
	}

	public int getTimeout() {
		return timeout;
	}

	/**
	 * timeout in ms of obtaining a connection/result. 0 means no timeout
	 * @ff.default 10000
	 */
	public void setTimeout(int i) {
		timeout = i;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	/**
	 * the maximum number of concurrent connections
	 * @ff.default 10
	 */
	public void setMaxConnections(int i) {
		maxConnections = i;
	}

	/** alias used to obtain credentials for authentication to host */
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	/** username used in authentication to host */
	public void setUsername(String string) {
		username = string;
	}

	/**
	 * password used to authenticate with the host
	 * @ff.default
	 */
	public void setPassword(String string) {
		password = string;
	}

	public String getAuthDomain() {
		return authDomain;
	}

	public void setAuthDomain(String string) {
		authDomain = string;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	/**
	 *
	 * @ff.default
	 */
	public void setProxyHost(String string) {
		proxyHost = string;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	/**
	 *
	 * @ff.default 80
	 */
	public void setProxyPort(int i) {
		proxyPort = i;
	}

	public String getSoapAction() {
		return soapAction;
	}

	/** the soapactionuri to be set in the requestheader */
	public void setSoapAction(String string) {
		soapAction = string;
	}

}
