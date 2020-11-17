package nl.nn.adapterframework.doc.testtarget.examples.simple;

import nl.nn.adapterframework.doc.IbisDoc;

public class DescribedPossibleIChild implements IChild {
	@IbisDoc({"20", "Second attribute of DescribedPossibleIChild"})
	public void setSecondAttribute(String value) {
	}

	@IbisDoc({"10", "First attribute of DescribedPossibleIChild"})
	public void setFirstAttribute(String value) {
	}
}
