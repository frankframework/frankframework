package nl.nn.credentialprovider;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.credentialprovider.util.AppConstants;

public class CredentialFactoryTest {

	@Test
	public void testFindAliasNoPrefix() {
		AppConstants ac = AppConstants.getInstance();
		ac.setProperty("credentialFactory.class", MockMapCredentialFactory.class.getName());
		ac.setProperty("credentialFactory.optionalPrefix", "fakePrefix:");
		
		ICredentials c = CredentialFactory.getCredentials("account", null, null);
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}
	
	@Test
	public void testFindAliasWithPrefix() {
		AppConstants ac = AppConstants.getInstance();
		ac.setProperty("credentialFactory.class", MockMapCredentialFactory.class.getName());
		ac.setProperty("credentialFactory.optionalPrefix", "fakePrefix:");
		
		ICredentials c = CredentialFactory.getCredentials("fakePrefix:account", null, null);
		assertEquals("fakeUsername", c.getUsername());
		assertEquals("fakePassword", c.getPassword());
	}
	
}
