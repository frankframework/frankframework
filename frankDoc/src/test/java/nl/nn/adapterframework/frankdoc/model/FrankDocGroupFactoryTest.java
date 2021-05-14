package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class FrankDocGroupFactoryTest {
	private FrankDocGroupFactory instance;

	@Before
	public void setUp() {
		instance = new FrankDocGroupFactory();
	}

	@Test
	public void whenSameGroupNameRequestedThenSameGroupReturned() throws Exception {
		FrankDocGroup first = instance.getGroup("MyGroup", Integer.MAX_VALUE);
		FrankDocGroup second = instance.getGroup("MyGroup", Integer.MAX_VALUE);
		assertSame(second, first);
		assertEquals("MyGroup", first.getName());
	}

	@Test
	public void whenAnnotationWithOrderDefaultThenGroupWithoutOrder() throws Exception {
		FrankDocGroup group = instance.getGroup("MyGroup", Integer.MAX_VALUE);
		assertEquals("MyGroup", group.getName());
		assertEquals(Integer.MAX_VALUE, group.getOrder());
	}

	@Test
	public void whenAnnotationWithOneValueThenGroupWithoutOrder() throws Exception {
		FrankDocGroup group = instance.getGroup("MyGroup", null);
		assertEquals("MyGroup", group.getName());
		assertEquals(Integer.MAX_VALUE, group.getOrder());
	}

	@Test
	public void whenAnnotationWithTwoValuesThenGroupWithOrder() throws Exception {
		FrankDocGroup group = instance.getGroup("MyGroup", 3);
		assertEquals("MyGroup", group.getName());
		assertEquals(3, group.getOrder());		
	}

	@Test
	public void whenGroupRequestedWithAndWithoutOrderThenOrderRemains() {
		FrankDocGroup group = instance.getGroup("MyGroup", 3);
		group = instance.getGroup("MyGroup", Integer.MAX_VALUE);
		assertEquals("MyGroup", group.getName());
		assertEquals(3, group.getOrder());				
	}

	@Test
	public void whenGroupRequestedWithoutAndWithOrderThenOrderRemains() {
		// Integer.MAX_VALUE is the default value for the groupOrder in the @FrankDocGroup annotation.
		FrankDocGroup group = instance.getGroup("MyGroup", Integer.MAX_VALUE);
		group = instance.getGroup("MyGroup", 3);
		assertEquals("MyGroup", group.getName());
		assertEquals(3, group.getOrder());				
	}

	@Test
	public void whenConflictingOrdersThenMinimumTaken() throws Exception {
		FrankDocGroup first = instance.getGroup("MyGroup", 3);
		FrankDocGroup second = instance.getGroup("MyGroup", 2);
		assertSame(second, first);
		assertEquals("MyGroup", first.getName());
		assertEquals(2, first.getOrder());		
	}

	@Test
	public void whenSameOrderSetTwiceThenThatOrder() throws Exception {
		FrankDocGroup first = instance.getGroup("MyGroup", 3);
		FrankDocGroup second = instance.getGroup("MyGroup", 3);
		assertSame(second, first);
		assertEquals("MyGroup", first.getName());
		assertEquals(3, first.getOrder());		
	}

	@Test
	public void whenAllGroupsRequestedThenSortedGroupsReturned() throws Exception {
		instance.getGroup("A", 30);
		instance.getGroup("B", 20);
		instance.getGroup("C", 40);
		instance.getGroup("D", 10);
		List<FrankDocGroup> allGroups = instance.getAllGroups();
		List<String> actualGroupNames = allGroups.stream().map(FrankDocGroup::getName).collect(Collectors.toList());
		assertArrayEquals(new String[] {"D", "B", "A", "C"}, actualGroupNames.toArray(new String[] {}));
	}
}
