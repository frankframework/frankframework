package org.frankframework.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import jakarta.xml.soap.SOAPException;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;

import org.frankframework.encryption.KeystoreType;
import org.frankframework.lifecycle.LoadBouncyCastleBean;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.StreamUtil;

public class SoapUtilsTest {

	private KeyStore createDummyKeyStoreWithNullKeyPassword(String certificateName, String certificatePassword) throws Exception {
		// Load BouncyCastle if not already set.
		new LoadBouncyCastleBean().afterPropertiesSet();

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keyPair = keyGen.generateKeyPair();

		X500Name owner = new X500Name("CN=Test, OU=Test, O=Test, L=Test, C=US");
		BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
		Instant validFrom = Instant.now();
		Instant validTo = validFrom.plus(365, ChronoUnit.DAYS);

		JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
				owner,
				serial,
				Date.from(validFrom),
				Date.from(validTo),
				owner,
				keyPair.getPublic()
		);
		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(signer));

		KeyStore ks = KeyStore.getInstance(KeystoreType.PKCS12.name());
		ks.load(null, null);
		ks.setKeyEntry(certificateName, keyPair.getPrivate(), certificatePassword.toCharArray(), new Certificate[] { cert } );
		return ks;
	}

	@ParameterizedTest
	@CsvSource({"true, true", "true, false", "false, true", "false, false"})
	void validateEncryptedSoap1_1(boolean includeCertificateInMessage, boolean removeSecurityHeader) throws Exception {
		URL file = TestFileUtils.getTestFileURL("/Soap/Encryption/SZeebraSoap.xml");
		assertNotNull(file); // ensure we can find the file

		String certificateName = "myCertificateNameWithCasing";
		KeyStore keystore = createDummyKeyStoreWithNullKeyPassword(certificateName, "changeit");

		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey secretKey = keyGen.generateKey();

		Message encrypted = SoapUtils.encryptMessage(new UrlMessage(file), keystore, certificateName, secretKey, includeCertificateInMessage,
				SoapUtils.KeyIdentifierType.THUMBPRINT_IDENTIFIER, SoapUtils.DigestAlgorithm.SHA1, SoapUtils.KeyEncryptionAlgorithm.RSA_OAEP, SoapUtils.DataEncryptionAlgorithm.AES_256);

		String encryptedString = encrypted.asString()
				.replaceAll("<xenc:CipherValue>.*?</xenc:CipherValue>", "<xenc:CipherValue>IGNORE-CIPHER-VALUE</xenc:CipherValue>")
				.replaceAll("<wsu:Created>.*?</wsu:Created>", "<wsu:Created>IGNORE-CREATED</wsu:Created>")
				.replaceAll("<wsu:Expires>.*?</wsu:Expires>", "<wsu:Expires>IGNORE-EXPIRES</wsu:Expires>")
				.replaceAll("(Id=\")[^\"]*\"", "Id=\"id-here\"")
				.replaceAll("(URI=\")[^\"]*\"", "URI=\"uri-here\"")
				.replaceAll("(<wsse:BinarySecurityToken[^>]*>)[^<]*(</wsse:BinarySecurityToken>)", "$1IGNORE-BST$2")
				.replaceAll("(<wsse:KeyIdentifier[^>]*>)[^<]*(</wsse:KeyIdentifier>)", "$1IGNORE-KI$2");

		URL expectedFile = TestFileUtils.getTestFileURL("/Soap/Encryption/SZeebraSoap-encrypted-"+(includeCertificateInMessage ? "with" : "no")+"cert.xml");
		assertNotNull(expectedFile); // ensure we can find the file
		MatchUtils.assertXmlEquals(StreamUtil.resourceToString(expectedFile), encryptedString);

		Message decrypted = SoapUtils.decryptMessage(encrypted, keystore, certificateName, "changeit", removeSecurityHeader);
		// Ensure the decrypted result is the same as the initial document
		MatchUtils.assertXmlEquals(removeSecurityHeader ? StreamUtil.resourceToString(file) : encrypted.asString(), decrypted.asString());
	}

	@Test
	void validateEncryptedErrorSoap1_1() throws Exception {
		URL file = TestFileUtils.getTestFileURL("/Soap/Encryption/SZeebraSoap.xml");
		assertNotNull(file); // ensure we can find the file

		String certificateName = "myCustomCertificateName";
		KeyStore keystore = createDummyKeyStoreWithNullKeyPassword(certificateName, "changeit");

		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey secretKey = keyGen.generateKey();

		Message encrypted = SoapUtils.encryptMessage(new UrlMessage(file), keystore, certificateName, secretKey, false,
				SoapUtils.KeyIdentifierType.THUMBPRINT_IDENTIFIER, SoapUtils.DigestAlgorithm.SHA1, SoapUtils.KeyEncryptionAlgorithm.RSA_OAEP, SoapUtils.DataEncryptionAlgorithm.AES_256);

		WSSecurityException e1 = assertThrows(WSSecurityException.class, () -> SoapUtils.decryptMessage(encrypted, keystore, certificateName, "wrong-password", false));
		assertEquals("unable to process security header", e1.getMessage());
		assertNotNull(e1.getCause());
		// Cause trace logs: "the private key for the supplied alias does not exist in the keystore"

		WSSecurityException e2 = assertThrows(WSSecurityException.class, () -> SoapUtils.decryptMessage(encrypted, keystore, "wrong-cert", "changeit", false));
		assertEquals("unable to process security header", e2.getMessage());
		assertNotNull(e2.getCause());
		// Cause trace logs: "the private key for the supplied alias does not exist in the keystore"

		KeyStore differentStoreSameCertname = createDummyKeyStoreWithNullKeyPassword(certificateName, "changeit");
		WSSecurityException e3 = assertThrows(WSSecurityException.class, () -> SoapUtils.decryptMessage(encrypted, differentStoreSameCertname, certificateName, "changeit", false));
		assertEquals("unable to process security header", e3.getMessage());
		assertNotNull(e3.getCause());
		// Cause trace logs: "No certificates were found for decryption (KeyId)"

		Message manipulatedMessage = new Message(encrypted.asString()
				.replaceAll("<xenc:CipherValue>.*?</xenc:CipherValue>", "<xenc:CipherValue>IGNORE-CIPHER-VALUE</xenc:CipherValue>"));
		WSSecurityException e4 = assertThrows(WSSecurityException.class, () -> SoapUtils.decryptMessage(manipulatedMessage, keystore, certificateName, "changeit", false));
		assertEquals("unable to process security header", e4.getMessage());
		assertNotNull(e3.getCause());
		// Cause trace logs: "The signature or decryption was invalid"

		// Swap the first 4 characters of the CipherValue
		Message manipulatedMessage2 = new Message(encrypted.asString()
				.replaceAll("<xenc:CipherValue>(.{4})(.{4})(.*)</xenc:CipherValue>", "<xenc:CipherValue>$2$1$3</xenc:CipherValue>"));
		assertNotEquals(encrypted.asString(), manipulatedMessage2.asString());

		Exception e5 = assertThrows(Exception.class, () -> SoapUtils.decryptMessage(manipulatedMessage2, keystore, certificateName, "changeit", false));
		assertInstanceOf(WSSecurityException.class, e5, "expected a WSSecurityException but got: (%s): %s".formatted(e5.getClass(), e5.getMessage()));

		Message resultMessage = SoapUtils.decryptMessage(new UrlMessage(file), keystore, certificateName, "changeit", false);
		MatchUtils.assertXmlEquals(StreamUtil.resourceToString(file), resultMessage.asString());
	}

	// Reduce overhead when message is already of type SOAP
	@Test
	public void testMultipleToSoapCalls() throws SOAPException, IOException {
		URL file = TestFileUtils.getTestFileURL("/Soap/Encryption/SZeebraSoap.xml");
		assertNotNull(file); // ensure we can find the file

		Message soapMessage = new UrlMessage(file);
		// soapMessage.getContext().get(SoapContext.SOAP_VERSION_KEY);
		final Document doc = SoapUtils.toSoapDocument(soapMessage);
		Message soapDocumentMessage = new Message(doc);
		final Document doc2 = SoapUtils.toSoapDocument(soapDocumentMessage);

		assertEquals(doc, doc2, "expected both instances to be the same");
	}

	@ParameterizedTest
	@CsvSource({"true, true", "true, false", "false, true", "false, false"})
	void validateSignedSoap1_1(boolean includeCertificateInMessage, boolean removeSecurityHeader) throws Exception {
		URL file = TestFileUtils.getTestFileURL("/Soap/Encryption/SZeebraSoap.xml");
		assertNotNull(file); // ensure we can find the file

		String certificateName = "tralalal";
		String certificatePass = "tralalal";
		KeyStore keystore = createDummyKeyStoreWithNullKeyPassword(certificateName, certificatePass);

		Message encrypted = SoapUtils.signMessage(new UrlMessage(file), keystore, certificateName, certificatePass, includeCertificateInMessage, SoapUtils.KeyIdentifierType.ISSUER_SERIAL, SoapUtils.DigestAlgorithm.SHA1, SoapUtils.SignatureAlgorithm.RSA_SHA1);

		String encryptedString = encrypted.asString()
				.replaceAll("<ds:SignatureValue>.*?</ds:SignatureValue>", "<ds:SignatureValue>IGNORE-SIGNATURE-VALUE</ds:SignatureValue>")
				.replaceAll("<ds:DigestValue>.*?</ds:DigestValue>", "<ds:DigestValue>IGNORE-DIGEST-VALUE</ds:DigestValue>")
				.replaceAll("<wsu:Created>.*?</wsu:Created>", "<wsu:Created>IGNORE-CREATED</wsu:Created>")
				.replaceAll("<wsu:Expires>.*?</wsu:Expires>", "<wsu:Expires>IGNORE-EXPIRES</wsu:Expires>")
				.replaceAll("(Id=\")[^\"]*\"", "Id=\"id-here\"")
				.replaceAll("(URI=\")[^\"]*\"", "URI=\"uri-here\"")
				.replaceAll("(<wsse:BinarySecurityToken[^>]*>)[^<]*(</wsse:BinarySecurityToken>)", "$1IGNORE-BST$2")
				.replaceAll("(<ds:X509SerialNumber[^>]*>)[^<]*(</ds:X509SerialNumber>)", "$1IGNORE-KI$2");

		URL expectedFile = TestFileUtils.getTestFileURL("/Soap/Encryption/SZeebraSoap-signed-"+(includeCertificateInMessage ? "with" : "no")+"cert.xml");
		assertNotNull(expectedFile); // ensure we can find the file
		MatchUtils.assertXmlEquals(StreamUtil.resourceToString(expectedFile), encryptedString);

		Message decrypted = SoapUtils.verifyMessage(encrypted, keystore, certificateName, certificatePass, removeSecurityHeader);
		String decryptedString = decrypted.asString()
				.replaceAll("(<env:Body )[^<]*(>)", "$1$2");
		// Ensure the decrypted result is the same as the initial document
		String originalEncryptedMessage = encrypted.asString()
				.replaceAll("(<env:Body )[^<]*(>)", "$1$2");
		// If removeSecurityHeader==true the input should match the output. Else it should contain the wsse header
		MatchUtils.assertXmlEquals(removeSecurityHeader ? StreamUtil.resourceToString(file) : originalEncryptedMessage, decryptedString);
	}

	@Test
	void validateSignedEncryptedSoap() throws Exception {
		URL file = TestFileUtils.getTestFileURL("/Soap/Encryption/SZeebraSoap.xml");
		assertNotNull(file); // ensure we can find the file

		String certificateName = "myCustomCertificateName";
		String certificatePass = "Super$3cure";
		KeyStore keystore = createDummyKeyStoreWithNullKeyPassword(certificateName, certificatePass);

		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey secretKey = keyGen.generateKey();

		Message encrypted = SoapUtils.encryptMessage(new UrlMessage(file), keystore, certificateName, secretKey, true,
				SoapUtils.KeyIdentifierType.THUMBPRINT_IDENTIFIER, SoapUtils.DigestAlgorithm.SHA1, SoapUtils.KeyEncryptionAlgorithm.RSA_OAEP, SoapUtils.DataEncryptionAlgorithm.AES_256);

		Message signed = SoapUtils.signMessage(encrypted, keystore, certificateName, certificatePass, true, SoapUtils.KeyIdentifierType.THUMBPRINT_IDENTIFIER, SoapUtils.DigestAlgorithm.SHA1, SoapUtils.SignatureAlgorithm.RSA_SHA1);

		Message unsigned = SoapUtils.verifyMessage(signed, keystore, certificateName, certificatePass, false);
		Message decrypted = SoapUtils.decryptMessage(unsigned, keystore, certificateName, certificatePass, true);

		String decryptedString = decrypted.asString().replaceAll("(<env:Body )[^<]*(>)", "$1$2");
		// Ensure the decrypted result is the same as the initial document
		MatchUtils.assertXmlEquals(StreamUtil.resourceToString(file), decryptedString);
	}
}
