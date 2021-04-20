package nl.nn.adapterframework.frankdoc.testtarget.doclet;

import nl.nn.adapterframework.doc.IbisDoc;

public class Parent {
	// We test here that inner classes are omitted as implementations of an interface.
	public class InnerMyInterfaceImplementation implements MyInterface {
		@Override
		public void myAnnotatedMethod() {
		}
	}

	/**
	 * This is the JavaDoc of method "setInherited".
	 * @param value
	 */
	@IbisDoc("50")
	public void setInherited(String value) {
	}

	public String getInherited() {
		return null;
	}
}