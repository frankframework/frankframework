package nl.nn.adapterframework.frankdoc.testtarget.doclet;

public interface MyInterface extends MyInterfaceParent {
	/**
	 * This is the javadoc of "myAnnotatedMethod".
	 * @author martijn
	 * @frankdoc.default InheritedDefault
	 */
	@Deprecated
	void myAnnotatedMethod();
}
