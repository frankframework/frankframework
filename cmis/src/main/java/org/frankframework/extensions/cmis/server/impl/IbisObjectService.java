/*
   Copyright 2019-2020 Nationale-Nederlanden

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
package org.frankframework.extensions.cmis.server.impl;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.BulkUpdateObjectIdAndChangeToken;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.extensions.cmis.CmisUtils;
import org.frankframework.extensions.cmis.server.CmisEvent;
import org.frankframework.extensions.cmis.server.CmisEventDispatcher;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Wrapper that delegates when a matching CmisEvent is present.
 *
 * @author Niels
 */
@Log4j2
public class IbisObjectService implements ObjectService {

	public static final String REPOSITORY_ID_ELEMENT = "repositoryId";
	private final ObjectService objectService;
	private final CmisEventDispatcher eventDispatcher = CmisEventDispatcher.getInstance();
	private final CallContext callContext;

	public IbisObjectService(ObjectService objectService, CallContext callContext) {
		this.objectService = objectService;
		this.callContext = callContext;
	}

	private XmlBuilder buildXml(String name, Object value) {
		return CmisUtils.buildXml(name, value);
	}

	@Override
	public String createDocument(String repositoryId, Properties properties,
			String folderId, ContentStream contentStream,
			VersioningState versioningState, List<String> policies,
			Acl addAces, Acl removeAces, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.CREATE_DOCUMENT)) {
			return objectService.createDocument(repositoryId, properties, folderId, contentStream, versioningState, policies, addAces, removeAces, extension);
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			cmisXml.addSubElement(buildXml("folderId", folderId));
			cmisXml.addSubElement(buildXml("versioningState", versioningState));

			XmlBuilder contentStreamXml = new XmlBuilder("contentStream");
			contentStreamXml.addAttribute("filename", contentStream.getFileName());
			contentStreamXml.addAttribute("length", contentStream.getLength());
			contentStreamXml.addAttribute("mimeType", contentStream.getMimeType());
			cmisXml.addSubElement(contentStreamXml);

			XmlBuilder propertiesXml = new XmlBuilder("properties");
			for (PropertyData<?> propertyData : properties.getPropertyList()) {
				propertiesXml.addSubElement(CmisUtils.getPropertyXml(propertyData));
			}
			cmisXml.addSubElement(propertiesXml);

			PipeLineSession context = new PipeLineSession();
			context.put("ContentStream", contentStream.getStream());
			context.put(CmisUtils.CMIS_CALLCONTEXT_KEY, callContext);
			Element result = eventDispatcher.trigger(CmisEvent.CREATE_DOCUMENT, cmisXml.asXmlString(), context);
			return XmlUtils.getChildTagAsString(result, "id");
		}
	}

	@Override
	public String createDocumentFromSource(String repositoryId, String sourceId, Properties properties, String folderId, VersioningState versioningState,
			List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
		return objectService.createDocumentFromSource(repositoryId, sourceId, properties, folderId, versioningState, policies, addAces, removeAces, extension);
	}

	@Override
	public String createFolder(String repositoryId, Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.CREATE_FOLDER)) {
			return objectService.createFolder(repositoryId, properties, folderId, policies, addAces, removeAces, extension);
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			cmisXml.addSubElement(buildXml("folderId", folderId));
			cmisXml.addSubElement(buildXml("policies", policies));

			XmlBuilder propertiesXml = new XmlBuilder("properties");
			for (PropertyData<?> propertyData : properties.getPropertyList()) {
				propertiesXml.addSubElement(CmisUtils.getPropertyXml(propertyData));
			}
			cmisXml.addSubElement(propertiesXml);

			Element result = eventDispatcher.trigger(CmisEvent.CREATE_FOLDER, cmisXml.asXmlString(), callContext);
			return XmlUtils.getChildTagAsString(result, "id");
		}
	}

	@Override
	public String createRelationship(String repositoryId, Properties properties, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
		return objectService.createRelationship(repositoryId, properties, policies, addAces, removeAces, extension);
	}

	@Override
	public String createPolicy(String repositoryId, Properties properties, String folderId, List<String> policies, Acl addAces,
			Acl removeAces, ExtensionsData extension) {
		return objectService.createPolicy(repositoryId, properties, folderId, policies, addAces, removeAces, extension);
	}

	@Override
	public String createItem(String repositoryId, Properties properties,
			String folderId, List<String> policies, Acl addAces,
			Acl removeAces, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.CREATE_ITEM)) {
			return objectService.createItem(repositoryId, properties, folderId, policies, addAces, removeAces, extension);
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			cmisXml.addSubElement(buildXml("folderId", folderId));
			cmisXml.addSubElement(buildXml("policies", policies));

			XmlBuilder propertiesXml = new XmlBuilder("properties");
			for (PropertyData<?> propertyData : properties.getPropertyList()) {
				propertiesXml.addSubElement(CmisUtils.getPropertyXml(propertyData));
			}
			cmisXml.addSubElement(propertiesXml);

			Element result = eventDispatcher.trigger(CmisEvent.CREATE_ITEM, cmisXml.asXmlString(), callContext);
			return XmlUtils.getChildTagAsString(result, "id");
		}
	}

	@Override
	public AllowableActions getAllowableActions(String repositoryId, String objectId, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.GET_ALLOWABLE_ACTIONS)) {
			return objectService.getAllowableActions(repositoryId, objectId, extension);
		}
		else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			cmisXml.addSubElement(buildXml("objectId", objectId));
			AllowableActionsImpl allowableActions = new AllowableActionsImpl();

			Element cmisElement = eventDispatcher.trigger(CmisEvent.GET_ALLOWABLE_ACTIONS, cmisXml.asXmlString(), callContext);
			Element allowableActionsElem = XmlUtils.getFirstChildTag(cmisElement, "allowableActions");
			if(allowableActionsElem != null) {
				Set<Action> actions = EnumSet.noneOf(Action.class);

				for (Node node : XmlUtils.getChildTags(allowableActionsElem, "action")) {
					String property = XmlUtils.getStringValue((Element) node);
					actions.add(Action.fromValue(property));
				}

				allowableActions.setAllowableActions(actions);
			}
			return allowableActions;
		}
	}

	@Override
	public ObjectData getObject(String repositoryId, String objectId, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
			String renditionFilter, Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extensions) {

		if(!eventDispatcher.contains(CmisEvent.GET_OBJECT)) {
			return objectService.getObject(repositoryId, objectId, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl, extensions);
		} else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			cmisXml.addSubElement(buildXml("objectId", objectId));
			cmisXml.addSubElement(buildXml("filter", filter));
			cmisXml.addSubElement(buildXml("includeAllowableActions", includeAllowableActions));
			cmisXml.addSubElement(buildXml("includePolicies", includePolicyIds));
			cmisXml.addSubElement(buildXml("includeAcl", includeAcl));

			PipeLineSession context = new PipeLineSession();
			context.put(CmisUtils.CMIS_CALLCONTEXT_KEY, callContext);
			Element cmisElement = eventDispatcher.trigger(CmisEvent.GET_OBJECT, cmisXml.asXmlString(), context);

			return CmisUtils.xml2ObjectData(cmisElement, context);
		}
	}

	@Override
	public Properties getProperties(String repositoryId, String objectId, String filter, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.GET_PROPERTIES)) {
			return objectService.getProperties(repositoryId, objectId, filter, extension);
		} else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			cmisXml.addSubElement(buildXml("objectId", objectId));
			cmisXml.addSubElement(buildXml("filter", filter));
			try {

				Element result = eventDispatcher.trigger(CmisEvent.GET_PROPERTIES, cmisXml.asXmlString(), callContext);

				return CmisUtils.processProperties(result);
			}
			catch(Exception e) {
				log.error("error creating CMIS objectData: {}", e.getMessage(), e.getCause());
			}
			return new PropertiesImpl();
		}
	}

	@Override
	public List<RenditionData> getRenditions(String repositoryId, String objectId, String renditionFilter, BigInteger maxItems, BigInteger skipCount,
			ExtensionsData extension) {
		return objectService.getRenditions(repositoryId, objectId, renditionFilter, maxItems, skipCount, extension);
	}

	@Override
	public ObjectData getObjectByPath(String repositoryId, String path,
			String filter, Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter,
			Boolean includePolicyIds, Boolean includeAcl,
			ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.GET_OBJECT_BY_PATH)) {
			return objectService.getObjectByPath(repositoryId, path, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl, extension);
		} else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			cmisXml.addSubElement(buildXml("path", path));
			cmisXml.addSubElement(buildXml("filter", filter));
			cmisXml.addSubElement(buildXml("includeAllowableActions", includeAllowableActions));
			cmisXml.addSubElement(buildXml("includeRelationships", includeRelationships));
			cmisXml.addSubElement(buildXml("renditionFilter", renditionFilter));
			cmisXml.addSubElement(buildXml("includePolicyIds", includePolicyIds));
			cmisXml.addSubElement(buildXml("includeAcl", includeAcl));

			PipeLineSession context = new PipeLineSession();
			context.put(CmisUtils.CMIS_CALLCONTEXT_KEY, callContext);
			Element cmisElement = eventDispatcher.trigger(CmisEvent.GET_OBJECT_BY_PATH, cmisXml.asXmlString(), context);

			return CmisUtils.xml2ObjectData(cmisElement, context);
		}
	}

	@Override
	public void updateProperties(String repositoryId, Holder<String> objectId, Holder<String> changeToken, Properties properties, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.UPDATE_PROPERTIES)) {
			objectService.updateProperties(repositoryId, objectId, changeToken, properties, extension);
		} else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			if(objectId != null)
				cmisXml.addSubElement(buildXml("objectId", objectId.getValue()));
			if(changeToken != null)
				cmisXml.addSubElement(buildXml("changeToken", changeToken.getValue()));

			XmlBuilder propertiesXml = new XmlBuilder("properties");
			for (PropertyData<?> propertyData : properties.getPropertyList()) {
				propertiesXml.addSubElement(CmisUtils.getPropertyXml(propertyData));
			}
			cmisXml.addSubElement(propertiesXml);

			eventDispatcher.trigger(CmisEvent.UPDATE_PROPERTIES, cmisXml.asXmlString(), callContext);
		}
	}

	@Override
	public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(String repositoryId, List<BulkUpdateObjectIdAndChangeToken> objectIdsAndChangeTokens,
			Properties properties, List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds, ExtensionsData extension) {
		return objectService.bulkUpdateProperties(repositoryId, objectIdsAndChangeTokens, properties, addSecondaryTypeIds, removeSecondaryTypeIds, extension);
	}

	@Override
	public void moveObject(String repositoryId, Holder<String> objectId, String targetFolderId, String sourceFolderId, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.MOVE_OBJECT)) {
			objectService.moveObject(repositoryId, objectId, targetFolderId, sourceFolderId, extension);
		} else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			if(objectId != null)
				cmisXml.addSubElement(buildXml("objectId", objectId.getValue()));
			cmisXml.addSubElement(buildXml("targetFolderId", targetFolderId));
			cmisXml.addSubElement(buildXml("sourceFolderId", sourceFolderId));

			eventDispatcher.trigger(CmisEvent.MOVE_OBJECT, cmisXml.asXmlString(), callContext);
		}
	}

	@Override
	public void deleteObject(String repositoryId, String objectId, Boolean allVersions, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.DELETE_OBJECT)) {
			objectService.deleteObject(repositoryId, objectId, allVersions, extension);
		} else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			cmisXml.addSubElement(buildXml("objectId", objectId));
			cmisXml.addSubElement(buildXml("allVersions", allVersions));

			eventDispatcher.trigger(CmisEvent.DELETE_OBJECT, cmisXml.asXmlString(), callContext);
		}
	}

	@Override
	public FailedToDeleteData deleteTree(String repositoryId, String folderId, Boolean allVersions, UnfileObject unfileObjects,
			Boolean continueOnFailure, ExtensionsData extension) {
		return objectService.deleteTree(repositoryId, folderId, allVersions, unfileObjects, continueOnFailure, extension);
	}

	@Override
	public ContentStream getContentStream(String repositoryId, String objectId,
			String streamId, BigInteger offset, BigInteger length, ExtensionsData extension) {

		if(!eventDispatcher.contains(CmisEvent.GET_CONTENTSTREAM)) {
			return objectService.getContentStream(repositoryId, objectId, streamId, offset, length, extension);
		} else {
			XmlBuilder cmisXml = new XmlBuilder("cmis");
			cmisXml.addSubElement(buildXml(REPOSITORY_ID_ELEMENT, repositoryId));
			cmisXml.addSubElement(buildXml("objectId", objectId));
			cmisXml.addSubElement(buildXml("streamId", streamId));
			cmisXml.addSubElement(buildXml("offset", offset));
			cmisXml.addSubElement(buildXml("length", length));

			PipeLineSession context = new PipeLineSession();
			context.put(CmisUtils.CMIS_CALLCONTEXT_KEY, callContext);
			Element cmisResult = eventDispatcher.trigger(CmisEvent.GET_CONTENTSTREAM, cmisXml.asXmlString(), context);

			Element contentStreamXml = XmlUtils.getFirstChildTag(cmisResult, "contentStream");
			InputStream stream = (InputStream) context.get("ContentStream");
			String fileName = contentStreamXml.getAttribute("filename");
			String mediaType = contentStreamXml.getAttribute("mimeType");
			long longLength = Long.parseLong(contentStreamXml.getAttribute("length"));
			BigInteger fileLength = BigInteger.valueOf(longLength);

			return new ContentStreamImpl(fileName, fileLength, mediaType, stream);
		}
	}

	@Override
	public void setContentStream(String repositoryId, Holder<String> objectId, Boolean overwriteFlag, Holder<String> changeToken,
			ContentStream contentStream, ExtensionsData extension) {
		objectService.setContentStream(repositoryId, objectId, overwriteFlag, changeToken, contentStream, extension);
	}

	@Override
	public void deleteContentStream(String repositoryId, Holder<String> objectId, Holder<String> changeToken, ExtensionsData extension) {
		objectService.deleteContentStream(repositoryId, objectId, changeToken, extension);
	}

	@Override
	public void appendContentStream(String repositoryId, Holder<String> objectId, Holder<String> changeToken,
			ContentStream contentStream, boolean isLastChunk, ExtensionsData extension) {
		objectService.appendContentStream(repositoryId, objectId, changeToken, contentStream, isLastChunk, extension);
	}
}
