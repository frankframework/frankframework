/* $Log: CalcboxContentHandler.java,v $
/* Revision 1.3  2011-11-30 13:52:03  europe\m168309
/* adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
/*
/* Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
/* Upgraded from WebSphere v5.1 to WebSphere v6.1
/*
/* Revision 1.1  2008/11/25 10:17:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
/* first version
/* */
package nl.nn.adapterframework.extensions.rekenbox;

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
 * @version Id
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
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
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
