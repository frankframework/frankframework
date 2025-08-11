package org.frankframework.management.switchboard;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.KeySourceException;

class CloudAgentJwtVerifierTest {

	private MtlsHelper mtlsHelper;

	@BeforeEach
	void setUp() {
		mtlsHelper = mock(MtlsHelper.class);
	}

	@Test
	@DisplayName("When keyStoreLocation is null, Then IllegalArgumentException is thrown")
	void nullKeyStoreLocationTest() {
		assertThrows(IllegalArgumentException.class, MtlsHelper::new);
	}

	@Test
	@DisplayName("When KeyStore can't be found in provided location, Then CloudAgentException is thrown")
	void constructorThrowsWhenKeystoreCannotBeLoaded() {
		String propertyName = "client.ssl.key-store";
		String badLocation = "/path/to/nowhere/does-not-exist.jks";
		System.setProperty(propertyName, badLocation);

		CloudAgentException ex = assertThrows(CloudAgentException.class, MtlsHelper::new);

		assertTrue(ex.getMessage().contains("Unable to load keystore"), "Expected a message about being unable to load the keystore");
		System.clearProperty(propertyName);
	}

	@Test
	@DisplayName("When an empty KeyStore is provided, Then IOException is thrown")
	void verifyThrowsIOExceptionOnEmptyKeyStore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
		KeyStore emptyKs = KeyStore.getInstance(KeyStore.getDefaultType());
		emptyKs.load(null, null);
		when(mtlsHelper.getKeyStore()).thenReturn(emptyKs);
		CloudAgentJwtVerifier verifier = new CloudAgentJwtVerifier(mtlsHelper);

		IOException ex = assertThrows(IOException.class, () -> verifier.verify("eyJhbGciOiJSUzUxMiJ9.e30.signature"));

		assertTrue(ex.getMessage().contains("unable to parse JWT"));
		assertNotNull(ex.getCause());
		assertInstanceOf(KeySourceException.class, ex.getCause());
		assertTrue(ex.getCause().getMessage().contains("No aliases in keystore"));
	}

	@Test
	@DisplayName("When invalid JWT is provided, Then IOException is thrown")
	void verifyThrowsIOExceptionOnMalformedJwt() {
		CloudAgentJwtVerifier verifier = new CloudAgentJwtVerifier(mtlsHelper);

		IOException ex = assertThrows(IOException.class, () -> verifier.verify("not-a-jwt"));

		assertTrue(ex.getMessage().contains("unable to parse JWT"));
		assertNotNull(ex.getCause());
	}

	@Test
	void verifyThrowsIOExceptionWhenCertificateNotRSA() throws Exception {
		KeyStore ksWithBadCert = KeyStore.getInstance(KeyStore.getDefaultType());
		ksWithBadCert.load(null, null);

		String alias = "onlyAlias";
		X509Certificate badCert = mock(X509Certificate.class);
		PublicKey notRsaKey = mock(PublicKey.class);
		when(badCert.getPublicKey()).thenReturn(notRsaKey);
		ksWithBadCert.setCertificateEntry(alias, badCert);

		when(mtlsHelper.getKeyStore()).thenReturn(ksWithBadCert);

		CloudAgentJwtVerifier verifier = new CloudAgentJwtVerifier(mtlsHelper);

		String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"RS512\"}".getBytes());
		String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
		String jwt = header + "." + payload + ".signature";

		IOException ex = assertThrows(IOException.class, () -> verifier.verify(jwt));

		assertInstanceOf(KeySourceException.class, ex.getCause());
		assertTrue(ex.getCause().getMessage().contains("Certificate is not RSA"));
	}
}
