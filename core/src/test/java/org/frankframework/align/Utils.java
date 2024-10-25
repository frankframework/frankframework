package org.frankframework.align;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import jakarta.json.Json;
import jakarta.json.JsonStructure;

import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import org.frankframework.util.LogUtil;

public class Utils {
	private static final Logger LOG = LogUtil.getLogger(Utils.class);

	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

	public static boolean validate(URL schemaURL, String xml) {
		return validate(schemaURL, string2Source(xml));
	}

	public static boolean validate(URL schemaURL, Document doc) {
		return validate(schemaURL, new DOMSource(doc));
	}
//	public static boolean validate(String namespace, String xsd, InputSource inputSource) throws XmlNormalizerException {
//		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//
//		dbf.setNamespaceAware(true);
//		dbf.setValidating(true);
//        dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
//        dbf.setAttribute(JAXP_SCHEMA_SOURCE, xsd);
//        try {
//	        DocumentBuilder db = dbf.newDocumentBuilder();
//	        db.setErrorHandler(getValidationErrorHandler(""));
//	        Document doc = db.parse(inputSource);
//	        return true;
//        } catch (Exception e) {
//        	e.printStackTrace();
////			System.out.println("caught exception while validating: "+e);
//        	return false;
//		}
//
//	}

//	public static Schema getSchemaFromResource(String xsd) throws SAXException {
//		System.out.println("xsd: "+xsd);
//		URL schemaURL = ClassLoaderUtils.getResourceURL(Utils.class,xsd);
//		return getSchemaFromResource(schemaURL);
//	}

	public static Schema getSchemaFromResource(URL schemaURL) throws SAXException {
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(schemaURL);
		return schema;
	}

	public static boolean validate(URL schemaURL, Source input)  {
		try {
			Schema schema = getSchemaFromResource(schemaURL);
			Validator validator = schema.newValidator();
			validator.setErrorHandler(getValidationErrorHandler("sax "));
			validator.validate(input);
			return true;
		} catch (Exception e) {
			LOG.warn("("+e.getClass().getName()+"): "+e.getMessage(), e);
			return false;
		}
	}

	public static ErrorHandler getValidationErrorHandler(final String prefix) {
		return new ErrorHandler() {

			@Override
			public void error(SAXParseException e) throws SAXException {
				LOG.error(prefix+"validation error: "+e);
				throw e;
			}

			@Override
			public void fatalError(SAXParseException e) throws SAXException {
				LOG.error(prefix+"validation fatalError: "+e);
			}

			@Override
			public void warning(SAXParseException e) throws SAXException {
				LOG.warn(prefix+"validation warning: "+e);
			}

		};
	}


//	public static Document constructValididatedXml(String xml, final String xsd) throws ParserConfigurationException, SAXException, IOException {
//		URL schemaUrl =  ClassLoaderUtils.getResourceURL(Utils.class, xsd);
//
//		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//		dbf.setNamespaceAware(true);
//	    try {
//	        dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
//	    }
//	    catch (IllegalArgumentException x) {
//	        System.err.println("Error: JAXP DocumentBuilderFactory attribute " + "not recognized: " + JAXP_SCHEMA_LANGUAGE);
//	        System.err.println("Check to see if parser conforms to JAXP spec.");
//	        System.exit(1);
//	    }
////		dbf.setValidating(true);
////		dbf.setAttribute(JAXP_SCHEMA_SOURCE, schemaUrl.getFile());
//
//		ErrorHandler errh = new ErrorHandler() {
//
//			@Override
//			public void error(SAXParseException arg0) throws SAXException {
//				throw arg0;
//			}
//
//			@Override
//			public void fatalError(SAXParseException arg0) throws SAXException {
//				throw arg0;
//			}
//
//			@Override
//			public void warning(SAXParseException arg0) throws SAXException {
//				System.out.println(arg0.getMessage());
//			}
//
//		};
//
//		DocumentBuilder builder = dbf.newDocumentBuilder();
//		builder.setErrorHandler(errh);
//
//		InputSource is = new InputSource(new StringReader(xml));
//
//		Document document = builder.parse(is);
//
//		return document;
//	}

	public static Source string2Source(String xml) {
		LOG.debug("xml:"+xml);
		return new StreamSource(new StringReader(xml));
	}

	public static InputSource string2InputSource(String xml) {
		return new InputSource(new StringReader(xml));
	}

	public static Document string2Dom(String xml) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(string2InputSource(xml));
			return doc;
		} catch (Exception e) {
			LOG.error("caught exception while parsing", e);
			return null;
		}
	}

	public static String dom2String1(Document dom) {
		return source2String(new DOMSource(dom));
	}

	public static String xmlReader2String1(XMLReader reader, InputSource inputSource) {
		return source2String(new SAXSource(reader, inputSource));
	}

	public static String source2String(Source source) {
		try {
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(source, result);
			writer.flush();
			return writer.toString();
		} catch (TransformerException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static String dom2String2(Document document) {
		DOMImplementationRegistry registry;
		try {
			registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS domImplLS = (DOMImplementationLS) registry.getDOMImplementation("LS");

			LSSerializer ser = domImplLS.createLSSerializer(); // Create a serializer for the DOM
			LSOutput out = domImplLS.createLSOutput();
			StringWriter stringOut = new StringWriter(); // Writer will be a String
			out.setCharacterStream(stringOut);
			ser.write(document, out); // Serialize the DOM

			LOG.debug("STRXML = " + stringOut.toString()); // Spit out the DOM as a String

			return stringOut.toString();
		} catch (Exception e) {
			LOG.error("Cannot create registry", e);
		}
		return null;
	}

	public static JsonStructure string2Json(String json) {
		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return jsonStructure;
	}
//	public static JSONObject xml2Json(String xml) throws Exception {
//		return XML.toJSONObject(xml);
//	}
//
//	public static String json2Xml(JSONObject json) throws Exception {
//		return  XML.toString(json);
//	}

//	public static String xml2JsonString(String xml) throws Exception {
//		//valididateXml(xml, schemaUrl);
//		JSONObject jsonObject = XML.toJSONObject(xml);
//		String result = jsonObject.toString();
//		return result;
//	}

	public static String getTestXml(String resourcename) throws IOException {
		BufferedReader buf = new BufferedReader(new InputStreamReader(Utils.class.getResourceAsStream(resourcename)));
		StringBuilder string = new StringBuilder();
		String line = buf.readLine();
		while(line != null) {
			string.append(line);
			line = buf.readLine();
		}
		return string.toString();
	}

	public static void clean(Document dom) {
		clean(dom.getDocumentElement());
	}

	public static void clean(Node node) {
		Node child = node.getFirstChild();
		while (child != null) {
			Node next= child.getNextSibling();
			switch (child.getNodeType()) {
			case Node.ELEMENT_NODE:
				clean(child);
				break;
			case Node.TEXT_NODE:
				if ("".equals(child.getNodeValue().trim())) {
					node.removeChild(child);
				}
				break;
			case Node.COMMENT_NODE:
				node.removeChild(child);
			}
			child=next;
		}
	}
}
