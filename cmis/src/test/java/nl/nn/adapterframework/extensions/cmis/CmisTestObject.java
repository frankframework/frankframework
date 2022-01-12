package nl.nn.adapterframework.extensions.cmis;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.mockito.Mockito;

public abstract class CmisTestObject extends Mockito implements Document {

	public static CmisObject newInstance() {
		return mock(CmisTestObject.class, CALLS_REAL_METHODS);
	}

	@Override
	public String getId() {
		return "dummy_id";
	}

	@Override
	public String getName() {
		return "dummy";
	}

	@SuppressWarnings("unchecked")
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
	public AllowableActions getAllowableActions() {
		AllowableActions actions = mock(AllowableActions.class);
		Set<Action> actionSet = new HashSet<Action>() {} ;
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
		List<Relationship> list = new ArrayList<Relationship>();
		Relationship relationship = mock(Relationship.class);
		when(relationship.getId()).thenReturn("dummyId");
		list.add(relationship);
		return list;
	}

	@Override
	public Acl getAcl() {
		return mock(Acl.class);
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
	public ContentStream getContentStream() {
		return getContentStream(null);
	}

	@Override
	public ContentStream getContentStream(String streamId) {
		return new ContentStream() {

			@Override
			public void setExtensions(List<CmisExtensionElement> extensions) {
				//Can't set something that doesn't exist!
			}

			@Override
			public List<CmisExtensionElement> getExtensions() {
				return null;
			}

			@Override
			public InputStream getStream() {
				return new ByteArrayInputStream("dummy_stream".getBytes());
			}

			@Override
			public String getMimeType() {
				return "text/xml";
			}

			@Override
			public long getLength() {
				return 0;
			}

			@Override
			public String getFileName() {
				return null;
			}

			@Override
			public BigInteger getBigLength() {
				return null;
			}
		};
	}

}
