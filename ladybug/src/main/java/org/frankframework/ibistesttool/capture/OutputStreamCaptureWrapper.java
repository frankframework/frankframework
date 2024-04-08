/*
   Copyright 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.ibistesttool.capture;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class OutputStreamCaptureWrapper extends OutputStream {
	private final AtomicBoolean hasDataWritten = new AtomicBoolean(false);
	private static final byte[] NO_DATA_WRITTEN = ">> Captured stream was closed without being read.".getBytes(StandardCharsets.UTF_8);
	private final OutputStream delegate;

	public OutputStreamCaptureWrapper(OutputStream out) {
		delegate = out;
	}

	@Override
	public void write(int b) throws IOException {
		hasDataWritten.set(true);
		delegate.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		hasDataWritten.set(true);
		delegate.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		hasDataWritten.set(true);
		delegate.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		delegate.flush();
	}

	@Override
	public void close() throws IOException {
		if(!hasDataWritten.get()) {
			delegate.write(NO_DATA_WRITTEN);
		}

		delegate.close();
	}

}
