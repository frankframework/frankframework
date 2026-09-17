package org.frankframework.management.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.nimbusds.jwt.SignedJWT;

import org.frankframework.util.SpringUtils;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class KeystoreJwtGeneratorTest {

	@Test
	@WithMockUser
	void testInitWithValidRsaKeyStore() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair pair = kpg.generateKeyPair();
		X509Certificate cert = SelfSignedCertGenerator.generate("CN=Issuer, OU=Test, O=Test, L=Test, C=US", pair, 365);

		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(null, null);
		ks.setKeyEntry("test", pair.getPrivate(), new char[]{}, new java.security.cert.Certificate[]{ cert });

		GenericApplicationContext ac = new GenericApplicationContext();
		ac.setDisplayName("bla bla appl");
		ac.refresh();
		KeystoreJwtGenerator generator = new KeystoreJwtGenerator(ks);
		SpringUtils.autowireByType(ac, generator);

		String jwt = generator.createJWT();
		assertNotNull(jwt);

		assertNotNull(generator.jwtHeader, "jwtHeader should be set");
		assertEquals("RS512", generator.jwtHeader.getAlgorithm().getName(), "Should use RS512");
		assertNotNull(generator.getPublicJwk(), "Jwk should be present");
		Map<String, Object> claims = SignedJWT.parse(jwt).getPayload().toJSONObject();
		assertEquals("user", claims.get("sub"));
		assertEquals("[ROLE_USER]", claims.get("scope").toString());
	}

	@Test
	void testEmptyKeyStoreThrows() throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(null, null);
		KeystoreJwtGenerator kg = new KeystoreJwtGenerator(ks);
		assertThrows(IllegalStateException.class, kg::afterPropertiesSet);
	}
}
