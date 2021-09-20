package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.nn.adapterframework.frankdoc.model.DigesterRulesPattern.ViolationChecker;

public class DigesterRulesPatternTest {
	@Test
	public void whenPatternIsNullThenInvalid() {
		assertNotNull(new DigesterRulesPattern(null).getError());
	}

	@Test
	public void whenPatternIsEmptyThenInvalid() {
		assertNotNull(new DigesterRulesPattern("").getError());
	}

	@Test
	public void whenPatternHasMultipleWordsWithoutWildcardThenViolationChecker() {
		DigesterRulesPattern p = new DigesterRulesPattern("first/second");
		assertNull(p.getError());
		assertFalse(p.isRoot());
		ViolationChecker v = p.getViolationChecker();
		assertNotNull(v);
		assertTrue(v.isPatternOnlyMatchesRoot());
	}

	@Test
	public void whenPatternHasMultipleWildcardsThenInvalid() {
		assertNotNull(new DigesterRulesPattern("*/*").getError());
	}

	@Test
	public void whenPatternEndsWithWildcardThenInvalid() {
		assertNotNull(new DigesterRulesPattern("*/adapter/*").getError());
	}

	@Test
	public void whenPatternIsOneWordThenRootAndNoViolationChecker() {
		DigesterRulesPattern p = new DigesterRulesPattern("configuration");
		assertNull(p.getError());
		assertTrue(p.isRoot());
		assertNull(p.getViolationChecker());
	}

	@Test
	public void whenPatternIsStarFollowedByWordThenNonRootNoViolationChecker() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/listener");
		assertNull(p.getError());
		assertFalse(p.isRoot());
		assertNull(p.getViolationChecker());
	}

	@Test
	public void whenPatternIsStarThenMultipleWordsThenNonRootHaveViolationChecker() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/receiver/listener");
		assertNull(p.getError());
		assertFalse(p.isRoot());
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertEquals("*/receiver/listener", v.getOriginalPattern());
		assertFalse(v.isPatternOnlyMatchesRoot());
	}

	@Test
	public void whenViolatorMatchesPatternThenCheckSucceeds() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/adapter/receiver/listener");
		TestDigesterRulesConfigChild c = TestDigesterRulesConfigChild.getInstance("listener");
		c.addParent("notRelevant");
		c.addParent("receiver").addParent("adapter");
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertEquals("ViolationChecker backtracking(receiver, adapter)", v.toString());
		assertTrue(v.check(c));
	}

	@Test
	public void whenViolatorDoesNotMatchThenCheckFails() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/receiver/listener");
		TestDigesterRulesConfigChild c = TestDigesterRulesConfigChild.getInstance("listener");
		c.addParent("somethingElse");
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertEquals("ViolationChecker backtracking(receiver)", v.toString());
		assertFalse(v.check(c));		
	}

	@Test
	public void whenViolatorMatchesPatternButParentViolatesThenChildViolates() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/violated/listener");
		TestDigesterRulesConfigChild c = TestDigesterRulesConfigChild.getInstance("listener");
		TestDigesterRulesConfigChild parent = c.addParent("violated");
		parent.setViolatesDigesterRules(true);
		assertFalse(p.getViolationChecker().check(c));
	}

	@Test
	public void whenRootViolatorMatchesAtRootThenChildAccepted() {
		DigesterRulesPattern p = new DigesterRulesPattern("root/child");
		assertNull(p.getError());
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertEquals("ViolationChecker backtracking(root) at root", v.toString());
		TestDigesterRulesConfigChild c = TestDigesterRulesConfigChild.getRootOwnedInstance("child", "root");
		assertTrue(v.check(c));
	}

	@Test
	public void whenRootViolatorMatchesButNotAtRootThenChildViolates() {
		DigesterRulesPattern p = new DigesterRulesPattern("child/grandChild");
		assertNull(p.getError());
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		TestDigesterRulesConfigChild gc = TestDigesterRulesConfigChild.getInstance("grandChild");
		gc.addRootOwnedParent("child", "root");
		assertFalse(v.check(gc));
	}
}
