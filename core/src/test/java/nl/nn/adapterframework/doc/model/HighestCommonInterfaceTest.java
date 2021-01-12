package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

public class HighestCommonInterfaceTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.role.inherit.";
	private FrankDocModel model;
	private ElementType founder;
	private ElementType interfaceParent;
	private ElementType interfaceElementType;
	private ElementType interface2ElementType;

	@Before
	public void setUp() {
		model = FrankDocModel.populate("doc/role-inherit-digester-rules.xml", PACKAGE + "Master");
		founder = model.findElementType(PACKAGE + "IFounder");
		interfaceParent = model.findElementType(PACKAGE + "IInterfaceParent");
		interfaceElementType = model.findElementType(PACKAGE + "IInterface");
		interface2ElementType = model.findElementType(PACKAGE + "IInterface2");
	}

	@Test
	public void testTestSetup() {
		assertNotNull(founder);
		assertNotNull(interfaceParent);
		assertNotNull(interfaceElementType);
		assertNull(model.findElementType(PACKAGE + "IIrrelevantAncestor"));
		assertNull(model.findElementType(PACKAGE + "IIrrelevant"));
		assertNotNull(interface2ElementType);
	}

	@Test
	public void testFounder() {
		assertSame(founder, founder.getHighestCommonInterface());
		assertSame(founder, interfaceParent.getHighestCommonInterface());
		assertSame(founder, interfaceElementType.getHighestCommonInterface());
		assertSame(founder, interface2ElementType.getHighestCommonInterface());
	}
}
