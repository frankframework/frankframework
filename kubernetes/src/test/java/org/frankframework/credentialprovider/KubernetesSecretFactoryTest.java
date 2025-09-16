package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;

import org.frankframework.credentialprovider.util.CredentialConstants;

class KubernetesSecretFactoryTest {

	private static final KubernetesCredentialFactory credentialFactory = new KubernetesCredentialFactory();
	private static final KubernetesClient client = mock(KubernetesClient.class);

	@BeforeAll
	public static void setUp() {
		Secret secret1 = createSecret("alias1", "testUsername1", "testPassword1");
		Secret secret2 = createSecret("alias2", "testUsername2", "testPassword2");
		Secret secret3 = createSecret(null, "noAliasUsername", "noAliasPassword");
		Secret secret4 = createSecret("alias4", "noPassword", null);
		Secret secret5 = createSecret("alias5", null, "noUsername");
		secret5.getData().replace("username", "wrongBase64Encoding");

		when(client.secrets()).thenReturn(mock(MixedOperation.class));
		when(client.secrets().inNamespace(KubernetesCredentialFactory.DEFAULT_NAMESPACE)).thenReturn(mock(NonNamespaceOperation.class));
		when(client.secrets().inNamespace(KubernetesCredentialFactory.DEFAULT_NAMESPACE).list()).thenReturn(mock(SecretList.class));
		when(client.secrets().inNamespace(KubernetesCredentialFactory.DEFAULT_NAMESPACE).list().getItems()).thenReturn(List.of(secret1, secret2, secret3,
				secret4, secret5));

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
		CredentialAlias alias1 = CredentialAlias.parse("alias1"); // need to fetch the alias first
		assertTrue(credentialFactory.hasSecret(alias1));

		List<String> aliases = List.copyOf(credentialFactory.getConfiguredAliases());
		assertFalse(aliases.isEmpty());
		assertEquals("alias1", aliases.get(0));
	}

	@Test
	void testGetCredential1() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("alias1");
		ISecret credential = credentialFactory.getSecret(alias);
		assertEquals("testUsername1", credential.getField("username"));
		assertEquals("testPassword1", credential.getField("password"));
	}

	@Test
	void testGetCredential2() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("alias2");
		ISecret credential = credentialFactory.getSecret(alias);
		assertEquals("testUsername2", credential.getField("username"));
		assertEquals("testPassword2", credential.getField("password"));
	}

	@Test
	void testGetCredentialWithoutPassword() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("alias4");
		ISecret credential = credentialFactory.getSecret(alias);
		assertEquals("noPassword", credential.getField("username"));
		assertNull(credential.getField("password"));
	}

	@Test
	void testGetCredentialWithoutUsername() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("alias5");
		ISecret credential = credentialFactory.getSecret(alias);
		assertNull(credential.getField("username"));
		assertEquals("noUsername", credential.getField("password"));
	}

	@Test
	void testHasCredential() throws UnsupportedOperationException {
		CredentialAlias alias1 = CredentialAlias.parse("alias1");
		assertTrue(credentialFactory.hasSecret(alias1));

		CredentialAlias aliasX = CredentialAlias.parse("aliasX");
		assertFalse(credentialFactory.hasSecret(aliasX));
	}

	@Test
	void testGetCredentialsWithDetails() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("alias1");
		ISecret credential1 = credentialFactory.getSecret(alias);
		assertEquals("testUsername1", credential1.getField("username"));
		assertEquals("testPassword1", credential1.getField("password"));

		ISecret credential2 = credentialFactory.getSecret(alias);
		assertEquals("testUsername1", credential2.getField("username"));
		assertEquals("testPassword1", credential2.getField("password"));
	}

	@Test
	void testUnknownAliasNoDefaults() {
		CredentialAlias alias = CredentialAlias.parse("fakeAlias");
		assertThrows(NoSuchElementException.class, () -> credentialFactory.getSecret(alias));
	}

	private static Secret createSecret(String alias, String userName, String password) {
		Map<String, String> usernamePasswordMap = new HashMap<>();
		if (userName != null) usernamePasswordMap.put(CredentialFactory.DEFAULT_USERNAME_FIELD, Base64.getEncoder().encodeToString(userName.getBytes()));
		if (password != null) usernamePasswordMap.put(CredentialFactory.DEFAULT_PASSWORD_FIELD, Base64.getEncoder().encodeToString(password.getBytes()));

		ObjectMeta objectMeta = new ObjectMeta().toBuilder().withName(alias).build();
		return new Secret().toBuilder().withMetadata(objectMeta).withData(usernamePasswordMap).build();
	}
}
