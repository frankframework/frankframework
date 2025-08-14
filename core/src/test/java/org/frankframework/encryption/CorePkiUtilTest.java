package org.frankframework.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import org.frankframework.util.TimeProvider;

@Isolated("Tests manipulate current time, so should not be run concurrently with other tests")
public class CorePkiUtilTest {
	private static final String MULTI_KEY_KEYSTORE = "Encryption/MultiKeyKeystore.jks";

	@AfterEach
	public void tearDown() {
		TimeProvider.resetClock();
	}

	@Nonnull
	private static ZonedDateTime getTimeBeforeCertificateExpiryCheckWindow() {
		// NB: Test-certificate expires on 2025-06-08, expiry-check window is 31 days
		return ZonedDateTime.of(2025, 4, 15, 10, 0, 0, 0, ZoneId.systemDefault());
	}

	@Nonnull
	private static ZonedDateTime getTimeWhenCertificateSoonToExpire() {
		// NB: Test-certificate expires on 2025-06-08, expiry-check window is 31 days
		return ZonedDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneId.systemDefault());
	}

	@Nonnull
	private static ZonedDateTime getTimeAfterCertificateExpiryDate() {
		// NB: Test-certificate expires on 2025-06-08
		return ZonedDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneId.systemDefault());
	}

	@Test
	public void testGetPrivateKeyMultiKeyKeyStoreAlias1() throws EncryptionException {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias("alias1");
		keystoreOwner.setKeystoreAliasPassword("AliasPW1");
		PrivateKey privateKey = CorePkiUtil.getPrivateKey(keystoreOwner);
		assertNotNull(privateKey);
	}

	@Test
	public void testGetPublicKeyMultiKeyKeyStoreAlias1() throws EncryptionException {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias("alias1");
		keystoreOwner.setKeystoreAliasPassword("AliasPW1");
		PublicKey publicKey = CorePkiUtil.getPublicKey(CorePkiUtil.keyStoreAsTrustStore(keystoreOwner));
		assertNotNull(publicKey);
	}

	@Test
	public void testExpiredCertificateFromJKS() throws Exception {
		TimeProvider.setTime(getTimeAfterCertificateExpiryDate());

		KeystoreOwner keystoreOwner = new KeystoreOwner("Encryption/expiredCert.jks");
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("changeit");
		keystoreOwner.setKeystoreAlias("common-name");
		keystoreOwner.setKeystoreAliasPassword("changeme");

		KeyStore keystore = CorePkiUtil.createKeyStore(keystoreOwner);
		List<String> keystoreAliasses = CorePkiUtil.getExpiringCertificates(keystore, Duration.ofDays(31L));
		assertEquals(1, keystoreAliasses.size());

		KeyStore truststore = CorePkiUtil.createKeyStore(CorePkiUtil.keyStoreAsTrustStore(keystoreOwner));
		List<String> truststoreAliasses = CorePkiUtil.getExpiringCertificates(truststore, Duration.ofDays(31L));
		assertEquals(1, truststoreAliasses.size());
	}

	@Test
	public void testExpiredCertificateFromPKCS12() throws Exception {
		TimeProvider.setTime(getTimeAfterCertificateExpiryDate());

		KeystoreOwner keystoreOwner = new KeystoreOwner("Encryption/common_name.p12");
		keystoreOwner.setKeystoreType(KeystoreType.PKCS12);
		keystoreOwner.setKeystorePassword("changeit");

		KeyStore keystore = CorePkiUtil.createKeyStore(keystoreOwner);
		List<String> keystoreAliasses = CorePkiUtil.getExpiringCertificates(keystore, Duration.ofDays(31L));
		assertEquals(1, keystoreAliasses.size());

		KeyStore truststore = CorePkiUtil.createKeyStore(CorePkiUtil.keyStoreAsTrustStore(keystoreOwner));
		List<String> truststoreAliasses = CorePkiUtil.getExpiringCertificates(truststore, Duration.ofDays(31L));
		assertEquals(1, truststoreAliasses.size());
	}

	@Test
	public void testSoonToExpireCertificateFromJKS() throws Exception {
		TimeProvider.setTime(getTimeWhenCertificateSoonToExpire());

		KeystoreOwner keystoreOwner = new KeystoreOwner("Encryption/expiredCert.jks");
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("changeit");
		keystoreOwner.setKeystoreAlias("common-name");
		keystoreOwner.setKeystoreAliasPassword("changeme");

		KeyStore keystore = CorePkiUtil.createKeyStore(keystoreOwner);
		List<String> keystoreAliasses = CorePkiUtil.getExpiringCertificates(keystore, Duration.ofDays(31L));
		assertEquals(1, keystoreAliasses.size());

		KeyStore truststore = CorePkiUtil.createKeyStore(CorePkiUtil.keyStoreAsTrustStore(keystoreOwner));
		List<String> truststoreAliasses = CorePkiUtil.getExpiringCertificates(truststore, Duration.ofDays(31L));
		assertEquals(1, truststoreAliasses.size());
	}

	@Test
	public void testSoonToExpireCertificateFromPKCS12() throws Exception {
		TimeProvider.setTime(getTimeWhenCertificateSoonToExpire());

		KeystoreOwner keystoreOwner = new KeystoreOwner("Encryption/common_name.p12");
		keystoreOwner.setKeystoreType(KeystoreType.PKCS12);
		keystoreOwner.setKeystorePassword("changeit");

		KeyStore keystore = CorePkiUtil.createKeyStore(keystoreOwner);
		List<String> keystoreAliasses = CorePkiUtil.getExpiringCertificates(keystore, Duration.ofDays(31L));
		assertEquals(1, keystoreAliasses.size());

		KeyStore truststore = CorePkiUtil.createKeyStore(CorePkiUtil.keyStoreAsTrustStore(keystoreOwner));
		List<String> truststoreAliasses = CorePkiUtil.getExpiringCertificates(truststore, Duration.ofDays(31L));
		assertEquals(1, truststoreAliasses.size());
	}

	@Test
	public void testNoExpiredCertificatesFromJKS() throws Exception {
		TimeProvider.setTime(getTimeBeforeCertificateExpiryCheckWindow());

		KeystoreOwner keystoreOwner = new KeystoreOwner("Encryption/expiredCert.jks");
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("changeit");
		keystoreOwner.setKeystoreAlias("common-name");
		keystoreOwner.setKeystoreAliasPassword("changeme");

		KeyStore keystore = CorePkiUtil.createKeyStore(keystoreOwner);
		List<String> keystoreAliasses = CorePkiUtil.getExpiringCertificates(keystore, Duration.ofDays(31L));
		assertEquals(0, keystoreAliasses.size());

		KeyStore truststore = CorePkiUtil.createKeyStore(CorePkiUtil.keyStoreAsTrustStore(keystoreOwner));
		List<String> truststoreAliasses = CorePkiUtil.getExpiringCertificates(truststore, Duration.ofDays(31L));
		assertEquals(0, truststoreAliasses.size());
	}

	@Test
	public void testNoExpiredCertificateFromPKCS12() throws Exception {
		TimeProvider.setTime(getTimeBeforeCertificateExpiryCheckWindow());

		KeystoreOwner keystoreOwner = new KeystoreOwner("Encryption/common_name.p12");
		keystoreOwner.setKeystoreType(KeystoreType.PKCS12);
		keystoreOwner.setKeystorePassword("changeit");

		KeyStore keystore = CorePkiUtil.createKeyStore(keystoreOwner);
		List<String> keystoreAliasses = CorePkiUtil.getExpiringCertificates(keystore, Duration.ofDays(31L));
		assertEquals(0, keystoreAliasses.size());

		KeyStore truststore = CorePkiUtil.createKeyStore(CorePkiUtil.keyStoreAsTrustStore(keystoreOwner));
		List<String> truststoreAliasses = CorePkiUtil.getExpiringCertificates(truststore, Duration.ofDays(31L));
		assertEquals(0, truststoreAliasses.size());
	}
}
