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
	public void testNoAlias() {
		String alias = null;

		assertThrows(IllegalArgumentException.class, () -> new MapCredentials(alias, null));
	}

	@Test
	public void testUnknownAliasNoDefaults() {
		String alias = "fakeAlias";
		String username = null;
		String password = null;

		assertThrows(NoSuchElementException.class, () -> {
			MapCredentials mc = new MapCredentials(alias, null);
			assertEquals(username, mc.getUsername());
			assertEquals(password, mc.getPassword());
		});
	}

	@Test
	public void testUnknownAlias() {
		String alias = "fakeAlias";

		MapCredentials mc = new MapCredentials(alias, aliases);

		assertThrows(NoSuchElementException.class, mc::getUsername);
	}

	@Test
	public void testPasswordWithSlashes() {
		String alias = "slash";

		MapCredentials mc = new MapCredentials(alias, aliases);
		assertEquals("username from alias", mc.getUsername());
		assertEquals("password/with/slash", mc.getPassword());
	}

	@Test
	public void testPlainAlias() {
		String alias = "straight";
		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";

		MapCredentials mc = new MapCredentials(alias, aliases);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testAliasWithoutUsername() {
		String alias = "noUsername";
		String expectedPassword = "password from alias";

		MapCredentials mc = new MapCredentials(alias, aliases);

		assertNull(mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testPlainCredential() {
		String alias = "singleValue";
		String expectedUsername = null;
		String expectedPassword = "Plain Credential";

		MapCredentials mc = new MapCredentials(alias, aliases);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}
}
