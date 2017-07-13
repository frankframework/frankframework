package nl.nn.adapterframework.soap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * @author Peter Leeuwenburgh
 */
public class SoapWrapperTest {

	@Test
	public void getBody11() throws ConfigurationException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
				+ "<soapenv:Header><MessageHeader xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\"><HeaderFields><MessageId>messageId</MessageId></HeaderFields></MessageHeader></soapenv:Header>"
				+ "<soapenv:Body><FindDocuments_Response xmlns=\"http://api.nn.nl/FindDocuments\"><Result xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\">"
				+ "<Status>OK</Status></Result></FindDocuments_Response></soapenv:Body></soapenv:Envelope>";
		String soapBody = null;
		try {
			soapBody = soapWrapper.getBody(soapMessage);
		} catch (Exception e) {
			soapBody = e.getMessage();
		}
		String expectedSoapBody = "<FindDocuments_Response xmlns=\"http://api.nn.nl/FindDocuments\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><Result xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\">"
				+ "<Status>OK</Status></Result></FindDocuments_Response>";
		assertEquals(expectedSoapBody, soapBody);
	}

	@Test
	public void getBody12() throws ConfigurationException {
		SoapWrapper soapWrapper = SoapWrapper.getInstance();
		String soapMessage = "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">"
				+ "<soapenv:Header><MessageHeader xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\"><HeaderFields><MessageId>messageId</MessageId></HeaderFields></MessageHeader></soapenv:Header>"
				+ "<soapenv:Body><FindDocuments_Response xmlns=\"http://api.nn.nl/FindDocuments\"><Result xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\">"
				+ "<Status>OK</Status></Result></FindDocuments_Response></soapenv:Body></soapenv:Envelope>";
		String soapBody = null;
		try {
			soapBody = soapWrapper.getBody(soapMessage);
		} catch (Exception e) {
			soapBody = e.getMessage();
		}
		String expectedSoapBody = "<FindDocuments_Response xmlns=\"http://api.nn.nl/FindDocuments\" xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\"><Result xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\">"
				+ "<Status>OK</Status></Result></FindDocuments_Response>";
		assertEquals(expectedSoapBody, soapBody);
	}
}
