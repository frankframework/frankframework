/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.dataconversion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.jspecify.annotations.Nullable;

import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;

public class ThrowingSupplierConverter implements BinaryDataConversionSupport<ThrowingSupplier<InputStream, Exception>> {
	@Override
	public String asString(ThrowingSupplier<InputStream, Exception> data, String encodingCharset) throws IOException {
		return StreamUtil.readerToString(asReader(data, encodingCharset), null);
	}

	@Override
	public Reader asReader(ThrowingSupplier<InputStream, Exception> data, String encodingCharset) throws IOException {
		return new InputStreamReader(asInputStream(data), encodingCharset);
	}

	@Override
	public long size(ThrowingSupplier<InputStream, Exception> data) {
		return Message.MESSAGE_SIZE_UNKNOWN;
	}

	@Override
	public byte @Nullable [] asByteArray(ThrowingSupplier<InputStream, Exception> data) throws IOException {
		return StreamUtil.streamToBytes(asInputStream(data));
	}

	@Override
	public InputStream asInputStream(ThrowingSupplier<InputStream, Exception> data) throws IOException {
		try {
			return data.get();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean isEmpty(ThrowingSupplier<InputStream, Exception> data) {
		try (InputStream is = asInputStream(data)) {
			return is.read() == -1;
		} catch (IOException e) {
			// On any exception, consider empty
			return true;
		}
	}
}
