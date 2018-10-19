/*
   Copyright 2013, 2016-2018 Nationale-Nederlanden

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
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.http.mime.MultipartEntityBuilder;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
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
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.http.HttpSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>URL or base of URL to be used </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrlParam(String) urlParam}</td><td>parameter that is used to obtain url; overrides url-attribute.</td><td>url</td></tr>
 * <tr><td>{@link #setMethodType(String) methodType}</td><td>type of method to be executed, either 'GET', 'POST', 'PUT', 'DELETE', 'HEAD' or 'REPORT'</td><td>GET</td></tr>
 * <tr><td>{@link #setContentType(String) contentType}</td><td>content-type of the request, only for POST and PUT methods</td><td>text/html; charset=UTF-8</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>timeout in ms of obtaining a connection/result. 0 means no timeout</td><td>10000</td></tr>
 * <tr><td>{@link #setMaxConnections(int) maxConnections}</td><td>the maximum number of concurrent connections</td><td>10</td></tr>
 * <tr><td>{@link #setMaxExecuteRetries(int) maxExecuteRetries}</td><td>the maximum number of times it the execution is retried</td><td>1</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyHost(String) proxyHost}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) proxyPort}</td><td>&nbsp;</td><td>80</td></tr>
 * <tr><td>{@link #setProxyAuthAlias(String) proxyAuthAlias}</td><td>alias used to obtain credentials for authentication to proxy</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUserName(String) proxyUserName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(String) proxyPassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyRealm(String) proxyRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificate(String) certificate}</td><td>resource URL to certificate to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificateAuthAlias(String) certificateAuthAlias}</td><td>alias used to obtain certificate password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificatePassword(String) certificatePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeystoreType(String) keystoreType}</td><td>&nbsp;</td><td>pkcs12</td></tr>
 * <tr><td>{@link #setKeyManagerAlgorithm(String) keyManagerAlgorithm}</td><td>&nbsp;</td><td></td></tr>
 * <tr><td>{@link #setTruststore(String) truststore}</td><td>resource URL to truststore to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreAuthAlias(String) truststoreAuthAlias}</td><td>alias used to obtain truststore password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststorePassword(String) truststorePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreType(String) truststoreType}</td><td>&nbsp;</td><td>jks</td></tr>
 * <tr><td>{@link #setTrustManagerAlgorithm(String) trustManagerAlgorithm}</td><td>&nbsp;</td><td></td></tr>
 * <tr><td>{@link #setAllowSelfSignedCertificates(boolean) allowSelfSignedCertificates}</td><td>when true, self signed certificates are accepted</td><td>false</td></tr>
 * <tr><td>{@link #setFollowRedirects(boolean) followRedirects}</td><td>when true, a redirect request will be honoured, e.g. to switch to https</td><td>true</td></tr>
 * <tr><td>{@link #setVerifyHostname(boolean) verifyHostname}</td><td>when true, the hostname in the certificate will be checked against the actual hostname</td><td>true</td></tr>
 * <tr><td>{@link #setStaleChecking(boolean) staleChecking}</td><td>controls whether connections checked to be stale, i.e. appear open, but are not.</td><td>true</td></tr>
 * <tr><td>{@link #setEncodeMessages(boolean) encodeMessages}</td><td>specifies whether messages will encoded, e.g. spaces will be replaced by '+' etc.</td><td>false</td></tr>
 * <tr><td>{@link #setParamsInUrl(boolean) paramsInUrl}</td><td>when false and <code>methodeType=POST</code>, request parameters are put in the request body instead of in the url</td><td>true</td></tr>
 * <tr><td>{@link #setInputMessageParam(String) inputMessageParam}</td><td>(only used when <code>methodeType=POST</code> and <code>paramsInUrl=false</code>) name of the request parameter which is used to put the input message in</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHeadersParams(String) headersParams}</td><td>Comma separated list of parameter names which should be set as http headers</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIgnoreRedirects(boolean) ignoreRedirects}</td><td>when true, besides http status code 200 (OK) also the code 301 (MOVED_PERMANENTLY), 302 (MOVED_TEMPORARILY) and 307 (TEMPORARY_REDIRECT) are considered successful</td><td>false</td></tr>
 * <tr><td>{@link #setIgnoreCertificateExpiredException(boolean) ignoreCertificateExpiredException}</td><td>when true, the CertificateExpiredException is ignored</td><td>false</td></tr>
 * <tr><td>{@link #setXhtml(boolean) xhtml}</td><td>when true, the html response is transformed to xhtml</td><td>false</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>>(only used when <code>xhtml=true</code>) stylesheet to apply to the html response</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMultipart(boolean) multipart}</td><td>when true and <code>methodeType=POST</code> and <code>paramsInUrl=false</code>, request parameters are put in a multipart/form-data entity instead of in the request body</td><td>false</td></tr>
 * <tr><td>{@link #setMultipartResponse(boolean) multipartResponse}</td><td>when true the response body is expected to be in mime multipart which is the case when a soap message with attachments is received (see also <a href="https://docs.oracle.com/javaee/7/api/javax/xml/soap/SOAPMessage.html">https://docs.oracle.com/javaee/7/api/javax/xml/soap/SOAPMessage.html</a>). The first part will be returned as result of this sender. Other parts are returned as streams in sessionKeys with names multipart1, multipart2, etc. The http connection is held open until the last stream is read.</td><td>false</td></tr>
 * <tr><td>{@link #setStreamResultToServlet(boolean) streamResultToServlet}</td><td>if set, the result is streamed to the HttpServletResponse object of the RestServiceDispatcher (instead of passed as a String)</td><td>false</td></tr>
 * <tr><td>{@link #setBase64(boolean) base64}</td><td>when true, the result is base64 encoded</td><td>false</td></tr>
 * <tr><td>{@link #setProtocol(String) protocol}</td><td>Secure socket protocol (such as "SSL" and "TLS") to use when a SSLContext object is generated. If empty the protocol "SSL" is used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStreamResultToFileNameSessionKey(String) streamResultToFileNameSessionKey}</td><td>if set, the result is streamed to a file (instead of passed as a String)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultAsStreamInSessionKey(String) storeResultAsStreamInSessionKey}</td><td>if set, a pointer to an input stream of the result is put in the specified sessionKey (as the sender interface only allows a sender to return a string a sessionKey is used instead to return the stream)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResultStatusCodeSessionKey(String) resultStatusCodeSessionKey}</td><td>if set, the status code of the HTTP response is put in specified in the sessionKey and the (error or okay) response message is returned</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMultipartXmlSessionKey(String) multipartXmlSessionKey}</td><td>if set and <code>methodeType=POST</code> and <code>paramsInUrl=false</code>, a multipart/form-data entity is created instead of a request body. For each part element in the session key a part in the multipart entity is created</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
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

public class HttpSender extends HttpSenderBase implements HasPhysicalDestination {

	private String streamResultToFileNameSessionKey = null;
	private String storeResultAsStreamInSessionKey;
	private String storeResultAsByteArrayInSessionKey;
	private boolean base64=false;
	private boolean streamResultToServlet=false;

	private boolean multipart=false;
	private boolean multipartResponse=false;
	private String multipartXmlSessionKey;
	private boolean mtomEnabled = false;
	private String mtomContentTransferEncoding = null;

	public void configure() throws ConfigurationException {
		super.configure();

		if(StringUtils.isEmpty(getContentType()) && !getMethodType().equalsIgnoreCase("POST"))
			setContentType("text/html; charset="+getCharSet());
	}

	protected HttpRequestBase getMethod(URIBuilder uri, String message, ParameterValueList parameters, Map<String, String> headersParamsMap, IPipeLineSession session) throws SenderException {
		if(isParamsInUrl())
			return getMethod(uri, message, parameters, headersParamsMap);
		else
			return getPostMethodWithParamsInBody(uri, message, parameters, headersParamsMap, session);
	}

	protected HttpRequestBase getMethod(URIBuilder uri, String message, ParameterValueList parameters, Map<String, String> headersParamsMap) throws SenderException {
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
					queryParametersAppended = appendParameters(queryParametersAppended,path,parameters,headersParamsMap);
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"path after appending of parameters ["+path.toString()+"]");
				}
				HttpGet method = new HttpGet(path+(parameters==null? message:""));
				for (String param: headersParamsMap.keySet()) {
					method.setHeader(param, headersParamsMap.get(param));
				}
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"HttpSender constructed GET-method ["+method.getURI().getQuery()+"]");
				return method;
			} else if (getMethodType().equals("POST")) {
				HttpPost method = new HttpPost(path.toString());
				if (parameters!=null) {
					StringBuffer msg = new StringBuffer(message);
					appendParameters(true,msg,parameters,headersParamsMap);
					if (StringUtils.isEmpty(message) && msg.length()>1) {
						message=msg.substring(1);
					} else {
						message=msg.toString();
					}
				}
				for (String param: headersParamsMap.keySet()) {
					method.setHeader(param, headersParamsMap.get(param));
				}
				HttpEntity entity = new ByteArrayEntity(message.getBytes(getCharSet()));
				method.setEntity(entity);
				return method;
			}
			if (getMethodType().equals("PUT")) {
				HttpPut method = new HttpPut(path.toString());
				if (parameters!=null) {
					StringBuffer msg = new StringBuffer(message);
					appendParameters(true,msg,parameters,headersParamsMap);
					if (StringUtils.isEmpty(message) && msg.length()>1) {
						message=msg.substring(1);
					} else {
						message=msg.toString();
					}
				}
				HttpEntity entity = new ByteArrayEntity(message.getBytes(getCharSet()));
				method.setEntity(entity);
				return method;
			}
			if (getMethodType().equals("DELETE")) {
				HttpDelete method = new HttpDelete(path.toString());
				return method;
			}
			if (getMethodType().equals("HEAD")) {
				HttpHead method = new HttpHead(path.toString());
				return method;
			}

			if (getMethodType().equals("REPORT")) {
				Element element = XmlUtils.buildElement(message, true);
				HttpReport method = new HttpReport(path.toString(), element);
				if (StringUtils.isNotEmpty(getContentType())) {
					method.setHeader("Content-Type", getContentType());
				}
				return method;
			}

			throw new SenderException("unknown methodtype ["+getMethodType()+"], must be either GET, PUT, POST, DELETE, HEAD or REPORT");
		} catch (Exception e) {
			//Catch all exceptions and throw them as SenderException
			throw new SenderException(e);
		}
	}

	protected HttpPost getPostMethodWithParamsInBody(URIBuilder uri, String message, ParameterValueList parameters, Map<String, String> headersParamsMap, IPipeLineSession session) throws SenderException {
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
						if (headersParamsMap.keySet().contains(name)) {
							hmethod.addHeader(name,value);
							if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended header ["+name+"] with value ["+value+"]");
						} else {
							requestFormElements.add(new BasicNameValuePair(name,value));
							if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended parameter ["+name+"] with value ["+value+"]");
						}
					}
				}
				try {
					hmethod.setEntity(new UrlEncodedFormEntity(requestFormElements));
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
			return createMultipartBodypart(name, message, ContentType.DEFAULT_TEXT.getMimeType());
	}

	protected FormBodyPart createMultipartBodypart(String name, String message, String contentType) {
		FormBodyPartBuilder bodyPart = FormBodyPartBuilder.create()
			.setName(name)
			.setBody(new StringBody(message, ContentType.create(contentType, getCharSet())));

		if (StringUtils.isNotEmpty(getMtomContentTransferEncoding()))
			bodyPart.setField(MIME.CONTENT_TRANSFER_ENC, getMtomContentTransferEncoding());

		return bodyPart.build();
	}

	protected FormBodyPart createMultipartBodypart(String name, InputStream is, String fileName) {
		return createMultipartBodypart(name, is, fileName, ContentType.APPLICATION_OCTET_STREAM.getMimeType());
	}

	protected FormBodyPart createMultipartBodypart(String name, InputStream is, String fileName, String contentType) {
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
			log.debug(getLogPrefix()+"appended stringpart ["+getInputMessageParam()+"] with value ["+message+"]");
		}
		if (parameters!=null) {
			for(int i=0; i<parameters.size(); i++) {
				ParameterValue pv = parameters.getParameterValue(i);
				String paramType = pv.getDefinition().getType();
				String name = pv.getDefinition().getName();

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
						log.debug(getLogPrefix()+"appended filepart ["+name+"] with value ["+value+"] and name ["+fileName+"]");
					} else {
						throw new SenderException(getLogPrefix()+"unknown inputstream ["+value.getClass()+"] for parameter ["+name+"]");
					}
				} else {
					String value = pv.asStringValue("");
					entity.addPart(createMultipartBodypart(name, value));
					log.debug(getLogPrefix()+"appended stringpart ["+name+"] with value ["+value+"]");
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
						//String partType = partElement.getAttribute("type");
						String partName = partElement.getAttribute("name");
						String partSessionKey = partElement.getAttribute("sessionKey");
						String partMimeType = partElement.getAttribute("mimeType");
						Object partObject = session.get(partSessionKey);
						if (partObject instanceof InputStream) {
							InputStream fis = (InputStream) partObject;

							entity.addPart(createMultipartBodypart(partSessionKey, fis, partName, partMimeType));
							log.debug(getLogPrefix()+"appended filepart ["+partSessionKey+"] with value ["+partObject+"] and name ["+partName+"]");
						} else {
							String partValue = (String) session.get(partSessionKey);

							entity.addPart(createMultipartBodypart(partName, partValue, partMimeType));
							log.debug(getLogPrefix()+"appended stringpart ["+partSessionKey+"] with value ["+partValue+"]");
						}
					}
				}
			}
		}

		return entity.build();
	}

	protected String extractResult(HttpResponseHandler responseHandler, ParameterResolutionContext prc) throws SenderException, IOException {
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
			response = (HttpServletResponse) prc.getSession().get(IPipeLineSession.HTTP_RESPONSE_KEY);

		if (response==null) {
			if (StringUtils.isEmpty(getStreamResultToFileNameSessionKey())) {
				if (isBase64()) {
					return getResponseBodyAsBase64(responseHandler.getResponse());
				} else if (StringUtils.isNotEmpty(getStoreResultAsStreamInSessionKey())) {
					prc.getSession().put(getStoreResultAsStreamInSessionKey(), responseHandler.getResponse());
					return "";
				} else if (StringUtils.isNotEmpty(getStoreResultAsByteArrayInSessionKey())) {
					prc.getSession().put(getStoreResultAsByteArrayInSessionKey(), Misc.streamToBytes(responseHandler.getResponse()));
					return "";
				} else if (isMultipartResponse()) {
					return handleMultipartResponse(responseHandler, prc);
				} else {
					return getResponseBodyAsString(responseHandler);
				}
			} else {
				String fileName = (String) prc.getSession().get(getStreamResultToFileNameSessionKey());
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
		int rbLength = responseBody.length();
		long rbSizeWarn = Misc.getResponseBodySizeWarnByDefault();
		if (rbLength >= rbSizeWarn) {
			log.warn(getLogPrefix()+"retrieved result size [" +Misc.toFileSize(rbLength)+"] exceeds ["+Misc.toFileSize(rbSizeWarn)+"]");
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

	public static String handleMultipartResponse(HttpResponseHandler httpHandler, ParameterResolutionContext prc) throws IOException, SenderException {
		return handleMultipartResponse(httpHandler.getContentType().getMimeType(), httpHandler.getResponse(), prc, httpHandler);
	}
	public static String handleMultipartResponse(String mimeType, InputStream inputStream, ParameterResolutionContext prc, HttpResponseHandler httpHandler) throws IOException, SenderException {
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
					prc.getSession().put("multipart" + i, new ReleaseConnectionAfterReadInputStream( lastPart ? httpHandler : null, bodyPart.getInputStream()));
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


	public String getStreamResultToFileNameSessionKey() {
		return streamResultToFileNameSessionKey;
	}
	public void setStreamResultToFileNameSessionKey(String string) {
		streamResultToFileNameSessionKey = string;
	}

	public String getStoreResultAsStreamInSessionKey() {
		return storeResultAsStreamInSessionKey;
	}
	public void setStoreResultAsStreamInSessionKey(String storeResultAsStreamInSessionKey) {
		this.storeResultAsStreamInSessionKey = storeResultAsStreamInSessionKey;
	}

	public String getStoreResultAsByteArrayInSessionKey() {
		return storeResultAsByteArrayInSessionKey;
	}
	public void setStoreResultAsByteArrayInSessionKey(String storeResultAsByteArrayInSessionKey) {
		this.storeResultAsByteArrayInSessionKey = storeResultAsByteArrayInSessionKey;
	}

	public void setBase64(boolean b) {
		base64 = b;
	}
	public boolean isBase64() {
		return base64;
	}

	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}
	public boolean isStreamResultToServlet() {
		return streamResultToServlet;
	}

	public void setMultipart(boolean b) {
		multipart = b;
	}
	public boolean isMultipart() {
		return multipart;
	}

	public void setMultipartResponse(boolean b) {
		multipartResponse = b;
	}
	public boolean isMultipartResponse() {
		return multipartResponse;
	}

	public String getMultipartXmlSessionKey() {
		return multipartXmlSessionKey;
	}
	public void setMultipartXmlSessionKey(String multipartXmlSessionKey) {
		this.multipartXmlSessionKey = multipartXmlSessionKey;
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