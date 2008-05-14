/*
 * $Log: XmlUtils.java,v $
 * Revision 1.47  2008-05-14 09:24:48  europe\L190409
 * added isAutoReload
 * added skipXmlDeclaration
 *
 * Revision 1.46  2008/02/13 13:33:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added makeSkipEmptyTagsXslt
 *
 * Revision 1.45  2008/01/29 12:18:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added parse functions
 *
 * Revision 1.44  2007/10/15 13:13:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed typo + modified Xerces version retrieval
 *
 * Revision 1.41.2.3  2007/10/12 09:09:07  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix compilation-problems after code-merge
 *
 * Revision 1.41.2.2  2007/10/10 14:30:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.43  2007/10/10 07:23:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * multiple style versions of Xerces
 *
 * Revision 1.42  2007/10/08 12:25:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.41  2007/07/17 11:02:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * encodeChars for byteArray
 *
 * Revision 1.40  2007/05/08 16:04:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added transform() with result as parameter
 *
 * Revision 1.39  2007/02/12 14:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.38  2007/02/05 15:07:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * change RootElementFindingHandler to XmlFindingHandler
 *
 * Revision 1.37  2006/08/24 09:24:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * separated finding wrong root from non-wellformedness;
 * used RootElementFindingHandler in XmlUtils.isWellFormed()
 *
 * Revision 1.36  2006/08/22 12:57:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow use of parameters in xpathExpression
 *
 * Revision 1.35  2006/07/17 07:51:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added buildNode() with default namespace-awareness
 *
 * Revision 1.34  2006/03/21 07:36:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * buildNode with cast to Node
 *
 * Revision 1.33  2006/03/20 15:10:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added buildNode()
 *
 * Revision 1.32  2006/03/15 14:06:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed NullPointerException in encodeChars()
 *
 * Revision 1.31  2006/02/06 11:50:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added isWellFormed() (by Peter Leeuwenburgh)
 *
 * Revision 1.29  2005/12/28 08:30:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed endless recursion in createXpathEvaluator
 *
 * Revision 1.28  2005/10/24 09:26:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * uncapped xml.namespaceAware.default
 *
 * Revision 1.27  2005/10/19 09:06:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added namespaceAware-parameter for stringToSourceForSingleUse
 * added decode/unescape functions
 *
 * Revision 1.26  2005/10/17 11:02:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * isPrintableUnicodeChar made public
 *
 * Revision 1.25  2005/10/17 09:21:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made namspaceAwareByDefault configurable in AppConstants
 * added encodeCdataString
 *
 * Revision 1.24  2005/09/27 08:59:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.23  2005/09/22 15:55:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added streaming transform-function
 * added &apos; to escape-chars
 *
 * Revision 1.22  2005/09/20 13:31:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added schemaLocation-resolver
 *
 * Revision 1.21  2005/07/05 11:12:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added non-namespace aware optionfor buildElement
 *
 * Revision 1.20  2005/06/20 09:01:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made identity_transform public
 *
 * Revision 1.19  2005/06/13 11:48:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made namespaceAware option for stringToSource
 * made separate version of stringToSource, optimized for single use
 *
 * Revision 1.18  2005/06/13 10:12:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected handling of namespaces in DomDocumentBuilder;
 * namespaceAware is now an optional parameter (default=true)
 *
 * Revision 1.17  2005/05/31 09:38:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added versionInfo() and stringToSource()
 *
 * Revision 1.16  2005/01/10 08:56:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Xslt parameter handling by Maps instead of by Ibis parameter system
 *
 * Revision 1.15  2004/11/10 13:01:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added dynamic setting of copy-method in createXpathEvaluatorSource
 *
 * Revision 1.14  2004/10/26 16:18:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set UTF-8 as default inputstream encoding
 *
 * Revision 1.13  2004/10/26 15:35:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reduced logging for transformer parameter setting
 *
 * Revision 1.12  2004/10/12 15:15:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected XmlDeclaration-handling
 *
 * Revision 1.11  2004/10/05 09:56:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of TransformerPool
 *
 * Revision 1.10  2004/09/01 07:15:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added namespaced xpath evaluator
 *
 * Revision 1.9  2004/06/21 10:04:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added version of createXPathEvaluator() that allows to 
 * set output-method to xml (instead of only text), and uses copy-of instead of
 * value-of
 *
 * Revision 1.8  2004/06/16 13:08:34  Johan Verrips <johan.verrips@ibissource.org>
 * added IdentityTransform
 *
 * Revision 1.7  2004/05/25 09:11:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added transformXml from Document
 *
 * Revision 1.6  2004/05/25 08:41:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added createXPathEvaluator()
 *
 * Revision 1.5  2004/04/27 10:52:50  unknown <unknown@ibissource.org>
 * Make thread-safety a little more efficient
 *
 * Revision 1.4  2004/03/26 10:42:37  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/23 17:09:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
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
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.NestableException;
import org.apache.log4j.Logger;
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
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Some utilities for working with XML. 
 *
 * @author  Johan Verrips
 * @version Id
 */
