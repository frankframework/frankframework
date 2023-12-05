/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.validation;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import javanet.staxutils.helpers.StreamWriterDelegate;

/**
 * This writer simply filters an other writer. It copies everything, except it
 * maps 'wrong' namespaces to correct ones.
 * 
 * @author Michiel Meeuwissen
 */
public class NamespaceCorrectingXMLStreamWriter extends StreamWriterDelegate {
	private final Map<String, String> map;

	public NamespaceCorrectingXMLStreamWriter(XMLStreamWriter out, Map<String, String> map) {
		super(out);
		this.map = map;
	}

	@Override
	public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
		super.writeAttribute(prefix, correct(namespaceURI), localName, value);
	}

	@Override
	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		super.writeNamespace(prefix, correct(namespaceURI));
	}

	@Override
	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		super.writeDefaultNamespace(correct(namespaceURI));
	}

	/**
	 * Uses a Map to 'correct' values. If there is no corresponding key in the map,
	 * the value itself will simply be returned, otherwise the corrected value which
	 * is the value in the map.
	 */
	private String correct(String ns) {
		return map.containsKey(ns) ? map.get(ns) : ns;
	}

}
