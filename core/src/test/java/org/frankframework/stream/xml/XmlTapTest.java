package org.frankframework.stream.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.documentbuilder.xml.XmlTap;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.XmlWriter;

public class XmlTapTest {

	@Test
	public void testBasic() throws Exception {
		String input    = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = input;
		XmlWriter xmlWriter = new XmlWriter();
		XmlTap xmlTap = new XmlTap(xmlWriter);
		XmlUtils.parseXml(input, xmlTap);
		assertEquals(expected,xmlWriter.toString());
		assertEquals(expected,xmlTap.getWriter().toString());
	}
}