public class XmlUtils {
	public static final String version = "$RCSfile: XmlUtils.java,v $ $Revision: 1.47 $ $Date: 2008-05-14 09:24:48 $";
	static Logger log = LogUtil.getLogger(XmlUtils.class);

	static final String W3C_XML_SCHEMA =       "http://www.w3.org/2001/XMLSchema";
	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static final String JAXP_SCHEMA_SOURCE =   "http://java.sun.com/xml/jaxp/properties/schemaSource";

	public static final String NAMESPACE_AWARE_BY_DEFAULT_KEY = "xml.namespaceAware.default";
	public static final String AUTO_RELOAD_KEY = "xml.auto.reload";

	public final static String OPEN_FROM_FILE = "file";
	public final static String OPEN_FROM_URL = "url";
	public final static String OPEN_FROM_RESOURCE = "resource";
	public final static String OPEN_FROM_XML = "xml";
	
	private static Boolean namespaceAwareByDefault = null;
	private static Boolean autoReload = null;

	public static final String XPATH_GETROOTNODENAME = "name(/node()[position()=last()])";

	public static String IDENTITY_TRANSFORM =
		"<?xml version=\"1.0\"?><xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:template match=\"@*|*|processing-instruction()|comment()\">"
			+ "<xsl:copy><xsl:apply-templates select=\"*|@*|text()|processing-instruction()|comment()\" />"
			+ "</xsl:copy></xsl:template></xsl:stylesheet>";

	public XmlUtils() {
		super();
	}


	public static String makeSkipEmptyTagsXslt(boolean omitXmlDeclaration, boolean indent) {
		return 	
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:output method=\"xml\" indent=\""+(indent?"yes":"no")+"\" omit-xml-declaration=\""+(omitXmlDeclaration?"yes":"no")+"\"/>"
			+ "<xsl:strip-space elements=\"*\"/>"
			+ "<xsl:template match=\"* [.//text()] | text()|@*|comment()|processing-instruction()\">"
			+ "<xsl:copy>"
			+ "<xsl:apply-templates select=\"*|@*|comment()|processing-instruction()|text()\"/>"
			+ "</xsl:copy>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static synchronized boolean isNamespaceAwareByDefault() {
		if (namespaceAwareByDefault==null) {
			boolean aware=AppConstants.getInstance().getBoolean(NAMESPACE_AWARE_BY_DEFAULT_KEY, false);
			namespaceAwareByDefault = new Boolean(aware);
		}
		return namespaceAwareByDefault.booleanValue();
	}

	public static synchronized boolean isAutoReload() {
		if (autoReload==null) {
			boolean reload=AppConstants.getInstance().getBoolean(AUTO_RELOAD_KEY, false);
			autoReload = new Boolean(reload);
		}
		return autoReload.booleanValue();
	}


	static public void parseXml(ContentHandler handler, String source) throws IOException, SAXException {
		parseXml(handler,new Variant(source).asXmlInputSource());
	}
	
	static public void parseXml(ContentHandler handler, InputSource source) throws IOException, SAXException {
		XMLReader parser;
		parser = getParser();
		parser.setContentHandler(handler);
		parser.parse(source);
	}

	static private XMLReader getParser() throws SAXException  {
		XMLReader parser = null;
		parser = XMLReaderFactory.createXMLReader();
		return parser;
	}


	static public Document buildDomDocument(File file)
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
	
	static public Document buildDomDocument(Reader in) throws DomBuilderException {
		return buildDomDocument(in,isNamespaceAwareByDefault());
	}
	
	static public Document buildDomDocument(Reader in, boolean namespaceAware)
		throws DomBuilderException {
		Document document;
		InputSource src;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
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
		return (buildDomDocument(sr));
	}

