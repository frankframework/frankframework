package nl.nn.adapterframework.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.util.UsernameTokenUtil;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WsuIdAllocator;
import org.apache.xml.security.algorithms.JCEMapper;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
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
		String expectedSoapBody = TestFileUtils.getTestFile("/Soap/signedSoap1_1_passwordDigest_mock.xml");
		Message soapBody = soapWrapper.signMessage(new Message(soapMessage), "dummy-username", "dummy-password", true);
		String result = replaceDynamicElements(soapBody);
		MatchUtils.assertXmlEquals(expectedSoapBody, result);

		//TODO assertTrue(verifySoapDigest(soapBody)); //does not read digest methods, only plain text
	}

	@Test
	public void signSoap11Message() throws Exception {
		resetWSConfig();

		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = soapMessageSoap11;
		Message soapBody = soapWrapper.signMessage(new Message(soapMessage), "dummy-username", "dummy-password", false);
		String result = replaceDynamicElements(soapBody);

		String expectedSoapBody = TestFileUtils.getTestFile("/Soap/signedSoap1_1_mock.xml");
		MatchUtils.assertXmlEquals(expectedSoapBody, result);

		assertTrue(verifySoapDigest(soapBody));
	}

	@Test
	public void validateSignedSoap1_1() throws Exception {
		URL file = TestFileUtils.getTestFileURL("/Soap/signedSoap1_1.xml");
		assertNotNull(file); //ensure we can find the file

		Message soapBody = toSoapMessage(file);
		assertTrue(verifySoapDigest(soapBody));
	}

	private Message toSoapMessage(URL url) throws Exception {
		MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
		SOAPMessage msg = factory.createMessage();
		SOAPPart part = msg.getSOAPPart();
		part.setContent(new StreamSource(url.openStream()));

		// create unsigned envelope
		SOAPEnvelope unsignedEnvelope = part.getEnvelope();
		Document doc = unsignedEnvelope.getOwnerDocument();
		return new Message(doc);
	}

	private boolean verifySoapDigest(Message soapBody) throws Exception {
		Document doc = (Document) soapBody.asObject();
		NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		if (nl.getLength() == 0) {
			fail("Cannot find Signature element");
		}

		DOMValidateContext valContext = new DOMValidateContext(new UsernameTokenSelector(), nl.item(0));
		XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
		XMLSignature signature = factory.unmarshalXMLSignature(valContext);
		return signature.validate(valContext);
	}

	/**
	 * Test the creation of the UT token which is used to generate the ds:SignatureValue
	 */
	@Test
	public void testPasswordDigest() throws Exception {
		String nonce = "4mG1rTLgRa8q40VPUrB+sQ==";
		String timestamp = "2022-01-31T12:05:17Z";
		String password = "dummy-password";

		byte[] decodedNonce = org.apache.xml.security.utils.XMLUtils.decode(nonce);
		String signatureValue = UsernameTokenUtil.doPasswordDigest(decodedNonce, timestamp, password);

		assertTrue(verifySignature(decodedNonce, timestamp, password, signatureValue));
	}

	private boolean verifySignature(byte[] nonceBytes, String created, String pwd, String passwordDigest) {
		// http://www.w3.org/2000/09/xmldsig#hmac-sha1
		try {
			String charset = StandardCharsets.UTF_8.displayName();
			byte[] createdBytes = created.getBytes(charset);
			byte[] passwordBytes = pwd.getBytes(charset);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(nonceBytes);
			outputStream.write(createdBytes);
			outputStream.write(passwordBytes);
			byte[] concatenatedBytes = outputStream.toByteArray();
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(concatenatedBytes, 0, concatenatedBytes.length);
			byte[] digestBytes = digest.digest();

			String digestString = Base64.getEncoder().encodeToString(digestBytes);

			String result = "";
			if (digestString.equals(passwordDigest)) {
				result = "valid";
			} else {
				result = "invalid";
			}
			System.out.println("Provided password digest is: " + result);
			System.out.println("   Nonce: " + Base64.getEncoder().encodeToString(nonceBytes));
			System.out.println("   Timestamp: " + created);
			System.out.println("   Password: " + pwd);
			System.out.println("   Computed digest: " + digestString);
			System.out.println("   Provided digest: " + passwordDigest);
			return digestString.equals(passwordDigest);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private static class UsernameTokenSelector extends KeySelector {

		public UsernameTokenSelector() {
			JCEMapper.registerDefaultAlgorithms();
		}

		@Override
		public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) throws KeySelectorException {

			if (keyInfo == null) {
				throw new KeySelectorException("no KeyInfo supplied!");
			}

			SignatureMethod sm = (SignatureMethod) method;
			List list = keyInfo.getContent();

			for (int i = 0; i < list.size(); i++) {
				XMLStructure xmlStructure = (XMLStructure) list.get(i);
				if (xmlStructure instanceof DOMStructure && purpose.equals(KeySelector.Purpose.VERIFY)) {
					DOMStructure dom = (DOMStructure) xmlStructure;
					Node wsse = dom.getNode();
					Element reference = XmlUtils.getFirstChildTag((Element) wsse, "wsse:Reference");
					String uri = reference.getAttribute("URI"); //reference to the UsernameTokenElement

					Document document = wsse.getOwnerDocument();
					NodeList utl = document.getElementsByTagNameNS(WSS4JConstants.WSSE_NS, "UsernameToken");
					Element ut = (Element) utl.item(0); //TODO use the URI here to select the correct UT item

					byte[] passwordDigest = getPasswordDigest(ut);

					return new KeySelectorResult() {
						@Override
						public Key getKey() {
							String keyAlgorithm = JCEMapper.getJCEKeyAlgorithmFromURI(WSConstants.HMAC_SHA1); //TODO use the node's namespace
							assertNotNull(keyAlgorithm);
							return new SecretKeySpec(passwordDigest, keyAlgorithm);
						}
					};
				}
			}
			throw new KeySelectorException("No KeyValue element found!");
		}

		private byte[] getPasswordDigest(Element usernameTokenElement) throws KeySelectorException {
			try {
				String password = XmlUtils.getChildTagAsString(usernameTokenElement, "wsse:Password");
				String nonce = XmlUtils.getChildTagAsString(usernameTokenElement, "wsse:Nonce");
				byte[] decodedNonce = org.apache.xml.security.utils.XMLUtils.decode(nonce);
				String created = XmlUtils.getChildTagAsString(usernameTokenElement, "wsu:Created");

				String key = UsernameTokenUtil.doPasswordDigest(decodedNonce, created, password);
				return key.getBytes("UTF-8");
			} catch (Exception e) {
				throw new KeySelectorException(e);
			}
		}
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
