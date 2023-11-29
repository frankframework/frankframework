/*
   Copyright 2017-2022 WeAreFrank!

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.util.StreamUtil;

public class HttpResponseHandler {
	private final HttpResponse httpResponse;
	private HttpEntity httpEntity;
	private Message responseMessage = null;

	public HttpResponseHandler(HttpResponse resp) throws IOException {
		httpResponse = resp;
		if(httpResponse.getEntity() != null) {
			httpEntity = httpResponse.getEntity();

			MessageContext context = new MessageContext();
			Header[] headers = resp.getAllHeaders();
			if (headers!=null) {
				for(Header header:headers) {
					context.put(header.getName(),header.getValue());
				}
			}
			context.withCharset(getCharset());
			InputStream entityStream = new ReleaseConnectionAfterReadInputStream(this, httpEntity.getContent()); //Wrap the contentStream in a ReleaseConnectionAfterReadInputStream
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

	public ContentType getContentType() {
		Header contentTypeHeader = this.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		ContentType contentType;
		if (contentTypeHeader != null) {
			contentType = ContentType.parse(contentTypeHeader.getValue());
		} else {
			contentType = ContentType.getOrDefault(httpEntity);
		}
		return contentType;
	}

	public String getCharset() {
		ContentType contentType = getContentType();
		String charSet = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		if(contentType != null && contentType.getCharset() != null)
			charSet = contentType.getCharset().displayName();
		return charSet;
	}

	/**
	 * Consumes the {@link HttpEntity} and will release the connection.
	 */
	public void close() throws IOException {
		if(httpEntity != null) {
			EntityUtils.consume(httpEntity);
		}
	}

	public Header getFirstHeader(String string) {
		return httpResponse.getFirstHeader(string);
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

	public boolean isMultipart() {
		return getContentType().getMimeType().startsWith("multipart/");
	}
}
