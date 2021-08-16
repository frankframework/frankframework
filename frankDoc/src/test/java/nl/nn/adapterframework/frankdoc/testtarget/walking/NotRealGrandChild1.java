package nl.nn.adapterframework.frankdoc.testtarget.walking;

// Inherits everything not excluded from NotRealChildExcludingInterface
public class NotRealGrandChild1 extends NotRealChildExcludingInterface {
	/** Documented, we reintroduce it */
	@Override
	public void setNotChildAttribute(String value) {
	}
}
