package org.frankframework.credentialprovider.delinea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.credentialprovider.CredentialAlias;
import org.frankframework.credentialprovider.ISecret;
import org.frankframework.credentialprovider.util.CredentialConstants;

class DelineaCredentialFactoryTest {

	private DelineaCredentialFactory credentialFactory;

	@BeforeEach
	void beforeEach() {
		Secret secret1 = createSecret(1, 11, "user1", "password1");
		Secret secret2 = createSecret(2, 22, "user2", "password2");
		Secret secret3 = createSecret(3, 33, "user3", "password3");
		Secret secret4 = createSecret(4, 44, "user4", "password4");

		DelineaClient client = mock(DelineaClient.class);

		// Get secret
		when(client.getSecret(eq("1"), any())).thenReturn(secret1);
		when(client.getSecret(eq("2"), any())).thenReturn(secret2);
		when(client.getSecret(eq("3"), any())).thenReturn(secret3);
		when(client.getSecret(eq("4"), any())).thenReturn(secret4);

		// setup constants
		CredentialConstants.getInstance().setProperty(DelineaCredentialFactory.API_ROOT_URL_KEY, "http://localhost:8080");
		CredentialConstants.getInstance().setProperty(DelineaCredentialFactory.OAUTH_TOKEN_URL_KEY, "http://localhost:8080");
		CredentialConstants.getInstance().setProperty(DelineaCredentialFactory.TENANT_KEY, "testTenant");

		credentialFactory = new DelineaCredentialFactory();
		credentialFactory.setDelineaClient(client);
		credentialFactory.initialize();
	}

	@AfterAll
	static void tearDown() {
		// Since credential constants is a singleton, make sure to clean up what we set up in the beforeEach
		CredentialConstants.getInstance().remove(DelineaCredentialFactory.API_ROOT_URL_KEY);
		CredentialConstants.getInstance().remove(DelineaCredentialFactory.OAUTH_TOKEN_URL_KEY);
		CredentialConstants.getInstance().remove(DelineaCredentialFactory.TENANT_KEY);
	}

	@Test
	void testConfiguredAliases() {
		// Expect an empty list before any calls.
		Collection<String> configuredAliases = credentialFactory.getConfiguredAliases();
		assertEquals(0, configuredAliases.size());

		credentialFactory.hasSecret(CredentialAlias.parse("1"));
		credentialFactory.getSecret(CredentialAlias.parse("2"));

		// Expect a list of 2 secrets after hasCredentials and getCredentials calls
		configuredAliases = credentialFactory.getConfiguredAliases();
		assertEquals(2, configuredAliases.size());
	}

	@Test
	void testGetSecret() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("1");
		ISecret credentials = credentialFactory.getSecret(alias);

		assertNotNull(credentials);
		assertEquals("user1", credentials.getField("username"));
	}

	@Test
	void testGetNonExistingSecret() {
		CredentialAlias alias = CredentialAlias.parse("16");
		assertThrows(NoSuchElementException.class, () -> credentialFactory.hasSecret(alias));
		assertThrows(NoSuchElementException.class, () -> credentialFactory.getSecret(alias));
	}

	static Secret createSecret(int id, int folderId, String username, String password) {
		Secret.Field usernameField = new Secret.Field(1, username, "username");
		Secret.Field passwordField = new Secret.Field(2, password, "password");

		return new Secret(id, folderId, "", true, List.of(usernameField, passwordField));
	}
}
