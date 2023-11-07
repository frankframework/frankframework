package nl.nn.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	public void testNoAlias() {

		String alias = null;
		String username = "fakeUsername";
		String password = "fakePassword";

		FileSystemCredentials fsc = new FileSystemCredentials(alias, ()->username, ()->password, null);

		assertEquals(username, fsc.getUsername());
		assertEquals(password, fsc.getPassword());
	}

	@Test
	public void testAliasNoDefault() {

		String alias = "dummy";
		String username = null;
		String password = null;

		FileSystemCredentials fsc = new FileSystemCredentials(alias, ()->username, ()->password, null);

		assertEquals(username, fsc.getUsername());
		assertEquals(password, fsc.getPassword());
	}

	@Test
	public void testNoFileSystem() {

		String alias = "fakeAlias";
		String username = "fakeUsername";
		String password = "fakePassword";

		FileSystemCredentials fsc = new FileSystemCredentials(alias, ()->username, ()->password, null);

		assertEquals(username, fsc.getUsername());
		assertEquals(password, fsc.getPassword());
	}

	@Test
	public void testPlainAlias() {

		String alias = "straight";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";

		FileSystemCredentials fsc = new FileSystemCredentials(alias, ()->username, ()->password, root);

		assertEquals(expectedUsername, fsc.getUsername());
		assertEquals(expectedPassword, fsc.getPassword());
	}

	@Test
	public void testAliasWithoutUsername() {

		String alias = "noUsername";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = username;
		String expectedPassword = "password from alias";

		FileSystemCredentials fsc = new FileSystemCredentials(alias, ()->username, ()->password, root);

		assertEquals(expectedUsername, fsc.getUsername());
		assertEquals(expectedPassword, fsc.getPassword());
	}

	@Test
	public void testPlainCredential() {

		String alias = "singleValue";
		String username = null;
		String password = "fakePassword";
		String expectedUsername = null;
		String expectedPassword = "Plain Credential";

		FileSystemCredentials fsc = new FileSystemCredentials(alias, ()->username, ()->password, root);

		assertEquals(expectedUsername, fsc.getUsername());
		assertEquals(expectedPassword, fsc.getPassword());
	}
}
