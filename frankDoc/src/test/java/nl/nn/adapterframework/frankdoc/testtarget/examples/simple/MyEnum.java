package nl.nn.adapterframework.frankdoc.testtarget.examples.simple;

import nl.nn.adapterframework.doc.EnumLabel;

public enum MyEnum {
	ONE,
	
	/** Description of TWO */ @EnumLabel("customLabelTwo")
	TWO;
}
