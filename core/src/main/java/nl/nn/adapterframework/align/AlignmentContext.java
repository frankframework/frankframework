/*
   Copyright 2018 Nationale-Nederlanden, 2021 WeAreFrank!

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

import java.util.Set;

import org.apache.xerces.xs.XSTypeDefinition;
import org.xml.sax.Attributes;

/**
 * Top of a stack of parsed elements, that represent the current position in the aligned document.
 */
public class AlignmentContext {

	private AlignmentContext parent;

	private String namespaceUri;
	private String localName;
	private String qName;
	private Attributes attributes;

	private XSTypeDefinition typeDefinition;
	private int indentLevel;

	private Set<String> multipleOccurringChildElements=null;
	private boolean parentOfSingleMultipleOccurringChildElement=false;

	public AlignmentContext(AlignmentContext parent, String namespaceUri, String localName, String qName,
			Attributes attributes, XSTypeDefinition typeDefinition, int indentLevel,
			Set<String> multipleOccurringChildElements, boolean parentOfSingleMultipleOccurringChildElement) {
		super();
		this.parent = parent;
		this.namespaceUri = namespaceUri;
		this.localName = localName;
		this.qName = qName;
		this.attributes = attributes;
		this.typeDefinition = typeDefinition;
		this.indentLevel = indentLevel;
		this.multipleOccurringChildElements = multipleOccurringChildElements;
		this.parentOfSingleMultipleOccurringChildElement = parentOfSingleMultipleOccurringChildElement;
	}

	public AlignmentContext getParent() {
		return parent;
	}

	public String getNamespaceUri() {
		return namespaceUri;
	}

	public String getLocalName() {
		return localName;
	}

	public String getqName() {
		return qName;
	}

	public Attributes getAttributes() {
		return attributes;
	}

	public XSTypeDefinition getTypeDefinition() {
		return typeDefinition;
	}

	public int getIndentLevel() {
		return indentLevel;
	}

	public Set<String> getMultipleOccurringChildElements() {
		return multipleOccurringChildElements;
	}

	public boolean isParentOfSingleMultipleOccurringChildElement() {
		return parentOfSingleMultipleOccurringChildElement;
	}
}
