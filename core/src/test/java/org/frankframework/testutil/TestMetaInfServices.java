package org.frankframework.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;

import org.junit.jupiter.api.Test;

import org.frankframework.xml.StaxParserFactory;

/**
 * This class only tests whether the required factory's are created directly with the iaf-core module.
 * This is required when running the Frank!Framework standalone (without application server).
 */
public class TestMetaInfServices {

	@Test //Xerces 2.12 with xml schema 1.1 enabled
	public void testSAXParserFactory() {
		Class<?> parser = org.apache.xerces.jaxp.SAXParserFactoryImpl.class;
		assertEquals(parser, SAXParserFactory.newInstance().getClass());
	}

	@Test //Original sun.com MessageFactory used for creating SOAP messages
	public void testMessageFactory() throws SOAPException {
		Class<?> messageFactory = com.sun.xml.messaging.saaj.soap.dynamic.SOAPMessageFactoryDynamicImpl.class;
		assertEquals(messageFactory, MessageFactory.newInstance().getClass());
	}

	@Test //WstxInputFactory with xml schema 1.1 enabled to fix 'Illegal character entity: expansion character'
	public void testXMLInputFactory() {
		Class<?> inputFactory = StaxParserFactory.class;
		assertEquals(inputFactory, XMLInputFactory.newInstance().getClass());
	}

	@Test //ZephyrWriterFactory to improve fix namespaces support in WSDL's
	public void testXMLOutputFactory() {
		Class<?> outputFactory = com.sun.xml.stream.ZephyrWriterFactory.class;
		assertEquals(outputFactory, XMLOutputFactory.newInstance().getClass());
	}

	@Test //Xerces to ensure that the sax parser is the same as event factory
	public void testXMLEventFactory() {
		Class<?> outputFactory = org.apache.xerces.stax.XMLEventFactoryImpl.class;
		assertEquals(outputFactory, XMLEventFactory.newInstance().getClass());
	}
}
