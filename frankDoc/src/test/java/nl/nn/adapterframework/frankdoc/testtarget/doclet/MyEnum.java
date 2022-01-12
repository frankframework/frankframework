package nl.nn.adapterframework.frankdoc.testtarget.doclet;

public enum MyEnum {
	// These constants are not sorted alphabetically. We can thus test that the enum constants are not sorted.
	ONE("dummy"),
	TWO("dummy"),
	THREE("dummy");

	// We test that this field is not taken as an enum constant.
	// Note that this field is private, so public fields might
	// still be erroneously taken as enum constants. This is
	// not relevant in practice, because we do not use enums
	// with non-private fields.
	@SuppressWarnings("unused")
	private String notEnumField;

	MyEnum(String dummyValue) {
		this.notEnumField = dummyValue;
	}
}
