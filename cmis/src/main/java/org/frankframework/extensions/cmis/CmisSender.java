/*
   Copyright 2016-2019 Nationale-Nederlanden, 2020-2025 WeAreFrank

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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;

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
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Forward;
import org.frankframework.doc.Mandatory;
import org.frankframework.doc.Unsafe;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.HasTruststore;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.extensions.cmis.CmisSessionBuilder.BindingTypes;
import org.frankframework.extensions.cmis.server.CmisEvent;
import org.frankframework.extensions.cmis.server.CmisEventDispatcher;
import org.frankframework.http.AbstractHttpSession;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Sender to obtain information from and write to a CMIS application.
 *
 * <p>
 * When <code>action=get</code> the input (xml string) indicates the id of the document to get. This input is mandatory.
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * <cmis>
 *     <id>documentId</id>
 * </cmis>
 * }</pre>
 * </p>
 * <p>
 * When <code>action=delete</code> the input (xml string) indicates the id of the document to get. This input is mandatory.
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * <cmis>
 *     <id>documentId</id>
 * </cmis>
 * }</pre>
 * </p>
 * <p>
 * When <code>action=create</code> the input (xml string) indicates document properties to set. This input is optional.
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * <cmis>
 *     <name>Offerte</name>
 *     <objectTypeId>NNB_Geldlening</objectTypeId>
 *     <mediaType>application/pdf</mediaType>
 *     <properties>
 *         <property name="ArrivedAt" type="datetime" formatString="yyyy-MM-dd'T'HH:mm:ss.SSSz">2014-11-27T16:43:01.268+0100</property>
 *         <property name="ArrivedBy">HDN</property>
 *         <property name="DocumentType">Geldlening</property>
 *     </properties>
 * </cmis>
 * }</pre>
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
 * <b>Example:</b>
 * <pre>{@code
 * <query>
 *    <statement>select * from cmis:document</statement>
 *    <maxItems>10</maxItems>
 *    <skipCount>0</skipCount>
 *    <searchAllVersions>true</searchAllVersions>
 *    <includeAllowableActions>true</includeAllowableActions>
 * </query
 * }</pre>
 * </p>
 * <p>
 * When <code>action=update</code> the input (xml string) indicates document properties to update.
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * <cmis>
 *    <id>123456789</id>
 *    <properties>
 *       <property name="ArrivedAt" type="datetime" formatString="yyyy-MM-dd'T'HH:mm:ss.SSSz">2014-11-27T16:43:01.268+0100</property>
 *       <property name="ArrivedBy">HDN</property>
 *       <property name="DocumentType">Geldlening</property>
 *       <property name="DocumentDetail" isNull="true" />
 *    </properties>
 * </cmis>
 * }</pre>
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
 * @ff.parameter authAlias overrides authAlias specified by the attribute <code>authAlias</code>
 * @ff.parameter username overrides username specified by the attribute <code>username</code>
 * @ff.parameter password overrides password specified by the attribute <code>password</code>
 *
 * @author	Peter Leeuwenburgh
 * @author	Niels Meijer
 */
@Forward(name = "notFound", description = "if the requested object could not be found for actions GET, UPDATE and DELETE")
public class CmisSender extends AbstractSenderWithParameters implements HasKeystore, HasTruststore {

	private static final String NOT_FOUND_FORWARD_NAME="notFound";

	public enum CmisAction {
		/** Create a document */
		CREATE,
		/** Delete a document */
		DELETE,
		/** Get the content of a document (and optional the properties) */
		GET,
		/** Perform a query that returns properties */
		FIND,
		/** Update the properties of an existing document */
		UPDATE,
		/** Get the (meta)data of a folder or document */
		FETCH,
		/** Determine action based on the incoming CmisEvent */
		DYNAMIC;
	}
	private @Getter CmisAction action;
	private @Getter String authAlias;
	private @Getter String username;
	private @Getter String password;
	private @Getter String filenameSessionKey;
	private @Getter String defaultMediaType = "application/octet-stream";
	private @Getter boolean getProperties = false;
	private @Getter boolean getDocumentContent = true;
	private @Getter boolean useRootFolder = true;
	private @Getter String resultOnNotFound;
	private boolean runtimeSession = false;
	private @Getter boolean keepSession = true;

