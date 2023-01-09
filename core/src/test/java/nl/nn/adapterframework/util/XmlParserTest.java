package nl.nn.adapterframework.util;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.xml.XmlWriter;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class XmlParserTest {

	protected String getInputAsString(String file) throws IOException {
		String input    = TestFileUtils.getTestFile(file);
		URL entity      = TestFileUtils.getTestFileURL("/XmlUtils/EntityResolution/in-plain.xml");
		input=input.replaceAll("\\[ENTITY_URL\\]", entity.toExternalForm());
		System.out.println("input ["+input+"]");
		return input;
	}

	@Test
	public void testParseXmlNoExternalEntityInjection() throws IOException, SAXException {
		String input    = getInputAsString("/XmlUtils/EntityResolution/in-file-entity.xml");
		String expected = TestFileUtils.getTestFile("/XmlUtils/EntityResolution/out-not-resolved.xml");

		XmlWriter writer = new XmlWriter();
		XmlUtils.parseXml(input, writer);
		
		MatchUtils.assertXmlEquals(expected,writer.toString());		
	}

	@Test
	public void testParseXmlNoRelativeEntityInjection() throws IOException, SAXException {
		URL    input    = TestFileUtils.getTestFileURL("/XmlUtils/EntityResolution/in-relative-entity.xml");
		String expected = TestFileUtils.getTestFile("/XmlUtils/EntityResolution/out-not-resolved.xml");

		XmlWriter writer = new XmlWriter();
		InputSource source = Message.asInputSource(input);
		
		XmlUtils.parseXml(source, writer);
		
		MatchUtils.assertXmlEquals(expected,writer.toString());
	}

	@Test
	public void testParseXmlResourceWithRelativeEntityInjection() throws IOException, SAXException, ParserConfigurationException {
		Resource input  = Resource.getResource("/XmlUtils/EntityResolution/in-relative-entity.xml");
		String expected = TestFileUtils.getTestFile("/XmlUtils/EntityResolution/out-resolved.xml");

		XmlWriter writer = new XmlWriter();
		XmlUtils.parseXml(input, writer);

		MatchUtils.assertXmlEquals(expected,writer.toString());
	}

	@Test
	@Disabled 	//("requires proper setup on the classpath filesystem. In order for this test to pass properly, the file referenced by the external entity in the input file must exist on the file system. "+
				//"I currently consider it too much of a hassle to automate this setup in a way that works for both Windows and Linux")
	public void testParseXmlResourceWithExternalEntityInjection() throws IOException, SAXException, ParserConfigurationException {
		Resource input  = Resource.getResource("/XmlUtils/EntityResolution/in-file-entity-c-temp.xml");
		String expected = TestFileUtils.getTestFile("/XmlUtils/EntityResolution/out-resolved.xml");
		
		XmlWriter writer = new XmlWriter();
		XmlUtils.parseXml(input, writer);
		
		MatchUtils.assertXmlEquals(expected,writer.toString());
	}
}
