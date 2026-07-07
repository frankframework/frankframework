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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.frankframework.util.XmlUtils;

public class NodeConverter extends StringableDataConverter<Node> {
	@Override
	public String asString(Node data) {
		try {
			return XmlUtils.nodeToString(data);
		} catch (TransformerException e) {
			throw new RuntimeException("Could not convert type Node to String", e);
		}
	}

	@Override
	public byte[] asByteArray(Node data) throws IOException {
		try {
			return XmlUtils.nodeToByteArray(data);
		} catch (TransformerException e) {
			throw new RuntimeException("Could not convert type Node to byte[]", e);
		}
	}

	@Override
	public byte[] asByteArray(Node data, String encodingCharset) throws IOException {
		return asByteArray(data);
	}

	@Override
	public @Nullable Source asSource(Node data) throws IOException, SAXException {
		return new DOMSource(data);
	}
}
