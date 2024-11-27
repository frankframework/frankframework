/*
   Copyright 2017-2024 WeAreFrank!

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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.springframework.util.MimeType;

import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.MessageUtils;

public class HttpResponseHandler {
	private final HttpResponse httpResponse;
	private HttpEntity httpEntity;
	private Message responseMessage = null;

	public HttpResponseHandler(HttpResponse resp) throws IOException {
		httpResponse = resp;
		if(httpResponse.getEntity() != null) {
			httpEntity = httpResponse.getEntity();

			MessageContext context = MessageUtils.getContext(httpResponse);
			InputStream entityStream = new ReleaseConnectionAfterReadInputStream(this, httpEntity.getContent()); // Wrap the contentStream in a ReleaseConnectionAfterReadInputStream
			responseMessage = new Message(entityStream, context);
		}
	}

	public StatusLine getStatusLine() {
		return httpResponse.getStatusLine();
	}

	public Header[] getAllHeaders() {
		return httpResponse.getAllHeaders();
	}

	/**
	 * Returns an {@link ReleaseConnectionAfterReadInputStream InputStream} that will automatically close the HttpRequest when fully read
	 * @return an {@link ReleaseConnectionAfterReadInputStream InputStream} retrieved from {@link HttpEntity#getContent()} or NULL when no {@link HttpEntity} is present
	 */
	public InputStream getResponse() throws IOException {
		if(responseMessage == null) {
			return null;
		}

		return responseMessage.asInputStream();// IOException cannot occur as the input and output are both InputStreams
	}

	public Message getResponseMessage() {
		return responseMessage;
	}

	public String getHeader(String header) {
		if(httpResponse.getFirstHeader(header) == null)
			return null;

		return httpResponse.getFirstHeader(header).getValue();
	}

	/**
	 * Consumes the {@link HttpEntity} and will release the connection.
	 */
	public void close() throws IOException {
		if(httpEntity != null) {
			EntityUtils.consume(httpEntity);
		}
	}

	public Map<String, List<String>> getHeaderFields() {
		Map<String, List<String>> headerMap = new HashMap<>();
		Header[] headers = httpResponse.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			Header header = headers[i];
			String name = header.getName().toLowerCase();
			List<String> value;
			if(headerMap.containsKey(name)) {
				value = headerMap.get(name);
			}
			else {
				value = new ArrayList<>();
			}
			value.add(header.getValue());
			headerMap.put(name, value);
		}
		return headerMap;
	}

	@Nullable
	public MimeType getMimeType() {
		if (responseMessage == null) {
			return null;
		}
		return responseMessage.getContext().getMimeType();
	}

	public boolean isMultipart() {
		MimeType mType = getMimeType();
		return mType != null && "multipart".equals(mType.getType());
	}
}
