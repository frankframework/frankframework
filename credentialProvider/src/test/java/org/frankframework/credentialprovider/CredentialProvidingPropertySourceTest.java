package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CredentialProvidingPropertySourceTest {

	@Test
	public void testGetExistingUsername() {
		CredentialProvidingPropertySource cpps = new CredentialProvidingPropertySource();

		String key = "alias1/username";
		String expected = "username1";

		String actual = cpps.getProperty(key);
		assertEquals(expected, actual);
	}

	@Test
	public void testGetExistingPassword() {
		CredentialProvidingPropertySource cpps = new CredentialProvidingPropertySource();

		String key = "alias1/password";
		String expected = "password1";

		String actual = cpps.getProperty(key);
		assertEquals(expected, actual);
	}

	@Test
	public void testGetExistingPasswordAsDefault() {
		CredentialProvidingPropertySource cpps = new CredentialProvidingPropertySource();

		String key = "alias1";
		String expected = "password1";

		String actual = cpps.getProperty(key);
		assertEquals(expected, actual);
	}

	@Test
	public void testGetUnknownKey() {
		CredentialProvidingPropertySource cpps = new CredentialProvidingPropertySource();

		String key = "unkownAlias";
		String expected = null;

		String actual = cpps.getProperty(key);

		assertEquals(expected, actual);
	}
}
