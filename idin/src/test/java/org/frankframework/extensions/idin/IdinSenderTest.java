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
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.frankframework.core.PipeLineSession;
import org.frankframework.extensions.idin.IdinSender.Action;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import net.bankid.merchant.library.CommunicatorException;
import net.bankid.merchant.library.Configuration;
import net.bankid.merchant.library.IMessenger;
import net.bankid.merchant.library.SigningKeyPair;

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
		sender.configure();
		session = new PipeLineSession();
	}

	private class DummyMessenger implements IMessenger {

		@Override
		public String sendMessage(Configuration config, String request, URI url) throws CommunicatorException {
			String requestType = url.toString().replaceAll("http://example.com/", "");

			validateRequest(requestType, request);
			return generateResponse(config, requestType);
		}

		private void validateRequest(String requestType, String request) throws CommunicatorException {
			try {
				URL expected = ClassUtils.getResourceURL("/messages/"+requestType+"-request.xml");
				assertNotNull(expected, "did not find [/messages/"+requestType+"-request.xml]");
				String expectedString = StreamUtil.resourceToString(expected);
				request = request.replaceAll("<createDateTimestamp[\\s\\S]*?<\\/createDateTimestamp>", "<createDateTimestamp/>");
				request = request.replaceAll("AuthnRequest [\\s\\S]*?>", "AuthRequest signature=\"here\">");
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
	public void testSimpleRequestResponse() throws Exception {
		String message = "<idin><transactionID>1111111111111111</transactionID></idin>";

		sender.setAction(Action.RESPONSE);
		String result = sender.sendMessageOrThrow(new Message(message), session).asString();
		URL expectedUrl = ClassUtils.getResourceURL("/expected/simpleRequestResponse.xml");
		String expected = StreamUtil.resourceToString(expectedUrl);

		assertEquals(expected, result);
	}

	@Test
	public void getIssuersByCountry() throws Exception {
		String message = "<idin><issuersByCountry>true</issuersByCountry></idin>";

		sender.setAction(Action.DIRECTORY);
		String result = sender.sendMessageOrThrow(new Message(message), session).asString();
		assertEquals("<result>\n"
				+ "	<issuers>\n"
				+ "		<country name=\"NL\">\n"
				+ "			<issuer id=\"INGBNL2A\">TEST</issuer>\n"
				+ "		</country>\n"
				+ "	</issuers>\n"
				+ "	<timestamp>2015-07-15 12:10:10.123</timestamp>\n"
				+ "</result>", result);
	}

	@Test
	public void testAuthenticate() throws Exception {
		String message = "<idin><issuerId>BANKNL2Y</issuerId><requestedServiceId>21968</requestedServiceId><entranceCode>abc4ef7ons</entranceCode></idin>";

		sender.setAction(Action.AUTHENTICATE);
		String result = sender.sendMessageOrThrow(new Message(message), session).asString();

		assertEquals("<result>\n"
				+ "	<authenticationURL>http://localhost/consumerAuthenticationSim?hash=4444</authenticationURL>\n"
				+ "	<transactionID>0050000021142024</transactionID>\n"
				+ "	<createDateTimestamp>2015-07-15 12:10:10.123</createDateTimestamp>\n"
				+ "</result>", result);
	}

	private static String addSignature(SigningKeyPair keyEntry, String xml) throws Exception {
		X509Certificate cert = (X509Certificate) keyEntry.getCertificate();

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

	private static String serialize(Document doc) throws TransformerConfigurationException, TransformerException {
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
