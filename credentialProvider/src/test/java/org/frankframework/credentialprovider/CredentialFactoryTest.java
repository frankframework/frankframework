package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.credentialprovider.util.CredentialConstants;

class CredentialFactoryTest {

	@BeforeEach
	void setup() {
		MockCredentialFactory.getInstance().clear();
		CredentialFactory.clearInstance();

		// Make sure the defaults are always the same
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "org.frankframework.credentialprovider.PropertyFileCredentialFactory");
	}

	@AfterEach
	void tearDown() {
		// Since credential constants is a singleton, make sure to clean up what we set up in the beforeEach
		CredentialConstants.getInstance().remove("credentialFactory.class");
		MockCredentialFactory.getInstance().clear();
	}

	@Test
	void testFindAliasNoPrefix() {
		// Act
		ICredentials c = CredentialFactory.getCredentials("account");

		// Assert
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}

	@Test
	void testUnknownAliasWithNoUsernameAndDefaultPassword() {
		// Act
		ICredentials c = CredentialFactory.getCredentials("unknown", null, "fakePassword");

		// Assert
		assertNull(c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}

	@Test
	void testFindAliasWithPrefix() {
		// test depends on setting credentialFactory.optionalPrefix=fakePrefix: in test/resources/credentialprovider.properties

		// Act
		ICredentials c = CredentialFactory.getCredentials("fakePrefix:account");

		// Assert
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}

	@Test
	void testGetAliases() throws Exception {
		Collection<String> aliases = CredentialFactory.getConfiguredAliases();
		assertEquals("[aliasWith, alias1, account, alias2]", aliases.toString());
	}

	@Test
	void testNoFactory() {
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "");

		// Act
		assertNull(CredentialFactory.getCredentials("account")); // This should return null
		ICredentials credentials = CredentialFactory.getCredentials("account", null, null); // this a FallbackCredential

		// Assert
		assertInstanceOf(FallbackCredential.class, credentials);
		assertNull(credentials.getUsername());
		assertNull(credentials.getPassword());
		assertEquals("account", credentials.getAlias());
	}

	@Test
	void testAliasWithCustomFields() {
		// Init setting on purpose with extra whitespaces, commas etc.
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "org.frankframework.credentialprovider.PropertyFileCredentialFactory");

		// Act
		ICredentials c = CredentialFactory.getCredentials("aliasWith{username@domain,secret}");

		// Assert values are from the first factory that returns a value
		assertEquals("name@domain.com", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}

	@Test
	void testMultipleFactories() {
		// Init setting on purpose with extra whitespaces, commas etc.
		CredentialConstants.getInstance().setProperty("credentialFactory.class", " java.util.doesNotExist , org.frankframework.credentialprovider.PropertyFileCredentialFactory,,  , org.frankframework.credentialprovider.MockCredentialFactory");
		MockCredentialFactory.add("account", "mockUsername", "mockPassword");
		// Act
		ICredentials c = CredentialFactory.getCredentials("account");

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
		MockCredentialFactory.add("account2", "mockUsername", "mockPassword");

		// Act
		ICredentials c = CredentialFactory.getCredentials("account2");

		// Assert values are from the second factory
		assertEquals("mockUsername", c.getUsername());
		assertEquals("mockPassword", c.getPassword());
	}

	@Test
	void testRightOrderMockFirst() throws Exception {
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "org.frankframework.credentialprovider.MockCredentialFactory, org.frankframework.credentialprovider.PropertyFileCredentialFactory");
		MockCredentialFactory.add("account", "fakeUsername", "mockGoesFirst");
		MockCredentialFactory.add("alias1", "alias1Username", "alias1Password");
		MockCredentialFactory.add("alias2", null, "alias2Password");

		// Act
		ICredentials account = CredentialFactory.getCredentials("account");
		ICredentials alias1 = CredentialFactory.getCredentials("alias1");
		ICredentials alias2 = CredentialFactory.getCredentials("alias2");

		// Assert
		assertEquals("fakeUsername", account.getUsername());
		assertEquals("mockGoesFirst", account.getPassword());
		assertEquals("alias1Username", alias1.getUsername());
		assertEquals("alias1Password", alias1.getPassword());
		assertEquals("alias2Password", alias2.getPassword());
		assertFalse(CredentialFactory.hasCredential("notExists"));
		assertEquals(4, CredentialFactory.getConfiguredAliases().size());
	}

	@Test
	void testRightOrderMockLast() throws Exception {
		CredentialConstants.getInstance().setProperty("credentialFactory.class", "org.frankframework.credentialprovider.PropertyFileCredentialFactory, org.frankframework.credentialprovider.MockCredentialFactory, ");
		MockCredentialFactory.add("account", "mockUsername", "mockGoesSecond");
		MockCredentialFactory.add("alias1", "alias1Username", "alias1Password");
		MockCredentialFactory.add("alias2", null, "alias2Password");
		MockCredentialFactory.add("TheMaster", "masterUsername", "masterPassword");
		MockCredentialFactory.add("TheBachelor", "bachelorUsername", "bachelorPassword");

		// Act
		ICredentials account = CredentialFactory.getCredentials("account");
		ICredentials alias1 = CredentialFactory.getCredentials("alias1");
		ICredentials alias2 = CredentialFactory.getCredentials("alias2");

		// Assert
		assertEquals("fakeUsername", account.getUsername());
		assertEquals("fakePassword", account.getPassword()); // comes from property file, ignore mock value
		assertEquals("username1", alias1.getUsername());
		assertEquals("password1", alias1.getPassword());
		assertEquals("passwordOnly", alias2.getPassword());
		assertEquals("[aliasWith, alias1, account, alias2, TheMaster, TheBachelor]", CredentialFactory.getConfiguredAliases().toString());
		assertTrue(CredentialFactory.hasCredential("TheMaster"));
		assertTrue(CredentialFactory.hasCredential("fakePrefix:TheBachelor"));
	}
}
