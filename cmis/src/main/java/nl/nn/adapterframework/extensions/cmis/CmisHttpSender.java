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
package nl.nn.adapterframework.extensions.cmis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
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
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ByteArrayEntity;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.http.HttpResponseHandler;
import nl.nn.adapterframework.http.HttpSenderBase;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

public class CmisHttpSender extends HttpSenderBase {

	private Map<String, String> headers;

	public CmisHttpSender() {
	}

	@Override
	public boolean isSynchronous() {
		return false;
	}

	@Override
	public HttpRequestBase getMethod(URIBuilder uri, String message, ParameterValueList pvl, Map<String, String> headersParamsMap, IPipeLineSession session) throws SenderException {
		HttpRequestBase method = null;

		try {
			if(getMethodType().equals("GET")) {
				method = new HttpGet(uri.build());
			}
			else if (getMethodType().equals("POST")) {
				HttpPost httpPost = new HttpPost(uri.build());

				// send data
				if (pvl.getParameterValue("writer") != null) {
					Output writer = (Output) pvl.getParameterValue("writer").getValue();
					ByteArrayOutputStream out = new ByteArrayOutputStream();

					Object clientCompression = pvl.getParameterValue(SessionParameter.CLIENT_COMPRESSION);
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
			}
			else if (getMethodType().equals("PUT")) {
				HttpPut httpPut = new HttpPut(uri.build());

				// send data
				if (pvl.getParameterValue("writer") != null) {
					Output writer = (Output) pvl.getParameterValue("writer").getValue();
					ByteArrayOutputStream out = new ByteArrayOutputStream();

					Object clientCompression = pvl.getParameterValue(SessionParameter.CLIENT_COMPRESSION);
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
			}
			else if (getMethodType().equals("DELETE")) {
				method = new HttpDelete(uri.build());
			}
			else {
				throw new MethodNotSupportedException("method ["+getMethodType()+"] not implemented");
			}
		}
		catch (Exception e) {
			throw new SenderException(e);
		}

		for(Map.Entry<String, String> entry : headers.entrySet()) {
			log.debug("append header ["+ entry.getKey() +"] with value ["+  entry.getValue() +"]");

			method.addHeader(entry.getKey(), entry.getValue());
		}

		//Cmis creates it's own contentType depending on the method and bindingType
		method.setHeader("Content-Type", getContentType());

		log.debug(getLogPrefix()+"HttpSender constructed "+getMethodType()+"-method ["+method.getURI()+"] query ["+method.getURI().getQuery()+"] ");
		return method;
	}

	@Override
	public String extractResult(HttpResponseHandler responseHandler, ParameterResolutionContext prc) throws SenderException, IOException {
		int responseCode = -1;
		try {
			StatusLine statusline = responseHandler.getStatusLine();
			responseCode = statusline.getStatusCode();

			InputStream responseStream = null;
			InputStream errorStream = null;
			Map<String, List<String>> headerFields = responseHandler.getHeaderFields();
			if (responseCode == 200 || responseCode == 201 || responseCode == 203 || responseCode == 206) {
				responseStream = responseHandler.getResponse();
			}
			else {
				errorStream = responseHandler.getResponse();
			}
			Response response = new Response(responseCode, statusline.toString(), headerFields, responseStream, errorStream);
			prc.getSession().put("response", response);
		}
		catch(Exception e) {
			throw new CmisConnectionException(getUrl(), responseCode, e);
		}

		return "response";
	}

	public Response invoke(String url, Map<String, String> headers, Output writer, BindingSession session, BigInteger offset, BigInteger length) throws SenderException, TimeOutException {
		//Prepare the message. We will overwrite things later...
		this.headers = headers;
		int responseCode = -1;

		IPipeLineSession pls = new PipeLineSessionBase();
		pls.put("writer", writer);
		pls.put("url", url);

		ParameterResolutionContext prc = new ParameterResolutionContext("", pls);
		try {
			sendMessageWithTimeoutGuarded(null, null, prc);
			return (Response) pls.get("response");
		}
		catch(Exception e) {
			throw new CmisConnectionException(getUrl(), responseCode, e);
		}
	}

}
