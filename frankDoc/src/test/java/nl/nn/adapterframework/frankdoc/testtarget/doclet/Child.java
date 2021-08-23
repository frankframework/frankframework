package nl.nn.adapterframework.frankdoc.testtarget.doclet;

/**
 * This is test class "Child". We use this comment to see how
 * JavaDoc text is treated by the Doclet API.
 * @author martijn
 *
 * @ff.myTag This is the tag argument.  
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

	public enum MyInnerEnum {
		INNER_FIRST,
		
		// It would be nice if the JavaDoc could go after the Java 5 annotation.
		// The doclet API does not support that, however.
		/** Description of INNER_SECOND */ @Java5Annotation(myStringArray = {"a", "b"}, myString = "s", myInt = 4)
		INNER_SECOND};

	public MyInnerEnum getMyInnerEnum() {
		return null;
	}

	@Override
	public void myAnnotatedMethod() {
	}

	public void methodWithoutAnnotations() {
	}
}
