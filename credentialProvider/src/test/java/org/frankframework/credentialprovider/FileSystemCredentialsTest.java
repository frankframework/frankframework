package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class FileSystemCredentialsTest {

	private Path root;

	@BeforeEach
	public void setup() {
		String url = this.getClass().getResource("/secrets").toExternalForm();
		root =  Paths.get(url.substring(url.indexOf(":/")+2));
		assumeTrue(Files.exists(root));
	}

	@Test
	public void testPlainAlias() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("straight");

		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";

		FileSystemSecret fsc = new FileSystemSecret(alias, root);

		assertEquals(expectedUsername, fsc.getField("username"));
		assertEquals(expectedPassword, fsc.getField("password"));
	}

	@Test
	public void testAliasWithoutUsername() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("noUsername");

		String expectedPassword = "password from alias";

		FileSystemSecret fsc = new FileSystemSecret(alias, root);

		assertNull(fsc.getField("username"));
		assertEquals(expectedPassword, fsc.getField("password"));
	}

	@Test
	public void testPlainCredential() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("singleValue");

		FileSystemSecret fsc = new FileSystemSecret(alias, root);

		assertThrows(NoSuchElementException.class, () -> fsc.getField("username"));
		assertEquals("Plain Credential", fsc.getField(""));
	}
}
