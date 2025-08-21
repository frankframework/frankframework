package org.frankframework.management.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.frankframework.encryption.CommonsPkiUtil;

class KeystoreJwtKeyGeneratorTest {

	@Test
	void testInitWithValidRsaKeyStore() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair pair = kpg.generateKeyPair();
		X509Certificate cert = SelfSignedCertGenerator.generate("CN=Test", pair, 365);

		KeyStore ks = KeyStore.getInstance("JKS");
		char[] storePw = "secret".toCharArray();
		ks.load(null, storePw);
		ks.setKeyEntry("test", pair.getPrivate(), storePw, new java.security.cert.Certificate[]{ cert });

		try (MockedStatic<CommonsPkiUtil> mocked = Mockito.mockStatic(CommonsPkiUtil.class)) {
			mocked.when(() -> CommonsPkiUtil.getRsaPrivateKey(ks))
					.thenReturn(pair.getPrivate());

			KeystoreJwtKeyGenerator generator = new KeystoreJwtKeyGenerator(ks);

			assertNotNull(generator.jwtHeader, "jwtHeader should be set");
			assertEquals("RS512", generator.jwtHeader.getAlgorithm().getName(), "Should use RS512");
			assertNotNull(generator.publicJwkSet, "publicJwkSet should be populated");

			assertThrows(Exception.class, generator::getSigner);
		}
	}

	@Test
	void testEmptyKeyStoreThrows() throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(null, null);
		assertThrows(IllegalStateException.class, () -> new KeystoreJwtKeyGenerator(ks));
	}
}
