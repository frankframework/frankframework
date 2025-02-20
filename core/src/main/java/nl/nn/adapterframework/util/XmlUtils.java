/*
   Copyright 2013, 2016-2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.ErrorListener;
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
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.stax.WstxInputFactory;

import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xalan.processor.TransformerFactoryImpl;
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
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.validation.RootValidations;
import nl.nn.adapterframework.validation.XmlValidatorContentHandler;
import nl.nn.adapterframework.validation.XmlValidatorErrorHandler;
import nl.nn.adapterframework.xml.BodyOnlyFilter;
import nl.nn.adapterframework.xml.CanonicalizeFilter;
import nl.nn.adapterframework.xml.ClassLoaderEntityResolver;
import nl.nn.adapterframework.xml.NonResolvingExternalEntityResolver;
import nl.nn.adapterframework.xml.PrettyPrintFilter;
import nl.nn.adapterframework.xml.SaxException;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * Some utilities for working with XML.
 *
 * @author  Johan Verrips
 */
public class XmlUtils {
	public static final int HTML_MAX_PREAMBLE_SIZE = 512;
	static Logger log = LogManager.getLogger(XmlUtils.class);

	public static final int DEFAULT_XSLT_VERSION = AppConstants.getInstance().getInt("xslt.version.default", 2);

	public static final String NAMESPACE_AWARE_BY_DEFAULT_KEY = "xml.namespaceAware.default";
	public static final String XSLT_STREAMING_BY_DEFAULT_KEY = "xslt.streaming.default";
	public static final String AUTO_RELOAD_KEY = "xslt.auto.reload";
	public static final String XSLT_BUFFERSIZE_KEY = "xslt.bufsize";
	public static final int XSLT_BUFFERSIZE_DEFAULT=4096;
	public static final String INCLUDE_FIELD_DEFINITION_BY_DEFAULT_KEY = "query.includeFieldDefinition.default";

	private static Boolean namespaceAwareByDefault = null;
	private static Boolean xsltStreamingByDefault = null;
	private static Boolean includeFieldDefinitionByDefault = null;
	private static Boolean autoReload = null;
	private static Integer buffersize=null;

	private static ConcurrentHashMap<String,TransformerPool> utilityTPs = new ConcurrentHashMap<String,TransformerPool>();
	public static final String XPATH_GETROOTNODENAME = "name(/node()[position()=last()])";

	private static final String ADAPTERSITE_XSLT = "/xml/xsl/web/adapterSite.xsl";

	public static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newFactory();
	public static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newFactory();
	public static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newFactory();
	public static final XMLOutputFactory REPAIR_NAMESPACES_OUTPUT_FACTORY = XMLOutputFactory.newFactory();

	static {
		REPAIR_NAMESPACES_OUTPUT_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
	}


	private static TransformerPool getUtilityTransformerPool(Supplier<String> xsltSupplier, String key, boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		//log.debug("utility transformer pool key ["+key+"] xslt ["+xslt+"]");
		return getUtilityTransformerPool(xsltSupplier, key, omitXmlDeclaration, indent, 0);
	}

	private static TransformerPool getUtilityTransformerPool(Supplier<String> xsltSupplier, String key, boolean omitXmlDeclaration, boolean indent, int xsltVersion) throws ConfigurationException {
		String fullKey=key+"-"+omitXmlDeclaration+"-"+indent;
		TransformerPool result = utilityTPs.get(fullKey);
		if (result==null) {
			try {
				TransformerPool newtp=TransformerPool.getUtilityInstance(xsltSupplier.get(), xsltVersion);
				result=utilityTPs.put(fullKey, newtp);
				if (result==null) {
					result=newtp;
				}
			} catch (TransformerConfigurationException te) {
				throw new ConfigurationException("Could not create TransformerPool for ["+key+"]", te);
			}
		}
		return result;
	}

	protected static String makeDetectXsltVersionXslt() {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
			+ "<xsl:output method=\"text\"/>"
			+ "<xsl:template match=\"/\">"
			+ "<xsl:value-of select=\"xsl:stylesheet/@version\"/>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static TransformerPool getDetectXsltVersionTransformerPool() throws TransformerException {
		try {
			return getUtilityTransformerPool(XmlUtils::makeDetectXsltVersionXslt,"DetectXsltVersion",true,false,2);
		} catch (ConfigurationException e) {
			throw new TransformerException(e);
		}
	}


	protected static String makeGetXsltConfigXslt() {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
			+ "<xsl:output method=\"text\"/>"
			+ 	"<xsl:template match=\"/\">"
			+ 		"<xsl:for-each select=\"/xsl:stylesheet/@*\">"
			+ 			"<xsl:value-of select=\"concat(name(),'=',.,';')\"/>"
			+ 		"</xsl:for-each>"
			+ 		"<xsl:for-each select=\"/xsl:transform/@*\">"
			+ 			"<xsl:value-of select=\"concat(name(),'=',.,';')\"/>"
			+ 		"</xsl:for-each>"
			+ 		"<xsl:for-each select=\"/xsl:stylesheet/xsl:output/@*\">"
			+ 			"<xsl:value-of select=\"concat('output-',name(),'=',.,';')\"/>"
			+ 		"</xsl:for-each>"
			+ 		"disable-output-escaping=<xsl:choose>"
			+ 				"<xsl:when test=\"//*[@disable-output-escaping='yes']\">yes</xsl:when>"
			+ 				"<xsl:otherwise>no</xsl:otherwise>"
			+ 			"</xsl:choose>;"
			+ 	"</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static TransformerPool getGetXsltConfigTransformerPool() throws TransformerException {
		try {
			return getUtilityTransformerPool(XmlUtils::makeGetXsltConfigXslt,"detectXsltOutputType",true,false,2);
		} catch (ConfigurationException e) {
			throw new TransformerException(e);
		}
	}

