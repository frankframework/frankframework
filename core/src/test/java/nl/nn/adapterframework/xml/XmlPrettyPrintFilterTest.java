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
		PrettyPrintFilter filter =  new PrettyPrintFilter(xmlWriter);
		XmlUtils.parseXml(input, filter);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testTextMode() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/PrettyPrintedAsText.txt");
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setTextMode(true);
		PrettyPrintFilter filter =  new PrettyPrintFilter(xmlWriter);
		XmlUtils.parseXml(input, filter);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testNoLexicalHandling() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/PrettyPrintedNoLexicalHandler.xml");
		XmlWriter xmlWriter = new XmlWriter();

		PrettyPrintFilter filter =  new PrettyPrintFilter(xmlWriter);

		InputSource inputSource = new InputSource(new StringReader(input));
		XMLReader xmlReader = XmlUtils.getXMLReader(filter);
		// lexical handling is automatically set, when the contentHandler (filter in this case) implements  the interface LexicalHandler.
		// To test the output of the PrettyPrintFilter without lexical handling, it must be switched off explicitly in the XmlReader
		xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", null);

		xmlReader.parse(inputSource);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testSortingAttributes() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in-with-attributes.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/out-with-attributes.xml");
		XmlWriter xmlWriter = new XmlWriter();

		PrettyPrintFilter filter =  new PrettyPrintFilter(xmlWriter, true);

		InputSource inputSource = new InputSource(new StringReader(input));
		XMLReader xmlReader = XmlUtils.getXMLReader(filter);

		xmlReader.parse(inputSource);
		assertEquals(expected,xmlWriter.toString());
	}
}
