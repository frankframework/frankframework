package nl.nn.adapterframework.frankdoc.testtarget.groups;

import nl.nn.adapterframework.doc.IbisDoc;

public class GroupContainer implements IGroupContainer {
	@IbisDoc("20")
	public void setListener(IGroupContainer child) {
	}

	@IbisDoc("10")
	public void setListener(GroupChild groupChild) {
	}
}
