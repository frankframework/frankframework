package nl.nn.adapterframework.frankdoc.testtarget.doclet;

public class Child extends Parent implements MyInterface {
	@Override
	public void setInherited(String value) {
	}

	String packagePrivateMethod() {
		return null;
	}
}
