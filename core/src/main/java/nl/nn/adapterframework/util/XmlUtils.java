/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.validation.XmlValidatorContentHandler;
import nl.nn.adapterframework.validation.XmlValidatorErrorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultDocument;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Some utilities for working with XML.
 *
 * @author  Johan Verrips
 */
public class XmlUtils {
	static Logger log = LogUtil.getLogger(XmlUtils.class);

	static final String W3C_XML_SCHEMA =       "http://www.w3.org/2001/XMLSchema";
	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static final String JAXP_SCHEMA_SOURCE =   "http://java.sun.com/xml/jaxp/properties/schemaSource";

	public static final String NAMESPACE_AWARE_BY_DEFAULT_KEY = "xml.namespaceAware.default";
	public static final String AUTO_RELOAD_KEY = "xslt.auto.reload";
	public static final String XSLT_BUFFERSIZE_KEY = "xslt.bufsize";
	public static final int XSLT_BUFFERSIZE_DEFAULT=4096;
	public static final String INCLUDE_FIELD_DEFINITION_BY_DEFAULT_KEY = "query.includeFieldDefinition.default";

	public static final String OPEN_FROM_FILE = "file";
	public static final String OPEN_FROM_URL = "url";
	public static final String OPEN_FROM_RESOURCE = "resource";
	public static final String OPEN_FROM_XML = "xml";

	private static Boolean namespaceAwareByDefault = null;
	private static Boolean includeFieldDefinitionByDefault = null;
	private static Boolean autoReload = null;
	private static Integer buffersize=null;

	public static final char REPLACE_NON_XML_CHAR = 0x00BF; // Inverted question mark.
	public static final String XPATH_GETROOTNODENAME = "name(/node()[position()=last()])";

	public static final String IDENTITY_TRANSFORM =
		"<?xml version=\"1.0\"?><xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:template match=\"@*|*|processing-instruction()|comment()\">"
			+ "<xsl:copy><xsl:apply-templates select=\"*|@*|text()|processing-instruction()|comment()\" />"
			+ "</xsl:copy></xsl:template></xsl:stylesheet>";

	private static final String ADAPTERSITE_XSLT = "/xml/xsl/web/adapterSite.xsl";

	public static final XMLEventFactory EVENT_FACTORY;
	public static final XMLInputFactory INPUT_FACTORY;
	public static final XMLOutputFactory OUTPUT_FACTORY;
	public static final XMLOutputFactory REPAIR_NAMESPACES_OUTPUT_FACTORY;
	public static final String STREAM_FACTORY_ENCODING  = "UTF-8";

	static {
		// Use the Sun Java Streaming XML Parser (SJSXP) as StAX implementation
		// on all Application Servers. Don't leave it up to the newFactory
		// method of javax.xml.stream.XMLOutputFactory which for example on
		// WAS 8.5 with classloader parent first will result in
		// com.ibm.xml.xlxp2.api.stax.XMLOutputFactoryImpl being used while
		// with parent last it will use com.ctc.wstx.sw.RepairingNsStreamWriter
		// from woodstox-core-asl-4.2.0.jar. At the time of testing the
		// woodstox-core-asl-4.2.0.jar and sjsxp-1.0.2.jar were part of the
		// webapp which both provide META-INF/services/javax.xml.stream.*. On
		// Tomcat the sjsxp was used by newFactory while on WAS 8.5 with parent
		// last woodstox was used (giving "Response already committed" error
		// when a WSDL was generated).
		EVENT_FACTORY = new com.sun.xml.stream.events.ZephyrEventFactory();
		INPUT_FACTORY = new com.sun.xml.stream.ZephyrParserFactory();
		OUTPUT_FACTORY = new com.sun.xml.stream.ZephyrWriterFactory();
		REPAIR_NAMESPACES_OUTPUT_FACTORY = new com.sun.xml.stream.ZephyrWriterFactory();
		REPAIR_NAMESPACES_OUTPUT_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
	}

	public XmlUtils() {
		super();
	}
	public static String makeSkipEmptyTagsXslt(boolean omitXmlDeclaration, boolean indent) {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
			+ "<xsl:output method=\"xml\" indent=\""+(indent?"yes":"no")+"\" omit-xml-declaration=\""+(omitXmlDeclaration?"yes":"no")+"\"/>"
			+ "<xsl:strip-space elements=\"*\"/>"
			+ "<xsl:template match=\"node()|@*\">"
			+ "<xsl:copy>"
			+ "<xsl:apply-templates select=\"@*\"/>"
			+ "<xsl:apply-templates/>"
			+ "</xsl:copy>"
			+ "</xsl:template>"
			+ "<xsl:template match=\"*[not(normalize-space(.))]\"/>"
			+ "</xsl:stylesheet>";
	}

