package org.frankframework.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
	public void testExpiredCertificate() throws Exception {
		KeystoreOwner keystoreOwner = new KeystoreOwner("Encryption/expiredCert.jks");
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("changeit");
		keystoreOwner.setKeystoreAlias("common-name");
		keystoreOwner.setKeystoreAliasPassword("changeme");

		List<String> aliasses = PkiUtil.getExpiringCertificates(PkiUtil.keyStoreAsTrustStore(keystoreOwner), Duration.ofDays(31L));

		assertEquals(1, aliasses.size());
	}
}
