package org.frankframework.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.frankframework.testutil.junit.WithTimeTravel;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.TimeProvider;

@Isolated
@WithTimeTravel
public class CorePkiUtilTest {
	private static final String MULTI_KEY_KEYSTORE = "Encryption/MultiKeyKeystore.jks";
	private static final String ALIAS1_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjWvHNu00TJBPsbMQxRHBvhV6PIDvWC4nBVM29DBZXi/xvSDQYMwCcTRSzH+Iic7Wb0diQJS59OB8aROeya0W1Wl6XSqG890K/gpKpkchSJvbOyBJs9IPOjk6eweQbD+EjM6gK8MGuYjWdwZq681swWlDrjwYv4rakdwQytWIfziS2ZxPKuvnpP/U9Hs0c66zlb9JcsNG+mD9njUJqH1PmpXIpA3dha69DHaOVx8qzxQB2AvcptrcioKSJ/MmfWUV1pvmfVwe2NoDAX/jijhPK0VdoiUs1t7yvv7asBdz6SpcWWiWsFQBPHETAzLhFfIBCceh2qrMY2uccbmNAHZsAwIDAQAB";

	@NonNull
	private static ZonedDateTime getTimeBeforeCertificateExpiryCheckWindow() {
		// NB: Test-certificate expires on 2025-06-08, expiry-check window is 31 days
		return ZonedDateTime.of(2025, 4, 15, 10, 0, 0, 0, ZoneId.systemDefault());
	}