	public static String makeRemoveNamespacesXslt(boolean omitXmlDeclaration, boolean indent) {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:output method=\"xml\" indent=\""+(indent?"yes":"no")+"\" omit-xml-declaration=\""+(omitXmlDeclaration?"yes":"no")+"\"/>"
			+ "<xsl:template match=\"*\">"
			+ "<xsl:element name=\"{local-name()}\">"
			+ "<xsl:for-each select=\"@*\">"
			+ "<xsl:attribute name=\"{local-name()}\"><xsl:value-of select=\".\"/></xsl:attribute>"
			+ "</xsl:for-each>"
			+ "<xsl:apply-templates/>"
			+ "</xsl:element>"
			+ "</xsl:template>"
			+ "<xsl:template match=\"comment() | processing-instruction() | text()\">"
			+ "<xsl:copy>"
			+ "<xsl:apply-templates/>"
			+ "</xsl:copy>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static String makeGetIbisContextXslt() {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:output method=\"text\"/>"
			+ "<xsl:template match=\"/\">"
			+ "<xsl:for-each select=\"processing-instruction('ibiscontext')\">"
			+ "<xsl:variable name=\"ic\" select=\"normalize-space(.)\"/>"
			+ "<xsl:text>{</xsl:text>"
			+ "<xsl:value-of select=\"string-length($ic)\"/>"
			+ "<xsl:text>}</xsl:text>"
			+ "<xsl:value-of select=\"$ic\"/>"
			+ "</xsl:for-each>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static String makeGetRootNamespaceXslt() {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
			+ "<xsl:output method=\"text\"/>"
			+ "<xsl:template match=\"*\">"
			+ "<xsl:value-of select=\"namespace-uri()\"/>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static String makeAddRootNamespaceXslt(String namespace, boolean omitXmlDeclaration, boolean indent) {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" xmlns=\""+namespace+"\">"
			+ "<xsl:output method=\"xml\" indent=\""+(indent?"yes":"no")+"\" omit-xml-declaration=\""+(omitXmlDeclaration?"yes":"no")+"\"/>"
			+ "<xsl:template match=\"/*\">"
			+ "<xsl:element name=\"{local-name()}\">"
			+ "<xsl:apply-templates select=\"@* | comment() | node()\"/>"
			+ "</xsl:element>"
			+ "</xsl:template>"
			+ "<xsl:template match=\"*\">"
			+ "<xsl:choose>"
			+ "<xsl:when test=\"namespace-uri() = namespace-uri(/*)\">"
			+ "<xsl:element name=\"{local-name()}\">"
			+ "<xsl:apply-templates select=\"@* | comment() | node()\"/>"
			+ "</xsl:element>"
			+ "</xsl:when>"
			+ "<xsl:otherwise>"
			+ "<xsl:element namespace=\"{namespace-uri()}\" name=\"{local-name()}\">"
			+ "<xsl:apply-templates select=\"@* | comment() | node()\"/>"
			+ "</xsl:element>"
			+ "</xsl:otherwise>"
			+ "</xsl:choose>"
			+ "</xsl:template>"
			+ "<xsl:template match=\"@*\">"
			+ "<xsl:copy-of select=\".\"/>"
			+ "</xsl:template>"
			+ "<xsl:template match=\"comment()\">"
			+ "<xsl:copy-of select=\".\"/>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static String makeChangeRootXslt(String root, boolean omitXmlDeclaration, boolean indent) {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:output method=\"xml\" indent=\""+(indent?"yes":"no")+"\" omit-xml-declaration=\""+(omitXmlDeclaration?"yes":"no")+"\"/>"
			+ "<xsl:template match=\"/*\">"
			+ "<xsl:element name=\""+root+"\" namespace=\"{namespace-uri()}\">"
			+ "<xsl:for-each select=\"@*\">"
			+ "<xsl:attribute name=\"{name()}\"><xsl:value-of select=\".\"/></xsl:attribute>"
			+ "</xsl:for-each>"
			+ "<xsl:copy-of select=\"*\"/>"
			+ "</xsl:element>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static String makeRemoveUnusedNamespacesXslt(boolean omitXmlDeclaration, boolean indent) {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:output method=\"xml\" indent=\""+(indent?"yes":"no")+"\" omit-xml-declaration=\""+(omitXmlDeclaration?"yes":"no")+"\"/>"
			+ "<xsl:template match=\"*\">"
			+ "<xsl:element name=\"{local-name()}\" namespace=\"{namespace-uri()}\">"
			+ "<xsl:apply-templates select=\"@* | node()\"/>"
			+ "</xsl:element>"
			+ "</xsl:template>"
			+ "<xsl:template match=\"@* | comment() | processing-instruction() | text()\">"
			+ "<xsl:copy/>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static String makeRemoveUnusedNamespacesXslt2(boolean omitXmlDeclaration, boolean indent) {
		return "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
				+ "<xsl:output method=\"xml\" indent=\""
				+ (indent ? "yes" : "no")
				+ "\" omit-xml-declaration=\""
				+ (omitXmlDeclaration ? "yes" : "no")
				+ "\"/>"
				+ "<xsl:variable name=\"colon\" select=\"':'\"/>"
				+ "<xsl:template match=\"*\">"
				+ "<xsl:element name=\"{local-name()}\" namespace=\"{namespace-uri()}\">"
				+ "<xsl:apply-templates select=\"@* | node()\"/>"
				+ "</xsl:element>"
				+ "</xsl:template>"
				+ "<xsl:template match=\"@*\">"
				+ "<xsl:choose>"
				+ "<xsl:when test=\"local-name()='type' and namespace-uri()='http://www.w3.org/2001/XMLSchema-instance' and contains(.,$colon)\">"
				+ "<xsl:variable name=\"prefix\" select=\"substring-before(.,$colon)\"/>"
				+ "<xsl:variable name=\"namespace\" select=\"namespace-uri-for-prefix($prefix, parent::*)\"/>"
				+ "<xsl:variable name=\"parentNamespace\" select=\"namespace-uri(parent::*)\"/>"
				+ "<xsl:choose>"
				+ "<xsl:when test=\"$namespace=$parentNamespace\">"
				+ "<xsl:attribute name=\"{name()}\" namespace=\"{namespace-uri()}\"><xsl:value-of select=\"substring-after(.,$colon)\"/></xsl:attribute>"
				+ "</xsl:when>"
				+ "<xsl:otherwise>"
				+ "<xsl:copy/>"
				+ "</xsl:otherwise>"
				+ "</xsl:choose>"
				+ "</xsl:when>"
				+ "<xsl:otherwise>"
				+ "<xsl:copy/>"
				+ "</xsl:otherwise>"
				+ "</xsl:choose>"
				+ "</xsl:template>"
				+ "<xsl:template match=\"comment() | processing-instruction() | text()\">"
				+ "<xsl:copy/>" + "</xsl:template>" + "</xsl:stylesheet>";
	}

	public static String makeCopyOfSelectXslt(String xpath,
			boolean omitXmlDeclaration, boolean indent) {
		return "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
				+ "<xsl:output method=\"xml\" indent=\""
				+ (indent ? "yes" : "no")
				+ "\" omit-xml-declaration=\""
				+ (omitXmlDeclaration ? "yes" : "no")
				+ "\"/>"
				+ "<xsl:strip-space elements=\"*\"/>"
				+ "<xsl:template match=\"/*\">"
				+ "<xsl:copy-of select=\""
				+ xpath + "\"/>" + "</xsl:template>" + "</xsl:stylesheet>";
	}

	public static synchronized boolean isNamespaceAwareByDefault() {
		if (namespaceAwareByDefault==null) {
			boolean aware=AppConstants.getInstance().getBoolean(NAMESPACE_AWARE_BY_DEFAULT_KEY, false);
			namespaceAwareByDefault = new Boolean(aware);
		}
		return namespaceAwareByDefault.booleanValue();
	}

	public static synchronized boolean isIncludeFieldDefinitionByDefault() {
		if (includeFieldDefinitionByDefault==null) {
			boolean definition=AppConstants.getInstance().getBoolean(INCLUDE_FIELD_DEFINITION_BY_DEFAULT_KEY, true);
			includeFieldDefinitionByDefault = new Boolean(definition);
		}
		return includeFieldDefinitionByDefault.booleanValue();
	}

	public static synchronized boolean isAutoReload() {
		if (autoReload==null) {
			boolean reload=AppConstants.getInstance().getBoolean(AUTO_RELOAD_KEY, false);
			autoReload = new Boolean(reload);
		}
		return autoReload.booleanValue();
	}

	public static synchronized int getBufSize() {
		if (buffersize==null) {
			int size=AppConstants.getInstance().getInt(XSLT_BUFFERSIZE_KEY, XSLT_BUFFERSIZE_DEFAULT);
			buffersize = new Integer(size);
		}
		return buffersize.intValue();
	}

	public static void parseXml(ContentHandler handler, String source) throws IOException, SAXException {
		parseXml(handler,new Variant(source).asXmlInputSource());
	}

	public static void parseXml(ContentHandler handler, InputSource source) throws IOException, SAXException {
		XMLReader parser;
		parser = getParser();
		parser.setContentHandler(handler);
		parser.parse(source);
	}

	private static XMLReader getParser() throws SAXException {
		return XMLReaderFactory.createXMLReader();
	}

	public static Document buildDomDocument(File file)
		throws DomBuilderException {
		Reader in;
		Document output;

		try {
			in = new FileReader(file);
		} catch (FileNotFoundException e) {
			throw new DomBuilderException(e);
		}
		output = buildDomDocument(in);
		try {
			in.close();
		} catch (IOException e) {
			log.debug("Exception closing file", e);
		}
		return output;
	}

	public static Document buildDomDocument(Reader in) throws DomBuilderException {
		return buildDomDocument(in,isNamespaceAwareByDefault());
	}

	public static Document buildDomDocument(Reader in, boolean namespaceAware)
		throws DomBuilderException {
			return buildDomDocument(in, namespaceAware, false, false);
		}

