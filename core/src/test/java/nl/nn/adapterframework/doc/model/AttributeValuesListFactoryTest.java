package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class AttributeValuesListFactoryTest {
	private AttributeValuesListFactory instance;

	@Before
	public void setUp() {
		instance = new AttributeValuesListFactory();
	}

	@Test
	public void whenSameClassRequestedMultipleTimesThenOnlyOnceAdded() {
		instance.findOrCreateAttributeValuesList("foo.Bar", "Bar", null);
		instance.findOrCreateAttributeValuesList("foo.Bar", "Bar", null);
		assertEquals(1, instance.size());
		AttributeValuesList item = instance.findAttributeValuesList("foo.Bar");
		assertEquals("BarList", item.getUniqueName("List"));
	}

	@Test
	public void whenDifferentWithSameSimpleNameAddedThenMultipleCreated() {
		instance.findOrCreateAttributeValuesList("foo.Bar", "Bar", null);
		instance.findOrCreateAttributeValuesList("baz.Bar", "Bar", null);
		assertEquals(2, instance.size());
		AttributeValuesList item = instance.findAttributeValuesList("foo.Bar");
		assertEquals("BarList", item.getUniqueName("List"));
		item = instance.findAttributeValuesList("baz.Bar");
		assertEquals("BarList_2", item.getUniqueName("List"));
	}
}
