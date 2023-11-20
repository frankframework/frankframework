/*
   Copyright 2022-2023 WeAreFrank!

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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.xml.SaxException;

public class NamespaceAligningFilter extends XMLFilterImpl {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private final XmlAligner aligner;
	private final Deque<ElementInfo> stack = new ArrayDeque<>();

	private final Map<String, String> namespacePrefixes = new HashMap<>();

	private static class ElementInfo {
		String namespacePrefix;
		String namespaceUri;
		boolean namespacePrefixCreated;
		XSTypeDefinition type;
	}

	public NamespaceAligningFilter(XmlAligner aligner, ContentHandler handler) {
		super();
		this.aligner = aligner;
		setContentHandler(handler);
	}

	protected String findNamespaceOfChildElement(XSParticle particle, String localName) throws SAXException {
		XSTerm term = particle.getTerm();

		if (localName.equals(term.getName())) {
			return term.getNamespace();
		}

		if (term instanceof XSModelGroup) {
			XSModelGroup modelGroup = (XSModelGroup)term;
			XSObjectList particles = modelGroup.getParticles();
			for (int i=0;i<particles.getLength();i++) {
				XSParticle childParticle = (XSParticle)particles.item(i);
				String namespace = findNamespaceOfChildElement(childParticle, localName);
				if (namespace!=null) {
					return namespace;
				}
			}
		} else if (term instanceof XSWildcard) {
			XSWildcard wildcard = (XSWildcard)term;
			if (StringUtils.isNotEmpty(wildcard.getNamespace())) {
				return wildcard.getNamespace();
			}
			XSElementDeclaration elementDeclaration = aligner.findElementDeclarationForName(null, localName);
			if (elementDeclaration!=null) {
				return elementDeclaration.getNamespace();
			}
			if (log.isTraceEnabled()) log.trace("Cannot find elementDeclaration for ["+localName+"], assuming it has parents namespace");
			return stack.peek().namespaceUri;
		}
		return null;
	}


	@Override
	public void startElement(String namespaceUri, String localName, String qName, Attributes attributes) throws SAXException {
		if (StringUtils.isEmpty(namespaceUri)) {
			namespaceUri=null;
		}
		ElementInfo elementInfo = new ElementInfo();
		if (stack.isEmpty()) {
			XSElementDeclaration elementDeclaration = aligner.findElementDeclarationForName(namespaceUri, localName);
			elementInfo.namespaceUri = elementDeclaration.getNamespace();
		} else {
			XSTypeDefinition parentType = stack.peek().type;
			if (parentType instanceof XSComplexTypeDefinition) {
				XSComplexTypeDefinition complexType = (XSComplexTypeDefinition)parentType;
				XSParticle particle = complexType.getParticle();
				elementInfo.namespaceUri = findNamespaceOfChildElement(particle, localName);
			} else {
				throw new SaxException("parent ["+parentType.getName()+"] of ["+localName+"] is simple type, cannot have children");
			}
		}
		if (elementInfo.namespaceUri!=null) {
			elementInfo.namespacePrefix = findPrefix(elementInfo.namespaceUri);
			if (elementInfo.namespacePrefix==null) {
				createPrefix(elementInfo);
			}
		}
		stack.push(elementInfo);
		String prefix = StringUtils.isEmpty(elementInfo.namespacePrefix) ? "" : elementInfo.namespacePrefix+":";
		super.startElement(elementInfo.namespaceUri, localName, prefix+localName, attributes);
		elementInfo.type = aligner.getTypeDefinition();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		ElementInfo elementInfo = stack.pop();
		String prefix = StringUtils.isEmpty(elementInfo.namespacePrefix) ? "" : elementInfo.namespacePrefix+":";
		super.endElement(elementInfo.namespaceUri, localName, prefix+localName);
		if (elementInfo.namespacePrefixCreated) {
			endPrefixMapping(elementInfo.namespacePrefix);
		}
	}

	protected String findPrefix(String uri) {
		return namespacePrefixes
			.entrySet().stream()
			.filter(e -> uri.equals(e.getValue()) )
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
	}

	protected void createPrefix(ElementInfo elementInfo) throws SAXException {
		if (!namespacePrefixes.containsKey("")) {
			elementInfo.namespacePrefix = "";
		} else {
			int i=1;
			while (namespacePrefixes.containsKey("ns"+i)) {
				i++;
			}
			elementInfo.namespacePrefix = "ns"+i;
		}
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
