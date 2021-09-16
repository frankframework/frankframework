package nl.nn.adapterframework.frankdoc.testtarget.examples.exclude;

// It is important that this class does not have shown attributes.
// It only has excluded attributes. We want to test that GroupCreator
// considers it properly.
public class Child extends Parent {
	/** @ff.noAttribute */
	public void setChildExcludedAttribute(String value) {
	}
}
