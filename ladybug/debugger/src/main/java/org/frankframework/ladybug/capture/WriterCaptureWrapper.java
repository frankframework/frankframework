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
package org.frankframework.ladybug.capture;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;

public class WriterCaptureWrapper extends Writer {
	private final AtomicBoolean hasDataWritten = new AtomicBoolean(false);
	private static final char[] NO_DATA_WRITTEN = ">> Captured writer was closed without being read.".toCharArray();
	private final Writer delegate;

	public WriterCaptureWrapper(Writer out) {
		delegate = out;
	}

	@Override
	public void flush() throws IOException {
		delegate.flush();
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		hasDataWritten.set(true);
		delegate.write(cbuf, off, len);
	}

	@Override
	public void close() throws IOException {
		if(!hasDataWritten.getAndSet(true)) {
			delegate.write(NO_DATA_WRITTEN);
		}

		delegate.close();
	}
}
