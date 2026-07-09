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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.transform.Source;

import org.jspecify.annotations.Nullable;
import org.xml.sax.InputSource;

class NullDataConverter implements BinaryDataConversionSupport<NullMarker> {
	@Override
	public @Nullable String asString(NullMarker data, String encodingCharset) throws IOException {
		return null;
	}

	@Override
	public Reader asReader(NullMarker data, String encodingCharset) throws IOException {
		return new BufferedReader(Reader.nullReader());
	}

	@Override
	public boolean isEmpty(NullMarker data) {
		return true;
	}

	@Override
	public long size(NullMarker data) {
		return 0;
	}

	@Override
	public byte @Nullable[] asByteArray(NullMarker data) throws IOException {
		return null	;
	}

	@Override
	public InputStream asInputStream(NullMarker data) throws IOException {
		return new BufferedInputStream(InputStream.nullInputStream());
	}

	@Override
	public @Nullable Source asSource(NullMarker data) throws IOException {
		return null;
	}

	@Override
	public @Nullable InputSource asInputSource(NullMarker data) throws IOException {
		return null;
	}

	@Override
	public @Nullable InputSource asInputSource(NullMarker data, String charset) throws IOException {
		return null;
	}
}
