package nl.nn.adapterframework.util;

import java.io.IOException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class XmlUtilsTest extends FunctionalTransformerPoolTestBase {

	public void testRemoveNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeRemoveNamespacesXslt(omitXmlDeclaration, indent),input,expected);
		testTransformerPool(XmlUtils.getRemoveNamespacesTransformerPool(omitXmlDeclaration, indent),input,expected);
	}

	public void testGetRootNamespace(String input, String expected) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeGetRootNamespaceXslt(),input,expected);
		testTransformerPool(XmlUtils.getGetRootNamespaceTransformerPool(),input,expected);
	}

	public void testAddRootNamespace(String namespace, String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeAddRootNamespaceXslt(namespace, omitXmlDeclaration, indent),input,expected);
		testTransformerPool(XmlUtils.getAddRootNamespaceTransformerPool(namespace, omitXmlDeclaration, indent),input,expected);
	}

	public void testChangeRoot(String root, String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeChangeRootXslt(root, omitXmlDeclaration, indent),input,expected);
		testTransformerPool(XmlUtils.getChangeRootTransformerPool(root, omitXmlDeclaration, indent),input,expected);
	}

	public void testRemoveUnusedNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeRemoveUnusedNamespacesXslt(omitXmlDeclaration, indent),input,expected);
		testTransformerPool(XmlUtils.getRemoveUnusedNamespacesTransformerPool(omitXmlDeclaration, indent),input,expected);
	}

	public void testRemoveUnusedNamespacesXslt2(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeRemoveUnusedNamespacesXslt2(omitXmlDeclaration, indent),input,expected);
