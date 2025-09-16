package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class CredentialFactoryTest {

	@Test
	public void testCredentialFactory() {
		CredentialFactory cf = new CredentialFactory("alias1");
		assertEquals("username1", cf.getUsername());
		assertEquals("password1", cf.getPassword());
	}

	@Test
	public void testCredentialFactoryDefaultsShouldNotInterfere() {
		CredentialFactory cf = new CredentialFactory("alias1", "fakeDefaultUsername", "fakeDefaultPassword");
		assertEquals("username1", cf.getUsername());
		assertEquals("password1", cf.getPassword());
	}

	@Test
	public void testCredentialFactoryUnknownAliasNoDefaults() {
		// This uses the PropertyFileCredentialFactory
		NoSuchElementException e = assertThrows(NoSuchElementException.class, () -> new CredentialFactory("unknown"));
		assertEquals("cannot obtain credentials from authentication alias [unknown]: alias not found", e.getMessage());

		// Technically the same method, but to ensure it works as expected test to see if we get the same exception.
		NoSuchElementException f = assertThrows(NoSuchElementException.class, () -> new CredentialFactory("unknown", null, null));
		assertEquals("cannot obtain credentials from authentication alias [unknown]: alias not found", f.getMessage());
	}

	@Test
	public void testCredentialFactoryUnknownAlias() {
		CredentialFactory cf = new CredentialFactory("unknown", "fakeDefaultUsername", "fakeDefaultPassword");
		assertEquals("fakeDefaultUsername", cf.getUsername());
		assertEquals("fakeDefaultPassword", cf.getPassword());
	}

	@Test
	public void testCredentialFactoryPasswordOnlyAlias() {
		CredentialFactory cf = new CredentialFactory("alias2", "fakeDefaultUsername", "fakeDefaultPassword");
		assertEquals("fakeDefaultUsername", cf.getUsername());
		assertEquals("passwordOnly", cf.getPassword());
	}

	@Test
	public void passwordOnlyAlias() {
		CredentialFactory cf = new CredentialFactory("alias2", null, null);
		assertNull(cf.getUsername());
		assertEquals("passwordOnly", cf.getPassword());
	}

	@Test
	public void passwordOnlyAliasWhichShouldOverrideDefault() {
		CredentialFactory cf = new CredentialFactory("alias2", null, "fakeDefaultPassword");
		assertNull(cf.getUsername());
		assertEquals("passwordOnly", cf.getPassword());
	}

	@Test
	public void hyphenInAlias() {
		CredentialFactory cf = new CredentialFactory("alias-with-hyphen", null, null);
		assertNull(cf.getUsername());
		assertEquals("passwordOnly", cf.getPassword());
	}
}
