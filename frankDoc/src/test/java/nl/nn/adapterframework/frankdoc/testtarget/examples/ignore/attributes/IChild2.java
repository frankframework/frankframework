package nl.nn.adapterframework.frankdoc.testtarget.examples.ignore.attributes;

import nl.nn.adapterframework.doc.FrankDocGroup;

@FrankDocGroup(name = "Group 2", order = 20)
public interface IChild2 {
	public void setNotSuppressedAttribute(String value);
}
