package nl.nn.adapterframework.frankdoc.testtarget.examples.simple;

import nl.nn.adapterframework.doc.IbisDoc;

/**
 * This is the header of the JavaDoc of "DescribedPossibleIChild".
 * 
 * And this is remaining documentation of "DescribedPossibleIChild".
 * @author martijn
 *
 */
public class DescribedPossibleIChild implements IChild {
	@IbisDoc({"10", "First attribute of DescribedPossibleIChild.", "this default value"})
	public void setFirstAttribute(String value) {
	}

	@IbisDoc({"20", "Second attribute of DescribedPossibleIChild"})
	public void setSecondAttribute(String value) {
	}
}
