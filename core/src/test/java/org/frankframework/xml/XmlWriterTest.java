package org.frankframework.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.XmlUtils;

public class XmlWriterTest {

	@Test
	public void testBasic() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = input;
		XmlWriter xmlWriter = new XmlWriter();
		XmlUtils.parseXml(input, xmlWriter);
		assertEquals(expected, xmlWriter.toString());
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
	public void testWithNullAttribute() throws Exception {
		XmlWriter xmlWriter = new XmlWriter();

		//String expected = "<document attr=\"null\"/>";
		String expected = "<document/>";

		try (SaxDocumentBuilder documentBuilder = new SaxDocumentBuilder("document", xmlWriter, false)) {
			documentBuilder.addAttribute("attr", null);
		}

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
	public void testBasicCheckClosedDefault() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = input;
		CloseObservableWriter writer = new CloseObservableWriter();
		XmlWriter xmlWriter = new XmlWriter(writer);
		XmlUtils.parseXml(input, xmlWriter);
		assertEquals(expected,writer.toString());
		assertFalse(writer.closeCalled);
	}

	@Test
	public void testBasicCheckClosed() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = input;
		CloseObservableWriter writer = new CloseObservableWriter();
		XmlWriter xmlWriter = new XmlWriter(writer, true);
		XmlUtils.parseXml(input, xmlWriter);
		assertEquals(expected,writer.toString());
		assertTrue(writer.closeCalled);
	}

	@Test
	public void testBasicCheckNotClosed() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = input;
		CloseObservableWriter writer = new CloseObservableWriter();
		XmlWriter xmlWriter = new XmlWriter(writer, false);
		XmlUtils.parseXml(input, xmlWriter);
		assertEquals(expected,writer.toString());
		assertFalse(writer.closeCalled);
	}

	@Test
	public void testMultinamespace() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/MultiNamespace/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/MultiNamespace/out.xml");
		CloseObservableWriter writer = new CloseObservableWriter();
		XmlWriter xmlWriter = new XmlWriter(writer, false);
		XmlUtils.parseXml(input, xmlWriter);
		assertEquals(expected,writer.toString());
		assertFalse(writer.closeCalled);
	}

	private class CloseObservableWriter extends StringWriter {
		public boolean closeCalled;

		@Override
		public void close() throws IOException {
			closeCalled = true;
			super.close();
		}
	}
}
