package org.frankframework.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.XmlUtils;

public class NodeSetFilterTest {

	private String document;

	public void testTargetElementFilter(String targetElement, boolean includeRoot, String input, String expected) throws IOException, SAXException {
		NodeSetFilter targetElementFilter = new NodeSetFilter(null, targetElement, true, includeRoot, null);
		testNodeSetFilter(targetElementFilter, targetElement, includeRoot, input, expected);
	}

	public void testContainerElementFilter(String containerElement, boolean includeRoot, String input, String expected) throws IOException, SAXException {
		NodeSetFilter containerElementFilter = new NodeSetFilter(null, containerElement, false, includeRoot, null);
		testNodeSetFilter(containerElementFilter, containerElement, includeRoot, input, expected);
	}

	public void testNodeSetFilter(NodeSetFilter targetElementFilter, String element, boolean includeRoot, String input, String expected) throws IOException, SAXException {
		XmlWriter xmlWriter = new XmlWriter();
		PrettyPrintFilter ppf = new PrefixMappingObservingFilter(xmlWriter);
		targetElementFilter.setContentHandler(ppf);
		XmlUtils.parseXml(input, targetElementFilter);
		assertEquals(expected, xmlWriter.toString(), "testElementFilter "+(includeRoot?"container element":"target element")+" ["+element+"]");
	}

	@BeforeEach
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
		String expected="""
							<prefix- uri="urn:tja">
								<root xmlns="urn:tja">
									<a>x</a>
									<a>y</a>
									<a>y</a>
								</root>
							</prefix->\
							""";
		testTargetElementFilter("a", true, document, expected);
	}

	@Test
	public void testContainerElementFilter() throws Exception {
		String expected="""
							<prefix- uri="urn:tja">
								<a xmlns="urn:tja">y</a>
							</prefix->
							<prefix- uri="urn:tja">
								<b xmlns="urn:tja">y</b>
							</prefix->
							<prefix- uri="urn:tja">
								<a xmlns="urn:tja">y</a>
							</prefix->
							<prefix- uri="urn:tja">
								<b xmlns="urn:tja">y</b>
							</prefix->\
							""";
		testContainerElementFilter("sub2", false, document, expected);
	}

	@Test
	public void testContainerElementFilterWithRoot() throws Exception {
		String expected="""
							<prefix- uri="urn:tja">
								<root xmlns="urn:tja">
									<a>y</a>
									<b>y</b>
									<a>y</a>
									<b>y</b>
								</root>
							</prefix->\
							""";
		testContainerElementFilter("sub2",true, document, expected);
	}

	@Test
	public void testNodeSetFilter() throws Exception {
		NodeSetFilter targetElementFilter = new NodeSetFilter(null, null, false, false, null);
		String expected="""
				<prefix- uri="urn:tja">
					<sub1 xmlns="urn:tja">
						<a>x</a>
					</sub1>
				</prefix->
				<prefix- uri="urn:tja">
					<x xmlns="urn:tja">
						<sub2>
							<a>y</a>
							<b>y</b>
						</sub2>
					</x>
				</prefix->
				<prefix- uri="urn:tja">
					<sub2 xmlns="urn:tja">
						<a>y</a>
						<b>y</b>
					</sub2>
				</prefix->
				<prefix- uri="urn:tja">
					<xx xmlns="urn:tja"/>
				</prefix->\
				""";
		testNodeSetFilter(targetElementFilter, "a", false, document, expected);
	}

	@Test
	public void testTargetElementFilterWithNamespace() throws Exception {
		NodeSetFilter targetElementFilter = new NodeSetFilter(XmlUtils.getNamespaceMap("urn:tja"), "a",true,false, null);
		String expected="""
				<prefix- uri="urn:tja">
					<a xmlns="urn:tja">x</a>
				</prefix->
				<prefix- uri="urn:tja">
					<a xmlns="urn:tja">y</a>
				</prefix->
				<prefix- uri="urn:tja">
					<a xmlns="urn:tja">y</a>
				</prefix->\
				""";
		testNodeSetFilter(targetElementFilter, "a", false, document, expected);
	}

	@Test
	public void testTargetElementFilterWithNamespace2() throws Exception {
		NodeSetFilter targetElementFilter = new NodeSetFilter(XmlUtils.getNamespaceMap("urn:tja"), "a",true,false, null);

		String document="<root xmlns:ns=\"urn:tja\"><sub1><a>x</a></sub1><x><sub2><ns:a>y</ns:a><b>y</b></sub2></x><sub2><a>y</a><b>y</b></sub2><xx/></root>";
		String expected = 	"""
								<prefix-ns uri="urn:tja">
									<ns:a xmlns:ns="urn:tja">y</ns:a>
								</prefix-ns>\
								""";

		testNodeSetFilter(targetElementFilter, "a", false, document,expected);
	}

	@Test
	public void testTargetElementFilterWithNamespace3() throws Exception {
		NodeSetFilter targetElementFilter = new NodeSetFilter(XmlUtils.getNamespaceMap("x=urn:tja"), "x:a",true,false, null);

		String document="<root xmlns:ns=\"urn:tja\"><sub1><a>x</a></sub1><x><sub2><ns:a>y</ns:a><b>y</b></sub2></x><sub2><a>y</a><b>y</b></sub2><xx/></root>";
		String expected =	"""
								<prefix-ns uri="urn:tja">
									<ns:a xmlns:ns="urn:tja">y</ns:a>
								</prefix-ns>\
								""";

		testNodeSetFilter(targetElementFilter, "a", false, document,expected);
	}

	@Test
	public void testTargetElementMultiNamespace() throws Exception {
		String namespaceDefs="urn:defns2";
		String targetElement="XDOC";

		NodeSetFilter nodeSetFilter = new NodeSetFilter(XmlUtils.getNamespaceMap(namespaceDefs), targetElement, true, false, null) {

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

		NodeSetFilter nodeSetFilter = new NodeSetFilter(XmlUtils.getNamespaceMap(namespaceDefs), containerElement, false, false, null) {

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
