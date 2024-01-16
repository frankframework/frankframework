package org.frankframework.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CloseObservableOutputStream extends ByteArrayOutputStream {

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
