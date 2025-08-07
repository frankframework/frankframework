package org.frankframework.util;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.configuration.ConfigurationException;

public class UtilityTransformerPoolsTest extends FunctionalTransformerPoolTestBase {

	void testGetRootNamespace(String input, String expected) throws SAXException, TransformerException, IOException, ConfigurationException {
		testTransformerPool(UtilityTransformerPools.getGetRootNamespaceTransformerPool(), input, expected);
	}

	void testAddRootNamespace(String namespace, String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testTransformerPool(UtilityTransformerPools.getAddRootNamespaceTransformerPool(namespace, omitXmlDeclaration, indent), input, expected);
	}

	void testChangeRoot(String root, String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testTransformerPool(UtilityTransformerPools.getChangeRootTransformerPool(root, omitXmlDeclaration, indent), input, expected);
	}

	void testRemoveUnusedNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testTransformerPool(UtilityTransformerPools.getRemoveUnusedNamespacesTransformerPool(omitXmlDeclaration, indent), input, expected);
	}

	// TODO: Why is this test not executed anymore?
	void testRemoveUnusedNamespacesXslt2(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testTransformerPool(UtilityTransformerPools.getRemoveUnusedNamespacesXslt2TransformerPool(omitXmlDeclaration, indent),input,expected);
	}

	@Test
	void testGetRootNamespace() throws SAXException, TransformerException, IOException, ConfigurationException {
		testGetRootNamespace("<root><a>a</a><b></b><c/></root>", "");
		testGetRootNamespace("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>", "xyz");
		testGetRootNamespace("<root xmlns:xx=\"xyz\"><a xmlns=\"xyz\">a</a><b></b><c/></root>", "");
		testGetRootNamespace("<xx:root xmlns:xx=\"xyz\"><a xmlns=\"xyz\">a</a><b></b><c/></xx:root>", "xyz");
	}

	@Test
	void testAddRootNamespace() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator = System.getProperty("line.separator");
		testAddRootNamespace("xyz", "<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>", false, false);
		testAddRootNamespace("xyz", "<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\">" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", false, true);
		testAddRootNamespace("xyz", "<root><a>a</a><b></b><c/></root>", "<root xmlns=\"xyz\"><a>a</a><b/><c/></root>", true, false);
		testAddRootNamespace("xyz", "<root><a>a</a><b></b><c/></root>", "<root xmlns=\"xyz\">" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", true, true);
	}

	@Test
	void testChangeRoot() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator = System.getProperty("line.separator");
		testChangeRoot("switch", "<root><a>a</a></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><switch><a>a</a></switch>", false, false);
		testChangeRoot("switch", "<root><a>a</a></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><switch>" + lineSeparator + "<a>a</a>" + lineSeparator + "</switch>", false, true);
		testChangeRoot("switch", "<root><a>a</a></root>", "<switch><a>a</a></switch>", true, false);
		testChangeRoot("switch", "<root><a>a</a></root>", "<switch>" + lineSeparator + "<a>a</a>" + lineSeparator + "</switch>", true, true);
	}

	@Test()
	void testRemoveUnusedNamespaces() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator = System.getProperty("line.separator");
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>", false, false);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", false, true);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>", "<root><a>a</a><b/><c/></root>", true, false);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>", "<root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", true, true);

		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>", false, false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", false, true);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>", "<root><a>a</a><b/><c/></root>", true, false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>", "<root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", true, true);

		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>", false, false);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\">" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", false, true);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>", "<root xmlns=\"xyz\"><a>a</a><b/><c/></root>", true, false);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>", "<root xmlns=\"xyz\">" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c/>" + lineSeparator + "</root>", true, true);

		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c xmlns=\"xyz\"/></root>", false, false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c xmlns=\"xyz\"/>" + lineSeparator + "</root>", false, true);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>", "<root><a>a</a><b/><c xmlns=\"xyz\"/></root>", true, false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>", "<root>" + lineSeparator + "<a>a</a>" + lineSeparator + "<b/>" + lineSeparator + "<c xmlns=\"xyz\"/>" + lineSeparator + "</root>", true, true);
	}
}
