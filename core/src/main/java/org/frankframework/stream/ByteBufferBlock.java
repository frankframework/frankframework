/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.frankframework.util.StreamUtil;

class ByteBufferBlock {

	final byte[] buffer = new byte[StreamUtil.BUFFER_SIZE];

	int count;

	boolean isFull() {
		return count == buffer.length;
	}

	int available() {
		return buffer.length - count;
	}

	int addFromStream(InputStream source, int maxBytesToRead) throws IOException {
		final int n = Math.min(available(), maxBytesToRead);
		int bytesRead = source.read(buffer, count, n);
		if (bytesRead == -1) {
			return -1;
		}
		count += bytesRead;
		return bytesRead;
	}

	void transferToStream(OutputStream out) throws IOException {
		out.write(buffer, 0, count);
	}
}
