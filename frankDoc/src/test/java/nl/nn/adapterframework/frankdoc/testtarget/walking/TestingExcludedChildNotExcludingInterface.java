package nl.nn.adapterframework.frankdoc.testtarget.walking;

public class TestingExcludedChildNotExcludingInterface extends TestingExcludedParent {
	/** This attribute is documented */
	@Override
	public void setExcludedAttribute2(String value) {
	}

	public void setChildAttribute(String value) {
	}

	/** @ff.noAttribute */
	public void setNotChildAttribute(String value) {
	}
}
