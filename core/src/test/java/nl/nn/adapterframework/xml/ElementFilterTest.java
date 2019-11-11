package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.XmlWriter;
import nl.nn.adapterframework.util.XmlUtils;

public class ElementFilterTest {

	private String document="<root xmlns=\"urn:tja\"><sub1><a>x</a></sub1><x><sub2><a>y</a><b>y</b></sub2></x><sub2><a>y</a><b>y</b></sub2><xx/></root>";
	
	public void testTargetElementFilter(String targetElement, boolean includeRoot, String input, String expected) throws IOException, SAXException {
		ElementFilter targetElementFilter = new ElementFilter(null, targetElement,true,includeRoot);
		testElementFilter(targetElementFilter, targetElement, includeRoot, input, expected);
	}

	public void testContainerElementFilter(String containerElement, boolean includeRoot, String input, String expected) throws IOException, SAXException {
		ElementFilter containerElementFilter = new ElementFilter(null, containerElement,false,includeRoot);
		testElementFilter(containerElementFilter, containerElement, includeRoot, input, expected);
	}
	
	public void testElementFilter(ElementFilter targetElementFilter, String targetElement, boolean includeRoot, String input, String expected) throws IOException, SAXException {
		XmlWriter writer = new XmlWriter();
		targetElementFilter.setContentHandler(writer);
		XmlUtils.parseXml(targetElementFilter, input);
		assertEquals("testElementFilter ["+targetElement+"]",expected,writer.toString());
	}

	@Test
	public void testTargetElementFilter() throws Exception {
		testTargetElementFilter("a",false, document,"<a>x</a><a>y</a><a>y</a>");
	}
	
	@Test
	public void testTargetElementFilterWithRoot() throws Exception {
		testTargetElementFilter("a",true, document,"<root><a>x</a><a>y</a><a>y</a></root>");
	}
	
	@Test
	public void testContainerElementFilter() throws Exception {
		testContainerElementFilter("sub2",false, document,"<a>y</a><b>y</b><a>y</a><b>y</b>");
	}

	@Test
	public void testContainerElementFilterWithRoot() throws Exception {
		testContainerElementFilter("sub2",true, document,"<root><a>y</a><b>y</b><a>y</a><b>y</b></root>");
	}

	@Test
	public void testTargetElementFilterWithNamespace() throws Exception {
		ElementFilter targetElementFilter = new ElementFilter(XmlUtils.getNamespaceMap("urn:tja"), "a",true,false);
		testElementFilter(targetElementFilter, "a", false, document,"<a>x</a><a>y</a><a>y</a>");
	}
	
	@Test
	public void testTargetElementFilterWithNamespace2() throws Exception {
		String document="<root xmlns:ns=\"urn:tja\"><sub1><a>x</a></sub1><x><sub2><ns:a>y</ns:a><b>y</b></sub2></x><sub2><a>y</a><b>y</b></sub2><xx/></root>";
		ElementFilter targetElementFilter = new ElementFilter(XmlUtils.getNamespaceMap("urn:tja"), "a",true,false);
		testElementFilter(targetElementFilter, "a", false, document,"<ns:a>y</ns:a>");
	}
	

}
