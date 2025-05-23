package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		aliases.put("singleValue", "Plain Credential");
	}

	@Test
	public void testNoAlias() {
		String alias = null;
		String username = "fakeUsername";
		String password = "fakePassword";

		MapCredentials mc = new MapCredentials(alias, ()->username, ()->password, null);

		assertEquals(username, mc.getUsername());
		assertEquals(password, mc.getPassword());
	}

	@Test
	public void testUnknownAliasNoDefaults() {
		String alias = "fakeAlias";
		String username = null;
		String password = null;

		assertThrows(NoSuchElementException.class, () -> {
			MapCredentials mc = new MapCredentials(alias, ()->username, ()->password, null);
			assertEquals(username, mc.getUsername());
			assertEquals(password, mc.getPassword());
		});
	}

	@Test
	public void testUnknownAlias() {
		String alias = "fakeAlias";
		String username = "fakeUsername";
		String password = "fakePassword";

		MapCredentials mc = new MapCredentials(alias, ()->username, ()->password, aliases);
		assertEquals(username, mc.getUsername());
		assertEquals(password, mc.getPassword());
	}

	@Test
	public void testPasswordWithSlashes() {
		String alias = "slash";
		String username = "fakeUsername";
		String password = "fakePassword";

		MapCredentials mc = new MapCredentials(alias, ()->username, ()->password, aliases);
		assertEquals("username from alias", mc.getUsername());
		assertEquals("password/with/slash", mc.getPassword());
	}

	@Test
	public void testPlainAlias() {
		String alias = "straight";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";

		MapCredentials mc = new MapCredentials(alias, ()->username, ()->password, aliases);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testAliasWithoutUsername() {
		String alias = "noUsername";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = username;
		String expectedPassword = "password from alias";

		MapCredentials mc = new MapCredentials(alias, ()->username, ()->password, aliases);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testPlainCredential() {
		String alias = "singleValue";
		String username = null;
		String password = "fakePassword";
		String expectedUsername = null;
		String expectedPassword = "Plain Credential";

		MapCredentials mc = new MapCredentials(alias, ()->username, ()->password, aliases);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}
}
