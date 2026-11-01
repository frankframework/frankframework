package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import org.frankframework.util.CloseUtils;
import org.frankframework.util.Environment;

class KubernetesNamedSecretProviderTest {

	private static final String SECRET_A = "central-credentials";
	private static final String SECRET_B = "partner-credentials";

	private static final KubernetesNamedSecretProvider provider = new KubernetesNamedSecretProvider();
	private static final KubernetesClient client = mock(KubernetesClient.class);
	private static MockedStatic<Environment> environmentMock;

	@BeforeAll
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static void setUp() {
		environmentMock = mockStatic(Environment.class);
		environmentMock.when(Environment::isRunningOnKubernetes).thenReturn(true);

		Map<String, String> dataA = new HashMap<>();
		dataA.put("authdatabase.username", encode("dbUser"));
		dataA.put("authdatabase.password", encode("dbPass"));
		dataA.put("authqueue.token", encode("queueToken"));
		dataA.put("standalonekey", encode("ignored"));
		Secret secretA = buildSecret(SECRET_A, dataA);

		Map<String, String> dataB = new HashMap<>();
		dataB.put("authservice.username", encode("svcUser"));
		dataB.put("authservice.password", encode("svcPass"));
		Secret secretB = buildSecret(SECRET_B, dataB);

		MixedOperation secretsOp = mock(MixedOperation.class);
		NonNamespaceOperation nsOp = mock(NonNamespaceOperation.class);
		Resource resourceA = mock(Resource.class);
		Resource resourceB = mock(Resource.class);

		when(client.secrets()).thenReturn(secretsOp);
		when(secretsOp.inNamespace(AbstractKubernetesCredentialProvider.DEFAULT_NAMESPACE)).thenReturn(nsOp);

		when(nsOp.withName(SECRET_A)).thenReturn(resourceA);
		when(nsOp.withName(SECRET_B)).thenReturn(resourceB);

		when(resourceA.get()).thenReturn(secretA);
		when(resourceB.get()).thenReturn(secretB);

		when(client.getConfiguration()).thenReturn(mock(Config.class));

		CredentialFactory.reset();
		CredentialFactory.setProperty(AbstractKubernetesCredentialProvider.K8_MASTER_URL, "http://localhost:8080");
		CredentialFactory.setProperty(KubernetesNamedSecretProvider.K8_SECRET_NAMES_PROPERTY, SECRET_A + "," + SECRET_B);

		provider.setClient(client);
		provider.initialize();
	}

	@AfterAll
	static void tearDown() {
		CredentialFactory.reset();
		CloseUtils.closeSilently(environmentMock, provider);
	}

	@Test
	void testGetConfiguredAliasesDiscoversAcrossSecrets() {
		Collection<String> aliases = provider.getConfiguredAliases();
		assertTrue(aliases.contains("authdatabase"));
		assertTrue(aliases.contains("authqueue"));
		assertTrue(aliases.contains("authservice"));
		assertFalse(aliases.contains("standalonekey"), "keys without a dot should not be treated as aliases");
	}

	@Test
	void testGetCredentialFromFirstSecret() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("authdatabase");
		assertNotNull(alias);
		ISecret credential = provider.getSecret(alias);
		assertEquals("dbUser", credential.getField("username"));
		assertEquals("dbPass", credential.getField("password"));
	}

	@Test
	void testGetCredentialFromSecondSecret() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("authservice");
		assertNotNull(alias);
		ISecret credential = provider.getSecret(alias);
		assertEquals("svcUser", credential.getField("username"));
		assertEquals("svcPass", credential.getField("password"));
	}

	@Test
	void testGetFieldReturnsNullForMissingField() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("authqueue");
		assertNotNull(alias);
		ISecret credential = provider.getSecret(alias);
		assertEquals("queueToken", credential.getField("token"));
		assertNull(credential.getField("username"), "field not present for this prefix should be null");
	}

	@Test
	void testGetFieldReturnsNullForEmptyFieldname() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("authdatabase");
		assertNotNull(alias);
		ISecret credential = provider.getSecret(alias);
		assertNull(credential.getField(null));
		assertNull(credential.getField(""));
	}

	@Test
	void testHasSecret() {
		CredentialAlias alias1 = CredentialAlias.parse("authdatabase");
		assertNotNull(alias1);
		assertTrue(provider.hasSecret(alias1));
		CredentialAlias alias2 = CredentialAlias.parse("authservice");
		assertNotNull(alias2);
		assertTrue(provider.hasSecret(alias2));
		CredentialAlias alias3 = CredentialAlias.parse("nonExistentAlias");
		assertNotNull(alias3);
		assertFalse(provider.hasSecret(alias3));
	}

	@Test
	void testUnknownAliasThrows() {
		CredentialAlias alias = CredentialAlias.parse("fakeAlias");
		assertNotNull(alias);
		NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> provider.getSecret(alias));
		assertTrue(ex.getMessage().contains("fakeAlias"));
	}

	@Test
	void testCachingReturnsConsistentResults() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("authdatabase");
		assertNotNull(alias);
		ISecret first = provider.getSecret(alias);
		ISecret second = provider.getSecret(alias);
		assertEquals(first.getField("username"), second.getField("username"));
		assertEquals(first.getField("password"), second.getField("password"));
	}

	@Test
	void testInvalidStartAndEndCharactersLogWarnings() {
		Logger rootLogger = Logger.getLogger("");
		rootLogger.setLevel(Level.WARNING);

		TestLogHandler handler = new TestLogHandler();
		rootLogger.addHandler(handler);

		CredentialAlias illegalChars = CredentialAlias.parse("-invalidAlias-");
		assertNotNull(illegalChars);
		assertThrows(NoSuchElementException.class, () -> provider.getSecret(illegalChars));
		assertTrue(handler.contains("must start and end with an alphanumeric"));

		rootLogger.removeHandler(handler);
	}

	@Test
	void testMissingSecretNamesPropertyThrows() {
		CredentialFactory.setProperty(KubernetesNamedSecretProvider.K8_SECRET_NAMES_PROPERTY, "");
		try (KubernetesNamedSecretProvider freshProvider = new KubernetesNamedSecretProvider()) {
			freshProvider.setClient(client);
			assertThrows(KubernetesClientException.class, freshProvider::initialize);
		} finally {
			CredentialFactory.setProperty(KubernetesNamedSecretProvider.K8_SECRET_NAMES_PROPERTY, SECRET_A + "," + SECRET_B);
		}
	}

	private static String encode(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes());
	}

	private static Secret buildSecret(String name, Map<String, String> data) {
		ObjectMeta meta = new ObjectMeta().toBuilder().withName(name).build();
		return new Secret().toBuilder().withMetadata(meta).withData(data).build();
	}
}
