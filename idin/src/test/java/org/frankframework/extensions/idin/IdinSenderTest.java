package org.frankframework.extensions.idin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import net.bankid.merchant.library.CommunicatorException;
import net.bankid.merchant.library.Configuration;
import net.bankid.merchant.library.IMessenger;
import net.bankid.merchant.library.SigningKeyPair;

import org.frankframework.core.PipeLineSession;
import org.frankframework.extensions.idin.IdinSender.Action;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;

/**
 * Initially I thought, hey lets add some unittests...
 * Let's just skip them for now shall we? :)
 *
 */
public class IdinSenderTest {

	private IdinSender sender = null;
	private PipeLineSession session = null;

	@BeforeEach
	public void initializeIdinSender() throws Exception {
		sender = new IdinSender();
		sender.setConfigurationXML("configs/default-config.xml");

		IMessenger messenger = new DummyMessenger();
		sender.setMessenger(messenger);
		session = new PipeLineSession();
	}

	private class DummyMessenger implements IMessenger {

		@Override
		public String sendMessage(Configuration config, String request, URI url) throws CommunicatorException {
			String requestType = url.toString().replaceAll("http://example.com/", "");
			int testId = config.getMerchantSubID();

			// post-fix the last character of the merchantID to differentiate between tests
			String expectedFile = testId != 0 ? requestType + testId : requestType;

			validateRequest(expectedFile, request);
			return generateResponse(config, requestType);
		}

		private void validateRequest(String expectedFile, String request) throws CommunicatorException {
			try {
				URL expected = ClassUtils.getResourceURL("/messages/"+expectedFile+"-request.xml");
				assertNotNull(expected, "did not find [/messages/"+expectedFile+"-request.xml]");
				String expectedString = StreamUtil.resourceToString(expected);

				// Complex regex, but ensures the correct format: `2024-08-22T11:49:01.760Z` is used.
				request = request.replaceAll("<createDateTimestamp>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z<\\/createDateTimestamp>", "<createDateTimestamp/>");
				request = request.replaceAll("AuthnRequest [\\s\\S]*?>", "AuthnRequest xmlns:ns3=\"http://dummy\" signature=\"here\">");
				request = request.replaceAll("<Signature[\\s\\S]*?<\\/Signature>", "");

				assertEquals(expectedString.replace("\r", ""), request.replace("\r", ""));
			} catch(Exception e) {
				throw new CommunicatorException("unable to find/read request message", e);
			}
		}

		private String generateResponse(Configuration config, String requestType) throws CommunicatorException {
			try {
				URL response = ClassUtils.getResourceURL("/messages/"+requestType+"-response.xml");
				assertNotNull(response);
				String responseString = StreamUtil.resourceToString(response);
				SigningKeyPair keyEntry = config.getKeyProvider().getMerchantSigningKeyPair();
				return addSignature(keyEntry, responseString);
			} catch(Exception e) {
				throw new CommunicatorException("unable to find/read reponse message", e);
			}
		}
	}

	@Test
	public void testCredentialsAliasesToPasswordMappings() {
			sender.setKeyStoreAuthAlias("alias1");
			assertEquals("password1", sender.getKeyStorePassword());

			sender.setMerchantCertificateAuthAlias("alias1");
			assertEquals("password1", sender.getMerchantCertificatePassword());

			sender.setSAMLCertificateAuthAlias("alias1");
			assertEquals("password1", sender.getSAMLCertificatePassword());
	}

	@Test
	public void testXmlConfigurationFile() throws Exception {
		String message = "<idin><transactionID>1111111111111111</transactionID></idin>";

		sender.setAction(Action.RESPONSE);
		sender.configure();

		String result = sender.sendMessageOrThrow(new Message(message), session).asString();
		String expected = """
				<result>
					<status>Success</status>
					<saml>
						<acquirerId>BANKNL2U</acquirerId>
						<attributes>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.initials">SJĆ</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.partnerlastnameprefix">Ja</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.legallastnameprefix">Sm</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.bin">Some Subject</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.legallastname">Smith</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:bankid.deliveredserviceid">4096</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.preferredlastname">John</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.partnerlastname">Jane</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.preferredlastnameprefix">Jo</attribute>
						</attributes>
						<merchantReference>BANKID-1234029966811132</merchantReference>
						<version>BANKNL2U</version>
					</saml>
					<transactionID>1234567890123457</transactionID>
					<timestamp>2020-08-17 17:28:10.008</timestamp>
				</result>\
				""";
		assertEquals(expected, result);
	}

