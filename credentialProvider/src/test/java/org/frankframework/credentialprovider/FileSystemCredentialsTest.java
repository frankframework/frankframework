package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	public void testPlainAlias() {
		String alias = "straight";

		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";

		FileSystemCredentials fsc = new FileSystemCredentials(alias, root);

		assertEquals(expectedUsername, fsc.getUsername());
		assertEquals(expectedPassword, fsc.getPassword());
	}

	@Test
	public void testAliasWithoutUsername() {
		String alias = "noUsername";

		String expectedPassword = "password from alias";

		FileSystemCredentials fsc = new FileSystemCredentials(alias, root);

		assertNull(fsc.getUsername());
		assertEquals(expectedPassword, fsc.getPassword());
	}

	@Test
	public void testPlainCredential() {

		String alias = "singleValue";

		FileSystemCredentials fsc = new FileSystemCredentials(alias, root);

		assertNull(fsc.getUsername());
		assertEquals("Plain Credential", fsc.getPassword());
	}
}
