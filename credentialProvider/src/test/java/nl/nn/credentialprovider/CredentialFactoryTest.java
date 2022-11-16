package nl.nn.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Can only run before other tests that use CredentialFactory")
public class CredentialFactoryTest {

	@Test
	public void testFindAliasNoPrefix() {
		// test depends on setting credentialFactory.class=nl.nn.credentialprovider.MockMapCredentialFactory in test/resources/credentialprovider.properties
		ICredentials c = CredentialFactory.getCredentials("account", null, null);
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}

	@Test
	public void testFindAliasWithPrefix() {
		ICredentials c = CredentialFactory.getCredentials("fakePrefix:account", null, null);
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}
}
