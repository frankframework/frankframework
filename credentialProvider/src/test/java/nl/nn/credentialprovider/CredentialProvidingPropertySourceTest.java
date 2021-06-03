package nl.nn.credentialprovider;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class CredentialProvidingPropertySourceTest {

	
	@Before 
	public void setup() {
		MockCredentialFactory mcf = new MockCredentialFactory();
		mcf.add("alias1", "username1", "password1");
		CredentialFactory.getInstance().forceDelegate(mcf);
	}
	
	
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
