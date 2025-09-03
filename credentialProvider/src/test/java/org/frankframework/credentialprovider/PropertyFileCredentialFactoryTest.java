package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
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

	/**
	 *  Make sure to clean up the system properties after the test
	 */
	@AfterAll
	public static void tearDown() {
		System.clearProperty("credentialFactory.map.properties");
	}

	@Test
	public void testPlainAlias() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("straight");
		String expectedUsername = "\\username from alias";
		String expectedPassword = "passw\\urd from alias";

		ISecret mc = credentialFactory.getSecret(alias);

		assertEquals(expectedUsername, mc.getField("username"));
		assertEquals(expectedPassword, mc.getField("password"));
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

		assertThrows(NoSuchElementException.class, () -> mc.getField("username"));
		assertEquals(expectedPassword, mc.getField("password"));
	}

	@Test
	public void testPlainCredential() throws IOException {
		CredentialAlias alias = CredentialAlias.parse("singleValue");

		String expectedPassword = "Plain Credential";

		ISecret mc = credentialFactory.getSecret(alias);

		assertEquals(expectedPassword, mc.getField(""));
		assertThrows(NoSuchElementException.class, () -> mc.getField("password"));
	}

	@Test
	public void testPasswordWithSlashes() throws IOException {
		// Act
		CredentialAlias alias = CredentialAlias.parse("slash");
		ISecret mc = credentialFactory.getSecret(alias);

		// Assert
		assertEquals("username from alias", mc.getField("username"));
		assertEquals("password/with/slash", mc.getField("password"));
	}

	@Test
	public void testGetAliases() {
		// Act
		Collection<String> aliases = credentialFactory.getConfiguredAliases();

		// Arrange
		List<String> sortedAliases = aliases.stream().sorted().collect(Collectors.toList());
		assertEquals("[noUsername, singleValue, slash, straight]", sortedAliases.toString());
	}
}
