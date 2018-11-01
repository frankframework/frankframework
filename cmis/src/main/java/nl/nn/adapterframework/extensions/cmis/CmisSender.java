/*
   Copyright 2016 - 2018 Nationale-Nederlanden

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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.extensions.cmis.server.CmisServletDispatcher;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.pipes.PipeAware;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.chemistry.opencmis.client.SessionParameterMap;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Sender to obtain information from and write to a CMIS application.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.extensions.cmis.CmisSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>AtomPub service document URL</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRepository(String) repository}</td><td>Repository id</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAction(String) action}</td><td>specifies action to perform. Must be one of 
 * <ul>
 * <li><code>get</code>: get the content of a document (and optional the properties)</li>
 * <li><code>create</code>: create a document</li>
 * <li><code>find</code>: perform a query that returns properties</li>
 * <li><code>update</code>: update the properties of an existing document</li>
 * <li><code>fetch</code>: get the (meta)data of a folder or document</li>
 * </ul></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBindingType(String) bindingType}</td><td>"atompub", "webservices" or "browser"</td><td>"atompub"</td></tr>
 * <tr><td>{@link #setFileNameSessionKey(String) fileNameSessionKey}</td><td>(only used when <code>action=create</code>) The session key that contains the name of the file to use. If not set, the value of the property <code>fileName</code> from the input message is used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileInputStreamSessionKey(String) fileInputStreamSessionKey}</td><td>When <code>action=create</code>: the session key that contains the input stream of the file to use. When <code>action=get</code> and <code>getProperties=true</code>: the session key in which the input stream of the document is stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileContentSessionKey(String) fileContentSessionKey}</td><td>When <code>action=create</code>: the session key that contains the base64 encoded content of the file to use. When <code>action=get</code> and <code>getProperties=true</code>: the session key in which the base64 encoded content of the document is stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultMediaType(String) defaultMediaType}</td><td>(only used when <code>action=create</code>) The MIME type used to store the document when it's not set in the input message by a property</td><td>"application/octet-stream"</td></tr>
 * <tr><td>{@link #setStreamResultToServlet(boolean) streamResultToServlet}</td><td>(only used when <code>action=get</code>) if true, the content of the document is streamed to the HttpServletResponse object of the RestServiceDispatcher (instead of passed as a String)</td><td>false</td></tr>
 * <tr><td>{@link #setGetProperties(boolean) getProperties}</td><td>(only used when <code>action=get</code>) if true, the content of the document is streamed to <code>fileInputStreamSessionKey</code> and all document properties are put in the result as a xml string</td><td>false</td></tr>
 * <tr><td>{@link #setUseRootFolder(boolean) useRootFolder}</td><td>(only used when <code>action=create</code>) if true, the document is created in the root folder of the repository. Otherwise the document is created in the repository</td><td>true</td></tr>
 * <tr><td>{@link #setResultOnNotFound(String) resultOnNotFound}</td><td>(only used when <code>action=get</code>) result returned when no document was found for the given id (e.g. "[NOT_FOUND]"). If empty an exception is thrown</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeepSession(boolean) keepSession}</td><td>if true, the session is not closed at the end and it will be used in the next call</td><td>true</td></tr>
 * 
 * <tr><td>{@link #setCertificateUrl(String) certificate}</td><td>resource URL to certificate to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificateAuthAlias(String) certificateAuthAlias}</td><td>alias used to obtain certificate password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificatePassword(String) certificatePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeystoreType(String) keystoreType}</td><td>&nbsp;</td><td>pkcs12</td></tr>
 * <tr><td>{@link #setKeyManagerAlgorithm(String) keyManagerAlgorithm}</td><td>&nbsp;</td><td>PKIX</td></tr>
 * <tr><td>{@link #setTruststore(String) truststore}</td><td>resource URL to truststore to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreAuthAlias(String) truststoreAuthAlias}</td><td>alias used to obtain truststore password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststorePassword(String) truststorePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreType(String) truststoreType}</td><td>&nbsp;</td><td>jks</td></tr>
 * <tr><td>{@link #setTrustManagerAlgorithm(String) trustManagerAlgorithm}</td><td>&nbsp;</td><td>PKIX</td></tr>
 * <tr><td>{@link #setAllowSelfSignedCertificates(boolean) allowSelfSignedCertificates}</td><td>when true, self signed certificates are accepted</td><td>false</td></tr>
 * <tr><td>{@link #setVerifyHostname(boolean) verifyHostname}</td><td>when true, the hostname in the certificate will be checked against the actual hostname</td><td>true</td></tr>
 * <tr><td>{@link #setIgnoreCertificateExpiredException(boolean) ignoreCertificateExpiredException}</td><td>when true, the CertificateExpiredException is ignored</td><td>false</td></tr>
 * 
 * <tr><td>{@link #setProxyHost(String) proxyHost}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) proxyPort}</td><td>&nbsp;</td><td>80</td></tr>
 * <tr><td>{@link #setProxyAuthAlias(String) proxyAuthAlias}</td><td>alias used to obtain credentials for authentication to proxy</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUserName(String) proxyUserName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(String) proxyPassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * 
 * <tr><td>{@link #setBridgeSender(boolean) isBridgeSender}</td><td>when a cmisListener is used, this specifies where non-bypassed requests should be send to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOverrideEntryPointWSDL(String) overrideEntryPointWSDL}</td><td>override entrypoint wsdl by reading it from the classpath, overrides url attribute</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>NOTE:</b></p>
 * <p>Only one CmisSender can act as the default 'bridged' sender!</p>
 * <p>When used to proxy requests, you should use the fetch action!</p>
 * <p></p>
 * 
 * <p>
 * <table border="1">
 * <b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>authAlias</td><td>string</td><td>When a parameter with name authAlias is present, it is used instead of the authAlias specified by the attribute</td></tr>
 * <tr><td>userName</td><td>string</td><td>When a parameter with name userName is present, it is used instead of the userName specified by the attribute</td></tr>
 * <tr><td>password</td><td>string</td><td>When a parameter with name password is present, it is used instead of the password specified by the attribute</td></tr>
 * </table>
 * </p>
 * <p><b>NOTE:</b></p>
 * <p>These parameters are incompatible when the sender is configured as a BridgeSender!</p>
 * <p></p>
 * 
 * <p>
 * When <code>action=get</code> the input (xml string) indicates the id of the document to get. This input is mandatory.
 * </p>
 * <p>
 * <b>example:</b>
 * <code>
 * <pre>
 *   &lt;cmis&gt;
 *      &lt;id&gt;
 *         documentId
 *      &lt;/id&gt;
 *   &lt;/cmis&gt;
 * </pre>
 * </code>
 * </p>
 * <p>
 * When <code>action=create</code> the input (xml string) indicates document properties to set. This input is optional.
 * </p>
 * <p>
 * <b>example:</b>
 * <code>
 * <pre>
 *   &lt;cmis&gt;
 *      &lt;name&gt;Offerte&lt;/name&gt;
 *      &lt;objectTypeId&gt;NNB_Geldlening&lt;/objectTypeId&gt;
 *      &lt;mediaType&gt;application/pdf&lt;/mediaType&gt;
 *      &lt;properties&gt;
 *         &lt;property name="ArrivedAt" type="datetime" formatString="yyyy-MM-dd'T'HH:mm:ss.SSSz"&gt;2014-11-27T16:43:01.268+0100&lt;/property&gt;
 *         &lt;property name="ArrivedBy"&gt;HDN&lt;/property&gt;
 *         &lt;property name="DocumentType"&gt;Geldlening&lt;/property&gt;
 *      &lt;/properties&gt;
 *   &lt;/cmis&gt;
 * </pre>
 * </code>
 * </p>
 * 
 * <p>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>name</td><td>mandatory property "cmis:name". If not set the sender attribute fileNameSessionKey is used</td><td>"[unknown]"</td></tr>
 * <tr><td>objectTypeId</td><td>mandatory property "cmis:objectTypeId"</td><td>"cmis:document"</td></tr>
 * <tr><td>mediaType</td><td>the MIME type of the document to store</td><td>"application/octet-stream"</td></tr>
 * <tr><td>property</td><td>custom document property to set. Possible attributes:
 * <table border="1">
 * <tr><th>name</th><th>description</th><th>default</th></tr>
 * <tr><td>type</td><td>
 * <ul>
 * <li><code>string</code>: renders the value</li>
 * <li><code>datetime</code>: converts the value to a Date, by default using formatString <code>yyyy-MM-dd HH:mm:ss</code></li>
 * </ul>
 * </td><td>string</td></tr>
 * <tr><td>formatString</td><td>used in combination with <code>datetime</code></td><td>yyyy-MM-dd HH:mm:ss</td></tr>
 * </table></td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p>
 * When <code>action=find</code> the input (xml string) indicates the query to perform.
 * </p>
 * <p>
 * <b>example:</b>
 * <code>
 * <pre>
 *   &lt;query&gt;
 *      &lt;statement&gt;select * from cmis:document&lt;/statement&gt;
 *      &lt;maxItems&gt;10&lt;/maxItems&gt;
 *      &lt;skipCount&gt;0&lt;/skipCount&gt;
 *      &lt;searchAllVersions&gt;true&lt;/searchAllVersions&gt;
 *      &lt;includeAllowableActions&gt;true&lt;/includeAllowableActions&gt;
 *   &lt;/query&gt;
 * </pre>
 * </code>
 * </p>
 * <p>
 * When <code>action=update</code> the input (xml string) indicates document properties to update.
 * </p>
 * <p>
 * <b>example:</b>
 * <code>
 * <pre>
 *   &lt;cmis&gt;
 *      &lt;id&gt;123456789&lt;/id&gt;
 *      &lt;properties&gt;
 *         &lt;property name="ArrivedAt" type="datetime" formatString="yyyy-MM-dd'T'HH:mm:ss.SSSz"&gt;2014-11-27T16:43:01.268+0100&lt;/property&gt;
 *         &lt;property name="ArrivedBy"&gt;HDN&lt;/property&gt;
 *         &lt;property name="DocumentType"&gt;Geldlening&lt;/property&gt;
 *      &lt;/properties&gt;
 *   &lt;/cmis&gt;
 * </pre>
 * </code>
 * </p>
 * 
 * <p>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>id</td><td>mandatory property "cmis:objectId" which indicates the document to update</td><td>&nbsp;</td></tr>
 * <tr><td>property</td><td>custom document property to update. See <code>action=create</code> for possible attributes</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *
 * @author	Peter Leeuwenburgh
 * @author	Niels Meijer
 */
