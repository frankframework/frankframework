package org.frankframework.stream;

import java.io.IOException;
import java.io.StringWriter;

public class CloseObservableWriter extends StringWriter {

	private boolean closeCalled=false;

	@Override
	public void close() throws IOException {
		super.close();
		closeCalled=true;
	}

	public boolean isCloseCalled() {
		return closeCalled;
	}
}