	@Test
	public void testPropertiesLoadedFromAttributes() throws Exception {
		String message = "<idin><transactionID>1111111111111111</transactionID></idin>";

		sender.setConfigurationXML("configs/minimal-config.xml"); // A minimal config with log setting is required

		// These were never set
		sender.setMerchantID("1234567890");
		sender.setMerchantReturnUrl("http://localhost");
		sender.setAcquirerStatusUrl("http://example.com/status");

		// Override the keystore and password
		sender.setKeyStoreLocation("certificates/BankID2020.Libs.sha256.2048.csp.jks");
		sender.setKeyStorePassword("123456");

		sender.setAction(Action.RESPONSE);
		sender.configure();

		String result = sender.sendMessageOrThrow(new Message(message), session).asString();

		String expected = """
				<result>
					<status>Success</status>
					<saml>
						<acquirerId>BANKNL2U</acquirerId>
						<attributes>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.initials">SJĆ</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.partnerlastnameprefix">Ja</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.legallastnameprefix">Sm</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.bin">Some Subject</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.legallastname">Smith</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:bankid.deliveredserviceid">4096</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.preferredlastname">John</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.partnerlastname">Jane</attribute>
							<attribute name="urn:nl:bvn:bankid:1.0:consumer.preferredlastnameprefix">Jo</attribute>
						</attributes>
						<merchantReference>BANKID-1234029966811132</merchantReference>
						<version>BANKNL2U</version>
					</saml>
					<transactionID>1234567890123457</transactionID>
					<timestamp>2020-08-17 17:28:10.008</timestamp>
				</result>\
				""";
		assertEquals(expected, result);
	}

	@Test
	public void getIssuersByCountry() throws Exception {
		String message = "<idin><issuersByCountry>true</issuersByCountry></idin>";

		sender.setMerchantSubID(1);
		sender.setMerchantID("1234567891");
		sender.setAction(Action.DIRECTORY);
		sender.configure();

		String result = sender.sendMessageOrThrow(new Message(message), session).asString();
		assertEquals("""
				<result>
					<issuers>
						<country name="NL">
							<issuer id="INGBNL2A">TEST</issuer>
						</country>
					</issuers>
					<timestamp>2015-07-15 12:10:10.123</timestamp>
				</result>\
				""", result);
	}

	@Test
	public void getIssuer() throws Exception {
		String message = "<idin/>";

		sender.setAction(Action.DIRECTORY);
		sender.configure();

		String result = sender.sendMessageOrThrow(new Message(message), session).asString();
		assertEquals("""
				<result>
					<issuers>
						<issuer id="INGBNL2A" country="NL">TEST</issuer>
					</issuers>
					<timestamp>2015-07-15 12:10:10.123</timestamp>
				</result>\
				""", result);
	}

	@Test
	public void testAuthenticate() throws Exception {
		String message = "<idin><issuerId>BANKNL2Y</issuerId><requestedServiceId>21968</requestedServiceId><entranceCode>abc4ef7ons</entranceCode></idin>";

		sender.setAction(Action.AUTHENTICATE);
		sender.configure();

		String result = sender.sendMessageOrThrow(new Message(message), session).asString();

		assertEquals("""
				<result>
					<authenticationURL>http://localhost/consumerAuthenticationSim?hash=4444</authenticationURL>
					<transactionID>0050000021142024</transactionID>
					<createDateTimestamp>2015-07-15 12:10:10.123</createDateTimestamp>
				</result>\
				""", result);
	}

	@Test
	public void testMerchantReturnUrlSessionKey() throws Exception {
		String message = "<idin><issuerId>BANKNL2Y</issuerId><requestedServiceId>21968</requestedServiceId><entranceCode>abc4ef7ons</entranceCode></idin>";

		sender.setMerchantReturnUrlSessionKey("sessionKeyName");
		sender.setMerchantSubID(2);
		sender.setAction(Action.AUTHENTICATE);
		sender.configure();
		session.put("sessionKeyName", "http://localhost:8080");

		String result = sender.sendMessageOrThrow(new Message(message), session).asString();

		assertEquals("""
				<result>
					<authenticationURL>http://localhost/consumerAuthenticationSim?hash=4444</authenticationURL>
					<transactionID>0050000021142024</transactionID>
					<createDateTimestamp>2015-07-15 12:10:10.123</createDateTimestamp>
				</result>\
				""", result);
	}

