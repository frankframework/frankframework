package nl.nn.adapterframework.doc.testtarget.enumattr;

public class Child extends Parent {
	public MyEnum getChildAttributeEnum() {
		return MyEnum.TWO;
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
