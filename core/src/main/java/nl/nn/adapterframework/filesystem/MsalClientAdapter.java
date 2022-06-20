/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;

import com.microsoft.aad.msal4j.HttpRequest;
import com.microsoft.aad.msal4j.IHttpClient;
import com.microsoft.aad.msal4j.IHttpResponse;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.http.HttpResponseHandler;
import nl.nn.adapterframework.http.HttpSenderBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.StreamUtil;

public class MsalClientAdapter extends HttpSenderBase implements IHttpClient {
	private static final String METHOD_SESSION_KEY = "HTTP_METHOD";
	private static final String REQUEST_HEADERS_SESSION_KEY = "HTTP_REQUEST_HEADERS";
	private static final String RESPONSE_HEADERS_SESSION_KEY = "HTTP_RESPONSE_HEADERS";
	private static final String URL_SESSION_KEY = "URL";
	private static final String STATUS_CODE_SESSION_KEY = "HTTP_STATUSCODE";

	public MsalClientAdapter() {
		setName("MSAL");

		Parameter urlParameter = new Parameter();
		urlParameter.setName("url");
		urlParameter.setSessionKey(URL_SESSION_KEY);
		addParameter(urlParameter);

		setResultStatusCodeSessionKey(STATUS_CODE_SESSION_KEY);
	}

	@Override
	public IHttpResponse send(HttpRequest httpRequest) throws Exception {
		PipeLineSession session = prepareSession(httpRequest);
		Message request = new Message(httpRequest.body());

		try {
			Message response = sendMessage(request, session);
			return translateResponse(response, session);
		} catch (Exception e) {
			log.error("An exception occurred whilst connecting with MSAL HTTPS call to [" + httpRequest.url().toString() + "]", e);
			throw e;
		} finally {
			session.close();
		}
	}

	private PipeLineSession prepareSession(HttpRequest httpRequest) {
		PipeLineSession session = new PipeLineSession();
		session.put(URL_SESSION_KEY, httpRequest.url().toString());
		log.debug("Put request URL [{}] in session under key [{}]", ()->httpRequest.url().toString(), ()->URL_SESSION_KEY);

		session.put(METHOD_SESSION_KEY, httpRequest.httpMethod().name());
		log.debug("Put http method [{}] in session under key [{}]", ()->httpRequest.httpMethod().name(), ()->METHOD_SESSION_KEY);

		session.put(REQUEST_HEADERS_SESSION_KEY, httpRequest.headers());
		if(log.isDebugEnabled()) log.debug("Put http headers [{}] in session under key [{}]", ()->httpRequest.headers(), ()->REQUEST_HEADERS_SESSION_KEY);

		return session;
	}

	@Override
	protected HttpRequestBase getMethod(URI uri, Message message, ParameterValueList parameters, PipeLineSession session) throws SenderException {
		HttpMethod httpMethod = EnumUtils.parse(HttpMethod.class, (String) session.get(METHOD_SESSION_KEY));
		Map<String, String> headers = (Map<String, String>) session.get(REQUEST_HEADERS_SESSION_KEY);

		if(uri == null) {
			throw new SenderException("No URI to connect to!");
		}

		boolean queryParametersAppended = false;
		StringBuffer relativePath = new StringBuffer(uri.getRawPath());
		if(!StringUtils.isEmpty(uri.getQuery())) {
			relativePath.append("?" + uri.getQuery());
			queryParametersAppended = true;
		}
		try {
			switch (httpMethod) {
			case GET:
				if(parameters != null) {
					queryParametersAppended = appendParameters(queryParametersAppended, relativePath, parameters);
					if(log.isDebugEnabled())
						log.debug(getLogPrefix() + "path after appending of parameters [" + relativePath + "]");
				}
				HttpGet getMethod = new HttpGet(relativePath + (parameters == null ? message.asString() : ""));

				if(log.isDebugEnabled())
					log.debug(getLogPrefix() + "HttpSender constructed GET-method [" + getMethod.getURI().getQuery() + "]");
				if(null != getFullContentType()) { // Manually set Content-Type header
					getMethod.setHeader("Content-Type", getFullContentType().toString());
				}

				return appendHeaders(headers, getMethod);
			case POST:
				String messageString = message.asString();
				if(parameters != null) {
					StringBuffer msg = new StringBuffer(messageString);
					appendParameters(true, msg, parameters);
					if(StringUtils.isEmpty(messageString) && msg.length() > 1) {
						messageString = msg.substring(1);
					} else {
						messageString = msg.toString();
					}
				}
				HttpEntity entity = new ByteArrayEntity(messageString.getBytes(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING), getFullContentType());

				HttpEntityEnclosingRequestBase method = new HttpPost(relativePath.toString());

				method.setEntity(entity);
				return appendHeaders(headers, method);
			default:
					throw new NotImplementedException("method ["+httpMethod+"] has not been implemented");
			}
		} catch (Exception e) {
			throw new SenderException("An exception occurred whilst getting method", e);
		}
	}

	@Override
	protected Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws SenderException, IOException {
		// Parse headers to comma separated string
		Header[] responseHeaders = responseHandler.getAllHeaders();
		String headers = "";
		for(Header header : responseHeaders) {
			session.put(header.getName(), header.getValue());
			if(headers.length() > 0) {
				headers += ",";
			}
			headers += header.getName();
		}
		session.put(RESPONSE_HEADERS_SESSION_KEY, headers);

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

	private IHttpResponse translateResponse(Message response, PipeLineSession session) {
		return new IHttpResponse() {
			@Override
			public int statusCode() {
				int statusCode = Integer.parseInt((String) session.get(STATUS_CODE_SESSION_KEY));
				if(log.isDebugEnabled())
					log.debug("Parsing status code [" + statusCode + "]");

				return statusCode;
			}

			@Override
			public Map<String, List<String>> headers() {
				String[] headersAsCsv = ((String) session.get(RESPONSE_HEADERS_SESSION_KEY)).split(",");
				Map<String, List<String>> headers = new HashMap<>();

				for(String headerName : headersAsCsv) {
					List<String> values = new ArrayList<>();
					String headerValue = (String) session.get(headerName);
					values.add(headerValue);

					if(log.isDebugEnabled())
						log.debug("Parsing header [" + headerName + "] [" + headerValue + "]");
					headers.put(headerName, values);
				}

				return headers;
			}

			@Override
			public String body() {
				log.debug("Parsing body [{}]", response::toString);
				try {
					return response.asString();
				} catch (IOException e) {
					log.error("An exception occurred whilst parsing the response body of MSAL authentication call.", e);
				}
				return "";
			}
		};
	}
}