	private CloseableCmisSession globalSession;

	private final CmisSessionBuilder sessionBuilder = new CmisSessionBuilder(this);

	private @Getter String fileSessionKey;

	public static final String HEADER_PARAM_PREFIX = "Header.";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (getAction()==CmisAction.CREATE){
			checkStringAttributeOrParameter("fileSessionKey", getFileSessionKey(), "fileSessionKey");
		}

		if (getAction()== CmisAction.GET && isGetProperties() && isGetDocumentContent() && StringUtils.isEmpty(getFileSessionKey())) {
			throw new ConfigurationException("FileSessionKey should be specified");
		}

		// Legacy; check if the session should be created runtime (and thus for each call)
		if(getParameterList().hasParameter("authAlias") || getParameterList().hasParameter("username")) {
			runtimeSession = true;
		}

		// If any Header params exist, use RuntimeSessions
		if(!runtimeSession && getParameterList().stream().anyMatch(param -> param.getName().startsWith(HEADER_PARAM_PREFIX))) {
			runtimeSession = true;
		}
		if(!isKeepSession()) {
			runtimeSession = true;
		}

		if (runtimeSession) {
			log.info("using runtime session");
		}
	}

	/**
	 * Creates a session during JMV runtime, tries to retrieve parameters and falls back on the defaults when they can't be found
	 */
	public CloseableCmisSession createCmisSession(ParameterValueList pvl) throws SenderException {
		String cfAuthAlias = getParameterOverriddenAttributeValue(pvl, "authAlias", getAuthAlias());
		String cfUsername = getParameterOverriddenAttributeValue(pvl, "username", getUsername());
		String cdPassword = getParameterOverriddenAttributeValue(pvl, "password", getPassword());

		CredentialFactory cf = new CredentialFactory(cfAuthAlias, cfUsername, cdPassword);
		try {
			Map<String, String> headers = new HashMap<>();
			if (pvl != null) {
				pvl.stream()
						.filter(pv -> pv.getName().startsWith(HEADER_PARAM_PREFIX))
						.forEach(pv -> headers.put(pv.getName(), pv.asStringValue()));
			}
			return getSessionBuilder().build(cf.getUsername(), cf.getPassword(), headers);
		}
		catch (CmisSessionException e) {
			throw new SenderException(e);
		}
	}

	protected CmisSessionBuilder getSessionBuilder() {
		return sessionBuilder;
	}

	@Override
	public void start() {
		// If we don't need to create the session at JVM runtime, create to test the connection
		if (!runtimeSession) {
			try {
				globalSession = getSessionBuilder().build();
			} catch (CmisSessionException e) {
				throw new LifecycleException("unable to create cmis session", e);
			}
		}
	}

	@Override
	public void stop() {
		if (globalSession != null) {
			log.debug("Closing global CMIS session");
			globalSession.close();
			globalSession = null;
		}
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		CloseableCmisSession cmisSession = null;
		try {
			ParameterValueList pvl;
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new SenderException("Sender [" + getName() + "] caught exception evaluating parameters", e);
			}

			if(runtimeSession) {
				cmisSession = createCmisSession(pvl);
			} else {
				cmisSession = globalSession;
			}

			switch (getAction()) {
				case GET:
					return sendMessageForActionGet(cmisSession, message, session, pvl);
				case CREATE:
					return sendMessageForActionCreate(cmisSession, message, session, pvl);
				case DELETE:
					return sendMessageForActionDelete(cmisSession, message, session);
				case FIND:
					return sendMessageForActionFind(cmisSession, message);
				case UPDATE:
					return sendMessageForActionUpdate(cmisSession, message);
				case FETCH:
					return sendMessageForDynamicActions(cmisSession, message, session);
				case DYNAMIC:
					return sendMessageForDynamicActions(cmisSession, message, session);

				default:
					throw new SenderException("unknown action [" + getAction() + "]");
			}
		} finally {
			if (cmisSession != null && runtimeSession) {
				log.debug("Closing CMIS runtime session");
				session.scheduleCloseOnSessionExit(cmisSession);
				cmisSession = null;
			}
		}
	}

	private SenderResult sendMessageForActionGet(Session cmisSession, Message message, PipeLineSession session, ParameterValueList pvl) throws SenderException {
		if (Message.isEmpty(message)) {
			throw new SenderException("input string cannot be empty but must contain a documentId");
		}

		CmisObject object;
		try {
			object = getCmisObject(cmisSession, message);
		} catch (CmisObjectNotFoundException e) {
			String errorMessage= "document with id [" + message + "] not found";
			if (StringUtils.isNotEmpty(getResultOnNotFound())) {
				log.info("{}", errorMessage, e);
				return new SenderResult(getResultOnNotFound());
			}
			return new SenderResult(false, Message.nullMessage(), errorMessage, NOT_FOUND_FORWARD_NAME);
		}

		Document document = (Document) object;

		boolean getProperties = isGetProperties();
		boolean getDocumentContent = isGetDocumentContent();
		if (pvl != null) {
			if(pvl.contains("getProperties"))
				getProperties = pvl.get("getProperties").asBooleanValue(isGetProperties());
			if(pvl.contains("getDocumentContent"))
				getDocumentContent = pvl.get("getDocumentContent").asBooleanValue(isGetDocumentContent());
		}

		if (getProperties) {
			if (getDocumentContent) {
				Message content = getMessageFromContentStream(document.getContentStream());

				content.closeOnCloseOf(session);
				session.put(getFileSessionKey(), content);
			}

			XmlBuilder propertiesXml = new XmlBuilder("properties");
			for (Iterator<Property<?>> it = document.getProperties().iterator(); it.hasNext();) {
				Property<?> property = it.next();
				propertiesXml.addSubElement(CmisUtils.getPropertyXml(property));
			}
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(propertiesXml);
			return new SenderResult(cmisXml.asMessage());
		}

		Message content = getMessageFromContentStream(document.getContentStream());
		content.closeOnCloseOf(session);

		if (StringUtils.isNotEmpty(getFileSessionKey())) {
			session.put(getFileSessionKey(), content);
			return new SenderResult(Message.nullMessage());
		}

		return new SenderResult(content);
	}

	private Message getMessageFromContentStream(ContentStream contentStream) {
		InputStream inputStream = contentStream.getStream();
		MessageContext context = new MessageContext();
		context.withName(contentStream.getFileName()).withMimeType(contentStream.getMimeType());
		return new Message(inputStream, context);
	}

	private SenderResult sendMessageForActionCreate(Session cmisSession, Message message, PipeLineSession session, ParameterValueList pvl) throws SenderException {
		String fileName = session.getString(getParameterOverriddenAttributeValue(pvl, "filenameSessionKey", getFilenameSessionKey()));

		String mediaType;
		Map<String, Object> props = new HashMap<>();
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
			throw new SenderException("exception parsing [" + message + "]", e);
		}

		if (StringUtils.isEmpty(mediaType)) {
			mediaType = getDefaultMediaType();
		}

		ContentStream contentStream;
		try {
			Message inputFromSessionKey = session.getMessage( getParameterOverriddenAttributeValue(pvl, "fileSessionKey", getFileSessionKey()) );

			long fileLength = inputFromSessionKey.size();
			contentStream = cmisSession.getObjectFactory().createContentStream(fileName, fileLength, mediaType, inputFromSessionKey.asInputStream());
		} catch (IOException e) {
			throw new SenderException(e);
		}

		if (isUseRootFolder()) {
			Folder folder = cmisSession.getRootFolder();
			Document document = folder.createDocument(props, contentStream, VersioningState.NONE);
			log.debug("created new document [{}]", document.getId());
			return new SenderResult(document.getId());
		}
		ObjectId objectId = cmisSession.createDocument(props, null, contentStream, VersioningState.NONE);
		log.debug("created new document [{}]", objectId.getId());
		return new SenderResult(objectId.getId());
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
				} else if (StringUtils.isEmpty(typeAttr) || "string".equalsIgnoreCase(typeAttr)) {
					props.put(nameAttr, property);
				} else if ("integer".equalsIgnoreCase(typeAttr)) {
					props.put(nameAttr, new BigInteger(property));
				} else if ("boolean".equalsIgnoreCase(typeAttr)) {
					props.put(nameAttr, Boolean.parseBoolean(property));
				} else if ("datetime".equalsIgnoreCase(typeAttr)) {
					String formatStringAttr = propertyElement.getAttribute("formatString");
					if (StringUtils.isEmpty(formatStringAttr)) {
						formatStringAttr = CmisUtils.FORMATSTRING_BY_DEFAULT;
					}
					//TODO to be removed in a few versions
					if(AppConstants.getInstance().getBoolean("cmissender.processproperties.legacydateformat", false)) {
						formatStringAttr = "yyyy-MM-dd HH:mm:ss";
					}
					DateTimeFormatter formatter = DateFormatUtils.getDateTimeFormatterWithOptionalComponents(formatStringAttr);
					GregorianCalendar calendar = new GregorianCalendar();

					try {
						TemporalAccessor parse = formatter.parse(property);
						calendar.setTimeInMillis(Instant.from(parse).getEpochSecond());
					} catch (DateTimeParseException e) {
						throw new SenderException("exception parsing date [" + property + "] using formatString [" + formatStringAttr + "]", e);
					}
					props.put(nameAttr, calendar);
				} else {
					log.warn("unknown type [{}], assuming 'string'", typeAttr);
					props.put(nameAttr, property);
				}
				if (log.isDebugEnabled()) {
					log.debug("set property name [{}] value [{}]", nameAttr, property);
				}
			} else {
				log.debug("empty property found, ignoring");
			}
		}
	}

	private SenderResult sendMessageForActionDelete(Session cmisSession, Message message, PipeLineSession session) throws SenderException {
		if (Message.isEmpty(message)) {
			throw new SenderException("input string cannot be empty but must contain a documentId");
		}
		CmisObject object = null;
		try {
			object = getCmisObject(cmisSession, message);
		} catch (CmisObjectNotFoundException e) {
			String errorMessage="document with id [" + message + "] not found";
			if (StringUtils.isNotEmpty(getResultOnNotFound())) {
				log.info(errorMessage, e);
				return new SenderResult(getResultOnNotFound());
			}
			return new SenderResult(false, Message.nullMessage(), errorMessage, NOT_FOUND_FORWARD_NAME);
		}
		if (object.hasAllowableAction(Action.CAN_DELETE_OBJECT)) { //// You can delete
			Document suppDoc = (Document) object;
			suppDoc.delete(true);
			String messageID = session==null ? null : session.getMessageId();
			return new SenderResult(messageID);

		}
		//// You can't delete
		throw new SenderException("Document cannot be deleted");
	}

	private SenderResult sendMessageForActionFind(Session cmisSession, Message message) throws SenderException {
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

		OperationContext operationContext = cmisSession.createOperationContext();
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
		ItemIterable<QueryResult> q = cmisSession.query(statement, sav, operationContext);

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
		return new SenderResult(cmisXml.asMessage());
	}

	private CmisObject getCmisObject(Session cmisSession, Message message) throws SenderException, CmisObjectNotFoundException {
		if (XmlUtils.isWellFormed(message, "cmis")) {
			try {
				Element queryElement = XmlUtils.buildElement(message);
				return getCmisObject(cmisSession, queryElement);
			} catch (DomBuilderException e) {
				throw new SenderException("unable to build cmis xml from sender input message", e);
			}
		}
		throw new SenderException("unable to build cmis xml from sender input message");
	}

	private CmisObject getCmisObject(Session cmisSession, Element queryElement) throws CmisObjectNotFoundException {
		String filter = XmlUtils.getChildTagAsString(queryElement, "filter");
		boolean includeAllowableActions = XmlUtils.getChildTagAsBoolean(queryElement, "includeAllowableActions");
		boolean includePolicies = XmlUtils.getChildTagAsBoolean(queryElement, "includePolicies");
		boolean includeAcl = XmlUtils.getChildTagAsBoolean(queryElement, "includeAcl");

		OperationContext operationContext = cmisSession.createOperationContext();

		if (StringUtils.isNotEmpty(filter))
			operationContext.setFilterString(filter);
		operationContext.setIncludeAllowableActions(includeAllowableActions);
		operationContext.setIncludePolicies(includePolicies);
		operationContext.setIncludeAcls(includeAcl);

		String objectIdstr = XmlUtils.getChildTagAsString(queryElement, "objectId");
		if(objectIdstr == null)
			objectIdstr = XmlUtils.getChildTagAsString(queryElement, "id");

		if(objectIdstr != null) {
			return cmisSession.getObject(cmisSession.createObjectId(objectIdstr), operationContext);
		}
		//Ok, id can still be null, perhaps its a path?
		String path = XmlUtils.getChildTagAsString(queryElement, "path");
		return cmisSession.getObjectByPath(path, operationContext);
	}

	private SenderResult sendMessageForDynamicActions(Session cmisSession, Message message, PipeLineSession session) throws SenderException {

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

		CmisEvent event = CmisEvent.GET_OBJECT;
		try {
			String cmisEvent = session.getString(CmisEventDispatcher.CMIS_EVENT_KEY);
			if(StringUtils.isNotEmpty(cmisEvent)) {
				event = EnumUtils.parse(CmisEvent.class, cmisEvent, true);
			}
		} catch (IllegalArgumentException e) {
			throw new SenderException("unable to parse CmisEvent", e);
		}

		switch (event) {
			case DELETE_OBJECT:
				getCmisObject(cmisSession, requestElement).delete(true);
				resultXml.addAttribute("deleted", true);
				break;

			case CREATE_DOCUMENT:
				Map<String, Object> props = new HashMap<>();
				Element propertiesElement = XmlUtils.getFirstChildTag(requestElement, "properties");
				if (propertiesElement != null) {
					processProperties(propertiesElement, props);
				}

				ObjectId folderId = null;
				String folderIdstr = XmlUtils.getChildTagAsString(requestElement, "folderId");
				if(StringUtils.isNotEmpty(folderIdstr))
					folderId = cmisSession.createObjectId(folderIdstr);

				String versioningStatestr = XmlUtils.getChildTagAsString(requestElement, "versioningState");
				VersioningState state = VersioningState.valueOf(versioningStatestr);

				Element contentStreamXml = XmlUtils.getFirstChildTag(requestElement, "contentStream");
				Message stream = session.getMessage("ContentStream");
				String fileName = contentStreamXml.getAttribute("filename");
				long fileLength = Long.parseLong(contentStreamXml.getAttribute("length"));
				String mediaType = contentStreamXml.getAttribute("mimeType");
				ContentStream contentStream;

				try {
					contentStream = cmisSession.getObjectFactory().createContentStream(fileName, fileLength, mediaType, stream.asInputStream());
				} catch (IOException e) {
					throw new SenderException("unable to parse ContentStream as InputStream", e);
				}

				ObjectId createdDocumentId = cmisSession.createDocument(props, folderId, contentStream, state);
				XmlBuilder cmisId = new XmlBuilder("id");
				cmisId.setValue(createdDocumentId.getId());
				resultXml.addSubElement(cmisId);
				break;

			case UPDATE_PROPERTIES:
				Map<String, Object> propss = new HashMap<>();
				Element propertiesElements = XmlUtils.getFirstChildTag(requestElement, "properties");
				if (propertiesElements != null) {
					processProperties(propertiesElements, propss);
				}
				getCmisObject(cmisSession, requestElement).updateProperties(propss);

				resultXml.addAttribute("updated", true);
				break;

			case GET_CONTENTSTREAM:
				String streamId = XmlUtils.getChildTagAsString(requestElement, "streamId");

				long offsetStr = XmlUtils.getChildTagAsLong(requestElement, "offset");
				BigInteger offset = BigInteger.valueOf(offsetStr);
				long lengthStr = XmlUtils.getChildTagAsLong(requestElement, "length");
				BigInteger length = BigInteger.valueOf(lengthStr);

				ObjectId objectIdforContentStream = getCmisObject(cmisSession, requestElement);
				ContentStream getContentStream = cmisSession.getContentStream(objectIdforContentStream, streamId, offset, length);

				XmlBuilder objectIdStream = new XmlBuilder("id");
				objectIdStream.setValue(objectIdforContentStream.getId());
				resultXml.addSubElement(objectIdStream);

				XmlBuilder contentStreamBuilder = new XmlBuilder("contentStream");
				contentStreamBuilder.addAttribute("filename", getContentStream.getFileName());
				contentStreamBuilder.addAttribute("length", getContentStream.getLength());
				contentStreamBuilder.addAttribute("mimeType", getContentStream.getMimeType());
				resultXml.addSubElement(contentStreamBuilder);
				session.put("ContentStream", getContentStream.getStream());
				break;

			case GET_TYPE_DEFINITION:
				String typeId = XmlUtils.getChildTagAsString(requestElement, "typeId");
				ObjectType objectType = cmisSession.getTypeDefinition(typeId);
				XmlBuilder typesXml = new XmlBuilder("typeDefinitions");
				typesXml.addSubElement(CmisUtils.typeDefinition2Xml(objectType));
				resultXml.addSubElement(typesXml);
				break;

			case GET_TYPE_DESCENDANTS:
				String typeDescId = XmlUtils.getChildTagAsString(requestElement, "typeId");
				int depth = Integer.parseInt(XmlUtils.getChildTagAsString(requestElement, "depth"));
				boolean includePropertyDefinitions = XmlUtils.getChildTagAsBoolean(requestElement, "includePropertyDefinitions");
				List<Tree<ObjectType>> objectTypes = cmisSession.getTypeDescendants(typeDescId, depth, includePropertyDefinitions);
				resultXml.addSubElement(CmisUtils.typeDescendants2Xml(objectTypes));
				break;

			case GET_REPOSITORIES:
				List<RepositoryInfo> repositories = cmisSession.getBinding().getRepositoryService().getRepositoryInfos(null);
				XmlBuilder repositoriesXml = new XmlBuilder("repositories");

				for (RepositoryInfo repository : repositories) {
					repositoriesXml.addSubElement(CmisUtils.repositoryInfo2xml(repository));
				}
				resultXml.addSubElement(repositoriesXml);
				break;

			case GET_REPOSITORY_INFO:
				String repositoryId = XmlUtils.getChildTagAsString(requestElement, "repositoryId");
				RepositoryInfo repository = cmisSession.getBinding().getRepositoryService().getRepositoryInfo(repositoryId, null);

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

				ObjectList result = cmisSession.getBinding().getDiscoveryService().query(repositoryQueryId, statement,
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

				ObjectInFolderList oifs = cmisSession.getBinding().getNavigationService().getChildren(rid, fid, getChildren_repositoryFilter,
						getChildren_repositoryOrderBy, getChildren_includeAllowableActions, getChildren_includeRelationships,
						getChildren_renditionFilter, getChildren_includePathSegment, getChildren_maxItems, getChildren_skipCount, null);

				resultXml.addSubElement(CmisUtils.objectInFolderList2xml(oifs));
				break;

			case GET_PROPERTIES:
			case GET_OBJECT:
			case GET_OBJECT_BY_PATH:
			case GET_ALLOWABLE_ACTIONS:
				CmisObject object = getCmisObject(cmisSession, requestElement);
				session.put(CmisUtils.ORIGINAL_OBJECT_KEY, object);
				CmisUtils.cmisObject2Xml(resultXml, object);
				break;
			default:
				throw new CmisNotSupportedException("Operation not implemented");
		}

		return new SenderResult(resultXml.asMessage());
	}

	private SenderResult sendMessageForActionUpdate(Session cmisSession, Message message) throws SenderException{
		String objectId = null;
		Map<String, Object> props = new HashMap<>();
		Element cmisElement;
		try {
			if (XmlUtils.isWellFormed(message, "cmis")) {
				cmisElement = XmlUtils.buildElement(message);
			} else {
				cmisElement = XmlUtils.buildElement("<cmis/>");
			}

			objectId = XmlUtils.getChildTagAsString(cmisElement, "id");
			Element propertiesElement = XmlUtils.getFirstChildTag(cmisElement, "properties");
			if (propertiesElement != null) {
				processProperties(propertiesElement, props);
			}
		} catch (DomBuilderException e) {
			throw new SenderException("exception parsing [" + message + "]", e);
		}

		CmisObject object = null;
		try {
			object = cmisSession.getObject(cmisSession.createObjectId(objectId));
		} catch (CmisObjectNotFoundException e) {
			String errorMessage="document with id [" + message + "] not found";
			if (StringUtils.isNotEmpty(getResultOnNotFound())) {
				log.info(errorMessage, e);
				return new SenderResult(getResultOnNotFound());
			}
			return new SenderResult(false, Message.nullMessage(), errorMessage, NOT_FOUND_FORWARD_NAME);
		}
		object.updateProperties(props);
		return new SenderResult(object.getId());
	}


	/** Specifies action to perform */
	@Mandatory
	public void setAction(CmisAction action) {
		this.action = action;
	}

	/**
	 * The maximum number of concurrent connections
	 * @ff.default 10
	 */
	public void setMaxConnections(int i) {
		sessionBuilder.setMaxConnections(i);
	}

	/**
	 * READ_TIMEOUT timeout in MS.
	 * Defaults to 10000, inherited from {@link AbstractHttpSession#setTimeout(int) HttpSender#setTimeout}.
	 * @ff.default 10000
	 */
	public void setTimeout(int i) {
		sessionBuilder.setTimeout(i);
	}

	/** URL to connect to */
	public void setUrl(String url) {
		sessionBuilder.setUrl(url);
	}

	/** Repository ID */
	public void setRepository(String repository) {
		sessionBuilder.setRepository(repository);
	}

	/** Alias used to obtain credentials for authentication to host */
	public void setAuthAlias(String authAlias) {
		sessionBuilder.setAuthAlias(authAlias);
		this.authAlias = authAlias;
	}

	/** Username used in authentication to host */
	public void setUsername(String username) {
		sessionBuilder.setUsername(username);
		this.username = username;
	}

	/** Password used in authentication to host */
	public void setPassword(String password) {
		sessionBuilder.setPassword(password);
		this.password = password;
	}

	/**
	 * BindingType CMIS protocol to use
	 */
	@Mandatory
	public void setBindingType(BindingTypes bindingType) {
		sessionBuilder.setBindingType(bindingType);
	}

	/** If <code>action=create</code> the sessionKey that contains the file to use. If <code>action=get</code> and <code>getProperties=true</code> the sessionKey to store the result in */
	public void setFileSessionKey(String string) {
		fileSessionKey = string;
	}

	/** If <code>action=create</code> the session key that contains the name of the file to use. If not set, the value of the property <code>filename</code> from the input message is used */
	public void setFilenameSessionKey(String string) {
		filenameSessionKey = string;
	}

	/**
	 * If <code>action=create</code> the mime type used to store the document when it's not set in the input message by a property
	 * @ff.default 'application/octet-stream'
	 */
	public void setDefaultMediaType(String string) {
		defaultMediaType = string;
	}

	/**
	 * (Only used when <code>action=get</code>). If true, the content of the document is put to <code>FileSessionKey</code> and all document properties are put in the result as a xml string
	 * @ff.default false
	 */
	public void setGetProperties(boolean b) {
		getProperties = b;
	}

	/**
	 * (Only used when <code>action=get</code>). If true, the attachment for the document is the sender result or, if set, stored in <code>fileSessionKey</code>. If false, only the properties are returned
	 * @ff.default true
	 */
	public void setGetDocumentContent(boolean getDocumentContent) {
		this.getDocumentContent = getDocumentContent;
	}

	/**
	 * (Only used when <code>action=create</code>). If true, the document is created in the root folder of the repository. Otherwise the document is created in the repository
	 * @ff.default true
	 */
	public void setUseRootFolder(boolean b) {
		useRootFolder = b;
	}

	/** (Only used when <code>action=get</code>) result returned when no document was found for the given id (e.g. '[not_found]'). If empty then 'notFound' is returned as forward name */
	@Deprecated(forRemoval = true, since = "7.9.0")
	@ConfigurationWarning("configure forward 'notFound' instead")
	public void setResultOnNotFound(String string) {
		resultOnNotFound = string;
	}

	/**
	 * If true, the session is not closed at the end and it will be used in the next call
	 * @ff.default true
	 */
	public void setKeepSession(boolean keepSession) {
		this.keepSession = keepSession;
	}

	/** Override entrypoint WSDL by reading it from the classpath, overrides url attribute */
	public void setOverrideEntryPointWSDL(String overrideEntryPointWSDL) {
		sessionBuilder.setOverrideEntryPointWSDL(overrideEntryPointWSDL);
	}

	@Override
	public void setKeystore(String keystore) {
		sessionBuilder.setKeystore(keystore);
	}
	@Override
	public String getKeystore() {
		return sessionBuilder.getKeystore();
	}

	@Override
	public void setKeystoreType(KeystoreType keystoreType) {
		sessionBuilder.setKeystoreType(keystoreType);
	}
	@Override
	public KeystoreType getKeystoreType() {
		return sessionBuilder.getKeystoreType();
	}

	@Override
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		sessionBuilder.setKeystoreAuthAlias(keystoreAuthAlias);
	}
	@Override
	public String getKeystoreAuthAlias() {
		return sessionBuilder.getKeystoreAuthAlias();
	}

	@Override
	public void setKeystorePassword(String keystorePassword) {
		sessionBuilder.setKeystorePassword(keystorePassword);
	}
	@Override
	public String getKeystorePassword() {
		return sessionBuilder.getKeystorePassword();
	}

	@Override
	public void setKeystoreAlias(String keystoreAlias) {
		sessionBuilder.setKeystoreAlias(keystoreAlias);
	}
	@Override
	public String getKeystoreAlias() {
		return sessionBuilder.getKeystoreAlias();
	}

	@Override
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		sessionBuilder.setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}
	@Override
	public String getKeystoreAliasAuthAlias() {
		return sessionBuilder.getKeystoreAliasAuthAlias();
	}

	@Override
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		sessionBuilder.setKeystoreAliasPassword(keystoreAliasPassword);
	}
	@Override
	public String getKeystoreAliasPassword() {
		return sessionBuilder.getKeystoreAliasPassword();
	}

	@Override
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		sessionBuilder.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	@Override
	public String getKeyManagerAlgorithm() {
		return sessionBuilder.getKeyManagerAlgorithm();
	}


	@Override
	public void setTruststore(String truststore) {
		sessionBuilder.setTruststore(truststore);
	}
	@Override
	public String getTruststore() {
		return sessionBuilder.getTruststore();
	}

	@Override
	public void setTruststoreType(KeystoreType truststoreType) {
		sessionBuilder.setTruststoreType(truststoreType);
	}
	@Override
	public KeystoreType getTruststoreType() {
		return sessionBuilder.getTruststoreType();
	}


	@Override
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		sessionBuilder.setTruststoreAuthAlias(truststoreAuthAlias);
	}
	@Override
	public String getTruststoreAuthAlias() {
		return sessionBuilder.getTruststoreAuthAlias();
	}

	@Override
	public void setTruststorePassword(String truststorePassword) {
		sessionBuilder.setTruststorePassword(truststorePassword);
	}
	@Override
	public String getTruststorePassword() {
		return sessionBuilder.getTruststorePassword();
	}

	@Override
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		sessionBuilder.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	@Override
	public String getTrustManagerAlgorithm() {
		return sessionBuilder.getTrustManagerAlgorithm();
	}

	@Unsafe
	@Override
	public void setVerifyHostname(boolean verifyHostname) {
		sessionBuilder.setVerifyHostname(verifyHostname);
	}
	@Override
	public boolean isVerifyHostname() {
		return sessionBuilder.isVerifyHostname();
	}

	@Unsafe
	@Override
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		sessionBuilder.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	@Override
	public boolean isAllowSelfSignedCertificates() {
		return sessionBuilder.isAllowSelfSignedCertificates();
	}

	@Unsafe
	@Override
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		sessionBuilder.setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}
	@Override
	public boolean isIgnoreCertificateExpiredException() {
		return sessionBuilder.isIgnoreCertificateExpiredException();
	}

	/** Proxy host url */
	public void setProxyHost(String proxyHost) {
		sessionBuilder.setProxyHost(proxyHost);
	}

	/**
	 * Proxy host port
	 * @ff.default 80
	 */
	public void setProxyPort(int proxyPort) {
		sessionBuilder.setProxyPort(proxyPort);
	}

	/** Alias used to obtain credentials for authentication to proxy */
	public void setProxyAuthAlias(String proxyAuthAlias) {
		sessionBuilder.setProxyAuthAlias(proxyAuthAlias);
	}

	/** Proxy Username */
	public void setProxyUsername(String proxyUsername) {
		sessionBuilder.setProxyUsername(proxyUsername);
	}
	/** Proxy Password */
	public void setProxyPassword(String proxyPassword) {
		sessionBuilder.setProxyPassword(proxyPassword);
	}
}
