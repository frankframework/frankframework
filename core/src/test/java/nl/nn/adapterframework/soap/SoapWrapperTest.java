package nl.nn.adapterframework.soap;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.transform.TransformerException;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WsuIdAllocator;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.AttributesWrapper;
import nl.nn.adapterframework.xml.FullXmlFilter;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * @author Peter Leeuwenburgh
 */
public class SoapWrapperTest {

	private String xmlMessage = "<FindDocuments_Response xmlns=\"http://api.nn.nl/FindDocuments\"><Result xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\">"
			+ "<Status>OK</Status></Result></FindDocuments_Response>";

	private String soapMessageSoap11 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
			+ "<soapenv:Header><MessageHeader xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\"><HeaderFields><MessageId>messageId</MessageId></HeaderFields></MessageHeader></soapenv:Header>"
			+ "<soapenv:Body>"+xmlMessage+"</soapenv:Body></soapenv:Envelope>";

	private String soapMessageSoap12 = "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">"
			+ "<soapenv:Header><MessageHeader xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\"><HeaderFields><MessageId>messageId</MessageId></HeaderFields></MessageHeader></soapenv:Header>"
			+ "<soapenv:Body>"+xmlMessage+"</soapenv:Body></soapenv:Envelope>";

	private String expectedSoapBody11 = "<FindDocuments_Response xmlns=\"http://api.nn.nl/FindDocuments\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><Result xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\">"
			+ "<Status>OK</Status></Result></FindDocuments_Response>";
	private String expectedSoapBody12 = "<FindDocuments_Response xmlns=\"http://api.nn.nl/FindDocuments\" xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\"><Result xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\">"
			+ "<Status>OK</Status></Result></FindDocuments_Response>";
	