//		testTransformerPool(XmlUtils.getRemoveUnusedNamespacesXslt2TransformerPool(omitXmlDeclaration, indent),input,expected);
	}


	@Test
	public void testRemoveNamespaces() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator=System.getProperty("line.separator");
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+lineSeparator+"<root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"   <b/>"+lineSeparator+"   <c/>"+lineSeparator+"</root>",false,true);
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<root><a>a</a><b/><c/></root>",true,false);
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"   <b/>"+lineSeparator+"   <c/>"+lineSeparator+"</root>",true,true);
	}

	@Test
	public void testGetRootNamespace() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator=System.getProperty("line.separator");
		testGetRootNamespace("<root><a>a</a><b></b><c/></root>","");
		testGetRootNamespace("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>","xyz");
		testGetRootNamespace("<root xmlns:xx=\"xyz\"><a xmlns=\"xyz\">a</a><b></b><c/></root>","");
		testGetRootNamespace("<xx:root xmlns:xx=\"xyz\"><a xmlns=\"xyz\">a</a><b></b><c/></xx:root>","xyz");
	}

	@Test
	public void testAddRootNamespace() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator=System.getProperty("line.separator");
		testAddRootNamespace("xyz","<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>",false,false);
		testAddRootNamespace("xyz","<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\">"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",false,true);
		testAddRootNamespace("xyz","<root><a>a</a><b></b><c/></root>","<root xmlns=\"xyz\"><a>a</a><b/><c/></root>",true,false);
		testAddRootNamespace("xyz","<root><a>a</a><b></b><c/></root>","<root xmlns=\"xyz\">"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",true,true);
	}

	@Test
	public void testChangeRoot() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator=System.getProperty("line.separator");
		testChangeRoot("switch","<root><a>a</a></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><switch><a>a</a></switch>",false,false);
		testChangeRoot("switch","<root><a>a</a></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><switch>"+lineSeparator+"<a>a</a>"+lineSeparator+"</switch>",false,true);
		testChangeRoot("switch","<root><a>a</a></root>","<switch><a>a</a></switch>",true,false);
		testChangeRoot("switch","<root><a>a</a></root>","<switch>"+lineSeparator+"<a>a</a>"+lineSeparator+"</switch>",true,true);
	}

	@Test()
	public void testRemoveUnusedNamespaces() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator=System.getProperty("line.separator");
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",false,true);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>","<root><a>a</a><b/><c/></root>",true,false);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",true,true);

		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",false,true);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>","<root><a>a</a><b/><c/></root>",true,false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",true,true);

		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>",false,false);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\">"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",false,true);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>","<root xmlns=\"xyz\"><a>a</a><b/><c/></root>",true,false);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>","<root xmlns=\"xyz\">"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",true,true);

		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c xmlns=\"xyz\"/></root>",false,false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c xmlns=\"xyz\"/>"+lineSeparator+"</root>",false,true);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>","<root><a>a</a><b/><c xmlns=\"xyz\"/></root>",true,false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>","<root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c xmlns=\"xyz\"/>"+lineSeparator+"</root>",true,true);

	}

	@Test
	public void testMakeDetectXsltVersionXslt(){
		String s = XmlUtils.makeDetectXsltVersionXslt();
		assertEquals("<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\"><xsl:output method=\"text\"/><xsl:template match=\"/\"><xsl:value-of select=\"xsl:stylesheet/@version\"/></xsl:template></xsl:stylesheet>", s);
	}

	@Test
	public void testGetDetectXsltVersionTransformerPool() throws Exception{
		TransformerPool tp = XmlUtils.getDetectXsltVersionTransformerPool();
		testTransformerPool(tp, "<xx:root xmlns:xx=\"xyz\"><a xmlns=\"xyz\">a</a><b></b><c/></xx:root>", "");
		assertEquals("text", tp.getOutputMethod());
	}

	@Test
	public void testSkipXMLDeclaration(){
		String s = XmlUtils.skipXmlDeclaration("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>");
		assertEquals("<root xmlns=\"xyz\"><a>a</a><b/><c/></root>", s);
	}
	@Test
	public void testSkipDocTypeDeclaration(){
		String s = XmlUtils.skipDocTypeDeclaration(
				"<!DOCTYPE note SYSTEM \"Note.dtd\">\n" + "<note>\n" + "<to>Tove</to>\n" +
						"<from>Jani</from>\n" + "<heading>Reminder</heading>\n" + "<body>Don't forget me this weekend!</body>\n" +
						"</note>");
		assertEquals("<note>\n" + "<to>Tove</to>\n" + "<from>Jani</from>\n" + "<heading>Reminder</heading>\n" +
				"<body>Don't forget me this weekend!</body>\n" +
				"</note>", s);
	}

	@Test
	public void testReadXmlSkipDeclaration() throws Exception{
		String s = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>";
		byte[] arr = s.getBytes();
		String res = XmlUtils.readXml(arr, "", true);
		assertEquals("<root xmlns=\"xyz\"><a>a</a><b/><c/></root>", res);
	}

	@Test
	public void testReadXMLDefaultEncoding() throws Exception{
		String s = "<?xml version=\"1.0\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>";
		byte[] arr = s.getBytes();
		String res = XmlUtils.readXml(arr, "utf-8", true);
		String resUtf16 = XmlUtils.readXml(arr, "utf-16", true);
		assertEquals("<root xmlns=\"xyz\"><a>a</a><b/><c/></root>", res);
		assertEquals("㰿硭氠癥牳楯渽∱⸰∿㸼牯潴⁸浬湳㴢硹稢㸼愾愼⽡㸼戯㸼振㸼⽲潯琾", resUtf16);
	}

	@Test
	public void testReadXMLwithOffsetLength() throws Exception{
		String s = "<?xml version=\"1.0\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>";
		byte[] arr = s.getBytes();
		String res = XmlUtils.readXml(arr, 6, 25, "", true, false);
		assertEquals("version=\"1.0\"?><root xmln", res);

	}

	/*
	@Test
	public void testCreateTransformer() throws Exception {
		String xslt = "<xsl:stylesheet version=\"1.0\"\n" + "xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" + "\n" + "<xsl:template match=\"/\">\n" +
				"  <html>\n" + "  <body>\n" + "    <h2>My CD Collection</h2>\n" + "    <table border=\"1\">\n" + "      <tr bgcolor=\"#9acd32\">\n" +
				"        <th>Title</th>\n" + "        <th>Artist</th>\n" + "      </tr>\n" + "      <xsl:for-each select=\"catalog/cd\">\n" +
				"        <tr>\n" + "          <td><xsl:value-of select=\"title\"/></td>\n" + "          <td><xsl:value-of select=\"artist\"/></td>\n" +
				"        </tr>\n" + "      </xsl:for-each>\n" + "    </table>\n" + "  </body>\n" + "  </html>\n" + "</xsl:template>\n" +
				"\n" + "</xsl:stylesheet>";
		Transformer t = XmlUtils.createTransformer(xslt);
		assertEquals( "ads", t.getOutputProperties().stringPropertyNames().toArray()[3]);
	}*/ // will edit this case

	@Test
	public void testEncodeChars() {
		String s = "test&";
		String encoded = XmlUtils.encodeChars(s);
		String decoded = XmlUtils.decodeChars(encoded);
		assertEquals("test&amp;", encoded);
		assertEquals("test&", decoded);
	}

	@Test
	public void testEncodeUrl(){
		String a = XmlUtils.encodeURL("https://wearefrank&.nl/");
		assertEquals("https%3A%2F%2Fwearefrank%26.nl%2F", a);
	}

	@Test
	public void testIsPrintableUnicodeChar(){
		boolean a = XmlUtils.isPrintableUnicodeChar(10);
		boolean b = XmlUtils.isPrintableUnicodeChar(10001);
		boolean c = XmlUtils.isPrintableUnicodeChar(-5);

		assertEquals(true, a);
		assertEquals(true, b);
		assertEquals(false, c);
	}


}
