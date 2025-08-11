package org.frankframework.management.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;

import org.frankframework.management.switchboard.MtlsHelper;

@DisplayName("KeystoreJwtKeyGenerator: init, signing, JWK export, and error handling")
class KeystoreJwtKeyGeneratorTest {

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("Initializes from mocked RSA keystore, exports public JWK, and signs RS512 JWT")
	void initializesAndSignsJwt() throws Exception {
		// Real RSA keypair for meaningful signature verification
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
		RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();

		// Mock keystore with single alias and X509 cert returning our RSA public key
		String alias = "test-alias";
		KeyStore ks = mock(KeyStore.class);
		when(ks.aliases()).thenReturn(Collections.enumeration(List.of(alias)));

		X509Certificate cert = mock(X509Certificate.class);
		when(cert.getPublicKey()).thenReturn(pub);
		when(ks.getCertificate(alias)).thenReturn(cert);

		try (MockedConstruction<MtlsHelper> mocked =
					 mockConstruction(
							 MtlsHelper.class, (mock, ctx) -> {
								 when(mock.getKeyStore()).thenReturn(ks);
								 when(mock.getPrivateKey()).thenReturn(priv);
							 }
					 )) {
			KeystoreJwtKeyGenerator gen = new KeystoreJwtKeyGenerator();

			// Public JWK set exported with kid == alias
			JWKSet set = JWKSet.parse(gen.getPublicJwkSet());
			assertEquals(1, set.getKeys().size(), "Should export exactly one public key");
			RSAKey rsaJwk = (RSAKey) set.getKeys().get(0);
			assertEquals(alias, rsaJwk.getKeyID());
			assertEquals(pub.getModulus(), rsaJwk.toRSAPublicKey().getModulus());

			// Provide Authentication and generate JWT
			var auth = new UsernamePasswordAuthenticationToken(
					"alice", "N/A",
					List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("read"))
			);
			SecurityContextHolder.getContext().setAuthentication(auth);

			String jwt = gen.create();
			SignedJWT parsed = SignedJWT.parse(jwt);

			// Header checks
			assertEquals(JWSAlgorithm.RS512, parsed.getHeader().getAlgorithm());
			assertEquals(JOSEObjectType.JWT, parsed.getHeader().getType());
			assertEquals(alias, parsed.getHeader().getKeyID());

			// Signature verifies with our RSA public key
			assertTrue(parsed.verify(new RSASSAVerifier(pub)));

			// Claims sanity
			var claims = parsed.getJWTClaimsSet();
			assertEquals("alice", claims.getSubject());
			assertNotNull(claims.getJWTID());
			assertTrue(claims.getStringListClaim("scope").containsAll(List.of("ROLE_ADMIN", "read")));

			Date iat = claims.getIssueTime();
			Date exp = claims.getExpirationTime();
			assertNotNull(iat);
			assertNotNull(exp);
			assertTrue(exp.after(iat));
			long deltaSec = Duration.ofMillis(exp.getTime() - iat.getTime()).toSeconds();
			assertTrue(deltaSec >= 110 && deltaSec <= 130, "Lifetime should be ~120s");
		}
	}

	@Test
	@DisplayName("Throws when keystore has no aliases")
	void throwsWhenNoAliases() throws Exception {
		KeyStore emptyKs = mock(KeyStore.class);
		when(emptyKs.aliases()).thenReturn(Collections.emptyEnumeration());

		try (MockedConstruction<MtlsHelper> mocked =
					 mockConstruction(MtlsHelper.class, (mock, ctx) -> when(mock.getKeyStore()).thenReturn(emptyKs))) {
			var ex = assertThrows(IllegalStateException.class, KeystoreJwtKeyGenerator::new);
			assertTrue(ex.getCause().getMessage().toLowerCase().contains("no aliases"));
		}
	}

	@Test
	@DisplayName("Throws when certificate public key is not RSA")
	void throwsWhenCertificateNotRsa() throws Exception {
		KeyStore ks = mock(KeyStore.class);
		when(ks.aliases()).thenReturn(Collections.enumeration(List.of("alias")));

		X509Certificate cert = mock(X509Certificate.class);
		PublicKey nonRsa = new PublicKey() {
			@Override
			public String getAlgorithm() {
				return "DSA";
			}

			@Override
			public String getFormat() {
				return "X.509";
			}

			@Override
			public byte[] getEncoded() {
				return new byte[]{ 1, 2, 3 };
			}
		};
		when(ks.getCertificate("alias")).thenReturn(cert);
		when(cert.getPublicKey()).thenReturn(nonRsa);

		try (MockedConstruction<MtlsHelper> mocked =
					 mockConstruction(MtlsHelper.class, (mock, ctx) -> when(mock.getKeyStore()).thenReturn(ks))) {
			var ex = assertThrows(IllegalStateException.class, KeystoreJwtKeyGenerator::new);
			assertTrue(ex.getCause().getMessage().contains("Certificate is not RSA"));
		}
	}

	@Test
	@DisplayName("Wraps MtlsHelper failures into IllegalStateException")
	void wrapsMtlsHelperFailures() {
		try (MockedConstruction<MtlsHelper> mocked =
					 mockConstruction(MtlsHelper.class, (mock, ctx) -> when(mock.getKeyStore()).thenThrow(new RuntimeException("boom")))) {
			var ex = assertThrows(IllegalStateException.class, KeystoreJwtKeyGenerator::new);
			assertTrue(ex.getMessage().contains("Failed to init KeystoreJwtKeyGenerator"));
			assertNotNull(ex.getCause());
			assertEquals("boom", ex.getCause().getMessage());
		}
	}

	@Test
	@DisplayName("create() throws when SecurityContext has no Authentication")
	void createThrowsWhenNoAuthentication() throws Exception {
		// Valid key material to get past constructor
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();

		KeyStore ks = mock(KeyStore.class);
		when(ks.aliases()).thenReturn(Collections.enumeration(List.of("alias")));
		X509Certificate cert = mock(X509Certificate.class);
		when(cert.getPublicKey()).thenReturn(kp.getPublic());
		when(ks.getCertificate("alias")).thenReturn(cert);

		try (MockedConstruction<MtlsHelper> mocked =
					 mockConstruction(
							 MtlsHelper.class, (mock, ctx) -> {
								 when(mock.getKeyStore()).thenReturn(ks);
								 when(mock.getPrivateKey()).thenReturn(kp.getPrivate());
							 }
					 )) {
			KeystoreJwtKeyGenerator gen = new KeystoreJwtKeyGenerator();
			SecurityContextHolder.clearContext();
			assertThrows(AuthenticationServiceException.class, gen::create);
		}
	}
}
