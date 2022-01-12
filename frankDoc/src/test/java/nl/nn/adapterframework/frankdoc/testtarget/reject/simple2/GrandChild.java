package nl.nn.adapterframework.frankdoc.testtarget.reject.simple2;

public class GrandChild extends Child {
	/**
	 * This does not re-introduce the attribute. If we would allow that, the logic of
	 * ignoring attributes unless they are introduced by another inherited interface
	 * would be broken.
	 */
	@Override
	public void setAttributeIIgnored(String value) {
	}
}
