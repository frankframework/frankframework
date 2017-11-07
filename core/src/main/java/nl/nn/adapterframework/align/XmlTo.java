/*
   Copyright 2017 Nationale-Nederlanden

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
import java.io.StringReader;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.align.content.DocumentContainer;

/**
 * XML Schema guided XML converter;
 * 
 * @author Gerrit van Brakel
 */
public class XmlTo<C extends DocumentContainer> extends XMLFilterImpl {
	protected Logger log = Logger.getLogger(this.getClass());

	private boolean writeAttributes=true;
	private boolean DEBUG=false; 

	private XmlAligner aligner;
	
	private C documentContainer;

	public XmlTo(XmlAligner aligner, C documentContainer) {
		this.aligner=aligner;	
		this.documentContainer=documentContainer;
	}

	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (DEBUG) log.debug("startElement ["+localName+"] xml array container ["+aligner.isParentOfSingleMultipleOccurringChildElement()+"] repeated element ["+aligner.isMultipleOccurringChildInParentElement(localName)+"]");
		documentContainer.startElement(localName,aligner.isParentOfSingleMultipleOccurringChildElement(),aligner.isMultipleOccurringChildInParentElement(localName), aligner.getTypeDefinition());
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
						if (DEBUG) log.debug("startElement ["+localName+"] searching attribute ["+attNS+":"+attName+"]");
						int attIndex=attNS!=null? atts.getIndex(attNS, attName):atts.getIndex(attName);
						if (attIndex>=0) {
							String value=atts.getValue(attIndex);
							if (DEBUG) log.debug("startElement ["+localName+"] attribute ["+attNS+":"+attName+"] value ["+value+"]");
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
		documentContainer.endElement(localName);
		super.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		XSSimpleType simpleType=aligner.getElementType();
		boolean numericType=false;
		boolean booleanType=false;
		if (simpleType!=null) {
			if (DEBUG) log.debug("characters ["+new String(ch,start,length)+"]");
			if (simpleType.getNumeric()) {
				numericType=true;
			}
			if (simpleType.getBuiltInKind()==XSConstants.BOOLEAN_DT) {
				booleanType=true;
			}
		}
		documentContainer.characters(ch, start, length, numericType, booleanType);
		super.characters(ch, start, length);
	}

	public static DocumentContainer translate(String xml, URL schemaURL, DocumentContainer documentContainer) throws SAXException, IOException {

		// create the ValidatorHandler
    	SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(schemaURL); 
		ValidatorHandler validatorHandler = schema.newValidatorHandler();
 	
    	// create the parser, setup the chain
    	XMLReader parser = new SAXParser();
    	XmlAligner aligner = new XmlAligner(validatorHandler);
    	XmlTo xml2object = new XmlTo(aligner, documentContainer);   	
    	parser.setContentHandler(validatorHandler);
    	aligner.setContentHandler(xml2object);
	
    	// start translating
    	InputSource is = new InputSource(new StringReader(xml));
		parser.parse(is);
		return documentContainer;
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
