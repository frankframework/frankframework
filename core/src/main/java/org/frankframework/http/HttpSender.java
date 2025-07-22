/*
   Copyright 2013, 2016-2020 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
import java.net.URI;
import java.net.URLEncoder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.w3c.dom.Element;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.http.mime.HttpEntityFactory;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * {@inheritClassDoc}
 *
 * @author Niels Meijer
 * @since 7.0
 * @version 2.0
 */
public class HttpSender extends AbstractHttpSender {

	private @Getter String firstBodyPartName=null;
	private @Getter String multipartXmlSessionKey;
	private @Getter String mtomContentTransferEncoding = null; // Defaults to 8-bit for normal String messages, 7-bit for e-mails and binary for streams
	private @Getter boolean encodeMessages = false;
	private @Getter Boolean treatInputMessageAsParameters = null;
	private @Getter HttpEntityType postType = HttpEntityType.RAW;

	private HttpEntityFactory entityBuilder;

	@Override
	public void configure() throws ConfigurationException {
		// For backwards compatibility we have to set the contentType to text/html on POST and PUT requests
		if (StringUtils.isEmpty(getContentType()) && postType == HttpEntityType.RAW
				&& (getHttpMethod() == HttpMethod.POST || getHttpMethod() == HttpMethod.PUT || getHttpMethod() == HttpMethod.PATCH)) {
			setContentType("text/html");
		}

		super.configure();

		if (getTreatInputMessageAsParameters() == null && getHttpMethod() != HttpMethod.GET) {
			setTreatInputMessageAsParameters(Boolean.TRUE);
		}

		if (getHttpMethod() != HttpMethod.POST && StringUtils.isNotEmpty(getFirstBodyPartName())) {
			throw new ConfigurationException("firstBodyPartName can only be set for methodType POST");
		}

		// This was introduced in 9.0.0 (issue #8088) but shouldn't be here
		if (postType == HttpEntityType.URLENCODED && StringUtils.isNotBlank(getMultipartXmlSessionKey())) {
			// Some weird backwards-compatibility hacks to be worked around. Now in better place than before, hopefully.
			postType = HttpEntityType.FORMDATA;

			ConfigurationWarnings.add(this, log, "please set postType to FORMDATA or MTOM to use multipartXmlSessionKey");
		}

		entityBuilder = HttpEntityFactory.Builder.create()
				.charSet(getCharSet())
				.entityType(postType)
				.contentType(getFullContentType())
				.parametersToSkipWhenEmpty(parametersToSkipWhenEmptySet)
				.parametersToUse(requestOrBodyParamsSet)
				.firstBodyPartName(getFirstBodyPartName())
				.mtomContentTransferEncoding(mtomContentTransferEncoding)
				.multipartXmlSessionKey(multipartXmlSessionKey)
				.rawWithParametersAppendsInputMessage(BooleanUtils.isTrue(getTreatInputMessageAsParameters()))
				.build();
	}

	@Override
	protected HttpRequestBase getMethod(URI url, Message message, @Nonnull ParameterValueList parameters, PipeLineSession session) throws SenderException {
		if (isEncodeMessages() && !Message.isEmpty(message)) {
			try {
				message = new Message(URLEncoder.encode(message.asString(), getCharSet()));
			} catch (IOException e) {
				throw new SenderException("unable to encode message",e);
			}
		}

		return createRequestMethod(url, message, parameters, session);
	}

	/**
	 * Returns HttpRequestBase, with (optional) RAW or as BINARY content
	 */
	protected HttpRequestBase createRequestMethod(URI uri, Message message, @Nonnull ParameterValueList parameters, PipeLineSession session) throws SenderException {
		try {
			boolean queryParametersAppended = false;
			StringBuilder relativePath = new StringBuilder(uri.getRawPath());
			if (!StringUtils.isEmpty(uri.getRawQuery())) {
				relativePath.append("?").append(uri.getRawQuery());
				queryParametersAppended = true;
			}

			switch (getHttpMethod()) {
			case GET:
				if (parameters.size() > 0) {
					queryParametersAppended = appendParameters(queryParametersAppended,relativePath,parameters);
					log.debug("path after appending of parameters [{}]", relativePath);
				}

				HttpGet getMethod = new HttpGet(relativePath+(parameters.size() == 0 && BooleanUtils.isTrue(getTreatInputMessageAsParameters()) && !Message.isEmpty(message)? message.asString():""));

				log.debug("HttpSender constructed GET-method [{}]", () -> getMethod.getURI().getQuery());
				if (null != getFullContentType()) { // Manually set Content-Type header
					getMethod.setHeader("Content-Type", getFullContentType().toString());
				}
				return getMethod;

			case POST:
			case PUT:
			case PATCH:
				HttpEntityEnclosingRequestBase method;
				if (getHttpMethod() == HttpMethod.POST) {
					method = new HttpPost(relativePath.toString());
				} else if (getHttpMethod() == HttpMethod.PATCH) {
					method = new HttpPatch(relativePath.toString());
				} else {
					method = new HttpPut(relativePath.toString());
				}

				method.setEntity(entityBuilder.create(message, parameters, session));
				return method;

			case DELETE:
				HttpDelete deleteMethod = new HttpDelete(relativePath.toString());
				if (null != getFullContentType()) { // Manually set Content-Type header
					deleteMethod.setHeader("Content-Type", getFullContentType().toString());
				}
				return deleteMethod;

			case HEAD:
				return new HttpHead(relativePath.toString());

			case REPORT:
				Element element = XmlUtils.buildElement(message.asString(), true);
				HttpReport reportMethod = new HttpReport(relativePath.toString(), element);
				if (null != getFullContentType()) { // Manually set Content-Type header
					reportMethod.setHeader("Content-Type", getFullContentType().toString());
				}
				return reportMethod;

			default:
				return null;
			}
		} catch (Exception e) {
			// Catch all exceptions and throw them as SenderException
			throw new SenderException(e);
		}
	}

