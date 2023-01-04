package nl.nn.adapterframework.encryption;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.PrivateKey;
import java.security.PublicKey;

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
		PrivateKey privateKey = PkiUtil.getPrivateKey(keystoreOwner, "Test");
		assertNotNull(privateKey);
	}

	@Test
	public void testGetPublicKeyMultiKeyKeyStoreAlias1() throws EncryptionException {
		KeystoreOwner keystoreOwner = new KeystoreOwner(MULTI_KEY_KEYSTORE);
		keystoreOwner.setKeystoreType(KeystoreType.JKS);
		keystoreOwner.setKeystorePassword("KeystorePW");
		keystoreOwner.setKeystoreAlias("alias1");
		keystoreOwner.setKeystoreAliasPassword("AliasPW1");
		PublicKey publicKey = PkiUtil.getPublicKey(PkiUtil.keyStoreAsTrustStore(keystoreOwner), "Test");
		assertNotNull(publicKey);
	}
}