	public static Map<String,String> getXsltConfig(Source source) throws TransformerException, IOException {
		TransformerPool tp = getGetXsltConfigTransformerPool();
		String metadataString = tp.transform(source);
		Map<String,String> result = new LinkedHashMap<>();
		for (final String s : StringUtil.split(metadataString, ";")) {
			List<String> kv = StringUtil.split(s, "=");
			String key = kv.get(0);
			String value = kv.get(1);
			result.put(key, value);
		}
		return result;
	}

	public static TransformerPool getGetRootNodeNameTransformerPool() throws ConfigurationException {
		return getUtilityTransformerPool(()->createXPathEvaluatorSource(XPATH_GETROOTNODENAME, OutputType.TEXT),"GetRootNodeName",true, false);
	}

	protected static String makeRemoveNamespacesXsltTemplates() {
		return
		"<xsl:template match=\"*\">"
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
		+ "</xsl:template>";
	}

	protected static String makeRemoveNamespacesXslt(boolean omitXmlDeclaration, boolean indent) {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
			+ "<xsl:output method=\"xml\" indent=\""+(indent?"yes":"no")+"\" omit-xml-declaration=\""+(omitXmlDeclaration?"yes":"no")+"\"/>"
			+ makeRemoveNamespacesXsltTemplates()
			+ "</xsl:stylesheet>";
	}

	public static TransformerPool getRemoveNamespacesTransformerPool(boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return getUtilityTransformerPool(()->makeRemoveNamespacesXslt(omitXmlDeclaration,indent),"RemoveNamespaces",omitXmlDeclaration,indent);
	}

	protected static String makeGetRootNamespaceXslt() {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
			+ "<xsl:output method=\"text\"/>"
			+ "<xsl:template match=\"*\">"
			+ "<xsl:value-of select=\"namespace-uri()\"/>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static TransformerPool getGetRootNamespaceTransformerPool() throws ConfigurationException {
		return getUtilityTransformerPool(XmlUtils::makeGetRootNamespaceXslt,"GetRootNamespace",true,false);
	}

