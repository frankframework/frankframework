/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.io.InputStream;

public class ReleaseConnectionAfterReadInputStream extends InputStream {
	InputStream inputStream;
	HttpResponseHandler responseHandler;

	public ReleaseConnectionAfterReadInputStream(HttpResponseHandler responseHandler, InputStream inputStream) throws IOException {
		this.responseHandler = responseHandler;
		this.inputStream = inputStream;
	}

	public ReleaseConnectionAfterReadInputStream(HttpResponseHandler responseHandler) throws IOException {
		this.responseHandler = responseHandler;
		this.inputStream = responseHandler.getResponse();
	}

	public int read() throws IOException {
		int i = inputStream.read();
		if (i == -1 && responseHandler != null) {
			responseHandler.close();
		}
		return i;
	}

	public int read(byte[] b) throws IOException {
		int i = inputStream.read(b);
		if (i == -1 && responseHandler != null) {
			responseHandler.close();
		}
		return i;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		int i = inputStream.read(b, off, len);
		if (i == -1 && responseHandler != null) {
			responseHandler.close();
		}
		return i;
	}

	public void close() throws IOException {
		inputStream.close();
		if (responseHandler != null) {
			responseHandler.close();
		}
	}
}
