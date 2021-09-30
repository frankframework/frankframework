package nl.nn.adapterframework.frankdoc.testtarget.attribute.overload;

public class Master extends Parent {
	// Should produce a warning because we inherit setOverloadedInherited(int).
	public void setOverloadedInherited(String arg) {
	}

	public void setOverloadedEnum(String arg) {
	}

	public void setOverloadedEnum(MyEnum arg) {
	}
}
