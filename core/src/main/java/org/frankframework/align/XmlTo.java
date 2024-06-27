/*
   Copyright 2017 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
import java.util.Stack;

import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.frankframework.align.content.DocumentContainer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import org.frankframework.util.LogUtil;
import org.frankframework.util.XmlUtils;

/**
 * XML Schema guided XML converter;
 *
 * @author Gerrit van Brakel
 */
public class XmlTo<C extends DocumentContainer> extends XMLFilterImpl {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private boolean writeAttributes=true;

	private final XmlAligner aligner;

	private final C documentContainer;
	Stack<String> element=new Stack<>();
	String topElement;

	public XmlTo(XmlAligner aligner, C documentContainer) {
		this.aligner=aligner;
		this.documentContainer=documentContainer;
	}


	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		boolean xmlArrayContainer=aligner.isParentOfSingleMultipleOccurringChildElement();
		boolean repeatedElement=aligner.isMultipleOccurringChildInParentElement(localName);
		XSTypeDefinition typeDefinition=aligner.getTypeDefinition();
		if (!localName.equals(topElement)) {
			if (topElement!=null) {
				if (log.isTraceEnabled()) log.trace("endElementGroup [{}]", topElement);
				documentContainer.endElementGroup(topElement);
			}
			if (log.isTraceEnabled()) log.trace("startElementGroup [{}]", localName);
			documentContainer.startElementGroup(localName, xmlArrayContainer, repeatedElement, typeDefinition);
			topElement=localName;
		}
		element.push(topElement);
		topElement=null;
		if (log.isTraceEnabled())
			log.trace("startElement [{}] xml array container [{}] repeated element [{}]", localName, aligner.isParentOfSingleMultipleOccurringChildElement(), aligner.isMultipleOccurringChildInParentElement(localName));
		documentContainer.startElement(localName,xmlArrayContainer,repeatedElement, typeDefinition);
		super.startElement(uri, localName, qName, atts);
		if (aligner.isNil(atts)) {
			documentContainer.setNull();
		} else {
			if (writeAttributes) {
				XSObjectList attributeUses=aligner.getAttributeUses();
				XSWildcard wildcard = typeDefinition instanceof XSComplexTypeDefinition xsctd ? xsctd.getAttributeWildcard():null;
				if (attributeUses==null && wildcard==null) {
					if (atts.getLength()>0) {
						log.warn("found [{}] attributes, but no declared AttributeUses", atts.getLength());
					}
				} else {
					if (wildcard!=null) {
						// if wildcard (xs:anyAttribute namespace="##any" processContents="lax") is present, then any attribute will be parsed
						for (int i=0;i<atts.getLength(); i++) {
							String name=atts.getLocalName(i);
							String namespace=atts.getURI(i);
							String value=atts.getValue(i);
							XSSimpleTypeDefinition attTypeDefinition = findAttributeTypeDefinition(attributeUses, namespace, name);
							if (log.isTraceEnabled()) log.trace("startElement [{}] attribute [{}:{}] value [{}]", localName, namespace, name, value);
							if (StringUtils.isNotEmpty(value)) {
								documentContainer.setAttribute(name, value, attTypeDefinition);
							}
						}
					} else {
						// if no wildcard is found, then only declared attributes will be parsed
						for (int i=0;i<attributeUses.getLength(); i++) {
							XSAttributeUse attributeUse=(XSAttributeUse)attributeUses.item(i);
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
		}
	}

	private XSSimpleTypeDefinition findAttributeTypeDefinition(XSObjectList attributeUses, String namespace, String name) {
		if (attributeUses==null) {
			return null;
		}
		for (int i=0;i<attributeUses.getLength(); i++) {
			XSAttributeUse attributeUse=(XSAttributeUse)attributeUses.item(i);
			XSAttributeDeclaration attributeDeclaration=attributeUse.getAttrDeclaration();
			String attUseName=attributeDeclaration.getName();
			String attUseNS=attributeDeclaration.getNamespace();
			if ((namespace==null && attUseNS==null || namespace!=null && namespace.equals(attUseNS)) && name.equals(attUseName)) {
				return attributeDeclaration.getTypeDefinition();
			}
		}
		return null;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (topElement!=null) {
			if (log.isTraceEnabled()) log.trace("endElementGroup [{}]", topElement);
			documentContainer.endElementGroup(topElement);
		}
		topElement=element.pop();
		if (log.isTraceEnabled()) log.trace("endElement [{}]", localName);
		documentContainer.endElement(localName);
		super.endElement(uri, localName, qName);
		if (element.isEmpty()) {
			if (log.isTraceEnabled()) log.trace("endElementGroup [{}]", localName);
			documentContainer.endElementGroup(localName);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		XSSimpleType simpleType=aligner.getElementType();
		ScalarType scalarType=ScalarType.findType(simpleType);
		if (log.isTraceEnabled() && simpleType!=null) {
			log.trace("SimpleType [{}] ScalarType [{}] characters [{}]", simpleType, scalarType, new String(ch, start, length));
		}
		documentContainer.characters(ch, start, length);
		super.characters(ch, start, length);
	}

	public static ValidatorHandler setupHandler(URL schemaURL, DocumentContainer documentContainer) throws SAXException {

		ValidatorHandler validatorHandler = XmlAligner.getValidatorHandler(schemaURL);

		// create the parser, setup the chain
		XmlAligner aligner = new XmlAligner(validatorHandler);
		XmlTo<DocumentContainer> xml2object = new XmlTo<>(aligner, documentContainer);
		aligner.setContentHandler(xml2object);

		return validatorHandler;
	}

	public static void translate(String xml, URL schemaURL, DocumentContainer documentContainer) throws SAXException, IOException {
		ValidatorHandler validatorHandler = setupHandler(schemaURL, documentContainer);
		XmlUtils.parseXml(xml, validatorHandler);
	}

	@Override
	public String toString() {
		return documentContainer.toString();
	}

	public C getDocumentContainer() {
		return documentContainer;
	}

	public boolean isWriteAttributes() {
		return writeAttributes;
	}
	public void setWriteAttributes(boolean writeAttributes) {
		this.writeAttributes = writeAttributes;
	}

}
