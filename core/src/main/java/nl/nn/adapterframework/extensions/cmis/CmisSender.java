/*
   Copyright 2016 Nationale-Nederlanden

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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

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
 * </ul></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBindingType(String) bindingType}</td><td>"atompub" or "webservices"</td><td>"atompub"</td></tr>
 * <tr><td>{@link #setFileNameSessionKey(String) fileNameSessionKey}</td><td>(only used when <code>action=create</code>) The session key that contains the name of the file to use. If not set, the value of the property <code>fileName</code> from the input message is used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileInputStreamSessionKey(String) fileInputStreamSessionKey}</td><td>When <code>action=create</code>: the session key that contains the input stream of the file to use. When <code>action=get</code> and <code>getProperties=true</code>: the session key in which the input stream of the document is stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileContentSessionKey(String) fileContentSessionKey}</td><td>When <code>action=create</code>: the session key that contains the base64 encoded content of the file to use. When <code>action=get</code> and <code>getProperties=true</code>: the session key in which the base64 encoded content of the document is stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultMediaType(String) defaultMediaType}</td><td>(only used when <code>action=create</code>) The MIME type used to store the document when it's not set in the input message by a property</td><td>"application/octet-stream"</td></tr>
 * <tr><td>{@link #setStreamResultToServlet(boolean) streamResultToServlet}</td><td>(only used when <code>action=get</code>) if true, the content of the document is streamed to the HttpServletResponse object of the RestServiceDispatcher (instead of passed as a String)</td><td>false</td></tr>
 * <tr><td>{@link #setGetProperties(boolean) getProperties}</td><td>(only used when <code>action=get</code>) if true, the content of the document is streamed to <code>fileInputStreamSessionKey</code> and all document properties are put in the result as a xml string</td><td>false</td></tr>
 * <tr><td>{@link #setUseRootFolder(boolean) useRootFolder}</td><td>(only used when <code>action=create</code>) if true, the document is created in the root folder of the repository. Otherwise the document is created in the repository</td><td>true</td></tr>
 * <tr><td>{@link #setResultOnNotFound(String) resultOnNotFound}</td><td>(only used when <code>action=get</code>) result returned when no document was found for the given id (e.g. "[NOT_FOUND]"). If empty an exception is thrown</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeepSession(boolean) keepSession}</td><td>if true, the session is not closed at the end and it will be used in the next call</td><td>true</td></tr>
 * </table>
 * </p>
 * <p>
 * <table border="1">
 * <b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>authAlias</td><td>string</td><td>When a parameter with name authAlias is present, it is used instead of the authAlias specified by the attribute</td></tr>
 * <tr><td>userName</td><td>string</td><td>When a parameter with name userName is present, it is used instead of the userName specified by the attribute</td></tr>
 * <tr><td>password</td><td>string</td><td>When a parameter with name password is present, it is used instead of the password specified by the attribute</td></tr>
 * </table>
 * </p>
 * 
 * <p>
 * When <code>action=get</code> the input (string) indicates the id of the document to get. This input is mandatory.
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
 * @author Peter Leeuwenburgh
 */
public class CmisSender extends SenderWithParametersBase {

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