	protected static String makeAddRootNamespaceXslt(String namespace, boolean omitXmlDeclaration, boolean indent) {
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

	public static TransformerPool getAddRootNamespaceTransformerPool(String namespace, boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return getUtilityTransformerPool(()->makeAddRootNamespaceXslt(namespace,omitXmlDeclaration,indent),"AddRootNamespace["+namespace+"]",omitXmlDeclaration,indent);
	}

	protected static String makeChangeRootXslt(String root, boolean omitXmlDeclaration, boolean indent) {
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

	public static TransformerPool getChangeRootTransformerPool(String root, boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return getUtilityTransformerPool(()->makeChangeRootXslt(root,omitXmlDeclaration,indent),"ChangeRoot["+root+"]",omitXmlDeclaration,indent);
	}

	protected static String makeRemoveUnusedNamespacesXslt(boolean omitXmlDeclaration, boolean indent) {
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

	public static TransformerPool getRemoveUnusedNamespacesTransformerPool(boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return getUtilityTransformerPool(()->makeRemoveUnusedNamespacesXslt(omitXmlDeclaration,indent),"RemoveUnusedNamespaces",omitXmlDeclaration,indent);
	}

	protected static String makeRemoveUnusedNamespacesXslt2(boolean omitXmlDeclaration, boolean indent) {
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

	public static TransformerPool getRemoveUnusedNamespacesXslt2TransformerPool(boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return getUtilityTransformerPool(()->makeRemoveUnusedNamespacesXslt2(omitXmlDeclaration,indent),"RemoveUnusedNamespacesXslt2",omitXmlDeclaration,indent);
	}

	protected static String makeCopyOfSelectXslt(String xpath, boolean omitXmlDeclaration, boolean indent) {
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

	public static TransformerPool getCopyOfSelectTransformerPool(String xpath, boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return getUtilityTransformerPool(()->makeCopyOfSelectXslt(xpath,omitXmlDeclaration,indent),"CopyOfSelect["+xpath+"]",omitXmlDeclaration,indent);
	}


	public static synchronized boolean isNamespaceAwareByDefault() {
		if (namespaceAwareByDefault==null) {
			namespaceAwareByDefault=AppConstants.getInstance().getBoolean(NAMESPACE_AWARE_BY_DEFAULT_KEY, true);
		}
		return namespaceAwareByDefault;
	}

	public static synchronized boolean isXsltStreamingByDefault() {
		if (xsltStreamingByDefault==null) {
			xsltStreamingByDefault=AppConstants.getInstance().getBoolean(XSLT_STREAMING_BY_DEFAULT_KEY, false);
		}
		return xsltStreamingByDefault;
	}

	public static synchronized boolean isIncludeFieldDefinitionByDefault() {
		if (includeFieldDefinitionByDefault==null) {
			includeFieldDefinitionByDefault=AppConstants.getInstance().getBoolean(INCLUDE_FIELD_DEFINITION_BY_DEFAULT_KEY, true);
		}
		return includeFieldDefinitionByDefault;
	}

	public static synchronized boolean isAutoReload() {
		if (autoReload==null) {
			autoReload=AppConstants.getInstance().getBoolean(AUTO_RELOAD_KEY, false);
		}
		return autoReload;
	}

	public static synchronized int getBufSize() {
		if (buffersize==null) {
			buffersize = AppConstants.getInstance().getInt(XSLT_BUFFERSIZE_KEY, XSLT_BUFFERSIZE_DEFAULT);
		}
		return buffersize;
	}

	public static void parseXml(Resource resource, ContentHandler handler) throws IOException, SAXException {
		try {
			XMLReader reader = getXMLReader(resource, handler);
			reader.parse(resource.asInputSource());
		} catch (ParserConfigurationException e) {
			throw new SaxException("Cannot configure parser",e);
		}
	}

	public static void parseXml(String source, ContentHandler handler) throws IOException, SAXException {
		parseXml(new InputSource(new StringReader(source)), handler, null);
	}

	/**
	 * like {@link #parseXml(String source, ContentHandler handler)}, but skips startDocument() and endDocument().
	 * Can be used to parse a string and inject its events in an existing SAX event stream.
	 */
	public static void parseNodeSet(String source, ContentHandler handler) throws IOException, SAXException {
		ContentHandler filter = new BodyOnlyFilter(handler);
		parseXml("<nodesetRoot>"+source+"</nodesetRoot>", filter);
	}

	public static void parseXml(InputSource inputSource, ContentHandler handler) throws IOException, SAXException {
		parseXml(inputSource, handler, null);
	}

	public static void parseXml(InputSource inputSource, ContentHandler handler, ErrorHandler errorHandler) throws IOException, SAXException {
		XMLReader xmlReader;
		try {
			xmlReader = getXMLReader(null, handler);
			if (errorHandler != null) {
				xmlReader.setErrorHandler(errorHandler);
			}
		} catch (ParserConfigurationException e) {
			throw new SaxException("Cannot configure parser",e);
		}
		xmlReader.parse(inputSource);
	}

	public static XMLReader getXMLReader(ContentHandler handler) throws ParserConfigurationException, SAXException {
		return getXMLReader(null, handler);
	}

	public static XMLReader getXMLReader(IScopeProvider scopeProvider) throws ParserConfigurationException, SAXException {
		return getXMLReader(true, scopeProvider);
	}

	private static XMLReader getXMLReader(IScopeProvider scopeProvider, ContentHandler handler) throws ParserConfigurationException, SAXException {
		XMLReader xmlReader = getXMLReader(true, scopeProvider);
		xmlReader.setContentHandler(handler);
		if (handler instanceof LexicalHandler) {
			xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
		}
		if (handler instanceof ErrorHandler) {
			xmlReader.setErrorHandler((ErrorHandler)handler);
		}
		return xmlReader;
	}

	private static XMLReader getXMLReader(boolean namespaceAware, IScopeProvider scopeProvider) throws ParserConfigurationException, SAXException {
		SAXParserFactory factory = getSAXParserFactory(namespaceAware);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		XMLReader xmlReader = factory.newSAXParser().getXMLReader();
		if (scopeProvider!=null) {
			xmlReader.setEntityResolver(new ClassLoaderEntityResolver(scopeProvider));
		} else {
			xmlReader.setEntityResolver(new NonResolvingExternalEntityResolver());
		}
		return xmlReader;
	}

	public static Document buildDomDocument(Reader in) throws DomBuilderException {
		return buildDomDocument(in,isNamespaceAwareByDefault());
	}

	public static Document buildDomDocument(Reader in, boolean namespaceAware) throws DomBuilderException {
		return buildDomDocument(in, namespaceAware, false);
	}

	public static Document buildDomDocument(InputSource src, boolean namespaceAware) throws DomBuilderException {
		return buildDomDocument(src, namespaceAware, false);
	}

	public static Document buildDomDocument(InputSource src, boolean namespaceAware, boolean resolveExternalEntities) throws DomBuilderException {
		Document document;
		try {
			DocumentBuilderFactory factory = getDocumentBuilderFactory(namespaceAware);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			if (!resolveExternalEntities) {
				builder.setEntityResolver(new NonResolvingExternalEntityResolver());
			}
			document = builder.parse(src);
		} catch (ParserConfigurationException | IOException | SAXException e) {
			throw new DomBuilderException(e);
		}
		if (document == null) {
			throw new DomBuilderException("Parsed Document is null");
		}
		return document;
	}
	public static Document buildDomDocument(Reader in, boolean namespaceAware, boolean resolveExternalEntities) throws DomBuilderException {
		return buildDomDocument(new InputSource(in), namespaceAware, resolveExternalEntities);
	}

	/**
	 * Convert an XML string to a Document
	 * Creation date: (20-02-2003 8:12:52)
	 */
	public static Document buildDomDocument(String s) throws DomBuilderException {
		StringReader sr = new StringReader(s);
		return buildDomDocument(sr);
	}

	public static Document buildDomDocument(String s, boolean namespaceAware) throws DomBuilderException {
		return buildDomDocument(s, namespaceAware, false);
	}

	public static Document buildDomDocument(String s, boolean namespaceAware, boolean resolveExternalEntities) throws DomBuilderException {
		if (StringUtils.isEmpty(s)) {
			throw new DomBuilderException("input is null");
		}
		StringReader sr = new StringReader(s);
		return buildDomDocument(sr, namespaceAware, resolveExternalEntities);
	}

	/**
	 * Build a Document from a URL
	 */
	public static Document buildDomDocument(URL url)
		throws DomBuilderException {
		Reader in;
		Document output;

		try {
			in = StreamUtil.getCharsetDetectingInputStreamReader(url.openStream());
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

	public static Element buildElement(Message s) throws DomBuilderException {
		try {
			return buildElement(s.asString());
		} catch (IOException e) {
			throw new DomBuilderException(e);
		}
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
			}
			throw new IllegalArgumentException("no valid xml declaration in string ["+xmlString+"]");
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
			}
			throw new IllegalArgumentException("no valid xml declaration in string ["+xmlString+"]");
		}
		return xmlString;
	}

	public static String getNamespaceClause(String namespaceDefs) {
		StringBuilder namespaceClause = new StringBuilder();
		for (Entry<String,String> namespaceDef:getNamespaceMap(namespaceDefs).entrySet()) {
			String prefixClause=namespaceDef.getKey()==null?"":":"+namespaceDef.getKey();
			namespaceClause.append(" xmlns").append(prefixClause).append("=\"").append(namespaceDef.getValue()).append("\"");
		}
		return namespaceClause.toString();
	}

	public static Map<String,String> getNamespaceMap(String namespaceDefs) {
		Map<String,String> namespaceMap= new LinkedHashMap<>();
		if (namespaceDefs != null) {
			for (final String namespaceDef : StringUtil.split(namespaceDefs, ", \t\r\n\f")) {
				int separatorPos = namespaceDef.indexOf('=');
				String prefix = separatorPos < 1 ? null : namespaceDef.substring(0, separatorPos);
				String namespace = namespaceDef.substring(separatorPos + 1);
				namespaceMap.put(prefix, namespace);
			}
		}
		return namespaceMap;
	}

	public static String createXPathEvaluatorSource(String XPathExpression) {
		return createXPathEvaluatorSource(XPathExpression, OutputType.TEXT);
	}

	public static String createXPathEvaluatorSource(String xPathExpression, OutputType outputMethod) {
		return createXPathEvaluatorSource(null, xPathExpression, outputMethod);
	}

	public static String createXPathEvaluatorSource(String namespaceDefs, String xPathExpression, OutputType outputMethod) {
		return createXPathEvaluatorSource(namespaceDefs, xPathExpression, outputMethod, false);
	}

	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, OutputType outputMethod, boolean includeXmlDeclaration) {
		return createXPathEvaluatorSource(namespaceDefs, XPathExpression, outputMethod, includeXmlDeclaration, null);
	}

	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, OutputType outputMethod, boolean includeXmlDeclaration, ParameterList params) {
		return createXPathEvaluatorSource(namespaceDefs, XPathExpression, outputMethod, includeXmlDeclaration, params, true);
	}

	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, OutputType outputMethod, boolean includeXmlDeclaration, ParameterList params, boolean stripSpace) {
		return createXPathEvaluatorSource(namespaceDefs, XPathExpression, outputMethod, includeXmlDeclaration, params, stripSpace, false, null, 0);
	}

	/*
	 * version of createXPathEvaluator that allows to set outputMethod, and uses copy-of instead of value-of, and enables use of parameters.
	 */
	public static String createXPathEvaluatorSource(String namespaceDefs, String xpathExpression, @Nonnull OutputType outputMethod, boolean includeXmlDeclaration, ParameterList params, boolean stripSpace, boolean ignoreNamespaces, String separator, int xsltVersion) {
		String namespaceClause = getNamespaceClause(namespaceDefs);

		final String copyMethod;
		if (outputMethod == OutputType.XML) {
			copyMethod = "copy-of";
		} else {
			copyMethod = "value-of";
		}

		final String separatorString = separator != null ? " separator=\"" + separator + "\"" : "";

		return createXPathEvaluatorSource(x -> "<xsl:"+copyMethod+" "+namespaceClause+" select=\"" + XmlEncodingUtils.encodeChars(xpathExpression) + "\"" + separatorString + "/>", xpathExpression, outputMethod, includeXmlDeclaration, params, stripSpace, ignoreNamespaces, xsltVersion);
	}

	public static String createXPathEvaluatorSource(Function<String,String> xpathContainerSupplier, String xpathExpression, @Nonnull OutputType outputMethod, boolean includeXmlDeclaration, ParameterList params, boolean stripSpace, boolean ignoreNamespaces, int xsltVersion) {
		if (StringUtils.isEmpty(xpathExpression)) {
			throw new IllegalArgumentException("XPathExpression must be filled");
		}

		StringBuilder paramsString = new StringBuilder();
		if (params != null) {
			for (Parameter param: params) {
				paramsString.append("<xsl:param name=\"").append(param.getName()).append("\"/>");
			}
		}
		int version = (xsltVersion == 0) ? DEFAULT_XSLT_VERSION : xsltVersion;

		//xslt version 1 ignores namespaces by default, setting this to true will generate a different non-xslt1-parsable xslt: xslt1 'Can not convert #RTREEFRAG to a NodeList'
		if(version == 1 && ignoreNamespaces) {
			ignoreNamespaces = false;
		}

		String xsl =
			// "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\""+version+".0\" xmlns:xalan=\"http://xml.apache.org/xslt\">" +
			"<xsl:output method=\""+outputMethod.getOutputMethod()+"\" omit-xml-declaration=\""+ (includeXmlDeclaration ? "no": "yes") +"\"/>" +
			(stripSpace?"<xsl:strip-space elements=\"*\"/>":"") +
			paramsString +
			(ignoreNamespaces ?
				"<xsl:template match=\"/\">" +
					"<xsl:variable name=\"prep\"><xsl:apply-templates/></xsl:variable>" +
					"<xsl:call-template name=\"expression\">" +
						"<xsl:with-param name=\"root\" select=\"$prep\"/>" +
					"</xsl:call-template>" +
				"</xsl:template>" +
				makeRemoveNamespacesXsltTemplates()+

				"<xsl:template name=\"expression\">" +
					"<xsl:param name=\"root\" />" +
					"<xsl:for-each select=\"$root\">" +
						xpathContainerSupplier.apply(xpathExpression) +
					"</xsl:for-each>" +
				"</xsl:template>"
			:
			"<xsl:template match=\"/\">" +
				xpathContainerSupplier.apply(xpathExpression) +
			"</xsl:template>" )+
			"</xsl:stylesheet>";

		return xsl;
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

	public static Source stringToSourceForSingleUse(String xmlString) throws SAXException {
		return stringToSourceForSingleUse(xmlString, isNamespaceAwareByDefault());
	}

	public static Source stringToSourceForSingleUse(String xmlString, boolean namespaceAware) throws SAXException {
		if (namespaceAware) {
			StringReader reader = new StringReader(xmlString);
			InputSource is = new InputSource(reader);
			return inputSourceToSAXSource(is, namespaceAware, null);
		}
		try {
			return stringToSource(xmlString, namespaceAware);
		} catch (DomBuilderException e) {
			throw new SaxException(e);
		}
	}

	public static SAXSource inputSourceToSAXSource(Resource resource) throws SAXException, IOException {
		return inputSourceToSAXSource(resource.asInputSource(), true, resource);
	}

	public static SAXSource inputSourceToSAXSource(InputSource is) throws SAXException {
		return inputSourceToSAXSource(is, true, null);
	}

	public static SAXSource inputSourceToSAXSource(InputSource is, boolean namespaceAware, Resource scopeProvider) throws SAXException {
		try {
			return new SAXSource(getXMLReader(namespaceAware, scopeProvider), is);
		} catch (ParserConfigurationException e) {
			throw new SaxException(e);
		}
	}

	public static int interpretXsltVersion(String xsltVersion) {
		if (StringUtils.isEmpty(xsltVersion)) {
			return 0;
		}
		int dotPos=xsltVersion.indexOf('.');
		if (dotPos>0) {
			xsltVersion=xsltVersion.substring(0, dotPos);
		}
		if (StringUtils.isEmpty(xsltVersion)) {
			return 0;
		}
		return Integer.parseInt(xsltVersion);
	}

	public static int detectXsltVersion(String xsltString) throws TransformerConfigurationException {
		try {
			TransformerPool tpVersion = XmlUtils.getDetectXsltVersionTransformerPool();
			String version=tpVersion.transform(xsltString, null, true);
			log.debug("detected version ["+version+"] for xslt ["+xsltString+"]");
			return interpretXsltVersion(version);
		} catch (Exception e) {
			throw new TransformerConfigurationException(e);
		}
	}

	public static int detectXsltVersion(URL xsltUrl) throws TransformerConfigurationException {
		try {
			TransformerPool tpVersion = XmlUtils.getDetectXsltVersionTransformerPool();
			StreamSource stylesource = new StreamSource(xsltUrl.openStream());
			stylesource.setSystemId(ClassUtils.getCleanedFilePath(xsltUrl.toExternalForm()));

			return interpretXsltVersion(tpVersion.transform(stylesource));
		} catch (Exception e) {
			throw new TransformerConfigurationException(e);
		}
	}

	public static Transformer createTransformer(String xsltString) throws TransformerConfigurationException {
		try {
			return createTransformer(xsltString, detectXsltVersion(xsltString));
		} catch (Exception e) {
			throw new TransformerConfigurationException(e);
		}
	}

	public static Transformer createTransformer(String xsltString, int xsltVersion) throws TransformerConfigurationException {

		StringReader sr = new StringReader(xsltString);

		StreamSource stylesource = new StreamSource(sr);
		return createTransformer(stylesource, xsltVersion);
	}

	public static Transformer createTransformer(URL url) throws TransformerConfigurationException {
		try {
			return createTransformer(url, detectXsltVersion(url));
		} catch (Exception e) {
			throw new TransformerConfigurationException(e);
		}
	}

	public static Transformer createTransformer(URL url, int xsltVersion) throws TransformerConfigurationException, IOException {
		StreamSource stylesource = new StreamSource(url.openStream());
		stylesource.setSystemId(ClassUtils.getCleanedFilePath(url.toExternalForm()));

		return createTransformer(stylesource, xsltVersion);
	}

	public static Transformer createTransformer(Source source, int xsltVersion) throws TransformerConfigurationException {
		TransformerFactory tFactory = getTransformerFactory(xsltVersion);

		return tFactory.newTransformer(source);
	}

	public static TransformerFactory getTransformerFactory() {
		return getTransformerFactory(1);
	}

	public static TransformerFactory getTransformerFactory(int xsltVersion) {
		return getTransformerFactory(xsltVersion, new TransformerErrorListener());
	}

	public static TransformerFactory getTransformerFactory(int xsltVersion, ErrorListener errorListener) {
		TransformerFactory factory;
		if (xsltVersion == 1) {
			factory = new TransformerFactoryImpl();
			factory.setErrorListener(errorListener);
			if (isXsltStreamingByDefault()) {
				factory.setAttribute(TransformerFactoryImpl.FEATURE_INCREMENTAL, Boolean.TRUE);
			}
			return factory;
		}
		// XSLT version 2 or 3.
		factory = new net.sf.saxon.TransformerFactoryImpl();
		// Use ErrorListener to prevent warning "Stylesheet module ....xsl
		// is included or imported more than once. This is permitted, but
		// may lead to errors or unexpected behavior" written to System.err
		// (https://stackoverflow.com/questions/10096086/how-to-handle-duplicate-imports-in-xslt)
		factory.setErrorListener(errorListener);
		return factory;
	}

	public static synchronized DocumentBuilderFactory getDocumentBuilderFactory() {
		return getDocumentBuilderFactory(isNamespaceAwareByDefault());
	}

	public static synchronized DocumentBuilderFactory getDocumentBuilderFactory(boolean namespaceAware) {
		DocumentBuilderFactory factory;
		factory = new org.apache.xerces.jaxp.DocumentBuilderFactoryImpl();
		factory.setNamespaceAware(namespaceAware);
		return factory;
	}

	public static SAXParserFactory getSAXParserFactory() {
		return getSAXParserFactory(isNamespaceAwareByDefault());
	}

	public static SAXParserFactory getSAXParserFactory(boolean namespaceAware) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);
		return factory;
	}