	public static Document buildDomDocument(String s, boolean namespaceAware) throws DomBuilderException {
		if (StringUtils.isEmpty(s)) {
			throw new DomBuilderException("input is null");
		}
		StringReader sr = new StringReader(s);
		return (buildDomDocument(sr,namespaceAware));
	}
	/**
	 * Build a Document from a URL
	 * @param url
	 * @return Document
	 * @throws DomBuilderException
	 */
	static public Document buildDomDocument(URL url)
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
	public static Element buildElement(String s, boolean namespaceAware) throws DomBuilderException {
		return buildDomDocument(s,namespaceAware).getDocumentElement();
	}

	/**
	 * Convert an XML string to a Document, then return the root-element as a Node
	 */
	public static Node buildNode(String s, boolean namespaceAware) throws DomBuilderException {
		log.debug("buildNode() ["+s+"],["+namespaceAware+"]");
		return (Node) buildElement(s,namespaceAware);
	}

	public static Node buildNode(String s) throws DomBuilderException {
		log.debug("buildNode() ["+s+"]");
		return (Node) buildElement(s,isNamespaceAwareByDefault());
	}


	/**
	 * Convert an XML string to a Document, then return the root-element.
	 * (namespace aware)
	 */
	public static Element buildElement(String s) throws DomBuilderException {

		return buildDomDocument(s).getDocumentElement();
	}


	public static String skipXmlDeclaration(String xmlString) {
		if (xmlString!=null && xmlString.startsWith("<?xml")) {
			int endPos = xmlString.indexOf("?>")+2;
			if (endPos>0) {
				try {
					while (Character.isWhitespace(xmlString.charAt(endPos))) {
						endPos++;
					} 
				} catch (IndexOutOfBoundsException e) {
					// silently ignore...
				}
				return xmlString.substring(endPos);
			} else {
				throw new IllegalArgumentException("no valid xml declaration in string ["+xmlString+"]");
			}
		}
		return xmlString;
	}

	public static String createXPathEvaluatorSource(String XPathExpression)
		throws TransformerConfigurationException {
			/*
		if (StringUtils.isEmpty(XPathExpression))
			throw new TransformerConfigurationException("XPathExpression must be filled");

		String xsl =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" xmlns:xalan=\"http://xml.apache.org/xslt\">"
				+ "<xsl:output method=\"text\"/>"
				+ "<xsl:strip-space elements=\"*\"/>"
				+ "<xsl:template match=\"/\">"
				+ "<xsl:value-of select=\""
				+ XPathExpression
				+ "\"/>"
				+ "</xsl:template>"
				+ "</xsl:stylesheet>";

		return xsl;
			*/
			return createXPathEvaluatorSource(XPathExpression,"text");
	}

