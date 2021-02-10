package nl.nn.adapterframework.util;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.testutil.TestScopeProvider;

public class XmlUtilsTest extends FunctionalTransformerPoolTestBase {

	public void testRemoveNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeRemoveNamespacesXslt(omitXmlDeclaration, indent),input,expected);
		testTransformerPool(XmlUtils.getRemoveNamespacesTransformerPool(omitXmlDeclaration, indent),input,expected);
	}
	
	public void testGetRootNamespace(String input, String expected) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeGetRootNamespaceXslt(),input,expected);
		testTransformerPool(XmlUtils.getGetRootNamespaceTransformerPool(),input,expected);
	}

	public void testAddRootNamespace(String namespace, String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeAddRootNamespaceXslt(namespace, omitXmlDeclaration, indent),input,expected);
		testTransformerPool(XmlUtils.getAddRootNamespaceTransformerPool(namespace, omitXmlDeclaration, indent),input,expected);
	}

	public void testChangeRoot(String root, String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeChangeRootXslt(root, omitXmlDeclaration, indent),input,expected);
		testTransformerPool(XmlUtils.getChangeRootTransformerPool(root, omitXmlDeclaration, indent),input,expected);
	}

	public void testRemoveUnusedNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeRemoveUnusedNamespacesXslt(omitXmlDeclaration, indent),input,expected);
		testTransformerPool(XmlUtils.getRemoveUnusedNamespacesTransformerPool(omitXmlDeclaration, indent),input,expected);
	}

	public void testRemoveUnusedNamespacesXslt2(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws SAXException, TransformerException, IOException, ConfigurationException {
		testXslt(XmlUtils.makeRemoveUnusedNamespacesXslt2(omitXmlDeclaration, indent),input,expected);
//		testTransformerPool(XmlUtils.getRemoveUnusedNamespacesXslt2TransformerPool(omitXmlDeclaration, indent),input,expected);
	}


	@Test
	public void testRemoveNamespaces() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator=System.getProperty("line.separator");
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+lineSeparator+"<root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"   <b/>"+lineSeparator+"   <c/>"+lineSeparator+"</root>",false,true);
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<root><a>a</a><b/><c/></root>",true,false);
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"   <b/>"+lineSeparator+"   <c/>"+lineSeparator+"</root>",true,true);
	}

	@Test
	public void testGetRootNamespace() throws SAXException, TransformerException, IOException, ConfigurationException {
		testGetRootNamespace("<root><a>a</a><b></b><c/></root>","");
		testGetRootNamespace("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>","xyz");
		testGetRootNamespace("<root xmlns:xx=\"xyz\"><a xmlns=\"xyz\">a</a><b></b><c/></root>","");
		testGetRootNamespace("<xx:root xmlns:xx=\"xyz\"><a xmlns=\"xyz\">a</a><b></b><c/></xx:root>","xyz");
	}

	@Test
	public void testAddRootNamespace() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator=System.getProperty("line.separator");
		testAddRootNamespace("xyz","<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>",false,false);
		testAddRootNamespace("xyz","<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\">"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",false,true);
		testAddRootNamespace("xyz","<root><a>a</a><b></b><c/></root>","<root xmlns=\"xyz\"><a>a</a><b/><c/></root>",true,false);
		testAddRootNamespace("xyz","<root><a>a</a><b></b><c/></root>","<root xmlns=\"xyz\">"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",true,true);
	}

	@Test
	public void testChangeRoot() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator=System.getProperty("line.separator");
		testChangeRoot("switch","<root><a>a</a></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><switch><a>a</a></switch>",false,false);
		testChangeRoot("switch","<root><a>a</a></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><switch>"+lineSeparator+"<a>a</a>"+lineSeparator+"</switch>",false,true);
		testChangeRoot("switch","<root><a>a</a></root>","<switch><a>a</a></switch>",true,false);
		testChangeRoot("switch","<root><a>a</a></root>","<switch>"+lineSeparator+"<a>a</a>"+lineSeparator+"</switch>",true,true);
	}

	@Test()
	public void testRemoveUnusedNamespaces() throws SAXException, TransformerException, IOException, ConfigurationException {
		String lineSeparator=System.getProperty("line.separator");
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",false,true);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>","<root><a>a</a><b/><c/></root>",true,false);
		testRemoveUnusedNamespaces("<root><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",true,true);

		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",false,true);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>","<root><a>a</a><b/><c/></root>",true,false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",true,true);

		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\"><a>a</a><b/><c/></root>",false,false);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"xyz\">"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",false,true);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>","<root xmlns=\"xyz\"><a>a</a><b/><c/></root>",true,false);
		testRemoveUnusedNamespaces("<root xmlns=\"xyz\"><a>a</a><b></b><c/></root>","<root xmlns=\"xyz\">"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",true,true);

		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c xmlns=\"xyz\"/></root>",false,false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c xmlns=\"xyz\"/>"+lineSeparator+"</root>",false,true);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>","<root><a>a</a><b/><c xmlns=\"xyz\"/></root>",true,false);
		testRemoveUnusedNamespaces("<root xmlns:xx=\"xyz\"><a>a</a><b></b><xx:c/></root>","<root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c xmlns=\"xyz\"/>"+lineSeparator+"</root>",true,true);

	}

	@Test
	public void testIdentityTransform() throws Exception {
		Resource resource = Resource.getResource(new TestScopeProvider(), "XmlUtils/Configuration.xml");
		SAXException thrown = assertThrows(SAXException.class, () -> {
			XmlUtils.identityTransform(resource);
		});

		String errorMessage = "Cannot get resource for href [file:/G:/1GB.bin] with base [classpath:XmlUtils/Configuration.xml] as ref [file:/G:/1GB.bin] protocol [file] in scope [nl.nn.adapterframework.testutil.TestScopeProvider";
		assertTrue("SaxParseException should contain [Cannot get resource ...]", thrown.getMessage().startsWith(errorMessage));
	}
}
