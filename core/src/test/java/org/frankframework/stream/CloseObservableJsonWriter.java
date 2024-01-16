package org.frankframework.stream;

import org.frankframework.stream.json.JsonWriter;
import org.xml.sax.SAXException;

public class CloseObservableJsonWriter extends JsonWriter {

	private boolean closeCalled=false;

	@Override
	public void endDocument() throws SAXException {
		closeCalled=true;
		super.endDocument();
	}

	public boolean isCloseCalled() {
		return closeCalled;
	}
}
