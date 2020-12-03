package nl.nn.adapterframework.doc.model;

import static java.util.Arrays.asList;
import static nl.nn.adapterframework.doc.model.ElementChild.ALL;
import static nl.nn.adapterframework.doc.model.ElementChild.DEPRECATED;
import static nl.nn.adapterframework.doc.model.ElementChild.NONE;
import static nl.nn.adapterframework.doc.model.ElementChild.IN_XSD;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import lombok.EqualsAndHashCode;
import nl.nn.adapterframework.doc.Utils;

@RunWith(Parameterized.class)
public class NavigationTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.walking";

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return asList(new Object[][] {
			{"Parent", IN_XSD, NONE, asList(
					ref(RefKind.DECLARED, "Parent"))},
			{"Child", IN_XSD, NONE, asList(
					// Attribute childAttribute is not selected, so we do not have a real override.
					ref(RefKind.DECLARED, "Child"), ref(RefKind.DECLARED, "Parent"))},
			{"Child", ALL, NONE, asList(
					// Attribute parentAttributeFirst is overridden. Keep with Child, omit with Parent
					ref(RefKind.DECLARED, "Child"), ref(RefKind.CHILD, "parentAttributeSecond"))},
			{"GrandChild", ALL, NONE, asList(
					// All attributes of Parent were overridden. Nothing to reference for Parent.
					ref(RefKind.DECLARED, "GrandChild"), ref(RefKind.DECLARED, "Child"))},
			{"GrandChild", IN_XSD, NONE, asList(
					// The override of parentAttributeSecond counts, in Child parentAttributeFirst is ignored as child
					ref(RefKind.DECLARED, "GrandChild"),
					ref(RefKind.DECLARED, "Child"),
					ref(RefKind.CHILD, "parentAttributeFirst"))},
			{"GrandChild2", ALL, NONE, asList(
					ref(RefKind.DECLARED, "GrandChild2"), ref(RefKind.CUMULATIVE, "Child2"))},
			{"GrandChild2", IN_XSD, NONE, asList(
					// All children of Child2 are deprecated, so Child2 is ignored in the ancestor hierarchy
					ref(RefKind.DECLARED, "GrandChild2"), ref(RefKind.DECLARED, "Parent"))},
			{"GrandChild3", ALL, DEPRECATED, asList(
					// All attributes of Parent are overridden by deprecated methods and should be de-inherited
					)}
		});
	}

	private static enum RefKind {
		CHILD,
		DECLARED,
		CUMULATIVE;
	}

	@EqualsAndHashCode
	private static class Ref {
		private RefKind kind;
		private String name;

		Ref(RefKind kind, String name) {
			this.kind = kind;
			this.name = name;
		}

		@Override
		public String toString() {
			return "(" + kind.toString() + ", " + name + ")"; 
		}
	}

	private static Ref ref(RefKind kind, String name) {
		return new Ref(kind, name);
	}

	@Parameter(0)
	public String simpleClassName;

	@Parameter(1)
	public Predicate<ElementChild> childSelector;

	@Parameter(2)
	public Predicate<ElementChild> childRejector;

	@Parameter(3)
	public List<Ref> expectedRefs;

	private List<Ref> actual;

	@Before
	public void setUp() {
		actual = new ArrayList<>();
	}

	@Test
	public void test() throws Exception {
		String rootClassName = PACKAGE + "." + simpleClassName;
		FrankDocModel model = FrankDocModel.populate("doc/empty-digester-rules.xml", rootClassName);
		FrankElement walkFrom = model.findOrCreateFrankElement(Utils.getClass(rootClassName));
		walkFrom.walkCumulativeAttributes(new CumulativeChildHandler<FrankAttribute>() {
			@Override
			public void handleSelectedChildren(List<FrankAttribute> children, FrankElement owner) {
				children.forEach(c -> actual.add(ref(RefKind.CHILD, c.getName())));
			}

			@Override
			public void handleChildrenOf(FrankElement frankElement) {
				actual.add(ref(RefKind.DECLARED, frankElement.getSimpleName()));
			}

			@Override
			public void handleCumulativeChildrenOf(FrankElement frankElement) {
				actual.add(ref(RefKind.CUMULATIVE, frankElement.getSimpleName()));
			}
			
		}, childSelector, childRejector);
		assertEquals(expectedRefs, actual);
	}
}
