package nl.nn.adapterframework.util;

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
import java.util.LinkedList;


/**
 * Some utilities for working with XML. As soon as the Apache XML Commons project
 * delivers something usefull, this class will possibly be removed.
 * <p>Creation date: (20-02-2003 8:05:19)</p>
 * <p>$Id: XmlUtils.java,v 1.2 2004-02-04 10:02:00 a1909356#db2admin Exp $</p>
 * @author Johan Verrips IOS
 */
public class XmlUtils {
	public static final String version="$Id: XmlUtils.java,v 1.2 2004-02-04 10:02:00 a1909356#db2admin Exp $";
	
    static final String W3C_XML_SCHEMA       = "http://www.w3.org/2001/XMLSchema";
    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String JAXP_SCHEMA_SOURCE   = "http://java.sun.com/xml/jaxp/properties/schemaSource";

	
	public final static String OPEN_FROM_FILE = "file";
    public final static String OPEN_FROM_URL = "url";
    public final static String OPEN_FROM_RESOURCE = "resource";
    public final static String OPEN_FROM_XML = "xml";
    static Logger log = Logger.getLogger("XmlUtils");

    public static final String XSLT_GETROOTNODENAME = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
            + "<xsl:output omit-xml-declaration=\"yes\" media-type=\"text\"/>"
            + "<xsl:template match=\"/\"><xsl:value-of select=\"name(/node()[position()=last()])\"/></xsl:template>"
            + "</xsl:stylesheet>";

/**
 * XmlUtils constructor comment.
 */
public XmlUtils() {
	super();
}
    static public Document buildDomDocument(File file)
        throws DomBuilderException
    {
        Reader      in;
        Document    output;
        
        try
        {
            in = new FileReader(file);
        }
        catch (FileNotFoundException e)
        {
            throw new DomBuilderException(e);
        }
        output = buildDomDocument(in);
        try
        {
            in.close();
        }
        catch (IOException e)
        {
            log.debug("Ëxception closing file", e);
        }
        return output;
    }
    static public Document buildDomDocument(Reader in)
        throws DomBuilderException
    {
        Document document;
        InputSource src;
        
        DocumentBuilderFactory factory = 
            DocumentBuilderFactory.newInstance();
        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            src = new InputSource(in);
            document = builder.parse(src);
        }
        catch (SAXParseException e)
        {
            throw new DomBuilderException(e);
        }
        catch (ParserConfigurationException e)
        {
            throw new DomBuilderException(e);
        }
        catch (IOException e)
        {
            throw new DomBuilderException(e);
        }
        catch (SAXException e)
        {
            throw new DomBuilderException(e);
        }
        if (document==null) {
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
    /**
     * Build a Document from a URL
     * @param url
     * @return Document
     * @throws DomBuilderException
     */
    static public Document buildDomDocument(URL url)
        throws DomBuilderException
    {
        Reader      in;
        Document    output;
        
		try
		{
			in = new InputStreamReader(url.openStream());
		}
		catch (IOException e)
		{
            throw new DomBuilderException(e);
		}
        output = buildDomDocument(in);
        try
        {
            in.close();
        }
        catch (IOException e)
        {
           log.debug("Exception closing URL-stream", e);
        }
        return output;
    }
/**
 * Convert an XML string to a Document
 * Creation date: (20-02-2003 8:12:52)
 * @return org.w3c.dom.Document
 * @exception nl.nn.adapterframework.util.DomBuilderException The exception description.
 */
public static Element buildElement(String s) throws DomBuilderException {

		return buildDomDocument(s).getDocumentElement();
	}
public static synchronized Transformer createTransformer(String xsltString) throws javax.xml.transform.TransformerConfigurationException{

    StringReader sr = new StringReader(xsltString);

    StreamSource stylesource = new StreamSource(sr);
    return createTransformer(stylesource);
}
public static synchronized Transformer createTransformer(URL url) throws TransformerConfigurationException, IOException {

    StreamSource stylesource = new StreamSource(url.openStream());
    stylesource.setSystemId(url.toString());
    return createTransformer(stylesource);
}
public static synchronized Transformer createTransformer(Source source) throws javax.xml.transform.TransformerConfigurationException {

   
    TransformerFactory tFactory = TransformerFactory.newInstance();
    Transformer result;
    result = tFactory.newTransformer(source);

        //TODO: Read the parameters
    return result;
}
  /**
   * translates special characters to xml equivalents
   * like <b>&gt;</b> and <b>&amp;</b>
   */
  public static String encodeChars(String string){

     int length = string.length();
     char[] characters = new char[length];
     string.getChars(0, length, characters, 0);
     StringBuffer encoded = new StringBuffer();
     String escape;
     for(int i = 0;i<length;i++){
        escape = escapeChar(characters[i]);
        if(escape == null) encoded.append(characters[i]);
           else encoded.append(escape);
     }
     return encoded.toString();
  }
/**
   * Conversion of special xml signs
   **/
  private static String escapeChar(char c){
     switch(c){
        case('<')  : return "&lt;";
        case('>')  : return "&gt;";
        case('&')  : return "&amp;";
        case('\"') : return "&quot;";
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
    static public boolean getChildTagAsBoolean(Element el, String tag)
    {
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
    static public boolean getChildTagAsBoolean(Element el, String tag, boolean defaultValue)
    {
        String str;
        boolean bool;
        
        str = getChildTagAsString(el, tag, null);
        if (str == null)
        {
            return defaultValue;
        }

        bool = false;
        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("yes") ||
            str.equalsIgnoreCase("on"))
        {
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
    static public long getChildTagAsLong(Element el, String tag)
    {
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
    static public long getChildTagAsLong(Element el, String tag, long defaultValue)
    {
        String str;
        long num;
        
        str = getChildTagAsString(el, tag, null);
        num = 0;
        if (str == null)
        {
            return defaultValue;
        }
        try
        {
            num = Long.parseLong(str);
        }
        catch (NumberFormatException e)
        {
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
    static public String getChildTagAsString(Element el, String tag)
    {
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
    static public String getChildTagAsString(Element el, String tag,
            String defaultValue)
    {
        Element tmpEl;
        String str = "";
        
        tmpEl = getFirstChildTag(el, tag);
        if (tmpEl != null)
        {
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
    public static Collection getChildTags(Element el, String tag)
    {
        Collection  c;
        NodeList    nl;
        int         len;
        boolean     allChildren;
        
        c = new LinkedList();
        nl = el.getChildNodes();
        len = nl.getLength();
        
        if ("*".equals(tag))
        {
            allChildren = true;
        }
        else
        {
            allChildren = false;
        }
        
        for (int i = 0; i < len; i++)
        {
            Node n = nl.item(i);
            if (n instanceof Element)
            {
                Element e = (Element)n;
                if (allChildren || e.getTagName().equals(tag))
                {
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
    static public Element getFirstChildTag(Element el, String tag)
    {
        NodeList        nl;
        int             len;
        
        nl = el.getChildNodes();
        len = nl.getLength();
        for (int i = 0; i < len; ++i)
        {
            Node n = nl.item(i);
            if (n instanceof Element)
            {
                Element elem = (Element) n;
                if (elem.getTagName().equals(tag))
                {
                    return elem;
                }
            }
        }
        return null;
    }
    static public String getStringValue(Element el)
    {
        return getStringValue(el, true);
    }
    static public String getStringValue(Element el, boolean trimWhitespace)
    {
        StringBuffer sb = new StringBuffer(1024);
        String str;
        
        NodeList nl = el.getChildNodes();
        for (int i = 0; i < nl.getLength(); ++i)
        {
            Node n = nl.item(i);
            if (n instanceof Text)
            {
                sb.append(n.getNodeValue());
            }
        }
        if (trimWhitespace)
        {
            str = sb.toString().trim();
        }
        else
        {
            str = sb.toString();
        }
        return str;
        
    }
public static synchronized String transformXml(Transformer t, String s) 
	throws TransformerException,IOException {
	
	Variant inputVar = new Variant(s);
    Source in = inputVar.asXmlSource();
 
	return transformXml( t,in);	
}
public static synchronized String transformXml(Transformer t, Source s)
    throws TransformerException,IOException 
{
	    
    StringWriter out = new StringWriter(64 * 1024);
    Result result = new StreamResult(out);

    t.transform(s, result); 

    out = (StringWriter) ((StreamResult) result).getWriter();
    out.close();

    return (out.getBuffer().toString());

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
}
