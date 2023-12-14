package nl.nn.adapterframework.extensions.cmis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.codec.binary.Base64;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public abstract class CmisTestObject extends Mockito implements Document, Answer<CmisObject> {
	protected String objectId = "dummy_id";

	public static CmisTestObject newInstance() {
		return mock(CmisTestObject.class, CALLS_REAL_METHODS);
	}

	@Override
	public CmisObject answer(InvocationOnMock invocation) throws Throwable {
		Object obj = invocation.getArguments()[0];
		if(obj instanceof ObjectId) {
			ObjectId id = (ObjectId) obj;
			objectId = id.getId();
		} else if(obj instanceof Map) {
			@SuppressWarnings({"unchecked", "rawtypes"})
			Map<String, Object> properties = (Map) obj;
			objectId = (String) properties.get(PropertyIds.NAME);
		}

		if(objectId != null && objectId.equals("NOT_FOUND")) {
			throw new CmisObjectNotFoundException();
		}

		return this; //created at newInstance() to mock unimplemented methods, initialized once answer is called
	}

	@Override
	public String getId() {
		return Base64.encodeBase64String(objectId.getBytes());
	}
	public String getObjectId() {
		return Base64.encodeBase64String(objectId.getBytes());
	}

	@Override
	public String getName() {
		return "dummy";
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Property<?>> getProperties() {
		List<Property<?>> list = new ArrayList<>();

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
	public AllowableActions getAllowableActions() {
		AllowableActions actions = mock(AllowableActions.class);
		Set<Action> actionSet = new HashSet<>();
		Action action = Action.CAN_CREATE_DOCUMENT;
		actionSet.add(action);
		when(actions.getAllowableActions()).thenReturn(actionSet);
		return actions;
	}

	@Override
	public CmisObject updateProperties(Map<String, ?> properties) {
		return this;
	}

	@Override
	public List<Relationship> getRelationships() {
		List<Relationship> list = new ArrayList<>();
		Relationship relationship = mock(Relationship.class);
		when(relationship.getId()).thenReturn(objectId);
		list.add(relationship);
		return list;
	}

	@Override
	public boolean hasAllowableAction(Action action) {
		return objectId.equals("ALLOWED");
	}

	@Override
	public Acl getAcl() {
		return mock(Acl.class);
	}

	@Override
	public List<ObjectId> getPolicyIds() {
		List<ObjectId> policies = new ArrayList<>();
		ObjectId objectId = mock(ObjectId.class);
		when(objectId.getId()).thenReturn("dummyObjectId");
		policies.add(objectId);
		return policies;
	}

	@Override
	public ContentStream getContentStream() {
		return new CmisSenderTestBase.ContentStreamMock();
	}

	@Override
	public ContentStream getContentStream(String streamId) {
		return new CmisSenderTestBase.ContentStreamMock();
	}
}
