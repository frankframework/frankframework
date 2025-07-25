/*
   Copyright 2018-2020 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import jakarta.annotation.Nonnull;

import org.apache.chemistry.opencmis.client.bindings.spi.http.Output;
import org.apache.chemistry.opencmis.client.bindings.spi.http.Response;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.http.HttpEntity;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ByteArrayEntity;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.http.AbstractHttpSender;
import org.frankframework.http.HttpResponseHandler;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

/**
 * Abstract class to prevent Frank!Developers from including/using this Sender in their configurations.
 * It should solely be used by the @{link CmisHttpInvoker}.
 */
public abstract class CmisHttpSender extends AbstractHttpSender {

	@Override
	public HttpRequestBase getMethod(URI uri, Message message, @Nonnull ParameterValueList pvl, PipeLineSession session) throws SenderException {
		HttpRequestBase method = null;

		HttpMethod methodType = (HttpMethod) session.get("method");
		if(methodType == null) {
			throw new SenderException("unable to determine method from pipeline session");
		}

		try {
			switch (methodType) {
			case GET:
				method = new HttpGet(uri);
				break;

			case POST:
				HttpPost httpPost = new HttpPost(uri);

				// send data
				if (pvl.get("writer") != null) {
					Output writer = (Output) pvl.get("writer").getValue();
					ByteArrayOutputStream out = new ByteArrayOutputStream();

					Object clientCompression = pvl.get(SessionParameter.CLIENT_COMPRESSION);
					if ((clientCompression != null) && Boolean.parseBoolean(clientCompression.toString())) {
						httpPost.setHeader("Content-Encoding", "gzip");
						writer.write(new GZIPOutputStream(out, 4096));
					} else {
						writer.write(out);
					}

					HttpEntity entity = new BufferedHttpEntity( new ByteArrayEntity(out.toByteArray()) );
					httpPost.setEntity(entity);
					out.close();

					method = httpPost;
				}
				break;

			case PUT:
				HttpPut httpPut = new HttpPut(uri);

				// send data
				if (pvl.get("writer") != null) {
					Output writer = (Output) pvl.get("writer").getValue();
					ByteArrayOutputStream out = new ByteArrayOutputStream();

					Object clientCompression = pvl.get(SessionParameter.CLIENT_COMPRESSION);
					if ((clientCompression != null) && Boolean.parseBoolean(clientCompression.toString())) {
						httpPut.setHeader("Content-Encoding", "gzip");
						writer.write(new GZIPOutputStream(out, 4096));
					} else {
						writer.write(out);
					}

					HttpEntity entity = new BufferedHttpEntity( new ByteArrayEntity(out.toByteArray()) );
					httpPut.setEntity(entity);
					out.close();

					method = httpPut;
				}
				break;
			case DELETE:
				method = new HttpDelete(uri);
				break;

			default:
				throw new MethodNotSupportedException("method ["+methodType+"] not implemented");
			}
		}
		catch (Exception e) {
			throw new SenderException(e);
		}

		if (session.get("headers") != null) {
			@SuppressWarnings("unchecked")
			Map<String, String> headers = (Map<String, String>) session.get("headers");

			for(Map.Entry<String, String> entry : headers.entrySet()) {
				if(log.isTraceEnabled()) log.trace("appending header [{}] with value [{}]", entry.getKey(), entry.getValue());

				method.addHeader(entry.getKey(), entry.getValue());
			}
		}

		log.debug("HttpSender constructed {}-method [{}] query [{}] ", methodType, method.getURI(), method.getURI().getQuery());
		return method;
	}

	@Override
	protected boolean validateResponseCode(int statusCode) {
		return true; //Always success
	}

	@Override
	public Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws IOException {
		int responseCode = -1;
		try {
			StatusLine statusline = responseHandler.getStatusLine();
			responseCode = statusline.getStatusCode();

			Message responseMessage = responseHandler.getResponseMessage();
			InputStream responseStream = null;
			InputStream errorStream = null;
			Map<String, List<String>> headerFields = responseHandler.getHeaderFields();
			if (responseCode == 200 || responseCode == 201 || responseCode == 203 || responseCode == 206) {
				responseStream = responseMessage.asInputStream();
			}
			else {
				errorStream = responseMessage.asInputStream();
			}
			Response response = new Response(responseCode, statusline.toString(), headerFields, responseStream, errorStream);
			session.put("__response", response);
		}
		catch(Exception e) {
			throw new CmisConnectionException(getUrl(), responseCode, e);
		}

		return Message.nullMessage();
	}

	public Response invoke(HttpMethod method, String url, Map<String, String> headers, Output writer) {
		//Prepare the message. We will overwrite things later...
		int responseCode = -1;

		try(PipeLineSession pls = new PipeLineSession()) {
			pls.put("writer", writer);
			pls.put("url", url);
			pls.put("method", method);
			pls.put("headers", headers);
	
			try {
				// Message is unused, we use 'Output writer' instead
				SenderResult ignored = sendMessage(Message.nullMessage(), pls);
				ignored.getResult().close(); // close our nullMessage
				return (Response) pls.get("__response");
			} catch (Exception e) {
				throw new CmisConnectionException(getUrl(), responseCode, e);
			}
		}
	}

}
