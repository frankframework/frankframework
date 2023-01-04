package nl.nn.adapterframework.stream.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;

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
