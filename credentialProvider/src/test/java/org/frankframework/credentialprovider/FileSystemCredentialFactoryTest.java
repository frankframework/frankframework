package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
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
	public void testAliasNoDefault() {
		CredentialAlias alias = CredentialAlias.parse("dummy");

		NoSuchElementException e = assertThrows(NoSuchElementException.class, () -> credentialFactory.getSecret(alias));
		assertEquals("cannot obtain credentials from authentication alias [dummy]: alias not found", e.getMessage());
	}

	@Test
	public void testPlainAlias() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("straight");

		ISecret mc = credentialFactory.getSecret(alias);

		assertEquals("username from alias", mc.getField("username"));
		assertEquals("password from alias", mc.getField("password"));
	}

	@Test
	public void testUnknownAlias() {
		CredentialAlias alias = CredentialAlias.parse("unknown");

		assertThrows(NoSuchElementException.class, () -> credentialFactory.getSecret(alias));
	}

	@Test
	public void testAliasWithoutUsername() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("noUsername");
		String expectedPassword = "password from alias";

		ISecret mc = credentialFactory.getSecret(alias);

		assertNull(mc.getField("username"));
		assertEquals(expectedPassword, mc.getField("password"));
	}

	@Test
	public void testPlainCredential() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("singleValue");

		ISecret mc = credentialFactory.getSecret(alias);

		assertThrows(NoSuchElementException.class, () -> mc.getField("username"));
		assertEquals("Plain Credential", mc.getField(""));
	}

	@Test
	public void testGetAliases() throws Exception {
		Collection<String> aliases = credentialFactory.getConfiguredAliases();
		assertEquals("[noUsername, singleValue, straight]", aliases.toString());
	}

}
