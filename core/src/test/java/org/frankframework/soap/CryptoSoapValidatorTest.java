package org.frankframework.soap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import lombok.Getter;
import lombok.SneakyThrows;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.encryption.KeystoreConfiguration;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.lifecycle.LoadBouncyCastleBean;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.validation.ValidatorTestBase;

class CryptoSoapValidatorTest extends PipeTestBase<CryptoSoapValidator> {
	private static final URL TEST_FILE = TestFileUtils.getTestFileURL(ValidatorTestBase.BASE_DIR_VALIDATION + "/Basic/in/ok-in-soap-envelope.xml");

	@Override
	@SneakyThrows(ConfigurationException.class)
	public CryptoSoapValidator createPipe() {
		CryptoSoapValidator pipe = spy(new CryptoSoapValidator());
		doAnswer(i -> {
			KeystoreConfiguration keystoreConfiguration = i.getArgument(0);
			if (keystoreConfiguration instanceof KeyStoreWrapper ks) {
				return ks.getKeystore();
			}
			throw new ConfigurationException("KeystoreConfiguration is not of type KeyStoreWrapper");
		}).when(pipe).createKeyStore(any(KeystoreConfiguration.class));
		return pipe;
	}

	@Test
	void withoutKeystore() {
		pipe.setOperations(CryptoSoapValidator.Operation.DECRYPT);

		ConfigurationException ex = assertThrows(ConfigurationException.class, this::configurePipe);
		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("element [Keystore] must be specified"));
	}

	@Test
	void withKeystore() throws Exception {
		pipe.setOperations(CryptoSoapValidator.Operation.DECRYPT);

		KeyStore keystore = createDummyKeyStoreWithNullKeyPassword("certificateName", "certificatePass");
		pipe.setKeystoreConfiguration(new KeyStoreWrapper(keystore));

		ConfigurationException ex = assertThrows(ConfigurationException.class, this::configurePipe);
		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("attribute [keystoreAlias] (the name of the certificate) must be specified"));
	}

	@Test
	void mixedMode() throws Exception {
		pipe.setOperations(CryptoSoapValidator.Operation.DECRYPT);
		pipe.setSchemaLocation("bla bla");
		pipe.setResponseRoot("response");

		KeyStore keystore = createDummyKeyStoreWithNullKeyPassword("certificateName", "certificatePass");
		pipe.setKeystoreConfiguration(new KeyStoreWrapper(keystore));

		pipe.setKeystoreAlias("dummy");
		pipe.setThrowException(true);
		pipe.setSchemaLocation("http://www.ing.com/testxmlns " + ValidatorTestBase.BASE_DIR_VALIDATION + "/Basic/xsd/A_correct.xsd");

		ConfigurationException ex = assertThrows(ConfigurationException.class, this::configurePipe);
		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("this validator does not support input/output processing in '1' element, configure an OutputValidator explicitly"));
	}

	@Test
	void withKeystoreAndCert() throws Exception {
		pipe.setOperations(CryptoSoapValidator.Operation.DECRYPT);

		String certificateName = "myCustomCertificateName";
		String certificatePass = "Super$3cure";
		KeyStore keystore = createDummyKeyStoreWithNullKeyPassword(certificateName, certificatePass);
		pipe.setKeystoreConfiguration(new KeyStoreWrapper(keystore));
		pipe.setKeystoreAlias(certificateName);
		pipe.setKeystoreAliasPassword(certificatePass);

		pipe.setThrowException(true);
		pipe.setAllowPlainXml(true);
		pipe.setSchemaLocation("http://www.ing.com/testxmlns " + ValidatorTestBase.BASE_DIR_VALIDATION + "/Basic/xsd/A_correct.xsd");
		pipe.setSoapVersion(SoapVersion.AUTO);

		assertDoesNotThrow(this::configureAndStartPipe);

		Message encrypted = createEncryptedSoapMessage(keystore, certificateName);
		assertTrue(encrypted.asString().contains("<SOAP:Header>"));

		PipeRunResult result = doPipe(encrypted);
		assertNotNull(result);

		String expected = new UrlMessage(TEST_FILE).asString();

		// The result contains an empty SOAP header, it used to contain the soap security header, but that was removed by the validator.
		assertTrue(result.getResult().asString().contains("<SOAP:Header/>"));
		String stringResult = result.getResult().asString().replace("<SOAP:Header/>", "");

		TestAssertions.assertEqualsIgnoreCRLF(expected, stringResult);
	}

	@Test
	void testSignOperation() throws Exception {
		pipe.setOperations(CryptoSoapValidator.Operation.SIGN);

		String certificateName = "myCustomCertificateName";
		String certificatePass = "Super$3cure";
		KeyStore keystore = createDummyKeyStoreWithNullKeyPassword(certificateName, certificatePass);
		pipe.setKeystoreConfiguration(new KeyStoreWrapper(keystore));
		pipe.setKeystoreAlias(certificateName);
		pipe.setKeystoreAliasPassword(certificatePass);

		pipe.setThrowException(true);
		pipe.setAllowPlainXml(true);
		pipe.setSchemaLocation("http://www.ing.com/testxmlns " + ValidatorTestBase.BASE_DIR_VALIDATION + "/Basic/xsd/A_correct.xsd");
		pipe.setSoapVersion(SoapVersion.AUTO);

		assertDoesNotThrow(this::configureAndStartPipe);

		PipeRunResult result = doPipe(new UrlMessage(TEST_FILE));
		assertNotNull(result);

		assertTrue(result.getResult().asString().contains("<SOAP:Header>"));
	}

	private Message createEncryptedSoapMessage(KeyStore keystore, String certificateName) throws Exception {
		assertNotNull(TEST_FILE); // ensure we can find the file

		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey secretKey = keyGen.generateKey();

		return SoapUtils.encryptMessage(new UrlMessage(TEST_FILE), keystore, certificateName, secretKey, true,
				SoapUtils.KeyIdentifierType.THUMBPRINT_SHA1, SoapUtils.DigestAlgorithm.SHA1, SoapUtils.KeyEncryptionAlgorithm.RSA_OAEP, SoapUtils.DataEncryptionAlgorithm.AES_256, 300);
	}

	public static class KeyStoreWrapper extends KeystoreConfiguration {
		private final @Getter KeyStore keystore;
		public KeyStoreWrapper(KeyStore keystore) {
			this.keystore = keystore;
		}

		@Override
		public String getResource() {
			return "inline-keystore";
		}
	}

	private static KeyStore createDummyKeyStoreWithNullKeyPassword(String certificateName, String certificatePassword) throws Exception {
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
}
