package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CredentialFactoryTest {

	@Test
	public void testFindAliasNoPrefix() {
		// test depends on setting credentialFactory.class=...credentialprovider.PropertyFileCredentialFactory in test/resources/credentialprovider.properties

		// Act
		ICredentials c = CredentialFactory.getCredentials("account", null, null);

		// Assert
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}

	@Test
	public void testFindAliasWithPrefix() {
		// test depends on setting credentialFactory.optionalPrefix=fakePrefix: in test/resources/credentialprovider.properties

		// Act
		ICredentials c = CredentialFactory.getCredentials("fakePrefix:account", null, null);

		// Assert
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}
}
