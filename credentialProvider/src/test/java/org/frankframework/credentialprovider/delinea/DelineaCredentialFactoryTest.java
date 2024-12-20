package org.frankframework.credentialprovider.delinea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.frankframework.credentialprovider.ICredentials;
import org.frankframework.credentialprovider.util.CredentialConstants;

public class DelineaCredentialFactoryTest {

	private static final DelineaCredentialFactory credentialFactory = new DelineaCredentialFactory();
	private static final DelineaClient client = mock(DelineaClient.class);

	@BeforeAll
	public static void setUpBeforeClass() {
		Secret secret1 = createSecret(1, 11, "user1", "password1");
		Secret secret2 = createSecret(2, 22, "user2", "password2");
		Secret secret3 = createSecret(3, 33, "user3", "password3");
		Secret secret4 = createSecret(4, 44, "user4", "password4");

		List<String> list = Stream.of(secret1, secret2, secret3, secret4)
				.map(Secret::id)
				.map(Objects::toString)
				.toList();

		when(client.getSecret("1")).thenReturn(secret1);

		// TODO mock client.getSecretsPage
		when(client.getSecrets()).thenReturn(list);

		// setup constants
		CredentialConstants.getInstance().setProperty(DelineaCredentialFactory.API_ROOT_URL_KEY, "http://localhost:8080");
		CredentialConstants.getInstance().setProperty(DelineaCredentialFactory.OAUTH_TOKEN_URL_KEY, "http://localhost:8080");
		CredentialConstants.getInstance().setProperty(DelineaCredentialFactory.TENANT_KEY, "testTenant");

		credentialFactory.setDelineaClient(client);
		credentialFactory.initialize();
	}

	@Test
	void testGetSecrets() {
		Collection<String> configuredAliases = credentialFactory.getConfiguredAliases();
		assertEquals(4, configuredAliases.size());

		assertTrue(configuredAliases.contains("1"));
		assertFalse(configuredAliases.contains("5"));
	}

	@Test
	void testGetSecret() {
		ICredentials credentials = credentialFactory.getCredentials("1", () -> null, () -> null);

		assertNotNull(credentials);
		assertEquals("user1", credentials.getUsername());

		// Non-existing secret
		ICredentials credentials2 = credentialFactory.getCredentials("16", () -> null, () -> null);
		assertNotNull(credentials2);
		assertNull(credentials.getUsername());
	}

	private static Secret createSecret(int id, int folderId, String username, String password) {
		Secret.Field usernameField = new Secret.Field(1, username, "username");
		Secret.Field passwordField = new Secret.Field(2, password, "password");

		return new Secret(id, folderId, "", true, List.of(usernameField, passwordField));
	}
}
