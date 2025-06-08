package org.frankframework.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

public class PkiUtilTest {
	private final String MULTI_KEY_KEYSTORE = "Encryption/MultiKeyKeystore.jks";

	@Test
	public void testGetPrivateKeyMultiKeyKeyStoreAlias1() throws EncryptionException {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias("alias1");
		keystoreOwner.setKeystoreAliasPassword("AliasPW1");
		PrivateKey privateKey = PkiUtil.getPrivateKey(keystoreOwner);
		assertNotNull(privateKey);
	}

	@Test
	public void testGetPublicKeyMultiKeyKeyStoreAlias1() throws EncryptionException {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias("alias1");
		keystoreOwner.setKeystoreAliasPassword("AliasPW1");
		PublicKey publicKey = PkiUtil.getPublicKey(PkiUtil.keyStoreAsTrustStore(keystoreOwner));
		assertNotNull(publicKey);
	}

	@Test
	public void testExpiredCertificateFromJKS() throws Exception {
		KeystoreOwner keystoreOwner = new KeystoreOwner("Encryption/expiredCert.jks");
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("changeit");
		keystoreOwner.setKeystoreAlias("common-name");
		keystoreOwner.setKeystoreAliasPassword("changeme");

		KeyStore keystore = PkiUtil.createKeyStore(keystoreOwner);
		List<String> keystoreAliasses = PkiUtil.getExpiringCertificates(keystore, Duration.ofDays(31L));
		assertEquals(1, keystoreAliasses.size());

		KeyStore truststore = PkiUtil.createKeyStore(PkiUtil.keyStoreAsTrustStore(keystoreOwner));
		List<String> truststoreAliasses = PkiUtil.getExpiringCertificates(truststore, Duration.ofDays(31L));
		assertEquals(1, truststoreAliasses.size());
	}

	@Test
	public void testExpiredCertificateFromPKCS12() throws Exception {
		KeystoreOwner keystoreOwner = new KeystoreOwner("Encryption/common_name.p12");
		keystoreOwner.setKeystoreType(KeystoreType.PKCS12);
		keystoreOwner.setKeystorePassword("changeit");

		KeyStore keystore = PkiUtil.createKeyStore(keystoreOwner);
		List<String> keystoreAliasses = PkiUtil.getExpiringCertificates(keystore, Duration.ofDays(31L));
		assertEquals(1, keystoreAliasses.size());

		KeyStore truststore = PkiUtil.createKeyStore(PkiUtil.keyStoreAsTrustStore(keystoreOwner));
		List<String> truststoreAliasses = PkiUtil.getExpiringCertificates(truststore, Duration.ofDays(31L));
		assertEquals(1, truststoreAliasses.size());
	}
}
