package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class SkipEmptyTagsFilterTest {

	
	public void testXmlWriter(XMLFilterImpl filter, String input, String expected) throws IOException, SAXException {
		XmlWriter xmlWriter = new XmlWriter();
		PrettyPrintFilter ppf = new PrefixMappingObservingFilter(xmlWriter);
		ppf.setIndent("");
		filter.setContentHandler(ppf);
		XmlUtils.parseXml(input, filter);
		assertEquals(expected,xmlWriter.toString());
	}

	@Test
	public void testSkipEmptyTagsFilter() throws Exception {
		String input =    TestFileUtils.getTestFile("/SkipEmptyTags/in.xml");
		String expected = TestFileUtils.getTestFile("/SkipEmptyTags/SkipEmptyTagsTestOut.xml");
		SkipEmptyTagsFilter filter = new SkipEmptyTagsFilter(null);
		testXmlWriter(filter,input,expected);
	}
	
}
