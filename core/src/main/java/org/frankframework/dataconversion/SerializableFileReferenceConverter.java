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

import org.frankframework.stream.SerializableFileReference;
import org.frankframework.util.StreamUtil;

public class SerializableFileReferenceConverter implements BinaryDataConversionSupport<SerializableFileReference> {

	@Override
	public @Nullable String asString(SerializableFileReference data, String encodingCharset) throws IOException {
		return StreamUtil.readerToString(asReader(data, encodingCharset), null);
	}

	@Override
	public Reader asReader(SerializableFileReference data, String encodingCharset) throws IOException {
		return new InputStreamReader(data.getInputStream(), encodingCharset);
	}

	@Override
	public long size(SerializableFileReference data) {
		return data.getSize();
	}

	@Override
	public byte @Nullable [] asByteArray(SerializableFileReference data) throws IOException {
		return StreamUtil.streamToBytes(data.getInputStream());
	}

	@Override
	public InputStream asInputStream(SerializableFileReference data) throws IOException {
		return data.getInputStream();
	}
}
