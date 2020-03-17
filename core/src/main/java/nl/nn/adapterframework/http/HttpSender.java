/*
   Copyright 2013, 2016-2020 Nationale-Nederlanden

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
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.http.mime.MultipartEntityBuilder;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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

	private String streamResultToFileNameSessionKey = null;
	private String storeResultAsStreamInSessionKey;
	private String storeResultAsByteArrayInSessionKey;
	private boolean base64=false;
	private boolean streamResultToServlet=false;

	private boolean paramsInUrl=true;
	private boolean ignoreRedirects=false;
	private String inputMessageParam=null;

	private boolean multipart=false;
	private boolean multipartResponse=false;
	private String multipartXmlSessionKey;
	private boolean mtomEnabled = false;
	private String mtomContentTransferEncoding = null; //Defaults to 8-bit for normal String messages, 7-bit for e-mails and binary for streams

	@Override
	public void configure() throws ConfigurationException {
		//For backwards compatibility we have to set the contentType to text/html on POST and PUT requests
		if(StringUtils.isEmpty(getContentType()) && (getMethodType().equals("POST") || getMethodType().equals("PUT"))) {
			setContentType("text/html");
		}

		super.configure();

		if (!getMethodType().equals("POST")) {
			if (!isParamsInUrl()) {
				throw new ConfigurationException(getLogPrefix()+"paramsInUrl can only be set to false for methodType POST");
			}
			if (StringUtils.isNotEmpty(getInputMessageParam())) {
				throw new ConfigurationException(getLogPrefix()+"inputMessageParam can only be set for methodType POST");
			}
		}
	}

	@Override
	protected HttpRequestBase getMethod(URIBuilder uri, String message, ParameterValueList parameters, IPipeLineSession session) throws SenderException {
		if(isParamsInUrl())
			return getMethod(uri, message, parameters);
		else
			return getPostMethodWithParamsInBody(uri, message, parameters, session);
	}

	protected HttpRequestBase getMethod(URIBuilder uri, String message, ParameterValueList parameters) throws SenderException {
		try {
			boolean queryParametersAppended = false;

			StringBuffer path = new StringBuffer(uri.getPath());

			if (uri.getQueryParams().size() > 0) {
				path.append("?");
				for (Iterator<NameValuePair> it=uri.getQueryParams().iterator(); it.hasNext(); ) {
					NameValuePair pair = it.next();
					path.append(pair.getName()).append("=").append(pair.getValue());
					if(it.hasNext()) path.append("&");
				}
				queryParametersAppended = true;
			}

			if (getMethodType().equals("GET")) {
				if (parameters!=null) {
					queryParametersAppended = appendParameters(queryParametersAppended,path,parameters);
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"path after appending of parameters ["+path.toString()+"]");
				}
				HttpGet method = new HttpGet(path+(parameters==null? message:""));

				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"HttpSender constructed GET-method ["+method.getURI().getQuery()+"]");
				if (null != getFullContentType()) { //Manually set Content-Type header
					method.setHeader("Content-Type", getFullContentType().toString());
				}
				return method;
			} else if (getMethodType().equals("POST")) {
				HttpPost method = new HttpPost(path.toString());
				if (parameters!=null) {
					StringBuffer msg = new StringBuffer(message);
					appendParameters(true,msg,parameters);
					if (StringUtils.isEmpty(message) && msg.length()>1) {
						message=msg.substring(1);
					} else {
						message=msg.toString();
					}
				}

				HttpEntity entity = new ByteArrayEntity(message.getBytes(getCharSet()), getFullContentType());

				method.setEntity(entity);
				return method;
			}
			if (getMethodType().equals("PUT")) {
				HttpPut method = new HttpPut(path.toString());
				if (parameters!=null) {
					StringBuffer msg = new StringBuffer(message);
					appendParameters(true,msg,parameters);
					if (StringUtils.isEmpty(message) && msg.length()>1) {
						message=msg.substring(1);
					} else {
						message=msg.toString();
					}
				}
				HttpEntity entity = new ByteArrayEntity(message.getBytes(getCharSet()), getFullContentType());
				method.setEntity(entity);
				return method;
			}
			if (getMethodType().equals("DELETE")) {
				HttpDelete method = new HttpDelete(path.toString());
				if (null != getFullContentType()) { //Manually set Content-Type header
					method.setHeader("Content-Type", getFullContentType().toString());
				}
				return method;
			}
			if (getMethodType().equals("HEAD")) {
				HttpHead method = new HttpHead(path.toString());
				return method;
			}

			if (getMethodType().equals("REPORT")) {
				Element element = XmlUtils.buildElement(message, true);
				HttpReport method = new HttpReport(path.toString(), element);
				if (null != getFullContentType()) { //Manually set Content-Type header
					method.setHeader("Content-Type", getFullContentType().toString());
				}
				return method;
			}

			throw new SenderException("unknown methodtype ["+getMethodType()+"], must be either GET, PUT, POST, DELETE, HEAD or REPORT");
		} catch (Exception e) {
			//Catch all exceptions and throw them as SenderException
			throw new SenderException(e);
		}
	}

	protected HttpPost getPostMethodWithParamsInBody(URIBuilder uri, String message, ParameterValueList parameters, IPipeLineSession session) throws SenderException {
		try {
			HttpPost hmethod = new HttpPost(uri.build());

			if (!isMultipart() && StringUtils.isEmpty(getMultipartXmlSessionKey())) {
				List<NameValuePair> requestFormElements = new ArrayList<NameValuePair>();

				if (StringUtils.isNotEmpty(getInputMessageParam())) {
					requestFormElements.add(new BasicNameValuePair(getInputMessageParam(),message));
					log.debug(getLogPrefix()+"appended parameter ["+getInputMessageParam()+"] with value ["+message+"]");
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
			else {
				HttpEntity requestEntity = createMultiPartEntity(message, parameters, session);
				hmethod.setEntity(requestEntity);
			}
		return hmethod;
		} catch (URISyntaxException e) {
			throw new SenderException(getLogPrefix()+"cannot find path from url ["+getUrl()+"]", e);
		}
	}

	protected FormBodyPart createMultipartBodypart(String name, String message) {
		if(isMtomEnabled())
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

	protected HttpEntity createMultiPartEntity(String message, ParameterValueList parameters, IPipeLineSession session) throws SenderException {
		MultipartEntityBuilder entity = MultipartEntityBuilder.create();

		entity.setCharset(Charset.forName(getCharSet()));
		if(isMtomEnabled())
			entity.setMtomMultipart();

		if (StringUtils.isNotEmpty(getInputMessageParam())) {
			entity.addPart(createMultipartBodypart(getInputMessageParam(), message));
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended stringpart ["+getInputMessageParam()+"] with value ["+message+"]");
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
							fileName = (String) session.get(sessionKey + "Name");
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
			String multipartXml = (String) session.get(getMultipartXmlSessionKey());
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

	protected FormBodyPart elementToFormBodyPart(Element element, IPipeLineSession session) {
		String partName = element.getAttribute("name"); //Name of the part
		String partSessionKey = element.getAttribute("sessionKey"); //SessionKey to retrieve data from
		String partMimeType = element.getAttribute("mimeType"); //MimeType of the part
		Object partObject = session.get(partSessionKey);

		if (partObject instanceof InputStream) {
			InputStream fis = (InputStream) partObject;

			return createMultipartBodypart(partSessionKey, fis, partName, partMimeType);
		} else {
			String partValue = (String) session.get(partSessionKey);

			return createMultipartBodypart(partName, partValue, partMimeType);
		}
	}

	@Override
	protected String extractResult(HttpResponseHandler responseHandler, IPipeLineSession session) throws SenderException, IOException {
		int statusCode = responseHandler.getStatusLine().getStatusCode();

		boolean ok = false;
		if (StringUtils.isNotEmpty(getResultStatusCodeSessionKey())) {
			ok = true;
		} else {
			if (statusCode==HttpServletResponse.SC_OK) {
				ok = true;
			} else {
				if (isIgnoreRedirects()) {
					if (statusCode==HttpServletResponse.SC_MOVED_PERMANENTLY || statusCode==HttpServletResponse.SC_MOVED_TEMPORARILY || statusCode==HttpServletResponse.SC_TEMPORARY_REDIRECT) {
						ok = true;
					}
				}
			}
		}

		if (!ok) {
			throw new SenderException(getLogPrefix() + "httpstatus "
					+ statusCode + ": " + responseHandler.getStatusLine().getReasonPhrase()
					+ " body: " + getResponseBodyAsString(responseHandler));
		}

		HttpServletResponse response = null;
		if (isStreamResultToServlet())
			response = (HttpServletResponse) session.get(IPipeLineSession.HTTP_RESPONSE_KEY);

		if (response==null) {
			if (StringUtils.isEmpty(getStreamResultToFileNameSessionKey())) {
				if (isBase64()) {
					return getResponseBodyAsBase64(responseHandler.getResponse());
				} else if (StringUtils.isNotEmpty(getStoreResultAsStreamInSessionKey())) {
					session.put(getStoreResultAsStreamInSessionKey(), responseHandler.getResponse());
					return "";
				} else if (StringUtils.isNotEmpty(getStoreResultAsByteArrayInSessionKey())) {
					session.put(getStoreResultAsByteArrayInSessionKey(), Misc.streamToBytes(responseHandler.getResponse()));
					return "";
				} else if (isMultipartResponse()) {
					return handleMultipartResponse(responseHandler, session);
				} else {
					return getResponseBodyAsString(responseHandler);
				}
			} else {
				String fileName = (String) session.get(getStreamResultToFileNameSessionKey());
				File file = new File(fileName);
				Misc.streamToFile(responseHandler.getResponse(), file);
				return fileName;
			}
		} else {
			streamResponseBody(responseHandler, response);
			return "";
		}
	}

	public String getResponseBodyAsString(HttpResponseHandler responseHandler) throws IOException {
		String charset = responseHandler.getCharset();
		log.debug(getLogPrefix()+"response body uses charset ["+charset+"]");
		if ("HEAD".equals(getMethodType())) {
			XmlBuilder headersXml = new XmlBuilder("headers");
			Header[] headers = responseHandler.getAllHeaders();
			for (Header header : headers) {
				XmlBuilder headerXml = new XmlBuilder("header");
				headerXml.addAttribute("name", header.getName());
				headerXml.setCdataValue(header.getValue());
				headersXml.addSubElement(headerXml);
			}
			return headersXml.toXML();
		}
		String responseBody = responseHandler.getResponseAsString(true);
		if (StringUtils.isEmpty(responseBody)) {
			log.warn(getLogPrefix()+"responseBody is empty");
		} else {
			int rbLength = responseBody.length();
			long rbSizeWarn = Misc.getResponseBodySizeWarnByDefault();
			if (rbLength >= rbSizeWarn) {
				log.warn(getLogPrefix()+"retrieved result size [" +Misc.toFileSize(rbLength)+"] exceeds ["+Misc.toFileSize(rbSizeWarn)+"]");
			}
		}
		return responseBody;
	}

	public String getResponseBodyAsBase64(InputStream is) throws IOException {
		byte[] bytes = Misc.streamToBytes(is);
		if (bytes == null) {
			return null;
		}

		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"base64 encodes response body");
		return Base64.encodeBase64String(bytes);
	}

	public static String handleMultipartResponse(HttpResponseHandler httpHandler, IPipeLineSession session) throws IOException, SenderException {
		return handleMultipartResponse(httpHandler.getContentType().getMimeType(), httpHandler.getResponse(), session, httpHandler);
	}
	public static String handleMultipartResponse(String mimeType, InputStream inputStream, IPipeLineSession session, HttpResponseHandler httpHandler) throws IOException, SenderException {
		String result = null;
		try {
			InputStreamDataSource dataSource = new InputStreamDataSource(mimeType, inputStream);
			MimeMultipart mimeMultipart = new MimeMultipart(dataSource);
			for (int i = 0; i < mimeMultipart.getCount(); i++) {
				BodyPart bodyPart = mimeMultipart.getBodyPart(i);
				boolean lastPart = mimeMultipart.getCount() == i + 1;
				if (i == 0) {
					String charset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
					ContentType contentType = ContentType.parse(bodyPart.getContentType());
					if(contentType.getCharset() != null)
						charset = contentType.getCharset().name();

					InputStream bodyPartInputStream = bodyPart.getInputStream();
					result = Misc.streamToString(bodyPartInputStream, charset);
					if (lastPart) {
						bodyPartInputStream.close();
					}
				} else {
					// When the last stream is read the
					// httpMethod.releaseConnection() can be called, hence pass
					// httpMethod to ReleaseConnectionAfterReadInputStream.
					session.put("multipart" + i, new ReleaseConnectionAfterReadInputStream( lastPart ? httpHandler : null, bodyPart.getInputStream()));
				}
			}
		} catch(MessagingException e) {
			throw new SenderException("Could not read mime multipart response", e);
		}
		return result;
	}

	public void streamResponseBody(HttpResponseHandler responseHandler, HttpServletResponse response) throws IOException {
		streamResponseBody(responseHandler.getResponse(),
				responseHandler.getHeader("Content-Type"),
				responseHandler.getHeader("Content-Disposition"),
				response, log, getLogPrefix());
	}

	public static void streamResponseBody(InputStream is, String contentType,
			String contentDisposition, HttpServletResponse response,
			Logger log, String logPrefix) throws IOException {
		streamResponseBody(is, contentType, contentDisposition, response, log, logPrefix, null);
	}

	public static void streamResponseBody(InputStream is, String contentType,
			String contentDisposition, HttpServletResponse response,
			Logger log, String logPrefix, String redirectLocation) throws IOException {
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
			OutputStream outputStream = response.getOutputStream();
			Misc.streamToStream(is, outputStream);
			outputStream.close();
			log.debug(logPrefix + "copied response body input stream [" + is + "] to output stream [" + outputStream + "]");
		}
	}

	/**
	 * When false and methodeType=POST, request parameters are put in the request body instead of in the url
	 * @IbisDoc.default true
	 */
	@IbisDoc({"when false and <code>methodetype=post</code>, request parameters are put in the request body instead of in the url", "true"})
	public void setParamsInUrl(boolean b) {
		paramsInUrl = b;
	}
	public boolean isParamsInUrl() {
		return paramsInUrl;
	}

	/**
	 * Only used when methodeType=POST and paramsInUrl=false.
	 * Name of the request parameter which is used to put the input message in
	 * @param inputMessageParam
	 */
	@IbisDoc({"(only used when <code>methodetype=post</code> and <code>paramsinurl=false</code>) name of the request parameter which is used to put the input message in", ""})
	public void setInputMessageParam(String inputMessageParam) {
		this.inputMessageParam = inputMessageParam;
	}
	public String getInputMessageParam() {
		return inputMessageParam;
	}

	/**
	 * When true, besides http status code 200 (OK) also the code 301 (MOVED_PERMANENTLY), 302 (MOVED_TEMPORARILY) and 307 (TEMPORARY_REDIRECT) are considered successful
	 */
	@IbisDoc({"when true, besides http status code 200 (ok) also the code 301 (moved_permanently), 302 (moved_temporarily) and 307 (temporary_redirect) are considered successful", "false"})
	public void setIgnoreRedirects(boolean b) {
		ignoreRedirects = b;
	}
	public boolean isIgnoreRedirects() {
		return ignoreRedirects;
	}

	public String getStreamResultToFileNameSessionKey() {
		return streamResultToFileNameSessionKey;
	}

	@IbisDoc({"if set, the result is streamed to a file (instead of passed as a string)", ""})
	public void setStreamResultToFileNameSessionKey(String string) {
		streamResultToFileNameSessionKey = string;
	}

	public String getStoreResultAsStreamInSessionKey() {
		return storeResultAsStreamInSessionKey;
	}

	@IbisDoc({"if set, a pointer to an input stream of the result is put in the specified sessionkey (as the sender interface only allows a sender to return a string a sessionkey is used instead to return the stream)", ""})
	public void setStoreResultAsStreamInSessionKey(String storeResultAsStreamInSessionKey) {
		this.storeResultAsStreamInSessionKey = storeResultAsStreamInSessionKey;
	}

	public String getStoreResultAsByteArrayInSessionKey() {
		return storeResultAsByteArrayInSessionKey;
	}
	public void setStoreResultAsByteArrayInSessionKey(String storeResultAsByteArrayInSessionKey) {
		this.storeResultAsByteArrayInSessionKey = storeResultAsByteArrayInSessionKey;
	}

	@IbisDoc({"when true, the result is base64 encoded", "false"})
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

	@IbisDoc({"when true and <code>methodetype=post</code> and <code>paramsinurl=false</code>, request parameters are put in a multipart/form-data entity instead of in the request body", "false"})
	public void setMultipart(boolean b) {
		multipart = b;
	}
	public boolean isMultipart() {
		return multipart;
	}

	@IbisDoc({"when true the response body is expected to be in mime multipart which is the case when a soap message with attachments is received (see also <a href=\"https://docs.oracle.com/javaee/7/api/javax/xml/soap/soapmessage.html\">https://docs.oracle.com/javaee/7/api/javax/xml/soap/soapmessage.html</a>). the first part will be returned as result of this sender. other parts are returned as streams in sessionkeys with names multipart1, multipart2, etc. the http connection is held open until the last stream is read.", "false"})
	public void setMultipartResponse(boolean b) {
		multipartResponse = b;
	}
	public boolean isMultipartResponse() {
		return multipartResponse;
	}

	@IbisDoc({"if set and <code>methodetype=post</code> and <code>paramsinurl=false</code>, a multipart/form-data entity is created instead of a request body. for each part element in the session key a part in the multipart entity is created", ""})
	public void setMultipartXmlSessionKey(String multipartXmlSessionKey) {
		this.multipartXmlSessionKey = multipartXmlSessionKey;
	}

	public String getMultipartXmlSessionKey() {
		return multipartXmlSessionKey;
	}

	public boolean isMtomEnabled() {
		return mtomEnabled;
	}

	public void setMtomEnabled(boolean mtomEnabled) {
		this.mtomEnabled = mtomEnabled;
	}

	public String getMtomContentTransferEncoding() {
		return mtomContentTransferEncoding;
	}
	public void setMtomContentTransferEncoding(String mtomContentTransferEncoding) {
		this.mtomContentTransferEncoding = mtomContentTransferEncoding;
	}
}