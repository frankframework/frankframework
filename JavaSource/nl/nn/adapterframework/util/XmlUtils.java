/*
 * $Log: XmlUtils.java,v $
 * Revision 1.22  2005-09-20 13:31:01  europe\L190409
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


import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.NestableException;
import org.apache.log4j.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Some utilities for working with XML. As soon as the Apache XML Commons project
 * delivers something usefull, this class will possibly be removed.
 * <p>Creation date: (20-02-2003 8:05:19)</p>
 * @version Id
 * @author Johan Verrips IOS
 */
public class XmlUtils {
	public static final String version = "$RCSfile: XmlUtils.java,v $ $Revision: 1.22 $ $Date: 2005-09-20 13:31:01 $";
	static Logger log = Logger.getLogger(XmlUtils.class);

	static final String W3C_XML_SCHEMA =       "http://www.w3.org/2001/XMLSchema";
	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static final String JAXP_SCHEMA_SOURCE =   "http://java.sun.com/xml/jaxp/properties/schemaSource";

	public final static String OPEN_FROM_FILE = "file";
	public final static String OPEN_FROM_URL = "url";
	public final static String OPEN_FROM_RESOURCE = "resource";
	public final static String OPEN_FROM_XML = "xml";

	public static final String XPATH_GETROOTNODENAME = "name(/node()[position()=last()])";

	public static String IDENTITY_TRANSFORM =
		"<?xml version=\"1.0\"?><xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:template match=\"@*|*|processing-instruction()|comment()\">"
			+ "<xsl:copy><xsl:apply-templates select=\"*|@*|text()|processing-instruction()|comment()\" />"
			+ "</xsl:copy></xsl:template></xsl:stylesheet>";

	public XmlUtils() {
		super();
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
			log.debug("Ëxception closing file", e);
		}
		return output;
	}
	
	static public Document buildDomDocument(Reader in) throws DomBuilderException {
		return buildDomDocument(in,true);
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
	 * Convert an XML string to a Document, then return the root-element.
	 * (namespace aware)
	 */
	public static Element buildElement(String s) throws DomBuilderException {

		return buildDomDocument(s).getDocumentElement();
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
			
		String xsl = 
			// "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" xmlns:xalan=\"http://xml.apache.org/xslt\">" +
			"<xsl:output method=\""+outputMethod+"\" omit-xml-declaration=\""+ (includeXmlDeclaration ? "no": "yes") +"\"/>" +
			"<xsl:strip-space elements=\"*\"/>" +
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
		return createXPathEvaluator(XPathExpression, outputMethod);
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
		return stringToSource(xmlString,true);
	}

	public static Source stringToSourceForSingleUse(String xmlString) throws DomBuilderException {
		StringReader sr = new StringReader(xmlString);
		return new StreamSource(sr);
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

		int length = string.length();
		char[] characters = new char[length];
		string.getChars(0, length, characters, 0);
		StringBuffer encoded = new StringBuffer();
		String escape;
		for (int i = 0; i < length; i++) {
			escape = escapeChar(characters[i]);
			if (escape == null)
				encoded.append(characters[i]);
			else
				encoded.append(escape);
		}
		return encoded.toString();
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
		}
		return null;
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
	 * sets all the parameters of the transformer using a HashMap with parameter values. 
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

	
	public static String transformXml(Transformer t, Source s)
		throws TransformerException, IOException {

		StringWriter out = new StringWriter(64 * 1024);
		Result result = new StreamResult(out);

		synchronized (t) {
			t.transform(s, result);
		}

		out = (StringWriter) ((StreamResult) result).getWriter();
		out.close();

		return (out.getBuffer().toString());

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
			sb.append("Xerces-Version="+org.apache.xerces.framework.Version.fVersion+SystemUtils.LINE_SEPARATOR);
		}  catch (Throwable t) {
			sb.append("Xerces-Version not found ("+t.getClass().getName()+": "+t.getMessage()+")"+SystemUtils.LINE_SEPARATOR);
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
