/*
   Copyright 2013, 2016-2020 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
import java.util.Iterator;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.DocumentedEnum;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.http.mime.MultipartEntityBuilder;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sender for the HTTP protocol using GET, POST, PUT or DELETE.
 * 
 * <p><b>Parameters:</b></p>
 * <p>Any parameters present are appended to the request as request-parameters except the headersParams list which are added as http headers</p>
 * 
 * <p><b>Expected message format:</b></p>
 * <p>GET methods expect a message looking like this</p>
 * <pre>
 *   param_name=param_value&another_param_name=another_param_value
 * </pre>
 * <p>POST AND PUT methods expect a message similar as GET, or looking like this</p>
 * <pre>
 *   param_name=param_value
 *   another_param_name=another_param_value
 * </pre>
 *
 * <p>
 * Note 1:
 * Some certificates require the &lt;java_home&gt;/jre/lib/security/xxx_policy.jar files to be upgraded to unlimited strength. Typically, in such a case, an error message like 
 * <code>Error in loading the keystore: Private key decryption error: (java.lang.SecurityException: Unsupported keysize or algorithm parameters</code> is observed.
 * For IBM JDKs these files can be downloaded from http://www.ibm.com/developerworks/java/jdk/security/50/ (scroll down to 'IBM SDK Policy files')
 * </p>
 * Replace in the directory java\jre\lib\security the following files:
 * <ul>
 * <li>local_policy.jar</li>
 * <li>US_export_policy.jar</li>
 * </ul>
 * <p>
 * Note 2:
 * To debug ssl-related problems, set the following system property:
 * <ul>
 * <li>IBM / WebSphere: <code>-Djavax.net.debug=true</code></li>
 * <li>SUN: <code>-Djavax.net.debug=all</code></li>
 * </ul>
 * </p>
 * <p>
 * Note 3:
 * In case <code>javax.net.ssl.SSLHandshakeException: unknown certificate</code>-exceptions are thrown, 
 * probably the certificate of the other party is not trusted. Try to use one of the certificates in the path as your truststore by doing the following:
 * <ul>
 *   <li>open the URL you are trying to reach in InternetExplorer</li>
 *   <li>click on the yellow padlock on the right in the bottom-bar. This opens the certificate information window</li>
 *   <li>click on tab 'Certificeringspad'</li>
 *   <li>double click on root certificate in the tree displayed. This opens the certificate information window for the root certificate</li>
 *   <li>click on tab 'Details'</li>
 *   <li>click on 'Kopieren naar bestand'</li>
 *   <li>click 'next', choose 'DER Encoded Binary X.509 (.CER)'</li>
 *   <li>click 'next', choose a filename</li>
 *   <li>click 'next' and 'finish'</li>
 * 	 <li>Start IBM key management tool ikeyman.bat, located in Program Files/IBM/WebSphere Studio/Application Developer/v5.1.2/runtimes/base_v51/bin (or similar)</li>
 *   <li>create a new key-database (Sleuteldatabase -> Nieuw...), or open the default key.jks (default password="changeit")</li>
 *   <li>add the generated certificate (Toevoegen...)</li>
 *   <li>store the key-database in JKS format</li>
 *   <li>if you didn't use the standard keydatabase, then reference the file in the truststore-attribute in Configuration.xml (include the file as a resource)</li>
 *   <li>use jks for the truststoreType-attribute</li>
 *   <li>restart your application</li>
 *   <li>instead of IBM ikeyman you can use the standard java tool <code>keytool</code> as follows: 
 *      <code>keytool -import -alias <i>yourAlias</i> -file <i>pathToSavedCertificate</i></code></li>
 * </ul>
 * <p>
 * Note 4:
 * In case <code>cannot create or initialize SocketFactory: (IOException) Unable to verify MAC</code>-exceptions are thrown,
 * please check password or authAlias configuration of the correspondig certificate. 
 * </p>
 * 
 * <p>
 * Note 5:
 * When used as MTOM sender and MTOM receiver doesn't support Content-Transfer-Encoding "base64", messages without line feeds will give an error.
 * This can be fixed by setting the Content-Transfer-Encoding in the MTOM sender.
 * </p>
 * 
 * @author Niels Meijer
 * @since 7.0
 * @version 2.0
 */

public class HttpSender extends HttpSenderBase {

	@Deprecated private String streamResultToFileNameSessionKey = null;
	@Deprecated private String storeResultAsStreamInSessionKey;
	@Deprecated private String storeResultAsByteArrayInSessionKey;

