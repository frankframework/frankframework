package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

		assertThrows(IllegalArgumentException.class, () -> credentialFactory.getCredentials(alias));
	}

	@Test
	public void testAliasNoDefault() {
		String alias = "dummy";

		ICredentials mc = credentialFactory.getCredentials(alias);
		NoSuchElementException e = assertThrows(NoSuchElementException.class, mc::getUsername);
		assertEquals("cannot obtain credentials from authentication alias [dummy]: alias not found", e.getMessage());
	}

	@Test
	public void testPlainAlias() {

		String alias = "straight";

		ICredentials mc = credentialFactory.getCredentials(alias);

		assertEquals("username from alias", mc.getUsername());
		assertEquals("password from alias", mc.getPassword());
	}

	@Test
	public void testUnknownAlias() {
		String alias = "unknown";

		ICredentials mc = credentialFactory.getCredentials(alias);

		assertThrows(NoSuchElementException.class, mc::getUsername);
	}

	@Test
	public void testAliasWithoutUsername() {

		String alias = "noUsername";
		String expectedPassword = "password from alias";

		ICredentials mc = credentialFactory.getCredentials(alias);

		assertNull(mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testPlainCredential() {

		String alias = "singleValue";
		String expectedPassword = "Plain Credential";

		ICredentials mc = credentialFactory.getCredentials(alias);

		assertNull(mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testGetAliases() throws Exception {
		Collection<String> aliases = credentialFactory.getConfiguredAliases();
		assertEquals("[noUsername, singleValue, straight]", aliases.toString());
	}

}
