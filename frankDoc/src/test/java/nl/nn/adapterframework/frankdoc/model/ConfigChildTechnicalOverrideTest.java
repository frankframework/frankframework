package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

public class ConfigChildTechnicalOverrideTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.technical.override.";
	private static final String DIGESTER_RULES = "doc/technical-override-digester-rules.xml";

	private static FrankDocModel model;

	@BeforeClass
	public static void setUp() {
		FrankClassRepository repository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		model = FrankDocModel.populate(DIGESTER_RULES, PACKAGE + "Master", repository);
	}

	@Test
	public void whenConfigChildNotInheritedThenNoTechnicalOverride() {
		FrankElement element = model.findFrankElement(PACKAGE + "ChildTechnicalOverride");
		ConfigChild child = selectChildByArgumentType(element, "ChildTechnicalOverride");
		assertFalse(child.isTechnicalOverride());
	}

	private ConfigChild selectChildByArgumentType(FrankElement element, String simpleNameArgumentType) {
		List<ConfigChild> children = element.getConfigChildren(ElementChild.ALL_NOT_EXCLUDED).stream()
				.filter(c -> c instanceof ObjectConfigChild)
				.map(c -> (ObjectConfigChild) c)
				.filter(c -> c.getElementType().getFullName().equals(PACKAGE + simpleNameArgumentType))
				.collect(Collectors.toList());
		assertEquals(1, children.size());
		ConfigChild child = children.get(0);
		return child;
	}

	@Test
	public void whenConfigChildOverriddenThenTechnicalOverride() {
		FrankElement element = model.findFrankElement(PACKAGE + "ChildTechnicalOverride");
		ConfigChild child = selectChildByArgumentType(element, "Master");
		assertTrue(child.isTechnicalOverride());
	}

	@Test
	public void whenConfigChildInheritedWithoutJavaOverrideThenNoTechnicalOverride() {
		FrankElement element = model.findFrankElement(PACKAGE + "ChildMeaningfulOverride");
		FrankElement superElement = model.findFrankElement(PACKAGE + "ParentMeaningfulOverride");
		ConfigChild child = selectChildByArgumentType(element, "Master");
		assertFalse(child.isTechnicalOverride());
		assertEquals(superElement.getSimpleName(), child.getOverriddenFrom().getSimpleName());
		assertTrue(child.isAllowMultiple());
		ConfigChild inherited = selectChildByArgumentType(superElement, "Master");
		assertFalse(inherited.isTechnicalOverride());
		assertFalse(inherited.isAllowMultiple());
	}
}
