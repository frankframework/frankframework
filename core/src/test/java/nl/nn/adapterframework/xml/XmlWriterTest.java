package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class XmlWriterTest {

	
	@Test
	public void testBasic() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = input;
		XmlWriter xmlWriter = new XmlWriter();
		XmlUtils.parseXml(input, xmlWriter);
		assertEquals(expected,xmlWriter.toString());
	}
	
	@Test
	public void testWithXmlDeclaration() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/WithXmlDeclaration.xml");
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setIncludeXmlDeclaration(true);
		xmlWriter.setNewlineAfterXmlDeclaration(true);
		XmlUtils.parseXml(input, xmlWriter);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testTextMode() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/AsTextPlain.txt");
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setTextMode(true);
		XmlUtils.parseXml(input, xmlWriter);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testIncludeComments() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = input;
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setIncludeComments(true);
		XmlUtils.parseXml(input, xmlWriter);
		assertEquals(expected,xmlWriter.toString());
	}
	

	@Test
	public void testNoLexicalHandling() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/NoComments.xml");
		XmlWriter xmlWriter = new XmlWriter();
		
		InputSource inputSource = new InputSource(new StringReader(input));
		XMLReader xmlReader = XmlUtils.getXMLReader(xmlWriter);
		// lexical handling is automatically set, when the contentHandler (xmlWriter in this case) implements  the interface LexicalHandler.
		// To test the output of the XmlWriter without lexical handling, it must be switched off explicitly in the XmlReader
		xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", null);

		xmlReader.parse(inputSource);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testDisableOutputEscaping() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/ConfigNewLine.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/ParsedXmlNewLine.xml");
		XmlWriter xmlWriter = new XmlWriter();
		
		InputSource inputSource = new InputSource(new StringReader(input));
		XMLReader xmlReader = XmlUtils.getXMLReader(xmlWriter);
		// lexical handling is automatically set, when the contentHandler (xmlWriter in this case) implements  the interface LexicalHandler.
		// To test the output of the XmlWriter without lexical handling, it must be switched off explicitly in the XmlReader
		xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", null);

		xmlReader.parse(inputSource);
		assertEquals(expected,xmlWriter.toString());
	}

}
