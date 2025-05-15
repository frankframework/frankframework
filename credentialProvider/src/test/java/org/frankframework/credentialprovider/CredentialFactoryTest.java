package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.credentialprovider.util.CredentialConstants;

class CredentialFactoryTest {

	@BeforeEach
	void setup() {
		CredentialFactory.clearInstance();
		// Make sure the defaults are always the same
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "org.frankframework.credentialprovider.PropertyFileCredentialFactory");
	}

	@Test
	void testFindAliasNoPrefix() {
		// Act
		ICredentials c = CredentialFactory.getCredentials("account", null, null);

		// Assert
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}

	@Test
	void testFindAliasWithPrefix() {
		// test depends on setting credentialFactory.optionalPrefix=fakePrefix: in test/resources/credentialprovider.properties

		// Act
		ICredentials c = CredentialFactory.getCredentials("fakePrefix:account", null, null);

		// Assert
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}

	@Test
	void testGetAliases() throws Exception {
		Collection<String> aliases = CredentialFactory.getConfiguredAliases();
		assertEquals("[alias1, account, alias2]", aliases.toString());
	}

	@Test
	void testNoFactory() {
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "");

		// Act
		ICredentials credentials = CredentialFactory.getCredentials("account", null, null);

		// Assert
		assertNull(credentials.getUsername());
		assertNull(credentials.getPassword());
		assertEquals("account", credentials.getAlias());
	}

	@Test
	void testMultipleFactories() {
		// Init setting on purpose with extra whitespaces, commas etc.
		CredentialConstants.getInstance().setProperty("credentialFactory.class", " java.util.doesNotExist , org.frankframework.credentialprovider.PropertyFileCredentialFactory,,  , org.frankframework.credentialprovider.MockCredentialFactory");
		MockCredentialFactory.getInstance().add("account", "mockUsername", "mockPassword");
		// Act
		ICredentials c = CredentialFactory.getCredentials("account", null, null);

		// Assert values are from the first factory that returns a value
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}

	/**
	 * Account2 doesn't exist in the first provider, but does exist in the second. This test verifies that querying the first provider doesn't
	 * break the lookup by not handling an exception.
	 */
	@Test
	void testMultipleFactoriesWithNotExistingItem() {
		// Init setting on purpose with extra whitespaces, commas etc.
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "nl.nn.credentialprovider.PropertyFileCredentialFactory,nl.nn.credentialprovider.MockCredentialFactory");
		CredentialFactory.getCredentials(null, null, null); // Make sure the factories are initialized and class loading is done
		MockCredentialFactory.getInstance().add("account2", "mockUsername", "mockPassword");

		// Act
		ICredentials c = CredentialFactory.getCredentials("account2", null, null);

		// Assert values are from the second factory
		assertEquals("mockUsername", c.getUsername());
		assertEquals("mockPassword", c.getPassword());
	}

	@Test
	void testRightOrderMockFirst() throws Exception {
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "org.frankframework.credentialprovider.MockCredentialFactory, org.frankframework.credentialprovider.PropertyFileCredentialFactory");
		CredentialFactory.getCredentials(null, null, null); // Make sure the factories are initialized and class loading is done
		MockCredentialFactory.getInstance().add("account", "fakeUsername", "mockGoesFirst");
		MockCredentialFactory.getInstance().add("alias1", "alias1Username", "alias1Password");
		MockCredentialFactory.getInstance().add("alias2", null, "alias2Password");
		// Act
		ICredentials account = CredentialFactory.getCredentials("account", null, null);
		ICredentials alias1 = CredentialFactory.getCredentials("alias1", null, null);
		ICredentials alias2 = CredentialFactory.getCredentials("alias2", null, null);

		// Assert
		assertEquals("fakeUsername", account.getUsername());
		assertEquals("mockGoesFirst", account.getPassword());
		assertEquals("alias1Username", alias1.getUsername());
		assertEquals("alias1Password", alias1.getPassword());
		assertEquals("alias2Password", alias2.getPassword());
		assertFalse(CredentialFactory.hasCredential("notExists"));
		assertEquals(3, CredentialFactory.getConfiguredAliases().size());
	}

	@Test
	void testRightOrderMockLast() throws Exception {
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "org.frankframework.credentialprovider.PropertyFileCredentialFactory, org.frankframework.credentialprovider.MockCredentialFactory, ");
		CredentialFactory.getCredentials(null, null, null); // Make sure the factories are initialized and class loading is done
		MockCredentialFactory.getInstance().add("account", "mockUsername", "mockGoesSecond");
		MockCredentialFactory.getInstance().add("alias1", "alias1Username", "alias1Password");
		MockCredentialFactory.getInstance().add("alias2", null, "alias2Password");
		MockCredentialFactory.getInstance().add("TheMaster", "masterUsername", "masterPassword");
		MockCredentialFactory.getInstance().add("TheBachelor", "bachelorUsername", "bachelorPassword");

		// Act
		ICredentials account = CredentialFactory.getCredentials("account", null, null);
		ICredentials alias1 = CredentialFactory.getCredentials("alias1", null, null);
		ICredentials alias2 = CredentialFactory.getCredentials("alias2", null, null);

		// Assert
		assertEquals("fakeUsername", account.getUsername());
		assertEquals("fakePassword", account.getPassword()); // comes from property file, ignore mock value
		assertEquals("username1", alias1.getUsername());
		assertEquals("password1", alias1.getPassword());
		assertEquals("passwordOnly", alias2.getPassword());
		assertEquals("[alias1, account, alias2, TheMaster, TheBachelor]", CredentialFactory.getConfiguredAliases().toString());
		assertTrue(CredentialFactory.hasCredential("TheMaster"));
		assertTrue(CredentialFactory.hasCredential("fakePrefix:TheBachelor"));
	}
}
