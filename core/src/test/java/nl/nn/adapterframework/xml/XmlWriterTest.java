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
		XmlUtils.parseXml(xmlWriter, input);
		assertEquals(expected,xmlWriter.toString());
	}
	
	@Test
	public void testWithXmlDeclaration() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/WithXmlDeclaration.xml");
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setIncludeXmlDeclaration(true);
		xmlWriter.setNewlineAfterXmlDeclaration(true);
		XmlUtils.parseXml(xmlWriter, input);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testTextMode() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/TextMode.xml");
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setTextMode(true);
		XmlUtils.parseXml(xmlWriter, input);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testNoLexicalHandling() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/Escaped.xml");
		XmlWriter xmlWriter = new XmlWriter();
		
		InputSource inputSource = new InputSource(new StringReader(input));
		XMLReader xmlReader = XmlUtils.getXMLReader(true, true);
		xmlReader.setContentHandler(xmlWriter);

		xmlReader.parse(inputSource);
		assertEquals(expected,xmlWriter.toString());
	}

}
