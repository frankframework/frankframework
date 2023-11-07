package nl.nn.adapterframework.configuration.digester;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;

public class IncludeFilterTest {

	@Test
	public void testMultiInclude() throws Exception {
		Resource root = Resource.getResource("/IncludeFilter/multiOk-in.xml");
		String expected = TestFileUtils.getTestFile("/IncludeFilter/multiOk-out.xml");

		XmlWriter writer = new XmlWriter();
		IncludeFilter filter = new IncludeFilter(writer, root);

		XmlUtils.parseXml(root, filter);

		assertEquals(expected, writer.toString());
	}

	@Test
	public void testMultiIncludeError() throws Exception {
		Resource root = Resource.getResource("/IncludeFilter/multiError-in.xml");
		String expected = TestFileUtils.getTestFile("/IncludeFilter/multiError-out.xml");

		XmlWriter writer = new XmlWriter();
		IncludeFilter filter = new IncludeFilter(writer, root);

		SAXParseException e = assertThrows(SAXParseException.class, ()->XmlUtils.parseXml(root, filter));

		assertThat(e.getSystemId(), containsString("ErrA.xml"));
		assertEquals(expected, writer.toString().trim());
	}
}
