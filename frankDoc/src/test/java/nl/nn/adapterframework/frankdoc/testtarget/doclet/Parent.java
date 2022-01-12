package nl.nn.adapterframework.frankdoc.testtarget.doclet;

import nl.nn.adapterframework.doc.IbisDoc;

/** @ff.myTag */
@Java5Annotation(myStringArray = {"first", "second"}, myString = "A string", myInt = 5)
public class Parent {
	// We test here that inner classes are omitted as implementations of an interface.
	public class InnerMyInterfaceImplementation implements MyInterface {
		@Override
		public void myAnnotatedMethod() {
		}
	}

	// There are spaces around the @ff.default value, please leave them! We test that the value is trimmed.
	/**
	 * This is the JavaDoc of method "setInherited".
	 * @param value
	 * @ff.default   DefaultValue   
	 */
	@IbisDoc("50")
	public void setInherited(String value) {
	}

	public String getInherited() {
		return null;
	}

	// Asking this JavaDoc tag should produce an empty string.
	/** @ff.default */
	public void myMethod() {
	}
}