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

import javax.xml.transform.Source;

import org.jspecify.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.frankframework.util.XmlUtils;

public sealed interface DataConverter permits CharacterDataConverter, BinaryDataConverter {
	boolean isBinary();

	long size();
	boolean isEmpty();

	byte @Nullable [] asByteArray() throws IOException;
	InputStream asInputStream() throws IOException;

	@Nullable InputSource asInputSource() throws IOException;

	default @Nullable Source asSource() throws IOException, SAXException {
		return XmlUtils.inputSourceToSAXSource(asInputSource());
	}
}
