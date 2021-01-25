/*
   Copyright 2017 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;
import java.net.URL;
import java.util.Stack;

import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.align.content.DocumentContainer;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * XML Schema guided XML converter;
 * 
 * @author Gerrit van Brakel
 */
public class XmlTo<C extends DocumentContainer> extends XMLFilterImpl {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private boolean writeAttributes=true;

	private XmlAligner aligner;
	
	private C documentContainer;
	Stack<String> element=new Stack<String>();
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
				if (log.isTraceEnabled()) log.trace("endElementGroup ["+topElement+"]");
				documentContainer.endElementGroup(topElement);	
			}
			if (log.isTraceEnabled()) log.trace("startElementGroup ["+localName+"]");
			documentContainer.startElementGroup(localName, xmlArrayContainer, repeatedElement, typeDefinition);	
			topElement=localName;			
		}
		element.push(topElement);
		topElement=null;
		if (log.isTraceEnabled()) log.trace("startElement ["+localName+"] xml array container ["+aligner.isParentOfSingleMultipleOccurringChildElement()+"] repeated element ["+aligner.isMultipleOccurringChildInParentElement(localName)+"]");
		documentContainer.startElement(localName,xmlArrayContainer,repeatedElement, typeDefinition);
		super.startElement(uri, localName, qName, atts);
		if (aligner.isNil(atts)) {
			documentContainer.setNull();
		} else {
			if (writeAttributes) {
				XSObjectList attributeUses=aligner.getAttributeUses();
				if (attributeUses==null) {
					if (atts.getLength()>0) {
						log.warn("found ["+atts.getLength()+"] attributes, but no declared AttributeUses");
					}
				} else {
					for (int i=0;i<attributeUses.getLength(); i++) {
						XSAttributeUse attributeUse=(XSAttributeUse)attributeUses.item(i);
						XSAttributeDeclaration attributeDeclaration=attributeUse.getAttrDeclaration();
						XSSimpleTypeDefinition attTypeDefinition=attributeDeclaration.getTypeDefinition();
						String attName=attributeDeclaration.getName();
						String attNS=attributeDeclaration.getNamespace();
						if (log.isTraceEnabled()) log.trace("startElement ["+localName+"] searching attribute ["+attNS+":"+attName+"]");
						int attIndex=attNS!=null? atts.getIndex(attNS, attName):atts.getIndex(attName);
						if (attIndex>=0) {
							String value=atts.getValue(attIndex);
							if (log.isTraceEnabled()) log.trace("startElement ["+localName+"] attribute ["+attNS+":"+attName+"] value ["+value+"]");
							if (StringUtils.isNotEmpty(value)) {
								documentContainer.setAttribute(attName, value, attTypeDefinition);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (topElement!=null) {
			if (log.isTraceEnabled()) log.trace("endElementGroup ["+topElement+"]");
			documentContainer.endElementGroup(topElement);	
		}
		topElement=element.pop();
		if (log.isTraceEnabled()) log.trace("endElement ["+localName+"]");
		documentContainer.endElement(localName);
		super.endElement(uri, localName, qName);
		if (element.isEmpty()) {
			if (log.isTraceEnabled()) log.trace("endElementGroup ["+localName+"]");
			documentContainer.endElementGroup(localName);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		XSSimpleType simpleType=aligner.getElementType();
		ScalarType scalarType=ScalarType.findType(simpleType);
		if (log.isTraceEnabled() && simpleType!=null) {
			log.trace("SimpleType ["+simpleType+"] ScalarType ["+scalarType+"] characters ["+new String(ch,start,length)+"]");
		}
		documentContainer.characters(ch, start, length);
		super.characters(ch, start, length);
	}

	public static ValidatorHandler setupHandler(URL schemaURL, DocumentContainer documentContainer) throws SAXException, IOException {

		ValidatorHandler validatorHandler = XmlAligner.getValidatorHandler(schemaURL);

		// create the parser, setup the chain
		XmlAligner aligner = new XmlAligner(validatorHandler);
		XmlTo<DocumentContainer> xml2object = new XmlTo<DocumentContainer>(aligner, documentContainer);
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
