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
		instance.findOrCreateAttributeEnum("foo.Bar", "Bar", new ArrayList<>());
		instance.findOrCreateAttributeEnum("foo.Bar", "Bar", new ArrayList<>());
		assertEquals(1, instance.size());
		AttributeEnum item = instance.findAttributeEnum("foo.Bar");
		assertEquals("BarList", item.getUniqueName("List"));
	}

	@Test
	public void whenDifferentWithSameSimpleNameAddedThenMultipleCreated() {
		instance.findOrCreateAttributeEnum("foo.Bar", "Bar", new ArrayList<>());
		instance.findOrCreateAttributeEnum("baz.Bar", "Bar", new ArrayList<>());
		assertEquals(2, instance.size());
		AttributeEnum item = instance.findAttributeEnum("foo.Bar");
		assertEquals("BarList", item.getUniqueName("List"));
		item = instance.findAttributeEnum("baz.Bar");
		assertEquals("BarList_2", item.getUniqueName("List"));
	}
}
