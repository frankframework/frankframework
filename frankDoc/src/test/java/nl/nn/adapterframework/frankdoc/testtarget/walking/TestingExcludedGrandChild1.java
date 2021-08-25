package nl.nn.adapterframework.frankdoc.testtarget.walking;

// Inherits everything not excluded from TestingExcludedChildExcludingInterface
public class TestingExcludedGrandChild1 extends TestingExcludedChildExcludingInterface {
	/** Documented, we reintroduce it */
	@Override
	public void setNotChildAttribute(String value) {
	}
}
