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

import org.apache.log4j.Logger;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

public class Xml2Json extends XMLFilterImpl {
	protected Logger log = Logger.getLogger(this.getClass());

	private boolean skipArrayElementContainers;
	private boolean DEBUG=false; 

	private XmlAligner aligner;
	private Stack<JsonContentContainer> elementStack=new Stack<JsonContentContainer>(); 
	private JsonContentContainer contentContainer=new JsonContentContainer(null, false, false, false);

	public Xml2Json(XmlAligner aligner, boolean skipArrayElementContainers) {
		this.aligner=aligner;	
		this.skipArrayElementContainers=skipArrayElementContainers;
	}

	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (DEBUG && log.isDebugEnabled()) log.debug("startElement ["+localName+"] xml array element ["+aligner.isSingleMultipleOccurringChildElement()+"] repeated element ["+aligner.isMultipleOccuringChildInParentElement(localName)+"]");
		elementStack.push(contentContainer);
		contentContainer=new JsonContentContainer(localName, aligner.isSingleMultipleOccurringChildElement(),aligner.isMultipleOccuringChildInParentElement(localName), skipArrayElementContainers);
		super.startElement(uri, localName, qName, atts);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		JsonContentContainer result=contentContainer;
		contentContainer=elementStack.pop();
		if (DEBUG && log.isDebugEnabled()) log.debug("endElement name ["+localName+"] result ["+result+"] parent ["+contentContainer+"] ");
		contentContainer.addContent(result);
		super.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		contentContainer.setContent(new String(ch,start,length));
		//log.debug("characters() content is now ["+contentContainer+"]");
		super.characters(ch, start, length);
	}

//	@Override
//	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
//		log.debug("ignorable whitespace");
//	}


	public static String translate(String xml, URL schemaURL, boolean compactJsonArrays) throws SAXException, IOException {

		// create the ValidatorHandler
    	SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(schemaURL); 
		ValidatorHandler validatorHandler = schema.newValidatorHandler();
 	
    	// create the parser, setup the chain
    	XMLReader parser = new SAXParser();
    	XmlAligner aligner = new XmlAligner(validatorHandler);
    	Xml2Json xml2json = new Xml2Json(aligner, compactJsonArrays);   	
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
}