public class CmisSender extends SenderWithParametersBase implements PipeAware {

	private String action;
	private String url;
	private String repository;
	private String authAlias;
	private String userName;
	private String password;
	private String bindingType = "atompub";
	private String fileNameSessionKey;
	private String fileInputStreamSessionKey;
	private String fileContentStreamSessionKey;
	private String defaultMediaType = "application/octet-stream";
	private boolean streamResultToServlet = false;
	private boolean getProperties = false;
	private boolean useRootFolder = true;
	private String resultOnNotFound;
	private boolean keepSession = true;

	private Session session;

	private boolean allowSelfSignedCertificates = false;
	private boolean verifyHostname = true;
	private boolean ignoreCertificateExpiredException = false;
	private String certificate = null;
	private String certificateAuthAlias = null;
	private String certificatePassword = null;
	private String truststore = null;
	private String truststoreAuthAlias = null;
	private String truststorePassword = null;
	private String keystoreType = "pkcs12";
	private String keyManagerAlgorithm = "PKIX";
	private String truststoreType = "jks";
	private String trustManagerAlgorithm = "PKIX";

	/** PROXY **/
	private String proxyHost;
	private int proxyPort = 80;
	private String proxyAuthAlias;
	private String proxyUserName;
	private String proxyPassword;
	private String proxyRealm = null;

