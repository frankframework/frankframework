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
		TestDigesterRulesFrankElement f = new TestDigesterRulesFrankElement();
		f.addParent("notRelevant");
		f.addParent("receiver").addParent("adapter");
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertEquals("ViolationChecker backtracking(receiver, adapter)", v.toString());
		// When f has a method to register a child with role "listener", then pattern p will allow it.
		assertTrue(v.matches(f));
	}

	@Test
	public void whenViolatorDoesNotMatchThenCheckFails() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/receiver/listener");
		TestDigesterRulesFrankElement f = new TestDigesterRulesFrankElement();
		f.addParent("somethingElse");
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertEquals("ViolationChecker backtracking(receiver)", v.toString());
		// f does not match pattern component "receiver", so it cannot create a child with role "listener".
		assertFalse(v.matches(f));		
	}

	@Test
	public void whenRootViolatorMatchesAtRootThenChildAccepted() {
		DigesterRulesPattern p = new DigesterRulesPattern("root/child");
		assertNull(p.getError());
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertEquals("ViolationChecker backtracking(root) at root", v.toString());
		TestDigesterRulesFrankElement f = new TestDigesterRulesRootFrankElement("root");
		assertTrue(v.matches(f));
	}

	@Test
	public void whenRootViolatorMatchesButNotAtRootThenChildViolates() {
		DigesterRulesPattern p = new DigesterRulesPattern("child/grandChild");
		assertNull(p.getError());
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		TestDigesterRulesFrankElement f = new TestDigesterRulesFrankElement();
		f.addRootOwnedParent("child", "root");
		assertFalse(v.matches(f));
	}
}
