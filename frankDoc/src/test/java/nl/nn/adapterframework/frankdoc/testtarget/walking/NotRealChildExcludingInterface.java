package nl.nn.adapterframework.frankdoc.testtarget.walking;

/** @ff.ignoreTypeMembership nl.nn.adapterframework.frankdoc.testtarget.walking.INotRealExcluded */
public class NotRealChildExcludingInterface extends NotRealParent {
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
