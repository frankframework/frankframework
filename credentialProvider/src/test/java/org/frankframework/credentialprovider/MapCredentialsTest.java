package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MapCredentialsTest {

	private Map<String,String> aliases;

	@BeforeEach
	public void setup() {
		aliases = new HashMap<>();
		aliases.put("noUsername/password", "password from alias");
		aliases.put("straight/username", "username from alias");
		aliases.put("straight/password", "password from alias");
		aliases.put("slash/username", "username from alias");
		aliases.put("slash/password", "password/with/slash");
		aliases.put("slash/secret", "p4ss~w#rd");
		aliases.put("singleValue", "Plain Credential");
	}

	@Test
	public void testUnknownAliasNoDefaults() {
		MapCredentials mc = new MapCredentials(CredentialAlias.parse("fakeAlias"), null);
		assertThrows(NoSuchElementException.class, mc::getUsername);
	}

	@Test
	public void testPasswordWithSlashes() {
		CredentialAlias alias = CredentialAlias.parse("slash");

		MapCredentials mc = new MapCredentials(alias, aliases);
		assertEquals("username from alias", mc.getUsername());
		assertEquals("password/with/slash", mc.getPassword());
	}

	@Test
	public void testPlainAlias() {
		CredentialAlias alias = CredentialAlias.parse("straight");
		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";

		MapCredentials mc = new MapCredentials(alias, aliases);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testAliasWithoutUsername() {
		CredentialAlias alias = CredentialAlias.parse("noUsername");
		String expectedPassword = "password from alias";

		MapCredentials mc = new MapCredentials(alias, aliases);

		assertNull(mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testPlainCredential() {
		CredentialAlias alias = CredentialAlias.parse("singleValue");
		String expectedUsername = null;
		String expectedPassword = "Plain Credential";

		MapCredentials mc = new MapCredentials(alias, aliases);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}
}
