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

import java.io.Serializable;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Node;

import org.frankframework.util.XmlUtils;

final class NodeConverter extends StringableDataConverter<Node> {

	@Override
	public Serializable asSerializable(Node data) {
		return asString(data);
	}

	@Override
	public String asString(Node data) {
		try {
			return XmlUtils.nodeToString(data);
		} catch (TransformerException e) {
			throw new IllegalArgumentException("Could not convert type Node to String", e);
		}
	}

	@Override
	public byte[] asByteArray(Node data) {
		try {
			return XmlUtils.nodeToByteArray(data);
		} catch (TransformerException e) {
			throw new IllegalArgumentException("Could not convert type Node to byte[]", e);
		}
	}

	@Override
	public Source asSource(Node data) {
		return new DOMSource(data);
	}
}
