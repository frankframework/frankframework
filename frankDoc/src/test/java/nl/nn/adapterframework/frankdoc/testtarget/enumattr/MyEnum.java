package nl.nn.adapterframework.frankdoc.testtarget.enumattr;

import nl.nn.adapterframework.doc.EnumLabel;

public enum MyEnum {
	// These values are not sorted alphabetically. With these values we test that the values are ordered by occurrence,
	// not alphabetically, when put into the model.
	TWO,

	/** Description of customLabelOne */ @EnumLabel("customLabelOne")
	ONE,

	THREE;
}