	private final static String FORMATSTRING_BY_DEFAULT = "yyyy-MM-dd HH:mm:ss";

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getUrl())) {
			throw new ConfigurationException("CmisSender [" + getName()
					+ "] has no url configured");
		}
		if (StringUtils.isEmpty(getRepository())) {
			throw new ConfigurationException("CmisSender [" + getName()
					+ "] has no repository configured");
		}
		if (!getBindingType().equalsIgnoreCase("atompub")
				&& !getBindingType().equalsIgnoreCase("webservices")) {
			throw new ConfigurationException("illegal value for bindingType ["
					+ getBindingType()
					+ "], must be 'atompub' or 'webservices'");
		}
		if (!getAction().equalsIgnoreCase("create")
				&& !getAction().equalsIgnoreCase("get")
				&& !getAction().equalsIgnoreCase("find")
				&& !getAction().equalsIgnoreCase("update")) {
			throw new ConfigurationException("illegal value for action ["
					+ getAction() + "], must be 'create', 'get', 'find' or 'update");
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

	public String sendMessage(String correlationID, String message,
			ParameterResolutionContext prc) throws SenderException,
			TimeOutException {
		try {
			if (session == null || !isKeepSession()) {
				String authAlias_work = null;
				String userName_work = null;
				String password_work = null;

				ParameterValueList pvl = null;
				try {
					if (prc != null && paramList != null) {
						pvl = prc.getValues(paramList);
						if (pvl != null) {
							ParameterValue pv = pvl
									.getParameterValue("authAlias");
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
					throw new SenderException(getLogPrefix() + "Sender ["
							+ getName()
							+ "] caught exception evaluating parameters", e);
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

				CredentialFactory cf = new CredentialFactory(authAlias_work,
						userName_work, password_work);
				session = connect(cf.getUsername(), cf.getPassword());
			}

			if (getAction().equalsIgnoreCase("get")) {
				return sendMessageForActionGet(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("create")) {
				return sendMessageForActionCreate(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("find")) {
				return sendMessageForActionFind(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("update")) {
				return sendMessageForActionUpdate(correlationID, message, prc);
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
			throw new SenderException(
					getLogPrefix()
							+ "input string cannot be empty but must contain a documentId");
		}

		CmisObject object = null;
		try {
			object = session.getObject(session.createObjectId(message));
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
						.getSession().get("restListenerServletResponse");
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
				for (Iterator it = document.getProperties().iterator(); it
						.hasNext();) {
					Property property = (Property) it.next();
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

	private XmlBuilder getPropertyXml(PropertyData property) {
		XmlBuilder propertyXml = new XmlBuilder("property");
		String name = property.getId();
		propertyXml.addAttribute("name", name);
		Object value = property.getFirstValue();
		if (value == null) {
			propertyXml.addAttribute("isNull", "true");
		} else {
			if (value instanceof BigInteger) {
				BigInteger bi = (BigInteger) property.getFirstValue();
				propertyXml.setValue(String.valueOf(bi));
			} else if (value instanceof Boolean) {
				Boolean b = (Boolean) property.getFirstValue();
				propertyXml.setValue(String.valueOf(b));
			} else if (value instanceof GregorianCalendar) {
				GregorianCalendar gc = (GregorianCalendar) property
						.getFirstValue();
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd'T'HH:mm:ss.SSSZ");
				propertyXml.setValue(sdf.format(gc.getTime()));
			} else {
				propertyXml.setValue((String) property.getFirstValue());
			}
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
		Map props = new HashMap();
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

		if (isUseRootFolder()) {
			Folder folder = session.getRootFolder();
			ContentStream contentStream = session.getObjectFactory()
					.createContentStream(fileName, fileLength, mediaType,
							inputStream);
			Document document = folder.createDocument(props, contentStream,
					null);
			log.debug(getLogPrefix() + "created new document [ "
					+ document.getId() + "]");
			return document.getId();
		} else {
			ContentStream contentStream = session.getObjectFactory()
					.createContentStream(fileName, fileLength, mediaType,
							inputStream);
			ObjectId objectId = session.createDocument(props, null,
					contentStream, null);
			log.debug(getLogPrefix() + "created new document [ "
					+ objectId.getId() + "]");
			return objectId.getId();
		}
	}

	private void processProperties(Element propertiesElement, Map props)
			throws SenderException {
		Collection properties = XmlUtils.getChildTags(propertiesElement,
				"property");
		Iterator iter = properties.iterator();
		while (iter.hasNext()) {
			Element propertyElement = (Element) iter.next();
			String property = XmlUtils.getStringValue(propertyElement);
			if (StringUtils.isNotEmpty(property)) {
				String nameAttr = propertyElement.getAttribute("name");
				String typeAttr = propertyElement.getAttribute("type");
				if (StringUtils.isEmpty(typeAttr)
						|| typeAttr.equalsIgnoreCase("string")) {
					props.put(nameAttr, property);
				} else if (typeAttr.equalsIgnoreCase("datetime")) {
					String formatStringAttr = propertyElement
							.getAttribute("formatString");
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
	
	private String sendMessageForActionFind(String correlationID,
			String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
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
		String statement = XmlUtils.getChildTagAsString(queryElement,
				"statement");
		String maxItems = XmlUtils
				.getChildTagAsString(queryElement, "maxItems");
		String skipCount = XmlUtils.getChildTagAsString(queryElement,
				"skipCount");
		String searchAllVersions = XmlUtils.getChildTagAsString(queryElement,
				"searchAllVersions");

		String includeAllowableActions = XmlUtils.getChildTagAsString(
				queryElement, "includeAllowableActions");

		OperationContext operationContext = session.createOperationContext();
		if (StringUtils.isNotEmpty(maxItems)) {
			operationContext.setMaxItemsPerPage(Integer.parseInt(maxItems));
		}
		boolean sav = false;
		if (StringUtils.isNotEmpty(searchAllVersions)) {
			sav = Boolean.parseBoolean(searchAllVersions);
		}
		if (StringUtils.isNotEmpty(includeAllowableActions)) {
			operationContext.setIncludeAllowableActions(Boolean
					.parseBoolean(searchAllVersions));
		}
		ItemIterable<QueryResult> q = session.query(statement, sav,
				operationContext);
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

	private String sendMessageForActionUpdate(String correlationID,
			String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		String objectId = null;
		Map props = new HashMap();
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

	private Session connect() {
		CredentialFactory cf = new CredentialFactory(getAuthAlias(),
				getUserName(), getPassword());
		return connect(cf.getUsername(), cf.getPassword());
	}

	private Session connect(String userName, String password) {
		log.debug(getLogPrefix() + "connecting with url [" + getUrl()
				+ "] repository [" + getRepository() + "]");
		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put(SessionParameter.USER, userName);
		parameter.put(SessionParameter.PASSWORD, password);
		if (getBindingType().equalsIgnoreCase("atompub")) {
			parameter.put(SessionParameter.ATOMPUB_URL, getUrl());
			parameter.put(SessionParameter.BINDING_TYPE,
					BindingType.ATOMPUB.value());
		} else {
			parameter.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE,
					getUrl() + "/RepositoryService.svc?wsdl");
			parameter.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE,
					getUrl() + "/NavigationService.svc?wsdl");
			parameter.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, getUrl()
					+ "/ObjectService.svc?wsdl");
			parameter.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE,
					getUrl() + "/VersioningService.svc?wsdl");
			parameter.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE,
					getUrl() + "/DiscoveryService.svc?wsdl");
			parameter.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE,
					getUrl() + "/RelationshipService.svc?wsdl");
			parameter.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE,
					getUrl() + "/MultiFilingService.svc?wsdl");
			parameter.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, getUrl()
					+ "/PolicyService.svc?wsdl");
			parameter.put(SessionParameter.WEBSERVICES_ACL_SERVICE, getUrl()
					+ "/ACLService.svc?wsdl");
			parameter.put(SessionParameter.BINDING_TYPE,
					BindingType.WEBSERVICES.value());
		}
		parameter.put(SessionParameter.REPOSITORY_ID, getRepository());
		Session session = sessionFactory.createSession(parameter);
		log.debug(getLogPrefix() + "connected with repository ["
				+ getRepositoryInfo(session) + "]");
		return session;
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
		return action;
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
		return bindingType;
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

	public void setKeepSession(boolean b) {
		keepSession = b;
	}

	public boolean isKeepSession() {
		return keepSession;
	}
}
