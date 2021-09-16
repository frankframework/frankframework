package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

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
	public void whenPatternHasMultipleWordsWithoutWildcardThenNotSupported() {
		assertNotNull(new DigesterRulesPattern("first/second").getError());
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
	public void whenPatternIsStarThenMultipleWordsWithoutRootThenNonRootHaveViolationChecker() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/receiver/listener");
		assertNull(p.getError());
		assertFalse(p.isRoot());
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertEquals("*/receiver/listener", v.getOriginalPattern());
		assertTrue(v.checkImplemented(new HashSet<String>(Arrays.asList("configuration"))));
	}

	@Test
	public void whenPatternIsStartThenMultipleWordsIncludingRootThenNotImplemented() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/configuration/adapter");
		assertNull(p.getError());
		assertFalse(p.isRoot());
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertFalse(v.checkImplemented(new HashSet<>(Arrays.asList("configuration"))));
	}

	@Test
	public void whenViolaterMatchesPatternThenCheckSucceeds() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/adapter/receiver/listener");
		TestDigesterRulesConfigChild c = new TestDigesterRulesConfigChild("listener");
		c.addParent("notRelevant");
		c.addParent("receiver").addParent("adapter");
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertEquals("ViolationChecker backtracking(receiver, adapter)", v.toString());
		assertTrue(v.check(c));
	}

	@Test
	public void whenViolaterDoesNotMatchThenCheckFails() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/receiver/listener");
		TestDigesterRulesConfigChild c = new TestDigesterRulesConfigChild("listener");
		c.addParent("somethingElse");
		DigesterRulesPattern.ViolationChecker v = p.getViolationChecker();
		assertEquals("ViolationChecker backtracking(receiver)", v.toString());
		assertFalse(v.check(c));		
	}

	@Test
	public void whenViolaterDoesNotMatchBecauseParentOmittedThenCheckFails() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/adapter/receiver/listener");
		TestDigesterRulesConfigChild c = new TestDigesterRulesConfigChild("listener");
		c.addParent("receiver");
		// reveiver has no parents, so no match.
		assertFalse(p.getViolationChecker().check(c));		
	}

	@Test
	public void whenViolatorMatchesPatternButParentViolatesThenChildViolates() {
		DigesterRulesPattern p = new DigesterRulesPattern("*/violated/listener");
		TestDigesterRulesConfigChild c = new TestDigesterRulesConfigChild("listener");
		TestDigesterRulesConfigChild parent = c.addParent("violated");
		parent.setViolatesDigesterRules(true);
		assertFalse(p.getViolationChecker().check(c));
	}
}
