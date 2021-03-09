package nl.nn.adapterframework.doc.testtarget.enumattr;

public class Child extends Parent {
	public MyEnum getChildAttributeEnum() {
		return MyEnum.TWO;
	}

	public String getNotForAttributeBecauseNoEnumReturnedEnum() {
		return null;
	}
}
