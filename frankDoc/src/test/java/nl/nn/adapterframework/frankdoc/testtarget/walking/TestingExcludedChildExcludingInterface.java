package nl.nn.adapterframework.frankdoc.testtarget.walking;

/** @ff.ignoreTypeMembership nl.nn.adapterframework.frankdoc.testtarget.walking.ITestingExcluded */
public class TestingExcludedChildExcludingInterface extends TestingExcludedParent {
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
