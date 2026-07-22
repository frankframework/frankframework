package org.frankframework.http.cxf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.util.MessageUtils;

public class SoapContextTest {

	private Message getFile(String file) {
		URL url = this.getClass().getResource(file);
		if (url == null) {
			fail("file not found");
		}
		return new UrlMessage(url);
	}

	private SoapContext createContextFromRawMessage(String filename) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/address/test123");
		Message rawContent = getFile("/Soap/"+filename);

		try (BufferedReader reader = new BufferedReader(rawContent.asReader())) {
			for (String line; null != (line = reader.readLine()); ) {
				if (line.isBlank()) {
					break;
				}

				String[] h = StringUtils.split(line, ": ", 2);
				request.addHeader(h[0], h[1]);
			}

			Writer sw = new StringWriter();
			reader.transferTo(sw);
			request.setContent(sw.toString().getBytes(StandardCharsets.UTF_8));
		}

		return SoapMessage.from(request).getSoapContext();
	}

	@Test
	public void testGetNamespaceURI() throws Exception {
		SoapContext context = new SoapContext(getFile("/Soap/VrijeBerichten_PipelineRequest.xml"));
		assertEquals("http://www.egem.nl/StUF/sector/zkn/0310", context.getNamespaceURI());
	}

	@ParameterizedTest
	@CsvSource({
			"1, '/raw/soap1_1.txt'",
			"1, '/raw/soap1_1_with_action_in_contenttype.txt'",
			"2, '/raw/soap1_2.txt'",
			"2, '/raw/soap1_2_with_soapaction_header.txt'"
	})
	public void testFindSoapAction(int version, String file) throws Exception {
		SoapContext context = createContextFromRawMessage(file);

		assertEquals("SOAP 1.%d Protocol".formatted(version), context.getSoapProtocol());
		assertEquals("http://www.egem.nl/StUF/sector/zkn/0310", context.getSoapAction());
	}

	@Test
	public void validXmlInvalidSoap() {
		SOAPException e = assertThrows(SOAPException.class, () -> new SoapContext(new Message("""
				<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
					<soapenv:Boody>
						<hallo/>
					</soapenv:Body>
				</soapenv:Envelope>
				""")));
		assertEquals("invalid SOAP message", e.getMessage());
	}

	@Test
	public void validXmlInvalidSoap2() {
		SOAPException e = assertThrows(SOAPException.class, () -> new SoapContext(new Message("""
				<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
					<soapenv:Body>
						<hallo/>
					</soapenv:Body>
					<soapenv:Header />
				</soapenv:Envelope>
				""")));
		assertEquals("invalid SOAP message", e.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/Validation/EB-XML/Soap/valid-soap-with-must-understand-attrib.xml"
	})
	public void validSoap(String resource) {
		assertDoesNotThrow(() -> new SoapContext(getFile(resource)));
	}

	@Test
	public void invalidXml() {
		SOAPException e = assertThrows(SOAPException.class, () -> new SoapContext(new Message("""
				<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
					<soapenv:Body>
						<hallo>
					</soapenv:Body>
				</soapenv:Envelope>
				""")));
		assertEquals("invalid SOAP message", e.getMessage());
	}

	@ParameterizedTest
	@CsvSource({
			"text/xml;action=tralala, tralala",
			"application/soap+xml;charset=UTF-8;action=0310, 0310",
			"application/soap+xml;charset=UTF-8;action=\"0310\", 0310",
			"application/soap+xml;charset=UTF-8;action='0310', 0310",
			"application/soap+xml;charset=UTF-8;action=\"http://www.egem.nl/StUF/sector/zkn/0310\", http://www.egem.nl/StUF/sector/zkn/0310"
	})
	public void findAction(String contentType, String action) {
		MediaType mediaType = MediaType.parseMediaType(contentType);
		assertEquals(action, SoapContext.findAction(mediaType));
	}

	@ParameterizedTest
	@CsvSource({
			"application/soap+xml;charset=UTF-8;action=\"\"",
			"application/soap+xml;charset=UTF-8;action=''",
	})
	public void emptyAction(String contentType) {
		MediaType mediaType = MediaType.parseMediaType(contentType);
		assertEquals("", SoapContext.findAction(mediaType));
	}

	@Test
	public void nullAction() {
		MediaType mediaType = MediaType.parseMediaType("application/soap+xml;charset=UTF-8;action");
		assertNull(SoapContext.findAction(mediaType));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"soap-addressing.xml",
			"soap-addressing-ns.xml",
			"soap-addressing-minimal.xml",
			"soap-addressing-ns-on-header.xml",
			"soap-addressing-with-selfclosing-relatesto.xml",
			"soap-addressing-with-empty-relatesto.xml"
		})
	public void soapAddressing(String input) throws Throwable {
		SoapContext context = new SoapContext(getFile("/Soap/" + input));

		assertEquals("6B29FC40-CA47-1067-B31D-00DD010662DA", context.getMessageId());

		Message responseMessage = new Message("""
			<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
				<soapenv:Body>
					<hallo/>
				</soapenv:Body>
			</soapenv:Envelope>
			""");

		Message responseWithId = context.setMessageId(responseMessage);

		MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
		SOAPMessage soapMessage = factory.createMessage();
		soapMessage.getSOAPPart().setContent(responseWithId.asSource());

		NodeList nodes = soapMessage.getSOAPHeader().getElementsByTagNameNS("https://www.w3.org/2006/03/addressing/ws-addr.xsd", "RelatesTo");
		if (nodes.getLength() > 0 && nodes.item(0).getNodeType() == Node.ELEMENT_NODE) {
			String relatesTo = nodes.item(0).getTextContent();
			assertEquals("6B29FC40-CA47-1067-B31D-00DD010662DA", relatesTo);
		} else {
			fail("no message id found in response: " + responseWithId.asString());
		}
	}

	@Test
	public void soapAddressNoMid() throws Throwable {
		SoapContext context = new SoapContext(getFile("/Soap/soap-addressing-ns-but-no-mid.xml"));

		String mid = context.getMessageId();
		assertNotNull(mid);
		assertTrue(mid.startsWith(MessageUtils.DEFAULT_MESSAGE_ID_PREFIX));

		Message responseWithId = context.setMessageId(getFile("/Soap/soapmsg1_2.xml"));

		assertFalse(responseWithId.asString().contains("RelatesTo"), "response should not contain the <RelatesTo../> element.");
	}

	@Test
	/**
	 * We want to ensure that when somebody has manually set a relatesTo (although I cannot imagine why) we don't overwrite it.
	 */
	public void soapWithManualRelatesTo() throws Throwable {
		SoapContext context = new SoapContext(getFile("/Soap/soap-addressing-with-relatesto.xml"));

		Message response = getFile("/Soap/soap-addressing-with-relatesto.xml");
		Message responseWithId = context.setMessageId(response);

		MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		SOAPMessage soapMessage = factory.createMessage();
		soapMessage.getSOAPPart().setContent(responseWithId.asSource());

		NodeList nodes = soapMessage.getSOAPHeader().getElementsByTagNameNS("*", "RelatesTo");
		if (nodes.getLength() > 0 && nodes.item(0).getNodeType() == Node.ELEMENT_NODE) {
			String relatesTo = nodes.item(0).getTextContent();
			assertEquals("test123", relatesTo);
		} else {
			fail("no message id found in response: " + responseWithId.asString());
		}
	}
}
