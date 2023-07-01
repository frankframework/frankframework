/*
   Copyright 2013, 2016-2020 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MIME;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;
import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.http.mime.MessageContentBody;
import nl.nn.adapterframework.http.mime.MultipartEntityBuilder;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sender for the HTTP protocol using {@link nl.nn.adapterframework.http.HttpSenderBase.HttpMethod HttpMethod}. By default, any response code outside the 2xx or 3xx range
 * is considered an error and the <code>exception</code> forward of the SenderPipe is followed if present and if there
 * is no forward for the specific HTTP status code. Forwards for specific HTTP codes (e.g. "200", "201", ...)
 * are returned by this sender so they are available to the SenderPipe.
 *
 * <p><b>Expected message format:</b></p>
 * <p>GET methods expect a message looking like this:
 * <pre>
 *    param_name=param_value&another_param_name=another_param_value
 * </pre>
 * <p>POST AND PUT methods expect a message similar as GET, or looking like this:
 * <pre>
 *   param_name=param_value
 *   another_param_name=another_param_value
 * </pre>
 *
 * Note:
 * When used as MTOM sender and MTOM receiver doesn't support Content-Transfer-Encoding "base64", messages without line feeds will give an error.
 * This can be fixed by setting the Content-Transfer-Encoding in the MTOM sender.
 * </p>
 *
 * @author Niels Meijer
 * @since 7.0
 * @version 2.0
 */

public class HttpSender extends HttpSenderBase {

	@Deprecated private @Getter String streamResultToFileNameSessionKey = null;
	@Deprecated private @Getter String storeResultAsStreamInSessionKey;
	@Deprecated private @Getter String storeResultAsByteArrayInSessionKey;

	private @Getter boolean base64=false;
	private @Getter boolean streamResultToServlet=false;

	private @Getter boolean paramsInUrl=true;
	private @Getter String firstBodyPartName=null;

	private @Getter Boolean multipartResponse=null;
	private @Getter String multipartXmlSessionKey;
	private @Getter String mtomContentTransferEncoding = null; //Defaults to 8-bit for normal String messages, 7-bit for e-mails and binary for streams
	private @Getter boolean encodeMessages = false;
	private @Getter Boolean treatInputMessageAsParameters = null;

	private @Getter PostType postType = PostType.RAW;

	private static final MimeType APPLICATION_XOP_XML = MimeType.valueOf("application/xop+xml");

	public enum PostType {
		/** The input message is sent unchanged as character data, like text, XML or JSON, with possibly parameter data appended */
		RAW, // text/html;charset=UTF8
		/** The input message is sent unchanged as binary data */
		BINARY, //application/octet-stream
//		SWA("Soap with Attachments"), // text/xml
		/** Yields a x-www-form-urlencoded form entity */
		URLENCODED,
		/** Yields a multipart/form-data form entity */
		FORMDATA,
		/** Yields a MTOM multipart/related form entity */
		MTOM;
	}

	@Override
	public void configure() throws ConfigurationException {
		//For backwards compatibility we have to set the contentType to text/html on POST and PUT requests
		if(StringUtils.isEmpty(getContentType()) && postType == PostType.RAW && (getHttpMethod() == HttpMethod.POST || getHttpMethod() == HttpMethod.PUT || getHttpMethod() == HttpMethod.PATCH)) {
			setContentType("text/html");
		}

		super.configure();

		if (getTreatInputMessageAsParameters()==null && getHttpMethod()!=HttpMethod.GET) {
			setTreatInputMessageAsParameters(Boolean.TRUE);
		}

		if (getHttpMethod() != HttpMethod.POST) {
			if (!isParamsInUrl()) {
				throw new ConfigurationException(getLogPrefix()+"paramsInUrl can only be set to false for methodType POST");
			}
			if (StringUtils.isNotEmpty(getFirstBodyPartName())) {
				throw new ConfigurationException(getLogPrefix()+"firstBodyPartName can only be set for methodType POST");
			}
		}
	}

