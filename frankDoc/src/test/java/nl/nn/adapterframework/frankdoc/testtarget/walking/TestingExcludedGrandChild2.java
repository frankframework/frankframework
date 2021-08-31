package nl.nn.adapterframework.frankdoc.testtarget.walking;

public class TestingExcludedGrandChild2 extends TestingExcludedChildExcludingInterface {
	// Technical override, not reintroduced.
	@Override
	public void setExcludedAttribute1(String value) {
	}

	/** Documented, so reintroduced as attribute */
	public void setExcludedAttribute2(String value) {
	}
}
