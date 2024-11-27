package org.frankframework.encryption;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;

public class AuthenticationContextFactoryTest {

	private final String MULTI_KEY_KEYSTORE = "Encryption/MultiKeyKeystore.jks";

	@Test
	public void testValidateConfiguration() throws ConfigurationException {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		AuthSSLContextFactory.verifyKeystoreConfiguration(keystoreOwner, null);
	}

	@Test
	public void testGetPrivateKeyMultiKeyKeyStoreAlias1() throws EncryptionException {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias("alias1");
		keystoreOwner.setKeystoreAliasPassword("AliasPW1");
		PrivateKey privateKey = PkiUtil.getPrivateKey(keystoreOwner, "Test");
		assertNotNull(privateKey);
	}

	@Test
	public void testGetContextMultiKeyKeyStoreAlias1() throws Exception {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias("alias1");
		keystoreOwner.setKeystoreAliasPassword("AliasPW1");
		SSLContext context = AuthSSLContextFactory.createSSLContext(keystoreOwner, null, null);
		assertNotNull(context);
	}

	@Test
	public void testGetContextMultiKeyKeyStoreAlias2() throws Exception {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias("alias2");
		keystoreOwner.setKeystoreAliasPassword("AliasPW2");
		SSLContext context = AuthSSLContextFactory.createSSLContext(keystoreOwner, null, null);
		assertNotNull(context);
	}

	@Test
	public void testGetContextMultiKeyKeyStoreWrongKeystorePassword() throws Exception {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("wrong");
		keystoreOwner.setKeystoreAlias("alias2");
		keystoreOwner.setKeystoreAliasPassword("AliasPW2");
		assertThrows(IOException.class, ()-> AuthSSLContextFactory.createSSLContext(keystoreOwner, null, null), "password was incorrect");
	}

	@Test
	public void testGetContextMultiKeyKeyStoreWrongAliasPassword() throws Exception {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias("alias2");
		keystoreOwner.setKeystoreAliasPassword("wrong");
		assertThrows(UnrecoverableKeyException.class, ()-> AuthSSLContextFactory.createSSLContext(keystoreOwner, null, null), "Cannot recover key");
	}
}
