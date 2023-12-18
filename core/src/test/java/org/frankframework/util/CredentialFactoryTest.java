package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		assertThrows(NoSuchElementException.class, () -> {
			CredentialFactory cf = new CredentialFactory("unknown");
			assertEquals("fakeDefaultUsername", cf.getUsername());
			assertEquals("fakeDefaultPassword", cf.getPassword());
		});
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

}
