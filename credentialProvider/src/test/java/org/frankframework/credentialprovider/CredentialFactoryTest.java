package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.frankframework.credentialprovider.util.CredentialConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
		ICredentials c = CredentialFactory.getCredentials("account", null, null);

		// Assert
		assertNull(c.getUsername());
		assertNull(c.getPassword());
		assertEquals("account", c.getAlias());
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
