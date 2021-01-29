package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

public class ConflictTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.conflict.";
	private static FrankDocModel model;

	@BeforeClass
	public static void setUp() {
		model = FrankDocModel.populate("doc/conflict-digester-rules.xml", PACKAGE + "Container");
	}

	@Test
	public void testConflicts() {
		ElementRole roleWithConflictingElement = model.findElementRole(PACKAGE + "IChild", "child");
		FrankElement conflictedWithRole = model.findFrankElement(PACKAGE + "Child");
		assertSame(conflictedWithRole, roleWithConflictingElement.getConflictingElement(f -> true));
		assertFalse(conflictedWithRole.isCausesNameConflict());
		FrankElement causesNameConflict = model.findFrankElement(PACKAGE + "My");
		FrankElement chosenInNameConflict = model.findFrankElement(PACKAGE + "MyChild");
		assertTrue(causesNameConflict.isCausesNameConflict());
		assertFalse(chosenInNameConflict.isCausesNameConflict());
	}
}
