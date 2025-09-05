package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MapSecretTest {

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
		CredentialAlias alias = CredentialAlias.parse("fakeAlias");
		assertThrows(NoSuchElementException.class, () -> new MapSecret(alias, aliases));
	}

	@Test
	public void testPasswordWithSlashes() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("slash");

		MapSecret mc = new MapSecret(alias, aliases);
		assertEquals("username from alias", mc.getField("username"));
		assertEquals("password/with/slash", mc.getField("password"));
	}

	@Test
	public void testPlainAlias() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("straight");
		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";

		MapSecret mc = new MapSecret(alias, aliases);

		assertEquals(expectedUsername, mc.getField("username"));
		assertEquals(expectedPassword, mc.getField("password"));
	}

	@Test
	public void testAliasWithoutUsername() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("noUsername");
		String expectedPassword = "password from alias";

		MapSecret mc = new MapSecret(alias, aliases);

		assertThrows(NoSuchElementException.class, () -> mc.getField("username"));
		assertEquals(expectedPassword, mc.getField("password"));
	}

	@Test
	public void testPlainCredential() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("singleValue");

		MapSecret mc = new MapSecret(alias, aliases);

		assertThrows(NoSuchElementException.class, () -> mc.getField("username"));
		assertEquals("Plain Credential", mc.getField(""));
	}
}
