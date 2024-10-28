package org.frankframework.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.XmlUtils;

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

	@Test
	public void testSkipEmptyTagsFilterWithNamespaces1() throws Exception {
		String input =    TestFileUtils.getTestFile("/SkipEmptyTags/inWithNamespaces1.xml");
		String expected = TestFileUtils.getTestFile("/SkipEmptyTags/outWithNamespaces1.xml");
		SkipEmptyTagsFilter filter = new SkipEmptyTagsFilter(null);
		testXmlWriter(filter,input,expected);
	}

	@Test
	public void testSkipEmptyTagsFilterWithNamespaces2() throws Exception {
		String input =    TestFileUtils.getTestFile("/SkipEmptyTags/inWithNamespaces2.xml");
		String expected = TestFileUtils.getTestFile("/SkipEmptyTags/outWithNamespaces2.xml");
		SkipEmptyTagsFilter filter = new SkipEmptyTagsFilter(null);
		testXmlWriter(filter,input,expected);
	}
}
