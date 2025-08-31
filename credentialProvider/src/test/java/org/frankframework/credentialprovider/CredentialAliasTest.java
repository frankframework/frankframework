package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class CredentialAliasTest {

	@Test
	public void aliasWithInvalidCharacters() {
		assertNull(CredentialAlias.parse("@#$%^&*"));
	}

	@Test
	public void aliasWithValidCharacters() {
		assertDoesNotThrow(() -> CredentialAlias.parse("alias"));
	}

	@Test
	public void verifyAliasNameWithDefaults() {
		CredentialAlias alias = CredentialAlias.parse("alias");
		assertEquals("alias", alias.getName());
		assertEquals("username", alias.getUsernameField());
		assertEquals("password", alias.getPasswordField());
	}

	@Test
	public void verifyAliasNameWithDifferentUsername() {
		CredentialAlias alias = CredentialAlias.parse("alias{clientId}");
		assertEquals("alias", alias.getName());
		assertEquals("clientId", alias.getUsernameField());
		assertEquals("password", alias.getPasswordField());
	}

	@Test
	public void verifyAliasNameWithDifferentUsernameComma() {
		CredentialAlias alias = CredentialAlias.parse("alias{clientId,}");
		assertEquals("alias", alias.getName());
		assertEquals("clientId", alias.getUsernameField());
		assertEquals("password", alias.getPasswordField());
	}

	@Test
	public void verifyAliasNameWithDifferentPassword() {
		CredentialAlias alias = CredentialAlias.parse("alias{,secret}");
		assertEquals("alias", alias.getName());
		assertEquals("username", alias.getUsernameField());
		assertEquals("secret", alias.getPasswordField());
	}

	@Test
	public void verifyAliasNameWithDifferentUsernameAndPassword() {
		CredentialAlias alias = CredentialAlias.parse("alias{clientId,secret}");
		assertEquals("alias", alias.getName());
		assertEquals("clientId", alias.getUsernameField());
		assertEquals("secret", alias.getPasswordField());
	}

	@Test
	public void aliasWithMultipleFields() {
		CredentialAlias alias = CredentialAlias.parse("aliasWith{name@domain,secret}");
		assertNotNull(alias);
		assertEquals("aliasWith", alias.getName());
		assertEquals("name@domain", alias.getUsernameField());
		assertEquals("secret", alias.getPasswordField());
	}
}
