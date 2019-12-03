package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class SkipEmptyTagsFilterTest {

	
	public void testXmlWriter(XMLFilterImpl filter, String input, String expected) throws IOException, SAXException {
		XmlWriter xmlWriter = new XmlWriter() {

			@Override
			public void startPrefixMapping(String prefix, String uri) throws SAXException {
				AttributesImpl attributes = new AttributesImpl();
				attributes.addAttribute("", "uri", "uri", "string", uri);
				startElement("", "prefix-"+prefix, "prefix-"+prefix, attributes);
				super.startPrefixMapping(prefix, uri);
			}

			@Override
			public void endPrefixMapping(String prefix) throws SAXException {
				super.endPrefixMapping(prefix);
				endElement("", "prefix-"+prefix, "prefix-"+prefix);
			}
			
		};
		xmlWriter.setIncludeComments(true);
		PrettyPrintFilter ppf = new PrettyPrintFilter();
		ppf.setIndent("");
		ppf.setContentHandler(xmlWriter);
		filter.setContentHandler(ppf);
		XmlUtils.parseXml(filter, input);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testSkipEmptyTagsFilter() throws Exception {
		String input =    TestFileUtils.getTestFile("/SkipEmptyTags/in.xml");
		String expected = TestFileUtils.getTestFile("/SkipEmptyTags/SkipEmptyTagsTestOut.xml");
		SkipEmptyTagsFilter filter = new SkipEmptyTagsFilter();
		testXmlWriter(filter,input,expected);
	}
	
}
