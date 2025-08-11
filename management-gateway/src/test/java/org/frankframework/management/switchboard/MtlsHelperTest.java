package org.frankframework.management.switchboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.GeneralSecurityException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MtlsHelperTest {

	@BeforeEach
	void setUpKeystoreProperties() {
		System.setProperty("client.ssl.key-store", "classpath:keystores/client.jks");
		System.setProperty("client.ssl.key-store-password", "AmsterdamClientOnePassword");
		System.setProperty("client.ssl.trust-store", "classpath:keystores/trust.jks");
		System.setProperty("client.ssl.trust-store-password", "SwitchboardPassword");
	}

	@Test
	@DisplayName("When keyStoreLocation is null, Then IllegalArgumentException is thrown")
	void nullKeyStoreLocationTest() {
		System.clearProperty("client.ssl.key-store");
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
	@DisplayName("Given valid PKCS12 keystore properties, When constructing MtlsHelper, Then PKCS12 KeyStore and keys are loaded")
	void constructorLoadsPkcs12Keystore() {
		MtlsHelper helper = new MtlsHelper();

		assertNotNull(helper.getKeyStore(), "KeyStore should be initialized");
		assertEquals("PKCS12", helper.getKeyStore().getType());
		assertNotNull(helper.getPrivateKey(), "PrivateKey should be extracted");
		assertNotNull(helper.getPublicKey(), "PublicKey should be extracted");
	}

	@Test
	@DisplayName("When keystore location property is missing, Then IllegalArgumentException('location is null') is thrown")
	void constructorThrowsWhenKeystoreLocationMissing() {
		// Remove keystore location property to simulate null
		System.clearProperty("client.ssl.key-store");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, MtlsHelper::new);
		assertEquals("location is null", ex.getMessage());
	}

	@Test
	@DisplayName("Given wrong keystore password, Then CloudAgentException wraps GeneralSecurityException")
	void constructorThrowsOnInvalidPassword() {
		// Provide wrong password
		System.setProperty("client.ssl.key-store", "classpath:keystores/client.p12");
		System.setProperty("client.ssl.key-store-password", "wrongpw");

		CloudAgentException ex = assertThrows(
				CloudAgentException.class,
				MtlsHelper::new,
				"Expected CloudAgentException when both PKCS12 and JKS loading fail"
		);
		assertTrue(
				ex.getCause() instanceof GeneralSecurityException,
				"Cause should be GeneralSecurityException"
		);
	}
}
