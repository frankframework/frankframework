package nl.nn.adapterframework.frankdoc.testtarget.attribute.enumExclude;

/**
 * @ff.ignoreTypeMembership nl.nn.adapterframework.frankdoc.testtarget.attribute.enumExclude.IIgnored
 * @author martijn
 *
 */
public class AttributeOwner implements IChild, IIgnored {
	// Ignored because of @ff.ignoreTypeMembership JavaDoc tag.
	@Override
	public void setIgnored(MyEnum arg) {
	}

	/**
	 * @ff.noAttribute
	 * @param arg
	 */
	public void setNotAttribute(MyEnum arg) {
	}

	// Produces an attribute.
	public void setEnumAttribute(MyEnum arg) {
	}
}