	private boolean base64=false;
	private boolean streamResultToServlet=false;

	private boolean paramsInUrl=true;
	private boolean ignoreRedirects=false;
	private String firstBodyPartName=null;

	private Boolean multipartResponse=null;
	private String multipartXmlSessionKey;
	private String mtomContentTransferEncoding = null; //Defaults to 8-bit for normal String messages, 7-bit for e-mails and binary for streams
	private boolean encodeMessages = false;

	private PostType postType = PostType.RAW;

	public enum PostType implements DocumentedEnum {
		RAW("raw text/xml/json"), // text/html;charset=UTF8
		BINARY("binary content"), //application/octet-stream
//		SWA("Soap with Attachments"), // text/xml
		URLENCODED("x-www-form-urlencoded"), // application/x-www-form-urlencoded
		FORMDATA("form-data"), // multipart/form-data
		MTOM("mtom"); // multipart/related

		private String description;
		PostType(String description) {
			this.description = description;
		}
		@Override
		public String toString() {
			return description;
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		//For backwards compatibility we have to set the contentType to text/html on POST and PUT requests
		if(StringUtils.isEmpty(getContentType()) && postType == PostType.RAW && (getMethodTypeEnum() == HttpMethod.POST || getMethodTypeEnum() == HttpMethod.PUT || getMethodTypeEnum() == HttpMethod.PATCH)) {
			setContentType("text/html");
		}

		super.configure();

		if (getMethodTypeEnum() != HttpMethod.POST) {
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
		if (isEncodeMessages()) {
			try {
				message = new Message(URLEncoder.encode(message.asString(), getCharSet()));
			} catch (IOException e) {
				throw new SenderException(getLogPrefix()+"unable to encode message",e);
			}
		}

		URI uri = null;
		try {
			uri = encodeQueryParameters(url);
		} catch (UnsupportedEncodingException | URISyntaxException e) {
			throw new SenderException("error encoding queryparameters in url ["+url.toString()+"]", e);
		}

		if(postType.equals(PostType.URLENCODED) || postType.equals(PostType.FORMDATA) || postType.equals(PostType.MTOM)) {
			try {
				return getMultipartPostMethodWithParamsInBody(uri, message.asString(), parameters, session);
			} catch (IOException e) {
				throw new SenderException(getLogPrefix()+"unable to read message", e);
			}
		} else { // RAW + BINARY
			return getMethod(uri, message, parameters);
		}
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
			StringBuffer relativePath = new StringBuffer(uri.getRawPath());
			if (!StringUtils.isEmpty(uri.getQuery())) {
				relativePath.append("?"+uri.getQuery());
				queryParametersAppended = true;
			}

			switch (getMethodTypeEnum()) {
			case GET:
				if (parameters!=null) {
					queryParametersAppended = appendParameters(queryParametersAppended,relativePath,parameters);
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"path after appending of parameters ["+relativePath+"]");
				}
				HttpGet getMethod = new HttpGet(relativePath+(parameters==null? message.asString():""));

				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"HttpSender constructed GET-method ["+getMethod.getURI().getQuery()+"]");
				if (null != getFullContentType()) { //Manually set Content-Type header
					getMethod.setHeader("Content-Type", getFullContentType().toString());
				}
				return getMethod;

			case POST:
			case PUT:
			case PATCH:
				HttpEntity entity;
				if(postType.equals(PostType.RAW)) {
					String messageString = message.asString();
					if (parameters!=null) {
						StringBuffer msg = new StringBuffer(messageString);
						appendParameters(true,msg,parameters);
						if (StringUtils.isEmpty(messageString) && msg.length()>1) {
							messageString=msg.substring(1);
						} else {
							messageString=msg.toString();
						}
					}
					entity = new ByteArrayEntity(messageString.getBytes(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING), getFullContentType());
				} else if(postType.equals(PostType.BINARY)) {
					entity = new InputStreamEntity(message.asInputStream(), getFullContentType());
				} else {
					throw new SenderException("PostType ["+postType.name()+"] not allowed!");
				}

				HttpEntityEnclosingRequestBase method;
				if (getMethodTypeEnum() == HttpMethod.POST) {
					method = new HttpPost(relativePath.toString());
				} else if (getMethodTypeEnum() == HttpMethod.PATCH) {
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
	protected HttpPost getMultipartPostMethodWithParamsInBody(URI uri, String message, ParameterValueList parameters, PipeLineSession session) throws SenderException, IOException {
		HttpPost hmethod = new HttpPost(uri);

		if (postType.equals(PostType.URLENCODED) && StringUtils.isEmpty(getMultipartXmlSessionKey())) { // x-www-form-urlencoded
			List<NameValuePair> requestFormElements = new ArrayList<NameValuePair>();

			if (StringUtils.isNotEmpty(getFirstBodyPartName())) {
				requestFormElements.add(new BasicNameValuePair(getFirstBodyPartName(),message));
				log.debug(getLogPrefix()+"appended parameter ["+getFirstBodyPartName()+"] with value ["+message+"]");
			}
			if (parameters!=null) {
				for(int i=0; i<parameters.size(); i++) {
					ParameterValue pv = parameters.getParameterValue(i);
					String name = pv.getDefinition().getName();
					String value = pv.asStringValue("");

					// Skip parameters that are configured as ignored
					if (skipParameter(name))
						continue;

					requestFormElements.add(new BasicNameValuePair(name,value));
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended parameter ["+name+"] with value ["+value+"]");
				}
			}
			try {
				hmethod.setEntity(new UrlEncodedFormEntity(requestFormElements, getCharSet()));
			} catch (UnsupportedEncodingException e) {
				throw new SenderException(getLogPrefix()+"unsupported encoding for one or more post parameters");
			}
		}
		else { //formdata and mtom
			HttpEntity requestEntity = createMultiPartEntity(message, parameters, session);
			hmethod.setEntity(requestEntity);
		}

		return hmethod;
	}

	protected FormBodyPart createMultipartBodypart(String name, String message) {
		if(postType.equals(PostType.MTOM))
			return createMultipartBodypart(name, message, "application/xop+xml");
		else
			return createMultipartBodypart(name, message, null);
	}

	protected FormBodyPart createMultipartBodypart(String name, String message, String contentType) {
		ContentType cType = ContentType.create("text/plain", getCharSet());
		if(StringUtils.isNotEmpty(contentType))
			cType = ContentType.create(contentType, getCharSet());

		FormBodyPartBuilder bodyPart = FormBodyPartBuilder.create()
			.setName(name)
			.setBody(new StringBody(message, cType));

		if (StringUtils.isNotEmpty(getMtomContentTransferEncoding()))
			bodyPart.setField(MIME.CONTENT_TRANSFER_ENC, getMtomContentTransferEncoding());

		return bodyPart.build();
	}

	protected FormBodyPart createMultipartBodypart(String name, InputStream is, String fileName) {
		return createMultipartBodypart(name, is, fileName, ContentType.APPLICATION_OCTET_STREAM.getMimeType());
	}

	protected FormBodyPart createMultipartBodypart(String name, InputStream is, String fileName, String contentType) {
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appending filepart ["+name+"] with value ["+is+"] fileName ["+fileName+"] and contentType ["+contentType+"]");
		FormBodyPartBuilder bodyPart = FormBodyPartBuilder.create()
			.setName(name)
			.setBody(new InputStreamBody(is, ContentType.create(contentType, getCharSet()), fileName));
		return bodyPart.build();
	}

	protected HttpEntity createMultiPartEntity(String message, ParameterValueList parameters, PipeLineSession session) throws SenderException, IOException {
		MultipartEntityBuilder entity = MultipartEntityBuilder.create();

		entity.setCharset(Charset.forName(getCharSet()));
		if(postType.equals(PostType.MTOM))
			entity.setMtomMultipart();

		if (StringUtils.isNotEmpty(getFirstBodyPartName())) {
			entity.addPart(createMultipartBodypart(getFirstBodyPartName(), message));
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended stringpart ["+getFirstBodyPartName()+"] with value ["+message+"]");
		}
		if (parameters!=null) {
			for(int i=0; i<parameters.size(); i++) {
				ParameterValue pv = parameters.getParameterValue(i);
				String paramType = pv.getDefinition().getType();
				String name = pv.getDefinition().getName();

				// Skip parameters that are configured as ignored
				if (skipParameter(name))
					continue;


				if (Parameter.TYPE_INPUTSTREAM.equals(paramType)) {
					Object value = pv.getValue();
					if (value instanceof InputStream) {
						InputStream fis = (InputStream)value;
						String fileName = null;
						String sessionKey = pv.getDefinition().getSessionKey();
						if (sessionKey != null) {
							fileName = session.getMessage(sessionKey + "Name").asString();
						}

						entity.addPart(createMultipartBodypart(name, fis, fileName));
						if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended filepart ["+name+"] with value ["+value+"] and name ["+fileName+"]");
					} else {
						throw new SenderException(getLogPrefix()+"unknown inputstream ["+value.getClass()+"] for parameter ["+name+"]");
					}
				} else {
					String value = pv.asStringValue("");
					entity.addPart(createMultipartBodypart(name, value));
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended stringpart ["+name+"] with value ["+value+"]");
				}
			}
		}

		if (StringUtils.isNotEmpty(getMultipartXmlSessionKey())) {
			String multipartXml = session.getMessage(getMultipartXmlSessionKey()).asString();
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
				if (parts==null || parts.size()==0) {
					log.warn(getLogPrefix()+"no part(s) in multipart xml [" + multipartXml + "]");
				} else {
					Iterator<Node> iter = parts.iterator();
					while (iter.hasNext()) {
						Element partElement = (Element) iter.next();
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

		if (partObject.isBinary()) {
			return createMultipartBodypart(partSessionKey, partObject.asInputStream(), partName, partMimeType);
		} else {
			return createMultipartBodypart(partName, partObject.asString(), partMimeType);
		}
	}

	protected boolean validateResponseCode(int statusCode) {
		boolean ok = false;
		if (StringUtils.isNotEmpty(getResultStatusCodeSessionKey())) {
			ok = true;
		} else {
			if (statusCode==200 || statusCode==201 || statusCode==202 || statusCode==204 || statusCode==206) {
				ok = true;
			} else {
				if (isIgnoreRedirects() && (statusCode==HttpServletResponse.SC_MOVED_PERMANENTLY || statusCode==HttpServletResponse.SC_MOVED_TEMPORARILY || statusCode==HttpServletResponse.SC_TEMPORARY_REDIRECT)) {
					ok = true;
				}
			}
		}
		return ok;
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
			throw new SenderException(getLogPrefix() + "httpstatus [" + statusCode + "] reason [" + responseHandler.getStatusLine().getReasonPhrase() + "] body [" + body +"]");
		}

		HttpServletResponse response = null;
		if (isStreamResultToServlet())
			response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);

		if (response==null) {
			Message responseMessage = responseHandler.getResponseMessage();
			if(!Message.isEmpty(responseMessage)) {
				responseMessage.closeOnCloseOf(session);
			}

			if (StringUtils.isNotEmpty(getStreamResultToFileNameSessionKey())) {
				try {
					String fileName = session.getMessage(getStreamResultToFileNameSessionKey()).asString();
					File file = new File(fileName);
					Misc.streamToFile(responseMessage.asInputStream(), file);
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
		} else {
			streamResponseBody(responseHandler, response);
			return Message.nullMessage();
		}
	}

	public Message getResponseBody(HttpResponseHandler responseHandler) {
		if (getMethodTypeEnum() == HttpMethod.HEAD) {
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
				Misc.streamToStream(is, outputStream);
				log.debug(logPrefix + "copied response body input stream [" + is + "] to output stream [" + outputStream + "]");
			}
		}
	}

	@IbisDoc({"When <code>methodType=POST</code>, the type of post request", "RAW"})
	public void setPostType(String type) {
		this.postType = EnumUtils.parse(PostType.class, type);
	}

	@IbisDoc({"When false and <code>methodType=POST</code>, request parameters are put in the request body instead of in the url", "true"})
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
	public boolean isParamsInUrl() {
		return paramsInUrl;
	}

	@Deprecated
	@ConfigurationWarning("Use the <code>firstBodyPartName</code> attribute instead")
	public void setInputMessageParam(String inputMessageParam) {
		setFirstBodyPartName(inputMessageParam);
	}
	@IbisDoc({"(Only used when <code>methodType=POST</code> and <code>postType=URLENCODED, FORM-DATA or MTOM</code>) Name of the first body part", ""})
	public void setFirstBodyPartName(String firstBodyPartName) {
		this.firstBodyPartName = firstBodyPartName;
	}
	public String getFirstBodyPartName() {
		return firstBodyPartName;
	}

	@IbisDoc({"When true, besides http status code 200 (OK) also the code 301 (MOVED_PERMANENTLY), 302 (MOVED_TEMPORARILY) and 307 (TEMPORARY_REDIRECT) are considered successful", "false"})
	public void setIgnoreRedirects(boolean b) {
		ignoreRedirects = b;
	}
	public boolean isIgnoreRedirects() {
		return ignoreRedirects;
	}

	@IbisDoc({"if set, the result is streamed to a file (instead of passed as a string)", ""})
	@Deprecated
	@ConfigurationWarning("no longer required to store the result as a file in the PipeLineSession, the sender can return binary data")
	public void setStreamResultToFileNameSessionKey(String string) {
		streamResultToFileNameSessionKey = string;
	}
	public String getStreamResultToFileNameSessionKey() {
		return streamResultToFileNameSessionKey;
	}

	@IbisDoc({"if set, a pointer to an input stream of the result is put in the specified sessionkey (as the sender interface only allows a sender to return a string a sessionkey is used instead to return the stream)", ""})
	@Deprecated
	@ConfigurationWarning("no longer required to store the result as a stream in the PipeLineSession, the sender can return binary data")
	public void setStoreResultAsStreamInSessionKey(String storeResultAsStreamInSessionKey) {
		this.storeResultAsStreamInSessionKey = storeResultAsStreamInSessionKey;
	}
	public String getStoreResultAsStreamInSessionKey() {
		return storeResultAsStreamInSessionKey;
	}

	@Deprecated
	@ConfigurationWarning("no longer required to store the result as a byte array in the PipeLineSession, the sender can return binary data")
	public void setStoreResultAsByteArrayInSessionKey(String storeResultAsByteArrayInSessionKey) {
		this.storeResultAsByteArrayInSessionKey = storeResultAsByteArrayInSessionKey;
	}
	public String getStoreResultAsByteArrayInSessionKey() {
		return storeResultAsByteArrayInSessionKey;
	}

	@IbisDoc({"when true, the result is base64 encoded", "false"})
	@Deprecated
	@ConfigurationWarning("use Base64Pipe instead")
	public void setBase64(boolean b) {
		base64 = b;
	}
	public boolean isBase64() {
		return base64;
	}

	@IbisDoc({"if set, the result is streamed to the httpservletresponse object of the restservicedispatcher (instead of passed as a string)", "false"})
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}
	public boolean isStreamResultToServlet() {
		return streamResultToServlet;
	}

	@Deprecated
	@ConfigurationWarning("multipart has been replaced by postType='formdata'")
	@IbisDoc({"when true and <code>methodetype=post</code> and <code>paramsinurl=false</code>, request parameters are put in a multipart/form-data entity instead of in the request body", "false"})
	public void setMultipart(boolean b) {
		if(b && !postType.equals(PostType.MTOM)) {
			postType = PostType.FORMDATA;
		}
	}

	@Deprecated
	@ConfigurationWarning("Unless set explicitly multipart response will be detected automatically")
	@IbisDoc({"when true the response body is expected to be in mime multipart which is the case when a soap message with attachments is received (see also <a href=\"https://docs.oracle.com/javaee/7/api/javax/xml/soap/soapmessage.html\">https://docs.oracle.com/javaee/7/api/javax/xml/soap/soapmessage.html</a>). the first part will be returned as result of this sender. other parts are returned as streams in sessionkeys with names multipart1, multipart2, etc. the http connection is held open until the last stream is read.", "false"})
	public void setMultipartResponse(Boolean b) {
		multipartResponse = b;
	}
	public Boolean getMultipartResponse() {
		return multipartResponse;
	}

	@IbisDoc({"if set and <code>methodetype=post</code> and <code>paramsinurl=false</code>, a multipart/form-data entity is created instead of a request body. for each part element in the session key a part in the multipart entity is created", ""})
	public void setMultipartXmlSessionKey(String multipartXmlSessionKey) {
		this.multipartXmlSessionKey = multipartXmlSessionKey;
	}

	public String getMultipartXmlSessionKey() {
		return multipartXmlSessionKey;
	}

	@Deprecated
	@ConfigurationWarning("mtomEnabled has been replaced by postType='mtom'")
	public void setMtomEnabled(boolean b) {
		if(b) postType = PostType.MTOM;
	}

	public String getMtomContentTransferEncoding() {
		return mtomContentTransferEncoding;
	}
	public void setMtomContentTransferEncoding(String mtomContentTransferEncoding) {
		this.mtomContentTransferEncoding = mtomContentTransferEncoding;
	}

	@IbisDoc({"64", "specifies whether messages will encoded, e.g. spaces will be replaced by '+' etc.", "false"})
	public void setEncodeMessages(boolean b) {
		encodeMessages = b;
	}
	public boolean isEncodeMessages() {
		return encodeMessages;
	}
}