	public static String convertEndOfLines(String input) {
		if (input==null) {
			return null;
		}
		return input.replaceAll("\r\n?", "\n");
	}

	public static String normalizeWhitespace(String input) {
		if (input==null) {
			return null;
		}
		return input.replaceAll("[\t\n\r]", " ");
	}

	public static String normalizeAttributeValue(String input) {
		return normalizeWhitespace(convertEndOfLines(input));
	}

	public static String cleanseElementName(String candidateName) {
		return candidateName!=null ? candidateName.replaceAll("[^\\w\\-.]", "_") : null;
	}

	/**
	 * encodes a url
	 */
	public static String encodeURL(String url) {
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

	private static char toHexChar(int digitValue) {
		if (digitValue < 10) {
			return (char) ('0' + digitValue);
		}
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
	public static boolean getChildTagAsBoolean(Element el, String tag) {
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
	public static boolean getChildTagAsBoolean(
		Element el,
		String tag,
		boolean defaultValue) {
		String str;

		str = getChildTagAsString(el, tag, null);
		if (str == null) {
			return defaultValue;
		}
		return str.equalsIgnoreCase("true")
				|| str.equalsIgnoreCase("yes")
				|| str.equalsIgnoreCase("on");
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
	public static long getChildTagAsLong(Element el, String tag) {
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
	public static long getChildTagAsLong(
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
	public static String getChildTagAsString(Element el, String tag) {
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
	public static String getChildTagAsString(
		Element el,
		String tag,
		String defaultValue) {
		Element tmpEl;
		String str = "";

		tmpEl = getFirstChildTag(el, tag);
		if (tmpEl != null) {
			str = getStringValue(tmpEl, true);
		}
		return (str.isEmpty()) ? (defaultValue) : (str);
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

		c = new ArrayList<>();
		nl = el.getChildNodes();
		len = nl.getLength();

		allChildren = "*".equals(tag);

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
	public static Element getFirstChildTag(Element el, String tag) {
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
	public static String getStringValue(Element el) {
		return getStringValue(el, true);
	}
	public static String getStringValue(Element el, boolean trimWhitespace) {
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
	 * sets all the parameters of the transformer using a Map with parameter values.
	 * @throws IOException If an IOException occurs.
	 */
	public static void setTransformerParameters(Transformer t, Map<String,Object> parameters) throws IOException {
		t.clearParameters();
		if (parameters == null) {
			return;
		}
		for (String paramName:parameters.keySet()) {
			Object value = parameters.get(paramName);
			if (value != null) {
				if (value instanceof Reader || value instanceof InputStream || value instanceof byte[] || value instanceof Message) {
					try {
						value = Message.asString(value);
					} catch (IOException e) {
						throw new IOException("Cannot get value of parameter ["+paramName+"]", e);
					}
				}
				t.setParameter(paramName, value);
				log.debug("setting parameter [" + paramName+ "] on transformer from class ["+value.getClass().getTypeName()+"]");
			}
			else {
				log.info("omitting setting of parameter ["+paramName+"] on transformer, as it has a null-value");
			}
		}
	}

	public static String transformXml(Transformer t, String s) throws TransformerException, IOException, SAXException {
		return transformXml(t, s, isNamespaceAwareByDefault());
	}

	public static String transformXml(Transformer t, String s, boolean namespaceAware) throws TransformerException, IOException, SAXException {
		return transformXml(t, stringToSourceForSingleUse(s, namespaceAware));
	}

	public static void transformXml(Transformer t, String s, Result result) throws TransformerException, SAXException {
		synchronized (t) {
			t.transform(stringToSourceForSingleUse(s), result);
		}
	}


	public static String transformXml(Transformer t, Source s) throws TransformerException, IOException {

		StringWriter out = new StringWriter(getBufSize());
		transformXml(t,s,out);
		out.close();

		return (out.getBuffer().toString());

	}

	public static void transformXml(Transformer t, Source s, Writer out) throws TransformerException {

		Result result = new StreamResult(out);
		synchronized (t) {
			t.transform(s, result);
		}
	}

	public static boolean isWellFormed(String input) {
		return isWellFormed(input, null);
	}

	public static boolean isWellFormed(String input, String root) {
		return isWellFormed(Message.asMessage(input), root);
	}

	public static boolean isWellFormed(Message input, String root) {
		RootValidations rootValidations = null;
		if (StringUtils.isNotEmpty(root)) {
			rootValidations = new RootValidations(root);
		}
		XmlValidatorContentHandler xmlHandler = new XmlValidatorContentHandler(null, rootValidations, null, true);
		XmlValidatorErrorHandler xmlValidatorErrorHandler = new XmlValidatorErrorHandler(xmlHandler, "Is not well formed");
		xmlHandler.setXmlValidatorErrorHandler(xmlValidatorErrorHandler);
		try {
			// set ErrorHandler to prevent message in System.err: [Fatal Error] :-1:-1: Premature end of file.
			parseXml(input.asInputSource(), xmlHandler, xmlValidatorErrorHandler);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public static Map<String, String> getVersionInfo() {
		Map<String,String> map = new LinkedHashMap<>();

		SAXParserFactory spFactory = getSAXParserFactory();
		map.put("SAXParserFactory-class", spFactory.getClass().getName());
		DocumentBuilderFactory domFactory1 = getDocumentBuilderFactory(false);
		map.put("DocumentBuilderFactory1-class", domFactory1.getClass().getName());
		DocumentBuilderFactory domFactory2 = getDocumentBuilderFactory(true);
		map.put("DocumentBuilderFactory2-class", domFactory2.getClass().getName());

		TransformerFactory tFactory1 = getTransformerFactory(1);
		map.put("TransformerFactory1-class", tFactory1.getClass().getName());
		TransformerFactory tFactory2 = getTransformerFactory(2);
		map.put("TransformerFactory2-class", tFactory2.getClass().getName());

		XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
		map.put("XMLEventFactory-class", xmlEventFactory.getClass().getName());
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		map.put("XMLInputFactory-class", xmlInputFactory.getClass().getName());
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		map.put("XMLOutputFactory-class", xmlOutputFactory.getClass().getName());

		try {
			MessageFactory messageFactory = MessageFactory.newInstance();
			map.put("MessageFactory-class", messageFactory.getClass().getName());
		} catch (SOAPException e) {
			log.warn("unable to create MessageFactory", e);
			map.put("MessageFactory-class", "unable to create MessageFactory (" + e.getClass().getName() + "): "+ e.getMessage() + ")");
		}

		try {
			map.put("Xerces-Version", org.apache.xerces.impl.Version.getVersion());
		} catch (Throwable t) {
			log.warn("could not get Xerces version", t);
			map.put("Xerces-Version", "not found (" + t.getClass().getName() + "): "+ t.getMessage() + ")");
		}

		try {
			String xalanVersion = org.apache.xalan.Version.getVersion();
			map.put("Xalan-Version", xalanVersion);
		} catch (Throwable t) {
			log.warn("could not get Xalan version", t);
			map.put("Xalan-Version", "not found (" + t.getClass().getName() + "): "+ t.getMessage() + ")");
		}
		try {
			String saxonVersion = net.sf.saxon.Version.getProductTitle();
			map.put("Saxon-Version", saxonVersion);
		} catch (Throwable t) {
			log.warn("could not get Saxon version", t);
			map.put("Saxon-Version", "not found (" + t.getClass().getName() + "): "+ t.getMessage() + ")");
		}
		try {
			if (xmlInputFactory instanceof WstxInputFactory) {
				ReaderConfig woodstoxConfig = ((WstxInputFactory)xmlInputFactory).createPrivateConfig();
				String woodstoxVersion = ReaderConfig.getImplName()+" "+ReaderConfig.getImplVersion()+"; xml1.1 "+(woodstoxConfig.isXml11()?"":"not ")+"enabled";
				map.put("Woodstox-Version", woodstoxVersion);
			}
		} catch (Throwable t) {
			log.warn("could not get Woodstox version", t);
			map.put("Woodstox-Version", "not found (" + t.getClass().getName() + "): "+ t.getMessage() + ")");
		}

		return map;
	}

	public static String source2String(Source source, boolean removeNamespaces) throws TransformerException {
		if (removeNamespaces) {
			try {
				TransformerPool tp = getRemoveNamespacesTransformerPool(true,false);
				return tp.transform(source);
			} catch (Exception e) {
				throw new TransformerException(e);
			}
		}

		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = XmlUtils.getTransformerFactory(2); // set xslt2=true to avoid problems with diacritics
		Transformer transformer = tf.newTransformer();
		transformer.transform(source, result);
		writer.flush();
		return writer.toString();
	}

	public static String removeNamespaces(String input) {
		try {
			TransformerPool tp = getRemoveNamespacesTransformerPool(true,false);
			return tp.transform(input,null);
		} catch (Exception e) {
			log.warn("unable to remove namespaces", e);
			return null;
		}
	}

	public static String getRootNamespace(String input) {
		try {
			TransformerPool tp = getGetRootNamespaceTransformerPool();
			return tp.transform(input,null);
		} catch (Exception e) {
			log.warn("unable to find root-namespace", e);
			return null;
		}
	}

	public static String addRootNamespace(String input, String namespace) {
		try {
			TransformerPool tp = getAddRootNamespaceTransformerPool(namespace,true,false);
			return tp.transform(input,null);
		} catch (Exception e) {
			log.warn("unable to add root-namespace", e);
			return null;
		}
	}

	public static String copyOfSelect(String input, String xpath) {
		try {
			TransformerPool tp = getCopyOfSelectTransformerPool(xpath, true,false);
			return tp.transform(input,null);
		} catch (Exception e) {
			log.warn("unable to execute xpath expression ["+xpath+"]", e);
			return null;
		}
	}

	public static String canonicalize(String input) throws IOException {
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setIncludeComments(false);
		ContentHandler handler = new PrettyPrintFilter(xmlWriter, true);
		handler = new CanonicalizeFilter(handler);
		try {
			XmlUtils.parseXml(input, handler);
			return xmlWriter.toString();
		} catch (SAXException e) {
			throw new IOException("ERROR: could not canonicalize ["+input+"]",e);
		}
	}

	public static String nodeToString(Node node) throws TransformerException {
		return nodeToString(node, false);
	}

	public static String nodeToString(Node node, boolean useIndentation) throws TransformerException {
		Transformer t = getTransformerFactory().newTransformer();
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		if (useIndentation) {
			t.setOutputProperty(OutputKeys.INDENT, "yes");
		}
		StringWriter sw = new StringWriter();
		t.transform(new DOMSource(node), new StreamResult(sw));
		return sw.toString();
	}

	public static byte[] nodeToByteArray(Node node) throws TransformerException {
		return nodeToByteArray(node, true);
	}
	public static byte[] nodeToByteArray(Node node, boolean omitXmlDeclaration) throws TransformerException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Transformer t = getTransformerFactory().newTransformer();
		if (omitXmlDeclaration) {
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		}
		Result outputTarget = new StreamResult(outputStream);
		t.transform(new DOMSource(node), outputTarget);
		return outputStream.toByteArray();
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
	 */
	public static StartElement mergeAttributes(StartElement tag, Iterator<? extends Attribute> attrs, Iterator<? extends Namespace> nsps, XMLEventFactory factory) {
		// create Attribute map
		Map<QName, Attribute> attributes = new HashMap<>();

		// iterate through start tag's attributes
		for (Iterator<Attribute> i = tag.getAttributes(); i.hasNext();) {
			Attribute attr = i.next();
			attributes.put(attr.getName(), attr);
		}
		if (attrs != null) {
			// iterate through new attributes
			while (attrs.hasNext()) {
				Attribute attr = attrs.next();
				attributes.put(attr.getName(), attr);
			}
		}

		Map<QName, Namespace> namespaces = new HashMap<>();
		for (Iterator<Namespace> i = tag.getNamespaces(); i.hasNext();) {
			Namespace ns = i.next();
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

	public static boolean attributesEqual(Attribute attribute1, Attribute attribute2) {
		if (!attribute1.getName().equals(attribute2.getName())) {
			return false;
		} else {
			return attribute1.getValue().equals(attribute2.getValue());
		}
	}


	public static String getAdapterSite(String input, Map parameters) throws IOException, SAXException, TransformerException {
		URL xsltSource = ClassLoaderUtils.getResourceURL(ADAPTERSITE_XSLT);
		Transformer transformer = XmlUtils.createTransformer(xsltSource);
		if (parameters != null) {
			XmlUtils.setTransformerParameters(transformer, parameters);
		}
		return XmlUtils.transformXml(transformer, input);
	}

	public static Collection<String> evaluateXPathNodeSet(String input, String xpathExpr) throws DomBuilderException, XPathExpressionException {
		String msg = XmlUtils.removeNamespaces(input);

		Collection<String> c = new ArrayList<>();
		Document doc = buildDomDocument(msg, true, true);
		XPath xPath = getXPathFactory().newXPath();
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
		if (!c.isEmpty()) {
			return c;
		}
		return null;
	}

	public static String evaluateXPathNodeSetFirstElement(String input, String xpathExpr) throws DomBuilderException, XPathExpressionException {
		Collection<String> c = evaluateXPathNodeSet(input, xpathExpr);
		if (c != null && !c.isEmpty()) {
			return c.iterator().next();
		}
		return null;
	}

	public static Double evaluateXPathNumber(String input, String xpathExpr) throws DomBuilderException, XPathExpressionException {
		String msg = XmlUtils.removeNamespaces(input);

		Document doc = buildDomDocument(msg, true, true);
		XPath xPath = getXPathFactory().newXPath();
		XPathExpression xPathExpression = xPath.compile(xpathExpr);
		Object result = xPathExpression.evaluate(doc, XPathConstants.NUMBER);
		return (Double) result;
	}

	public static Map<String, String> evaluateXPathNodeSet(String input, String xpathExpr, String keyElement, String valueElement) throws DomBuilderException, XPathExpressionException {
		String msg = XmlUtils.removeNamespaces(input);

		Map<String, String> m = new HashMap<>();
		Document doc = buildDomDocument(msg, true, true);
		XPath xPath = getXPathFactory().newXPath();
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
		if (!m.isEmpty()) {
			return m;
		}
		return null;
	}

	public static String toXhtml(Message message) throws IOException {
		if (!Message.isEmpty(message)) {
			String messageCharset = message.getCharset();
			String xhtmlString = message.peek(HTML_MAX_PREAMBLE_SIZE);
			if (xhtmlString.contains("<html>") || xhtmlString.contains("<html ")) {
				CleanerProperties props = new CleanerProperties();
				props.setOmitDoctypeDeclaration(true);
				if(messageCharset != null) {
					props.setCharset(messageCharset);
				}
				HtmlCleaner cleaner = new HtmlCleaner(props);
				TagNode tagNode = cleaner.clean(message.asReader());
				return new SimpleXmlSerializer(props).getAsString(tagNode);
			}
		}
		return null;
	}

	public static XPathFactory getXPathFactory() {
		return getXPathFactory(2);
	}

	public static synchronized XPathFactory getXPathFactory(int xsltVersion) {
		// NB: Currently this method is only called always with XSLT version = 2, but it
		// should be prepared to work correctly if called with version 1 or 3 as well.
		if (xsltVersion == 1) {
			return new org.apache.xpath.jaxp.XPathFactoryImpl();
		}
		// XSLT version 2 or 3
		return new XPathFactoryImpl();
	}

	public static ValidatorHandler getValidatorHandler(URL schemaURL) throws SAXException {
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(schemaURL);
		return schema.newValidatorHandler();
	}

	public static ValidatorHandler getValidatorHandler(Source schemaSource) throws SAXException {
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(schemaSource);
		return schema.newValidatorHandler();
	}
}
