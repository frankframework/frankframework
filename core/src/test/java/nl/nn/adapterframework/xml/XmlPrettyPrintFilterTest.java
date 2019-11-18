package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class XmlPrettyPrintFilterTest {

	
	@Test
	public void testBasic() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/PrettyPrinted.xml");
		XmlWriter xmlWriter = new XmlWriter();
		PrettyPrintFilter filter =  new PrettyPrintFilter();
		filter.setContentHandler(xmlWriter);
		XmlUtils.parseXml(filter, input);
		assertEquals(expected,xmlWriter.toString());
	}
	
	@Test
	public void testTextMode() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/PrettyPrintedTextMode.xml");
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setTextMode(true);
		PrettyPrintFilter filter =  new PrettyPrintFilter();
		filter.setContentHandler(xmlWriter);
		XmlUtils.parseXml(filter, input);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testNoLexicalHandling() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/PrettyPrintedEscaped.xml");
		XmlWriter xmlWriter = new XmlWriter();
		
		xmlWriter.setIncludeXmlDeclaration(true);
		xmlWriter.setNewlineAfterXmlDeclaration(true);
		
		PrettyPrintFilter filter =  new PrettyPrintFilter();
		filter.setContentHandler(xmlWriter);

		InputSource inputSource = new InputSource(new StringReader(input));
		XMLReader xmlReader = XmlUtils.getXMLReader(true, true);
		xmlReader.setContentHandler(filter);

		xmlReader.parse(inputSource);
		assertEquals(expected,xmlWriter.toString());
	}

}
