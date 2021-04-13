package nl.nn.adapterframework.stream;

import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.json.JsonWriter;

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
