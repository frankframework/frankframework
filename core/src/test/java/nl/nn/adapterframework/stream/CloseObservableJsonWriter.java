package nl.nn.adapterframework.stream;

import nl.nn.adapterframework.stream.json.JsonWriter;

public class CloseObservableJsonWriter extends JsonWriter {

	private boolean closeCalled=false;
	
	@Override
	public boolean endJSON() {
		closeCalled=true;
		return super.endJSON();
	}

	public boolean isCloseCalled() {
		return closeCalled;
	}
}
