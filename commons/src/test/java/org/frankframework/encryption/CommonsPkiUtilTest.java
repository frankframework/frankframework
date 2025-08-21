package org.frankframework.encryption;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonsPkiUtilTest {

	private static final String DUMMY_PW = "dummyPw";
	private static final String DUMMY_ALIAS = "dummyAlias";
	private KeyStore keyStore;

	@BeforeAll
	static void beforeAll() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@BeforeEach
	void setUp() throws Exception {
		keyStore = createDummyKeyStore();
	}

	private KeyStore createDummyKeyStore() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keyPair = keyGen.generateKeyPair();
		PrivateKey privateKey = keyPair.getPrivate();

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
		ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);
		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(signer));

		KeyStore ks = KeyStore.getInstance(KeystoreType.PKCS12.name());
		ks.load(null, DUMMY_PW.toCharArray());
		ks.setKeyEntry(DUMMY_ALIAS, privateKey, DUMMY_PW.toCharArray(), new Certificate[]{ cert });

		return ks;
	}

	private KeyStore createDummyKeyStoreWithNullKeyPassword() throws Exception {
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
		ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(signer));

		KeyStore ks = KeyStore.getInstance(KeystoreType.PKCS12.name());
		ks.load(null, DUMMY_PW.toCharArray());
		ks.setKeyEntry(DUMMY_ALIAS, keyPair.getPrivate(), null, new Certificate[]{ cert });
		return ks;
	}


	private InputStream toInputStream(KeyStore ks) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ks.store(bos, DUMMY_PW.toCharArray());
		return new ByteArrayInputStream(bos.toByteArray());
	}

	@Test
	void testCreateKeyStoreSuccess() throws Exception {
		URL url = mock(URL.class);
		when(url.openStream()).thenReturn(toInputStream(keyStore));

		KeyStore result = CommonsPkiUtil.createKeyStore(url, DUMMY_PW, KeystoreType.PKCS12);

		assertNotNull(result);
		assertTrue(result.containsAlias(DUMMY_ALIAS));
		assertEquals("PKCS12", result.getType());
	}

	@Test
	void testCreateKeyStoreWithNullUrl() {
		IllegalArgumentException e = assertThrows(
				IllegalArgumentException.class, () ->
						CommonsPkiUtil.createKeyStore(null, DUMMY_PW, KeystoreType.PKCS12)
		);
		assertEquals("Keystore url may not be null", e.getMessage());
	}

	@Test
	void testCreateKeyStoreIOException() throws Exception {
		URL url = mock(URL.class);
		when(url.openStream()).thenThrow(new IOException("Failed to open stream"));

		IOException e = assertThrows(
				IOException.class, () ->
						CommonsPkiUtil.createKeyStore(url, DUMMY_PW, KeystoreType.PKCS12)
		);
		assertEquals("Failed to open stream", e.getMessage());
	}

	@Test
	void testCreateKeyStoreCorrupted() throws Exception {
		URL url = mock(URL.class);
		InputStream corruptedStream = new ByteArrayInputStream("corrupted data".getBytes());
		when(url.openStream()).thenReturn(corruptedStream);

		assertThrows(
				IOException.class, () ->
						CommonsPkiUtil.createKeyStore(url, DUMMY_PW, KeystoreType.PKCS12)
		);
	}

	@Test
	void testCreateKeyManagersSuccess() {
		KeyManager[] keyManagers = assertDoesNotThrow(() ->
				CommonsPkiUtil.createKeyManagers(keyStore, DUMMY_PW, null)
		);
		assertNotNull(keyManagers);
		assertTrue(keyManagers.length > 0);
	}

	@Test
	void testCreateKeyManagersWithSpecificAlgorithm() {
		KeyManager[] keyManagers = assertDoesNotThrow(() ->
				CommonsPkiUtil.createKeyManagers(keyStore, DUMMY_PW, "SunX509")
		);
		assertNotNull(keyManagers);
		assertTrue(keyManagers.length > 0);
	}

	@Test
	void testCreateKeyManagersWithNullKeystore() {
		IllegalArgumentException e = assertThrows(
				IllegalArgumentException.class, () ->
						CommonsPkiUtil.createKeyManagers(null, DUMMY_PW, null)
		);
		assertEquals("Keystore may not be null", e.getMessage());
	}

	@Test
	void testCreateKeyManagersUnrecoverableKey() throws Exception {
		KeyStore realStore = createDummyKeyStore();
		UnrecoverableKeyException e = assertThrows(
				UnrecoverableKeyException.class, () ->
						CommonsPkiUtil.createKeyManagers(realStore, "wrongPassword", null)
		);
		assertNotNull(e);
	}

	@Test
	void testCreateTrustManagersSuccess() {
		TrustManager[] trustManagers = assertDoesNotThrow(() ->
				CommonsPkiUtil.createTrustManagers(keyStore, null)
		);
		assertNotNull(trustManagers);
		assertTrue(trustManagers.length > 0);
	}

	@Test
	void testCreateTrustManagersWithSpecificAlgorithm() {
		TrustManager[] trustManagers = assertDoesNotThrow(() ->
				CommonsPkiUtil.createTrustManagers(keyStore, "PKIX")
		);
		assertNotNull(trustManagers);
		assertTrue(trustManagers.length > 0);
	}

	@Test
	void testCreateTrustManagersWithNullKeystore() {
		IllegalArgumentException e = assertThrows(
				IllegalArgumentException.class, () ->
						CommonsPkiUtil.createTrustManagers(null, null)
		);
		assertEquals("Keystore may not be null", e.getMessage());
	}

	@Test
	void testCreateTrustManagersNoSuchAlgorithm() {
		assertThrows(
				NoSuchAlgorithmException.class, () ->
						CommonsPkiUtil.createTrustManagers(keyStore, "InvalidAlgorithm")
		);
	}

	@Test
	void testGetRsaPrivateKeySuccess() throws Exception {
		KeyStore ks = createDummyKeyStoreWithNullKeyPassword();
		var rsaKey = assertDoesNotThrow(() -> CommonsPkiUtil.getRsaPrivateKey(ks));
		assertNotNull(rsaKey);
		assertEquals("RSA", rsaKey.getAlgorithm());
	}

	@Test
	void testGetRsaPrivateKeyWithNullKeystore() {
		IllegalArgumentException e = assertThrows(
				IllegalArgumentException.class,
				() -> CommonsPkiUtil.getRsaPrivateKey(null)
		);
		assertEquals("Keystore may not be null", e.getMessage());
	}

	@Test
	void testGetRsaPrivateKeyWhenNoKeyEntries() throws Exception {
		KeyStore ks = KeyStore.getInstance(KeystoreType.PKCS12.name());
		ks.load(null, DUMMY_PW.toCharArray());

		Certificate cert = keyStore.getCertificate(DUMMY_ALIAS);
		ks.setCertificateEntry("trustedCert", cert);

		KeyStoreException e = assertThrows(
				KeyStoreException.class,
				() -> CommonsPkiUtil.getRsaPrivateKey(ks)
		);
		assertEquals("Expected exactly one key entry, found 0", e.getMessage());
	}

	@Test
	void testGetRsaPrivateKeyWhenMultipleKeyEntries() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1024);
		KeyPair keyPair = keyGen.generateKeyPair();
		PrivateKey privateKey2 = keyPair.getPrivate();

		X500Name owner = new X500Name("CN=Test2");
		BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
		Instant now = Instant.now();
		JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
				owner, serial,
				Date.from(now),
				Date.from(now.plus(365, ChronoUnit.DAYS)),
				owner,
				keyPair.getPublic()
		);
		ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey2);
		X509Certificate cert2 = new JcaX509CertificateConverter()
				.setProvider("BC")
				.getCertificate(certBuilder.build(signer));

		keyStore.setKeyEntry("secondAlias", privateKey2, DUMMY_PW.toCharArray(), new Certificate[]{ cert2 });

		KeyStoreException e = assertThrows(
				KeyStoreException.class,
				() -> CommonsPkiUtil.getRsaPrivateKey(keyStore)
		);
		assertTrue(e.getMessage().contains("Expected exactly one key entry, found 2"));
	}

	@Test
	void testGetRsaPrivateKeyWithWrongKeyType() throws Exception {
		KeyStore ks = KeyStore.getInstance(KeystoreType.PKCS12.name());
		ks.load(null, DUMMY_PW.toCharArray());

		javax.crypto.SecretKey secretKey =
				new javax.crypto.spec.SecretKeySpec("1234567890123456".getBytes(), "AES");
		ks.setKeyEntry("secretAlias", secretKey, null, null);

		UnrecoverableKeyException e = assertThrows(
				UnrecoverableKeyException.class,
				() -> CommonsPkiUtil.getRsaPrivateKey(ks)
		);
		assertTrue(e.getMessage().contains("is not an RSAPrivateKey entry"));
	}

}
