package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.XmlWriter;
import nl.nn.adapterframework.util.XmlUtils;

public class TargetElementFilterTest {

	private String document="<root><sub1><a>x</a></sub1><sub2><a>y</a><b>y</b></sub2><xx/><a>x</a></root>";
	
	public void testTargetElementFilter(String targetElement, boolean includeRoot, String input, String expected) throws IOException, SAXException {
		TargetElementFilter targetElementFilter = new TargetElementFilter(targetElement,includeRoot);
		XmlWriter writer = new XmlWriter();
		targetElementFilter.setContentHandler(writer);
		XmlUtils.parseXml(targetElementFilter, input);
		assertEquals("testContainerElementFilter ["+targetElement+"]",expected,writer.toString());
	}
	
	@Test
	public void testBasic() throws Exception {
		testTargetElementFilter("a",false, document,"<a>x</a><a>y</a><a>x</a>");
	}
	
	@Test
	public void testWithRoot() throws Exception {
		testTargetElementFilter("a",true, document,"<root><a>x</a><a>y</a><a>x</a></root>");
	}
}
