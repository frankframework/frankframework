package org.frankframework.management.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

		KeystoreJwtGenerator generator = new KeystoreJwtGenerator(ks);
		generator.setApplicationContext(mock(ApplicationContext.class));
		generator.afterPropertiesSet();

		assertNotNull(generator.createJWT());

		assertNotNull(generator.jwtHeader, "jwtHeader should be set");
		assertEquals("RS512", generator.jwtHeader.getAlgorithm().getName(), "Should use RS512");
		assertNotNull(generator.publicJwkSet, "publicJwkSet should be populated");
	}

	@Test
	void testEmptyKeyStoreThrows() throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(null, null);
		KeystoreJwtGenerator kg = new KeystoreJwtGenerator(ks);
		assertThrows(IllegalStateException.class, kg::afterPropertiesSet);
	}
}
