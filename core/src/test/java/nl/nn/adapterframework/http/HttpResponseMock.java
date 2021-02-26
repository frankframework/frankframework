/*
   Copyright 2018 Nationale-Nederlanden

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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import nl.nn.adapterframework.http.mime.MultipartEntity;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class HttpResponseMock extends Mockito implements Answer<HttpResponse> {
	private String lineSeparator = System.getProperty("line.separator");

	private HttpResponse buildResponse(InputStream content) throws UnsupportedOperationException, IOException {
		CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		HttpEntity httpEntity = mock(HttpEntity.class);

		when(statusLine.getStatusCode()).thenReturn(200);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);

		when(httpEntity.getContent()).thenReturn(content);
		when(httpResponse.getEntity()).thenReturn(httpEntity);
		return httpResponse;
	}

	@Override
	public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
		HttpHost host = (HttpHost) invocation.getArguments()[0];
		HttpRequestBase request = (HttpRequestBase) invocation.getArguments()[1];
		HttpContext context = (HttpContext) invocation.getArguments()[2];

		InputStream response = null;
		if(request instanceof HttpGet)
			response = doGet(host, (HttpGet) request, context);
		else if(request instanceof HttpPost)
			response = doPost(host, (HttpPost) request, context);
		else if(request instanceof HttpPut)
			response = doPut(host, (HttpPut) request, context);
		else
			throw new Exception("mock method not implemented");

		return buildResponse(response);
	}

	public InputStream doGet(HttpHost host, HttpGet request, HttpContext context) {
		assertEquals("GET", request.getMethod());
		StringBuilder response = new StringBuilder();
		response.append(request.toString() + lineSeparator);

		appendHeaders(request, response);

		return new ByteArrayInputStream(response.toString().getBytes());
	}

	private void appendHeaders(HttpRequestBase request, StringBuilder response) {
		Header[] headers = request.getAllHeaders();
		for (Header header : headers) {
			String headerName = header.getName();
			String headerValue = header.getValue();
			if(headerName.equals("X-Akamai-ACS-Auth-Data")) { //Ignore timestamps in request header
				int start = StringUtils.ordinalIndexOf(headerValue, ",", 3);
				int end = headerValue.lastIndexOf(",");
				headerValue = headerValue.substring(0, start) + ", timestamp, timestamp" + headerValue.substring(end);
			}
			response.append(headerName + ": " + headerValue + lineSeparator);
		}
	}

	public InputStream doPost(HttpHost host, HttpPost request, HttpContext context) throws IOException {
		assertEquals("POST", request.getMethod());
		StringBuilder response = new StringBuilder();
		response.append(request.toString() + lineSeparator);

		appendHeaders(request, response);

		HttpEntity entity = request.getEntity();
		if(entity instanceof MultipartEntity) {
			MultipartEntity multipartEntity = (MultipartEntity) entity;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			multipartEntity.writeTo(baos);
			String contentType = multipartEntity.getContentType().getValue();
			String boundary = getBoundary(contentType);
			contentType = contentType.replaceAll(boundary, "IGNORE");
			response.append("Content-Type: " + contentType + lineSeparator);

			response.append(lineSeparator);
			String content = new String(baos.toByteArray());
			content = content.replaceAll(boundary, "IGNORE");
			response.append(content);
		} else {
			Header contentTypeHeader = request.getEntity().getContentType();
			if(contentTypeHeader != null) {
				response.append(contentTypeHeader.getName() + ": " + contentTypeHeader.getValue() + lineSeparator);
			}

			response.append(lineSeparator);
			String resultString = EntityUtils.toString(entity);
			int i = resultString.indexOf("%PDF-1.");
			if(i >= 0) {
				resultString = String.format("%s\n...%d more characters", resultString.substring(0, i+8), (resultString.length()-i));
			}
			response.append(resultString);
		}

		return new ByteArrayInputStream(response.toString().getBytes());
	}

	public InputStream doPut(HttpHost host, HttpPut request, HttpContext context) throws IOException {
		assertEquals("PUT", request.getMethod());
		StringBuilder response = new StringBuilder();
		response.append(request.toString() + lineSeparator);

		appendHeaders(request, response);

		Header contentTypeHeader = request.getEntity().getContentType();
		if(contentTypeHeader != null) {
			response.append(contentTypeHeader.getName() + ": " + contentTypeHeader.getValue() + lineSeparator);
		}

		response.append(lineSeparator);
		response.append(EntityUtils.toString(request.getEntity()));
		return new ByteArrayInputStream(response.toString().getBytes());
	}

	private String getBoundary(String contentType) {
		String boundary = contentType.substring(contentType.indexOf("boundary=")+9);
		boundary = boundary.substring(0, boundary.indexOf(";"));
		return boundary.replaceAll("\"", "");
	}
}
