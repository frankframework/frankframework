package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PropertyFileCredentialFactoryTest {

	public String PROPERTIES_FILE="/credentials-unencrypted.txt";

	private PropertyFileCredentialFactory credentialFactory;

	@BeforeEach
	public void setup() throws IOException {
		String propertiesUrl = this.getClass().getResource(PROPERTIES_FILE).toExternalForm();
		String propertiesFile =  Paths.get(propertiesUrl.substring(propertiesUrl.indexOf(":/")+2)).toString();
		assumeTrue(Files.exists(Paths.get(propertiesFile)));

		System.setProperty("credentialFactory.map.properties", propertiesFile);

		credentialFactory = new PropertyFileCredentialFactory();
		credentialFactory.initialize();
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
	public void testPlainAlias() {

		String alias = "straight";
		String defaultUsername = "fakeDefaultUsername";
		String defaultPassword = "fakeDefaultPassword";
		String expectedUsername = "\\username from alias";
		String expectedPassword = "passw\\urd from alias";

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
	public void testPasswordWithSlashes() {
		// Act
		ICredentials mc = credentialFactory.getCredentials("slash", null, null);

		// Assert
		assertEquals("username from alias", mc.getUsername());
		assertEquals("password/with/slash", mc.getPassword());
	}

	@Test
	public void testGetAliases() throws Exception {
		// Act
		Collection<String> aliases = credentialFactory.getConfiguredAliases();

		// Arrange
		List<String> sortedAliases = aliases.stream().sorted().collect(Collectors.toList());
		assertEquals("[noUsername, singleValue, slash, straight]", sortedAliases.toString());
	}
}
