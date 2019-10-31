package nl.nn.adapterframework.extensions.cmis;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.DocumentType;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ContentStreamHash;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.ExtensionLevel;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.mockito.Mockito;

public class CmisTestObject extends Mockito implements Document {

	@Override
	public String getId() {
		return "dummy_id";
	}

	@Override
	public List<Property<?>> getProperties() {
		List<Property<?>> list = new ArrayList<Property<?>>();
		
		Property<String> pName = mock(Property.class);
		when(pName.getId()).thenReturn("cmis:name");
		when(pName.getFirstValue()).thenReturn("dummy");
		when(pName.getType()).thenReturn(PropertyType.ID);
		list.add(pName);
		
		Property<BigInteger> pProjectNumber = mock(Property.class);
		when(pProjectNumber.getType()).thenReturn(PropertyType.INTEGER);
		when(pProjectNumber.getId()).thenReturn("project:number");
		when(pProjectNumber.getFirstValue()).thenReturn(new BigInteger("123456789"));
		list.add(pProjectNumber);
		
		Property<GregorianCalendar> pLastModified = mock(Property.class);
		when(pLastModified.getType()).thenReturn(PropertyType.DATETIME);
		when(pLastModified.getId()).thenReturn("project:lastModified");
		when(pLastModified.getFirstValue()).thenReturn(new GregorianCalendar(2019, 1, 26, 16, 31, 15));
		list.add(pLastModified);
		
		Property<Boolean> pOnTime = mock(Property.class);
		when(pOnTime.getId()).thenReturn("project:onTime");
		when(pOnTime.getType()).thenReturn(PropertyType.BOOLEAN);
		when(pOnTime.getFirstValue()).thenReturn(true);
		list.add(pOnTime);
		
		// TODO Fill this list

		return list;
	}

