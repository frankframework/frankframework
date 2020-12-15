package nl.nn.adapterframework.doc.model;

import static java.util.Arrays.asList;
import static nl.nn.adapterframework.doc.model.ElementChild.ALL;
import static nl.nn.adapterframework.doc.model.ElementChild.IN_XSD;
import static nl.nn.adapterframework.doc.model.ElementChild.NONE;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class NavigationCumulativeTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.walking";

	@Parameters(name = "{0} with {1}")
	public static Collection<Object[]> data() {
		return asList(new Object[][] {
			{"Parent", IN_XSD, NONE, asList("parentAttributeFirst", "parentAttributeSecond")},
			{"Child", IN_XSD, NONE, asList("childAttribute", "parentAttributeFirst", "parentAttributeSecond")},
			{"Child", ALL, NONE, asList("childAttribute", "parentAttributeFirst", "parentAttributeSecond")},
			{"GrandChild", ALL, NONE, asList("grandChildAttribute", "parentAttributeSecond", "childAttribute", "parentAttributeFirst")},
			{"GrandChild2", ALL, NONE, asList("grandChild2Attribute", "child2Attribute", "parentAttributeFirst", "parentAttributeSecond")}
		});
	}

	@Parameter(0)
	public String simpleClassName;

	@Parameter(1)
	public Predicate<ElementChild> childSelector;

	@Parameter(2)
	public Predicate<ElementChild> childRejector;

	@Parameter(3)
	public List<String> childNames;

	@Test
	public void test() {
		String rootClassName = PACKAGE + "." + simpleClassName;
		FrankDocModel model = FrankDocModel.populate("doc/empty-digester-rules.xml", rootClassName);
		FrankElement subject = model.findFrankElement(rootClassName);
		List<String> actual = subject.getCumulativeAttributes(childSelector, childRejector).stream()
				.map(a -> a.getName()).collect(Collectors.toList());
		assertEquals(childNames, actual);
	}
}
