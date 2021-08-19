package nl.nn.adapterframework.frankdoc.testtarget.examples.ignore.attributes;

public class GrandChild extends SuppressingChild{
	// Not re-introduced, technical override
	@Override
	public void setSuppressedAttribute(String value) {
	}

	/** Documented, so re-introduced as attribute */
	@Override
	public void setSuppressedAttribute2(String value) {
	}
}