	@Override
	protected Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws SenderException, IOException {
		int statusCode = responseHandler.getStatusLine().getStatusCode();

		if (!validateResponseCode(statusCode)) {
			Message responseBody = responseHandler.getResponseMessage();
			String body = "";
			if(responseBody != null) {
				try {
					body = responseBody.asString();
				} catch(IOException e) {
					body = "(" + ClassUtils.nameOf(e) + "): " + e.getMessage();
				}
			}
			log.warn("httpstatus [{}] reason [{}]", statusCode, responseHandler.getStatusLine().getReasonPhrase());
			return new Message(body);
		}

		if (responseHandler.isMultipart()) {
			return handleMultipartResponse(responseHandler, session);
		} else {
			return getResponseBody(responseHandler);
		}
	}

	public Message getResponseBody(HttpResponseHandler responseHandler) {
		if (getHttpMethod() == HttpMethod.HEAD) {
			XmlBuilder headersXml = new XmlBuilder("headers");
			Header[] headers = responseHandler.getAllHeaders();
			for (Header header : headers) {
				XmlBuilder headerXml = new XmlBuilder("header");
				headerXml.addAttribute("name", header.getName());
				headerXml.setCdataValue(header.getValue());
				headersXml.addSubElement(headerXml);
			}
			return headersXml.asMessage();
		}

		return responseHandler.getResponseMessage();
	}

	/**
	 * return the first part as Message and put the other parts as InputStream in the PipeLineSession
	 */
	private static Message handleMultipartResponse(@Nonnull HttpResponseHandler httpHandler, @Nonnull PipeLineSession session) throws IOException {
		return handleMultipartResponse(httpHandler.getMimeType(), httpHandler.getResponse(), session);
	}

	/**
	 * return the first part as Message and put the other parts as InputStream in the PipeLineSession
	 */
	private static Message handleMultipartResponse(@Nullable MimeType mimeType, @Nonnull InputStream inputStream, @Nonnull PipeLineSession session) throws IOException {
		Message result = null;
		try {
			InputStreamDataSource dataSource = new InputStreamDataSource(mimeType != null ? mimeType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE, inputStream); // The entire InputStream will be read here!
			MimeMultipart mimeMultipart = new MimeMultipart(dataSource);
			for (int i = 0; i < mimeMultipart.getCount(); i++) {
				BodyPart bodyPart = mimeMultipart.getBodyPart(i);
				if (i == 0) {
					result = new PartMessage(bodyPart);
				} else {
					session.put("multipart" + i, new PartMessage(bodyPart));
				}
			}
		} catch(MessagingException e) {
			throw new IOException("Could not read mime multipart response", e);
		}
		return result;
	}

	/**
	 * If <code>methodType</code>=<code>POST</code>, <code>PUT</code> or <code>PATCH</code>, the type of post request
	 * @ff.default RAW
	 */
	public void setPostType(HttpEntityType type) {
		this.postType = type;
	}

	/** (Only used when <code>methodType=POST</code> and <code>postType=URLENCODED</code>, <code>FORM-DATA</code> or <code>MTOM</code>) Prepends a new BodyPart using the specified name and uses the input of the Sender as content */
	public void setFirstBodyPartName(String firstBodyPartName) {
		this.firstBodyPartName = firstBodyPartName;
	}

	/**
	 * If set and <code>methodType=POST</code> and <code>paramsInUrl=false</code>, a multipart/form-data entity is created instead of a request body.
	 * For each part element in the session key a part in the multipart entity is created. Part elements can contain the following attributes:
	 * <ul>
	 * <li>name: optional, used as 'filename' in Content-Disposition</li>
	 * <li>sessionKey: mandatory, refers to contents of part</li>
	 * <li>value: optional, the contents of the part if the sessionKey specified contains no data</li>
	 * <li>mimeType: optional MIME type</li>
	 * </ul>
	 * The name of the part is determined by the name attribute, unless that is empty, or the contents is binary. In those cases the sessionKey name is used as name of the part.
	 */
	public void setMultipartXmlSessionKey(String multipartXmlSessionKey) {
		this.multipartXmlSessionKey = multipartXmlSessionKey;
	}

	public void setMtomContentTransferEncoding(String mtomContentTransferEncoding) {
		this.mtomContentTransferEncoding = mtomContentTransferEncoding;
	}

	/**
	 * Specifies whether messages will encoded, e.g. spaces will be replaced by '+' etc.
	 * @ff.default false
	 */
	public void setEncodeMessages(boolean b) {
		encodeMessages = b;
	}

	/**
	 * If <code>true</code>, the input will be added to the URL for <code>methodType=GET</code>, or for <code>methodType=POST</code>, <code>PUT</code> or
	 * <code>PATCH</code> if <code>postType=RAW</code>. This used to be the default behaviour in framework version 7.7 and earlier
	 * @ff.default for methodType=<code>GET</code>: <code>false</code>,<br/>for methodTypes <code>POST</code>, <code>PUT</code>, <code>PATCH</code>: <code>true</code>
	 */
	public void setTreatInputMessageAsParameters(Boolean b) {
		treatInputMessageAsParameters = b;
	}
}
