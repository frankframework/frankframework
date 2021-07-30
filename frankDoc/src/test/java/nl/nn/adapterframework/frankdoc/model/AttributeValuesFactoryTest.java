package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

public class AttributeValuesFactoryTest {
	private AttributeEnumFactory instance;

	@Before
	public void setUp() {
		instance = new AttributeEnumFactory();
	}

	@Test
	public void whenSameClassRequestedMultipleTimesThenOnlyOnceAdded() {
		instance.findOrCreateAttributeValues("foo.Bar", "Bar", new ArrayList<>());
		instance.findOrCreateAttributeValues("foo.Bar", "Bar", new ArrayList<>());
		assertEquals(1, instance.size());
		AttributeEnum item = instance.findAttributeValues("foo.Bar");
		assertEquals("BarList", item.getUniqueName("List"));
	}

	@Test
	public void whenDifferentWithSameSimpleNameAddedThenMultipleCreated() {
		instance.findOrCreateAttributeValues("foo.Bar", "Bar", new ArrayList<>());
		instance.findOrCreateAttributeValues("baz.Bar", "Bar", new ArrayList<>());
		assertEquals(2, instance.size());
		AttributeEnum item = instance.findAttributeValues("foo.Bar");
		assertEquals("BarList", item.getUniqueName("List"));
		item = instance.findAttributeValues("baz.Bar");
		assertEquals("BarList_2", item.getUniqueName("List"));
	}
}
