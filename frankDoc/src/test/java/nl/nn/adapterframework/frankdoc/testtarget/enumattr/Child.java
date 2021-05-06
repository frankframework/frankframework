package nl.nn.adapterframework.frankdoc.testtarget.enumattr;

public class Child extends Parent {
	public void setChildStringAttribute(String value) {
	}

	public MyEnum getChildStringAttributeEnum() {
		return MyEnum.TWO;
	}

	// Attributes are sorted alphabetically by name. This one should be included before attribute childString
	public void setChildIntAttribute(int value) {
	}

	public String getNotForAttributeBecauseNoEnumReturnedEnum() {
		return null;
	}

	public void getNotForAttributeBecauseReturnsVoidEnum() {
	}

	public MyEnum getNotForAttributeBecauseHasArgumentEnum(int value) {
		return MyEnum.TWO;
	}
}
