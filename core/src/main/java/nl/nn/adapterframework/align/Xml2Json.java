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
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
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

/**
 * XML Schema guided XML to JSON converter;
 * 
 * @author Gerrit van Brakel
 */
public class Xml2Json extends XMLFilterImpl {
	protected Logger log = Logger.getLogger(this.getClass());

	private boolean skipArrayElementContainers;
	private boolean writeAttributes=true;
	private String attributePrefix="@";
	private boolean DEBUG=false; 

	private XmlAligner aligner;
	private Stack<JsonContentContainer> elementStack=new Stack<JsonContentContainer>(); 
	private JsonContentContainer contentContainer=new JsonContentContainer(null, false, false, false);

	public Xml2Json(XmlAligner aligner, boolean skipArrayElementContainers, boolean skipRootElement) {
		this.aligner=aligner;	
		this.skipArrayElementContainers=skipArrayElementContainers;
		contentContainer.setSkipRootElement(skipRootElement);
	}

	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (DEBUG) log.debug("startElement ["+localName+"] xml array container ["+aligner.isParentOfSingleMultipleOccurringChildElement()+"] repeated element ["+aligner.isMultipleOccurringChildInParentElement(localName)+"]");
		elementStack.push(contentContainer);
		contentContainer=new JsonContentContainer(localName, aligner.isParentOfSingleMultipleOccurringChildElement(),aligner.isMultipleOccurringChildInParentElement(localName), skipArrayElementContainers);
		super.startElement(uri, localName, qName, atts);
		if (aligner.isNil(atts)) {
			contentContainer.setContent(null);
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
								JsonContentContainer attributeContainer = new JsonContentContainer(attributePrefix+attName, false, false, false);
								if (attTypeDefinition.getNumeric()) {
									attributeContainer.setQuoted(false);
								}
								attributeContainer.setContent(value);
								contentContainer.addContent(attributeContainer);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		JsonContentContainer result=contentContainer;
		contentContainer=elementStack.pop();
		if (DEBUG) log.debug("endElement name ["+localName+"] result ["+result+"] parent ["+contentContainer+"] ");
		contentContainer.addContent(result);
		super.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		XSSimpleType simpleType=aligner.getElementType();
		if (simpleType!=null) {
			if (DEBUG) log.debug("characters ["+new String(ch,start,length)+"]");
			if (simpleType.getNumeric()) {
				contentContainer.setQuoted(false);
			}
			if (simpleType.getBuiltInKind()==XSConstants.BOOLEAN_DT) {
				contentContainer.setQuoted(false);
			}
		}
		contentContainer.setContent(new String(ch,start,length));
		//log.debug("characters() content is now ["+contentContainer+"]");
		super.characters(ch, start, length);
	}

	public static String translate(String xml, URL schemaURL, boolean compactJsonArrays, boolean skipRootElement) throws SAXException, IOException {

		// create the ValidatorHandler
    	SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(schemaURL); 
		ValidatorHandler validatorHandler = schema.newValidatorHandler();
 	
    	// create the parser, setup the chain
    	XMLReader parser = new SAXParser();
    	XmlAligner aligner = new XmlAligner(validatorHandler);
    	Xml2Json xml2json = new Xml2Json(aligner, compactJsonArrays, skipRootElement);   	
    	parser.setContentHandler(validatorHandler);
    	aligner.setContentHandler(xml2json);
	
    	// start translating
    	InputSource is = new InputSource(new StringReader(xml));
		parser.parse(is);
    	String json=xml2json.toString();
		
    	return json;
	}

	@Override
	public String toString() {
		return contentContainer.toString();
	}

	public String toString(boolean indent) {
		return contentContainer.toString(indent);
	}


	public boolean isWriteAttributes() {
		return writeAttributes;
	}
	public void setWriteAttributes(boolean writeAttributes) {
		this.writeAttributes = writeAttributes;
	}

	public String getAttributePrefix() {
		return attributePrefix;
	}
	public void setAttributePrefix(String attributePrefix) {
		this.attributePrefix = attributePrefix;
	}
}
