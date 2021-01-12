package nl.nn.adapterframework.doc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.doc.model.ElementRole;
import nl.nn.adapterframework.doc.model.FrankDocModel;

public class XmlSchemaVersionTest {
	private final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.compatibility.";
	private final String startClassName = PACKAGE + "Container";
	private final String digesterRulesFname = "doc/compatibility-digester-rules.xml";

	private FrankDocModel model;
	private XmlSchemaVersionImpl instance;
	private ElementRole elementRoleMismatch;
	private ElementRole elementRoleOk;

	@Before
	public void setUp() {
		model = FrankDocModel.populate(digesterRulesFname, startClassName);
		// This would also work for STRICT, because we are testing a method of the base class.
		instance = XmlSchemaVersion.COMPATIBILITY.getStrategy();
		elementRoleMismatch = model.findElementRole(new ElementRole.Key(PACKAGE + "IChildMismatch", "syntax1NameChildMismatch"));
		elementRoleOk = model.findElementRole(new ElementRole.Key(PACKAGE + "IChildOk", "syntax1NameChildOk"));
	}

	@Test
	public void whenDeprecatedChildClashesWithInterfaceNameThenStrictNotCompatibleWithCompatibility() {
		assertFalse(instance.checkGenericOptionElementEqualsFixedCompatibility(elementRoleMismatch));
	}

	@Test
	public void whenNoChildMismatchesThenStrictCompatibleWithCompatibility() {
		assertTrue(instance.checkGenericOptionElementEqualsFixedCompatibility(elementRoleOk));
	}
}
