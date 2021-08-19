package nl.nn.adapterframework.frankdoc.testtarget.examples.ignore.attributes;

import nl.nn.adapterframework.doc.FrankDocGroup;

@FrankDocGroup(name = "Group 1", order = 10)
public interface IChild1 {
	public void setSuppressedAttribute(String value);
	public void setSuppressedAttribute2(String value);
	public void setNotSuppressedAttribute(String value);
}
