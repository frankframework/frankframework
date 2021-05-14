package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;

public class FrankDocGroupFactoryTest {
	private FrankDocGroupFactory instance;

	@Before
	public void setUp() {
		instance = new FrankDocGroupFactory();
	}

	@Test
	public void whenSameGroupNameRequestedThenSameGroupReturned() throws Exception {
		FrankDocGroup first = instance.getGroup(new String[] {"MyGroup"});
		FrankDocGroup second = instance.getGroup(new String[] {"MyGroup"});
		assertSame(second, first);
		assertEquals("MyGroup", first.getName());
	}

	@Test
	public void whenGroupOtherRequestedTwiceThenSameGroupReturned() throws Exception {
		FrankDocGroup first = instance.getGroup((String[]) null);
		FrankDocGroup second = instance.getGroup((String[]) null);
		assertSame(second, first);
		assertEquals(FrankDocGroup.GROUP_NAME_OTHER, first.getName());		
	}

	@Test
	public void whenNoAnnotationThenHaveOtherWithoutOrder() throws Exception {
		FrankDocGroup group = instance.getGroup((String[]) null);
		assertEquals(FrankDocGroup.GROUP_NAME_OTHER, group.getName());
		assertEquals(Integer.MAX_VALUE, group.getOrder());
	}

	@Test
	public void whenAnnotationWithOneValueThenGroupWithoutOrder() throws Exception {
		FrankDocGroup group = instance.getGroup(new String[] {"MyGroup"});
		assertEquals("MyGroup", group.getName());
		assertEquals(Integer.MAX_VALUE, group.getOrder());
	}

	@Test
	public void whenAnnotationWithTwoValuesThenGroupWithOrder() throws Exception {
		FrankDocGroup group = instance.getGroup(new String[] {"3", "MyGroup"});
		assertEquals("MyGroup", group.getName());
		assertEquals(3, group.getOrder());		
	}

	@Test
	public void whenConflictingOrdersThenMinimumTaken() throws Exception {
		FrankDocGroup first = instance.getGroup(new String[] {"3", "MyGroup"});
		FrankDocGroup second = instance.getGroup(new String[] {"2", "MyGroup"});
		assertSame(second, first);
		assertEquals("MyGroup", first.getName());
		assertEquals(2, first.getOrder());		
	}

	@Test(expected = FrankDocException.class)
	public void whenTooManyValuesInAnnotationThenError() throws Exception {
		instance.getGroup(new String[] {"3", "MyGroup", "extra"});		
	}

	@Test
	public void whenAllGroupsRequestedThenSortedGroupsReturned() throws Exception {
		instance.getGroup(new String[] {"30", "A"});
		instance.getGroup(new String[] {"20", "B"});
		instance.getGroup(new String[] {"40", "C"});
		instance.getGroup(new String[] {"10", "D"});
		List<FrankDocGroup> allGroups = instance.getAllGroups();
		List<String> actualGroupNames = allGroups.stream().map(FrankDocGroup::getName).collect(Collectors.toList());
		assertArrayEquals(new String[] {"D", "B", "A", "C"}, actualGroupNames.toArray(new String[] {}));
	}
}