	@NonNull
	private static ZonedDateTime getTimeWhenCertificateSoonToExpire() {
		// NB: Test-certificate expires on 2025-06-08, expiry-check window is 31 days
		return ZonedDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneId.systemDefault());
	}

	@NonNull
	private static ZonedDateTime getTimeAfterCertificateExpiryDate() {
		// NB: Test-certificate expires on 2025-06-08
		return ZonedDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneId.systemDefault());
	}

	@ParameterizedTest
	@CsvSource({"alias1, AliasPW1, -1314909411", "alias2, AliasPW2, -1445044487"})
	public void testGetPrivateKeyMultiKeyKeyStoreAlias1(String alias, String password, int privateExponent) throws EncryptionException {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias(alias);
		keystoreOwner.setKeystoreAliasPassword(password);
		PrivateKey privateKey = CorePkiUtil.getPrivateKey(keystoreOwner);
		assertNotNull(privateKey);
		RSAPrivateKey rsaKey = assertInstanceOf(RSAPrivateKey.class, privateKey);
		assertEquals(privateExponent, rsaKey.getPrivateExponent().intValue());
	}

	@ParameterizedTest
	@CsvSource({"alias1, AliasPW1, -1314909411", "alias2, AliasPW2, -1445044487"})
	public void testDefaultKeyStoreAlias1(String alias, String password, int privateExponent) throws Exception {
		try {
			URL url = ClassLoaderUtils.getResourceURL(MULTI_KEY_KEYSTORE);
			assertNotNull(url);
			String sanitizedUrl = new File(url.toURI()).getAbsolutePath();
			System.setProperty("javax.net.ssl.keyStore", sanitizedUrl);
			System.setProperty("javax.net.ssl.keyStorePassword", "KeystorePW");
			System.setProperty("javax.net.ssl.keyStoreType", "JKS");

			KeystoreOwner keystoreOwner = new KeystoreOwner(null);
			keystoreOwner.setKeystoreType(KeystoreType.JKS);
			keystoreOwner.setKeystoreAlias(alias);
			keystoreOwner.setKeystoreAliasPassword(password);

			PrivateKey privateKey = CorePkiUtil.getPrivateKey(keystoreOwner);
			assertNotNull(privateKey);
			RSAPrivateKey rsaKey = assertInstanceOf(RSAPrivateKey.class, privateKey);
			assertEquals(privateExponent, rsaKey.getPrivateExponent().intValue());
		} finally {
			System.clearProperty("javax.net.ssl.keyStore");
			System.clearProperty("javax.net.ssl.keyStorePassword");
			System.clearProperty("javax.net.ssl.keyStoreType");
		}
	}

	@ParameterizedTest
	@CsvSource({"alias1, AliasPW1, 65537", "alias2, AliasPW2, 65537"})
	public void testDefaultGetCertificateAlias1(String alias, String password, int publicExponent) throws Exception {
		try {
			URL url = ClassLoaderUtils.getResourceURL(MULTI_KEY_KEYSTORE);
			assertNotNull(url);
			String sanitizedUrl = new File(url.toURI()).getAbsolutePath();
			System.setProperty("application.keystore.location", sanitizedUrl);
			System.setProperty("application.keystore.password", "KeystorePW");
			System.setProperty("application.keystore.type", "JKS");

			KeystoreOwner keystoreOwner = new KeystoreOwner(null);
			keystoreOwner.setKeystoreType(KeystoreType.JKS);
			keystoreOwner.setKeystorePassword("KeystorePW");
			keystoreOwner.setKeystoreAlias(alias);
			keystoreOwner.setKeystoreAliasPassword(password);

			Certificate certificate = CorePkiUtil.getCertificate(keystoreOwner);
			assertNotNull(certificate);
			RSAPublicKey rsaKey = assertInstanceOf(RSAPublicKey.class, certificate.getPublicKey());
			assertEquals(publicExponent, rsaKey.getPublicExponent().intValue());
		} finally {
			System.clearProperty("application.keystore.location");
			System.clearProperty("application.keystore.password");
			System.clearProperty("application.keystore.type");
		}
	}

	@Test
	public void testGetPublicKeyMultiKeyKeyStoreAlias1() throws EncryptionException {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias("this doesnt work with truststores");
		keystoreOwner.setKeystoreAliasPassword("AliasPW1");
		Certificate certificate = CorePkiUtil.getFirstCertificateFromTrustStore(CorePkiUtil.keyStoreAsTrustStore(keystoreOwner));
		assertNotNull(certificate);
		RSAPublicKey rsaKey = assertInstanceOf(RSAPublicKey.class, certificate.getPublicKey());
		assertEquals(65537, rsaKey.getPublicExponent().intValue());
	}

	@Test
	public void testDefaultTrustStoreAlias1() throws Exception {
		try {
			URL url = ClassLoaderUtils.getResourceURL(MULTI_KEY_KEYSTORE);
			assertNotNull(url);
			String sanitizedUrl = new File(url.toURI()).getAbsolutePath();
			System.setProperty("javax.net.ssl.trustStore", sanitizedUrl);
			System.setProperty("javax.net.ssl.trustStorePassword", "KeystorePW");
			System.setProperty("javax.net.ssl.trustStoreType", "JKS");

			KeystoreOwner keystoreOwner = new KeystoreOwner(null);
			keystoreOwner.setKeystoreType(KeystoreType.JKS);
			keystoreOwner.setKeystorePassword("KeystorePW");
			keystoreOwner.setKeystoreAlias("unused - field");
			keystoreOwner.setKeystoreAliasPassword("AliasPW1");

			Certificate certificate = CorePkiUtil.getFirstCertificateFromTrustStore(CorePkiUtil.keyStoreAsTrustStore(keystoreOwner));

			assertNotNull(certificate);
			PublicKey publicKey = assertInstanceOf(PublicKey.class, certificate.getPublicKey());
			assertEquals(ALIAS1_PUBLIC_KEY, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
		} finally {
			System.clearProperty("javax.net.ssl.trustStore");
			System.clearProperty("javax.net.ssl.trustStorePassword");
			System.clearProperty("javax.net.ssl.trustStoreType");
		}
	}

	@Test
	public void testExpiredCertificateFromJKS(TimeProvider.TimeTraveller timeTraveller) throws Exception {
		timeTraveller.setTime(getTimeAfterCertificateExpiryDate());

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
	public void testExpiredCertificateFromPKCS12(TimeProvider.TimeTraveller timeTraveller) throws Exception {
		timeTraveller.setTime(getTimeAfterCertificateExpiryDate());

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
	public void testSoonToExpireCertificateFromJKS(TimeProvider.TimeTraveller timeTraveller) throws Exception {
		timeTraveller.setTime(getTimeWhenCertificateSoonToExpire());

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
	public void testSoonToExpireCertificateFromPKCS12(TimeProvider.TimeTraveller timeTraveller) throws Exception {
		timeTraveller.setTime(getTimeWhenCertificateSoonToExpire());

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
	public void testNoExpiredCertificatesFromJKS(TimeProvider.TimeTraveller timeTraveller) throws Exception {
		timeTraveller.setTime(getTimeBeforeCertificateExpiryCheckWindow());

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
	public void testNoExpiredCertificateFromPKCS12(TimeProvider.TimeTraveller timeTraveller) throws Exception {
		timeTraveller.setTime(getTimeBeforeCertificateExpiryCheckWindow());

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
