/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.extensions.cmis.server;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.BulkUpdateObjectIdAndChangeToken;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlListImpl;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.ObjectInfo;
import org.apache.chemistry.opencmis.commons.spi.AclService;
import org.apache.chemistry.opencmis.commons.spi.DiscoveryService;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.commons.spi.MultiFilingService;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.apache.chemistry.opencmis.commons.spi.PolicyService;
import org.apache.chemistry.opencmis.commons.spi.RelationshipService;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.chemistry.opencmis.commons.spi.VersioningService;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;

/**
 * Forwards incoming calls to a CMIS repository.
 */
public abstract class FilterCmisService extends AbstractCmisService implements CallContextAwareCmisService,
		Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private transient CallContext context;

	/**
	 * Called after the object has been created.
	 *
	 * @param parameters
	 *            the parameters provided to bridge service factory
	 */
	public void init(Map<String, String> parameters) {
	}

	/**
	 * Called at the beginning of a request.
	 */
	@Override
	public void setCallContext(CallContext context) {
		this.context = context;
	}

	/**
	 * Returns the current call context.
	 */
	@Override
	public CallContext getCallContext() {
		return context;
	}

	/**
	 * Returns a client repository service.
	 */
	public abstract RepositoryService getRepositoryService();

	/**
	 * Returns a client navigation service.
	 */
	public abstract NavigationService getNavigationService();

	/**
	 * Returns a client object service.
	 */
	public abstract ObjectService getObjectService();

	/**
	 * Returns a client versioning service.
	 */
	public abstract VersioningService getVersioningService();

	/**
	 * Returns a client discovery service.
	 */
	public abstract DiscoveryService getDiscoveryService();

	/**
	 * Returns a client multifiling service.
	 */
	public abstract MultiFilingService getMultiFilingService();

	/**
	 * Returns a client relationship service.
	 */
	public abstract RelationshipService getRelationshipService();

	/**
	 * Returns a client ACL service.
	 */
	public abstract AclService getAclService();

	/**
	 * Returns a client policy service.
	 */
	public abstract PolicyService getPolicyService();

	@Override
	public RepositoryInfo getRepositoryInfo(String repositoryId, ExtensionsData extension) {
		return getRepositoryService().getRepositoryInfo(repositoryId, extension);
	}

	@Override
	public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extension) {
		return getRepositoryService().getRepositoryInfos(extension);
	}

	@Override
	public TypeDefinitionList getTypeChildren(String repositoryId, String typeId, Boolean includePropertyDefinitions,
			BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		return getRepositoryService().getTypeChildren(repositoryId, typeId, includePropertyDefinitions, maxItems,
				skipCount, extension);
	}

	@Override
	public List<TypeDefinitionContainer> getTypeDescendants(String repositoryId, String typeId, BigInteger depth,
			Boolean includePropertyDefinitions, ExtensionsData extension) {
		return getRepositoryService().getTypeDescendants(repositoryId, typeId, depth, includePropertyDefinitions,
				extension);
	}

	@Override
	public TypeDefinition getTypeDefinition(String repositoryId, String typeId, ExtensionsData extension) {
		return getRepositoryService().getTypeDefinition(repositoryId, typeId, extension);
	}

	@Override
	public TypeDefinition createType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
		return getRepositoryService().createType(repositoryId, type, extension);
	}

	@Override
	public TypeDefinition updateType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
		return getRepositoryService().updateType(repositoryId, type, extension);
	}

	@Override
	public void deleteType(String repositoryId, String typeId, ExtensionsData extension) {
		getRepositoryService().deleteType(repositoryId, typeId, extension);
	}

	@Override
	public ObjectInFolderList getChildren(String repositoryId, String folderId, String filter, String orderBy,
			Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
			Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		return getNavigationService().getChildren(repositoryId, folderId, filter, orderBy, includeAllowableActions,
				includeRelationships, renditionFilter, includePathSegment, maxItems, skipCount, extension);
	}

	@Override
	public List<ObjectInFolderContainer> getDescendants(String repositoryId, String folderId, BigInteger depth,
			String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
			String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
		return getNavigationService().getDescendants(repositoryId, folderId, depth, filter, includeAllowableActions,
				includeRelationships, renditionFilter, includePathSegment, extension);
	}

	@Override
	public List<ObjectInFolderContainer> getFolderTree(String repositoryId, String folderId, BigInteger depth,
			String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
			String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
		return getNavigationService().getFolderTree(repositoryId, folderId, depth, filter, includeAllowableActions,
				includeRelationships, renditionFilter, includePathSegment, extension);
	}

	@Override
	public List<ObjectParentData> getObjectParents(String repositoryId, String objectId, String filter,
			Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
			Boolean includeRelativePathSegment, ExtensionsData extension) {
		return getNavigationService().getObjectParents(repositoryId, objectId, filter, includeAllowableActions,
				includeRelationships, renditionFilter, includeRelativePathSegment, extension);
	}

	@Override
	public ObjectData getFolderParent(String repositoryId, String folderId, String filter, ExtensionsData extension) {
		return getNavigationService().getFolderParent(repositoryId, folderId, filter, extension);
	}

	@Override
	public ObjectList getCheckedOutDocs(String repositoryId, String folderId, String filter, String orderBy,
			Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
			BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		return getNavigationService().getCheckedOutDocs(repositoryId, folderId, filter, orderBy,
				includeAllowableActions, includeRelationships, renditionFilter, maxItems, skipCount, extension);
	}

	@Override
	public String create(String repositoryId, Properties properties, String folderId, ContentStream contentStream,
			VersioningState versioningState, List<String> policies, ExtensionsData extension) {
		return super.create(repositoryId, properties, folderId, contentStream, versioningState, policies, extension);
	}

	@Override
	public String createDocument(String repositoryId, Properties properties, String folderId,
			ContentStream contentStream, VersioningState versioningState, List<String> policies, Acl addAces,
			Acl removeAces, ExtensionsData extension) {
		return getObjectService().createDocument(repositoryId, properties, folderId, contentStream, versioningState,
				policies, addAces, removeAces, extension);
	}

	@Override
	public String createDocumentFromSource(String repositoryId, String sourceId, Properties properties,
			String folderId, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces,
			ExtensionsData extension) {
		return getObjectService().createDocumentFromSource(repositoryId, sourceId, properties, folderId,
				versioningState, policies, addAces, removeAces, extension);
	}

	@Override
	public String createFolder(String repositoryId, Properties properties, String folderId, List<String> policies,
			Acl addAces, Acl removeAces, ExtensionsData extension) {
		return getObjectService().createFolder(repositoryId, properties, folderId, policies, addAces, removeAces,
				extension);
	}

	@Override
	public String createRelationship(String repositoryId, Properties properties, List<String> policies, Acl addAces,
			Acl removeAces, ExtensionsData extension) {
		return getObjectService()
				.createRelationship(repositoryId, properties, policies, addAces, removeAces, extension);
	}

	@Override
	public String createPolicy(String repositoryId, Properties properties, String folderId, List<String> policies,
			Acl addAces, Acl removeAces, ExtensionsData extension) {
		return getObjectService().createPolicy(repositoryId, properties, folderId, policies, addAces, removeAces,
				extension);
	}

	@Override
	public String createItem(String repositoryId, Properties properties, String folderId, List<String> policies,
			Acl addAces, Acl removeAces, ExtensionsData extension) {
		return getObjectService().createItem(repositoryId, properties, folderId, policies, addAces, removeAces,
				extension);
	}

	@Override
	public AllowableActions getAllowableActions(String repositoryId, String objectId, ExtensionsData extension) {
		return getObjectService().getAllowableActions(repositoryId, objectId, extension);
	}

	@Override
	public ObjectData getObject(String repositoryId, String objectId, String filter, Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
			Boolean includeAcl, ExtensionsData extension) {
		return getObjectService().getObject(repositoryId, objectId, filter, includeAllowableActions,
				includeRelationships, renditionFilter, includePolicyIds, includeAcl, extension);
	}

	@Override
	public Properties getProperties(String repositoryId, String objectId, String filter, ExtensionsData extension) {
		return getObjectService().getProperties(repositoryId, objectId, filter, extension);
	}

	@Override
	public List<RenditionData> getRenditions(String repositoryId, String objectId, String renditionFilter,
			BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		return getObjectService()
				.getRenditions(repositoryId, objectId, renditionFilter, maxItems, skipCount, extension);
	}

	@Override
	public ObjectData getObjectByPath(String repositoryId, String path, String filter, Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
			Boolean includeAcl, ExtensionsData extension) {
		return getObjectService().getObjectByPath(repositoryId, path, filter, includeAllowableActions,
				includeRelationships, renditionFilter, includePolicyIds, includeAcl, extension);
	}

	@Override
	public ContentStream getContentStream(String repositoryId, String objectId, String streamId, BigInteger offset,
			BigInteger length, ExtensionsData extension) {
		return getObjectService().getContentStream(repositoryId, objectId, streamId, offset, length, extension);
	}

	@Override
	public void updateProperties(String repositoryId, Holder<String> objectId, Holder<String> changeToken,
			Properties properties, ExtensionsData extension) {
		getObjectService().updateProperties(repositoryId, objectId, changeToken, properties, extension);
	}

	@Override
	public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(String repositoryId,
			List<BulkUpdateObjectIdAndChangeToken> objectIdAndChangeToken, Properties properties,
			List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds, ExtensionsData extension) {
		return getObjectService().bulkUpdateProperties(repositoryId, objectIdAndChangeToken, properties,
				addSecondaryTypeIds, removeSecondaryTypeIds, extension);
	}

	@Override
	public void moveObject(String repositoryId, Holder<String> objectId, String targetFolderId, String sourceFolderId,
			ExtensionsData extension) {
		getObjectService().moveObject(repositoryId, objectId, targetFolderId, sourceFolderId, extension);
	}

	@Override
	public void deleteObject(String repositoryId, String objectId, Boolean allVersions, ExtensionsData extension) {
		getObjectService().deleteObject(repositoryId, objectId, allVersions, extension);
	}

	@Override
	public void deleteObjectOrCancelCheckOut(String repositoryId, String objectId, Boolean allVersions,
			ExtensionsData extension) {
		// TODO: rework -> object cache
		getObjectService().deleteObject(repositoryId, objectId, allVersions, extension);
	}

	@Override
	public FailedToDeleteData deleteTree(String repositoryId, String folderId, Boolean allVersions,
			UnfileObject unfileObjects, Boolean continueOnFailure, ExtensionsData extension) {
		return getObjectService().deleteTree(repositoryId, folderId, allVersions, unfileObjects, continueOnFailure,
				extension);
	}

	@Override
	public void setContentStream(String repositoryId, Holder<String> objectId, Boolean overwriteFlag,
			Holder<String> changeToken, ContentStream contentStream, ExtensionsData extension) {
		getObjectService().setContentStream(repositoryId, objectId, overwriteFlag, changeToken, contentStream,
				extension);
	}

	@Override
	public void appendContentStream(String repositoryId, Holder<String> objectId, Holder<String> changeToken,
			ContentStream contentStream, boolean isLastChunk, ExtensionsData extension) {
		getObjectService().appendContentStream(repositoryId, objectId, changeToken, contentStream, isLastChunk,
				extension);
	}

	@Override
	public void deleteContentStream(String repositoryId, Holder<String> objectId, Holder<String> changeToken,
			ExtensionsData extension) {
		getObjectService().deleteContentStream(repositoryId, objectId, changeToken, extension);
	}

	@Override
	public void checkOut(String repositoryId, Holder<String> objectId, ExtensionsData extension,
			Holder<Boolean> contentCopied) {
		getVersioningService().checkOut(repositoryId, objectId, extension, contentCopied);
	}

	@Override
	public void cancelCheckOut(String repositoryId, String objectId, ExtensionsData extension) {
		getVersioningService().cancelCheckOut(repositoryId, objectId, extension);
	}

	@Override
	public void checkIn(String repositoryId, Holder<String> objectId, Boolean major, Properties properties,
			ContentStream contentStream, String checkinComment, List<String> policies, Acl addAces, Acl removeAces,
			ExtensionsData extension) {
		getVersioningService().checkIn(repositoryId, objectId, major, properties, contentStream, checkinComment,
				policies, addAces, removeAces, extension);
	}

	@Override
	public ObjectData getObjectOfLatestVersion(String repositoryId, String objectId, String versionSeriesId,
			Boolean major, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
			String renditionFilter, Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension) {
		return getVersioningService()
				.getObjectOfLatestVersion(repositoryId, objectId, versionSeriesId, major, filter,
						includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl,
						extension);
	}

	@Override
	public Properties getPropertiesOfLatestVersion(String repositoryId, String objectId, String versionSeriesId,
			Boolean major, String filter, ExtensionsData extension) {
		return getVersioningService().getPropertiesOfLatestVersion(repositoryId, objectId, versionSeriesId, major,
				filter, extension);
	}

	@Override
	public List<ObjectData> getAllVersions(String repositoryId, String objectId, String versionSeriesId, String filter,
			Boolean includeAllowableActions, ExtensionsData extension) {
		return getVersioningService().getAllVersions(repositoryId, objectId, versionSeriesId, filter,
				includeAllowableActions, extension);
	}

	@Override
	public ObjectList getContentChanges(String repositoryId, Holder<String> changeLogToken, Boolean includeProperties,
			String filter, Boolean includePolicyIds, Boolean includeAcl, BigInteger maxItems, ExtensionsData extension) {
		return getDiscoveryService().getContentChanges(repositoryId, changeLogToken, includeProperties, filter,
				includePolicyIds, includeAcl, maxItems, extension);
	}

	@Override
	public ObjectList query(String repositoryId, String statement, Boolean searchAllVersions,
			Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
			BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		return getDiscoveryService().query(repositoryId, statement, searchAllVersions, includeAllowableActions,
				includeRelationships, renditionFilter, maxItems, skipCount, extension);
	}

	@Override
	public void addObjectToFolder(String repositoryId, String objectId, String folderId, Boolean allVersions,
			ExtensionsData extension) {
		getMultiFilingService().addObjectToFolder(repositoryId, objectId, folderId, allVersions, extension);
	}

	@Override
	public void removeObjectFromFolder(String repositoryId, String objectId, String folderId, ExtensionsData extension) {
		getMultiFilingService().removeObjectFromFolder(repositoryId, objectId, folderId, extension);
	}

	@Override
	public ObjectList getObjectRelationships(String repositoryId, String objectId, Boolean includeSubRelationshipTypes,
			RelationshipDirection relationshipDirection, String typeId, String filter, Boolean includeAllowableActions,
			BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		return getRelationshipService().getObjectRelationships(repositoryId, objectId, includeSubRelationshipTypes,
				relationshipDirection, typeId, filter, includeAllowableActions, maxItems, skipCount, extension);
	}

	@Override
	public Acl applyAcl(String repositoryId, String objectId, Acl addAces, Acl removeAces,
			AclPropagation aclPropagation, ExtensionsData extension) {
		return getAclService().applyAcl(repositoryId, objectId, addAces, removeAces, aclPropagation, extension);
	}

	@Override
	public Acl applyAcl(String repositoryId, String objectId, Acl aces, AclPropagation aclPropagation) {
		Acl orgAcl = getAclService().getAcl(repositoryId, objectId, Boolean.FALSE, null);

		Acl removeAces = null;
		if (orgAcl != null && orgAcl.getAces() != null && !orgAcl.getAces().isEmpty()) {
			List<Ace> directAces = new ArrayList<>();

			for (Ace ace : orgAcl.getAces()) {
				if (ace.isDirect()) {
					directAces.add(ace);
				}
			}

			if (!directAces.isEmpty()) {
				removeAces = new AccessControlListImpl(directAces);
			}
		}

		return getAclService().applyAcl(repositoryId, objectId, aces, removeAces, aclPropagation, null);
	}

	@Override
	public Acl getAcl(String repositoryId, String objectId, Boolean onlyBasicPermissions, ExtensionsData extension) {
		return getAclService().getAcl(repositoryId, objectId, onlyBasicPermissions, extension);
	}

	@Override
	public void applyPolicy(String repositoryId, String policyId, String objectId, ExtensionsData extension) {
		getPolicyService().applyPolicy(repositoryId, policyId, objectId, extension);
	}

	@Override
	public List<ObjectData> getAppliedPolicies(String repositoryId, String objectId, String filter,
			ExtensionsData extension) {
		return getPolicyService().getAppliedPolicies(repositoryId, objectId, filter, extension);
	}

	@Override
	public void removePolicy(String repositoryId, String policyId, String objectId, ExtensionsData extension) {
		getPolicyService().removePolicy(repositoryId, policyId, objectId, extension);
	}

	@Override
	public ObjectInfo getObjectInfo(String repositoryId, String objectId) {
		// TODO: add intelligent object info cache
		return super.getObjectInfo(repositoryId, objectId);
	}

	@Override
	protected ObjectInfo getObjectInfoIntern(String repositoryId, ObjectData object) {
		// TODO: add intelligent object info cache
		return super.getObjectInfoIntern(repositoryId, object);
	}

	@Override
	public void close() {
		super.close();
		context = null;
	}
}
