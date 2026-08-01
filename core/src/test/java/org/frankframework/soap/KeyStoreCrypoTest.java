package org.frankframework.soap;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.xml.security.algorithms.JCEMapper;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.encryption.KeystoreType;
import org.frankframework.lifecycle.LoadBouncyCastleBean;

@Log4j2
public class KeyStoreCrypoTest {
	private static KeyPairGenerator KEY_GENERATOR;

	@BeforeAll
	static void setup() throws NoSuchAlgorithmException {
		// Load BouncyCastle if not already set.
		new LoadBouncyCastleBean().afterPropertiesSet();
		WSSConfig.init();
		JCEMapper.registerDefaultAlgorithms();

		KEY_GENERATOR = KeyPairGenerator.getInstance("RSA");
		KEY_GENERATOR.initialize(2048);
	}

//	private KeyStore createDummyKeyStoreWithNullKeyPassword(String certificateName, String certificatePassword) throws Exception {
//		KeyStore ks = KeyStore.getInstance(KeystoreType.PKCS12.name(), "SUN");
//		ks.load(null, "password".toCharArray());
//		ks.setKeyEntry(certificateName, keyPair.getPrivate(), certificatePassword.toCharArray(), new Certificate[] { cert } );
//		return ks;
//	}

	private X509Certificate createCertificate(String issuer, String subject, KeyPair keyPair) throws Exception {
		BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
		Instant validFrom = Instant.now();
		Instant validTo = validFrom.plus(365, ChronoUnit.DAYS);

		JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
				new X500Name(issuer),
				serial,
				Date.from(validFrom),
				Date.from(validTo),
				new X500Name(subject),
				keyPair.getPublic()
		);
		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
		return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(signer));
	}

	@Test
	void getCertificateFactory() throws Exception {
		KeyStore keystore = KeyStore.getInstance(KeystoreType.PKCS12.name(), "SUN");
		keystore.load(null, "password".toCharArray());

		KeyStoreCrypto ksCrypto = new KeyStoreCrypto(keystore);
		CertificateFactory certFactory = ksCrypto.getCertificateFactory();
		Assertions.assertNotNull(certFactory);

		// Both keystore and the crypto implementation should use the same provider
		Assertions.assertEquals("SUN", keystore.getProvider().getName());
		Assertions.assertEquals("SUN", certFactory.getProvider().getName());

		// We always expect the X509 type
		Assertions.assertEquals("X.509", certFactory.getType());

		Assertions.assertEquals(certFactory, ksCrypto.getCertificateFactory(), "should get the same factory twice");
	}

	@Test
	void getDefaultCertIdentifier() throws Exception {
		// Arrange
		String certificateAlias = "myTestCertAlias";

		KeyPair keyPair = KEY_GENERATOR.generateKeyPair();
		X509Certificate certificate = createCertificate("CN=Issuer, OU=Test, O=Test, L=Test, C=US", "CN=Subject, OU=Test, O=Test, L=Test, C=US", keyPair);
		KeyStore keystore = KeyStore.getInstance(KeystoreType.PKCS12.name(), "SUN");
		keystore.load(null, "password".toCharArray());
		keystore.setKeyEntry(certificateAlias, keyPair.getPrivate(), "certificatePassword".toCharArray(), new Certificate[] { certificate } );

		// Act
		KeyStoreCrypto ksCrypto = new KeyStoreCrypto(keystore);
		String defaultIdentifier = ksCrypto.getDefaultX509Identifier();

		// Assert
		Assertions.assertNotNull(defaultIdentifier);
		// The cert store is lowercased, so the expected value should be lowercase even though we inserted it with casing.
		Assertions.assertEquals(certificateAlias.toLowerCase(), defaultIdentifier);
	}

	@Test
	void getX509Identifier() throws Exception {
		// Arrange
		KeyStore keystore = KeyStore.getInstance(KeystoreType.PKCS12.name(), "SUN");
		keystore.load(null, "password".toCharArray());

		KeyPair keyPair = KEY_GENERATOR.generateKeyPair();
		X509Certificate certificate = createCertificate("CN=Issuer, OU=Test, O=Test, L=Test, C=US", "CN=Subject, OU=Test, O=Test, L=Test, C=US", keyPair);
		keystore.setKeyEntry("testCertificate", keyPair.getPrivate(), "certificatePassword".toCharArray(), new Certificate[] { certificate } );

		KeyPair keyPair2 = KEY_GENERATOR.generateKeyPair();
		X509Certificate certificate2 = createCertificate("CN=Issuer, OU=Test, O=Test, L=Test, C=US", "CN=Subject, OU=Test, O=Test, L=Test, C=US", keyPair2);
		keystore.setKeyEntry("otherCert", keyPair2.getPrivate(), "certificatePassword2".toCharArray(), new Certificate[] { certificate2 } );
		keystore.setKeyEntry("otherCert-DUPLICATE", keyPair2.getPrivate(), "certificatePassword2".toCharArray(), new Certificate[] { certificate2 } );

		// Act
		KeyStoreCrypto ksCrypto = new KeyStoreCrypto(keystore);

		// Assert
		Assertions.assertEquals("testcertificate", ksCrypto.getX509Identifier(certificate));
		Assertions.assertEquals("othercert", ksCrypto.getX509Identifier(certificate2));
	}

	@Test
	void getPrivateKeyFromCertificate() throws Exception {
		// Arrange
		KeyStore keystore = KeyStore.getInstance(KeystoreType.PKCS12.name(), "SUN");
		keystore.load(null, "password".toCharArray());

		KeyPair keyPair = KEY_GENERATOR.generateKeyPair();
		X509Certificate certificate = createCertificate("CN=Issuer, OU=Test, O=Test, L=Test, C=US", "CN=Subject, OU=Test, O=Test, L=Test, C=US", keyPair);
		keystore.setKeyEntry("testCertificate", keyPair.getPrivate(), "certificatePassword".toCharArray(), new Certificate[] { certificate } );

		// Act
		KeyStoreCrypto ksCrypto = new KeyStoreCrypto(keystore);

		// Assert
		CallbackHandler callback1 = new PasswordCallbackHandler("testcertificate", "certificatePassword");
		Assertions.assertEquals(keyPair.getPrivate(), ksCrypto.getPrivateKey(certificate, callback1));

		// Assert with casing should fail
		CallbackHandler callback2 = new PasswordCallbackHandler("testCertificate", "certificatePassword");
		WSSecurityException e1 = Assertions.assertThrows(WSSecurityException.class, () -> ksCrypto.getPrivateKey(certificate, callback2));
		Assertions.assertEquals("the private key for the supplied alias does not exist in the keystore", e1.getMessage());

		// Assert with wrong password should fail
		CallbackHandler callback3 = new PasswordCallbackHandler("testcertificate", "wrongPassword");
		WSSecurityException e2 = Assertions.assertThrows(WSSecurityException.class, () -> ksCrypto.getPrivateKey(certificate, callback3));
		Assertions.assertEquals("the private key for the supplied alias does not exist in the keystore", e2.getMessage());
	}

	@Test
	void getPrivateKeyFromPublicKey() throws Exception {
		// Arrange
		KeyStore keystore = KeyStore.getInstance(KeystoreType.PKCS12.name(), "SUN");
		keystore.load(null, "password".toCharArray());

		KeyPair keyPair = KEY_GENERATOR.generateKeyPair();
		X509Certificate certificate = createCertificate("CN=Issuer, OU=Test, O=Test, L=Test, C=US", "CN=Subject, OU=Test, O=Test, L=Test, C=US", keyPair);
		keystore.setKeyEntry("testCertificate", keyPair.getPrivate(), "certificatePassword".toCharArray(), new Certificate[] { certificate } );

		// Act
		KeyStoreCrypto ksCrypto = new KeyStoreCrypto(keystore);

		// Assert
		CallbackHandler callback1 = new PasswordCallbackHandler("testcertificate", "certificatePassword");
		Assertions.assertEquals(keyPair.getPrivate(), ksCrypto.getPrivateKey(keyPair.getPublic(), callback1));

		// Assert with casing should fail
		CallbackHandler callback2 = new PasswordCallbackHandler("testCertificate", "certificatePassword");
		WSSecurityException e1 = Assertions.assertThrows(WSSecurityException.class, () -> ksCrypto.getPrivateKey(keyPair.getPublic(), callback2));
		Assertions.assertEquals("the private key for the supplied alias does not exist in the keystore", e1.getMessage());

		// Assert with wrong password should fail
		CallbackHandler callback3 = new PasswordCallbackHandler("testcertificate", "wrongPassword");
		WSSecurityException e2 = Assertions.assertThrows(WSSecurityException.class, () -> ksCrypto.getPrivateKey(keyPair.getPublic(), callback3));
		Assertions.assertEquals("the private key for the supplied alias does not exist in the keystore", e2.getMessage());

		// Assert with different public key should fail
		CallbackHandler callback4 = new PasswordCallbackHandler("testcertificate", "wrongPassword");
		WSSecurityException e3 = Assertions.assertThrows(WSSecurityException.class, () ->
				ksCrypto.getPrivateKey(KEY_GENERATOR.generateKeyPair().getPublic(), callback3));
		Assertions.assertEquals("unable to find private key for corresponding public key", e3.getMessage());
	}

	@Test
	void getPrivateKeyFromIdentifier() throws Exception {
		// Arrange
		KeyStore keystore = KeyStore.getInstance(KeystoreType.PKCS12.name(), "SUN");
		keystore.load(null, "password".toCharArray());

		KeyPair keyPair = KEY_GENERATOR.generateKeyPair();
		X509Certificate certificate = createCertificate("CN=Issuer, OU=Test, O=Test, L=Test, C=US", "CN=Subject, OU=Test, O=Test, L=Test, C=US", keyPair);
		keystore.setKeyEntry("testCertificate", keyPair.getPrivate(), "certificatePassword".toCharArray(), new Certificate[] { certificate } );

		// Act
		KeyStoreCrypto ksCrypto = new KeyStoreCrypto(keystore);

		// Assert
		Assertions.assertEquals(keyPair.getPrivate(), ksCrypto.getPrivateKey("testcertificate", "certificatePassword"));

		// Assert with casing should NOT fail
		Assertions.assertEquals(keyPair.getPrivate(), ksCrypto.getPrivateKey("testCertificate", "certificatePassword"));

		// Assert with wrong password should fail
		WSSecurityException e2 = Assertions.assertThrows(WSSecurityException.class, () -> ksCrypto.getPrivateKey("testcertificate", "wrongPassword"));
		Assertions.assertEquals("the private key for the supplied alias does not exist in the keystore", e2.getMessage());
	}

	@Test
	void verifyTrust() throws Exception {
		// Arrange
		KeyStore keystore = KeyStore.getInstance(KeystoreType.PKCS12.name(), "SUN");
		keystore.load(null, "password".toCharArray());

		KeyPair keyPair = KEY_GENERATOR.generateKeyPair();
		X509Certificate certificate = createCertificate("CN=Issuer, OU=Test, O=Test, L=Test, C=US", "CN=Subject, OU=Test, O=Test, L=Test, C=US", keyPair);
		keystore.setKeyEntry("testCertificate", keyPair.getPrivate(), "certificatePassword".toCharArray(), new Certificate[] { certificate } );

		// Act
		KeyStoreCrypto ksCrypto = new KeyStoreCrypto(keystore);

		// Assert
		WSSecurityException e1 = Assertions.assertThrows(WSSecurityException.class, () -> ksCrypto.verifyTrust(null));
		Assertions.assertEquals("The security token could not be authenticated or authorized", e1.getMessage());

		// Public key exists
		Assertions.assertDoesNotThrow(() -> ksCrypto.verifyTrust(keyPair.getPublic()));

		// Invalid public key
		WSSecurityException e2 = Assertions.assertThrows(WSSecurityException.class, () -> ksCrypto.verifyTrust(KEY_GENERATOR.generateKeyPair().getPublic()));
		Assertions.assertEquals("The security token could not be authenticated or authorized", e2.getMessage());

		// Issuer match
		Assertions.assertDoesNotThrow(() -> ksCrypto.verifyTrust(new X509Certificate[] { certificate }, false, null, null));

		// Trust match
		WSSecurityException e = Assertions.assertThrows(WSSecurityException.class, () -> ksCrypto.verifyTrust(new X509Certificate[] { certificate }, true, null, null));
		Assertions.assertEquals("no trusted certificates found for [C=US,L=Test,O=Test,OU=Test,CN=Subject]", e.getMessage());
	}

	@Test
	void getX509CertificatesByType() throws Exception {
		// Arrange
		KeyStore keystore = KeyStore.getInstance(KeystoreType.PKCS12.name(), "SUN");
		keystore.load(null, "password".toCharArray());

		KeyPair keyPair = KEY_GENERATOR.generateKeyPair();
		X509Certificate certificate = createCertificate("CN=Issuer, OU=Test, O=Test, L=Test, C=US", "CN=Subject, OU=Test, O=Test, L=Test, C=US", keyPair);
		keystore.setKeyEntry("testCertificate", keyPair.getPrivate(), "certificatePassword".toCharArray(), new Certificate[] { certificate } );

		// Act
		KeyStoreCrypto ksCrypto = new KeyStoreCrypto(keystore);

		CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ENDPOINT);
		X509Certificate[] certificates1 = ksCrypto.getX509Certificates(cryptoType);
		Assertions.assertEquals(0, certificates1.length);

		cryptoType.setSubjectDN("CN=Subject, OU=Test, O=Test, L=Test, C=US");
		cryptoType.setAlias("testcertificate");
		cryptoType.setIssuerSerial("CN=Issuer, OU=Test, O=Test, L=Test, C=US", certificate.getSerialNumber());

		// Assert
		cryptoType.setType(CryptoType.TYPE.ALIAS);
		X509Certificate[] certificates2 = ksCrypto.getX509Certificates(cryptoType);
		Assertions.assertEquals(1, certificates2.length);
		Assertions.assertEquals(certificate, certificates2[0]);
		cryptoType.setAlias("dummy");
		Assertions.assertEquals(0, ksCrypto.getX509Certificates(cryptoType).length);

		cryptoType.setType(CryptoType.TYPE.SUBJECT_DN);
		X509Certificate[] certificates3 = ksCrypto.getX509Certificates(cryptoType);
		Assertions.assertEquals(1, certificates3.length);
		Assertions.assertEquals(certificate, certificates3[0]);
		cryptoType.setSubjectDN("CN=DUMMY, OU=Test, O=Test, L=Test, C=US");
		Assertions.assertEquals(0, ksCrypto.getX509Certificates(cryptoType).length);

		System.out.println(certificate.getIssuerX500Principal().getName());
		cryptoType.setType(CryptoType.TYPE.ISSUER_SERIAL);
		X509Certificate[] certificates4 = ksCrypto.getX509Certificates(cryptoType);
		Assertions.assertEquals(1, certificates4.length);
		Assertions.assertEquals(certificate, certificates4[0]);
		cryptoType.setIssuerSerial("CN=DUMMY, OU=Test, O=Test, L=Test, C=US", new BigInteger("0"));
		Assertions.assertEquals(0, ksCrypto.getX509Certificates(cryptoType).length);
	}

	@Test
	void getX509CertificatesByThumbprint() throws Exception {
		// Arrange
		KeyStore keystore = KeyStore.getInstance(KeystoreType.PKCS12.name(), "SUN");
		keystore.load(null, "password".toCharArray());

		KeyPair keyPair = KEY_GENERATOR.generateKeyPair();
		X509Certificate certificate = createCertificate("CN=Issuer, OU=Test, O=Test, L=Test, C=US", "CN=Subject, OU=Test, O=Test, L=Test, C=US", keyPair);
		keystore.setKeyEntry("testCertificate", keyPair.getPrivate(), "certificatePassword".toCharArray(), new Certificate[] { certificate } );

		// Act
		KeyStoreCrypto ksCrypto = new KeyStoreCrypto(keystore);
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] thumbprint = digest.digest(certificate.getEncoded());

		CryptoType cryptoType = new CryptoType(CryptoType.TYPE.THUMBPRINT_SHA1);
		cryptoType.setBytes(thumbprint);

		// Assert
		X509Certificate[] certificates = ksCrypto.getX509Certificates(cryptoType);
		Assertions.assertEquals(1, certificates.length);
		Assertions.assertEquals(certificate, certificates[0]);
	}

	private record PasswordCallbackHandler(String identifier, String password) implements CallbackHandler {

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			for (Callback callback : callbacks) {
				if (callback instanceof WSPasswordCallback pc) {
					if (identifier.equals(pc.getIdentifier())) {
						pc.setPassword(password);
					}
				} else {
					throw new UnsupportedCallbackException(callback, "Unknown Callback");
				}
			}
		}
	}
}