	@Override
	protected HttpRequestBase getMethod(URI url, Message message, ParameterValueList parameters, PipeLineSession session) throws SenderException {
		if (isEncodeMessages() && !Message.isEmpty(message)) {
			try {
				message = new Message(URLEncoder.encode(message.asString(), getCharSet()));
			} catch (IOException e) {
				throw new SenderException(getLogPrefix()+"unable to encode message",e);
			}
		}

		URI uri;
		try {
			uri = encodeQueryParameters(url);
		} catch (UnsupportedEncodingException | URISyntaxException e) {
			throw new SenderException("error encoding queryparameters in url ["+url.toString()+"]", e);
		}

		if(postType==PostType.URLENCODED || postType==PostType.FORMDATA || postType==PostType.MTOM) {
			try {
				return getMultipartPostMethodWithParamsInBody(uri, message, parameters, session);
			} catch (IOException e) {
				throw new SenderException(getLogPrefix()+"unable to read message", e);
			}
		}
		// RAW + BINARY
		return getMethod(uri, message, parameters);
	}

	// Encode query parameter values.
	private URI encodeQueryParameters(URI url) throws UnsupportedEncodingException, URISyntaxException {
		URIBuilder uri = new URIBuilder(url);
		ArrayList<NameValuePair> pairs = new ArrayList<>(uri.getQueryParams().size());
		for(NameValuePair pair : uri.getQueryParams()) {
			String paramValue = pair.getValue(); //May be NULL
			if(StringUtils.isNotEmpty(paramValue)) {
				paramValue = URLEncoder.encode(paramValue, getCharSet()); //Only encode if the value is not null
			}
			pairs.add(new BasicNameValuePair(pair.getName(), paramValue));
		}
		if(!pairs.isEmpty()) {
			uri.clearParameters();
			uri.addParameters(pairs);
		}
		return uri.build();
	}

