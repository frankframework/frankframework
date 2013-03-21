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
/* $Log: CalcboxContentHandler.java,v $
/* Revision 1.6  2012-03-14 11:23:57  europe\m168309
/* use getTransformerFactory() from XmlUtils instead of own code
/*
/* Revision 1.5  2012/02/03 11:18:29  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
/* for XSLT 1.0 the class com.sun.org.apache.xalan.internal.processor.TransformerFactoryImpl is used to be backward compatible with WAS5 (only for java vendor IBM and java version >= 1.5)
/*
/* Revision 1.4  2012/02/01 11:35:39  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
/* for XSLT 1.0 the class com.sun.org.apache.xalan.internal.processor.TransformerFactoryImpl is used to be backward compatible with WAS5
/*
/* Revision 1.3  2011/11/30 13:52:03  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
/* adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
/*
/* Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
/* Upgraded from WebSphere v5.1 to WebSphere v6.1
/*
/* Revision 1.1  2008/11/25 10:17:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
/* first version
/* */
package nl.nn.adapterframework.extensions.rekenbox;

import nl.nn.adapterframework.util.XmlUtils;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.StringWriter;
import java.io.FileReader;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.StringReader;

/**
 * @author Jaco de Groot
 * @version $Id$
 */
public class CalcboxContentHandler implements ContentHandler {
  int level = 0;
  org.w3c.dom.Document document;

	private String stringResult = null; 

  CalcboxContentHandler(String string) throws Exception {
		document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
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