	@Test
	public void getBody11() throws ConfigurationException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = soapMessageSoap11;
		String expectedSoapBody = expectedSoapBody11;
		String soapBody = null;
		try {
			soapBody = soapWrapper.getBody(new Message(soapMessage)).asString();
		} catch (Exception e) {
			soapBody = e.getMessage();
		}
		assertEquals(expectedSoapBody, soapBody);
	}

	@Test
	public void getBody12() throws ConfigurationException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = soapMessageSoap12;
		String expectedSoapBody = expectedSoapBody12;
		String soapBody = null;
		try {
			soapBody = soapWrapper.getBody(new Message(soapMessage)).asString();
		} catch (Exception e) {
			soapBody = e.getMessage();
		}
		assertEquals(expectedSoapBody, soapBody);
	}
	
	@Test
	public void getBodyXml() throws ConfigurationException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = xmlMessage;
		String expectedSoapBody = "";
		String soapBody = null;
		try {
			soapBody = soapWrapper.getBody(new Message(soapMessage)).asString();
		} catch (Exception e) {
			soapBody = e.getMessage();
		}
		assertEquals(expectedSoapBody, soapBody);
	}
	
	@Test
	public void getBody11AndStoreSoapNamespace() throws ConfigurationException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = soapMessageSoap11;
		String expectedSoapBody = expectedSoapBody11;
		String soapBody = null;
		PipeLineSession session = new PipeLineSession();
		String sessionKey = "SoapVersion";
		try {
			soapBody = soapWrapper.getBody(new Message(soapMessage), true, session, sessionKey).asString();
		} catch (Exception e) {
			soapBody = e.getMessage();
		}
		assertEquals(expectedSoapBody, soapBody);
		String soapVersion = (String)session.get(sessionKey);
		assertEquals(SoapVersion.SOAP11.namespace,soapVersion);
	}

	@Test
	public void getBody12AndStoreSoapNamespace() throws ConfigurationException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = soapMessageSoap12;
		String expectedSoapBody = expectedSoapBody12;
		String soapBody = null;
		PipeLineSession session = new PipeLineSession();
		String sessionKey = "SoapVersion";
		try {
			soapBody = soapWrapper.getBody(new Message(soapMessage), true, session, sessionKey).asString();
		} catch (Exception e) {
			soapBody = e.getMessage();
		}
		assertEquals(expectedSoapBody, soapBody);
		String soapVersion = (String)session.get(sessionKey);
		assertEquals(SoapVersion.SOAP12.namespace,soapVersion);
	}
	
	@Test
	public void getBodyXmlAndStoreSoapNamespace() throws ConfigurationException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = xmlMessage;
		String expectedSoapBody = xmlMessage;
		String soapBody = null;
		PipeLineSession session = new PipeLineSession();
		String sessionKey = "SoapVersion";
		try {
			soapBody = soapWrapper.getBody(new Message(soapMessage), true, session, sessionKey).asString();
		} catch (Exception e) {
			soapBody = e.getMessage();
		}
		assertEquals(expectedSoapBody, soapBody);
		String soapVersion = (String)session.get(sessionKey);
		assertEquals(SoapVersion.NONE.namespace,soapVersion);
	}

	@Test
	public void signSoap11MessageDigestPassword() throws Exception {
		resetWSConfig();

		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = soapMessageSoap11;
		String expectedSoapBody = TestFileUtils.getTestFile("/Soap/signedSoap1_1_passwordDigest.xml");
		Message soapBody = soapWrapper.signMessage(new Message(soapMessage), "dummy-username", "dummy-password", true);
		String result = replaceDynamicElements(soapBody);
		assertEquals(expectedSoapBody, result);
	}

	@Test
	public void signSoap11Message() throws Exception {
		resetWSConfig();

		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = soapMessageSoap11;
		Message soapBody = soapWrapper.signMessage(new Message(soapMessage), "dummy-username", "dummy-password", false);
		String result = replaceDynamicElements(soapBody);

		String expectedSoapBody = TestFileUtils.getTestFile("/Soap/signedSoap1_1.xml");
		assertEquals(expectedSoapBody, result);
	}

	@Test
	public void signSoap11MessageBadFlow() throws ConfigurationException, IOException, TransformerException, SAXException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();

		String soapMessage = "noSOAP";
		String soapBody = null;
		try {
			soapBody = soapWrapper.signMessage(new Message(soapMessage), "test", "test", false).asString();
		} catch (Exception e) {
			soapBody = e.getMessage();
		}
		assertEquals("Could not sign message", soapBody);
	}

	private String replaceDynamicElements(Message soapBody) throws Exception {
		XmlWriter writer = new XmlWriter();
		RemoveDynamicElements handler = new RemoveDynamicElements(writer);
		XmlUtils.parseXml(soapBody.asInputSource(), handler);
		return writer.toString();
	}

	private static class RemoveDynamicElements extends FullXmlFilter {
		private enum Namespace { Timestamp, Nonce, Password, SignatureValue };
		private Namespace ns = null;

		public RemoveDynamicElements(ContentHandler writer) {
			super(writer);
		}

		@Override
		public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) throws SAXException {
			if(uri.equals(WSConstants.WSU_NS)) {
				ns = Namespace.Timestamp;
			}
			if(uri.equals(WSConstants.WSSE_NS) && localName.equals("Nonce")) {
				ns = Namespace.Nonce;
			}
			if(uri.equals(WSConstants.WSSE_NS) && localName.equals("Password")) {
				ns = Namespace.Password;
			}
			if(uri.equals(WSConstants.SIG_NS) && localName.equals("SignatureValue")) {
				ns = Namespace.SignatureValue;
			}
			if(uri.equals(WSConstants.WSSE_NS) && localName.equals("SecurityTokenReference")) {
				atts = new AttributesWrapper(atts, "Id");
			}
			if(uri.equals(WSConstants.SIG_NS) && localName.equals("KeyInfo")) {
				atts = new AttributesWrapper(atts, "Id");
			}

			super.startElement(uri, localName, qName, atts);
		}
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if(ns != null && length > 1) {
				String replaceWith = "fake-"+ns.name();
				super.characters(replaceWith.toCharArray(), 0, replaceWith.length());
				return;
			}
			super.characters(ch, start, length);
		}
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(ns != null) {
				ns = null;
			}
			super.endElement(uri, localName, qName);
		};
	}

	private void resetWSConfig() throws Exception {
		SoapWrapper.getInstance().setIdAllocator(new WsuIdAllocator() {
			AtomicInteger i = new AtomicInteger(1);

			@Override
			public String createId(String prefix, Object o) {
				if(prefix.equals("TS-")) {
					prefix = "Timestamp-";
				}
				if(prefix.equals("SIG-")) {
					prefix = "Signature-";
				}
				return Misc.concat("", prefix, ""+i.getAndIncrement());
			}

			@Override
			public String createSecureId(String prefix, Object o) {
				return Misc.concat("", prefix, Misc.createUUID());
			}
		});
	}
}
