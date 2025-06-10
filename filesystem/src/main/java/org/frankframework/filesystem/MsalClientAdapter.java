/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.HttpRequest;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IHttpClient;
import com.microsoft.aad.msal4j.IHttpResponse;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Protected;
import org.frankframework.filesystem.exchange.ExchangeFileSystem;
import org.frankframework.filesystem.exchange.MailFolder;
import org.frankframework.filesystem.exchange.MailFolderResponse;
import org.frankframework.filesystem.exchange.MailMessage;
import org.frankframework.filesystem.exchange.MailMessageResponse;
import org.frankframework.http.AbstractHttpSender;
import org.frankframework.http.HttpMessageEntity;
import org.frankframework.http.HttpResponseHandler;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.JacksonUtils;
import org.frankframework.util.LogUtil;

/**
 * This class ensures that Microsoft Authentication Library (MSAL) requests are sent through the configured proxy and the correct SSLSocketFactory.
 * @see ExchangeFileSystem
 *
 */
@Protected
public class MsalClientAdapter extends AbstractHttpSender implements IHttpClient {
	private static final String METHOD_SESSION_KEY = "HTTP_METHOD";
	private static final String REQUEST_HEADERS_SESSION_KEY = "HTTP_REQUEST_HEADERS";
	private static final String RESPONSE_HEADERS_SESSION_KEY = "HTTP_RESPONSE_HEADERS";
	private static final String URL_SESSION_KEY = "URL";
	private static final String STATUS_CODE_SESSION_KEY = "HTTP_STATUSCODE";

	private static final String SCOPE = "https://graph.microsoft.com/.default";
	private static final String AUTHORITY = "https://login.microsoftonline.com/";

	private ConfidentialClientApplication client;
	private ExecutorService executor;
	private ClientCredentialParameters clientCredentialParam;

	private boolean configured = false;

	public MsalClientAdapter() {
		setName("MSAL Autentication Sender");

		Parameter urlParameter = new Parameter();
		urlParameter.setName("url");
		urlParameter.setSessionKey(URL_SESSION_KEY);
		addParameter(urlParameter);

		setResultStatusCodeSessionKey(STATUS_CODE_SESSION_KEY);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		configured = true;
	}

	@Override
	public void start() {
		if (!configured) {
			throw new LifecycleException("not yet configured");
		}
		super.start();
	}

	@Override
	public void stop() {
		if (executor != null) {
			executor.shutdown();
		}
		if (client != null) {
			client = null;
		}
		super.stop();
	}

	public GraphClient createGraphClient(String tenantId, CredentialFactory credentials) throws IOException {
		if (getHttpClient() == null) {
			throw new LifecycleException("not yet started");
		}

		executor = Executors.newSingleThreadExecutor(); // Create a new Executor in the same thread(context) to avoid SecurityExceptions when setting a ClassLoader on the Runnable.
		clientCredentialParam = ClientCredentialParameters.builder(Collections.singleton(SCOPE)).tenant(tenantId).build();

		client = ConfidentialClientApplication.builder(
				credentials.getUsername(),
				ClientCredentialFactory.createFromSecret(credentials.getPassword()))
			.authority(AUTHORITY + tenantId)
			.httpClient(this)
			.executorService(executor)
			.build();

		return new GraphClient(this);
	}

	protected String getAuthenticationToken() throws IOException {
		CompletableFuture<IAuthenticationResult> future = client.acquireToken(clientCredentialParam);
		try {
			String token = future.get().accessToken();
			return "Bearer " + token;
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			throw new IOException("could not generate access token", e);
		}
	}

	/** Silly wrapper to create a clean SDK */
	public static class GraphClient implements AutoCloseable {
		private final MsalClientAdapter msal;
		public GraphClient(MsalClientAdapter msal) {
			this.msal = msal;
		}

		public void execute(HttpRequestBase httpRequestBase) throws IOException {
			execute(httpRequestBase, null);
		}

		@SuppressWarnings("unchecked")
		public <T> T execute(HttpRequestBase httpRequestBase, Class<T> dto) throws IOException {
			httpRequestBase.addHeader("Authorization", msal.getAuthenticationToken());
			HttpResponse response;
			try {
				response = msal.execute(httpRequestBase.getURI(), httpRequestBase, null);
			} catch (TimeoutException e) {
				throw new IOException(e);
			}
			HttpStatus status = HttpStatus.valueOf(response.getStatusLine().getStatusCode());

			if (status.is2xxSuccessful()) {
				HttpEntity entity = response.getEntity();
				if (entity != null && dto != null) {
					try (InputStream contentStream = entity.getContent()) {
						if(String.class.isAssignableFrom(dto)) {
							return (T) new String(contentStream.readAllBytes());
						}
						return JacksonUtils.convertToDTO(contentStream, dto);
					} finally {
						EntityUtils.consume(entity);
					}
				}
				return null;
			}

			throw new IOException(status.getReasonPhrase());
		}

		public List<MailFolder> getMailFolders(String email) throws IOException {
			return MailFolderResponse.get(this, email, 200);
		}

		public List<MailFolder> getMailFolders(MailFolder folder) throws IOException {
			return MailFolderResponse.get(this, folder);
		}

		public List<MailMessage> getMailMessages(MailFolder folder) throws IOException {
			return getMailMessages(folder, 20);
		}

		public List<MailMessage> getMailMessages(MailFolder folder, int limit) throws IOException {
			return MailMessageResponse.get(this, folder, limit);
		}