	/*
	 * version of createXPathEvaluator that allows to set outputMethod, and uses copy-of instead of value-of
	 */
	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, String outputMethod, boolean includeXmlDeclaration) throws TransformerConfigurationException {
		return createXPathEvaluatorSource(namespaceDefs, XPathExpression, outputMethod, includeXmlDeclaration, null);
	}

	/*
	 * version of createXPathEvaluator that allows to set outputMethod, and uses copy-of instead of value-of, and enables use of parameters.
	 */
	public static String createXPathEvaluatorSource(String namespaceDefs, String XPathExpression, String outputMethod, boolean includeXmlDeclaration, List params) throws TransformerConfigurationException {
		if (StringUtils.isEmpty(XPathExpression))
			throw new TransformerConfigurationException("XPathExpression must be filled");
		
		if (namespaceDefs==null) {
			namespaceDefs="";
		}
		
		String copyMethod;	
		if ("xml".equals(outputMethod)) {
			copyMethod="copy-of";
		} else {
			copyMethod="value-of";
		}			
			
		String paramsString = "";
		if (params != null) {
			for (Iterator it = params.iterator(); it.hasNext();) {
				paramsString = paramsString + "<xsl:param name=\"" + it.next() + "\"/>";
			}
		}
		String xsl = 
			// "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" xmlns:xalan=\"http://xml.apache.org/xslt\">" +
			"<xsl:output method=\""+outputMethod+"\" omit-xml-declaration=\""+ (includeXmlDeclaration ? "no": "yes") +"\"/>" +
			"<xsl:strip-space elements=\"*\"/>" +
			paramsString +
			"<xsl:template match=\"/\">" +
			"<xsl:"+copyMethod+" "+namespaceDefs+" select=\"" + XPathExpression + "\"/>" +
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
		Document doc = XmlUtils.buildDomDocument(xmlString,namespaceAware);
		return new DOMSource(doc); 
	}

	public static Source stringToSource(String xmlString) throws DomBuilderException {
		return stringToSource(xmlString,isNamespaceAwareByDefault());
	}

	public static Source stringToSourceForSingleUse(String xmlString, boolean namespaceAware) throws DomBuilderException {
		if (namespaceAware) {
			StringReader sr = new StringReader(xmlString);
			return new StreamSource(sr);
		} else {
			return stringToSource(xmlString, false);
		}
	}

	public static Source stringToSourceForSingleUse(String xmlString) throws DomBuilderException {
		return stringToSourceForSingleUse(xmlString,isNamespaceAwareByDefault());
	}


	public static synchronized Transformer createTransformer(String xsltString)
		throws TransformerConfigurationException {

		StringReader sr = new StringReader(xsltString);

		StreamSource stylesource = new StreamSource(sr);
		return createTransformer(stylesource);
	}
	public static synchronized Transformer createTransformer(URL url)
		throws TransformerConfigurationException, IOException {

		StreamSource stylesource = new StreamSource(url.openStream(),Misc.DEFAULT_INPUT_STREAM_ENCODING);
		stylesource.setSystemId(url.toString());
		return createTransformer(stylesource);
	}
	public static synchronized Transformer createTransformer(Source source)
		throws TransformerConfigurationException {

		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer result;
		result = tFactory.newTransformer(source);

		return result;
	}
	
	/**
	 * translates special characters to xml equivalents
	 * like <b>&gt;</b> and <b>&amp;</b>
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
	 * translates special characters to xml equivalents
	 * like <b>&gt;</b> and <b>&amp;</b>
	 */
	public static String encodeChars(char[] chars, int offset, int length) {

		if (length<=0) {
			return "";
		}
		StringBuffer encoded = new StringBuffer(length);
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
		StringBuffer decoded = new StringBuffer();
		
		boolean inEscape=false;
		int escapeStartPos=0;
		
		for (int i = 0; i < string.length(); i++) {
			char cur=string.charAt(i);
			if (inEscape) {
				if ( cur == ';') {
					inEscape=false;
					String escapedString=string.substring(escapeStartPos,i+1);
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
					inEscape=true;
					escapeStartPos=i;
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
	   * Conversion of special xml signs
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
		StringBuffer encodedUrl = new StringBuffer();
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
			System.err.println("Tag " + tag + " has no integer value");
			e.printStackTrace();
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
	public static Collection getChildTags(Element el, String tag) {
		Collection c;
		NodeList nl;
		int len;
		boolean allChildren;

		c = new LinkedList();
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
		StringBuffer sb = new StringBuffer(1024);
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
	 * Replaces non-unicode-characters by '0x00BF'.
	 */
	public static String encodeCdataString(String string) {
		int length = string.length();
		char[] characters = new char[length];

		string.getChars(0, length, characters, 0);
		StringBuffer encoded = new StringBuffer();
		for (int i = 0; i < length; i++) {
			if (isPrintableUnicodeChar(characters[i]))
				encoded.append(characters[i]);
			else
				encoded.append((char) 0x00BF);
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
				log.warn("omitting setting of parameter ["+name+"] on transformer, as it has a null-value");
			}
		}
	}
	
	public static String transformXml(Transformer t, Document d) throws TransformerException, IOException {
		return transformXml(t, new DOMSource(d));
	}

	public static String transformXml(Transformer t, String s) throws TransformerException, IOException, DomBuilderException {
//		log.debug("transforming under the assumption that source document may contain namespaces (therefore using DOMSource)");
		return transformXml(t, stringToSourceForSingleUse(s));
	}

	public static void transformXml(Transformer t, String s, Result result) throws TransformerException, IOException, DomBuilderException {
		synchronized (t) {
			t.transform(stringToSourceForSingleUse(s), result);
		}
	}

	
	public static String transformXml(Transformer t, Source s)
		throws TransformerException, IOException {

		StringWriter out = new StringWriter(64 * 1024);
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

	
	static public String resolveSchemaLocations(String locationAttribute) {
		String result=null;
		StringTokenizer st = new StringTokenizer(locationAttribute);
		while (st.hasMoreTokens()) {
			if (result==null) {
				result="";
			} else {
				result+=" ";
			}
			String namespace=st.nextToken();
			result += namespace+" ";
			if (st.hasMoreTokens()) {
				String location=st.nextToken();
				URL url = ClassUtils.getResourceURL(XmlUtils.class, location);
				if (url!=null) {
					result+=url.toExternalForm();
				} else {
					log.warn("could not resolve location ["+location+"] for namespace ["+namespace+"] to URL");
					result+=location;
				}
			} else {
				log.warn("no location for namespace ["+namespace+"]");
			}
		}
		return result;
	}


	static public boolean isWellFormed(String input) {
		return isWellFormed(input, null);
	}

	static public boolean isWellFormed(String input, String root) {
		Variant in = new Variant(input);
		InputSource is = in.asXmlInputSource();

		XmlFindingHandler xmlHandler = new XmlFindingHandler();
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(is, xmlHandler);
		} catch (Exception e) {
			return false;
		}

		if (root != null) {
			String tagRoot = xmlHandler.getRootElementName();
			if (!tagRoot.equals(root))
				return false;
		}

		return true;
	}
	
	/*
	 *This function does not operate with Xerces 1.4.1
	 */

	static public boolean ValidateToSchema(InputSource src, URL schema)
		throws NestableException {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(true);

		SAXParser saxParser;
		try {
			saxParser = factory.newSAXParser();
			saxParser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
			saxParser.setProperty(JAXP_SCHEMA_SOURCE, schema.openStream());

		} catch (ParserConfigurationException e) {
			throw new NestableException(e);
		} catch (SAXException e) {
			throw new NestableException(e);
		} catch (IOException e) {
			throw new NestableException(e);
		}
		try {
			saxParser.parse(src, new DefaultHandler());
			return true;
		} catch (Exception e) {
			log.warn(e);
		}
		return false;
	}
	/**
	 * Performs an Identity-transform, with resolving entities with the content files in the classpath
	 * @param input
	 * @return String (the complete and xml)
	 * @throws DomBuilderException
	 */
	static public String identityTransform(String input)
		throws DomBuilderException {
		String result = "";
		try {
			DocumentBuilderFactory factory =
				DocumentBuilderFactory.newInstance();
			Document document;
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(new ClassPathEntityResolver());
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
		StringBuffer sb=new StringBuffer();
		sb.append(version+SystemUtils.LINE_SEPARATOR);
		sb.append("XML tool version info:"+SystemUtils.LINE_SEPARATOR);
		
		SAXParserFactory spFactory = SAXParserFactory.newInstance();
		sb.append("SAXParserFactory-class ="+spFactory.getClass().getName()+SystemUtils.LINE_SEPARATOR);
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		sb.append("DocumentBuilderFactory-class ="+domFactory.getClass().getName()+SystemUtils.LINE_SEPARATOR);

		TransformerFactory tFactory = TransformerFactory.newInstance();
		sb.append("TransformerFactory-class ="+tFactory.getClass().getName()+SystemUtils.LINE_SEPARATOR);

		sb.append("Apache-XML tool version info:"+SystemUtils.LINE_SEPARATOR);

		try {
			sb.append("Xerces-Version(old style)="+org.apache.xerces.framework.Version.fVersion+SystemUtils.LINE_SEPARATOR);
		}  catch (Throwable t) {
			sb.append("Xerces-Version(old style) not found ("+t.getClass().getName()+": "+t.getMessage()+")"+SystemUtils.LINE_SEPARATOR);
		}
		try {
			sb.append("Xerces-Version(new style)="+org.apache.xerces.impl.Version.getVersion()+SystemUtils.LINE_SEPARATOR);
		}  catch (Throwable t) {
			sb.append("Xerces-Version(new style) not found ("+t.getClass().getName()+": "+t.getMessage()+")"+SystemUtils.LINE_SEPARATOR);
		}
			
		try {
			sb.append("Xalan-version="+org.apache.xalan.Version.getVersion()+SystemUtils.LINE_SEPARATOR);
		}  catch (Throwable t) {
			sb.append("Xalan-Version not found ("+t.getClass().getName()+": "+t.getMessage()+")"+SystemUtils.LINE_SEPARATOR);
		}

		try {
//			sb.append("XmlCommons-Version="+org.apache.xmlcommons.Version.getVersion()+SystemUtils.LINE_SEPARATOR);
		}  catch (Throwable t) {
			sb.append("XmlCommons-Version not found ("+t.getClass().getName()+": "+t.getMessage()+")"+SystemUtils.LINE_SEPARATOR);
		}

		return sb.toString();
	}

}
