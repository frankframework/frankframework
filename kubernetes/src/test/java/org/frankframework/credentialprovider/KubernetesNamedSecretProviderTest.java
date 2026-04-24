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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import org.frankframework.credentialprovider.util.CredentialConstants;

class KubernetesNamedSecretProviderTest {

	private static final String SECRET_A = "central-credentials";
	private static final String SECRET_B = "partner-credentials";

	private static final KubernetesNamedSecretProvider provider = new KubernetesNamedSecretProvider();
	private static final KubernetesClient client = mock(KubernetesClient.class);

	@BeforeAll
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void setUp() {
		Map<String, String> dataA = new HashMap<>();
		dataA.put("authdatabase.username", encode("dbUser"));
		dataA.put("authdatabase.password", encode("dbPass"));
		dataA.put("authqueue.token",       encode("queueToken"));
		dataA.put("standalonekey",         encode("ignored"));
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
		when(secretsOp.inNamespace(BaseKubernetesCredentialProvider.DEFAULT_NAMESPACE)).thenReturn(nsOp);

		when(nsOp.withName(SECRET_A)).thenReturn(resourceA);
		when(nsOp.withName(SECRET_B)).thenReturn(resourceB);

		when(resourceA.get()).thenReturn(secretA);
		when(resourceB.get()).thenReturn(secretB);

		when(client.getConfiguration()).thenReturn(mock(Config.class));

		CredentialConstants.getInstance().setProperty(BaseKubernetesCredentialProvider.K8_MASTER_URL, "http://localhost:8080");
		CredentialConstants.getInstance().setProperty(KubernetesNamedSecretProvider.K8_SECRET_NAMES_PROPERTY, SECRET_A + "," + SECRET_B);

		provider.setClient(client);
		provider.initialize();
	}

	@AfterAll
	public static void tearDown() {
		provider.close();
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
		ISecret credential = provider.getSecret(alias);
		assertEquals("dbUser", credential.getField("username"));
		assertEquals("dbPass", credential.getField("password"));
	}

	@Test
	void testGetCredentialFromSecondSecret() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("authservice");
		ISecret credential = provider.getSecret(alias);
		assertEquals("svcUser", credential.getField("username"));
		assertEquals("svcPass", credential.getField("password"));
	}

	@Test
	void testGetFieldReturnsNullForMissingField() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("authqueue");
		ISecret credential = provider.getSecret(alias);
		assertEquals("queueToken", credential.getField("token"));
		assertNull(credential.getField("username"), "field not present for this prefix should be null");
	}

	@Test
	void testGetFieldReturnsNullForEmptyFieldname() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("authdatabase");
		ISecret credential = provider.getSecret(alias);
		assertNull(credential.getField(null));
		assertNull(credential.getField(""));
	}

	@Test
	void testHasSecret() {
		assertTrue(provider.hasSecret(CredentialAlias.parse("authdatabase")));
		assertTrue(provider.hasSecret(CredentialAlias.parse("authservice")));
		assertFalse(provider.hasSecret(CredentialAlias.parse("nonExistentAlias")));
	}

	@Test
	void testUnknownAliasThrows() {
		CredentialAlias alias = CredentialAlias.parse("fakeAlias");
		NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> provider.getSecret(alias));
		assertTrue(ex.getMessage().contains("fakeAlias"));
	}

	@Test
	void testCachingReturnsConsistentResults() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("authdatabase");
		ISecret first = provider.getSecret(alias);
		ISecret second = provider.getSecret(alias);
		assertEquals(first.getField("username"), second.getField("username"));
		assertEquals(first.getField("password"), second.getField("password"));
	}

	@Test
	void testInvalidStartAndEndCharactersLogWarnings() {
		TestLog4j2Appender appender = TestLog4j2Appender.attach(KubernetesNamedSecretProvider.class);
		try {
			CredentialAlias illegalChars = CredentialAlias.parse("-invalidAlias-");
			assertThrows(NoSuchElementException.class, () -> provider.getSecret(illegalChars));
			assertTrue(appender.contains("must start and end with an alphanumeric"));
		} finally {
			appender.detach();
		}
	}

	@Test
	void testMissingSecretNamesPropertyThrows() {
		CredentialConstants.getInstance().setProperty(KubernetesNamedSecretProvider.K8_SECRET_NAMES_PROPERTY, "");
		try {
			KubernetesNamedSecretProvider freshProvider = new KubernetesNamedSecretProvider();
			freshProvider.setClient(client);
			assertThrows(KubernetesClientException.class, freshProvider::initialize);
		} finally {
			CredentialConstants.getInstance().setProperty(KubernetesNamedSecretProvider.K8_SECRET_NAMES_PROPERTY, SECRET_A + "," + SECRET_B);
		}
	}

	private static String encode(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes());
	}

	private static Secret buildSecret(String name, Map<String, String> data) {
		ObjectMeta meta = new ObjectMeta().toBuilder().withName(name).build();
		return new Secret().toBuilder().withMetadata(meta).withData(data).build();
	}

	/**
	 * Minimal Log4j2 appender for asserting on log output.
	 * The existing TestLogHandler is tied to java.util.logging, which this class doesn't use.
	 */
	private static class TestLog4j2Appender extends AbstractAppender {
		private final StringBuilder buffer = new StringBuilder();
		private LoggerConfig loggerConfig;
		private Level previousLevel;

		private TestLog4j2Appender() {
			super("TestLog4j2Appender", null, null, false, Property.EMPTY_ARRAY);
		}

		static TestLog4j2Appender attach(Class<?> loggerClass) {
			TestLog4j2Appender appender = new TestLog4j2Appender();
			appender.start();

			LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			Configuration config = ctx.getConfiguration();
			appender.loggerConfig = config.getLoggerConfig(loggerClass.getName());
			appender.previousLevel = appender.loggerConfig.getLevel();
			appender.loggerConfig.setLevel(Level.WARN);
			appender.loggerConfig.addAppender(appender, Level.WARN, null);
			ctx.updateLoggers();
			return appender;
		}

		void detach() {
			loggerConfig.removeAppender(getName());
			loggerConfig.setLevel(previousLevel);
			((LoggerContext) LogManager.getContext(false)).updateLoggers();
			stop();
		}

		@Override
		public void append(LogEvent event) {
			buffer.append(event.getMessage().getFormattedMessage()).append('\n');
		}

		boolean contains(String text) {
			return buffer.toString().contains(text);
		}
	}
}
