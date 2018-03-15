package nl.nn.adapterframework.align;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class OverridesMapTest {

	OverridesMap<String> map;
	
	@Before
	public void setUp() throws Exception {
		map = new OverridesMap<String>();
	}

	AlignmentContext parse(String path) {
		AlignmentContext result=null;
		for (String element: path.split("/")) {
			result = new AlignmentContext(result, null, element, null, null, null, 0, null, false);
		}
		return result;
	}
	
	public void testSubst(String parsedPath, String child, String expectedValue) {
		AlignmentContext context=parsedPath==null?null:parse(parsedPath);
		assertEquals(expectedValue,map.getMatchingValue(context, child));
	}
	
	
	@Test
	public void test() {
		map.registerSubstitute("Party/Id", "PartyId");
		map.registerSubstitute("Address/Id", "AdressId");
		map.registerSubstitute("Id", "DefaultId");
		map.registerSubstitute("Data", "Data");
		map.registerSubstitute("Root/Sub", "SubValue");
		
		testSubst(null,"Id","DefaultId");
		testSubst(null,"Party",null);
		testSubst(null,"Data","Data");
		testSubst(null,"Sub",null);

		testSubst("Root","Id","DefaultId");
		testSubst("Root","Party",null);
		testSubst("Root","Data","Data");
		testSubst("Root","Sub","SubValue");

		testSubst("Root/Party","Id","PartyId");
		testSubst("Root/Party","Party",null);
		testSubst("Root/Party","Data","Data");

	}

}
