/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.align;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.xerces.xs.XSElementDeclaration;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

public class NamespaceAligningFilter extends XMLFilterImpl {
	
	private XmlAligner aligner;
	private Stack<ElementInfo> stack = new Stack<>();

	private Map<String,String> namespacePrefixes=new HashMap<>();
	
	private class ElementInfo {
		String namespacePrefix;
		String namespaceUri;
		boolean namespacePrefixCreated;
	}
	
	public NamespaceAligningFilter(XmlAligner aligner, ContentHandler handler) {
		super();
		this.aligner = aligner;
		setContentHandler(handler);
	}

	@Override
	public void startElement(String namespaceUri, String localName, String qName, Attributes attributes) throws SAXException {
		XSElementDeclaration elementDeclaration = aligner.findElementDeclarationForName(namespaceUri, localName);
		ElementInfo elementInfo = new ElementInfo();
		elementInfo.namespaceUri = elementDeclaration.getNamespace();
		elementInfo.namespacePrefix = findPrefix(elementInfo.namespaceUri);
		if (elementInfo.namespacePrefix==null) {
			createPrefix(elementInfo);
		}
		stack.push(elementInfo);
		super.startElement(elementInfo.namespaceUri, localName, elementInfo.namespacePrefix+":"+localName, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		ElementInfo elementInfo = stack.pop();
		super.endElement(elementInfo.namespaceUri, localName, elementInfo.namespacePrefix+":"+localName);
		if (elementInfo.namespacePrefixCreated) {
			endPrefixMapping(elementInfo.namespacePrefix);
		}
	}

	protected String findPrefix(String uri) {
		return namespacePrefixes
			.entrySet().stream()
			.filter(e -> e.getValue().equals(uri))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
	}

	protected void createPrefix(ElementInfo elementInfo) throws SAXException {
		int i=1; 
		while (namespacePrefixes.containsKey("ns"+i)) {
			i++;
		}
		elementInfo.namespacePrefix = "ns"+i;
		startPrefixMapping(elementInfo.namespacePrefix, elementInfo.namespaceUri);
		elementInfo.namespacePrefixCreated=true;
	}

	
	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		namespacePrefixes.put(prefix,uri);
		super.startPrefixMapping(prefix, uri);
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		super.endPrefixMapping(prefix);
		namespacePrefixes.remove(prefix);
	}


}
