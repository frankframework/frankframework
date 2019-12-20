/*
   Copyright 2016 - 2019 Nationale-Nederlanden

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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
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

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.cmis.server.CmisEvent;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.AppConstants;
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
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Sender to obtain information from and write to a CMIS application.
 *
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
 * When <code>action=delete</code> the input (xml string) indicates the id of the document to get. This input is mandatory.
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
public class CmisSender extends SenderWithParametersBase {

	private String action;
	private String authAlias;
	private String userName;
	private String password;
	private String fileNameSessionKey;
	private String fileInputStreamSessionKey;
	private String fileContentStreamSessionKey;
	private String defaultMediaType = "application/octet-stream";
	private boolean streamResultToServlet = false;
	private boolean getProperties = false;
	private boolean getDocumentContent = true;
	private boolean useRootFolder = true;
	private String resultOnNotFound;

	private boolean runtimeSession = false;
	private boolean keepSession = true;

	private Session session;

	private List<String> actions = Arrays.asList("create", "delete", "get", "find", "update", "fetch", "dynamic");

	private CmisSessionBuilder sessionBuilder = new CmisSessionBuilder(getConfigurationClassLoader());

	private boolean convert2Base64 = AppConstants.getInstance().getBoolean("CmisSender.Base64FileContent", true);

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (!actions.contains(getAction())) {
			throw new ConfigurationException("illegal value for action [" + getAction() + "], must be " + actions.toString());
		}
		if (getAction().equals("create")) {
			if (StringUtils.isEmpty(getFileInputStreamSessionKey())
					&& StringUtils.isEmpty(getFileContentSessionKey())) {
				throw new ConfigurationException("fileInputStreamSessionKey or fileContentSessionKey should be specified");
			}
		}
		if (getAction().equals("get")) {
			if (isGetProperties() && isGetDocumentContent()) {
				if (StringUtils.isEmpty(getFileInputStreamSessionKey()) && StringUtils.isEmpty(getFileContentSessionKey())) {
					throw new ConfigurationException("fileInputStreamSessionKey or fileContentSessionKey should be specified");
				}
			}
		}

		// Legacy; check if the session should be created runtime (and thus for each call)
		if(getParameterList() != null && (getParameterList().findParameter("authAlias") != null || getParameterList().findParameter("userName") != null )) {
			runtimeSession = true;
		}
	}

	/**
	 * Creates a session during JMV runtime, tries to retrieve parameters and falls back on the defaults when they can't be found
	 */
	public Session createSession(ParameterResolutionContext prc) throws SenderException {
		String authAlias_work = null;
		String userName_work = null;
		String password_work = null;

		ParameterValueList pvl = null;
		try {
			if (prc != null && getParameterList() != null) {
				pvl = prc.getValues(getParameterList());
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
		try {
			return getSessionBuilder().build(cf.getUsername(), cf.getPassword());
		}
		catch (CmisSessionException e) {
			throw new SenderException(e);
		}
	}

	public CmisSessionBuilder getSessionBuilder() {
		return sessionBuilder;
	}

	@Override
	public void open() throws SenderException {
		// If we don't need to create the session at JVM runtime, create to test the connection
		if (!runtimeSession) {
			try {
				session = getSessionBuilder().build();
			}
			catch (CmisSessionException e) {
				throw new SenderException("unable to create cmis session", e);
			}
		}
	}

	@Override
	public void close() throws SenderException {
		if (session != null) {
			session.clear();
			session = null;
		}
	}

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		try {
			if(runtimeSession || !isKeepSession())
				session = createSession(prc);

			if (getAction().equalsIgnoreCase("get")) {
				return sendMessageForActionGet(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("create")) {
				return sendMessageForActionCreate(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("delete")) {
				return sendMessageForActionDelete(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("find")) {
				return sendMessageForActionFind(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("update")) {
				return sendMessageForActionUpdate(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("fetch")) {
				return sendMessageForDynamicActions(correlationID, message, prc);
			} else if (getAction().equalsIgnoreCase("dynamic")) {
				return sendMessageForDynamicActions(correlationID, message, prc);
			} else {
				throw new SenderException(getLogPrefix() + "unknown action [" + getAction() + "]");
			}
		} finally {
			if (session != null && !isKeepSession()) {
				session.clear();
				session = null;
			}
		}
	}

	private String sendMessageForActionGet(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		if (StringUtils.isEmpty(message)) {
			throw new SenderException(getLogPrefix() + "input string cannot be empty but must contain a documentId");
		}

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

		Document document = (Document) object;

		try {
			boolean getProperties = isGetProperties();
			boolean getDocumentContent = isGetDocumentContent();
			if (getParameterList() != null) {
				ParameterValueList pvl = prc.getValues(getParameterList());
				if (pvl != null) {
					if(pvl.parameterExists("getProperties"))
						getProperties = pvl.getParameterValue("getProperties").asBooleanValue(isGetProperties());
					if(pvl.parameterExists("getDocumentContent"))
						getDocumentContent = pvl.getParameterValue("getDocumentContent").asBooleanValue(isGetDocumentContent());
				}
			}

			if (isStreamResultToServlet()) {
				HttpServletResponse response = (HttpServletResponse) prc.getSession().get(IPipeLineSession.HTTP_RESPONSE_KEY);

				ContentStream contentStream = document.getContentStream();
				InputStream inputStream = contentStream.getStream();
				String contentType = contentStream.getMimeType();
				if (StringUtils.isNotEmpty(contentType)) {
					log.debug(getLogPrefix() + "setting response Content-Type header [" + contentType + "]");
					response.setHeader("Content-Type", contentType);
				}
				String contentDisposition = "attachment; filename=\"" + contentStream.getFileName() + "\"";
				log.debug(getLogPrefix() + "setting response Content-Disposition header [" + contentDisposition + "]");
				response.setHeader("Content-Disposition", contentDisposition);
				OutputStream outputStream;
				outputStream = response.getOutputStream();
				Misc.streamToStream(inputStream, outputStream);
				log.debug(getLogPrefix() + "copied document content input stream [" + inputStream + "] to output stream [" + outputStream + "]");

				return "";
			}
			else if (getProperties) {
				if(getDocumentContent) {
					ContentStream contentStream = document.getContentStream();
					InputStream inputStream = contentStream.getStream();
					if (StringUtils.isNotEmpty(fileInputStreamSessionKey)) {
						prc.getSession().put(getFileInputStreamSessionKey(), inputStream);
					} else {
						byte[] bytes = Misc.streamToBytes(inputStream);

						if(convert2Base64)
							prc.getSession().put(getFileContentSessionKey(), Base64.encodeBase64String(bytes));
						else
							prc.getSession().put(getFileContentSessionKey(), bytes);
					}
				}

				XmlBuilder cmisXml = new XmlBuilder("cmis");
				XmlBuilder propertiesXml = new XmlBuilder("properties");
				for (Iterator<Property<?>> it = document.getProperties().iterator(); it.hasNext();) {
					Property<?> property = (Property<?>) it.next();
					propertiesXml.addSubElement(CmisUtils.getPropertyXml(property));
				}
				cmisXml.addSubElement(propertiesXml);

				return cmisXml.toXML();
			}
			else {
				ContentStream contentStream = document.getContentStream();
				InputStream inputStream = contentStream.getStream();

				if (StringUtils.isNotEmpty(fileInputStreamSessionKey)) {
					prc.getSession().put(getFileInputStreamSessionKey(), inputStream);
					return "";
				} else if (StringUtils.isNotEmpty(getFileContentSessionKey())) {
					byte[] bytes = Misc.streamToBytes(inputStream);
					if(convert2Base64)
						prc.getSession().put(getFileContentSessionKey(), Base64.encodeBase64String(bytes));
					else
						prc.getSession().put(getFileContentSessionKey(), bytes);
					return "";
				}
				else
					return Misc.streamToString(inputStream, null, false);
			}
		} catch (IOException e) {
			throw new SenderException(e);
		} catch (ParameterException e) {
			throw new SenderException(e);
		}
	}

	private String sendMessageForActionCreate(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		String fileName = (String) prc.getSession().get(getFileNameSessionKey());

		Object inputFromSessionKey;
		if(StringUtils.isNotEmpty(fileInputStreamSessionKey)) {
			inputFromSessionKey = prc.getSession().get(getFileInputStreamSessionKey());
		}
		else {
			inputFromSessionKey = prc.getSession().get(getFileContentSessionKey());
		}
		InputStream inputStream = null;
		if (inputFromSessionKey instanceof InputStream) {
			inputStream = (InputStream) inputFromSessionKey;
		} else if (inputFromSessionKey instanceof byte[]) {
			inputStream = new ByteArrayInputStream((byte[]) inputFromSessionKey);
		} else if(inputFromSessionKey instanceof String) {
			byte[] bytes = Base64.decodeBase64((String) inputFromSessionKey);
			inputStream = new ByteArrayInputStream(bytes);
		} else {
			throw new SenderException("expected InputStream, ByteArray or Base64-String but got ["+inputFromSessionKey.getClass().getName()+"] instead");
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

			String objectTypeId = XmlUtils.getChildTagAsString(cmisElement, "objectTypeId");
			if (StringUtils.isNotEmpty(objectTypeId)) {
				props.put(PropertyIds.OBJECT_TYPE_ID, objectTypeId);
			} else {
				props.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
			}
			String name = XmlUtils.getChildTagAsString(cmisElement, "name");
			if (StringUtils.isEmpty(fileName)) {
				fileName = XmlUtils.getChildTagAsString(cmisElement, "fileName");
			}
			mediaType = XmlUtils.getChildTagAsString(cmisElement, "mediaType");
			if (StringUtils.isNotEmpty(name)) {
				props.put(PropertyIds.NAME, name);
			} else if (StringUtils.isNotEmpty(fileName)) {
				props.put(PropertyIds.NAME, fileName);
			} else {
				props.put(PropertyIds.NAME, "[unknown]");
			}
			Element propertiesElement = XmlUtils.getFirstChildTag(cmisElement, "properties");
			if (propertiesElement != null) {
				processProperties(propertiesElement, props);
			}
		} catch (DomBuilderException e) {
			throw new SenderException(getLogPrefix() + "exception parsing [" + message + "]", e);
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

			boolean setPropertyAsNull = false;
			String isNull = propertyElement.getAttribute("isNull");
			if(StringUtils.isNotEmpty(isNull)) {
				setPropertyAsNull = Boolean.parseBoolean(isNull);
			}

			if (StringUtils.isNotEmpty(property) || setPropertyAsNull) {
				String nameAttr = propertyElement.getAttribute("name");
				String typeAttr = propertyElement.getAttribute("type");

				if (setPropertyAsNull) {
					props.put(nameAttr, null);
				} else if (StringUtils.isEmpty(typeAttr) || typeAttr.equalsIgnoreCase("string")) {
					props.put(nameAttr, property);
				} else if (typeAttr.equalsIgnoreCase("integer")) {
					props.put(nameAttr, new BigInteger(property));
				} else if (typeAttr.equalsIgnoreCase("boolean")) {
					props.put(nameAttr, Boolean.parseBoolean(property));
				} else if (typeAttr.equalsIgnoreCase("datetime")) {
					String formatStringAttr = propertyElement.getAttribute("formatString");
					if (StringUtils.isEmpty(formatStringAttr)) {
						formatStringAttr = CmisUtils.FORMATSTRING_BY_DEFAULT;
					}
					//TODO to be removed in a few versions
					if(AppConstants.getInstance().getBoolean("cmissender.processproperties.legacydateformat", false)) {
						formatStringAttr = "yyyy-MM-dd HH:mm:ss";
					}
					DateFormat df = new SimpleDateFormat(formatStringAttr);
					GregorianCalendar calendar = new GregorianCalendar();
					try {
						Date date = df.parse(property);
						calendar.setTime(date);
					} catch (ParseException e) {
						throw new SenderException(getLogPrefix()
								+ "exception parsing date [" + property
								+ "] using formatString [" + formatStringAttr
								+ "]", e);
					}
					props.put(nameAttr, calendar);
				} else {
					log.warn(getLogPrefix() + "unknown type ["+ typeAttr +"], assuming 'string'");
					props.put(nameAttr, property);
				}
				if (log.isDebugEnabled()) {
					log.debug(getLogPrefix() + "set property name ["+ nameAttr +"] value ["+ property +"]");
				}
			} else {
				log.debug(getLogPrefix() + "empty property found, ignoring");
			}
		}
	}

	private String sendMessageForActionDelete(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		if (StringUtils.isEmpty(message)) {
			throw new SenderException(getLogPrefix() + "input string cannot be empty but must contain a documentId");
		}
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
		if (object.hasAllowableAction(Action.CAN_DELETE_OBJECT)) { //// You can delete
			Document suppDoc = (Document) object;
			suppDoc.delete(true);
			return correlationID;

		} else {  //// You can't delete
			throw new SenderException(getLogPrefix() + "Document cannot be deleted");
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

		XmlBuilder cmisXml = new XmlBuilder("cmis");
		ItemIterable<QueryResult> q = session.query(statement, sav, operationContext);

		if(q == null) {
			cmisXml.addAttribute("totalNumItems", 0);
		} else {
			if (StringUtils.isNotEmpty(skipCount)) {
				long sc = Long.parseLong(skipCount);
				q = q.skipTo(sc);
			}
			if (StringUtils.isNotEmpty(maxItems)) {
				q = q.getPage();
			}
			XmlBuilder rowsetXml = new XmlBuilder("rowset");
			for (QueryResult qResult : q) {
				XmlBuilder rowXml = new XmlBuilder("row");
				for (PropertyData<?> property : qResult.getProperties()) {
					rowXml.addSubElement(CmisUtils.getPropertyXml(property));
				}
				rowsetXml.addSubElement(rowXml);
			}
			cmisXml.addAttribute("totalNumItems", q.getTotalNumItems());
			cmisXml.addSubElement(rowsetXml);
		}
		return cmisXml.toXML();
	}

	private CmisObject getCmisObject(String message) throws SenderException, CmisObjectNotFoundException {
		if (XmlUtils.isWellFormed(message, "cmis")) {
			try {
				Element queryElement = XmlUtils.buildElement(message);
				return getCmisObject(queryElement);
			} catch (DomBuilderException e) {
				throw new SenderException("unable to build cmis xml from sender input message", e);
			}
		}
		throw new SenderException("unable to build cmis xml from sender input message");
	}

	private CmisObject getCmisObject(Element queryElement) throws SenderException, CmisObjectNotFoundException {
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

		String objectIdstr = XmlUtils.getChildTagAsString(queryElement, "objectId");
		if(objectIdstr == null)
			objectIdstr = XmlUtils.getChildTagAsString(queryElement, "id");

		if(objectIdstr != null) {
			return session.getObject(session.createObjectId(objectIdstr), operationContext);
		}
		else { //Ok, id can still be null, perhaps its a path?
			String path = XmlUtils.getChildTagAsString(queryElement, "path");
			return session.getObjectByPath(path, operationContext);
		}
	}

	private String sendMessageForDynamicActions(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		IPipeLineSession messageContext = prc.getSession();

		XmlBuilder resultXml = new XmlBuilder("cmis");
		Element requestElement = null;
		try {
			if (XmlUtils.isWellFormed(message, "cmis")) {
				requestElement = XmlUtils.buildElement(message);
			} else {
				requestElement = XmlUtils.buildElement("<cmis/>");
			}
		} catch (DomBuilderException e) {
			throw new SenderException(e);
		}

		String cmisEvent = (String) prc.getSession().get("CmisEvent");
		CmisEvent event = CmisEvent.GET_OBJECT;
		if(StringUtils.isNotEmpty(cmisEvent))
			event = CmisEvent.valueOf(cmisEvent);

		switch (event) {
			case DELETE_OBJECT:
				getCmisObject(requestElement).delete(true);
				resultXml.addAttribute("deleted", true);
				break;

			case CREATE_DOCUMENT:
				Map<String, Object> props = new HashMap<String, Object>();
				Element propertiesElement = XmlUtils.getFirstChildTag(requestElement, "properties");
				if (propertiesElement != null) {
					processProperties(propertiesElement, props);
				}

				ObjectId folderId = null;
				String folderIdstr = XmlUtils.getChildTagAsString(requestElement, "folderId");
				if(StringUtils.isNotEmpty(folderIdstr))
					folderId = session.createObjectId(folderIdstr);

				String versioningStatestr = XmlUtils.getChildTagAsString(requestElement, "versioningState");
				VersioningState state = VersioningState.valueOf(versioningStatestr);

				Element contentStreamXml = XmlUtils.getFirstChildTag(requestElement, "contentStream");
				InputStream stream = (InputStream) messageContext.get("ContentStream");
				String fileName = contentStreamXml.getAttribute("filename");
				long fileLength = Long.parseLong(contentStreamXml.getAttribute("length"));
				String mediaType = contentStreamXml.getAttribute("mimeType");
				ContentStream contentStream = session.getObjectFactory().createContentStream(fileName, fileLength, mediaType, stream);

				ObjectId createdDocumentId = session.createDocument(props, folderId, contentStream, state);
				XmlBuilder cmisId = new XmlBuilder("id");
				cmisId.setValue(createdDocumentId.getId());
				resultXml.addSubElement(cmisId);
				break;

			case UPDATE_PROPERTIES:
				Map<String, Object> propss = new HashMap<String, Object>();
				Element propertiesElements = XmlUtils.getFirstChildTag(requestElement, "properties");
				if (propertiesElements != null) {
					processProperties(propertiesElements, propss);
				}
				getCmisObject(requestElement).updateProperties(propss);

				resultXml.addAttribute("updated", true);
				break;

			case GET_CONTENTSTREAM:
				String streamId = XmlUtils.getChildTagAsString(requestElement, "streamId");

				long offsetStr = XmlUtils.getChildTagAsLong(requestElement, "offset");
				BigInteger offset = BigInteger.valueOf(offsetStr);
				long lengthStr = XmlUtils.getChildTagAsLong(requestElement, "length");
				BigInteger length = BigInteger.valueOf(lengthStr);

				ObjectId objectIdforContentStream = getCmisObject(requestElement);
				ContentStream getContentStream = session.getContentStream(objectIdforContentStream, streamId, offset, length);

				XmlBuilder objectIdStream = new XmlBuilder("id");
				objectIdStream.setValue(objectIdforContentStream.getId());
				resultXml.addSubElement(objectIdStream);

				XmlBuilder contentStreamBuilder = new XmlBuilder("contentStream");
				contentStreamBuilder.addAttribute("filename", getContentStream.getFileName());
				contentStreamBuilder.addAttribute("length", getContentStream.getLength());
				contentStreamBuilder.addAttribute("mimeType", getContentStream.getMimeType());
				resultXml.addSubElement(contentStreamBuilder);
				prc.getSession().put("ContentStream", getContentStream.getStream());
				break;

			case GET_TYPE_DEFINITION:
				String typeId = XmlUtils.getChildTagAsString(requestElement, "typeId");
				ObjectType objectType = session.getTypeDefinition(typeId);
				XmlBuilder typesXml = new XmlBuilder("typeDefinitions");
				typesXml.addSubElement(CmisUtils.typeDefinition2Xml(objectType));
				resultXml.addSubElement(typesXml);
				break;

			case GET_TYPE_DESCENDANTS:
				String typeDescId = XmlUtils.getChildTagAsString(requestElement, "typeId");
				int depth = Integer.parseInt(XmlUtils.getChildTagAsString(requestElement, "depth"));
				boolean includePropertyDefinitions = XmlUtils.getChildTagAsBoolean(requestElement, "includePropertyDefinitions");
				List<Tree<ObjectType>> objectTypes = session.getTypeDescendants(typeDescId, depth, includePropertyDefinitions);
				resultXml.addSubElement(CmisUtils.typeDescendants2Xml(objectTypes));
				break;

			case GET_REPOSITORIES:
				List<RepositoryInfo> repositories = session.getBinding().getRepositoryService().getRepositoryInfos(null);
				XmlBuilder repositoriesXml = new XmlBuilder("repositories");

				for (RepositoryInfo repository : repositories) {
					repositoriesXml.addSubElement(CmisUtils.repositoryInfo2xml(repository));
				}
				resultXml.addSubElement(repositoriesXml);
				break;

			case GET_REPOSITORY_INFO:
				String repositoryId = XmlUtils.getChildTagAsString(requestElement, "repositoryId");
				RepositoryInfo repository = session.getBinding().getRepositoryService().getRepositoryInfo(repositoryId, null);

				XmlBuilder repositoryInfoXml = new XmlBuilder("repositories");
				repositoryInfoXml.addSubElement(CmisUtils.repositoryInfo2xml(repository));
				resultXml.addSubElement(repositoryInfoXml);
				break;

			case QUERY:
				String repositoryQueryId = XmlUtils.getChildTagAsString(requestElement, "repositoryId");
				String statement = XmlUtils.getChildTagAsString(requestElement, "statement");
				Boolean searchAllVersions = XmlUtils.getChildTagAsBoolean(requestElement, "searchAllVersions");
				Boolean includeAllowableActions = XmlUtils.getChildTagAsBoolean(requestElement, "includeAllowableActions");
				String includeRelationshipsEnum = XmlUtils.getChildTagAsString(requestElement, "includeRelationships");
				IncludeRelationships includeRelationships = IncludeRelationships.valueOf(includeRelationshipsEnum);
				String renditionFilter = XmlUtils.getChildTagAsString(requestElement, "renditionFilter");

				BigInteger maxItems = null;
				String maxItemsString = XmlUtils.getChildTagAsString(requestElement, "maxItems");
				if (StringUtils.isNotEmpty(maxItemsString)) {
					maxItems = BigInteger.valueOf(Long.parseLong(maxItemsString));
				}
				BigInteger skipCount = null;
				String skipCountString = XmlUtils.getChildTagAsString(requestElement, "skipCount");
				if (StringUtils.isNotEmpty(skipCountString)) {
					skipCount = BigInteger.valueOf(Long.parseLong(skipCountString));
				}

				//Create a operationContext and do session.query?
//				OperationContext context = session.createOperationContext();
//				context.setIncludeAllowableActions(includeAllowableActions);
//				context.setIncludeRelationships(includeRelationships);
//				context.setRenditionFilterString(renditionFilter);

				ObjectList result = session.getBinding().getDiscoveryService().query(repositoryQueryId, statement, 
						searchAllVersions, includeAllowableActions, includeRelationships, renditionFilter, maxItems, skipCount, null);
				resultXml.addSubElement(CmisUtils.objectList2xml(result));
				break;

			case GET_CHILDREN:
				String rid = XmlUtils.getChildTagAsString(requestElement, "repositoryId");
				String fid = XmlUtils.getChildTagAsString(requestElement, "folderId");
				String getChildren_repositoryFilter = XmlUtils.getChildTagAsString(requestElement, "filter");
				String getChildren_repositoryOrderBy = XmlUtils.getChildTagAsString(requestElement, "orderBy");
				Boolean getChildren_includeAllowableActions = XmlUtils.getChildTagAsBoolean(requestElement, "includeAllowableActions");
				IncludeRelationships getChildren_includeRelationships = IncludeRelationships.valueOf(XmlUtils.getChildTagAsString(requestElement, "includeRelationships"));
				String getChildren_renditionFilter = XmlUtils.getChildTagAsString(requestElement, "renditionFilter");
				Boolean getChildren_includePathSegment = XmlUtils.getChildTagAsBoolean(requestElement, "includePathSegment");
				BigInteger getChildren_maxItems = BigInteger.valueOf(XmlUtils.getChildTagAsLong(requestElement, "maxItems"));
				BigInteger getChildren_skipCount = BigInteger.valueOf(XmlUtils.getChildTagAsLong(requestElement, "skipCount"));

				ObjectInFolderList oifs = session.getBinding().getNavigationService().getChildren(rid, fid, getChildren_repositoryFilter, 
						getChildren_repositoryOrderBy, getChildren_includeAllowableActions, getChildren_includeRelationships, 
						getChildren_renditionFilter, getChildren_includePathSegment, getChildren_maxItems, getChildren_skipCount, null);

				resultXml.addSubElement(CmisUtils.objectInFolderList2xml(oifs));
				break;

			case GET_PROPERTIES:
			case GET_OBJECT:
			case GET_OBJECT_BY_PATH:
			case GET_ALLOWABLE_ACTIONS:
				CmisObject object = getCmisObject(requestElement);
				messageContext.put(CmisUtils.ORIGINAL_OBJECT_KEY, object);
				CmisUtils.cmisObject2Xml(resultXml, object);
				break;
			default:
				throw new CmisNotSupportedException("Operation not implemented");
		}

		return resultXml.toXML();
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


	@IbisDoc({"override entrypoint wsdl by reading it from the classpath, overrides url attribute", ""})
	public void setOverrideEntryPointWSDL(String overrideEntryPointWSDL) {
		sessionBuilder.setOverrideEntryPointWSDL(overrideEntryPointWSDL);
	}

	@IbisDoc({"Accept self signed certificates", "false"})
	public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		sessionBuilder.setAllowSelfSignedCertificates(allowSelfSignedCertificates);
	}

	@IbisDoc({"Ignore certificate hostname validation", "true"})
	public void setVerifyHostname(boolean verifyHostname) {
		sessionBuilder.setVerifyHostname(verifyHostname);
	}

	@IbisDoc({"Ignore expired certificate exceptions", "false"})
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		sessionBuilder.setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}

	@IbisDoc({"Path (or resource url) to certificate to be used for authentication", ""})
	public void setCertificateUrl(String certificate) {
		sessionBuilder.setCertificateUrl(certificate);
	}

	@IbisDoc({"Auth Alias used to obtain certificate password", ""})
	public void setCertificateAuthAlias(String certificateAuthAlias) {
		sessionBuilder.setCertificateAuthAlias(certificateAuthAlias);
	}

	@IbisDoc({"Certificate Password", ""})
	public void setCertificatePassword(String certificatePassword) {
		sessionBuilder.setCertificatePassword(certificatePassword);
	}

	@IbisDoc({"Path (or resource url) to truststore to be used for authentication", ""})
	public void setTruststore(String truststore) {
		sessionBuilder.setTruststore(truststore);
	}

	@IbisDoc({"Alias used to obtain truststore password", ""})
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		sessionBuilder.setTruststoreAuthAlias(truststoreAuthAlias);
	}

	@IbisDoc({"Truststore Password", ""})
	public void setTruststorePassword(String truststorePassword) {
		sessionBuilder.setTruststorePassword(truststorePassword);
	}

	@IbisDoc({"Keystore Type", "pkcs12"})
	public void setKeystoreType(String keystoreType) {
		sessionBuilder.setKeystoreType(keystoreType);
	}

	@IbisDoc({"KeyManager Algorithm", "pkix"})
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		sessionBuilder.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}

	@IbisDoc({"Truststore Type", "jks"})
	public void setTruststoreType(String truststoreType) {
		sessionBuilder.setTruststoreType(truststoreType);
	}

	@IbisDoc({"TrustManager Algorithm", "pkix"})
	public void setTrustManagerAlgorithm(String getTrustManagerAlgorithm) {
		sessionBuilder.setTrustManagerAlgorithm(getTrustManagerAlgorithm);
	}

	@IbisDoc({"Proxy host url", ""})
	public void setProxyHost(String proxyHost) {
		sessionBuilder.setProxyHost(proxyHost);
	}

	@IbisDoc({"Proxy host port", "80"})
	public void setProxyPort(int proxyPort) {
		sessionBuilder.setProxyPort(proxyPort);
	}

	@IbisDoc({"Alias used to obtain credentials for authentication to proxy", ""})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		sessionBuilder.setProxyAuthAlias(proxyAuthAlias);
	}

	@IbisDoc({"Proxy Username", ""})
	public void setProxyUserName(String proxyUserName) {
		sessionBuilder.setProxyUserName(proxyUserName);
	}

	@IbisDoc({"Proxy Password", ""})
	public void setProxyPassword(String proxyPassword) {
		sessionBuilder.setProxyPassword(proxyPassword);
	}

	@IbisDoc({"specifies action to perform. Must be one of \n" +
			" * <ul>\n" +
			" * <li><code>get</code>: get the content of a document (and optional the properties)</li>\n" +
			" * <li><code>create</code>: create a document</li>\n" +
			" * <li><code>find</code>: perform a query that returns properties</li>\n" +
			" * <li><code>update</code>: update the properties of an existing document</li>\n" +
			" * <li><code>fetch</code>: get the (meta)data of a folder or document</li>\n" +
			" * </ul>", ""})
	public void setAction(String string) {
		action = string;
	}

	public String getAction() {
		if(action != null)
			return action.toLowerCase();

		return null;
	}

	@IbisDoc({"the maximum number of concurrent connections", "10"})
	public void setMaxConnections(int i) throws ConfigurationException {
		sessionBuilder.setMaxConnections(i);
	}

	@IbisDoc({"the connection timeout in seconds", "10"})
	public void setTimeout(int i) throws ConfigurationException {
		sessionBuilder.setTimeout(i);
	}

	@IbisDoc({"url to connect to", ""})
	public void setUrl(String url) throws ConfigurationException {
		sessionBuilder.setUrl(url);
	}

	@IbisDoc({"repository id", ""})
	public void setRepository(String repository) throws ConfigurationException {
		sessionBuilder.setRepository(repository);
	}

	@IbisDoc({"alias used to obtain credentials for authentication to host", ""})
	public void setAuthAlias(String authAlias) {
		sessionBuilder.setAuthAlias(authAlias);
		this.authAlias = authAlias;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	@IbisDoc({"username used in authentication to host", ""})
	public void setUsername(String userName) {
		sessionBuilder.setUsername(userName);
		this.userName = userName;
	}
	/**
	 * @deprecated legacy username is one word..
	 */
	public void setUserName(String userName) {
		setUsername(userName);
	}
	public String getUserName() {
		return userName;
	}

	@IbisDoc({"", ""})
	public void setPassword(String password) {
		sessionBuilder.setPassword(password);
		this.password = password;
	}
	public String getPassword() {
		return password;
	}

	@IbisDoc({"'atompub', 'webservices' or 'browser'", "'atompub'"})
	public void setBindingType(String bindingType) throws ConfigurationException {
		sessionBuilder.setBindingType(bindingType);
	}

	public String getFileNameSessionKey() {
		return fileNameSessionKey;
	}

	@IbisDoc({"(only used when <code>action=create</code>) the session key that contains the name of the file to use. if not set, the value of the property <code>filename</code> from the input message is used", ""})
	public void setFileNameSessionKey(String string) {
		fileNameSessionKey = string;
	}

	@IbisDoc({"when <code>action=create</code>: the session key that contains the input stream of the file to use. when <code>action=get</code> and <code>getproperties=true</code>: the session key in which the input stream of the document is stored", ""})
	public void setFileInputStreamSessionKey(String string) {
		fileInputStreamSessionKey = string;
	}

	public String getFileInputStreamSessionKey() {
		return fileInputStreamSessionKey;
	}

	@IbisDoc({"when <code>action=create</code>: the session key that contains the base64 encoded content of the file to use. when <code>action=get</code> and <code>getproperties=true</code>: the session key in which the base64 encoded content of the document is stored", ""})
	public void setFileContentSessionKey(String string) {
		fileContentStreamSessionKey = string;
	}

	public String getFileContentSessionKey() {
		return fileContentStreamSessionKey;
	}

	@IbisDoc({"(only used when <code>action=create</code>) the mime type used to store the document when it's not set in the input message by a property", "'application/octet-stream'"})
	public void setDefaultMediaType(String string) {
		defaultMediaType = string;
	}

	public String getDefaultMediaType() {
		return defaultMediaType;
	}

	@IbisDoc({"(only used when <code>action=get</code>) if true, the content of the document is streamed to the httpservletresponse object of the restservicedispatcher (instead of passed as a string)", "false"})
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}

	public boolean isStreamResultToServlet() {
		return streamResultToServlet;
	}

	@IbisDoc({"(only used when <code>action=get</code>) if true, the content of the document is streamed to <code>fileinputstreamsessionkey</code> and all document properties are put in the result as a xml string", "false"})
	public void setGetProperties(boolean b) {
		getProperties = b;
	}

	public boolean isGetProperties() {
		return getProperties;
	}

	public boolean isGetDocumentContent() {
		return getDocumentContent;
	}

	@IbisDoc({"(only used when <code>action=get</code>) if true, the attachment for the document is streamed to <code>fileInputStreamSessionKey</code> otherwise only the properties are returned", "true"})
	public void setGetDocumentContent(boolean getDocumentContent) {
		this.getDocumentContent = getDocumentContent;
	}

	@IbisDoc({"(only used when <code>action=create</code>) if true, the document is created in the root folder of the repository. otherwise the document is created in the repository", "true"})
	public void setUseRootFolder(boolean b) {
		useRootFolder = b;
	}

	public boolean isUseRootFolder() {
		return useRootFolder;
	}

	@IbisDoc({"(only used when <code>action=get</code>) result returned when no document was found for the given id (e.g. '[not_found]'). if empty an exception is thrown", ""})
	public void setResultOnNotFound(String string) {
		resultOnNotFound = string;
	}

	public String getResultOnNotFound() {
		return resultOnNotFound;
	}

	@IbisDoc({"if true, the session is not closed at the end and it will be used in the next call", "true"})
	public void setKeepSession(boolean keepSession) {
		this.keepSession = keepSession;
	}

	public boolean isKeepSession() {
		return keepSession;
	}
}
