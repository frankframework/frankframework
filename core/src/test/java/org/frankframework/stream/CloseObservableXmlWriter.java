package org.frankframework.stream;

import org.frankframework.xml.XmlWriter;
import org.xml.sax.SAXException;

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
