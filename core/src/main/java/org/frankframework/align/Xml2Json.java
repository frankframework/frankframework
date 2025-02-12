/*
   Copyright 2017 Nationale-Nederlanden, 2020, 2024 WeAreFrank!

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
package org.frankframework.align;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import javax.xml.validation.ValidatorHandler;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import lombok.extern.log4j.Log4j2;

import org.frankframework.align.content.JsonDocumentContainer;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlUtils;

/**
 * XML Schema guided XML to JSON converter;
 *
 * @author Gerrit van Brakel
 */
@Log4j2
public class Xml2Json extends XMLFilterImpl {

	private final XmlAligner aligner;
	private final JsonDocumentContainer documentContainer;
	Deque<String> element = new ArrayDeque<>();
	String topElement;
	private boolean writeAttributes=true;

	public Xml2Json(XmlAligner aligner, boolean skipArrayElementContainers, boolean skipRootElement) {
		this.aligner = aligner;
		this.documentContainer = new JsonDocumentContainer(null, skipArrayElementContainers, skipRootElement);
	}

	public static JsonDocumentContainer translate(String xml, URL schemaURL, boolean compactJsonArrays, boolean skipRootElement) throws SAXException, IOException {
		ValidatorHandler validatorHandler = XmlAligner.getValidatorHandler(schemaURL);

		// create the parser, setup the chain
		XmlAligner aligner = new XmlAligner(validatorHandler);
		Xml2Json xml2object = new Xml2Json(aligner, compactJsonArrays, skipRootElement);
		aligner.setContentHandler(xml2object);
		XmlUtils.parseXml(xml, validatorHandler);
		return xml2object.getDocumentContainer();
	}

	public Message toMessage() throws IOException {
		return getDocumentContainer().toMessage();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		boolean xmlArrayContainer = aligner.isParentOfSingleMultipleOccurringChildElement();
		boolean repeatedElement = aligner.isMultipleOccurringChildInParentElement(localName);
		XSTypeDefinition typeDefinition = aligner.getTypeDefinition();
		if (!localName.equals(topElement)) {
			topElement=localName;
		}
		element.push(topElement);
		topElement = null;
		if (log.isTraceEnabled())
			log.trace("startElement [{}] xml array container [{}] repeated element [{}]", localName, aligner.isParentOfSingleMultipleOccurringChildElement(), aligner.isMultipleOccurringChildInParentElement(localName));

		documentContainer.startElement(localName,xmlArrayContainer,repeatedElement, typeDefinition);
		super.startElement(uri, localName, qName, atts);
		if (aligner.isNil(atts)) {
			documentContainer.setNull();
		} else if (writeAttributes) {
			List<XSAttributeUse> attributeUses = aligner.getAttributeUses();
			XSWildcard wildcard = typeDefinition instanceof XSComplexTypeDefinition xsctd ? xsctd.getAttributeWildcard() : null;
			if (attributeUses.isEmpty() && wildcard == null) {
				if (atts.getLength() > 0) {
					log.warn("found [{}] attributes, but no declared AttributeUses", atts.getLength());
				}
			} else if (wildcard != null) {
				// if wildcard (xs:anyAttribute namespace="##any" processContents="lax") is present, then any attribute will be parsed
				for (int i = 0; i < atts.getLength(); i++) {
					String name = atts.getLocalName(i);
					String namespace = atts.getURI(i);
					String value = atts.getValue(i);
					XSSimpleTypeDefinition attTypeDefinition = findAttributeTypeDefinition(attributeUses, namespace, name);
					log.trace("startElement [{}] attribute [{}:{}] value [{}]", localName, namespace, name, value);
					if (StringUtils.isNotEmpty(value)) {
						documentContainer.setAttribute(name, value, attTypeDefinition);
					}
				}
			} else {
				// if no wildcard is found, then only declared attributes will be parsed
				for (XSAttributeUse attributeUse : attributeUses) {
					XSAttributeDeclaration attributeDeclaration=attributeUse.getAttrDeclaration();
					XSSimpleTypeDefinition attTypeDefinition=attributeDeclaration.getTypeDefinition();
					String attName=attributeDeclaration.getName();
					String attNS=attributeDeclaration.getNamespace();
					if (log.isTraceEnabled()) log.trace("startElement [{}] searching attribute [{}:{}]", localName, attNS, attName);
					int attIndex=attNS!=null? atts.getIndex(attNS, attName):atts.getIndex(attName);
					if (attIndex>=0) {
						String value=atts.getValue(attIndex);
						if (log.isTraceEnabled()) log.trace("startElement [{}] attribute [{}:{}] value [{}]", localName, attNS, attName, value);
						if (StringUtils.isNotEmpty(value)) {
							documentContainer.setAttribute(attName, value, attTypeDefinition);
						}
					}
				}
			}
		}
	}

	private XSSimpleTypeDefinition findAttributeTypeDefinition(@Nonnull List<XSAttributeUse> attributeUses, String namespace, String name) {
		for (XSAttributeUse attributeUse : attributeUses) {
			XSAttributeDeclaration attributeDeclaration=attributeUse.getAttrDeclaration();
			String attUseName = attributeDeclaration.getName();
			String attUseNS = attributeDeclaration.getNamespace();
			if ((namespace == null && attUseNS == null || namespace != null && namespace.equals(attUseNS)) && name.equals(attUseName)) {
				return attributeDeclaration.getTypeDefinition();
			}
		}
		return null;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		topElement=element.pop();
		log.trace("endElement [{}]", localName);
		documentContainer.endElement();
		super.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		XSSimpleType simpleType = aligner.getElementType();
		ScalarType scalarType = ScalarType.findType(simpleType);
		if (log.isTraceEnabled() && simpleType != null) {
			log.trace("SimpleType [{}] ScalarType [{}] characters [{}]", simpleType, scalarType, new String(ch, start, length));
		}
		documentContainer.characters(ch, start, length);
		super.characters(ch, start, length);
	}

	@Override
	public String toString() {
		return documentContainer.toString();
	}

	public JsonDocumentContainer getDocumentContainer() {
		return documentContainer;
	}

	public boolean isWriteAttributes() {
		return writeAttributes;
	}

	public void setWriteAttributes(boolean writeAttributes) {
		this.writeAttributes = writeAttributes;
	}
}
