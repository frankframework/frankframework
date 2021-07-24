package nl.nn.adapterframework.frankdoc.testtarget.doclet;

public interface MyInterface extends MyInterfaceParent {
	/**
	 * This is the javadoc of "myAnnotatedMethod".
	 * @author martijn
	 * @ff.default InheritedDefault
	 */
	@Deprecated
	void myAnnotatedMethod();
}
