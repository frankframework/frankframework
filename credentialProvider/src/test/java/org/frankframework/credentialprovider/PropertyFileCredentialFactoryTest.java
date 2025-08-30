package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
	public void testNoAlias() {
		String alias = null;

		assertThrows(IllegalArgumentException.class, () -> credentialFactory.getCredentials(alias));
	}

	@Test
	public void testPlainAlias() {
		String alias = "straight";
		String expectedUsername = "\\username from alias";
		String expectedPassword = "passw\\urd from alias";

		ICredentials mc = credentialFactory.getCredentials(alias);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
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
		String expectedUsername = null;
		String expectedPassword = "Plain Credential";

		ICredentials mc = credentialFactory.getCredentials(alias);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testPasswordWithSlashes() {
		// Act
		ICredentials mc = credentialFactory.getCredentials("slash");

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