	List<String> actions = Arrays.asList("create", "get", "find", "update", "fetch");
	List<String> bindingTypes = Arrays.asList("atompub", "webservices", "browser");
	private AbstractPipe pipe = null;
	private boolean isBridgeSender = false;

	public final static String FORMATSTRING_BY_DEFAULT = "yyyy-MM-dd HH:mm:ss";

	public final static String OVERRIDE_WSDL_URL = "http://fake.url";
	public final static String OVERRIDE_WSDL_KEY = "override_wsdl_key";
	private String overrideEntryPointWSDL;

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getUrl()) && getOverrideEntryPointWSDL() == null) {
			throw new ConfigurationException("CmisSender [" + getName()
					+ "] has no url configured");
		}
		if (StringUtils.isEmpty(getRepository())) {
			throw new ConfigurationException("CmisSender [" + getName()
					+ "] has no repository configured");
		}
		if (!bindingTypes.contains(getBindingType())) {
			throw new ConfigurationException("illegal value for bindingType ["
					+ getBindingType() + "], must be " + bindingTypes.toString());
		}
		if(getOverrideEntryPointWSDL() != null && !"webservices".equalsIgnoreCase(getBindingType())) {
			throw new ConfigurationException("illegal value for bindingtype ["
					+ getBindingType() + "], overrideEntryPointWSDL only supports webservices");
		}
		if (!actions.contains(getAction())) {
			throw new ConfigurationException("illegal value for action ["
					+ getAction() + "], must be " + actions.toString());
		}
		if (getAction().equalsIgnoreCase("create")) {
			if (StringUtils.isEmpty(getFileInputStreamSessionKey())
					&& StringUtils.isEmpty(getFileContentSessionKey())) {
				throw new ConfigurationException(
						"fileInputStreamSessionKey or fileContentSessionKey should be specified");
			}
		}
		if (getAction().equalsIgnoreCase("get")) {
			if (isGetProperties()) {
				if (StringUtils.isEmpty(getFileInputStreamSessionKey())
						&& StringUtils.isEmpty(getFileContentSessionKey())) {
					throw new ConfigurationException(
							"fileInputStreamSessionKey or fileContentSessionKey should be specified");
				}
			}
		}

		if(isBridgeSender()) {
			CmisServletDispatcher.getInstance().registerServiceClient(this);
		}
	}

	public Session getSession() throws SenderException {
		if (session == null || !isKeepSession()) {
			CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUserName(), getPassword());
			session = connect(cf.getUsername(), cf.getPassword());
		}
		return session;
	}

	public Session getSession(ParameterResolutionContext prc) throws SenderException {
		if (session == null || !isKeepSession()) {
			String authAlias_work = null;
			String userName_work = null;
			String password_work = null;

			ParameterValueList pvl = null;
			try {
				if (prc != null && paramList != null) {
					pvl = prc.getValues(paramList);
					if (pvl != null) {
						ParameterValue pv = pvl.getParameterValue("authAlias");
						if (pv != null) {
							authAlias_work = (String) pv.getValue();
						}
						pv = pvl.getParameterValue("userName");
						if (pv != null) {
							userName_work = (String) pv.getValue();
						}
						pv = pvl.getParameterValue("password");
						if (pv != null) {
							password_work = (String) pv.getValue();
						}
					}
				}
			} catch (ParameterException e) {
				throw new SenderException(getLogPrefix() + "Sender [" + getName() + "] caught exception evaluating parameters", e);
			}
	
			if (authAlias_work == null) {
				authAlias_work = getAuthAlias();
			}
			if (userName_work == null) {
				userName_work = getUserName();
			}
			if (password_work == null) {
				password_work = getPassword();
			}
	
			CredentialFactory cf = new CredentialFactory(authAlias_work, userName_work, password_work);
			session = connect(cf.getUsername(), cf.getPassword());
		}
		return session;
	}

	public void open() throws SenderException {
		/*
		 * possible workaround to avoid
		 * "CWPST0164E: The /opt/WAS/7.0/profiles/AppSrv01/config/cells/IUFjava_Shared_HA_AS_V4.3/applications/null.ear/deployments/deployment.xml composition unit is not found."
		 * if (session == null) { session = connect(); }
		 */
	}

	public void close() throws SenderException {
		if (session != null) {
			session.clear();
			session = null;
		}
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		try {
			session = getSession(prc);

			if (getAction().equalsIgnoreCase("get")) {
				return sendMessageForActionGet(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("create")) {
				return sendMessageForActionCreate(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("find")) {
				return sendMessageForActionFind(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("update")) {
				return sendMessageForActionUpdate(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("fetch")) {
				return sendMessageForActionFetch(correlationID, message, prc);
			} else {
				throw new SenderException(getLogPrefix() + "unknown action ["
						+ getAction() + "]");
			}
		} finally {
			if (session != null && !isKeepSession()) {
				session.clear();
				session = null;
			}
		}
	}

	private String sendMessageForActionGet(String correlationID,
			String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		if (StringUtils.isEmpty(message)) {
			throw new SenderException(getLogPrefix() + "input string cannot be empty but must contain a documentId");
		}

		CmisObject object = null;
		try {
			object = getCmisObject(message);
		} catch (CmisObjectNotFoundException e) {
			if (StringUtils.isNotEmpty(getResultOnNotFound())) {
				log.info(getLogPrefix() + "document with id [" + message
						+ "] not found", e);
				return getResultOnNotFound();
			} else {
				throw new SenderException(e);
			}
		}

		Document document = (Document) object;
		ContentStream contentStream = document.getContentStream();

		try {
			InputStream inputStream = contentStream.getStream();
			if (isStreamResultToServlet()) {
				HttpServletResponse response = (HttpServletResponse) prc
						.getSession().get(IPipeLineSession.HTTP_RESPONSE_KEY);
				String contentType = contentStream.getMimeType();
				if (StringUtils.isNotEmpty(contentType)) {
					log.debug(getLogPrefix()
							+ "setting response Content-Type header ["
							+ contentType + "]");
					response.setHeader("Content-Type", contentType);
				}
				String contentDisposition = "attachment; filename=\""
						+ contentStream.getFileName() + "\"";
				log.debug(getLogPrefix()
						+ "setting response Content-Disposition header ["
						+ contentDisposition + "]");
				response.setHeader("Content-Disposition", contentDisposition);
				OutputStream outputStream;
				outputStream = response.getOutputStream();
				Misc.streamToStream(inputStream, outputStream);
				log.debug(getLogPrefix()
						+ "copied document content input stream ["
						+ inputStream + "] to output stream [" + outputStream
						+ "]");
				return "";
			} else if (isGetProperties()) {
				if (StringUtils.isNotEmpty(fileInputStreamSessionKey)) {
					prc.getSession().put(getFileInputStreamSessionKey(),
							inputStream);
				} else {
					byte[] bytes = Misc.streamToBytes(inputStream);
					prc.getSession().put(getFileContentSessionKey(),
							Base64.encodeBase64String(bytes));
				}

				XmlBuilder cmisXml = new XmlBuilder("cmis");
				XmlBuilder propertiesXml = new XmlBuilder("properties");
				for (Iterator<Property<?>> it = document.getProperties().iterator(); it.hasNext();) {
					Property<?> property = (Property<?>) it.next();
					propertiesXml.addSubElement(getPropertyXml(property));
				}
				cmisXml.addSubElement(propertiesXml);

				return cmisXml.toXML();
			} else {
				return Misc.streamToString(inputStream, null, false);
			}
		} catch (IOException e) {
			throw new SenderException(e);
		}
	}

	private XmlBuilder getPropertyXml(PropertyData<?> property) {
		XmlBuilder propertyXml = new XmlBuilder("property");
		String name = property.getId();
		propertyXml.addAttribute("name", name);
		Object value = property.getFirstValue();

		if (value == null) {
			propertyXml.addAttribute("isNull", "true");
		}
		if (value instanceof BigInteger) {
			BigInteger bi = (BigInteger) property.getFirstValue();
			propertyXml.setValue(String.valueOf(bi));
			propertyXml.addAttribute("type", "integer");
		} else if (value instanceof Boolean) {
			Boolean b = (Boolean) property.getFirstValue();
			propertyXml.setValue(String.valueOf(b));
			propertyXml.addAttribute("type", "boolean");
		} else if (value instanceof GregorianCalendar) {
			GregorianCalendar gc = (GregorianCalendar) property.getFirstValue();
			//TODO shouldn't this be "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
			SimpleDateFormat sdf = new SimpleDateFormat(FORMATSTRING_BY_DEFAULT);
			propertyXml.setValue(sdf.format(gc.getTime()));
			propertyXml.addAttribute("type", "datetime");
		} else {
			propertyXml.setValue((String) property.getFirstValue());
		}
		return propertyXml;
	}

	private String sendMessageForActionCreate(String correlationID,
			String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		String fileName = (String) prc.getSession()
				.get(getFileNameSessionKey());

		InputStream inputStream = null;
		if (StringUtils.isNotEmpty(fileInputStreamSessionKey)) {
			inputStream = (FileInputStream) prc.getSession().get(
					getFileInputStreamSessionKey());
		} else {
			String fileContent = (String) prc.getSession().get(
					getFileContentSessionKey());
			byte[] bytes = Base64.decodeBase64((String) fileContent);
			inputStream = new ByteArrayInputStream(bytes);
		}
		long fileLength = 0;
		try {
			fileLength = inputStream.available();
		} catch (IOException e) {
			log.warn(getLogPrefix() + "could not determine file size", e);
		}

		String mediaType;
		Map<String, Object> props = new HashMap<String, Object>();
		Element cmisElement;
		try {
			if (XmlUtils.isWellFormed(message, "cmis")) {
				cmisElement = XmlUtils.buildElement(message);
			} else {
				cmisElement = XmlUtils.buildElement("<cmis/>");
			}

			String objectTypeId = XmlUtils.getChildTagAsString(cmisElement,
					"objectTypeId");
			if (StringUtils.isNotEmpty(objectTypeId)) {
				props.put(PropertyIds.OBJECT_TYPE_ID, objectTypeId);
			} else {
				props.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
			}
			String name = XmlUtils.getChildTagAsString(cmisElement, "name");
			if (StringUtils.isEmpty(fileName)) {
				fileName = XmlUtils
						.getChildTagAsString(cmisElement, "fileName");
			}
			mediaType = XmlUtils.getChildTagAsString(cmisElement, "mediaType");
			if (StringUtils.isNotEmpty(name)) {
				props.put(PropertyIds.NAME, name);
			} else if (StringUtils.isNotEmpty(fileName)) {
				props.put(PropertyIds.NAME, fileName);
			} else {
				props.put(PropertyIds.NAME, "[unknown]");
			}
			Element propertiesElement = XmlUtils.getFirstChildTag(cmisElement,
					"properties");
			if (propertiesElement != null) {
				processProperties(propertiesElement, props);
			}
		} catch (DomBuilderException e) {
			throw new SenderException(getLogPrefix() + "exception parsing ["
					+ message + "]", e);
		}

		if (StringUtils.isEmpty(mediaType)) {
			mediaType = getDefaultMediaType();
		}

		ContentStream contentStream = session.getObjectFactory().createContentStream(fileName, fileLength, mediaType, inputStream);
		if (isUseRootFolder()) {
			Folder folder = session.getRootFolder();
			Document document = folder.createDocument(props, contentStream, VersioningState.NONE);
			log.debug(getLogPrefix() + "created new document [ " + document.getId() + "]");
			return document.getId();
		} else {
			ObjectId objectId = session.createDocument(props, null, contentStream, VersioningState.NONE);
			log.debug(getLogPrefix() + "created new document [ " + objectId.getId() + "]");
			return objectId.getId();
		}
	}

	private void processProperties(Element propertiesElement, Map<String, Object> props) throws SenderException {
		Iterator<Node> iter = XmlUtils.getChildTags(propertiesElement, "property").iterator();

		while (iter.hasNext()) {
			Element propertyElement = (Element) iter.next();
			String property = XmlUtils.getStringValue(propertyElement);
			if (StringUtils.isNotEmpty(property)) {
				String nameAttr = propertyElement.getAttribute("name");
				String typeAttr = propertyElement.getAttribute("type");

				if (StringUtils.isEmpty(typeAttr) || typeAttr.equalsIgnoreCase("string")) {
					props.put(nameAttr, property);
				} else if (typeAttr.equalsIgnoreCase("integer")) {
					props.put(nameAttr, new BigInteger(property));
				} else if (typeAttr.equalsIgnoreCase("boolean")) {
					props.put(nameAttr, Boolean.parseBoolean(property));
				} else if (typeAttr.equalsIgnoreCase("datetime")) {
					String formatStringAttr = propertyElement.getAttribute("formatString");
					if (StringUtils.isEmpty(formatStringAttr)) {
						formatStringAttr = FORMATSTRING_BY_DEFAULT;
					}
					DateFormat df = new SimpleDateFormat(formatStringAttr);
					Date date;
					try {
						date = df.parse(property);
					} catch (ParseException e) {
						throw new SenderException(getLogPrefix()
								+ "exception parsing date [" + property
								+ "] using formatString [" + formatStringAttr
								+ "]", e);
					}
					props.put(nameAttr, date);
				} else {
					log.warn(getLogPrefix() + "unknown type [" + typeAttr
							+ "], assuming 'string'");
					props.put(nameAttr, property);
				}
				if (log.isDebugEnabled()) {
					log.debug(getLogPrefix() + "set property name [" + nameAttr
							+ "] value [" + property + "]");
				}
			} else {
				log.debug(getLogPrefix() + "empty property found, ignoring");
			}
		}
	}
	
	private String sendMessageForActionFind(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		Element queryElement = null;
		try {
			if (XmlUtils.isWellFormed(message, "query")) {
				queryElement = XmlUtils.buildElement(message);
			} else {
				queryElement = XmlUtils.buildElement("<query/>");
			}
		} catch (DomBuilderException e) {
			throw new SenderException(e);
		}
		String statement = XmlUtils.getChildTagAsString(queryElement, "statement");
		String maxItems = XmlUtils.getChildTagAsString(queryElement, "maxItems");
		String skipCount = XmlUtils.getChildTagAsString(queryElement, "skipCount");
		String searchAllVersions = XmlUtils.getChildTagAsString(queryElement, "searchAllVersions");

		String includeAllowableActions = XmlUtils.getChildTagAsString(queryElement, "includeAllowableActions");

		OperationContext operationContext = session.createOperationContext();
		if (StringUtils.isNotEmpty(maxItems)) {
			operationContext.setMaxItemsPerPage(Integer.parseInt(maxItems));
		}
		boolean sav = false;
		if (StringUtils.isNotEmpty(searchAllVersions)) {
			sav = Boolean.parseBoolean(searchAllVersions);
		}
		if (StringUtils.isNotEmpty(includeAllowableActions)) {
			operationContext.setIncludeAllowableActions(Boolean.parseBoolean(searchAllVersions));
		}
		ItemIterable<QueryResult> q = session.query(statement, sav, operationContext);
		if (StringUtils.isNotEmpty(skipCount)) {
			long sc = Long.parseLong(skipCount);
			q = q.skipTo(sc);
		}
		if (StringUtils.isNotEmpty(maxItems)) {
			q = q.getPage();
		}

		XmlBuilder cmisXml = new XmlBuilder("cmis");
		XmlBuilder rowsetXml = new XmlBuilder("rowset");
		for (QueryResult qResult : q) {
			XmlBuilder rowXml = new XmlBuilder("row");
			for (PropertyData<?> property : qResult.getProperties()) {
				rowXml.addSubElement(getPropertyXml(property));
			}
			rowsetXml.addSubElement(rowXml);
		}
		cmisXml.addSubElement(rowsetXml);
		return cmisXml.toXML();
	}

	private CmisObject getCmisObject(String message) throws SenderException, CmisObjectNotFoundException {
		Element queryElement = null;
		try {
			if (XmlUtils.isWellFormed(message, "cmis")) {
				queryElement = XmlUtils.buildElement(message);
			} else {
				queryElement = XmlUtils.buildElement("<cmis/>");
			}
		} catch (DomBuilderException e) {
			throw new SenderException(e);
		}

		String objectIdstr = XmlUtils.getChildTagAsString(queryElement, "objectId");
		if(objectIdstr == null)
			objectIdstr = XmlUtils.getChildTagAsString(queryElement, "id");

		String filter = XmlUtils.getChildTagAsString(queryElement, "filter");
		boolean includeAllowableActions = XmlUtils.getChildTagAsBoolean(queryElement, "includeAllowableActions");
		boolean includePolicies = XmlUtils.getChildTagAsBoolean(queryElement, "includePolicies");
		boolean includeAcl = XmlUtils.getChildTagAsBoolean(queryElement, "includeAcl");

		OperationContext operationContext = session.createOperationContext();

		if (StringUtils.isNotEmpty(filter))
			operationContext.setFilterString(filter);
		operationContext.setIncludeAllowableActions(includeAllowableActions);
		operationContext.setIncludePolicies(includePolicies);
		operationContext.setIncludeAcls(includeAcl);

		return session.getObject(session.createObjectId(objectIdstr), operationContext);
	}

	private String sendMessageForActionFetch(String correlationID,
			String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {

		CmisObject object = null;
		try {
			object = getCmisObject(message);
		} catch (CmisObjectNotFoundException e) {
			if (StringUtils.isNotEmpty(getResultOnNotFound())) {
				log.info(getLogPrefix() + "document with id [" + message + "] not found", e);
				return getResultOnNotFound();
			} else {
				throw new SenderException(e);
			}
		}

		XmlBuilder cmisXml = new XmlBuilder("cmis");

		XmlBuilder propertiesXml = new XmlBuilder("properties");
		for (Iterator<Property<?>> it = object.getProperties().iterator(); it.hasNext();) {
			Property<?> property = (Property<?>) it.next();
			propertiesXml.addSubElement(getPropertyXml(property));
		}
		cmisXml.addSubElement(propertiesXml);

		XmlBuilder allowableActionsXml = new XmlBuilder("allowableActions");
		Set<Action> actions = object.getAllowableActions().getAllowableActions();
		for (Action action : actions) {
			XmlBuilder actionXml = new XmlBuilder("action");
			actionXml.setValue(action.value());
			allowableActionsXml.addSubElement(actionXml);
		}
		cmisXml.addSubElement(allowableActionsXml);

		XmlBuilder isExactAclXml = new XmlBuilder("isExactAcl");
		if(object.getAcl() != null)
			isExactAclXml.setValue(object.getAcl().isExact().toString());
		cmisXml.addSubElement(isExactAclXml);

		XmlBuilder policiesXml = new XmlBuilder("policyIds");
		List<ObjectId> policies = object.getPolicyIds();
		if(policies != null) {
			for (ObjectId objectId : policies) {
				XmlBuilder policyXml = new XmlBuilder("policyId");
				policyXml.setValue(objectId.getId());
				policiesXml.addSubElement(policyXml);
			}
		}
		cmisXml.addSubElement(policiesXml);

		XmlBuilder relationshipsXml = new XmlBuilder("relationships");
		List<Relationship> relationships = object.getRelationships();
		if(relationships != null) {
			for (Relationship relation : relationships) {
				XmlBuilder policyXml = new XmlBuilder("relation");
				policyXml.setValue(relation.getId());
				relationshipsXml.addSubElement(policyXml);
			}
		}
		cmisXml.addSubElement(relationshipsXml);

		return cmisXml.toXML();
	}

	private String sendMessageForActionUpdate(String correlationID,
			String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		String objectId = null;
		Map<String, Object> props = new HashMap<String, Object>();
		Element cmisElement;
		try {
			if (XmlUtils.isWellFormed(message, "cmis")) {
				cmisElement = XmlUtils.buildElement(message);
			} else {
				cmisElement = XmlUtils.buildElement("<cmis/>");
			}

			objectId = XmlUtils.getChildTagAsString(cmisElement, "id");
			Element propertiesElement = XmlUtils.getFirstChildTag(cmisElement,
					"properties");
			if (propertiesElement != null) {
				processProperties(propertiesElement, props);
			}
		} catch (DomBuilderException e) {
			throw new SenderException(getLogPrefix() + "exception parsing ["
					+ message + "]", e);
		}

		CmisObject object = null;
		try {
			object = session.getObject(session.createObjectId(objectId));
		} catch (CmisObjectNotFoundException e) {
			if (StringUtils.isNotEmpty(getResultOnNotFound())) {
				log.info(getLogPrefix() + "document with id [" + message
						+ "] not found", e);
				return getResultOnNotFound();
			} else {
				throw new SenderException(e);
			}

		}
		object.updateProperties(props);
		return object.getId();
	}

	private Session connect(String userName, String password) throws SenderException {
		log.debug(getLogPrefix() + "connecting with url [" + getUrl() + "] repository [" + getRepository() + "]");

		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
		SessionParameterMap parameterMap = new SessionParameterMap();

		parameterMap.setBasicAuthentication(userName, password);

		if (getBindingType().equalsIgnoreCase("atompub")) {
			parameterMap.setAtomPubBindingUrl(getUrl());
		} else if (getBindingType().equalsIgnoreCase("browser")) {
			parameterMap.setBrowserBindingUrl(getUrl());
		} else {
			// OpenCMIS requires an entrypoint url (wsdl), if this url has been secured and is not publicly accessible,
			// we can manually override this wsdl by reading it from the classpath.
			//TODO: Does this work with any binding type?
			if(getOverrideEntryPointWSDL() != null) {
				URL url = ClassUtils.getResourceURL(getClassLoader(), getOverrideEntryPointWSDL());
				if(url != null) {
					try {
						parameterMap.put(OVERRIDE_WSDL_KEY, Misc.streamToString(url.openStream()));
						//We need to setup a fake URL in order to initialize the CMIS Session
						parameterMap.setWebServicesBindingUrl(OVERRIDE_WSDL_URL);
					} catch (IOException e) {
						throw new SenderException(e);
					}
				}
			}
			else {
				parameterMap.setWebServicesBindingUrl(getUrl());
			}
		}
		parameterMap.setRepositoryId(getRepository());

		//SSL
		if (getCertificate()!=null || getTruststore()!=null || isAllowSelfSignedCertificates()) {
			CredentialFactory certificateCf = new CredentialFactory(getCertificateAuthAlias(), null, getCertificatePassword());
			CredentialFactory truststoreCf  = new CredentialFactory(getTruststoreAuthAlias(),  null, getTruststorePassword());

			parameterMap.put("certificateUrl", getCertificate());
			parameterMap.put("certificatePassword", certificateCf.getPassword());
			parameterMap.put("keystoreType", getKeystoreType());
			parameterMap.put("keyManagerAlgorithm", getKeyManagerAlgorithm());
			parameterMap.put("truststoreUrl", getTruststore());
			parameterMap.put("truststorePassword", truststoreCf.getPassword());
			parameterMap.put("truststoreType", getTruststoreType());
			parameterMap.put("trustManagerAlgorithm", getTrustManagerAlgorithm());
		}

		// SSL+
		parameterMap.put("isAllowSelfSignedCertificates", "" + isAllowSelfSignedCertificates());
		parameterMap.put("isVerifyHostname", "" + isVerifyHostname());
		parameterMap.put("isIgnoreCertificateExpiredException", "" + isIgnoreCertificateExpiredException());

		// PROXY
		if (StringUtils.isNotEmpty(getProxyHost())) {
			CredentialFactory pcf = new CredentialFactory(getProxyAuthAlias(), getProxyUserName(), getProxyPassword());
			parameterMap.put("proxyHost", getProxyHost());
			parameterMap.put("proxyPort", "" + getProxyPort());
			parameterMap.put("proxyUserName", pcf.getUsername());
			parameterMap.put("proxyPassword", pcf.getPassword());
		}

		// Custom IBIS HttpSender to support ssl connections and proxies
		parameterMap.setHttpInvoker(nl.nn.adapterframework.extensions.cmis.CmisHttpInvoker.class);

		Session session = sessionFactory.createSession(parameterMap);
		log.debug(getLogPrefix() + "connected with repository [" + getRepositoryInfo(session) + "]");
		return session;
	}

	public void setOverrideEntryPointWSDL(String overrideEntryPointWSDL) {
		if(!overrideEntryPointWSDL.isEmpty())
			this.overrideEntryPointWSDL = overrideEntryPointWSDL;
	}

	public String getOverrideEntryPointWSDL() {
		// never return an empty string, always null!
		return overrideEntryPointWSDL;
	}

	public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		this.allowSelfSignedCertificates = allowSelfSignedCertificates;
	}

	public boolean isAllowSelfSignedCertificates() {
		return allowSelfSignedCertificates;
	}

	public void setVerifyHostname(boolean verifyHostname) {
		this.verifyHostname = verifyHostname;
	}

	public boolean isVerifyHostname() {
		return verifyHostname;
	}

	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		this.ignoreCertificateExpiredException = ignoreCertificateExpiredException;
	}

	public boolean isIgnoreCertificateExpiredException() {
		return ignoreCertificateExpiredException;
	}

	public void setCertificateUrl(String certificate) {
		this.certificate = certificate;
	}

	public String getCertificate() {
		return certificate;
	}

	public void setCertificateAuthAlias(String certificateAuthAlias) {
		this.certificateAuthAlias = certificateAuthAlias;
	}

	public String getCertificateAuthAlias() {
		return certificateAuthAlias;
	}

	public void setCertificatePassword(String certificatePassword) {
		this.certificatePassword = certificatePassword;
	}

	public String getCertificatePassword() {
		return certificatePassword;
	}

	public void setTruststore(String truststore) {
		this.truststore = truststore;
	}

	public String getTruststore() {
		return truststore;
	}

	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		this.truststoreAuthAlias = truststoreAuthAlias;
	}

	public String getTruststoreAuthAlias() {
		return truststoreAuthAlias;
	}

	public void setTruststorePassword(String truststorePassword) {
		this.truststorePassword = truststorePassword;
	}

	public String getTruststorePassword() {
		return truststorePassword;
	}

	public void setKeystoreType(String keystoreType) {
		this.keystoreType = keystoreType;
	}

	public String getKeystoreType() {
		return keystoreType;
	}

	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}

	public String getKeyManagerAlgorithm() {
		return keyManagerAlgorithm;
	}

	public void setTruststoreType(String truststoreType) {
		this.truststoreType = truststoreType;
	}

	public String getTruststoreType() {
		return truststoreType;
	}

	public void setTrustManagerAlgorithm(String getTrustManagerAlgorithm) {
		this.trustManagerAlgorithm = getTrustManagerAlgorithm;
	}

	public String getTrustManagerAlgorithm() {
		return trustManagerAlgorithm;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String string) {
		proxyHost = string;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int i) {
		proxyPort = i;
	}

	public String getProxyAuthAlias() {
		return proxyAuthAlias;
	}

	public void setProxyAuthAlias(String string) {
		proxyAuthAlias = string;
	}

	public String getProxyUserName() {
		return proxyUserName;
	}

	public void setProxyUserName(String string) {
		proxyUserName = string;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String string) {
		proxyPassword = string;
	}

	public String getProxyRealm() {
		if(StringUtils.isEmpty(proxyRealm))
			return null;
		return proxyRealm;
	}

	public void setProxyRealm(String string) {
		proxyRealm = string;
	}

	public String getRepositoryInfo(Session session) {
		RepositoryInfo ri = session.getRepositoryInfo();
		String id = ri.getId();
		String productName = ri.getProductName();
		String productVersion = ri.getProductVersion();
		String cmisVersion = ri.getCmisVersion().value();
		return "id [" + id + "] cmis version [" + cmisVersion + "] product ["
				+ productName + "] version [" + productVersion + "]";
	}

	public void setAction(String string) {
		action = string;
	}

	public String getAction() {
		if(action != null)
			return action.toLowerCase();

		return null;
	}

	public void setUrl(String string) {
		url = string;
	}

	public String getUrl() {
		return url;
	}

	public void setRepository(String string) {
		repository = string;
	}

	public String getRepository() {
		return repository;
	}

	public void setAuthAlias(String string) {
		authAlias = string;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	public void setUserName(String string) {
		userName = string;
	}

	public String getUserName() {
		return userName;
	}

	public void setPassword(String string) {
		password = string;
	}

	public String getPassword() {
		return password;
	}

	public void setBindingType(String string) {
		bindingType = string;
	}

	public String getBindingType() {
		if(bindingType != null)
			return bindingType.toLowerCase();

		return null;
	}

	public String getFileNameSessionKey() {
		return fileNameSessionKey;
	}

	public void setFileNameSessionKey(String string) {
		fileNameSessionKey = string;
	}

	public void setFileInputStreamSessionKey(String string) {
		fileInputStreamSessionKey = string;
	}

	public String getFileInputStreamSessionKey() {
		return fileInputStreamSessionKey;
	}

	public void setFileContentSessionKey(String string) {
		fileContentStreamSessionKey = string;
	}

	public String getFileContentSessionKey() {
		return fileContentStreamSessionKey;
	}

	public void setDefaultMediaType(String string) {
		defaultMediaType = string;
	}

	public String getDefaultMediaType() {
		return defaultMediaType;
	}

	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}

	public boolean isStreamResultToServlet() {
		return streamResultToServlet;
	}

	public void setGetProperties(boolean b) {
		getProperties = b;
	}

	public boolean isGetProperties() {
		return getProperties;
	}

	public void setUseRootFolder(boolean b) {
		useRootFolder = b;
	}

	public boolean isUseRootFolder() {
		return useRootFolder;
	}

	public void setResultOnNotFound(String string) {
		resultOnNotFound = string;
	}

	public String getResultOnNotFound() {
		return resultOnNotFound;
	}

	public void setKeepSession(boolean keepSession) {
		this.keepSession = keepSession;
	}

	public boolean isKeepSession() {
		return keepSession;
	}

	public void setBridgeSender(boolean isBridgeSender) {
		this.isBridgeSender  = isBridgeSender;
	}

	public boolean isBridgeSender() {
		return isBridgeSender;
	}

	@Override
	public void setPipe(AbstractPipe pipe) {
		this.pipe  = pipe;
	}

	@Override
	public AbstractPipe getPipe() {
		return pipe;
	}
}