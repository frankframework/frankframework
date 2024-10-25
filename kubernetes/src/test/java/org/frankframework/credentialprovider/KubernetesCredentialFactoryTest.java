package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;

import org.frankframework.credentialprovider.util.CredentialConstants;

class KubernetesCredentialFactoryTest {

	private static final KubernetesCredentialFactory credentialFactory = new KubernetesCredentialFactory();
	private static final KubernetesClient client = mock(KubernetesClient.class);

	@BeforeAll
	public static void setUp() {
		Secret secret1 = createSecret("alias1", "testUsername1", "testPassword1");
		Secret secret2 = createSecret("alias2", "testUsername2", "testPassword2");
		Secret secret3 = createSecret(null, "noAliasUsername", "noAliasPassword");
		Secret secret4 = createSecret("alias4", "noPasswordUser", null);
		Secret secret5 = createSecret("alias5", "testUsername5", "testUsername5");
		secret5.getData().replace(KubernetesCredentialFactory.USERNAME_KEY, "wrongBase64Encoding");

		when(client.secrets()).thenReturn(mock(MixedOperation.class));
		when(client.secrets().inNamespace(KubernetesCredentialFactory.DEFAULT_NAMESPACE)).thenReturn(mock(NonNamespaceOperation.class));
		when(client.secrets().inNamespace(KubernetesCredentialFactory.DEFAULT_NAMESPACE).list()).thenReturn(mock(SecretList.class));
		when(client.secrets()
				.inNamespace(KubernetesCredentialFactory.DEFAULT_NAMESPACE)
				.list()
				.getItems()).thenReturn(List.of(secret1, secret2, secret3, secret4, secret5));

		when(client.getConfiguration()).thenReturn(new Config());
		CredentialConstants.getInstance().setProperty(KubernetesCredentialFactory.K8_MASTER_URL, "http://localhost:8080");
		credentialFactory.setClient(client);
		credentialFactory.initialize();
	}

	@AfterAll
	public static void tearDown() {
		credentialFactory.close();
	}

	@Test
	void testGetAliases() throws UnsupportedOperationException {
		List<String> aliases = (List) credentialFactory.getConfiguredAliases();
		assertEquals(4, aliases.size());
		assertEquals("alias1", aliases.get(0));
		assertEquals("alias2", aliases.get(1));
	}

	@Test
	void testGetCredentials() throws UnsupportedOperationException {
		List<Credentials> credentials = credentialFactory.getCredentials();
		assertEquals("alias1", credentials.get(0).getAlias());
		assertEquals("testUsername1", credentials.get(0).getUsername());
		assertEquals("testPassword1", credentials.get(0).getPassword());

		assertEquals("alias2", credentials.get(1).getAlias());
		assertEquals("testUsername2", credentials.get(1).getUsername());
		assertEquals("testPassword2", credentials.get(1).getPassword());

		assertNull(credentials.get(2).getAlias());
		assertEquals("noAliasUsername", credentials.get(2).getUsername());
		assertEquals("noAliasPassword", credentials.get(2).getPassword());

		assertEquals("alias4", credentials.get(3).getAlias());
		assertNull(credentials.get(3).getPassword());
		assertEquals(5, credentials.size());
	}

	@Test
	void testHasCredential() throws UnsupportedOperationException {
		assertTrue(credentialFactory.hasCredentials("alias1"));
		assertFalse(credentialFactory.hasCredentials("aliasX"));
	}

	@Test
	void testGetCredentialsWithDetails() throws UnsupportedOperationException {
		ICredentials credential1 = credentialFactory.getCredentials("alias1", () -> "testUsername", () -> "testPassword");
		assertEquals("testUsername1", credential1.getUsername());
		assertEquals("testPassword1", credential1.getPassword());

		ICredentials credential2 = credentialFactory.getCredentials("alias1", null, null);
		assertEquals("testUsername1", credential2.getUsername());
		assertEquals("testPassword1", credential2.getPassword());
	}

	@Test
	void testGetCredentialsWithoutAlias() throws UnsupportedOperationException {
		ICredentials credential1 = credentialFactory.getCredentials(null, () -> "testUsername2", () -> "testPassword2");
		assertNull(credential1.getAlias());
		assertEquals("testUsername2", credential1.getUsername());
		assertEquals("testPassword2", credential1.getPassword());
	}

	@Test
	void testGetCredentialsWithOneDetail() throws UnsupportedOperationException {
		ICredentials credentials1 = credentialFactory.getCredentials(null, () -> "testUsername2", null);
		ICredentials credentials2 = credentialFactory.getCredentials(null, null, () -> "testPassword2");

		assertEquals("testUsername2", credentials1.getUsername());
		assertNull(credentials1.getPassword());
		assertNull(credentials1.getAlias());

		assertEquals("testPassword2", credentials2.getPassword());
		assertNull(credentials2.getUsername());
		assertNull(credentials2.getAlias());
	}

	@Test
	void testUnknownAliasNoDefaults() {
		assertThrows(NoSuchElementException.class, () -> credentialFactory.getCredentials("fakeAlias", () -> null, () -> null));
	}

	@Test
	void testGetCredentialsWithNothing() {
		ICredentials credentials = credentialFactory.getCredentials(null, () -> null, () -> null);
		assertNull(credentials.getAlias());
		assertNull(credentials.getUsername());
		assertNull(credentials.getPassword());

		ICredentials credentials2 = credentialFactory.getCredentials(null, null, null);
		assertNull(credentials2.getAlias());
		assertNull(credentials2.getUsername());
		assertNull(credentials2.getPassword());
	}

	@Test
	void testGetCredentialsWithWrongDetailsDirectlyReturnsGivenData() {
		ICredentials credentials = credentialFactory.getCredentials(null, () -> "fakeUsername", () -> "fakePassword");
		assertEquals("fakeUsername", credentials.getUsername());
		assertEquals("fakePassword", credentials.getPassword());
		assertNull(credentials.getAlias());
	}

	@Test
	void testCachingSecretsWorks() {
		// Should be called once
		Mockito.clearInvocations(client);
		credentialFactory.clearTimer();
		credentialFactory.getCredentials();
		verify(client).secrets();

		// Now it should not be called again
		Mockito.clearInvocations(client);
		credentialFactory.getCredentials();
		verifyNoInteractions(client);

		// Simulate a new fetch
		credentialFactory.clearTimer();
		credentialFactory.getCredentials();
		verify(client).secrets();
	}

	private static Secret createSecret(String alias, String userName, String password) {
		Map<String, String> usernamePasswordMap = new HashMap<>();
		if (userName != null) usernamePasswordMap.put(KubernetesCredentialFactory.USERNAME_KEY, Base64.getEncoder().encodeToString(userName.getBytes()));
		if (password != null) usernamePasswordMap.put(KubernetesCredentialFactory.PASSWORD_KEY, Base64.getEncoder().encodeToString(password.getBytes()));

		ObjectMeta objectMeta = new ObjectMeta().toBuilder().withName(alias).build();
		return new Secret().toBuilder().withMetadata(objectMeta).withData(usernamePasswordMap).build();
	}
}
