package nl.nn.adapterframework.soap;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;

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
	public void signSoap11MessageDigestPassword() throws ConfigurationException, IOException, TransformerException, SAXException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = soapMessageSoap11;
		String expectedSoapBody = TestFileUtils.getTestFile("/Soap/signedSoap1_1_passwordDigest.xml");
		String soapBody = soapWrapper.signMessage(new Message(soapMessage), "digestPassword", "digestPassword", true).asString();
		String result = replaceDynamicElements(soapBody);
		assertEquals(expectedSoapBody, result);
	}

	@Test
	public void signSoap11Message() throws ConfigurationException, IOException, TransformerException, SAXException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = soapMessageSoap11;
		String expectedSoapBody = TestFileUtils.getTestFile("/Soap/signedSoap1_1.xml");
		String soapBody = soapWrapper.signMessage(new Message(soapMessage), "test", "test", false).asString();
		String result = replaceDynamicElements(soapBody);
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

	private String replaceDynamicElements(String result) throws IOException, TransformerException, SAXException {
		URL url = TestFileUtils.getTestFileURL("/Soap/ignoreElements.xsl");
		Transformer transformer = XmlUtils.createTransformer(url);
		return XmlUtils.transformXml(transformer, result);
	}
	
}
