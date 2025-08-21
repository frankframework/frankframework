package org.frankframework.management.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.operator.OperatorCreationException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.frankframework.encryption.CommonsPkiUtil;

class JwtGeneratorFactoryBeanTest {

	private static KeyStore newRsaJks(char[] storePw) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, OperatorCreationException {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		X509Certificate cert = SelfSignedCertGenerator.generate("CN=FactoryTest", kp, 365);

		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(null, storePw);
		ks.setKeyEntry("test", kp.getPrivate(), storePw, new java.security.cert.Certificate[]{ cert });
		return ks;
	}

	private static File writeKs(KeyStore ks, char[] pw) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
		File f = File.createTempFile("keystore", ".jks");
		try (FileOutputStream fos = new FileOutputStream(f)) {
			ks.store(fos, pw);
			return f;
		}
	}

	@Test
	void defaultReturnsJwtKeyGenerator() {
		JwtGeneratorFactoryBean factory = new JwtGeneratorFactoryBean();
		AbstractJwtKeyGenerator obj = factory.getObject();
		assertNotNull(obj, "Factory should not return null");
		assertTrue(obj instanceof JwtKeyGenerator, "Should return default JwtKeyGenerator");
	}

	@Test
	void validProps_andCreateKeyStoreSucceeds_returnsKeystoreJwtKeyGenerator() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, OperatorCreationException {
		char[] pw = "secret".toCharArray();
		KeyStore ks = newRsaJks(pw);
		File ksFile = writeKs(ks, pw);

		JwtGeneratorFactoryBean factory = new JwtGeneratorFactoryBean();
		factory.keyStoreLocation = ksFile.toURI().toString();
		factory.keyStorePassword = String.valueOf(pw);

		try (MockedStatic<CommonsPkiUtil> mocked = Mockito.mockStatic(CommonsPkiUtil.class)) {
			mocked.when(() -> CommonsPkiUtil.createKeyStore(ksFile.toURI().toURL(), factory.keyStorePassword, null))
					.thenReturn(ks);

			AbstractJwtKeyGenerator generator = factory.getObject();
			assertNotNull(generator);
			assertTrue(generator instanceof KeystoreJwtKeyGenerator);
			assertEquals(KeystoreJwtKeyGenerator.class, factory.getObjectType());
			assertSame(generator, factory.getObject());
		}
	}

	@Test
	void validProps_butCreateKeyStoreThrows_returnsNull_givenCurrentImplementation() {
		JwtGeneratorFactoryBean factory = new JwtGeneratorFactoryBean();
		factory.keyStoreLocation = "file:/bestaatniet.jks";
		factory.keyStorePassword = "dummy";

		try (MockedStatic<CommonsPkiUtil> mocked = Mockito.mockStatic(CommonsPkiUtil.class)) {
			mocked.when(() -> CommonsPkiUtil.createKeyStore(
							new java.net.URI(factory.keyStoreLocation).toURL(), factory.keyStorePassword, null))
					.thenThrow(new IOException("Should throw exception"));

			AbstractJwtKeyGenerator generator = factory.getObject();
			assertNull(generator);
			assertEquals(AbstractJwtKeyGenerator.class, factory.getObjectType());
		}
	}
}