	public static Document buildDomDocument(Reader in, boolean namespaceAware,
			boolean xslt2, boolean resolveExternalEntities)
		throws DomBuilderException {
		Document document;
		InputSource src;

		DocumentBuilderFactory factory = getDocumentBuilderFactory(xslt2, namespaceAware);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			if (!resolveExternalEntities) {
				builder.setEntityResolver(new XmlExternalEntityResolver());
			}
			src = new InputSource(in);
			document = builder.parse(src);
		} catch (SAXParseException e) {
			throw new DomBuilderException(e);
		} catch (ParserConfigurationException e) {
			throw new DomBuilderException(e);
		} catch (IOException e) {
			throw new DomBuilderException(e);
		} catch (SAXException e) {
			throw new DomBuilderException(e);
		}
		if (document == null) {
			throw new DomBuilderException("Parsed Document is null");
		}
		return document;
	}
	/**
	 * Convert an XML string to a Document
	 * Creation date: (20-02-2003 8:12:52)
	 * @return org.w3c.dom.Document
	 * @exception nl.nn.adapterframework.util.DomBuilderException The exception description.
	 */
	public static Document buildDomDocument(String s) throws DomBuilderException {
		StringReader sr = new StringReader(s);
		return buildDomDocument(sr);
	}

	public static Document buildDomDocument(String s, boolean namespaceAware) throws DomBuilderException {
		return buildDomDocument(s, namespaceAware, false);
	}

	public static Document buildDomDocument(String s, boolean namespaceAware,
			boolean xslt2) throws DomBuilderException {
		return buildDomDocument(s, namespaceAware, false, false);
	}

	public static Document buildDomDocument(String s, boolean namespaceAware,
			boolean xslt2, boolean resolveExternalEntities)
			throws DomBuilderException {
		if (StringUtils.isEmpty(s)) {
			throw new DomBuilderException("input is null");
		}
		StringReader sr = new StringReader(s);
		return buildDomDocument(sr, namespaceAware, xslt2, resolveExternalEntities);
	}

	/**
	 * Build a Document from a URL
	 * @param url
	 * @return Document
	 * @throws DomBuilderException
	 */
	public static Document buildDomDocument(URL url)
		throws DomBuilderException {
		Reader in;
		Document output;

		try {
			in = new InputStreamReader(url.openStream(),Misc.DEFAULT_INPUT_STREAM_ENCODING);
		} catch (IOException e) {
			throw new DomBuilderException(e);
		}
		output = buildDomDocument(in);
		try {
			in.close();
		} catch (IOException e) {
			log.debug("Exception closing URL-stream", e);
		}
		return output;
	}
	/**
	 * Convert an XML string to a Document, then return the root-element
	 */
	public static org.w3c.dom.Element buildElement(String s, boolean namespaceAware) throws DomBuilderException {
		return buildDomDocument(s,namespaceAware).getDocumentElement();
	}

	/**
	 * Convert an XML string to a Document, then return the root-element as a Node
	 */
	public static Node buildNode(String s, boolean namespaceAware) throws DomBuilderException {
		log.debug("buildNode() ["+s+"],["+namespaceAware+"]");
		return buildElement(s,namespaceAware);
	}

	public static Node buildNode(String s) throws DomBuilderException {
		log.debug("buildNode() ["+s+"]");
		return buildElement(s,isNamespaceAwareByDefault());
	}


	/**
	 * Convert an XML string to a Document, then return the root-element.
	 * (namespace aware)
	 */
	public static Element buildElement(String s) throws DomBuilderException {

		return buildDomDocument(s).getDocumentElement();
	}
	
	public static String skipXmlDeclaration(String xmlString) {
		if (xmlString != null && xmlString.startsWith("<?xml")) {
			int endPos = xmlString.indexOf("?>")+2;
			if (endPos > 0) {
				try {
					while (Character.isWhitespace(xmlString.charAt(endPos))) {
						endPos++;
					}
				} catch (IndexOutOfBoundsException e) {
					log.debug("ignoring IndexOutOfBoundsException, as this only happens for an xml document that contains only the xml declartion, and not any body");
				}
				return xmlString.substring(endPos);
			} else {
				throw new IllegalArgumentException("no valid xml declaration in string ["+xmlString+"]");
			}
		}
		return xmlString;
	}

	public static String skipDocTypeDeclaration(String xmlString) {
		if (xmlString!=null && xmlString.startsWith("<!DOCTYPE")) {
			int endPos = xmlString.indexOf(">")+2;
			if (endPos>0) {
				try {
					while (Character.isWhitespace(xmlString.charAt(endPos))) {
						endPos++;
					}
				} catch (IndexOutOfBoundsException e) {
					log.debug("ignoring IndexOutOfBoundsException, as this only happens for an xml document that contains only the DocType declartion, and not any body");
				}
				return xmlString.substring(endPos);
			} else {
				throw new IllegalArgumentException("no valid xml declaration in string ["+xmlString+"]");
			}
		}
		return xmlString;
	}

	public static String readXml(byte[] source, String defaultEncoding, boolean skipDeclaration) throws UnsupportedEncodingException {
		return readXml(source, 0, source.length, defaultEncoding, skipDeclaration);
	}
	public static String readXml(byte[] source, String defaultEncoding, boolean skipDeclaration, boolean useDeclarationEncoding) throws UnsupportedEncodingException {
		return readXml(source, 0, source.length, defaultEncoding, skipDeclaration, useDeclarationEncoding);
	}

	public static String readXml(byte[] source, int offset, int length, String defaultEncoding, boolean skipDeclaration) throws UnsupportedEncodingException {
		return readXml(source, 0, source.length, defaultEncoding, skipDeclaration, true);
	}

	public static String readXml(byte[] source, int offset, int length, String defaultEncoding, boolean skipDeclaration, boolean useDeclarationEncoding) throws UnsupportedEncodingException {
		String charset;

		charset=defaultEncoding;
		if (StringUtils.isEmpty(charset)) {
			charset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
		}

		String firstPart = new String(source,offset,length<100?length:100,charset);
		if (StringUtils.isEmpty(firstPart)) {
			return null;
		}
		if (firstPart.startsWith("<?xml")) {
			int endPos = firstPart.indexOf("?>")+2;
			if (endPos>0) {
				String declaration=firstPart.substring(6,endPos-2);
				log.debug("parsed declaration ["+declaration+"]");
				final String encodingTarget= "encoding=\"";
				int encodingStart=declaration.indexOf(encodingTarget);
				if (encodingStart>0) {
					encodingStart+=encodingTarget.length();
					log.debug("encoding-declaration ["+declaration.substring(encodingStart)+"]");
					int encodingEnd=declaration.indexOf("\"",encodingStart);
					if (encodingEnd>0) {
						if (useDeclarationEncoding) {
							charset=declaration.substring(encodingStart,encodingEnd);
						}
						log.debug("parsed charset ["+charset+"]");
					} else {
						log.warn("no end in encoding attribute in declaration ["+declaration+"]");
					}
				} else {
					log.warn("no encoding attribute in declaration ["+declaration+"]");
				}
				if (skipDeclaration) {
					try {
						while (Character.isWhitespace(firstPart.charAt(endPos))) {
							endPos++;
						}
					} catch (IndexOutOfBoundsException e) {
						log.debug("ignoring IndexOutOfBoundsException, as this only happens for an xml document that contains only the xml declartion, and not any body");
					}
					return new String(source,offset+endPos,length-endPos,charset);
				}
			} else {
				throw new IllegalArgumentException("no valid xml declaration in string ["+firstPart+"]");
			}
		}
		return new String(source,offset,length,charset);
	}


	public static String createXPathEvaluatorSource(String XPathExpression)	throws TransformerConfigurationException {
		return createXPathEvaluatorSource(XPathExpression,"text");
	}

	/*
	 * version of createXPathEvaluator that allows to set outputMethod, and uses copy-of instead of value-of
	 */
	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, String outputMethod, boolean includeXmlDeclaration) throws TransformerConfigurationException {
		return createXPathEvaluatorSource(namespaceDefs, XPathExpression, outputMethod, includeXmlDeclaration, null);
	}

	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, String outputMethod, boolean includeXmlDeclaration, List params) throws TransformerConfigurationException {
		return createXPathEvaluatorSource(namespaceDefs, XPathExpression, outputMethod, includeXmlDeclaration, params, true);
	}

	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, String outputMethod, boolean includeXmlDeclaration, List params, boolean stripSpace) throws TransformerConfigurationException {
		return createXPathEvaluatorSource(namespaceDefs, XPathExpression, outputMethod, includeXmlDeclaration, params, stripSpace, false);
	}

	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, String outputMethod, boolean includeXmlDeclaration, List params, boolean stripSpace, boolean xslt2) throws TransformerConfigurationException {
		return createXPathEvaluatorSource(namespaceDefs, XPathExpression, outputMethod, includeXmlDeclaration, params, stripSpace, xslt2, null);
	}

	/*
	 * version of createXPathEvaluator that allows to set outputMethod, and uses copy-of instead of value-of, and enables use of parameters.
	 */
	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, String outputMethod, boolean includeXmlDeclaration, List params, boolean stripSpace, boolean xslt2, String separator) throws TransformerConfigurationException {
		if (StringUtils.isEmpty(XPathExpression))
			throw new TransformerConfigurationException("XPathExpression must be filled");

		String namespaceClause = "";
		if (namespaceDefs != null) {
			StringTokenizer st1 = new StringTokenizer(namespaceDefs,", \t\r\n\f");
			while (st1.hasMoreTokens()) {
				String namespaceDef = st1.nextToken();
				int separatorPos = namespaceDef.indexOf('=');
				if (separatorPos < 1) {
					throw new TransformerConfigurationException("cannot parse namespace definition from string [" + namespaceDef + "]");
				} else {
					namespaceClause += " xmlns:" + namespaceDef.substring(0, separatorPos) + "=\"" + namespaceDef.substring(separatorPos + 1) + "\"";
				}
			}
		}


        final String copyMethod;
		if ("xml".equals(outputMethod)) {
			copyMethod = "copy-of";
		} else {
			copyMethod = "value-of";
		}

		String paramsString = "";
		if (params != null) {
			for (Iterator it = params.iterator(); it.hasNext();) {
				paramsString = paramsString + "<xsl:param name=\"" + it.next() + "\"/>";
			}
		}
		String separatorString = "";
		if (separator != null) {
			separatorString = " separator=\"" + separator + "\"";
		}
		String xsl =
			// "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\""+(xslt2?"2.0":"1.0")+"\" xmlns:xalan=\"http://xml.apache.org/xslt\">" +
			"<xsl:output method=\""+outputMethod+"\" omit-xml-declaration=\""+ (includeXmlDeclaration ? "no": "yes") +"\"/>" +
			(stripSpace?"<xsl:strip-space elements=\"*\"/>":"") +
			paramsString +
			"<xsl:template match=\"/\">" +
			"<xsl:"+copyMethod+" "+namespaceClause+" select=\"" + XPathExpression + "\"" + separatorString + "/>" +
			"</xsl:template>" +
			"</xsl:stylesheet>";

		return xsl;
	}

	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, String outputMethod) throws TransformerConfigurationException {
		return createXPathEvaluatorSource(namespaceDefs, XPathExpression, outputMethod, false);
	}

	public static String createXPathEvaluatorSource(String XPathExpression, String outputMethod) throws TransformerConfigurationException {
		return createXPathEvaluatorSource(null, XPathExpression, outputMethod);
	}


	public static Transformer createXPathEvaluator(String XPathExpression)
		throws TransformerConfigurationException {
		return createTransformer(createXPathEvaluatorSource(XPathExpression));
	}

	public static Transformer createXPathEvaluator(String namespaceDefs, String XPathExpression, String outputMethod)
		throws TransformerConfigurationException {
		return createTransformer(createXPathEvaluatorSource(namespaceDefs, XPathExpression, outputMethod));
	}

	public static Transformer createXPathEvaluator(String XPathExpression, String outputMethod) throws TransformerConfigurationException {
		return createXPathEvaluator(null, XPathExpression, outputMethod);
	}

	/**
	 * Converts a string containing xml-markup to a Source-object, that can be used as the input of a XSLT-transformer.
	 * The source may be used multiple times.
	 */
	public static Source stringToSource(String xmlString, boolean namespaceAware) throws DomBuilderException {
		Document doc = XmlUtils.buildDomDocument(xmlString, namespaceAware);
		return new DOMSource(doc);
	}

	public static Source stringToSource(String xmlString) throws DomBuilderException {
		return stringToSource(xmlString,isNamespaceAwareByDefault());
	}

	public static Source stringToSourceForSingleUse(String xmlString)
			throws DomBuilderException {
		return stringToSourceForSingleUse(xmlString,
				isNamespaceAwareByDefault());
	}

	public static Source stringToSourceForSingleUse(String xmlString,
			boolean namespaceAware) throws DomBuilderException {
		return stringToSourceForSingleUse(xmlString, namespaceAware, false);
	}

	public static Source stringToSourceForSingleUse(String xmlString,
			boolean namespaceAware, boolean resolveExternalEntities)
			throws DomBuilderException {
		if (namespaceAware) {
			return stringToSAXSource(xmlString, namespaceAware, false);
		} else {
			return stringToSource(xmlString, false);
		}
	}

	public static SAXSource stringToSAXSource(String xmlString,
			boolean namespaceAware, boolean resolveExternalEntities)
			throws DomBuilderException {
		Variant in = new Variant(xmlString);
		InputSource is = in.asXmlInputSource();
		SAXParserFactory factory = getSAXParserFactory(namespaceAware);
		try {
			XMLReader xmlReader = factory.newSAXParser().getXMLReader();
			if (!resolveExternalEntities) {
				xmlReader.setEntityResolver(new XmlExternalEntityResolver());
			}
			return new SAXSource(xmlReader, is);
		} catch (Exception e) {
			// TODO Use DomBuilderException as the stringToSource and calling
			// methods use them a lot. Rename DomBuilderException to
			// SourceBuilderException?
			throw new DomBuilderException(e);
		}
	}

	public static synchronized Transformer createTransformer(String xsltString)
		throws TransformerConfigurationException {

		return createTransformer(xsltString, false);
	}
	public static synchronized Transformer createTransformer(String xsltString, boolean xslt2)
		throws TransformerConfigurationException {

		StringReader sr = new StringReader(xsltString);

		StreamSource stylesource = new StreamSource(sr);
		return createTransformer(stylesource, xslt2);
	}
	public static synchronized Transformer createTransformer(URL url)
		throws TransformerConfigurationException, IOException {

		return createTransformer(url, false);
	}
	public static synchronized Transformer createTransformer(URL url, boolean xslt2)
		throws TransformerConfigurationException, IOException {

		StreamSource stylesource = new StreamSource(url.openStream(),Misc.DEFAULT_INPUT_STREAM_ENCODING);
		stylesource.setSystemId(url.toString());
		return createTransformer(stylesource, xslt2);
	}
	public static synchronized Transformer createTransformer(Source source)
		throws TransformerConfigurationException {
			return createTransformer(source, false);
	}
	public static synchronized Transformer createTransformer(Source source, boolean xslt2)
		throws TransformerConfigurationException {

		TransformerFactory tFactory = getTransformerFactory(xslt2);
		Transformer result = tFactory.newTransformer(source);

		return result;
		}

	public static synchronized TransformerFactory getTransformerFactory() {
		return getTransformerFactory(false);
	}

	//TransformerFactory.setNamespaceAware doesn't exists!
	
	public static synchronized TransformerFactory getTransformerFactory(boolean xslt2) {
		if (xslt2) {
			return new net.sf.saxon.TransformerFactoryImpl();
		} else {
			// Use a Xalan version with different package names to prevent the
			// WebSphere Xalan version being used and prevent differences
			// in XML transformations between WebSphere 5 and WebSphere 6.
			if (IbisContext.getApplicationServerType().startsWith("WAS")) {
				return new nl.nn.org.apache.xalan.processor.TransformerFactoryImpl();
			} else {
				return new org.apache.xalan.processor.TransformerFactoryImpl();
			}
		}
	}

	public static synchronized DocumentBuilderFactory getDocumentBuilderFactory() {
		return getDocumentBuilderFactory(false);
	}

	public static synchronized DocumentBuilderFactory getDocumentBuilderFactory(boolean xslt2) {
		if (xslt2) {
			return getDocumentBuilderFactory(xslt2, false);
		} else {
			return getDocumentBuilderFactory(xslt2, isNamespaceAwareByDefault());
		}
	}

	public static synchronized DocumentBuilderFactory getDocumentBuilderFactory(boolean xslt2, boolean namespaceAware) {
		if (xslt2) {
			if (!namespaceAware) {
				log.info("Saxon parser is always namespace aware, so setting namespaceAware=false is ignored");
			}
			return new net.sf.saxon.dom.DocumentBuilderFactoryImpl();
		} else {
			DocumentBuilderFactory factory;
			factory = new org.apache.xerces.jaxp.DocumentBuilderFactoryImpl();
			factory.setNamespaceAware(namespaceAware);
			return factory;
		}
	}

	public static synchronized SAXParserFactory getSAXParserFactory() {
		return getSAXParserFactory(isNamespaceAwareByDefault());
	}

	public static synchronized SAXParserFactory getSAXParserFactory(boolean namespaceAware) {
		SAXParserFactory factory = new org.apache.xerces.jaxp.SAXParserFactoryImpl();
		factory.setNamespaceAware(namespaceAware);
		return factory;
	}

	/**
	 * Translates special characters to xml equivalents
	 * like <b>&gt;</b> and <b>&amp;</b>. Please note that non valid xml chars
	 * are not changed, hence you might want to use
	 * replaceNonValidXmlCharacters() or stripNonValidXmlCharacters() too.
	 */
	public static String encodeChars(String string) {
		if (string==null) {
			return null;
		}
		int length = string.length();
		char[] characters = new char[length];
		string.getChars(0, length, characters, 0);
		return encodeChars(characters,0,length);
	}
	/**
	 * Translates special characters to xml equivalents
	 * like <b>&gt;</b> and <b>&amp;</b>. Please note that non valid xml chars
	 * are not changed, hence you might want to use
	 * replaceNonValidXmlCharacters() or stripNonValidXmlCharacters() too.
	 */
	public static String encodeChars(char[] chars, int offset, int length) {

		if (length<=0) {
			return "";
		}
		StringBuilder encoded = new StringBuilder(length);
		String escape;
		for (int i = 0; i < length; i++) {
			char c=chars[offset+i];
			escape = escapeChar(c);
			if (escape == null)
				encoded.append(c);
			else
				encoded.append(escape);
		}
		return encoded.toString();
	}

	/**
	 * Translates the five reserved XML characters (&lt; &gt; &amp; &quot; &apos;) to their normal selves
	 */
	public static String decodeChars(String string) {
		StringBuilder decoded = new StringBuilder();

		boolean inEscape = false;
		int escapeStartPos = 0;

		for (int i = 0; i < string.length(); i++) {
			char cur=string.charAt(i);
			if (inEscape) {
				if ( cur == ';') {
					inEscape = false;
					String escapedString = string.substring(escapeStartPos, i + 1);
					char unEscape = unEscapeString(escapedString);
					if (unEscape == 0x0) {
						decoded.append(escapedString);
					}
					else {
						decoded.append(unEscape);
					}
				}
			} else {
				if (cur == '&') {
					inEscape = true;
					escapeStartPos = i;
				} else {
					decoded.append(cur);
				}
			}
		}
		if (inEscape) {
			decoded.append(string.substring(escapeStartPos));
		}
		return decoded.toString();
	}

	/**
	   * Conversion of special xml signs. Please note that non valid xml chars
	   * are not changed, hence you might want to use
	   * replaceNonValidXmlCharacters() or stripNonValidXmlCharacters() too.
	   **/
	private static String escapeChar(char c) {
		switch (c) {
			case ('<') :
				return "&lt;";
			case ('>') :
				return "&gt;";
			case ('&') :
				return "&amp;";
			case ('\"') :
				return "&quot;";
			case ('\'') :
				// return "&apos;"; // apos does not work in Internet Explorer
				return "&#39;";
		}
		return null;
	}

	private static char unEscapeString(String str) {
		if (str.equalsIgnoreCase("&lt;"))
			return '<';
		else if (str.equalsIgnoreCase("&gt;"))
			return '>';
		else if (str.equalsIgnoreCase("&amp;"))
			return '&';
		else if (str.equalsIgnoreCase("&quot;"))
			return '\"';
		else if (str.equalsIgnoreCase("&apos;") || str.equalsIgnoreCase("&#39;"))
			return '\'';
		else
			return 0x0;
	}

	/**
	 * encodes a url
	 */

	static public String encodeURL(String url) {
		String mark = "-_.!~*'()\"";
		StringBuilder encodedUrl = new StringBuilder();
		int len = url.length();
		for (int i = 0; i < len; i++) {
			char c = url.charAt(i);
			if ((c >= '0' && c <= '9')
				|| (c >= 'a' && c <= 'z')
				|| (c >= 'A' && c <= 'Z'))
				encodedUrl.append(c);
			else {
				int imark = mark.indexOf(c);
				if (imark >= 0) {
					encodedUrl.append(c);
				} else {
					encodedUrl.append('%');
					encodedUrl.append(toHexChar((c & 0xF0) >> 4));
					encodedUrl.append(toHexChar(c & 0x0F));
				}
			}
		}
		return encodedUrl.toString();
	}

	static private char toHexChar(int digitValue) {
		if (digitValue < 10)
			return (char) ('0' + digitValue);
		else
			return (char) ('A' + (digitValue - 10));
	}

	/**
	 * Method getChildTagAsBoolean.
	 * Return the boolean-value of the first element with tag
	 * <code>tag</code> in the DOM subtree <code>el</code>.
	 *
	 * <p>
	 * To determine true or false, the value of the tag is compared case-
	 * insensitive with the values <pre>true</pre>, <pre>yes</pre>, or
	 * <pre>on</pre>. If it matches, <code>true</code> is returned. If not,
	 * <code>false</code> is returned.
	 *
	 * <p>
	 * If the tag can not be found, <code>false</code> is returned.
	 *
	 * @param el            DOM subtree
	 * @param tag           Name of tag to find
	 *
	 * @return boolean      The value found.
	 */
	static public boolean getChildTagAsBoolean(Element el, String tag) {
		return getChildTagAsBoolean(el, tag, false);
	}
	/**
	 * Method getChildTagAsBoolean.
	 * Return the boolean-value of the first element with tag
	 * <code>tag</code> in the DOM subtree <code>el</code>.
	 *
	 * <p>
	 * To determine true or false, the value of the tag is compared case-
	 * insensitive with the values <pre>true</pre>, <pre>yes</pre>, or
	 * <pre>on</pre>. If it matches, <code>true</code> is returned. If not,
	 * <code>false</code> is returned.
	 *
	 * <p>
	 * If the tag can not be found, the default-value is returned.
	 *
	 * @param el            DOM subtree
	 * @param tag           Name of tag to find
	 * @param defaultValue  Default-value in case tag can not
	 *                       be found.
	 *
	 * @return boolean      The value found.
	 */
	static public boolean getChildTagAsBoolean(
		Element el,
		String tag,
		boolean defaultValue) {
		String str;
		boolean bool;

		str = getChildTagAsString(el, tag, null);
		if (str == null) {
			return defaultValue;
		}

		bool = false;
		if (str.equalsIgnoreCase("true")
			|| str.equalsIgnoreCase("yes")
			|| str.equalsIgnoreCase("on")) {
			bool = true;
		}
		return bool;
	}
	/**
	 * Method getChildTagAsLong.
	 * Return the long integer-value of the first element with tag
	 * <code>tag</code> in the DOM subtree <code>el</code>.
	 *
	 * @param el            DOM subtree
	 * @param tag           Name of tag to find
	 *
	 * @return long          The value found. Returns 0 if no
	 *                       tag can be found, or if the tag
	 *                       doesn't have an integer-value.
	 */
	static public long getChildTagAsLong(Element el, String tag) {
		return getChildTagAsLong(el, tag, 0);
	}
	/**
	 * Method getChildTagAsLong.
	 * Return the long integer-value of the first element with tag
	 * <code>tag</code> in the DOM subtree <code>el</code>.
	 *
	 * @param el            DOM subtree
	 * @param tag           Name of tag to find
	 * @param defaultValue  Default-value in case tag can not
	 *                       be found, or is not numeric.
	 *
	 * @return long          The value found.
	 */
	static public long getChildTagAsLong(
		Element el,
		String tag,
		long defaultValue) {
		String str;
		long num;

		str = getChildTagAsString(el, tag, null);
		num = 0;
		if (str == null) {
			return defaultValue;
		}
		try {
			num = Long.parseLong(str);
		} catch (NumberFormatException e) {
			num = defaultValue;
			log.error("Tag [" + tag + "] has no integer value",e);
		}
		return num;
	}
	/**
	 * Method getChildTagAsString.
	 * Return the value of the first element with tag
	 * <code>tag</code> in the DOM subtree <code>el</code>.
	 *
	 * @param el            DOM subtree
	 * @param tag           Name of tag to find
	 *
	 * @return String       The value found, or null if no matching
	 *                       tag is found.
	 */
	static public String getChildTagAsString(Element el, String tag) {
		return getChildTagAsString(el, tag, null);
	}
	/**
	 * Method getChildTagAsString.
	 * Return the value of the first element with tag
	 * <code>tag</code> in the DOM subtree <code>el</code>.
	 *
	 * @param el            DOM subtree
	 * @param tag           Name of tag to find
	 * @param defaultValue  Default-value in case tag can not
	 *                       be found.
	 *
	 * @return String       The value found.
	 */
	static public String getChildTagAsString(
		Element el,
		String tag,
		String defaultValue) {
		Element tmpEl;
		String str = "";

		tmpEl = getFirstChildTag(el, tag);
		if (tmpEl != null) {
			str = getStringValue(tmpEl, true);
		}
		return (str.length() == 0) ? (defaultValue) : (str);
	}
	/**
	 * Method getChildTags. Get all direct children of given element which
	 * match the given tag.
	 * This method only looks at the direct children of the given node, and
	 * doesn't descent deeper into the tree. If a '*' is passed as tag,
	 * all elements are returned.
	 *
	 * @param el            Element where to get children from
	 * @param tag           Tag to match. Use '*' to match all tags.
	 * @return Collection  Collection containing all elements found. If
	 *                      size() returns 0, then no matching elements
	 *                      were found. All items in the collection can
	 *                      be safely cast to type
	 *                      <code>org.w3c.dom.Element</code>.
	 */
	public static Collection<Node> getChildTags(Element el, String tag) {
		Collection<Node> c;
		NodeList nl;
		int len;
		boolean allChildren;

		c = new LinkedList<Node>();
		nl = el.getChildNodes();
		len = nl.getLength();

		if ("*".equals(tag)) {
			allChildren = true;
		} else {
			allChildren = false;
		}

		for (int i = 0; i < len; i++) {
			Node n = nl.item(i);
			if (n instanceof Element) {
				Element e = (Element) n;
				if (allChildren || e.getTagName().equals(tag)) {
					c.add(n);
				}
			}
		}

		return c;
	}
	/**
	 * Method getFirstChildTag. Return the first child-node which is an element
	 * with tagName equal to given tag.
	 * This method only looks at the direct children of the given node, and
	 * doesn't descent deeper into the tree.
	 *
	 * @param el       Element where to get children from
	 * @param tag      Tag to match
	 * @return Element The element found, or <code>null</code> if no match
	 *                  found.
	 */
	static public Element getFirstChildTag(Element el, String tag) {
		NodeList nl;
		int len;

		nl = el.getChildNodes();
		len = nl.getLength();
		for (int i = 0; i < len; ++i) {
			Node n = nl.item(i);
			if (n instanceof Element) {
				Element elem = (Element) n;
				if (elem.getTagName().equals(tag)) {
					return elem;
				}
			}
		}
		return null;
	}
	static public String getStringValue(Element el) {
		return getStringValue(el, true);
	}
	static public String getStringValue(Element el, boolean trimWhitespace) {
		StringBuilder sb = new StringBuilder(1024);
		String str;

		NodeList nl = el.getChildNodes();
		for (int i = 0; i < nl.getLength(); ++i) {
			Node n = nl.item(i);
			if (n instanceof Text) {
				sb.append(n.getNodeValue());
			}
		}
		if (trimWhitespace) {
			str = sb.toString().trim();
		} else {
			str = sb.toString();
		}
		return str;

	}

	/**
	 * Replaces non-unicode-characters by '0x00BF' (inverted question mark).
	 */
	public static String encodeCdataString(String string) {
		return replaceNonValidXmlCharacters(string, REPLACE_NON_XML_CHAR, false);
	}

	/**
	 * Replaces non-unicode-characters by '0x00BF' (inverted question mark)
	 * appended with #, the character number and ;.
	 */
	public static String replaceNonValidXmlCharacters(String string) {
		return replaceNonValidXmlCharacters(string, REPLACE_NON_XML_CHAR, true);
	}

	public static String replaceNonValidXmlCharacters(String string, char to, boolean appendCharNum) {
		if (string==null) {
			return null;
		} else {
			int length = string.length();
			StringBuilder encoded = new StringBuilder(length);
			int counter = 0;
			for (int i = 0; i < length; i++) {
				char c=string.charAt(i);
				if (isPrintableUnicodeChar(c)) {
					encoded.append(c);
				} else {
					if (appendCharNum) {
						encoded.append(to + "#" + (int)c + ";"); 
					} else {
						encoded.append(to);
					}
					counter++;
				}
			}
			if (counter>0) {
				if (log.isDebugEnabled()) log.debug("replaced ["+counter+"] non valid xml characters to ["+to+"] in string of length ["+length+"]");
			}
			return encoded.toString();
		}
	}

	public static String stripNonValidXmlCharacters(String string) {
		int length = string.length();
		StringBuilder encoded = new StringBuilder(length);
		int counter = 0;
		for (int i = 0; i < length; i++) {
			char c=string.charAt(i);
			if (isPrintableUnicodeChar(c)) {
				encoded.append(c);
			} else {
				counter++;
			}
		}
		if (counter>0) {
			if (log.isDebugEnabled()) log.debug("stripped ["+counter+"] non valid xml characters in string of length ["+length+"]");
		}
		return encoded.toString();
	}

	public static boolean isPrintableUnicodeChar(char c) {
		return (c == 0x0009)
			|| (c == 0x000A)
			|| (c == 0x000D)
			|| (c >= 0x0020 && c <= 0xD7FF)
			|| (c >= 0xE000 && c <= 0xFFFD)
			|| (c >= 0x0010000 && c <= 0x0010FFFF);
	}

	/**
	 * sets all the parameters of the transformer using a Map with parameter values.
	 */
	public static void setTransformerParameters(Transformer t, Map parameters) {
		t.clearParameters();
		if (parameters == null) {
			return;
		}
		for (Iterator it=parameters.keySet().iterator(); it.hasNext();) {
			String name=(String)it.next();
			Object value = parameters.get(name);

			if (value != null) {
				t.setParameter(name, value);
				log.debug("setting parameter [" + name+ "] on transformer");
			}
			else {
				log.info("omitting setting of parameter ["+name+"] on transformer, as it has a null-value");
			}
		}
	}

	public static String transformXml(Transformer t, Document d) throws TransformerException, IOException {
		return transformXml(t, new DOMSource(d));
	}

	public static String transformXml(Transformer t, String s) throws TransformerException, IOException, DomBuilderException {
		return transformXml(t, s, isNamespaceAwareByDefault());
	}

	public static String transformXml(Transformer t, String s, boolean namespaceAware) throws TransformerException, IOException, DomBuilderException {
		return transformXml(t, stringToSourceForSingleUse(s, namespaceAware));
	}

	public static void transformXml(Transformer t, String s, Result result) throws TransformerException, IOException, DomBuilderException {
		synchronized (t) {
			t.transform(stringToSourceForSingleUse(s), result);
		}
	}


	public static String transformXml(Transformer t, Source s)
		throws TransformerException, IOException {

		StringWriter out = new StringWriter(getBufSize());
		transformXml(t,s,out);
		out.close();

		return (out.getBuffer().toString());

	}

	public static void transformXml(Transformer t, Source s, Writer out)
		throws TransformerException, IOException {

		Result result = new StreamResult(out);
		synchronized (t) {
			t.transform(s, result);
		}
	}

	static public boolean isWellFormed(String input) {
		return isWellFormed(input, null);
	}

	static public boolean isWellFormed(String input, String root) {
		Set<List<String>> rootValidations = null;
		if (StringUtils.isNotEmpty(root)) {
			List<String> path = new ArrayList<String>();
			path.add(root);
			rootValidations = new HashSet<List<String>>();
			rootValidations.add(path);
		}
		XmlValidatorContentHandler xmlHandler = new XmlValidatorContentHandler(
				null, rootValidations, null, true);
		XmlValidatorErrorHandler xmlValidatorErrorHandler =
				new XmlValidatorErrorHandler(xmlHandler, "Is not well formed");
		xmlHandler.setXmlValidatorErrorHandler(xmlValidatorErrorHandler);
		try {
			SAXSource saxSource = stringToSAXSource(input, true, false);
			XMLReader xmlReader = saxSource.getXMLReader();
			xmlReader.setContentHandler(xmlHandler);
			// Prevent message in System.err: [Fatal Error] :-1:-1: Premature end of file.
			xmlReader.setErrorHandler(xmlValidatorErrorHandler);
			xmlReader.parse(saxSource.getInputSource());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Performs an Identity-transform, with resolving entities with the content files in the classpath
	 * @param input
	 * @return String (the complete and xml)
	 * @throws DomBuilderException
	 */
	static public String identityTransform(ClassLoader classLoader, String input)
		throws DomBuilderException {
		String result = "";
		try {
			DocumentBuilderFactory factory = getDocumentBuilderFactory();
			Document document;
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(new ClassPathEntityResolver(classLoader));
			StringReader sr = new StringReader(input);
			InputSource src = new InputSource(sr);
			document = builder.parse(src);
			Transformer t = XmlUtils.createTransformer(IDENTITY_TRANSFORM);
			Source s = new DOMSource(document);
			result = XmlUtils.transformXml(t, s);
		} catch (Exception tce) {
			throw new DomBuilderException(tce);
		}

		return result;
	}

	public static String getVersionInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append(AppConstants.getInstance().getProperty("application.name") + " "
				+ AppConstants.getInstance().getProperty("application.version")).append(SystemUtils.LINE_SEPARATOR);
		sb.append("XML tool version info:").append(SystemUtils.LINE_SEPARATOR);

		SAXParserFactory spFactory = getSAXParserFactory();
		sb.append("SAXParserFactory-class =").append(spFactory.getClass().getName()).append(SystemUtils.LINE_SEPARATOR);
		DocumentBuilderFactory domFactory1 = getDocumentBuilderFactory(false);
		sb.append("DocumentBuilderFactory1-class =").append(domFactory1.getClass().getName()).append(SystemUtils.LINE_SEPARATOR);
		DocumentBuilderFactory domFactory2 = getDocumentBuilderFactory(true);
		sb.append("DocumentBuilderFactory2-class =").append(domFactory2.getClass().getName()).append(SystemUtils.LINE_SEPARATOR);

		TransformerFactory tFactory1 = getTransformerFactory(false);
		sb.append("TransformerFactory1-class =").append(tFactory1.getClass().getName()).append(SystemUtils.LINE_SEPARATOR);
		TransformerFactory tFactory2 = getTransformerFactory(true);
		sb.append("TransformerFactory2-class =").append(tFactory2.getClass().getName()).append(SystemUtils.LINE_SEPARATOR);

		sb.append("Apache-XML tool version info:").append(SystemUtils.LINE_SEPARATOR);

		try {
			sb.append("Xerces-Version=").append(org.apache.xerces.impl.Version.getVersion()).append(SystemUtils.LINE_SEPARATOR);
		}  catch (Throwable t) {
			sb.append("Xerces-Version not found (").append(t.getClass().getName()).append(": ").append(t.getMessage()).append(")").append(SystemUtils.LINE_SEPARATOR);
		}

		try {
			String xalanVersion;
			if (IbisContext.getApplicationServerType().startsWith("WAS")) {
				xalanVersion = nl.nn.org.apache.xalan.Version.getVersion();
			} else {
				xalanVersion = org.apache.xalan.Version.getVersion();
			}
 			sb.append("Xalan-Version=" + xalanVersion + SystemUtils.LINE_SEPARATOR);
		}  catch (Throwable t) {
			sb.append("Xalan-Version not found (").append(t.getClass().getName()).append(": ").append(t.getMessage()).append(")").append(SystemUtils.LINE_SEPARATOR);
		}

		try {
//			sb.append("XmlCommons-Version="+org.apache.xmlcommons.Version.getVersion()+SystemUtils.LINE_SEPARATOR);
		}  catch (Throwable t) {
			sb.append("XmlCommons-Version not found (").append(t.getClass().getName()).append(": ").append(t.getMessage()).append(")").append(SystemUtils.LINE_SEPARATOR);
		}

		return sb.toString();
	}

	public static String removeNamespaces(String input) {
		String removeNamespaces_xslt = makeRemoveNamespacesXslt(true,false);
		try {
			Transformer t = createTransformer(removeNamespaces_xslt);
			String query = transformXml(t, input);
			return query;
		} catch (Exception e) {
			return null;
		}
	}

	public static String getRootNamespace(String input) {
		String getRootNamespace_xslt = makeGetRootNamespaceXslt();
		try {
			Transformer t = createTransformer(getRootNamespace_xslt, true);
			String query = transformXml(t, input);
			return query;
		} catch (Exception e) {
			return null;
		}
	}

	public static String addRootNamespace(String input, String namespace) {
		String addRootNamespace_xslt = makeAddRootNamespaceXslt(namespace,true,false);
		try {
			Transformer t = createTransformer(addRootNamespace_xslt);
			String query = transformXml(t, input);
			return query;
		} catch (Exception e) {
			return null;
		}
	}

	public static String removeUnusedNamespaces(String input) {
		String removeUnusedNamespaces_xslt = makeRemoveUnusedNamespacesXslt(true,false);
		try {
			Transformer t = createTransformer(removeUnusedNamespaces_xslt);
			String query = transformXml(t, input);
			return query;
		} catch (Exception e) {
			return null;
		}
	}

	public static String copyOfSelect(String input, String xpath) {
		String copyOfSelect_xslt = makeCopyOfSelectXslt(xpath, true, false);
		try {
			Transformer t = createTransformer(copyOfSelect_xslt, true);
			String query = transformXml(t, input);
			return query;
		} catch (Exception e) {
			return null;
		}
	}

	public static Map<String, String> getIbisContext(String input) {
		if (input.startsWith("<") && !input.startsWith("<?") && !input.startsWith("<!")) {
			return null;
		}
		if (isWellFormed(input)) {
			String getIbisContext_xslt = XmlUtils.makeGetIbisContextXslt();
			try {
				Transformer t = XmlUtils.createTransformer(getIbisContext_xslt);
				String str = XmlUtils.transformXml(t, input);
				Map<String, String> ibisContexts = new LinkedHashMap<String, String>();
				int indexBraceOpen = str.indexOf("{");
				int indexBraceClose = 0;
				int indexStartNextSearch = 0;
				while (indexBraceOpen >= 0) {
					indexBraceClose = str.indexOf("}",indexBraceOpen+1);
					if (indexBraceClose > indexBraceOpen) {
						String ibisContextLength = str.substring(indexBraceOpen+1, indexBraceClose);
						int icLength = Integer.parseInt(ibisContextLength);
						if (icLength > 0) {
							indexStartNextSearch = indexBraceClose + 1 + icLength;
							String ibisContext = str.substring(indexBraceClose+1, indexStartNextSearch);
							int indexEqualSign = ibisContext.indexOf("=");
							String key;
							String value;
							if (indexEqualSign < 0) {
								key = ibisContext;
								value = "";
							} else {
								key = ibisContext.substring(0,indexEqualSign);
								value = ibisContext.substring(indexEqualSign+1);
							}
							ibisContexts.put(key, value);
						} else {
							indexStartNextSearch = indexBraceClose + 1;
						}
					} else {
						indexStartNextSearch = indexBraceOpen + 1;
					}
					indexBraceOpen = str.indexOf("{",indexStartNextSearch);
				}
				return ibisContexts;
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
		}
	}

	public static String canonicalize(String input) throws DocumentException, IOException {
		if (StringUtils.isEmpty(input)) {
			return null;
		}
		org.dom4j.Document doc = DocumentHelper.parseText(input);
		StringWriter sw = new StringWriter();
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setExpandEmptyElements(true);
		XMLWriter xw = new XMLWriter(sw, format);
		xw.write(doc);
		return sw.toString();
	}

	public static String nodeToString(Node node) throws TransformerException {
		Transformer t = getTransformerFactory().newTransformer();
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter sw = new StringWriter();
		t.transform(new DOMSource(node), new StreamResult(sw));
		return sw.toString();
	}

	public static String cdataToText(String input) {
		try {
			DocumentBuilderFactory factory = getDocumentBuilderFactory();
			factory.setCoalescing(true);
			StringReader sr = new StringReader(input);
			InputSource src = new InputSource(sr);
			Document doc = factory.newDocumentBuilder().parse(src);
			return nodeToString(doc);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Like {@link javanet.staxutils.XMLStreamUtils#mergeAttributes} but it can
	 * also merge namespaces
	 * 
	 * @param tag
	 * @param attrs
	 * @param nsps
	 */
	public static StartElement mergeAttributes(StartElement tag,
			Iterator<? extends Attribute> attrs,
			Iterator<? extends Namespace> nsps, XMLEventFactory factory) {
		// create Attribute map
		Map<QName, Attribute> attributes = new HashMap<QName, Attribute>();

		// iterate through start tag's attributes
		for (Iterator i = tag.getAttributes(); i.hasNext();) {
			Attribute attr = (Attribute) i.next();
			attributes.put(attr.getName(), attr);
		}
		if (attrs != null) {
			// iterate through new attributes
			while (attrs.hasNext()) {
				Attribute attr = attrs.next();
				attributes.put(attr.getName(), attr);
			}
		}

		Map<QName, Namespace> namespaces = new HashMap<QName, Namespace>();
		for (Iterator i = tag.getNamespaces(); i.hasNext();) {
			Namespace ns = (Namespace) i.next();
			namespaces.put(ns.getName(), ns);
		}
		if (nsps != null) {
			while (nsps.hasNext()) {
				Namespace ns = nsps.next();
				namespaces.put(ns.getName(), ns);
			}
		}

		factory.setLocation(tag.getLocation());

		QName tagName = tag.getName();
		return factory.createStartElement(tagName.getPrefix(), tagName
				.getNamespaceURI(), tagName.getLocalPart(), attributes.values()
				.iterator(), namespaces.values().iterator(), tag
				.getNamespaceContext());
	}

	public static boolean attributesEqual(Attribute attribute1,
			Attribute attribute2) {
		if (!attribute1.getName().equals(attribute2.getName())) {
			return false;
		} else if (!attribute1.getValue().equals(attribute2.getValue())) {
			return false;
		}
		return true;
	}

	public static String getAdapterSite(Object document)
			throws DomBuilderException, IOException, TransformerException {
		String input;
		if (document instanceof DefaultDocument) {
			DefaultDocument defaultDocument = (DefaultDocument) document;
			input = defaultDocument.asXML();
		} else {
			input = document.toString();
		}
		return getAdapterSite(input, null);
	}

	public static String getAdapterSite(String input, Map parameters)
			throws IOException, DomBuilderException, TransformerException {
		URL xsltSource = ClassUtils.getResourceURL(XmlUtils.class,
				ADAPTERSITE_XSLT);
		Transformer transformer = XmlUtils.createTransformer(xsltSource);
		if (parameters != null) {
			XmlUtils.setTransformerParameters(transformer, parameters);
		}
		return XmlUtils.transformXml(transformer, input);
	}

	public static Collection<String> evaluateXPathNodeSet(String input,
			String xpathExpr) throws DomBuilderException,
			XPathExpressionException {
		String msg = XmlUtils.removeNamespaces(input);

		Collection<String> c = new LinkedList<String>();
		Document doc = buildDomDocument(msg, true, true);
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression xPathExpression = xPath.compile(xpathExpr);
		Object result = xPathExpression.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getNodeType() == Node.ATTRIBUTE_NODE) {
				c.add(nodes.item(i).getNodeValue());
			} else {
				//c.add(nodes.item(i).getTextContent());
				c.add(nodes.item(i).getFirstChild().getNodeValue());
			}
		}
		if (c != null && c.size() > 0) {
			return c;
		}
		return null;
	}

	public static String evaluateXPathNodeSetFirstElement(String input,
			String xpathExpr) throws DomBuilderException,
			XPathExpressionException {
		Collection<String> c = evaluateXPathNodeSet(input, xpathExpr);
		if (c != null && c.size() > 0) {
			return c.iterator().next();
		}
		return null;
	}

	public static Double evaluateXPathNumber(String input,
			String xpathExpr) throws DomBuilderException,
			XPathExpressionException {
		String msg = XmlUtils.removeNamespaces(input);

		Document doc = buildDomDocument(msg, true, true);
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression xPathExpression = xPath.compile(xpathExpr);
		Object result = xPathExpression.evaluate(doc, XPathConstants.NUMBER);
		return (Double) result;
	}

	public static Map<String, String> evaluateXPathNodeSet(String input,
			String xpathExpr, String keyElement, String valueElement)
			throws DomBuilderException, XPathExpressionException {
		String msg = XmlUtils.removeNamespaces(input);

		Map<String, String> m = new HashMap<String, String>();
		Document doc = buildDomDocument(msg, true, true);
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression xPathExpression = xPath.compile(xpathExpr);
		Object result = xPathExpression.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String key = getChildTagAsString(element, keyElement);
				String value = getChildTagAsString(element, valueElement);
				m.put(key, value);
			}
		}
		if (m != null && m.size() > 0) {
			return m;
		}
		return null;
	}

	public static Collection<String> evaluateXPathNodeSet(String input,
			String xpathExpr, String xpathExpr2) throws DomBuilderException,
			XPathExpressionException {
		String msg = XmlUtils.removeNamespaces(input);

		Collection<String> c = new LinkedList<String>();
		Document doc = buildDomDocument(msg, true, true);
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression xPathExpression = xPath.compile(xpathExpr);
		Object result = xPathExpression.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				XPathExpression xPathExpression2 = xPath.compile(xpathExpr2);
				Object result2 = xPathExpression2.evaluate(element,
						XPathConstants.STRING);
				c.add((String) result2);
			}
		}
		if (c != null && c.size() > 0) {
			return c;
		}
		return null;
	}

	public static String toXhtml(String htmlString) {
		String xhtmlString = null;
		if (StringUtils.isNotEmpty(htmlString)) {
			xhtmlString = XmlUtils.skipDocTypeDeclaration(htmlString.trim());
			if (xhtmlString.startsWith("<html>")
					|| xhtmlString.startsWith("<html ")) {
				CleanerProperties props = new CleanerProperties();
				HtmlCleaner cleaner = new HtmlCleaner(props);
				TagNode tagNode = cleaner.clean(xhtmlString);
				xhtmlString = new SimpleXmlSerializer(props)
						.getXmlAsString(tagNode);
			}
		}
		return xhtmlString;
	}
}