	/**
	 * Returns HttpRequestBase, with (optional) RAW or as BINAIRY content
	 */
	protected HttpRequestBase getMethod(URI uri, Message message, ParameterValueList parameters) throws SenderException {
		try {
			boolean queryParametersAppended = false;
			StringBuilder relativePath = new StringBuilder(uri.getRawPath());
			if (!StringUtils.isEmpty(uri.getQuery())) {
				relativePath.append("?").append(uri.getQuery());
				queryParametersAppended = true;
			}

			switch (getHttpMethod()) {
			case GET:
				if (parameters!=null) {
					queryParametersAppended = appendParameters(queryParametersAppended,relativePath,parameters);
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"path after appending of parameters ["+relativePath+"]");
				}

				HttpGet getMethod = new HttpGet(relativePath+(parameters==null && BooleanUtils.isTrue(getTreatInputMessageAsParameters()) && !Message.isEmpty(message)? message.asString():""));

				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"HttpSender constructed GET-method ["+getMethod.getURI().getQuery()+"]");
				if (null != getFullContentType()) { //Manually set Content-Type header
					getMethod.setHeader("Content-Type", getFullContentType().toString());
				}
				return getMethod;

			case POST:
			case PUT:
			case PATCH:
				HttpEntity entity;
				if(postType == PostType.RAW) {
					String messageString = BooleanUtils.isTrue(getTreatInputMessageAsParameters()) && !Message.isEmpty(message) ? message.asString() : "";
					if (parameters!=null) {
						StringBuilder msg = new StringBuilder(messageString);
						appendParameters(true,msg,parameters);
						if (StringUtils.isEmpty(messageString) && msg.length()>1) {
							messageString=msg.substring(1);
						} else {
							messageString=msg.toString();
						}
					}
					entity = new ByteArrayEntity(messageString.getBytes(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING), getFullContentType());
				} else if(postType == PostType.BINARY) {
					entity = new HttpMessageEntity(message, getFullContentType());
				} else {
					throw new SenderException("PostType ["+postType.name()+"] not allowed!");
				}

				HttpEntityEnclosingRequestBase method;
				if (getHttpMethod() == HttpMethod.POST) {
					method = new HttpPost(relativePath.toString());
				} else if (getHttpMethod() == HttpMethod.PATCH) {
					method = new HttpPatch(relativePath.toString());
				} else {
					method = new HttpPut(relativePath.toString());
				}

				method.setEntity(entity);
				return method;

			case DELETE:
				HttpDelete deleteMethod = new HttpDelete(relativePath.toString());
				if (null != getFullContentType()) { //Manually set Content-Type header
					deleteMethod.setHeader("Content-Type", getFullContentType().toString());
				}
				return deleteMethod;

			case HEAD:
				return new HttpHead(relativePath.toString());

			case REPORT:
				Element element = XmlUtils.buildElement(message.asString(), true);
				HttpReport reportMethod = new HttpReport(relativePath.toString(), element);
				if (null != getFullContentType()) { //Manually set Content-Type header
					reportMethod.setHeader("Content-Type", getFullContentType().toString());
				}
				return reportMethod;

			default:
				return null;
			}
		} catch (Exception e) {
			//Catch all exceptions and throw them as SenderException
			throw new SenderException(e);
		}
	}

	/**
	 * Returns a multi-parted message, either as X-WWW-FORM-URLENCODED, FORM-DATA or MTOM
	 * @throws IOException
	 */
	private HttpPost getMultipartPostMethodWithParamsInBody(URI uri, Message message, ParameterValueList parameters, PipeLineSession session) throws SenderException, IOException {
		HttpPost hmethod = new HttpPost(uri);

		if (postType==PostType.URLENCODED && StringUtils.isEmpty(getMultipartXmlSessionKey())) { // x-www-form-urlencoded
			List<NameValuePair> requestFormElements = new ArrayList<>();

			if (StringUtils.isNotEmpty(getFirstBodyPartName())) {
				requestFormElements.add(new BasicNameValuePair(getFirstBodyPartName(), message.asString()));
				log.debug(getLogPrefix()+"appended parameter ["+getFirstBodyPartName()+"] with value ["+message+"]");
			}
			if (parameters!=null) {
				for(ParameterValue pv : parameters) {
					String name = pv.getDefinition().getName();
					String value = pv.asStringValue("");

					if (requestOrBodyParamsSet.contains(name) && (StringUtils.isNotEmpty(value) || !parametersToSkipWhenEmptySet.contains(name))) {
						requestFormElements.add(new BasicNameValuePair(name,value));
						if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended parameter ["+name+"] with value ["+value+"]");
					}
				}
			}
			try {
				hmethod.setEntity(new UrlEncodedFormEntity(requestFormElements, getCharSet()));
			} catch (UnsupportedEncodingException e) {
				throw new SenderException(getLogPrefix()+"unsupported encoding for one or more POST parameters", e);
			}
		}
		else { //formdata and mtom
			HttpEntity requestEntity = createMultiPartEntity(message, parameters, session);
			hmethod.setEntity(requestEntity);
		}

		return hmethod;
	}

	private FormBodyPart createStringBodypart(Message message) {
		MimeType mimeType = (postType == PostType.MTOM) ? APPLICATION_XOP_XML : MediaType.TEXT_PLAIN; // only the first part is XOP+XML, other parts should use their own content-type
		FormBodyPartBuilder bodyPart = FormBodyPartBuilder.create(getFirstBodyPartName(), new MessageContentBody(message, mimeType));

		// Should only be set when request is MTOM and it's the first BodyPart
		if (postType == PostType.MTOM && StringUtils.isNotEmpty(getMtomContentTransferEncoding())) {
			bodyPart.setField(MIME.CONTENT_TRANSFER_ENC, getMtomContentTransferEncoding());
		}

		return bodyPart.build();
	}

	private HttpEntity createMultiPartEntity(Message message, ParameterValueList parameters, PipeLineSession session) throws SenderException, IOException {
		MultipartEntityBuilder entity = MultipartEntityBuilder.create();

		entity.setCharset(Charset.forName(getCharSet()));
		if(postType == PostType.MTOM)
			entity.setMtomMultipart();

		if (StringUtils.isNotEmpty(getFirstBodyPartName())) {
			entity.addPart(createStringBodypart(message));
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended stringpart ["+getFirstBodyPartName()+"] with value ["+message+"]");
		}
		if (parameters!=null) {
			for(ParameterValue pv : parameters) {
				String name = pv.getDefinition().getName();
				if (requestOrBodyParamsSet.contains(name)) {
					Message msg = pv.asMessage();
					if (!msg.isEmpty() || !parametersToSkipWhenEmptySet.contains(name)) {

						String fileName = null;
						String sessionKey = pv.getDefinition().getSessionKey();
						if (sessionKey != null) {
							fileName = session.getString(sessionKey + "Name");
						}
						if(fileName != null) {
							log.warn("setting filename using [{}Name] for bodypart [{}]. Consider using a MultipartXml with the attribute [name] instead.", sessionKey, fileName, name);
						}

						entity.addPart(name, new MessageContentBody(msg, null, fileName));
						if (log.isDebugEnabled()) log.debug("{}appended bodypart [{}] with message [{}]", getLogPrefix(), name, msg);
					}
				}
			}
		}

		if (StringUtils.isNotEmpty(getMultipartXmlSessionKey())) {
			String multipartXml = session.getString(getMultipartXmlSessionKey());
			log.debug(getLogPrefix()+"building multipart message with MultipartXmlSessionKey ["+multipartXml+"]");
			if (StringUtils.isEmpty(multipartXml)) {
				log.warn(getLogPrefix()+"sessionKey [" +getMultipartXmlSessionKey()+"] is empty");
			} else {
				Element partsElement;
				try {
					partsElement = XmlUtils.buildElement(multipartXml);
				} catch (DomBuilderException e) {
					throw new SenderException(getLogPrefix()+"error building multipart xml", e);
				}
				Collection<Node> parts = XmlUtils.getChildTags(partsElement, "part");
				if (parts.isEmpty()) {
					log.warn(getLogPrefix()+"no part(s) in multipart xml [" + multipartXml + "]");
				} else {
					for (final Node part : parts) {
						Element partElement = (Element) part;
						entity.addPart(elementToFormBodyPart(partElement, session));
					}
				}
			}
		}

		return entity.build();
	}

	protected FormBodyPart elementToFormBodyPart(Element element, PipeLineSession session) throws IOException {
		String partName = element.getAttribute("name"); //Name of the part
		String partSessionKey = element.getAttribute("sessionKey"); //SessionKey to retrieve data from
		String partMimeType = element.getAttribute("mimeType"); //MimeType of the part
		Message partObject = session.getMessage(partSessionKey);
		MimeType mimeType = null;
		if(StringUtils.isNotEmpty(partMimeType)) {
			mimeType = MimeType.valueOf(partMimeType);
		}

		String name = partObject.isBinary() || StringUtils.isBlank(partName) ? partSessionKey : partName;
		String filename = StringUtils.isNotBlank(partName) ? partName : null;
		return FormBodyPartBuilder.create(name, new MessageContentBody(partObject, mimeType, filename)).build();
	}

	@Override
	protected Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws SenderException, IOException {
		int statusCode = responseHandler.getStatusLine().getStatusCode();

		if (!validateResponseCode(statusCode)) {
			Message responseBody = responseHandler.getResponseMessage();
			String body = "";
			if(responseBody != null) {
				responseBody.preserve();
				try {
					body = responseBody.asString();
				} catch(IOException e) {
					body = "(" + ClassUtils.nameOf(e) + "): " + e.getMessage();
				}
			}
			log.warn(getLogPrefix() + "httpstatus [" + statusCode + "] reason [" + responseHandler.getStatusLine().getReasonPhrase() + "]");
			return new Message(body);
		}

		HttpServletResponse response = null;
		if (isStreamResultToServlet())
			response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);

		if (response==null) {
			Message responseMessage = responseHandler.getResponseMessage();
			if(!Message.isEmpty(responseMessage)) {
				responseMessage.closeOnCloseOf(session, this);
			}

			if (StringUtils.isNotEmpty(getStreamResultToFileNameSessionKey())) {
				try {
					String fileName = session.getString(getStreamResultToFileNameSessionKey());
					File file = new File(fileName);
					StreamUtil.streamToFile(responseMessage.asInputStream(), file);
					return new Message(fileName);
				} catch (IOException e) {
					throw new SenderException("cannot find filename to stream result to", e);
				}
			} else if (isBase64()) { //This should be removed in a future iteration
				return getResponseBodyAsBase64(responseMessage.asInputStream());
			} else if (StringUtils.isNotEmpty(getStoreResultAsStreamInSessionKey())) {
				session.put(getStoreResultAsStreamInSessionKey(), responseMessage.asInputStream());
				return Message.nullMessage();
			} else if (StringUtils.isNotEmpty(getStoreResultAsByteArrayInSessionKey())) {
				session.put(getStoreResultAsByteArrayInSessionKey(), responseMessage.asByteArray());
				return Message.nullMessage();
			} else if (BooleanUtils.isTrue(getMultipartResponse()) || responseHandler.isMultipart()) {
				if(BooleanUtils.isFalse(getMultipartResponse())) {
					log.warn("multipart response was set to false, but the response is multipart!");
				}
				return handleMultipartResponse(responseHandler, session);
			} else {
				return getResponseBody(responseHandler);
			}
		}
		streamResponseBody(responseHandler, response);
		return Message.nullMessage();
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
			return Message.asMessage(headersXml.toXML());
		}

		return responseHandler.getResponseMessage();
	}

	public Message getResponseBodyAsBase64(InputStream is) {
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"base64 encodes response body");
		return new Message( new Base64InputStream(is, true) );
	}

	/**
	 * return the first part as Message and put the other parts as InputStream in the PipeLineSession
	 */
	private static Message handleMultipartResponse(HttpResponseHandler httpHandler, PipeLineSession session) throws IOException {
		return handleMultipartResponse(httpHandler.getContentType().getMimeType(), httpHandler.getResponse(), session);
	}

	/**
	 * return the first part as Message and put the other parts as InputStream in the PipeLineSession
	 */
	public static Message handleMultipartResponse(String mimeType, InputStream inputStream, PipeLineSession session) throws IOException {
		Message result = null;
		try {
			InputStreamDataSource dataSource = new InputStreamDataSource(mimeType, inputStream); //the entire InputStream will be read here!
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

	private void streamResponseBody(HttpResponseHandler responseHandler, HttpServletResponse response) throws IOException {
		streamResponseBody(responseHandler.getResponse(), responseHandler.getHeader("Content-Type"), responseHandler.getHeader("Content-Disposition"), response, log, getLogPrefix());
	}

	public static void streamResponseBody(InputStream is, String contentType, String contentDisposition, HttpServletResponse response, Logger log, String logPrefix) throws IOException {
		streamResponseBody(is, contentType, contentDisposition, response, log, logPrefix, null);
	}

	public static void streamResponseBody(InputStream is, String contentType, String contentDisposition, HttpServletResponse response, Logger log, String logPrefix, String redirectLocation) throws IOException {
		if (StringUtils.isNotEmpty(contentType)) {
			response.setHeader("Content-Type", contentType);
		}
		if (StringUtils.isNotEmpty(contentDisposition)) {
			response.setHeader("Content-Disposition", contentDisposition);
		}
		if (StringUtils.isNotEmpty(redirectLocation)) {
			response.sendRedirect(redirectLocation);
		}
		if (is != null) {
			try (OutputStream outputStream = response.getOutputStream()) {
				StreamUtil.streamToStream(is, outputStream);
				log.debug(logPrefix + "copied response body input stream [" + is + "] to output stream [" + outputStream + "]");
			}
		}
	}

	/**
	 * If <code>methodType</code>=<code>POST</code>, <code>PUT</code> or <code>PATCH</code>, the type of post request
	 * @ff.default RAW
	 */
	public void setPostType(PostType type) {
		this.postType = type;
	}

	/**
	 * If false and <code>methodType</code>=<code>POST</code>, request parameters are put in the request body instead of in the url
	 * @ff.default true
	 */
	@Deprecated
	public void setParamsInUrl(boolean b) {
		if(!b) {
			if(!postType.equals(PostType.MTOM) && !postType.equals(PostType.FORMDATA)) { //Don't override if another type has explicitly been set
				postType = PostType.URLENCODED;
				ConfigurationWarnings.add(this, log, "attribute [paramsInUrl] is deprecated: please use postType='URLENCODED' instead", SuppressKeys.DEPRECATION_SUPPRESS_KEY, null);
			} else {
				ConfigurationWarnings.add(this, log, "attribute [paramsInUrl] is deprecated: no longer required when using FORMDATA or MTOM requests", SuppressKeys.DEPRECATION_SUPPRESS_KEY, null);
			}
		}
		paramsInUrl = b;
	}

	@Deprecated
	@ConfigurationWarning("Use the firstBodyPartName attribute instead")
	public void setInputMessageParam(String inputMessageParam) {
		setFirstBodyPartName(inputMessageParam);
	}
	/** (Only used when <code>methodType</code>=<code>POST</code> and <code>postType</code>=<code>URLENCODED</code>, <code>FORM-DATA</code> or <code>MTOM</code>) Prepends a new BodyPart using the specified name and uses the input of the Sender as content */
	public void setFirstBodyPartName(String firstBodyPartName) {
		this.firstBodyPartName = firstBodyPartName;
	}

	/** If set, the result is streamed to a file (instead of passed as a string) */
	@Deprecated
	@ConfigurationWarning("no longer required to store the result as a file in the PipeLineSession, the sender can return binary data")
	public void setStreamResultToFileNameSessionKey(String string) {
		streamResultToFileNameSessionKey = string;
	}

	/** If set, a pointer to an input stream of the result is put in the specified sessionkey (as the sender interface only allows a sender to return a string a sessionkey is used instead to return the stream) */
	@Deprecated
	@ConfigurationWarning("no longer required to store the result as a stream in the PipeLineSession, the sender can return binary data")
	public void setStoreResultAsStreamInSessionKey(String storeResultAsStreamInSessionKey) {
		this.storeResultAsStreamInSessionKey = storeResultAsStreamInSessionKey;
	}

	@Deprecated
	@ConfigurationWarning("no longer required to store the result as a byte array in the PipeLineSession, the sender can return binary data")
	public void setStoreResultAsByteArrayInSessionKey(String storeResultAsByteArrayInSessionKey) {
		this.storeResultAsByteArrayInSessionKey = storeResultAsByteArrayInSessionKey;
	}

	/**
	 * If true, the result is Base64 encoded
	 * @ff.default false
	 */
	@Deprecated
	@ConfigurationWarning("use Base64Pipe instead")
	public void setBase64(boolean b) {
		base64 = b;
	}

	/**
	 * If set, the result is streamed to the HhttpServletResponse object of the RestServiceDispatcher (instead of passed as a string)
	 * @ff.default false
	 */
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}

	@Deprecated
	@ConfigurationWarning("multipart has been replaced by postType='formdata'")
	/**
	 * If true and <code>methodType<code>=<code>POST</code> and <code>paramsInUrl</code>=<code>false</code>, request parameters are put in a multipart/form-data entity instead of in the request body
	 * @ff.default false
	 */
	public void setMultipart(boolean b) {
		if(b && !postType.equals(PostType.MTOM)) {
			postType = PostType.FORMDATA;
		}
	}

	@Deprecated
	@ConfigurationWarning("Unless set explicitly multipart response will be detected automatically")
	/**
	 * If true the response body is expected to be in mime multipart which is the case when a soap message with attachments is received (see also <a href=\"https://docs.oracle.com/javaee/7/api/javax/xml/soap/soapmessage.html\">https://docs.oracle.com/javaee/7/api/javax/xml/soap/soapmessage.html</a>). the first part will be returned as result of this sender. other parts are returned as streams in sessionkeys with names multipart1, multipart2, etc. the http connection is held open until the last stream is read.
	 * @ff.default false
	 */
	public void setMultipartResponse(Boolean b) {
		multipartResponse = b;
	}

	/**
	 * If set and <code>methodType</code>=<code>POST</code> and <code>paramsInUrl</code>=<code>false</code>, a multipart/form-data entity is created instead of a request body.
	 * For each part element in the session key a part in the multipart entity is created. Part elements can contain the following attributes:
	 * <ul>
	 * <li>name: optional, used as 'filename' in Content-Disposition</li>
	 * <li>sessionKey: mandatory, refers to contents of part</li>
	 * <li>mimeType: optional MIME type</li>
	 * </ul>
	 * The name of the part is determined by the name attribute, unless that is empty, or the contents is binary. In those cases the sessionKey name is used as name of the part.
	 */
	public void setMultipartXmlSessionKey(String multipartXmlSessionKey) {
		this.multipartXmlSessionKey = multipartXmlSessionKey;
	}

	@Deprecated
	@ConfigurationWarning("mtomEnabled has been replaced by postType='mtom'")
	public void setMtomEnabled(boolean b) {
		if(b) postType = PostType.MTOM;
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
	 * If <code>true</code>, the input will be added to the URL for <code>methodType</code>=<code>GET</code>, or for <code>methodType</code>=<code>POST</code>, <code>PUT</code> or <code>PATCH</code> if <code>postType</code>=<code>RAW</code>. This used to be the default behaviour in framework version 7.7 and earlier
	 * @ff.default for methodType=<code>GET</code>: <code>false</code>,<br/>for methodTypes <code>POST</code>, <code>PUT</code>, <code>PATCH</code>: <code>true</code>
	 */
	public void setTreatInputMessageAsParameters(Boolean b) {
		treatInputMessageAsParameters = b;
	}
}