		public void createMailFolder(MailFolder mailFolder, String folderName) throws IOException {
			MailFolderResponse.create(this, mailFolder, folderName);
		}

		public void deleteMailFolder(MailFolder folderToDelete) throws IOException {
			MailFolderResponse.delete(this, folderToDelete);
		}

		public void deleteMailMessage(MailMessage file) throws IOException {
			MailMessageResponse.delete(this, file);
		}

		public MailMessage getMailMessage(MailMessage file) throws IOException {
			return MailMessageResponse.get(this, file);
		}

		public MailMessage moveMailMessage(MailMessage file, MailFolder destinationFolder) throws IOException {
			return MailMessageResponse.move(this, file, destinationFolder);
		}

		public MailMessage copyMailMessage(MailMessage file, MailFolder destinationFolder) throws IOException {
			return MailMessageResponse.copy(this, file, destinationFolder);
		}

		@Override
		public void close() throws IOException {
			msal.stop();
		}
	}


	@Override
	public IHttpResponse send(HttpRequest httpRequest) throws Exception {
		try (PipeLineSession session = prepareSession(httpRequest); Message request = new Message(httpRequest.body())) {

			Message response = sendMessageOrThrow(request, session);
			session.scheduleCloseOnSessionExit(response);
			return new MsalResponse(response, session);
		} catch (Exception e) {
			log.error("An exception occurred whilst connecting with MSAL HTTPS call to [{}]", httpRequest.url().toString(), e);
			throw e;
		}
	}

	private PipeLineSession prepareSession(HttpRequest httpRequest) {
		PipeLineSession session = new PipeLineSession();
		session.put(URL_SESSION_KEY, httpRequest.url().toString());
		log.debug("Put request URL [{}] in session under key [{}]", httpRequest::url, ()->URL_SESSION_KEY);

		session.put(METHOD_SESSION_KEY, httpRequest.httpMethod().name());
		log.debug("Put http method [{}] in session under key [{}]", httpRequest::httpMethod, ()->METHOD_SESSION_KEY);

		session.put(REQUEST_HEADERS_SESSION_KEY, httpRequest.headers());
		if(log.isDebugEnabled()) log.debug("Put http headers [{}] in session under key [{}]", httpRequest::headers, ()->REQUEST_HEADERS_SESSION_KEY);

		return session;
	}

	@Override
	protected HttpRequestBase getMethod(URI uri, Message message, @Nonnull ParameterValueList parameters, PipeLineSession session) throws SenderException {
		HttpMethod httpMethod = EnumUtils.parse(HttpMethod.class, (String) session.get(METHOD_SESSION_KEY));
		@SuppressWarnings("unchecked")
		Map<String, String> headers = (Map<String, String>) session.get(REQUEST_HEADERS_SESSION_KEY);

		if(uri == null) {
			throw new SenderException("No URI to connect to!");
		}

		switch (httpMethod) {
		case GET:
			HttpGet getMethod = new HttpGet(uri.toString());

			return appendHeaders(headers, getMethod);
		case POST:
			HttpEntityEnclosingRequestBase method = new HttpPost(uri.toString());
			HttpEntity entity = new HttpMessageEntity(message); // No need to set the content-type, MSAL sets it to application/soap+xml later on

			method.setEntity(entity);
			return appendHeaders(headers, method);
		default:
				throw new NotImplementedException("method ["+httpMethod+"] has not been implemented");
		}
	}

	@Override
	protected Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) {
		// Parse headers to comma separated string
		Header[] responseHeaders = responseHandler.getAllHeaders();
		StringBuilder headers = new StringBuilder();
		for(Header header : responseHeaders) {
			session.put(header.getName(), header.getValue());
			if(!headers.isEmpty()) {
				headers.append(",");
			}
			headers.append(header.getName());
		}
		session.put(RESPONSE_HEADERS_SESSION_KEY, headers.toString());

		return responseHandler.getResponseMessage();
	}

	private HttpRequestBase appendHeaders(Map<String, String> headers, HttpRequestBase request) {
		for(Map.Entry<String, String> header : headers.entrySet()) {
			String name = header.getKey();
			String value = header.getValue();
			request.setHeader(name, value);
			log.debug("Appending header [{}] [{}]", name, value);
		}
		return request;
	}

	private class MsalResponse implements IHttpResponse {
		protected Logger log = LogUtil.getLogger(this);

		private final int statusCode;
		private final Map<String, List<String>> headers = new HashMap<>();
		private String body = "";

		public MsalResponse(Message response, PipeLineSession session) {
			this.statusCode = Integer.parseInt((String) session.get(STATUS_CODE_SESSION_KEY));
			if(log.isDebugEnabled())
				log.debug("Parsing status code [{}]", statusCode);

			String[] headersAsCsv = ((String) session.get(RESPONSE_HEADERS_SESSION_KEY)).split(",");
			for (String headerName : headersAsCsv) {
				List<String> values = new ArrayList<>();
				String headerValue = (String) session.get(headerName);
				values.add(headerValue);

				if(log.isDebugEnabled())
					log.debug("Parsing header [{}] [{}]", headerName, headerValue);
				this.headers.put(headerName, values);
			}

			try {
				log.debug("Parsing body [{}]", response::toString);
				this.body = response.asString();
			} catch (IOException e) {
				log.error("An exception occurred whilst parsing the response body of MSAL authentication call.", e);
			}
		}

		@Override
		public int statusCode() {
			return statusCode;
		}

		@Override
		public Map<String, List<String>> headers() {
			return headers;
		}

		@Override
		public String body() {
			return body;
		}
	}
}
