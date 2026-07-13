/*
   Copyright 2018 Nationale-Nederlanden, 2026 WeAreFrank!

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import lombok.Getter;

import org.frankframework.http.mime.MultipartEntity;

public class HttpResponseMock extends Mockito implements Answer<CloseableHttpResponse> {

	private final String lineSeparator = System.lineSeparator();
	private final int statusCode;

	public HttpResponseMock() {
		this(200);
	}
	public HttpResponseMock(int statusCode) {
		this.statusCode = statusCode;
	}

	private CloseableHttpResponse buildResponse(InputStream content) throws UnsupportedOperationException, IOException {
		CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		HttpEntity httpEntity = mock(HttpEntity.class);

		when(statusLine.getStatusCode()).thenReturn(statusCode);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);

		when(httpEntity.getContent()).thenReturn(content);
		when(httpEntity.getContentLength()).thenReturn(-1L);
		when(httpResponse.getEntity()).thenReturn(httpEntity);
		when(httpResponse.getAllHeaders()).thenReturn(generateResponseHeaders());
		return httpResponse;
	}

	private Header[] generateResponseHeaders() {
		Header[] headers = new Header[2];
		headers[0] = new HeaderImpl("Connection", "Keep-Alive");
		headers[1] = new HeaderImpl("Content-Type", "text/plain");
		return headers;
	}

	private static class HeaderImpl implements Header {
		private final @Getter String name;
		private final @Getter String value;
		public HeaderImpl(String name, String value) {
			this.name = name;
			this.value = value;
		}
		@Override
		public HeaderElement[] getElements() throws ParseException {
			return null;
		}
	}

	@Override
	public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
		HttpHost host = (HttpHost) invocation.getArguments()[0];
		HttpRequestBase request = (HttpRequestBase) invocation.getArguments()[1];
		HttpContext context = (HttpContext) invocation.getArguments()[2];

		InputStream response = switch (request) {
			case HttpGet get -> doGet(host, get, context);
			case HttpPost post -> doPost(host, post, context);
			case HttpPut put -> doPut(host, put, context);
			case HttpPatch patch -> doPatch(host, patch, context);
			case HttpDelete delete -> doDelete(host, delete, context);
			case HttpHead head -> doHead(host, head, context);
			case null, default -> throw new Exception("mock method not implemented");
		};

		return buildResponse(response);
	}

	private InputStream doGet(HttpHost host, HttpGet request, HttpContext context) {
		assertEquals("GET", request.getMethod());
		StringBuilder response = new StringBuilder();
		response.append("HOST ").append(host.toHostString()).append(lineSeparator);
		response.append(request).append(lineSeparator);

		appendHeaders(request, response);

		return new ByteArrayInputStream(response.toString().getBytes());
	}

	private InputStream doHead(HttpHost host, HttpHead request, HttpContext context) {
		assertEquals("HEAD", request.getMethod());
		assertEquals("HEAD / HTTP/1.1", request.toString());
		return null; // HEAD requests do not have a body
	}

	private boolean appendHeaders(HttpRequestBase request, StringBuilder response) {
		boolean foundContentType = false;
		Header[] headers = request.getAllHeaders();
		for (Header header : headers) {
			String headerName = header.getName();
			if ("content-type".equalsIgnoreCase(headerName)) foundContentType = true;

			String headerValue = header.getValue();
			if("X-Akamai-ACS-Auth-Data".equals(headerName)) { // Ignore timestamps in request header
				int start = StringUtils.ordinalIndexOf(headerValue, ",", 3);
				int end = headerValue.lastIndexOf(",");
				headerValue = headerValue.substring(0, start) + ", timestamp, timestamp" + headerValue.substring(end);
			}
			response.append(headerName + ": ").append(headerValue).append(lineSeparator);
		}
		return foundContentType;
	}

	private InputStream doPost(HttpHost host, HttpPost request, HttpContext context) throws IOException {
		assertEquals("POST", request.getMethod());
		StringBuilder response = new StringBuilder();
		response.append(request).append(lineSeparator);

		boolean foundContentType = appendHeaders(request, response);

		HttpEntity entity = request.getEntity();
		if(entity instanceof MultipartEntity multipartEntity) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			multipartEntity.writeTo(baos);
			String contentType = multipartEntity.getContentType().getValue();
			String boundary = getBoundary(contentType);
			contentType = contentType.replaceAll(boundary, "IGNORE");
			response.append("Content-Type: ").append(contentType).append(lineSeparator);

			response.append(lineSeparator);
			String content = baos.toString();
			content = content.replaceAll(boundary, "IGNORE");
			response.append(content);
		}
		else if(entity != null) {
			Header contentTypeHeader = request.getEntity().getContentType();
			if(contentTypeHeader != null && !foundContentType) {
				response.append(contentTypeHeader.getName()).append(": ").append(contentTypeHeader.getValue()).append(lineSeparator);
			}

			response.append(lineSeparator);
			String resultString = EntityUtils.toString(entity);
			int i = resultString.indexOf("%PDF-1.");
			if(i >= 0) {
				resultString = "%s%n...%d more characters".formatted(resultString.substring(0, i + 8), (resultString.length() - i));
			}
			response.append(resultString);
		}

		return new ByteArrayInputStream(response.toString().getBytes());
	}

	private InputStream doPut(HttpHost host, HttpPut request, HttpContext context) throws IOException {
		assertEquals("PUT", request.getMethod());
		StringBuilder response = new StringBuilder();
		response.append(request).append(lineSeparator);

		appendHeaders(request, response);

		if(request.getEntity() != null) { // If an entity is present
			Header contentTypeHeader = request.getEntity().getContentType();
			if(contentTypeHeader != null) {
				response.append(contentTypeHeader.getName()).append(": ").append(contentTypeHeader.getValue()).append(lineSeparator);
			}

			response.append(lineSeparator);
			response.append(EntityUtils.toString(request.getEntity()));
		}

		return new ByteArrayInputStream(response.toString().getBytes());
	}

	public InputStream doPatch(HttpHost host, HttpPatch request, HttpContext context) throws IOException {
		assertEquals("PATCH", request.getMethod());
		StringBuilder response = new StringBuilder();
		response.append(request.toString()).append(lineSeparator);

		appendHeaders(request, response);

		Header contentTypeHeader = request.getEntity().getContentType();
		if(contentTypeHeader != null) {
			response.append(contentTypeHeader.getName() + ": ").append(contentTypeHeader.getValue()).append(lineSeparator);
		}

		response.append(lineSeparator);
		response.append(EntityUtils.toString(request.getEntity()));
		return new ByteArrayInputStream(response.toString().getBytes());
	}

	private InputStream doDelete(HttpHost host, HttpDelete request, HttpContext context) {
		assertEquals("DELETE", request.getMethod());
		StringBuilder response = new StringBuilder();
		response.append(request.toString()).append(lineSeparator);

		appendHeaders(request, response);

		return new ByteArrayInputStream(response.toString().getBytes());
	}

	private String getBoundary(String contentType) {
		String boundary = contentType.substring(contentType.indexOf("boundary=")+9);
		boundary = boundary.substring(0, boundary.indexOf(";"));
		return boundary.replace("\"", "");
	}
}
