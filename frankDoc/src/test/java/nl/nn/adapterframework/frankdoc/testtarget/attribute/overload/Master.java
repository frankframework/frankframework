package nl.nn.adapterframework.frankdoc.testtarget.attribute.overload;

public class Master extends Parent {
	// Should produce a warning because we inherit setOverloadedInherited(int).
	public void setOverloadedInherited(String arg) {
	}

	public void setOverloadedEnum(String arg) {
	}

	public void setOverloadedEnum(MyEnum arg) {
	}

	public void setMyAttribute(MyEnum arg) {
	}

	// Should produce a warning, return type differs from argument type of setter.
	public String getMyAttribute() {
		return null;
	}
}
