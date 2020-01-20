package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class NodeSetFilterTest {

	private String document;
	
	public void testTargetElementFilter(String targetElement, boolean includeRoot, String input, String expected) throws IOException, SAXException {
		NodeSetFilter targetElementFilter = new NodeSetFilter(null, targetElement,true,includeRoot);
		testNodeSetFilter(targetElementFilter, targetElement, includeRoot, input, expected);
	}

	public void testContainerElementFilter(String containerElement, boolean includeRoot, String input, String expected) throws IOException, SAXException {
		NodeSetFilter containerElementFilter = new NodeSetFilter(null, containerElement,false,includeRoot);
		testNodeSetFilter(containerElementFilter, containerElement, includeRoot, input, expected);
	}
	
	public void testNodeSetFilter(NodeSetFilter targetElementFilter, String element, boolean includeRoot, String input, String expected) throws IOException, SAXException {
		XmlWriter xmlWriter = new XmlWriter();
		PrettyPrintFilter ppf = new PrefixMappingObservingFilter();
		ppf.setContentHandler(xmlWriter);
		targetElementFilter.setContentHandler(ppf);
		XmlUtils.parseXml(targetElementFilter, input);
		assertEquals("testElementFilter "+(includeRoot?"container element":"target element")+" ["+element+"]",expected,xmlWriter.toString());
	}

	@Before
	public void setUp() throws IOException {
		document = TestFileUtils.getTestFile("/NodeSetFilter/document.xml");
	}
	
	@Test
	public void testTargetElementFilter() throws Exception {
		String expected = TestFileUtils.getTestFile("/NodeSetFilter/targetElementResult.txt");
		//System.out.println(expected);
		testTargetElementFilter("a", false, document, expected);
	}

	@Test
	public void testNamespaceMapping() throws Exception {
		String input = TestFileUtils.getTestFile("/NodeSetFilter/simpleIn.xml");
		String expected = TestFileUtils.getTestFile("/NodeSetFilter/simpleOut.txt");
		//System.out.println(expected);
		testTargetElementFilter(null, false, input, expected);
	}

	@Test
	public void testTargetElementFilterWithRoot() throws Exception {
		String expected="<prefix- uri=\"urn:tja\">\n" +
							"\t<root xmlns=\"urn:tja\">\n" + 
								"\t\t<a>x</a>\n" +
								"\t\t<a>y</a>\n" +
								"\t\t<a>y</a>\n" +
							"\t</root>\n" + 
						"</prefix->";
		testTargetElementFilter("a", true, document, expected);
	}
	
	@Test
	public void testContainerElementFilter() throws Exception {
		String expected="<prefix- uri=\"urn:tja\">\n" +
							"\t<a xmlns=\"urn:tja\">y</a>\n" +
						"</prefix->\n" +
						"<prefix- uri=\"urn:tja\">\n" +
							"\t<b xmlns=\"urn:tja\">y</b>\n" +
						"</prefix->\n" + 
						"<prefix- uri=\"urn:tja\">\n" +
							"\t<a xmlns=\"urn:tja\">y</a>\n" +
						"</prefix->\n" +
						"<prefix- uri=\"urn:tja\">\n" +
							"\t<b xmlns=\"urn:tja\">y</b>\n" +
						"</prefix->";
		testContainerElementFilter("sub2", false, document, expected);
	}

	@Test
	public void testContainerElementFilterWithRoot() throws Exception {
		String expected="<prefix- uri=\"urn:tja\">\n" +
							"\t<root xmlns=\"urn:tja\">\n" +
							"\t\t<a>y</a>\n" +
							"\t\t<b>y</b>\n" +
							"\t\t<a>y</a>\n" +
							"\t\t<b>y</b>\n" +
							"\t</root>\n" +
						"</prefix->";
		testContainerElementFilter("sub2",true, document, expected);
	}

	@Test
	public void testNodeSetFilter() throws Exception {
		NodeSetFilter targetElementFilter = new NodeSetFilter(null, null, false, false);
		String expected="<prefix- uri=\"urn:tja\">\n" +
				"\t<sub1 xmlns=\"urn:tja\">\n" +
					"\t\t<a>x</a>\n" +
				"\t</sub1>\n" +
				"</prefix->\n" +
				"<prefix- uri=\"urn:tja\">\n" +
				"\t<x xmlns=\"urn:tja\">\n" +
				"\t\t<sub2>\n" +
				"\t\t\t<a>y</a>\n" +
				"\t\t\t<b>y</b>\n" +
				"\t\t</sub2>\n" +
				"\t</x>\n" +
				"</prefix->\n" +
				"<prefix- uri=\"urn:tja\">\n" +
				"\t<sub2 xmlns=\"urn:tja\">\n" +
				"\t\t<a>y</a>\n" +
				"\t\t<b>y</b>\n" +
				"\t</sub2>\n" +
				"</prefix->\n" +
				"<prefix- uri=\"urn:tja\">\n" +
				"\t<xx xmlns=\"urn:tja\"/>\n" +
				"</prefix->";
		testNodeSetFilter(targetElementFilter, "a", false, document, expected);
	}

	@Test
	public void testTargetElementFilterWithNamespace() throws Exception {
		NodeSetFilter targetElementFilter = new NodeSetFilter(XmlUtils.getNamespaceMap("urn:tja"), "a",true,false);
		String expected="<prefix- uri=\"urn:tja\">\n" +
				"\t<a xmlns=\"urn:tja\">x</a>\n" +
			"</prefix->\n" +
			"<prefix- uri=\"urn:tja\">\n" +
				"\t<a xmlns=\"urn:tja\">y</a>\n" +
			"</prefix->\n" + 
			"<prefix- uri=\"urn:tja\">\n" +
				"\t<a xmlns=\"urn:tja\">y</a>\n" +
			"</prefix->";
		testNodeSetFilter(targetElementFilter, "a", false, document, expected);
	}
	
	@Test
	public void testTargetElementFilterWithNamespace2() throws Exception {
		NodeSetFilter targetElementFilter = new NodeSetFilter(XmlUtils.getNamespaceMap("urn:tja"), "a",true,false);

		String document="<root xmlns:ns=\"urn:tja\"><sub1><a>x</a></sub1><x><sub2><ns:a>y</ns:a><b>y</b></sub2></x><sub2><a>y</a><b>y</b></sub2><xx/></root>";
		String expected = 	"<prefix-ns uri=\"urn:tja\">\n" +
								"\t<ns:a xmlns:ns=\"urn:tja\">y</ns:a>\n" +
							"</prefix-ns>";

		testNodeSetFilter(targetElementFilter, "a", false, document,expected);
	}
	
	@Test
	public void testTargetElementFilterWithNamespace3() throws Exception {
		NodeSetFilter targetElementFilter = new NodeSetFilter(XmlUtils.getNamespaceMap("x=urn:tja"), "x:a",true,false);

		String document="<root xmlns:ns=\"urn:tja\"><sub1><a>x</a></sub1><x><sub2><ns:a>y</ns:a><b>y</b></sub2></x><sub2><a>y</a><b>y</b></sub2><xx/></root>";
		String expected =	"<prefix-ns uri=\"urn:tja\">\n" +
								"\t<ns:a xmlns:ns=\"urn:tja\">y</ns:a>\n" +
							"</prefix-ns>";
		
		testNodeSetFilter(targetElementFilter, "a", false, document,expected);
	}
	
	@Test
	public void testTargetElementMultiNamespace() throws Exception {
		String namespaceDefs="urn:defns2";
		String targetElement="XDOC";
		 
		NodeSetFilter nodeSetFilter = new NodeSetFilter(XmlUtils.getNamespaceMap(namespaceDefs), targetElement, true, false) {

			@Override
			public void startNode(String uri, String localName, String qName) throws SAXException {
				String msg = "before node ["+qName+"]";
				comment(msg.toCharArray(), 0, msg.length());
			}

			@Override
			public void endNode(String uri, String localName, String qName) throws SAXException {
				String msg = "after node ["+qName+"]";
				comment(msg.toCharArray(), 0, msg.length());
			}
			
			
		};

		String document=TestFileUtils.getTestFile("/NodeSetFilter/NoDuplicateNamespaces/xdocs.xml");
		String expected=TestFileUtils.getTestFile("/NodeSetFilter/NoDuplicateNamespaces/out.txt");
				
		testNodeSetFilter(nodeSetFilter, targetElement, false, document, expected);
	}
	
	@Test
	public void testContainerMultiNamespace() throws Exception {
		String namespaceDefs="urn:defcont";
		String containerElement="DocOutputPrintDocumentRequestCont";
		 
		NodeSetFilter nodeSetFilter = new NodeSetFilter(XmlUtils.getNamespaceMap(namespaceDefs), containerElement, false, false) {

			@Override
			public void startNode(String uri, String localName, String qName) throws SAXException {
				String msg = "before node ["+qName+"]";
				comment(msg.toCharArray(), 0, msg.length());
			}

			@Override
			public void endNode(String uri, String localName, String qName) throws SAXException {
				String msg = "after node ["+qName+"]";
				comment(msg.toCharArray(), 0, msg.length());
			}
			
			
		};

		String document=TestFileUtils.getTestFile("/NodeSetFilter/NoDuplicateNamespaces/xdocs.xml");
		String expected=TestFileUtils.getTestFile("/NodeSetFilter/NoDuplicateNamespaces/out.txt");
				
		testNodeSetFilter(nodeSetFilter, containerElement, false, document, expected);
	}

}
