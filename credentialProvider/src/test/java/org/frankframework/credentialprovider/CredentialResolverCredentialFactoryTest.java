package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.StringResolver;

/**
 * Test the working of resolving of credentials with and without a configured credential factory
 */
public class CredentialResolverCredentialFactoryTest {

	Properties properties;

	@BeforeEach
	void setUp() throws Exception {
		properties = new Properties();
		URL propertiesURL = getClass().getResource("/StringResolver.properties");
		assertNotNull(propertiesURL, "properties file [StringResolver.properties] not found!");

		InputStream propsStream = propertiesURL.openStream();
		properties.load(propsStream);
		assertFalse(properties.isEmpty(), "did not find any properties!");

		System.setProperty("authAliases.expansion.allowed", "${allowedAliases}");

		CredentialFactory.clearInstance();
	}

	@Test
	void testResolveWithCredentialFactoryConfigured() {
		// Make sure the defaults are always the same
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "org.frankframework.credentialprovider.PropertyFileCredentialFactory");

		String result = StringResolver.substVars("${credential:alias2}", properties);

		// expect "passwordOnly" length in '*' - since that is the length of the password configured in the property file credentials.properties
		assertEquals("passwordOnly", result);
	}

	@Test
	void testResolveWithoutCredentialFactoryConfigured() {
		// Make sure there is no credential factory configured
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "");

		String result = StringResolver.substVars("${credential:alias2}", properties);

		// Expect that this doesn't break without the credential factory
		assertEquals("", result);
	}
}
