package nl.nn.adapterframework.doc.testtarget.ibisdocref;

import nl.nn.adapterframework.doc.IbisDoc;

public class ChildTarget extends ParentTarget {
	@IbisDoc("Description of ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault")
	public void setIbisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault(String value) {
	}

	@IbisDoc("Description of otherMethod")
	public void otherMethod(String value) {
	}
}
