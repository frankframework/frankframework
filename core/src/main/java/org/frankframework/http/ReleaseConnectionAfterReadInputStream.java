/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2025 WeAreFrank!

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
package org.frankframework.http;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.annotation.Nullable;

public class ReleaseConnectionAfterReadInputStream extends FilterInputStream {
	HttpResponseHandler responseHandler;

	public ReleaseConnectionAfterReadInputStream(@Nullable HttpResponseHandler responseHandler, @Nullable InputStream inputStream) {
		super(inputStream != null ? inputStream : InputStream.nullInputStream());
		this.responseHandler = responseHandler;
	}

	@Override
	public void close() throws IOException {
		try {
			super.close();
		} finally {
			if (responseHandler != null) {
				responseHandler.close();
			}
		}
	}
}
