package nl.nn.adapterframework.frankdoc.testtarget.walking;

public class Child5 extends Parent {
	// Technical override, should not prevent the algorithm from omitting it.
	@Override
	public void setParentAttributeFirst(String value) {
	}

	/** Documented child */
	public void setParentAttributeSecond(String value) {
	}
}
