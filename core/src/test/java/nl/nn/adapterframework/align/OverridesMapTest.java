package nl.nn.adapterframework.align;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OverridesMapTest {

	OverridesMap<String> map;
	
	@BeforeEach
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
		testSubst(parsedPath, child, expectedValue, expectedValue!=null);
	}
	
	public void testSubst(String parsedPath, String child, String expectedValue, boolean expectsChild) {
		AlignmentContext context=parsedPath==null?null:parse(parsedPath);
		assertEquals(expectedValue, map.getMatchingValue(context, child), "value for '"+child+"' after '"+parsedPath+"'");
		assertEquals(expectsChild, map.hasSubstitutionsFor(context, child), "has something for '"+child+"' after '"+parsedPath+"'");
	}
	
	
	@Test
	public void test() {
		map.registerSubstitute("Party/Id", "PartyId");
		map.registerSubstitute("Address/Id", "AdressId");
		map.registerSubstitute("Id", "DefaultId");
		map.registerSubstitute("Data", "Data");
		map.registerSubstitute("Root/Sub", "SubValue");
		
		testSubst(null,"Id","DefaultId");
		testSubst(null,"Party",null, true);
		testSubst(null,"Data","Data");
		testSubst(null,"Sub",null);

		testSubst("Root","Id","DefaultId");
		testSubst("Root","Party",null, true);
		testSubst("Root","Data","Data");
		testSubst("Root","Sub","SubValue");

		testSubst("Root/Party","Id","PartyId");
		testSubst("Root/Party","Party",null, true);
		testSubst("Root/Party","Data","Data");

	}

}
