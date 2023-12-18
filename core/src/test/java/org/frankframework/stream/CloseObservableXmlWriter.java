package org.frankframework.stream;

import org.xml.sax.SAXException;

import org.frankframework.xml.XmlWriter;

public class CloseObservableXmlWriter extends XmlWriter {

	private boolean closeCalled=false;

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		closeCalled=true;
	}

	public boolean isCloseCalled() {
		return closeCalled;
	}
}
