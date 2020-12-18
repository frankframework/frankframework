package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

public class FounderAndMemberChildTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.role.inherit.";
	private FrankDocModel model;
	private ElementType founder;
	private ElementType interfaceParent;
	private ElementType interfaceElementType;

	@Before
	public void setUp() {
		model = FrankDocModel.populate("doc/role-inherit-digester-rules.xml", PACKAGE + "Master");
		founder = model.findElementType(PACKAGE + "IFounder");
		interfaceParent = model.findElementType(PACKAGE + "IInterfaceParent");
		interfaceElementType = model.findElementType(PACKAGE + "IInterface");
	}

	@Test
	public void testTestSetup() {
		assertNotNull(founder);
		assertNotNull(interfaceParent);
		assertNotNull(interfaceElementType);
		assertNull(model.findElementType(PACKAGE + "IIrrelevantAncestor"));
	}

	@Test
	public void testFounder() {
		assertSame(founder, founder.getHighestCommonInterface());
		assertSame(founder, interfaceParent.getHighestCommonInterface());
		assertSame(founder, interfaceElementType.getHighestCommonInterface());
	}
}
