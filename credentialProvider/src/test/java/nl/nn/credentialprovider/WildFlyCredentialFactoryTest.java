package nl.nn.credentialprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.password.interfaces.ClearPassword;

public class WildFlyCredentialFactoryTest {

	private WildFlyCredentialFactory credentialFactory;

	CredentialStore credentialStore = mock(CredentialStore.class);

	Set<String> aliases;


	@Rule
	public final ExternalResource serviceContainerMock = new ExternalResource() {
		MockedStatic<CurrentServiceContainer> currentServiceContainer;

		@Override
		protected void before() throws Throwable {
			currentServiceContainer = Mockito.mockStatic(CurrentServiceContainer.class);
			ServiceContainer serviceContainer = mock(ServiceContainer.class);
			ServiceController credStoreService = mock(ServiceController.class);

			currentServiceContainer.when(CurrentServiceContainer::getServiceContainer).thenReturn(serviceContainer);
			when(serviceContainer.getService(any(ServiceName.class))).thenReturn(credStoreService);
			when(credStoreService.getValue()).thenReturn(credentialStore);

			when(credentialStore.getAliases()).thenAnswer(i -> aliases);
			when(credentialStore.exists(any(String.class), any(Class.class))).thenAnswer(i -> {
				String alias = i.getArgument(0);
				Class credentialClass = i.getArgument(1);
				return credentialClass.equals(PasswordCredential.class) && aliases!=null && aliases.contains(alias);
			});
			when(credentialStore.retrieve(any(String.class), any(Class.class))).thenAnswer(i -> {
				String alias = i.getArgument(0);
				Class credentialClass = i.getArgument(1);

				PasswordCredential passwordCredential = mock(PasswordCredential.class);
				ClearPassword clearPassword = mock(ClearPassword.class);
				when(passwordCredential.getPassword()).thenReturn(clearPassword);
				when(clearPassword.getPassword()).thenReturn((alias+"-value").toCharArray());

				return credentialClass.equals(PasswordCredential.class) && aliases!=null && aliases.contains(alias) ? passwordCredential : null;
			});

			credentialFactory = new WildFlyCredentialFactory();
			credentialFactory.initialize();
		};

		@Override protected void after(){
			currentServiceContainer.close();
		};
	};


	@Test
	public void testGetAliases() throws UnsupportedOperationException, CredentialStoreException {
		String[] aliasesArray = {"a", "b", "c"};
		this.aliases = new HashSet<String>(Arrays.asList(aliasesArray));

		Collection<String> result = credentialFactory.getConfiguredAliases();

		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
		assertTrue(result.contains("b"));
		assertTrue(result.contains("c"));
	}

	@Test
	public void testGetAliasesWithUsername() throws UnsupportedOperationException, CredentialStoreException {
		String[] aliasesArray = {"a", "a/username", "b"};
		this.aliases = new HashSet<String>(Arrays.asList(aliasesArray));

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
	public void testHasCredentials() throws UnsupportedOperationException, CredentialStoreException {
		String[] aliasesArray = {"a", "a/username", "b", "c/username"};
		this.aliases = new HashSet<String>(Arrays.asList(aliasesArray));

		assertTrue(credentialFactory.hasCredentials("a"));
		assertTrue(credentialFactory.hasCredentials("b"));
		assertTrue(credentialFactory.hasCredentials("c"));
		assertFalse(credentialFactory.hasCredentials("d"));
	}

	@Test
	public void testGetCredentialsExistingAlias() throws UnsupportedOperationException, CredentialStoreException {
		String[] aliasesArray = {"a", "a/username", "b", "c/username"};
		this.aliases = new HashSet<String>(Arrays.asList(aliasesArray));

		WildFlyCredentials wfc = (WildFlyCredentials)credentialFactory.getCredentials("a", () -> "defaultUsername", () -> "defaultPassword");
		assertEquals("a", wfc.getAlias());
		assertEquals("a/username-value", wfc.getUsername());
		assertEquals("a-value", wfc.getPassword());
	}

	@Test
	public void testGetCredentialsExistingAliasNoUsername() throws UnsupportedOperationException, CredentialStoreException {
		String[] aliasesArray = {"a", "a/username", "b", "c/username"};
		this.aliases = new HashSet<String>(Arrays.asList(aliasesArray));

		WildFlyCredentials wfc = (WildFlyCredentials)credentialFactory.getCredentials("b", () -> "defaultUsername", () -> "defaultPassword");
		assertEquals("b", wfc.getAlias());
		assertEquals("", wfc.getUsername());
		assertEquals("b-value", wfc.getPassword());
	}

	@Test
	public void testGetCredentialsExistingAliasNoPassword() throws UnsupportedOperationException, CredentialStoreException {
		String[] aliasesArray = {"a", "a/username", "b", "c/username"};
		this.aliases = new HashSet<String>(Arrays.asList(aliasesArray));

		WildFlyCredentials wfc = (WildFlyCredentials)credentialFactory.getCredentials("c", () -> "defaultUsername", () -> "defaultPassword");
		assertEquals("c", wfc.getAlias());
		assertEquals("c/username-value", wfc.getUsername());
		assertEquals("", wfc.getPassword());
	}

	@Test
	public void testGetCredentialsNotExistingAlias() throws UnsupportedOperationException, CredentialStoreException {
		String[] aliasesArray = {"a", "a/username", "b", "c/username"};
		this.aliases = new HashSet<String>(Arrays.asList(aliasesArray));

		WildFlyCredentials wfc = (WildFlyCredentials)credentialFactory.getCredentials("d", () -> "defaultUsername", () -> "defaultPassword");
		assertEquals("d", wfc.getAlias());
		assertEquals("defaultUsername", wfc.getUsername());
		assertEquals("defaultPassword", wfc.getPassword());
	}

}