	@Test
	public void testAuthenticateFull() throws Exception {
		String message = """
				<idin>
					<issuerId>BANKNL2Y</issuerId>
					<language>de</language>
					<requestedServiceId>21968</requestedServiceId>
					<merchantReference>ref234</merchantReference>
					<expirationPeriod>PT2M</expirationPeriod>P0Y0M0DT0H5M0S
					<entranceCode>abc4ef7ons</entranceCode>
					<assuranceLevel>Loa3</assuranceLevel>
				</idin>
				""";

		sender.setMerchantSubID(1);
		sender.setAction(Action.AUTHENTICATE);
		sender.configure();

		String result = sender.sendMessageOrThrow(new Message(message), session).asString();

		assertEquals("""
				<result>
					<authenticationURL>http://localhost/consumerAuthenticationSim?hash=4444</authenticationURL>
					<transactionID>0050000021142024</transactionID>
					<createDateTimestamp>2015-07-15 12:10:10.123</createDateTimestamp>
				</result>\
				""", result);
	}

	@Test
	public void testAuthenticateDurationLargerThen5Minutes() throws Exception {
		String message = """
				<idin>
					<issuerId>BANKNL2Y</issuerId>
					<requestedServiceId>21968</requestedServiceId>
					<expirationPeriod>PT10M</expirationPeriod>
					<entranceCode>abc4ef7ons</entranceCode>
				</idin>
				""";

		sender.setAction(Action.AUTHENTICATE);
		sender.configure();

		String result = sender.sendMessageOrThrow(new Message(message), session).asString();

		assertEquals("""
				<result>
					<error>
						<statusCode/>
						<details/>
						<message>incorrect value: PT10M</message>
					</error>
				</result>\
				""", result);
	}

	@Test
	public void testPhysicalDestinationName() throws Exception {
		sender.setAcquirerStatusUrl("http://localhost.com");
		sender.setMerchantReturnUrlSessionKey("test123");
		sender.configure();
		assertEquals("returnUrl[dynamicReturnUrl] statusUrl[http://localhost.com]", sender.getPhysicalDestinationName());
	}

	private static String addSignature(SigningKeyPair keyEntry, String xml) throws Exception {
		X509Certificate cert = keyEntry.getCertificate();

		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

		List<Transform> transforms = new ArrayList<>();
		transforms.add(fac.newTransform("http://www.w3.org/2000/09/xmldsig#enveloped-signature", (TransformParameterSpec) null));
		transforms.add(fac.newTransform("http://www.w3.org/2001/10/xml-exc-c14n#", (TransformParameterSpec) null));

		Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA256, null), transforms, null, null);

		SignedInfo si = fac.newSignedInfo(
				fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
				fac.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
				Collections.singletonList(ref));

		KeyInfoFactory kif = fac.getKeyInfoFactory();
		KeyName kn = kif.newKeyName(sha1Hex(cert.getEncoded()));
		KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kn));

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));

		DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), doc.getDocumentElement());

		XMLSignature signature = fac.newXMLSignature(si, ki);
		signature.sign(dsc);

		return serialize(doc);
	}

	private static String serialize(Document doc) throws TransformerException {
		StringWriter sw = new StringWriter();
		Transformer trans = TransformerFactory.newInstance().newTransformer();

		doc.setXmlStandalone(true);
		trans.transform(new DOMSource(doc), new StreamResult(sw));

		return sw.toString();
	}

	private static String sha1Hex(final byte[] data) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		sha1.update(data);
		byte[] fp = sha1.digest();
		String fingerprint = "";
		for (int i = 0; i < fp.length; i++) {
			String f = "00" + Integer.toHexString(fp[i]);
			fingerprint = fingerprint + f.substring(f.length() - 2);
		}
		fingerprint = fingerprint.toUpperCase();

		return fingerprint;
	}
}
