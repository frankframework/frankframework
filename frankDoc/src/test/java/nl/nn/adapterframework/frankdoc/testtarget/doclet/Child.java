package nl.nn.adapterframework.frankdoc.testtarget.doclet;

/**
 * This is test class "Child". We use this comment to see how
 * JavaDoc text is treated by the Doclet API.
 * @author martijn
 *
 */
public class Child extends Parent implements MyInterface {
	@Override
	public void setInherited(String value) {
	}

	String packagePrivateMethod() {
		return null;
	}

	public void setVarargMethod(String ...value) {
	}

	public enum MyInnerEnum {INNER_FIRST, INNER_SECOND};

	public MyInnerEnum getMyInnerEnum() {
		return null;
	}

	@Override
	public void myAnnotatedMethod() {
	}

	public void methodWithoutAnnotations() {
	}
}
