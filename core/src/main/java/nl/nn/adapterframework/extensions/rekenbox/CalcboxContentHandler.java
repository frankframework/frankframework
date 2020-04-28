/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.rekenbox;

import nl.nn.adapterframework.util.XmlUtils;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXSource;
import java.io.StringWriter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.InputSource;
import java.io.StringReader;

/**
 * @author Jaco de Groot
 */
public class CalcboxContentHandler implements ContentHandler {
  int level = 0;
  org.w3c.dom.Document document;

	private String stringResult = null; 

  CalcboxContentHandler(String string) throws Exception {
		document = XmlUtils.getDocumentBuilderFactory().newDocumentBuilder().newDocument();
		document.createElement("TEST");
		
		StringWriter sw = new StringWriter();
		
		XMLReader reader = XMLReaderFactory.createXMLReader("nl.nn.adapterframework.extensions.rekenbox.CalcboxOutputReader");
		
		Source source = new SAXSource(reader, new InputSource(new StringReader(string)));
		
		Result result = new StreamResult(sw);

		// Write the DOM document to the file
		TransformerFactory xfactory = XmlUtils.getTransformerFactory();
		Transformer xformer = xfactory.newTransformer();
		xformer.transform(source, result);

		stringResult = sw.toString();

  }
  
  
  public void characters(char[] ch, int start, int length) {}
  public void endDocument(){}
  public void endElement(String namespaceURI, String localName, String qName) {}
  public void endPrefixMapping(String prefix){}
  public void ignorableWhitespace(char[] ch, int start, int length){}
  public void processingInstruction(String target, String data){}
  public void setDocumentLocator(Locator locator){}
  public void skippedEntity(String name){}
  public void startDocument(){}
  public void startElement(String namespaceURI, String localName, String qName, Attributes atts){}
  public void startPrefixMapping(String prefix, String uri){}

	public String getStringResult() {
		return stringResult;
	}

}