	@Override
	public <T> Property<T> getProperty(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getPropertyValue(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "dummy";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCreatedBy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GregorianCalendar getCreationDate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLastModifiedBy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GregorianCalendar getLastModificationDate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseTypeId getBaseTypeId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectType getBaseType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectType getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SecondaryType> getSecondaryTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ObjectType> findObjectType(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getChangeToken() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AllowableActions getAllowableActions() {
		AllowableActions actions = mock(AllowableActions.class);
		Set<Action> actionSet = new HashSet<Action>() {} ;
		Action action = Action.CAN_CREATE_DOCUMENT;
		actionSet.add(action);
		when(actions.getAllowableActions()).thenReturn(actionSet);
		return actions;
	}

	@Override
	public boolean hasAllowableAction(Action action) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Relationship> getRelationships() {
		List<Relationship> list = new ArrayList<Relationship>();
		Relationship relationship = mock(Relationship.class);
		when(relationship.getId()).thenReturn("dummyId");
		list.add(relationship);
		return list;
	}

	@Override
	public Acl getAcl() {
		Acl acl = mock(Acl.class);
		return acl;
	}

	@Override
	public Set<String> getPermissionsForPrincipal(String principalId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
	}

	@Override
	public void delete(boolean allVersions) {
		// TODO Auto-generated method stub
	}

	@Override
	public CmisObject updateProperties(Map<String, ?> properties) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectId updateProperties(Map<String, ?> properties, boolean refresh) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CmisObject updateProperties(Map<String, ?> properties, List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectId updateProperties(Map<String, ?> properties, List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds, boolean refresh) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CmisObject rename(String newName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectId rename(String newName, boolean refresh) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Rendition> getRenditions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void applyPolicy(ObjectId... policyIds) {
		// TODO Auto-generated method stub
	}

	@Override
	public void applyPolicy(ObjectId policyId, boolean refresh) {
		// TODO Auto-generated method stub
	}

	@Override
	public void removePolicy(ObjectId... policyIds) {
		// TODO Auto-generated method stub
	}

	@Override
	public void removePolicy(ObjectId policyId, boolean refresh) {
		// TODO Auto-generated method stub
	}

	@Override
	public List<Policy> getPolicies() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ObjectId> getPolicyIds() {
		List<ObjectId> policies = new ArrayList<ObjectId>();
		ObjectId objectId = mock(ObjectId.class);
		when(objectId.getId()).thenReturn("dummyObjectId");
		policies.add(objectId);
		return policies;
	}

	@Override
	public Acl applyAcl(List<Ace> addAces, List<Ace> removeAces, AclPropagation aclPropagation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Acl addAcl(List<Ace> addAces, AclPropagation aclPropagation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Acl removeAcl(List<Ace> removeAces, AclPropagation aclPropagation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Acl setAcl(List<Ace> aces) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<CmisExtensionElement> getExtensions(ExtensionLevel level) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getAdapter(Class<T> adapterInterface) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getRefreshTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub
	}

	@Override
	public void refreshIfOld(long durationInMillis) {
		// TODO Auto-generated method stub
	}

	@Override
	public FileableCmisObject move(ObjectId sourceFolderId, ObjectId targetFolderId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileableCmisObject move(ObjectId sourceFolderId, ObjectId targetFolderId, OperationContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Folder> getParents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Folder> getParents(OperationContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getPaths() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addToFolder(ObjectId folderId, boolean allVersions) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeFromFolder(ObjectId folderId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Boolean isImmutable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isLatestVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isMajorVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isLatestMajorVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isPrivateWorkingCopy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersionLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersionSeriesId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isVersionSeriesCheckedOut() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersionSeriesCheckedOutBy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersionSeriesCheckedOutId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCheckinComment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getContentStreamLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getContentStreamMimeType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContentStreamFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContentStreamId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ContentStreamHash> getContentStreamHashes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLatestAccessibleStateId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentType getDocumentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isVersionable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Boolean isVersionSeriesPrivateWorkingCopy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteAllVersions() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ContentStream getContentStream() {
		return getContentStream(null);
	}

	@Override
	public ContentStream getContentStream(BigInteger offset, BigInteger length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContentStream getContentStream(String streamId) {
		return new ContentStream() {

			@Override
			public void setExtensions(List<CmisExtensionElement> extensions) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public List<CmisExtensionElement> getExtensions() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public InputStream getStream() {
				// TODO Auto-generated method stub
				return new ByteArrayInputStream("dummy_stream".getBytes());
			}

			@Override
			public String getMimeType() {
				// TODO Auto-generated method stub
				return "text/xml";
			}

			@Override
			public long getLength() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public String getFileName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public BigInteger getBigLength() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	@Override
	public ContentStream getContentStream(String streamId, BigInteger offset, BigInteger length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContentUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContentUrl(String streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document setContentStream(ContentStream contentStream, boolean overwrite) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectId setContentStream(ContentStream contentStream, boolean overwrite, boolean refresh) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document appendContentStream(ContentStream contentStream, boolean isLastChunk) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectId appendContentStream(ContentStream contentStream, boolean isLastChunk, boolean refresh) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document deleteContentStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectId deleteContentStream(boolean refresh) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream createOverwriteOutputStream(String filename, String mimeType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream createOverwriteOutputStream(String filename, String mimeType, int bufferSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream createAppendOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream createAppendOutputStream(int bufferSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectId checkOut() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cancelCheckOut() {
		// TODO Auto-generated method stub
	}

	@Override
	public ObjectId checkIn(boolean major, Map<String, ?> properties, ContentStream contentStream, String checkinComment, List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectId checkIn(boolean major, Map<String, ?> properties, ContentStream contentStream, String checkinComment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document getObjectOfLatestVersion(boolean major) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document getObjectOfLatestVersion(boolean major, OperationContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Document> getAllVersions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Document> getAllVersions(OperationContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document copy(ObjectId targetFolderId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document copy(ObjectId targetFolderId, Map<String, ?> properties, VersioningState versioningState, List<Policy> policies, List<Ace> addACEs, List<Ace> removeACEs, OperationContext context) {
		// TODO Auto-generated method stub
		return null;
	}

}
