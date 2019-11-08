package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.XmlWriter;
import nl.nn.adapterframework.util.XmlUtils;

public class ContainerElementFilterTest {

	private String document="<root><sub1><a>x</a></sub1><x><sub2><a>y</a><b>y</b></sub2></x><sub2><a>y</a><b>y</b></sub2><xx/></root>";
	
	public void testContainerElementFilter(String containerElement, String input, String expected) throws IOException, SAXException {
		ContainerElementFilter containerElementFilter = new ContainerElementFilter(containerElement);
		XmlWriter writer = new XmlWriter();
		containerElementFilter.setContentHandler(writer);
		XmlUtils.parseXml(containerElementFilter, input);
		assertEquals("testContainerElementFilter ["+containerElement+"]",expected,writer.toString());
	}
	
	@Test
	public void testBasic() throws Exception {
		testContainerElementFilter("sub2",document,"<sub2><a>y</a><b>y</b></sub2><sub2><a>y</a><b>y</b></sub2>");
	}
	
//	@Test
//	public void testWithRoot() throws Exception {
//		testContainerElementFilter("sub2",true,document,"<root><sub2><a>y</a><b>y</b></sub2></root><");
//	}
}
