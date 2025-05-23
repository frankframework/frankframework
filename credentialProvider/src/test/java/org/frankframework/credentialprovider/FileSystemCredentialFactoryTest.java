package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileSystemCredentialFactoryTest {

	public String FS_SECRETS_FOLDER = "/credentials-unencrypted.txt";

	private FileSystemCredentialFactory credentialFactory;

	@BeforeEach
	public void setup() {
		String url = this.getClass().getResource("/secrets").toExternalForm();
		Path root =  Paths.get(url.substring(url.indexOf(":/")+2));
		assumeTrue(Files.exists(root));

		credentialFactory = new FileSystemCredentialFactory();
		System.setProperty("credentialFactory.filesystem.root", root.toString());
		credentialFactory.initialize();
	}

	/**
	 *  Make sure to clean up the system properties after the tests
	 */
	@AfterAll
	public static void tearDown() {
		System.clearProperty("credentialFactory.filesystem.root");
	}

	@Test
	public void testNoAlias() {

		String alias = null;
		String username = "fakeUsername";
		String password = "fakePassword";

		ICredentials mc = credentialFactory.getCredentials(alias, ()->username, ()->password);

		assertEquals(username, mc.getUsername());
		assertEquals(password, mc.getPassword());
	}

	@Test
	public void testAliasNoDefault() {
		String alias = "dummy";

		ICredentials mc = credentialFactory.getCredentials(alias, null, null);
		NoSuchElementException e = assertThrows(NoSuchElementException.class, mc::getUsername);
		assertEquals("cannot obtain credentials from authentication alias [dummy]: alias not found", e.getMessage());
	}

	@Test
	public void testPlainAlias() {

		String alias = "straight";
		String defaultUsername = "fakeDefaultUsername";
		String defaultPassword = "fakeDefaultPassword";
		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";

		ICredentials mc = credentialFactory.getCredentials(alias, ()->defaultUsername, ()->defaultPassword);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testUnknownAlias() {

		String alias = "unknown";
		String defaultUsername = "fakeDefaultUsername";
		String defaultPassword = "fakeDefaultPassword";
		String expectedUsername = defaultUsername;
		String expectedPassword = defaultPassword;

		ICredentials mc = credentialFactory.getCredentials(alias, ()->defaultUsername, ()->defaultPassword);

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

		ICredentials mc = credentialFactory.getCredentials(alias, ()->username, ()->password);

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

		ICredentials mc = credentialFactory.getCredentials(alias, ()->username, ()->password);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testGetAliases() throws Exception {
		Collection<String> aliases = credentialFactory.getConfiguredAliases();
		assertEquals("[noUsername, singleValue, straight]", aliases.toString());
	}

}
