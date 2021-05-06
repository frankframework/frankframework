package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class AttributeValuesFactoryTest {
	private AttributeValuesFactory instance;

	@Before
	public void setUp() {
		instance = new AttributeValuesFactory();
	}

	@Test
	public void whenSameClassRequestedMultipleTimesThenOnlyOnceAdded() {
		instance.findOrCreateAttributeValues("foo.Bar", "Bar", null);
		instance.findOrCreateAttributeValues("foo.Bar", "Bar", null);
		assertEquals(1, instance.size());
		AttributeValues item = instance.findAttributeValues("foo.Bar");
		assertEquals("BarList", item.getUniqueName("List"));
	}

	@Test
	public void whenDifferentWithSameSimpleNameAddedThenMultipleCreated() {
		instance.findOrCreateAttributeValues("foo.Bar", "Bar", null);
		instance.findOrCreateAttributeValues("baz.Bar", "Bar", null);
		assertEquals(2, instance.size());
		AttributeValues item = instance.findAttributeValues("foo.Bar");
		assertEquals("BarList", item.getUniqueName("List"));
		item = instance.findAttributeValues("baz.Bar");
		assertEquals("BarList_2", item.getUniqueName("List"));
	}
}
