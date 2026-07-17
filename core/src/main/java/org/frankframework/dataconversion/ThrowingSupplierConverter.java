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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.stream.Message;
import org.frankframework.stream.SerializableFileReference;
import org.frankframework.util.StreamUtil;

final class ThrowingSupplierConverter implements TypedBinaryDataConverter<ThrowingSupplier<InputStream, Exception>> {

	@Override
	public boolean prefersStreaming() {
		return true;
	}

	@Override
	public long size(ThrowingSupplier<InputStream, Exception> data) {
		return Message.MESSAGE_SIZE_UNKNOWN;
	}

	@Override
	public byte[] asByteArray(ThrowingSupplier<InputStream, Exception> data) throws IOException {
		return StreamUtil.streamToBytes(asInputStream(data));
	}

	@Override
	public InputStream asInputStream(ThrowingSupplier<InputStream, Exception> data) throws IOException {
		try {
			InputStream inputStream = data.get();
			if (inputStream.markSupported()) {
				return inputStream;
			}
			return new BufferedInputStream(inputStream);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean isEmpty(ThrowingSupplier<InputStream, Exception> data) {
		try (InputStream is = data.get()) {
			return is.read() == -1;
		} catch (Exception e) {
			// On any exception, consider empty
			return true;
		}
	}

	@Override
	public Serializable asSerializable(ThrowingSupplier<InputStream, Exception> data) throws IOException {
		try {
			SerializableFileReference sfr = SerializableFileReference.of(data.get());
			if (sfr.getSize() > Message.MESSAGE_MAX_IN_MEMORY) {
				return sfr;
			}
			// Load back into memory for performance
			try (sfr) {
				return StreamUtil.streamToBytes(sfr.getInputStream());
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
