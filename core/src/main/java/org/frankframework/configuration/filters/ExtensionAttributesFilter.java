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
package org.frankframework.configuration.filters;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

import org.frankframework.xml.FullXmlFilter;

/**
 * Turns attributes in a module-registered namespace into plain attributes, after validation.
 *
 * <p>A module that customizes a built-in element, for example by shadowing or subclassing a pipe to
 * add a setter, writes its extra attribute in its own namespace on the built-in element:
 * <pre>{@code <XsltPipe xmlns:acme="urn:acme" name="transform" styleSheetName="a.xsl" acme:auditLevel="full"/>}</pre>
 * The digester then sets {@code auditLevel} like any other attribute.
 *
 * <h2>Why a namespace, and not an unqualified attribute</h2>
 * <ul>
 * <li>A module cannot add an attribute to a built-in element's schema type. Those types are generated from
 *     the framework's own classes. XSD offers no clean way for another schema to amend them: a second
 *     declaration of the same element is an error, and {@code xs:redefine} is fragile and deprecated in XSD 1.1.</li>
 * <li>The generated types already accept attributes from other namespaces (an {@code xs:anyAttribute
 *     namespace="##other"} wildcard), so a namespaced attribute needs no schema change at all, while every
 *     built-in attribute on the element stays validated. A typo in {@code styleSheetName} is still reported.
 *     Because the editor-facing FrankConfig.xsd has the same wildcard, IDEs accept the attribute too. The
 *     generic {@code <Pipe className="...">} element, the only other way to avoid the warning today, accepts
 *     any attribute and validates none of them.</li>
 * <li>The prefix makes the ownership visible in the configuration: a reader, an editor or a later framework
 *     upgrade can tell a module's attribute from a framework attribute.</li>
 * <li>Only namespaces that a module registered through its {@link org.frankframework.components.ExtensionSchema}
 *     are kept. Other namespaced attributes, such as editor or tooling annotations, are still stripped as before.</li>
 * </ul>
 *
 * <p>The namespace is a validation-time marker only. This filter runs after the validator and before
 * {@link ElementRoleFilter}, so the digester, the loaded-configuration view and Ladybug all see an ordinary
 * {@code auditLevel="full"} attribute.
 *
 * <p>Limits:
 * <ul>
 * <li>The wildcard uses {@code processContents="skip"}, so the attribute's own declaration in the module
 *     schema is not checked.</li>
 * <li>A subclass named in {@code className} on a built-in element still gets the schema's warning that
 *     {@code className} is fixed. This filter does not change that; it fits a shadowed class directly.</li>
 * <li>Writing a built-in attribute in the module namespace bypasses that attribute's validation and is not
 *     intended.</li>
 * <li>Every generated element type carries the wildcard today, in both the compatibility schema and the
 *     editor-facing FrankConfig.xsd. A type without it would report the attribute as a validation warning.</li>
 * <li>If an element carries both {@code auditLevel} and {@code acme:auditLevel}, the unqualified one wins and
 *     the conflict is reported through the validation {@link ErrorHandler}.</li>
 * </ul>
 */
public class ExtensionAttributesFilter extends FullXmlFilter {

	private final Set<String> extensionNamespaces;
	private final @Nullable ErrorHandler errorHandler;

	public ExtensionAttributesFilter(ContentHandler handler, Set<String> extensionNamespaces, @Nullable ErrorHandler errorHandler) {
		super(handler);
		this.extensionNamespaces = extensionNamespaces;
		this.errorHandler = errorHandler;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		super.startElement(uri, localName, qName, extensionNamespaces.isEmpty() ? atts : flatten(localName, atts));
	}

	private Attributes flatten(String elementName, Attributes atts) throws SAXException {
		Set<String> names = new HashSet<>();
		boolean hasExtensionAttribute = false;
		for (int i = 0; i < atts.getLength(); i++) {
			if (StringUtils.isEmpty(atts.getURI(i))) {
				names.add(atts.getLocalName(i));
			} else if (extensionNamespaces.contains(atts.getURI(i))) {
				hasExtensionAttribute = true;
			}
		}
		if (!hasExtensionAttribute) {
			return atts;
		}

		AttributesImpl result = new AttributesImpl();
		for (int i = 0; i < atts.getLength(); i++) {
			String attributeUri = atts.getURI(i);
			if (StringUtils.isEmpty(attributeUri) || !extensionNamespaces.contains(attributeUri)) {
				result.addAttribute(attributeUri, atts.getLocalName(i), atts.getQName(i), atts.getType(i), atts.getValue(i));
				continue;
			}
			String name = atts.getLocalName(i);
			if (!names.add(name)) {
				report("attribute [" + atts.getQName(i) + "] on element [" + elementName + "] is ignored: the element already has an attribute [" + name + "]");
				continue;
			}
			result.addAttribute("", name, name, atts.getType(i), atts.getValue(i));
		}
		return result;
	}

	private void report(String message) throws SAXException {
		if (errorHandler != null) {
			errorHandler.error(new SAXParseException(message, getDocumentLocator()));
		}
	}
}
