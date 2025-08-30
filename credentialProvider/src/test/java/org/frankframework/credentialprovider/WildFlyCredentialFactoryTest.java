package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.security.Provider;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jboss.as.domain.management.plugin.Credential;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.credential.store.CredentialStoreSpi;
import org.wildfly.security.password.interfaces.ClearPassword;

public class WildFlyCredentialFactoryTest {

	private WildFlyCredentialFactory credentialFactory;
	private Set<String> aliases;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setUp() throws Exception {
		credentialFactory = spy(new WildFlyCredentialFactory());
		ServiceContainer serviceContainer = mock(ServiceContainer.class);
		when(credentialFactory.getServiceContainer()).thenReturn(serviceContainer);

		@SuppressWarnings("rawtypes")
		ServiceController credStoreService = mock(ServiceController.class);
		when(serviceContainer.getService(any(ServiceName.class))).thenReturn(credStoreService);

		Provider provider = mock(Provider.class);
		String algorithm = "dummy";
		Provider.Service service = mock(Provider.Service.class);
		CredentialStoreSpi spi = mock(CredentialStoreSpi.class);
		doReturn(spi).when(service).newInstance(null);
		doReturn(service).when(provider).getService(CredentialStore.CREDENTIAL_STORE_TYPE, algorithm);
		CredentialStore credentialStore = CredentialStore.getInstance(algorithm, provider);

		doReturn(credentialStore).when(credStoreService).getValue();

		when(spi.getAliases()).thenAnswer(i -> aliases);
		when(spi.exists(any(String.class), isA(Credential.class.getClass()))).thenAnswer(i -> {
			String alias = i.getArgument(0);
			Class<? extends Credential> credentialClass = i.getArgument(1);
			return credentialClass.equals(PasswordCredential.class) && aliases!=null && aliases.contains(alias);
		});

		doAnswer(i -> {
			String alias = i.getArgument(0);
			ClearPassword clearPassword = mock(ClearPassword.class);
			when(clearPassword.getPassword()).thenReturn((alias+"-value").toCharArray());
			return new PasswordCredential(clearPassword);
		}).when(spi).retrieve(anyString(), isA(Credential.class.getClass()), isNull(), isNull(), isNull());

		credentialFactory.initialize();
	}

	@Test
	public void testGetAliases() throws UnsupportedOperationException, CredentialStoreException {
		String[] aliasesArray = {"a", "b", "c"};
		this.aliases = new HashSet<>(Arrays.asList(aliasesArray));

		Collection<String> result = credentialFactory.getConfiguredAliases();

		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
		assertTrue(result.contains("b"));
		assertTrue(result.contains("c"));
	}

	@Test
	public void testGetAliasesWithUsername() throws UnsupportedOperationException, CredentialStoreException {
		String[] aliasesArray = {"a", "a/username", "b"};
		this.aliases = new HashSet<>(Arrays.asList(aliasesArray));

		Collection<String> result = credentialFactory.getConfiguredAliases();

		assertEquals(2, result.size());
		assertTrue(result.contains("a"));
		assertTrue(result.contains("b"));
	}

	@Test
	public void testGetAliasesNoneFound() throws UnsupportedOperationException, CredentialStoreException {
		this.aliases = null;

		Collection<String> result = credentialFactory.getConfiguredAliases();

		assertEquals(0, result.size());
	}

	@Test
	public void testHasCredentials() throws UnsupportedOperationException {
		String[] aliasesArray = {"a", "a/username", "b", "c/username"};
		this.aliases = new HashSet<>(Arrays.asList(aliasesArray));

		assertTrue(credentialFactory.hasCredentials("a"));
		assertTrue(credentialFactory.hasCredentials("b"));
		assertTrue(credentialFactory.hasCredentials("c"));
		assertFalse(credentialFactory.hasCredentials("d"));
	}

	@Test
	public void testGetCredentialsExistingAlias() throws UnsupportedOperationException {
		String[] aliasesArray = {"a", "a/username", "b", "c/username"};
		this.aliases = new HashSet<>(Arrays.asList(aliasesArray));

		WildFlyCredentials wfc = (WildFlyCredentials)credentialFactory.getCredentials("a");
		assertEquals("a", wfc.getAlias());
		assertEquals("a/username-value", wfc.getUsername());
		assertEquals("a-value", wfc.getPassword());
	}

	@Test
	public void testGetCredentialsExistingAliasNoUsername() throws UnsupportedOperationException {
		String[] aliasesArray = {"a", "a/username", "b", "c/username"};
		this.aliases = new HashSet<>(Arrays.asList(aliasesArray));

		WildFlyCredentials wfc = (WildFlyCredentials) credentialFactory.getCredentials("b");
		assertEquals("b", wfc.getAlias());
		assertEquals("", wfc.getUsername());
		assertEquals("b-value", wfc.getPassword());
	}

	@Test
	public void testGetCredentialsExistingAliasNoPassword() throws UnsupportedOperationException {
		String[] aliasesArray = {"a", "a/username", "b", "c/username"};
		this.aliases = new HashSet<>(Arrays.asList(aliasesArray));

		WildFlyCredentials wfc = (WildFlyCredentials)credentialFactory.getCredentials("c");
		assertEquals("c", wfc.getAlias());
		assertEquals("c/username-value", wfc.getUsername());
		assertEquals("", wfc.getPassword());
	}

	@Test
	public void testGetCredentialsNotExistingAlias() throws UnsupportedOperationException {
		String[] aliasesArray = {"a", "a/username", "b", "c/username"};
		this.aliases = new HashSet<>(Arrays.asList(aliasesArray));

		WildFlyCredentials wfc = (WildFlyCredentials)credentialFactory.getCredentials("d");
		assertEquals("d", wfc.getAlias());
		assertThrows(NoSuchElementException.class, wfc::getUsername);
		assertThrows(NoSuchElementException.class, wfc::getPassword);
	}

}
