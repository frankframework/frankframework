package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.doc.Utils;

public class FrankDocModelGroupsTest {
	private static final String I_GROUP_CONTAINER = "nl.nn.adapterframework.doc.testtarget.groups.IGroupContainer";

	private FrankDocModel instance;
	
	@Before
	public void setUp() throws SAXException, IOException, ReflectiveOperationException {
		instance = new FrankDocModel();
		instance.createConfigChildDescriptorsFrom("doc/fake-group-digester-rules.xml");
		instance.findOrCreateElementType(Utils.getClass(I_GROUP_CONTAINER));
		instance.buildGroups();
	}

	@Test
	public void whenElementTypeThenGroupCreated() {
		assertTrue(instance.getGroups().containsKey("IGroupContainer"));
		List<FrankElement> elements = instance.getGroups().get("IGroupContainer").getElements();
		assertEquals(1, elements.size());
		assertEquals("GroupContainer", instance.getGroups().get("IGroupContainer").getElements().get(0).getSimpleName());
	}

	@Test
	public void whenNonInterfaceElementTypeThenPutInGroupOther() {
		assertTrue(instance.getGroups().containsKey(FrankDocModel.OTHER));
		FrankDocGroup other = instance.getGroups().get(FrankDocModel.OTHER);
		assertEquals(1, other.getElements().size());
		FrankElement element = other.getElements().get(0);
		assertEquals("GroupChild", element.getSimpleName());
	}
}
