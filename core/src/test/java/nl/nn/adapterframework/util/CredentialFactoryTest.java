package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.NoSuchElementException;

import org.junit.Test;

public class CredentialFactoryTest {

	@Test
	public void testCredentialFactory() {
		CredentialFactory cf = new CredentialFactory("alias1", null, null);
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
			CredentialFactory cf = new CredentialFactory("unknown", null, null);
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
